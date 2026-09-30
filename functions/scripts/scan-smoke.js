/**
 * Live check after deploy: sends one photo to Gemini with the real key and prints what it saw.
 * Usage: GEMINI_API_KEY=... node scripts/scan-smoke.js photo.jpg [model]
 * Proves the key works and Gemini accepts our request; prints no secrets.
 */
const fs = require("fs");
const scan = require("../scan");

(async () => {
  const [photo, model = "gemini-flash-latest"] = process.argv.slice(2);
  const image = fs.readFileSync(photo).toString("base64");
  const { url, body } = scan.buildGeminiRequest(model, image, "lunch");
  const res = await fetch(url, {
    method: "POST",
    headers: { "content-type": "application/json", "x-goog-api-key": process.env.GEMINI_API_KEY },
    body: JSON.stringify(body),
  });
  const json = await res.json().catch(() => null);
  if (!res.ok) {
    const e = (json && json.error) || {};
    console.log(`::error::Gemini answered HTTP ${res.status}: ${e.status || ""} ${e.message || ""}`.trim());
    process.exit(1);
  }
  const answer = scan.geminiAnswer(json);
  if (!answer) {
    const c = json.candidates && json.candidates[0];
    console.log(`::error::Gemini gave no readable answer (finish: ${c && c.finishReason})`);
    process.exit(1);
  }
  const result = scan.sanitize(answer);
  console.log(`Gemini (${model}) works. It saw ${result.items.length} item(s):`);
  for (const it of result.items) {
    console.log(`  - ${it.name} ${it.foodId ? `[${it.foodId}]` : "[not in catalog]"}: ${it.servings} × ${it.serving}, ${it.kcal} kcal each (${it.confidence})`);
  }
  if (result.note) console.log(`  note: ${result.note}`);
})().catch((e) => {
  console.log(`::error::Scanner check failed: ${e.message}`);
  process.exit(1);
});
