// Pure helpers, kept free of Firebase so they can be unit-tested.
const crypto = require("crypto");

/** App product IDs → price in rupees. Must match Plan in the Android app. */
const PLANS = {
  rotitrack_pro_monthly: { rupees: 259, period: "monthly", totalCount: 120 },
  rotitrack_pro_yearly: { rupees: 990, period: "yearly", totalCount: 10 },
};

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
  };
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

module.exports = { PLANS, hmac, verifyPaymentSignature, verifyWebhookSignature, userFieldsFromSubscription, playAmounts };
