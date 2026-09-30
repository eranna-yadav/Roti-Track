/**
 * Roti Track payments backend (Firebase Cloud Functions, Node 22).
 *
 * The Razorpay key secret lives only here. The app asks for a subscription,
 * pays in Razorpay Checkout, then sends the result back to be verified.
 * Webhooks keep Pro in step with renewals, failures and cancellations.
 *
 * Set up (see ../README.md):
 *   firebase functions:secrets:set RAZORPAY_KEY_ID
 *   firebase functions:secrets:set RAZORPAY_KEY_SECRET
 *   firebase functions:secrets:set RAZORPAY_WEBHOOK_SECRET
 *   and put the two Razorpay plan IDs in functions/.env
 */
const { onCall, onRequest, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { defineSecret, defineString } = require("firebase-functions/params");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const Razorpay = require("razorpay");
const Anthropic = require("@anthropic-ai/sdk");
const lib = require("./lib");
const scan = require("./scan");

admin.initializeApp();
const db = admin.firestore();

const REGION = "asia-south1";
const KEY_ID = defineSecret("RAZORPAY_KEY_ID");
const KEY_SECRET = defineSecret("RAZORPAY_KEY_SECRET");
const WEBHOOK_SECRET = defineSecret("RAZORPAY_WEBHOOK_SECRET");
const PLAN_MONTHLY = defineString("RAZORPAY_PLAN_MONTHLY", { description: "Razorpay plan_id for ₹359/month" });
const PLAN_YEARLY = defineString("RAZORPAY_PLAN_YEARLY", { description: "Razorpay plan_id for ₹990/year" });
const ANDROID_PACKAGE = "com.rotitrack.app";
const ANTHROPIC_API_KEY = defineSecret("ANTHROPIC_API_KEY");
const SCAN_MODEL = defineString("CLAUDE_SCAN_MODEL", {
  default: "claude-opus-5-5",
  description: "Claude model that reads food photos",
});
const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");
const GEMINI_SCAN_MODEL = defineString("GEMINI_SCAN_MODEL", {
  default: "gemini-flash-latest",
  description: "Gemini model that reads food photos",
});

function razorpay() {
  return new Razorpay({ key_id: KEY_ID.value(), key_secret: KEY_SECRET.value() });
}

function razorpayPlanFor(planId) {
  if (planId === "rotitrack_pro_monthly") return PLAN_MONTHLY.value();
  if (planId === "rotitrack_pro_yearly") return PLAN_YEARLY.value();
  return null;
}

function requireAuth(req) {
  if (!req.auth) throw new HttpsError("unauthenticated", "Sign in first.");
  return req.auth.uid;
}

/** Step 1: create a Razorpay subscription for the signed-in user. */
exports.createRazorpaySubscription = onCall(
  { region: REGION, secrets: [KEY_ID, KEY_SECRET] },
  async (req) => {
    const uid = requireAuth(req);
    const planId = req.data && req.data.plan;
    const rzpPlan = razorpayPlanFor(planId);
    if (!rzpPlan) throw new HttpsError("invalid-argument", "Unknown plan.");

    const user = (await db.doc(`users/${uid}`).get()).data() || {};
    if (user.blocked) throw new HttpsError("permission-denied", "Account blocked.");
    if (user.razorpayUntil > Date.now() && user.razorpayStatus === "active") {
      throw new HttpsError("already-exists", "You already have an active Pro subscription.");
    }

    const sub = await razorpay().subscriptions.create({
      plan_id: rzpPlan,
      total_count: lib.PLANS[planId].totalCount,
      customer_notify: 1,
      notes: {
        uid,
        planId,
        // Present when the user picked Razorpay on Google Play's choice screen.
        externalTransactionToken: (req.data && req.data.externalTransactionToken) || "",
      },
    });
    await db.doc(`payments/${sub.id}`).set({
      uid, planId, status: sub.status, createdAt: Date.now(),
      externalTransactionToken: sub.notes.externalTransactionToken || null,
    });
    return { subscriptionId: sub.id, keyId: KEY_ID.value() };
  }
);

/** Step 2: verify the Checkout result and switch Pro on. */
exports.verifyRazorpayPayment = onCall(
  { region: REGION, secrets: [KEY_ID, KEY_SECRET] },
  async (req) => {
    const uid = requireAuth(req);
    const { paymentId, subscriptionId, signature } = req.data || {};
    if (!paymentId || !subscriptionId || !signature) throw new HttpsError("invalid-argument", "Missing payment details.");
    if (!lib.verifyPaymentSignature({ paymentId, subscriptionId, signature }, KEY_SECRET.value())) {
      logger.warn("Bad payment signature", { uid, subscriptionId });
      throw new HttpsError("permission-denied", "Payment could not be verified.");
    }

    const sub = await razorpay().subscriptions.fetch(subscriptionId);
    if (!sub.notes || sub.notes.uid !== uid) throw new HttpsError("permission-denied", "This payment belongs to another account.");

    const fields = lib.userFieldsFromSubscription(sub, sub.notes.planId);
    // The payment was just verified, so at least one has been made even if Razorpay hasn't counted it yet.
    fields.razorpayPaidCount = Math.max(fields.razorpayPaidCount, 1);
    // A just-authenticated subscription may not have current_end yet; cover the first cycle.
    if (!fields.razorpayUntil) {
      const days = sub.notes.planId === "rotitrack_pro_yearly" ? 366 : 31;
      fields.razorpayUntil = Date.now() + days * 86_400_000;
    }
    await setSubscriptionFields(uid, fields);
    await db.doc(`payments/${subscriptionId}`).set({ status: sub.status, lastPaymentId: paymentId, verifiedAt: Date.now() }, { merge: true });
    await reportToPlay(sub, paymentId, true).catch((e) => logger.error("Play report failed", e));
    return { ok: true, until: fields.razorpayUntil };
  }
);

/** Lets a user stop auto-renew. Pro stays until the end of the paid period. */
exports.cancelRazorpaySubscription = onCall(
  { region: REGION, secrets: [KEY_ID, KEY_SECRET] },
  async (req) => {
    const uid = requireAuth(req);
    const user = (await db.doc(`users/${uid}`).get()).data() || {};
    if (!user.razorpaySubscriptionId) throw new HttpsError("not-found", "No Razorpay subscription.");
    const sub = await razorpay().subscriptions.cancel(user.razorpaySubscriptionId, true);
    await db.doc(`users/${uid}`).set({ razorpayStatus: sub.status === "active" ? "cancelled" : sub.status }, { merge: true });
    return { ok: true };
  }
);

/** Saves a user's subscription state without ever lowering the count of payments made. */
async function setSubscriptionFields(uid, fields) {
  const ref = db.doc(`users/${uid}`);
  await db.runTransaction(async (tx) => {
    const existing = (await tx.get(ref)).data();
    tx.set(ref, lib.keepHighestPaidCount(existing, fields), { merge: true });
  });
}

/** Razorpay → us: renewals, failed charges, cancellations. Point the dashboard webhook here. */
exports.razorpayWebhook = onRequest(
  { region: REGION, secrets: [KEY_ID, KEY_SECRET, WEBHOOK_SECRET] },
  async (req, res) => {
    const signature = req.get("X-Razorpay-Signature");
    if (!lib.verifyWebhookSignature(req.rawBody, signature, WEBHOOK_SECRET.value())) {
      res.status(400).send("bad signature");
      return;
    }
    const event = req.body.event || "";
    const sub = req.body.payload && req.body.payload.subscription && req.body.payload.subscription.entity;
    if (!event.startsWith("subscription.") || !sub || !sub.notes || !sub.notes.uid) {
      res.status(200).send("ignored");
      return;
    }
    await setSubscriptionFields(sub.notes.uid, lib.userFieldsFromSubscription(sub, sub.notes.planId));
    await db.doc(`payments/${sub.id}`).set({ status: sub.status, lastEvent: event, updatedAt: Date.now() }, { merge: true });

    if (event === "subscription.charged") {
      const payment = req.body.payload.payment && req.body.payload.payment.entity;
      if (payment) await reportToPlay(sub, payment.id, false).catch((e) => logger.error("Play report failed", e));
    }
    res.status(200).send("ok");
  }
);

/**
 * Google Play user choice billing: every transaction made through Razorpay
 * after the user picked it on Play's choice screen must be reported to Google
 * within 24 hours. Needs the Android Publisher API enabled and this service
 * account invited in Play Console (see README). Skipped for direct installs.
 */
async function reportToPlay(sub, paymentId, initial) {
  const token = sub.notes && sub.notes.externalTransactionToken;
  if (!token) return;
  const { google } = require("googleapis");
  const auth = new google.auth.GoogleAuth({ scopes: ["https://www.googleapis.com/auth/androidpublisher"] });
  const publisher = google.androidpublisher({ version: "v3", auth });
  const payments = db.doc(`payments/${sub.id}`);
  const first = (await payments.get()).data() || {};
  const body = {
    ...lib.playAmounts(lib.PLANS[sub.notes.planId].rupees),
    transactionTime: new Date().toISOString(),
    userTaxAddress: { regionCode: "IN" },
    recurringTransaction: initial
      ? { externalTransactionToken: token, externalSubscription: { subscriptionType: "RECURRING" } }
      : { initialExternalTransactionId: first.playTransactionId, externalSubscription: { subscriptionType: "RECURRING" } },
  };
  const id = `rzp_${paymentId}`.replace(/[^A-Za-z0-9_-]/g, "_");
  await publisher.externaltransactions.createexternaltransaction({
    parent: `applications/${ANDROID_PACKAGE}`,
    externalTransactionId: id,
    requestBody: body,
  });
  if (initial) await payments.set({ playTransactionId: id }, { merge: true });
}

/**
 * Referral rewards, paid as a friend's payments are confirmed (see lib.js for the rules):
 * ₹500 for a yearly plan; ₹250 + ₹250 over the first two monthly payments.
 * Runs in a transaction and keeps a running total per friend, so nothing is paid twice.
 */
exports.creditReferral = onDocumentWritten({ document: "users/{uid}", region: REGION }, async (event) => {
  const after = event.data && event.data.after && event.data.after.data();
  if (!after) return;
  const uid = event.params.uid;
  if (lib.referralToCredit(after, Date.now()) <= 0) return;

  const codeDoc = await db.doc(`referralCodes/${after.referredByCode}`).get();
  const referrerUid = codeDoc.exists && codeDoc.data().uid;
  if (!referrerUid || referrerUid === uid) return;

  const credited = await db.runTransaction(async (tx) => {
    const userRef = db.doc(`users/${uid}`);
    const fresh = (await tx.get(userRef)).data() || {};
    const amount = lib.referralToCredit(fresh, Date.now());
    if (amount <= 0) return 0;
    const before = lib.referralCreditedSoFar(fresh);
    const total = before + amount;
    tx.set(userRef, { referralCredited: true, referralCreditedAmount: total, referralCreditedTo: referrerUid }, { merge: true });
    tx.set(db.doc(`users/${referrerUid}`), {
      referralEarnings: admin.firestore.FieldValue.increment(amount),
      // A friend counts once, on their first reward.
      referralCount: admin.firestore.FieldValue.increment(before === 0 ? 1 : 0),
    }, { merge: true });
    tx.set(db.doc(`referrals/${uid}_${total}`), {
      referrerUid, referredUid: uid, amount, totalForFriend: total,
      planId: fresh.razorpayPlanId || null, payments: fresh.razorpayPaidCount || 0, createdAt: Date.now(),
    });
    return amount;
  });
  if (credited) logger.info("Referral credited", { uid, referrerUid, amount: credited });
});

/**
 * Pro: reads a food photo with Claude and returns the dishes it sees, matched to the app's
 * food catalog where possible. The app shows them for the user to check before logging.
 */
exports.scanFood = onCall(
  { region: REGION, secrets: [ANTHROPIC_API_KEY, GEMINI_API_KEY], timeoutSeconds: 120, memory: "512MiB" },
  async (req) => {
    const uid = req.auth && req.auth.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Sign in to scan food.");
    const provider = scan.pickProvider(GEMINI_API_KEY.value(), ANTHROPIC_API_KEY.value());
    if (!provider) {
      throw new HttpsError("failed-precondition", "Food scanning isn't set up yet. Please try again later.");
    }
    const image = req.data && req.data.image;
    const bad = scan.checkImage(image);
    if (bad) throw new HttpsError("invalid-argument", bad);

    const user = (await db.doc(`users/${uid}`).get()).data();
    if (!scan.isPro(user)) throw new HttpsError("permission-denied", "Food scanning is part of Roti Track Pro.");

    // A daily cap per account keeps the AI bill (or the free quota) predictable.
    const usage = db.doc(`scanUsage/${uid}`);
    const day = scan.scanDay();
    await db.runTransaction(async (tx) => {
      const u = (await tx.get(usage)).data() || {};
      const count = u.day === day ? u.count || 0 : 0;
      if (count >= scan.DAILY_SCAN_LIMIT) {
        throw new HttpsError("resource-exhausted", "You've used today's food scans. Try again tomorrow.");
      }
      tx.set(usage, { day, count: count + 1 });
    });

    const meal = req.data.meal;
    const parsed = provider === "gemini" ? await askGemini(image, meal) : await askClaude(image, meal);
    logger.info("scanFood", { uid, provider, items: (parsed.items || []).length });
    return scan.sanitize(parsed);
  },
);

/** Reads the photo with Claude; returns the parsed answer or throws an HttpsError for the app. */
async function askClaude(image, meal) {
  const client = new Anthropic({ apiKey: ANTHROPIC_API_KEY.value() });
  let response;
  try {
    response = await client.beta.messages.create(scan.buildRequest(SCAN_MODEL.value(), image, meal));
  } catch (e) {
    if (e instanceof Anthropic.RateLimitError) {
      throw new HttpsError("resource-exhausted", "The scanner is busy. Try again in a minute.");
    } else if (e instanceof Anthropic.APIError) {
      logger.error("scanFood: Claude API error", { status: e.status, message: e.message });
    } else {
      logger.error("scanFood: request failed", { message: e.message });
    }
    throw new HttpsError("unavailable", "Couldn't scan the photo. Check your internet and try again.");
  }
  if (response.stop_reason === "refusal") {
    throw new HttpsError("failed-precondition", "Couldn't read this photo. Try another one.");
  }
  const text = response.content.filter((b) => b.type === "text").map((b) => b.text).join("");
  try {
    return JSON.parse(text);
  } catch (e) {
    logger.error("scanFood: unreadable Claude answer", { stop: response.stop_reason, length: text.length });
    throw new HttpsError("internal", "Couldn't read this photo. Try again.");
  }
}

/** Reads the photo with Gemini; returns the parsed answer or throws an HttpsError for the app. */
async function askGemini(image, meal) {
  const { url, body } = scan.buildGeminiRequest(GEMINI_SCAN_MODEL.value(), image, meal);
  let res;
  try {
    res = await fetch(url, {
      method: "POST",
      headers: { "content-type": "application/json", "x-goog-api-key": GEMINI_API_KEY.value() },
      body: JSON.stringify(body),
      signal: AbortSignal.timeout(100_000),
    });
  } catch (e) {
    logger.error("scanFood: Gemini request failed", { message: e.message });
    throw new HttpsError("unavailable", "Couldn't scan the photo. Check your internet and try again.");
  }
  const json = await res.json().catch(() => null);
  if (res.status === 429) {
    throw new HttpsError("resource-exhausted", "The scanner is busy. Try again in a minute.");
  }
  if (!res.ok) {
    logger.error("scanFood: Gemini API error", { status: res.status, error: json && json.error });
    throw new HttpsError("unavailable", "Couldn't scan the photo. Check your internet and try again.");
  }
  const parsed = scan.geminiAnswer(json);
  if (!parsed) {
    const c = json && json.candidates && json.candidates[0];
    logger.error("scanFood: unreadable Gemini answer", {
      finish: c && c.finishReason, block: json && json.promptFeedback && json.promptFeedback.blockReason,
    });
    throw new HttpsError("failed-precondition", "Couldn't read this photo. Try another one.");
  }
  return parsed;
}
