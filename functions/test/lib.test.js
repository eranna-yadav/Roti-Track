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
  assert.strictEqual(lib.PLANS.rotitrack_pro_yearly.rupees, 1099);
  assert.strictEqual(lib.PLANS.rotitrack_pro_yearly.referralRupees, 990);
});

test("referral price: yearly, someone else's code, first subscription only", () => {
  const user = { referredByCode: "RTABC234" };
  assert.ok(lib.referralPriceEligible(user, "rotitrack_pro_yearly", "friend", "me"));
  assert.ok(!lib.referralPriceEligible(user, "rotitrack_pro_monthly", "friend", "me"));
  assert.ok(!lib.referralPriceEligible({}, "rotitrack_pro_yearly", "friend", "me"));
  assert.ok(!lib.referralPriceEligible(user, "rotitrack_pro_yearly", null, "me"), "unknown code");
  assert.ok(!lib.referralPriceEligible(user, "rotitrack_pro_yearly", "me", "me"), "own code");
  assert.ok(!lib.referralPriceEligible({ ...user, razorpayPlanId: "rotitrack_pro_yearly" }, "rotitrack_pro_yearly", "friend", "me"), "already subscribed once");
});

test("referral subscription: ₹990 now, regular yearly billing from next year", () => {
  const f = lib.referralSubscriptionFields("rotitrack_pro_yearly", 1_000_000);
  assert.strictEqual(f.start_at, 1_000_000 + 365 * 86400);
  assert.strictEqual(f.addons[0].item.amount, 99000);
  assert.strictEqual(f.addons[0].item.currency, "INR");
  // Paid up until the regular plan's first charge.
  const fields = lib.userFieldsFromSubscription({ id: "sub_1", status: "authenticated", charge_at: f.start_at, paid_count: 0 }, "rotitrack_pro_yearly");
  assert.strictEqual(fields.razorpayUntil, f.start_at * 1000);
});

test("referral pays once, only for a verified paid plan", () => {
  const now = 1_000;
  const monthly = { referredByCode: "RTABC234", razorpayPlanId: "rotitrack_pro_monthly", razorpayUntil: 5_000 };
  // Monthly: ₹250 after the 1st payment, ₹250 more after the 2nd, then nothing.
  assert.strictEqual(lib.referralToCredit({ ...monthly, razorpayPaidCount: 1 }, now), 250);
  assert.strictEqual(lib.referralToCredit({ ...monthly, razorpayPaidCount: 1, referralCreditedAmount: 250 }, now), 0);
  assert.strictEqual(lib.referralToCredit({ ...monthly, razorpayPaidCount: 2, referralCreditedAmount: 250 }, now), 250);
  assert.strictEqual(lib.referralToCredit({ ...monthly, razorpayPaidCount: 5, referralCreditedAmount: 500 }, now), 0);
  // Yearly: ₹500 at once; switching from monthly pays the rest.
  const yearly = { ...monthly, razorpayPlanId: "rotitrack_pro_yearly", razorpayPaidCount: 1 };
  assert.strictEqual(lib.referralToCredit(yearly, now), 500);
  assert.strictEqual(lib.referralToCredit({ ...yearly, referralCreditedAmount: 250 }, now), 250);
  // Nothing without a code, without a current paid plan, or for accounts credited before instalments.
  assert.strictEqual(lib.referralToCredit({ ...monthly, referredByCode: undefined, razorpayPaidCount: 1 }, now), 0);
  assert.strictEqual(lib.referralToCredit({ ...monthly, razorpayUntil: 500, razorpayPaidCount: 1 }, now), 0);
  assert.strictEqual(lib.referralToCredit({ ...yearly, referralCredited: true }, now), 0);
});

test("a late Razorpay event never lowers the payment count", () => {
  const existing = { razorpaySubscriptionId: "sub_1", razorpayPaidCount: 2 };
  assert.strictEqual(lib.keepHighestPaidCount(existing, { razorpaySubscriptionId: "sub_1", razorpayPaidCount: 1 }).razorpayPaidCount, 2);
  assert.strictEqual(lib.keepHighestPaidCount(existing, { razorpaySubscriptionId: "sub_2", razorpayPaidCount: 1 }).razorpayPaidCount, 1);
});
