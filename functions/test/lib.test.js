const test = require("node:test");
const assert = require("node:assert");
const lib = require("../lib");

test("payment signature is HMAC of payment_id|subscription_id", () => {
  const secret = "test_secret";
  const signature = lib.hmac(secret, "pay_123|sub_456");
  assert.ok(lib.verifyPaymentSignature({ paymentId: "pay_123", subscriptionId: "sub_456", signature }, secret));
  assert.ok(!lib.verifyPaymentSignature({ paymentId: "pay_999", subscriptionId: "sub_456", signature }, secret));
  assert.ok(!lib.verifyPaymentSignature({ paymentId: "pay_123", subscriptionId: "sub_456", signature: "short" }, secret));
  assert.ok(!lib.verifyPaymentSignature({ paymentId: "pay_123", subscriptionId: "sub_456", signature }, "other"));
});

test("webhook signature covers the raw body", () => {
  const body = Buffer.from('{"event":"subscription.charged"}');
  const sig = lib.hmac("wh", body);
  assert.ok(lib.verifyWebhookSignature(body, sig, "wh"));
  assert.ok(!lib.verifyWebhookSignature(Buffer.from('{"event":"x"}'), sig, "wh"));
  assert.ok(!lib.verifyWebhookSignature(body, undefined, "wh"));
});

test("Pro lasts to the end of the paid cycle, including after cancel", () => {
  const f = lib.userFieldsFromSubscription({ id: "sub_1", status: "cancelled", current_end: 1_800_000_000 }, "rotitrack_pro_monthly");
  assert.strictEqual(f.razorpayUntil, 1_800_000_000_000);
  assert.strictEqual(f.razorpayPlanId, "rotitrack_pro_monthly");
  const halted = lib.userFieldsFromSubscription({ id: "sub_1", status: "halted", current_end: 1_800_000_000 }, "rotitrack_pro_monthly");
  assert.strictEqual(halted.razorpayUntil, 0);
});

test("Play amounts split 18% GST out of the price", () => {
  const a = lib.playAmounts(359);
  assert.strictEqual(Number(a.originalPreTaxAmount.priceMicros) + Number(a.originalTaxAmount.priceMicros), 359_000_000);
  assert.strictEqual(a.originalPreTaxAmount.priceMicros, "304237288");
});

test("plan prices match the app", () => {
  assert.strictEqual(lib.PLANS.rotitrack_pro_monthly.rupees, 359);
  assert.strictEqual(lib.PLANS.rotitrack_pro_yearly.rupees, 990);
});

test("referral pays once, only for a verified paid plan", () => {
  const now = 1_000;
  assert.ok(lib.shouldCreditReferral({ referredByCode: "RTABC234", razorpayUntil: 5_000 }, now));
  assert.ok(!lib.shouldCreditReferral({ referredByCode: "RTABC234", razorpayUntil: 5_000, referralCredited: true }, now));
  assert.ok(!lib.shouldCreditReferral({ referredByCode: "RTABC234", razorpayUntil: 500 }, now));
  assert.ok(!lib.shouldCreditReferral({ razorpayUntil: 5_000 }, now));
  assert.strictEqual(lib.REFERRAL_REWARD, 500);
});
