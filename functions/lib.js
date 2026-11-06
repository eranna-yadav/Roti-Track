// Pure helpers, kept free of Firebase so they can be unit-tested.
const crypto = require("crypto");

/** App product IDs → price in rupees. Must match Plan in the Android app. */
const PLANS = {
  rotitrack_pro_monthly: { rupees: 359, period: "monthly", totalCount: 120 },
  // Friends who sign up with a referral code pay referralRupees for the first year.
  rotitrack_pro_yearly: { rupees: 1099, referralRupees: 999, period: "yearly", totalCount: 10 },
};

const YEAR_SECONDS = 365 * 86400;

/** "rt 4dj 492" → "RT4DJ492"; null when empty. Must match normalizeReferralCode in the app. */
function normalizeReferralCode(code) {
  const c = String(code || "").trim().toUpperCase().replace(/ /g, "");
  return c || null;
}

/**
 * True when this user gets the first-year referral price: the yearly plan, a code that
 * belongs to someone else, and never subscribed before (Razorpay or Google Play).
 */
function referralPriceEligible(user, planId, referrerUid, uid) {
  return planId === "rotitrack_pro_yearly"
    && !!referrerUid && referrerUid !== uid
    && !(user && (user.razorpayPlanId || user.razorpaySubscriptionId || user.playPlanId));
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
 * - yearly plan: ₹300 once it's paid;
 * - monthly plan: ₹150 after the 1st payment and ₹150 after the 2nd;
 * - never more than ₹300 per friend in total;
 * - plus a monthly bonus for the number of yearly sales in a calendar month (IST).
 */
const REFERRAL_REWARD = 300;
const REFERRAL_MONTHLY_INSTALMENT = 150;

/** Yearly sales in one month → bonus for that month. Only the highest level reached counts. */
const REFERRAL_BONUS_LEVELS = [
  [100, 10000], [75, 8500], [50, 7000], [40, 5000], [30, 3500], [20, 2000],
];

function referralBonusFor(yearlySales) {
  for (const [sales, bonus] of REFERRAL_BONUS_LEVELS) if (yearlySales >= sales) return bonus;
  return 0;
}

/** "2026-11" for a moment in India time, so a month runs from the 1st to the last day in IST. */
function istMonthKey(ms) {
  const d = new Date(ms + 330 * 60_000);
  return `${d.getUTCFullYear()}-${String(d.getUTCMonth() + 1).padStart(2, "0")}`;
}

/** The plan a user has paid for right now, from Razorpay or a server-verified Google Play purchase. */
function paidPlan(user, now) {
  if (!user) return null;
  if ((user.razorpayUntil || 0) > now) return { planId: user.razorpayPlanId, payments: user.razorpayPaidCount || 0 };
  if ((user.playUntil || 0) > now) return { planId: user.playPlanId, payments: user.playPaidCount || 0 };
  return null;
}

/** How much a referred user's payments have earned their referrer in total so far. */
function referralEarnedFor(user, now) {
  if (!user || !user.referredByCode) return 0;
  const plan = paidPlan(user, now);
  if (!plan) return 0;
  if (plan.planId === "rotitrack_pro_yearly") return REFERRAL_REWARD;
  return Math.min(REFERRAL_REWARD, REFERRAL_MONTHLY_INSTALMENT * Math.max(plan.payments, 1));
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

/** True when this credit is for a yearly plan, so it counts as one yearly sale for the monthly bonus. */
function isYearlySale(user, now) {
  const plan = paidPlan(user, now);
  return !!plan && plan.planId === "rotitrack_pro_yearly";
}

/**
 * The referrer's fields after one more yearly sale in [month]: the month's sale count, and the
 * bonus not yet paid for it (the bonus for the new count minus what the month already paid).
 */
function addYearlySale(referrer, month) {
  const months = (referrer && referrer.referralMonths) || {};
  const prev = months[month] || { yearly: 0, bonus: 0 };
  const yearly = (prev.yearly || 0) + 1;
  const bonus = Math.max(prev.bonus || 0, referralBonusFor(yearly));
  return { month, yearly, bonus, bonusNow: bonus - (prev.bonus || 0) };
}

/** Number of payments so far for a Play subscription: renewals push the expiry one period on. */
function playPaymentsSoFar(planId, startMs, expiryMs) {
  if (planId === "rotitrack_pro_yearly") return 1;
  const month = 30.44 * 86_400_000;
  return Math.max(1, Math.round((expiryMs - startMs) / month));
}

/** Play subscription states in which the user has paid for the current period. */
const PLAY_PAID_STATES = ["SUBSCRIPTION_STATE_ACTIVE", "SUBSCRIPTION_STATE_IN_GRACE_PERIOD", "SUBSCRIPTION_STATE_CANCELED"];

module.exports = {
  normalizeReferralCode, referralPriceEligible, referralSubscriptionFields, YEAR_SECONDS,
  REFERRAL_REWARD, REFERRAL_MONTHLY_INSTALMENT, REFERRAL_BONUS_LEVELS, referralBonusFor, istMonthKey, paidPlan,
  isYearlySale, addYearlySale, playPaymentsSoFar, PLAY_PAID_STATES, referralEarnedFor, referralCreditedSoFar, referralToCredit, keepHighestPaidCount, PLANS, hmac, verifyPaymentSignature, verifyWebhookSignature, userFieldsFromSubscription, playAmounts };
