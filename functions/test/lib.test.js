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
  assert.strictEqual(lib.PLANS.rotitrack_pro_yearly.referralRupees, 999);
});

test("referral price: yearly, someone else's code, first subscription only", () => {
  const user = {};
  assert.ok(lib.referralPriceEligible(user, "rotitrack_pro_yearly", "friend", "me"));
  assert.ok(!lib.referralPriceEligible(user, "rotitrack_pro_monthly", "friend", "me"));
  assert.strictEqual(lib.normalizeReferralCode(" rt 4dj492 "), "RT4DJ492");
  assert.strictEqual(lib.normalizeReferralCode(""), null);
  assert.ok(!lib.referralPriceEligible(user, "rotitrack_pro_yearly", null, "me"), "unknown code");
  assert.ok(!lib.referralPriceEligible(user, "rotitrack_pro_yearly", "me", "me"), "own code");
  assert.ok(!lib.referralPriceEligible({ ...user, razorpayPlanId: "rotitrack_pro_yearly" }, "rotitrack_pro_yearly", "friend", "me"), "already subscribed once");
});

test("referral subscription: ₹999 now, regular yearly billing from next year", () => {
  const f = lib.referralSubscriptionFields("rotitrack_pro_yearly", 1_000_000);
  assert.strictEqual(f.start_at, 1_000_000 + 365 * 86400);
  assert.strictEqual(f.addons[0].item.amount, 99900);
  assert.strictEqual(f.addons[0].item.currency, "INR");
  // Paid up until the regular plan's first charge.
  const fields = lib.userFieldsFromSubscription({ id: "sub_1", status: "authenticated", charge_at: f.start_at, paid_count: 0 }, "rotitrack_pro_yearly");
  assert.strictEqual(fields.razorpayUntil, f.start_at * 1000);
});

test("referral pays once, only for a verified paid plan", () => {
  const now = 1_000;
  const monthly = { referredByCode: "PRIYA100", razorpayPlanId: "rotitrack_pro_monthly", razorpayUntil: 5_000 };
  // Monthly: ₹150 after the 1st payment, ₹150 more after the 2nd, then nothing.
  assert.strictEqual(lib.referralToCredit({ ...monthly, razorpayPaidCount: 1 }, now), 150);
  assert.strictEqual(lib.referralToCredit({ ...monthly, razorpayPaidCount: 1, referralCreditedAmount: 150 }, now), 0);
  assert.strictEqual(lib.referralToCredit({ ...monthly, razorpayPaidCount: 2, referralCreditedAmount: 150 }, now), 150);
  assert.strictEqual(lib.referralToCredit({ ...monthly, razorpayPaidCount: 5, referralCreditedAmount: 300 }, now), 0);
  // Yearly: ₹300 at once; switching from monthly pays the rest.
  const yearly = { ...monthly, razorpayPlanId: "rotitrack_pro_yearly", razorpayPaidCount: 1 };
  assert.strictEqual(lib.referralToCredit(yearly, now), 300);
  assert.strictEqual(lib.referralToCredit({ ...yearly, referralCreditedAmount: 150 }, now), 150);
  // Friends already paid out under the old ₹500 rules get nothing more.
  assert.strictEqual(lib.referralToCredit({ ...yearly, referralCreditedAmount: 500 }, now), 0);
  // Nothing without a code, without a current paid plan, or for accounts credited before instalments.
  assert.strictEqual(lib.referralToCredit({ ...monthly, referredByCode: undefined, razorpayPaidCount: 1 }, now), 0);
  assert.strictEqual(lib.referralToCredit({ ...monthly, razorpayUntil: 500, razorpayPaidCount: 1 }, now), 0);
  assert.strictEqual(lib.referralToCredit({ ...yearly, referralCredited: true }, now), 0);
});

test("Google Play purchases earn referrals like Razorpay ones", () => {
  const now = 1_000;
  const play = { referredByCode: "PRIYA100", playPlanId: "rotitrack_pro_yearly", playUntil: 5_000, playPaidCount: 1 };
  assert.strictEqual(lib.referralToCredit(play, now), 300);
  assert.ok(lib.isYearlySale(play, now));
  assert.strictEqual(lib.referralToCredit({ ...play, playUntil: 0 }, now), 0, "expired or not paid");
  const monthly = { ...play, playPlanId: "rotitrack_pro_monthly" };
  assert.strictEqual(lib.referralToCredit(monthly, now), 150);
  assert.ok(!lib.isYearlySale(monthly, now));
  // Payments counted from how far renewals have moved the expiry.
  const day = 86_400_000;
  assert.strictEqual(lib.playPaymentsSoFar("rotitrack_pro_monthly", 0, 31 * day), 1);
  assert.strictEqual(lib.playPaymentsSoFar("rotitrack_pro_monthly", 0, 61 * day), 2);
  assert.strictEqual(lib.playPaymentsSoFar("rotitrack_pro_yearly", 0, 366 * day), 1);
  // Paying through Google Play uses up the first-subscription referral price.
  assert.ok(!lib.referralPriceEligible({ playPlanId: "rotitrack_pro_monthly" }, "rotitrack_pro_yearly", "friend", "me"));
});

test("monthly bonus: the highest level reached in the month, paid once", () => {
  assert.strictEqual(lib.referralBonusFor(19), 0);
  assert.strictEqual(lib.referralBonusFor(20), 2000);
  assert.strictEqual(lib.referralBonusFor(29), 2000);
  assert.strictEqual(lib.referralBonusFor(30), 3500);
  assert.strictEqual(lib.referralBonusFor(40), 5000);
  assert.strictEqual(lib.referralBonusFor(50), 7000);
  assert.strictEqual(lib.referralBonusFor(75), 8500);
  assert.strictEqual(lib.referralBonusFor(100), 10000);
  assert.strictEqual(lib.referralBonusFor(250), 10000);
  // 20 sales × ₹300 + ₹2,000 = ₹8,000, as in the published table.
  assert.strictEqual(20 * lib.REFERRAL_REWARD + lib.referralBonusFor(20), 8000);
  assert.strictEqual(100 * lib.REFERRAL_REWARD + lib.referralBonusFor(100), 40000);

  // Reaching 20 pays ₹2,000; reaching 30 pays only the extra ₹1,500.
  let r = { referralMonths: { "2026-11": { yearly: 19, bonus: 0 } } };
  assert.deepStrictEqual(lib.addYearlySale(r, "2026-11"), { month: "2026-11", yearly: 20, bonus: 2000, bonusNow: 2000 });
  r = { referralMonths: { "2026-11": { yearly: 20, bonus: 2000 } } };
  assert.strictEqual(lib.addYearlySale(r, "2026-11").bonusNow, 0);
  r = { referralMonths: { "2026-11": { yearly: 29, bonus: 2000 } } };
  assert.strictEqual(lib.addYearlySale(r, "2026-11").bonusNow, 1500);
  // A new month starts again from zero.
  assert.deepStrictEqual(lib.addYearlySale(r, "2026-12"), { month: "2026-12", yearly: 1, bonus: 0, bonusNow: 0 });
  // Months follow India time: 30 Nov 19:00 UTC is already 1 Dec in India.
  assert.strictEqual(lib.istMonthKey(Date.UTC(2026, 10, 30, 19, 0)), "2026-12");
  assert.strictEqual(lib.istMonthKey(Date.UTC(2026, 10, 30, 18, 0)), "2026-11");
});

test("a late Razorpay event never lowers the payment count", () => {
  const existing = { razorpaySubscriptionId: "sub_1", razorpayPaidCount: 2 };
  assert.strictEqual(lib.keepHighestPaidCount(existing, { razorpaySubscriptionId: "sub_1", razorpayPaidCount: 1 }).razorpayPaidCount, 2);
  assert.strictEqual(lib.keepHighestPaidCount(existing, { razorpaySubscriptionId: "sub_2", razorpayPaidCount: 1 }).razorpayPaidCount, 1);
});
