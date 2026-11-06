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
const lib = require("./lib");

admin.initializeApp();
const db = admin.firestore();

const REGION = "asia-south1";
const KEY_ID = defineSecret("RAZORPAY_KEY_ID");
const KEY_SECRET = defineSecret("RAZORPAY_KEY_SECRET");
const WEBHOOK_SECRET = defineSecret("RAZORPAY_WEBHOOK_SECRET");
const PLAN_MONTHLY = defineString("RAZORPAY_PLAN_MONTHLY", { description: "Razorpay plan_id for ₹359/month" });
const PLAN_YEARLY = defineString("RAZORPAY_PLAN_YEARLY", { description: "Razorpay plan_id for ₹1,099/year" });
const ANDROID_PACKAGE = "com.rotitrack.app";

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
    if (user.razorpayUntil > Date.now() && ["active", "authenticated"].includes(user.razorpayStatus)) {
      throw new HttpsError("already-exists", "You already have an active Pro subscription.");
    }

    // A friend's referral code, typed on the yearly plan's payment page, makes the first year
    // cheaper. Checked here, never trusted from the app.
    const code = lib.normalizeReferralCode(req.data && req.data.referralCode);
    let referralPrice = false;
    if (code) {
      const codeDoc = await db.doc(`referralCodes/${code}`).get();
      // A replaced (retired) code still pays for friends who already used it, but takes no new ones.
      const referrerUid = codeDoc.exists && !codeDoc.data().retired ? codeDoc.data().uid : null;
      if (!lib.referralPriceEligible(user, planId, referrerUid, uid)) {
        throw new HttpsError("failed-precondition", "This referral code can't be used. Remove it to pay the regular price.");
      }
      referralPrice = true;
      // The friend who shared the code earns their reward once this is paid (see creditReferral).
      if (!user.referredByCode) await db.doc(`users/${uid}`).set({ referredByCode: code }, { merge: true });
    }

    const sub = await razorpay().subscriptions.create({
      plan_id: rzpPlan,
      total_count: lib.PLANS[planId].totalCount,
      customer_notify: 1,
      ...(referralPrice ? lib.referralSubscriptionFields(planId, Math.floor(Date.now() / 1000)) : {}),
      notes: {
        uid,
        planId,
        referralPrice: referralPrice ? "1" : "",
        // Present when the user picked Razorpay on Google Play's choice screen.
        externalTransactionToken: (req.data && req.data.externalTransactionToken) || "",
      },
    });
    await db.doc(`payments/${sub.id}`).set({
      uid, planId, status: sub.status, createdAt: Date.now(),
      externalTransactionToken: sub.notes.externalTransactionToken || null,
      referralPrice,
    });
    return { subscriptionId: sub.id, keyId: KEY_ID.value(), referralPrice };
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
    // The first payment of a referral-price subscription is the cheaper first year.
    ...lib.playAmounts(initial && sub.notes.referralPrice ? lib.PLANS[sub.notes.planId].referralRupees : lib.PLANS[sub.notes.planId].rupees),
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
 * Google Play purchase check. The app sends each Play subscription it sees; the server asks
 * Google whether it's real and who bought it, then records the paid plan on the user so the
 * referral rules below apply to Play payments too. Needs the Android Publisher API enabled
 * and this service account invited in Play Console (see README).
 */
exports.verifyPlayPurchase = onCall({ region: REGION }, async (req) => {
  const uid = requireAuth(req);
  const { purchaseToken, productId } = req.data || {};
  if (!purchaseToken || !lib.PLANS[productId]) throw new HttpsError("invalid-argument", "Missing purchase details.");

  const { google } = require("googleapis");
  const auth = new google.auth.GoogleAuth({ scopes: ["https://www.googleapis.com/auth/androidpublisher"] });
  const publisher = google.androidpublisher({ version: "v3", auth });
  const { data: sub } = await publisher.purchases.subscriptionsv2.get({ packageName: ANDROID_PACKAGE, token: purchaseToken });

  const buyer = sub.externalAccountIdentifiers && sub.externalAccountIdentifiers.obfuscatedExternalAccountId;
  if (buyer && buyer !== uid) throw new HttpsError("permission-denied", "This purchase belongs to another account.");
  const item = (sub.lineItems || []).find((i) => i.productId === productId);
  if (!item) throw new HttpsError("invalid-argument", "Purchase doesn't match the plan.");

  // One purchase can only ever count for one Roti Track account.
  const tokenRef = db.doc(`playPurchases/${lib.hmac("play", purchaseToken)}`);
  const owner = await db.runTransaction(async (tx) => {
    const doc = await tx.get(tokenRef);
    if (doc.exists) return doc.data().uid;
    tx.set(tokenRef, { uid, productId, createdAt: Date.now() });
    return uid;
  });
  if (owner !== uid) throw new HttpsError("permission-denied", "This purchase is already linked to another account.");

  const expiry = Date.parse(item.expiryTime || "") || 0;
  const paid = lib.PLAY_PAID_STATES.includes(sub.subscriptionState);
  const fields = {
    playPlanId: productId,
    playState: sub.subscriptionState || null,
    playUntil: paid ? expiry : 0,
    playPaidCount: lib.playPaymentsSoFar(productId, Date.parse(sub.startTime || "") || expiry, expiry),
    playTest: !!sub.testPurchase,
  };

  // The referral code typed on the payment page counts like one entered at sign-up.
  const userRef = db.doc(`users/${uid}`);
  const user = (await userRef.get()).data() || {};
  const code = lib.normalizeReferralCode(req.data.referralCode);
  if (code && !user.referredByCode) {
    const codeDoc = await db.doc(`referralCodes/${code}`).get();
    if (codeDoc.exists && !codeDoc.data().retired && codeDoc.data().uid !== uid) fields.referredByCode = code;
  }
  // Keep the highest payment count seen, in case Google reports an older state.
  fields.playPaidCount = Math.max(fields.playPaidCount, user.playPlanId === productId ? user.playPaidCount || 0 : 0);
  await userRef.set(fields, { merge: true });
  return { ok: true, until: fields.playUntil };
});

/**
 * Referral rewards, paid as a friend's payments are confirmed (see lib.js for the rules):
 * ₹300 for a yearly plan; ₹150 + ₹150 over the first two monthly payments; and a bonus
 * when the referrer's yearly sales in a month reach a new level.
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
    const now = Date.now();
    const userRef = db.doc(`users/${uid}`);
    const referrerRef = db.doc(`users/${referrerUid}`);
    const fresh = (await tx.get(userRef)).data() || {};
    const referrer = (await tx.get(referrerRef)).data() || {};
    const amount = lib.referralToCredit(fresh, now);
    if (amount <= 0) return null;
    const before = lib.referralCreditedSoFar(fresh);
    const total = before + amount;
    const plan = lib.paidPlan(fresh, now);
    const yearly = lib.isYearlySale(fresh, now);
    const sale = yearly ? lib.addYearlySale(referrer, lib.istMonthKey(now)) : null;
    const bonus = sale ? sale.bonusNow : 0;

    tx.set(userRef, { referralCredited: true, referralCreditedAmount: total, referralCreditedTo: referrerUid }, { merge: true });
    tx.set(referrerRef, {
      referralEarnings: admin.firestore.FieldValue.increment(amount + bonus),
      // A friend counts once, on their first reward.
      referralCount: admin.firestore.FieldValue.increment(before === 0 ? 1 : 0),
      ...(sale ? {
        referralBonusEarnings: admin.firestore.FieldValue.increment(bonus),
        referralMonths: { [sale.month]: { yearly: sale.yearly, bonus: sale.bonus } },
        // This month's numbers, for the Refer & earn screen.
        referralMonth: sale.month, referralMonthYearly: sale.yearly, referralMonthBonus: sale.bonus,
      } : {}),
    }, { merge: true });
    tx.set(db.doc(`referrals/${uid}_${total}`), {
      referrerUid, referredUid: uid, amount, totalForFriend: total,
      planId: plan ? plan.planId : null, payments: plan ? plan.payments : 0, createdAt: now,
    });
    if (bonus > 0) {
      tx.set(db.doc(`referrals/bonus_${referrerUid}_${sale.month}_${sale.bonus}`), {
        referrerUid, amount: bonus, bonusLevel: sale.yearly, month: sale.month, createdAt: now,
      });
    }
    return { amount, bonus };
  });
  if (credited) logger.info("Referral credited", { uid, referrerUid, ...credited });
});
