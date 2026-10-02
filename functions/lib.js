// Pure helpers, kept free of Firebase so they can be unit-tested.
const crypto = require("crypto");

/** App product IDs → price in rupees. Must match Plan in the Android app. */
const PLANS = {
  rotitrack_pro_monthly: { rupees: 359, period: "monthly", totalCount: 120 },
  // Friends who sign up with a referral code pay referralRupees for the first year.
  rotitrack_pro_yearly: { rupees: 1099, referralRupees: 990, period: "yearly", totalCount: 10 },
};

const YEAR_SECONDS = 365 * 86400;

/**
 * True when this user gets the first-year referral price: the yearly plan, signed up
 * with someone else's code, and never subscribed through Razorpay before.
 */
function referralPriceEligible(user, planId, referrerUid, uid) {
  return planId === "rotitrack_pro_yearly"
    && !!(user && user.referredByCode)
    && !!referrerUid && referrerUid !== uid
    && !user.razorpayPlanId && !user.razorpaySubscriptionId;
}

/**
 * Razorpay subscription for the referral price: the friend pays referralRupees now (an
 * upfront add-on), and the regular yearly plan starts charging one year later.
 */
function referralSubscriptionFields(planId, nowSec) {
  const plan = PLANS[planId];
  return {
    start_at: nowSec + YEAR_SECONDS,
    addons: [{ item: { name: "Roti Track Pro – first year (referral price)", amount: plan.referralRupees * 100, currency: "INR" } }],
  };
}

function hmac(secret, payload) {
  return crypto.createHmac("sha256", secret).update(payload).digest("hex");
}

function safeEqual(a, b) {
  const x = Buffer.from(String(a || ""));
  const y = Buffer.from(String(b || ""));
  return x.length === y.length && crypto.timingSafeEqual(x, y);
}

/** Checkout success signature for a subscription: HMAC(payment_id|subscription_id, key_secret). */
function verifyPaymentSignature({ paymentId, subscriptionId, signature }, keySecret) {
  return safeEqual(hmac(keySecret, `${paymentId}|${subscriptionId}`), signature);
}

/** Webhook signature: HMAC(raw body, webhook_secret) in the X-Razorpay-Signature header. */
function verifyWebhookSignature(rawBody, signature, webhookSecret) {
  return safeEqual(hmac(webhookSecret, rawBody), signature);
}

/**
 * Firestore fields for a user, from a Razorpay subscription entity.
 * Pro lasts until the end of the paid cycle, even after a cancellation.
 */
function userFieldsFromSubscription(sub, planId) {
  const endSec = sub.current_end || sub.charge_at || 0;
  const paid = ["active", "authenticated", "cancelled", "completed", "pending"].includes(sub.status);
  return {
    razorpaySubscriptionId: sub.id,
    razorpayPlanId: planId,
    razorpayStatus: sub.status,
    razorpayUntil: paid ? endSec * 1000 : 0,
    razorpayPaidCount: sub.paid_count || 0,
  };
}

/**
 * Razorpay events can arrive out of order; never let a late one lower the number
 * of payments already counted for the same subscription.
 */
function keepHighestPaidCount(existing, fields) {
  if (existing && existing.razorpaySubscriptionId === fields.razorpaySubscriptionId) {
    return { ...fields, razorpayPaidCount: Math.max(existing.razorpayPaidCount || 0, fields.razorpayPaidCount || 0) };
  }
  return fields;
}

/** Google Play external-transaction amounts. Prices are GST-inclusive (18%). */
function playAmounts(rupees) {
  const totalMicros = rupees * 1_000_000;
  const preTax = Math.round(totalMicros / 1.18);
  return {
    originalPreTaxAmount: { priceMicros: String(preTax), currency: "INR" },
    originalTaxAmount: { priceMicros: String(totalMicros - preTax), currency: "INR" },
  };
}

/**
 * Referral rules (must match the app's Referral rules page):
 * - yearly plan: ₹500 once it's paid;
 * - monthly plan: ₹250 after the 1st payment and ₹250 after the 2nd;
 * - never more than ₹500 per friend in total.
 */
const REFERRAL_REWARD = 500;
const REFERRAL_MONTHLY_INSTALMENT = 250;

/** How much a referred user's payments have earned their referrer in total so far. */
function referralEarnedFor(user, now) {
  if (!user || !user.referredByCode || !((user.razorpayUntil || 0) > now)) return 0;
  if (user.razorpayPlanId === "rotitrack_pro_yearly") return REFERRAL_REWARD;
  const payments = Math.max(user.razorpayPaidCount || 0, 1);
  return Math.min(REFERRAL_REWARD, REFERRAL_MONTHLY_INSTALMENT * payments);
}

/** Already credited for this user (accounts from before instalments count as fully paid). */
function referralCreditedSoFar(user) {
  if (!user) return 0;
  if (typeof user.referralCreditedAmount === "number") return user.referralCreditedAmount;
  return user.referralCredited ? REFERRAL_REWARD : 0;
}

/** ₹ to credit the referrer now; 0 when nothing new is owed. */
function referralToCredit(user, now) {
  return Math.max(0, referralEarnedFor(user, now) - referralCreditedSoFar(user));
}

module.exports = {
  referralPriceEligible, referralSubscriptionFields, YEAR_SECONDS,
  REFERRAL_REWARD, REFERRAL_MONTHLY_INSTALMENT, referralEarnedFor, referralCreditedSoFar, referralToCredit, keepHighestPaidCount, PLANS, hmac, verifyPaymentSignature, verifyWebhookSignature, userFieldsFromSubscription, playAmounts };
