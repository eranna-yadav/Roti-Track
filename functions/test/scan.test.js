const test = require("node:test");
const assert = require("node:assert");
const scan = require("../scan");

test("the prompt lists every catalog food", () => {
  const foods = require("../foods.json");
  assert.ok(foods.length > 100);
  for (const f of foods) assert.ok(scan.SYSTEM_PROMPT.includes(`${f.id} | ${f.name}`), f.id);
});

test("answers are cleaned: unknown ids become custom items, numbers stay sane", () => {
  const out = scan.sanitize({
    items: [
      { food_id: "roti", name: "Roti", serving: "1 roti", servings: 2.7, kcal: 120, protein: 3.14, carbs: 18, fat: 3, confidence: "high" },
      { food_id: "made-up", name: "Aloo tikki", serving: "1 piece", servings: -1, kcal: 99999, protein: 2, carbs: 20, fat: 8, confidence: "weird" },
    ],
    note: "ok",
  });
  assert.deepStrictEqual(out.items[0], {
    foodId: "roti", name: "Roti", serving: "1 roti", servings: 2.5, kcal: 120, protein: 3.1, carbs: 18, fat: 3, confidence: "high",
  });
  assert.strictEqual(out.items[1].foodId, "");
  assert.strictEqual(out.items[1].servings, 0.5);
  assert.strictEqual(out.items[1].kcal, 3000);
  assert.strictEqual(out.items[1].confidence, "low");
  assert.deepStrictEqual(scan.sanitize(null), { items: [], note: "" });
});

test("only Pro accounts may scan", () => {
  const now = 1_000_000;
  assert.ok(!scan.isPro(undefined, now));
  assert.ok(!scan.isPro({ razorpayUntil: now - 1 }, now));
  assert.ok(scan.isPro({ razorpayUntil: now + 1 }, now));
  assert.ok(scan.isPro({ planId: "rotitrack_pro_monthly" }, now));
  assert.ok(scan.isPro({ compPro: true }, now));
  assert.ok(scan.isPro({ role: "admin" }, now));
});

test("photos are checked before they cost anything", () => {
  assert.ok(scan.checkImage(undefined));
  assert.ok(scan.checkImage("abc"));
  assert.ok(scan.checkImage("<".repeat(500)));
  assert.ok(scan.checkImage("A".repeat(2_000_001)));
  assert.strictEqual(scan.checkImage("A".repeat(5000)), null);
});

test("the request matches the model", () => {
  const opus = scan.buildRequest("claude-opus-5-5", "AAAA", "lunch");
  assert.strictEqual(opus.output_config.effort, "low");
  assert.strictEqual(opus.fallbacks, "default");
  assert.deepStrictEqual(opus.betas, ["server-side-fallback-2026-07-01"]);
  assert.strictEqual(opus.output_config.format.type, "json_schema");
  assert.strictEqual(opus.messages[0].content[0].source.data, "AAAA");
  assert.match(opus.messages[0].content[1].text, /lunch/);
  const haiku = scan.buildRequest("claude-haiku-4-5", "AAAA", "dinner");
  assert.strictEqual(haiku.output_config.effort, undefined);
  assert.strictEqual(haiku.fallbacks, undefined);
  assert.strictEqual(haiku.betas, undefined);
});

test("the daily limit resets at midnight India time", () => {
  assert.strictEqual(scan.scanDay(Date.UTC(2026, 8, 29, 18, 29)), "2026-09-29");
  assert.strictEqual(scan.scanDay(Date.UTC(2026, 8, 29, 18, 31)), "2026-09-30");
});

test("Gemini is used when its key is set, else Claude, else nothing", () => {
  assert.strictEqual(scan.pickProvider("g-key", "c-key"), "gemini");
  assert.strictEqual(scan.pickProvider("not-set", "c-key"), "claude");
  assert.strictEqual(scan.pickProvider("", "c-key"), "claude");
  assert.strictEqual(scan.pickProvider("not-set", "not-set"), null);
  assert.strictEqual(scan.pickProvider(undefined, undefined), null);
});

test("the Gemini request carries the photo, the meal and the JSON schema", () => {
  const { url, body } = scan.buildGeminiRequest("gemini-flash-latest", "AAAA", "dinner");
  assert.strictEqual(url, "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent");
  assert.strictEqual(body.contents[0].parts[0].inlineData.data, "AAAA");
  assert.strictEqual(body.contents[0].parts[0].inlineData.mimeType, "image/jpeg");
  assert.match(body.contents[0].parts[1].text, /dinner/);
  assert.strictEqual(body.systemInstruction.parts[0].text, scan.SYSTEM_PROMPT);
  assert.strictEqual(body.generationConfig.responseMimeType, "application/json");
  assert.deepStrictEqual(
    body.generationConfig.responseSchema.properties.items.items.required,
    scan.RESULT_SCHEMA.properties.items.items.required,
  );
});

test("Gemini answers are read from the first candidate", () => {
  const answer = { items: [{ food_id: "idli", name: "Idli", serving: "1 piece", servings: 3, kcal: 58, protein: 2, carbs: 12, fat: 0.2, confidence: "high" }], note: "" };
  const text = JSON.stringify(answer);
  const response = { candidates: [{ content: { parts: [{ text: text.slice(0, 20) }, { text: text.slice(20) }] }, finishReason: "STOP" }] };
  assert.deepStrictEqual(scan.geminiAnswer(response), answer);
  assert.strictEqual(scan.geminiAnswer({ promptFeedback: { blockReason: "SAFETY" } }), null);
  assert.strictEqual(scan.geminiAnswer({ candidates: [{ content: { parts: [{ text: "{not json" }] } }] }), null);
  assert.strictEqual(scan.geminiAnswer(null), null);
});
