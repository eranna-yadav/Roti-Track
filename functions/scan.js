/**
 * Food photo scanning: the prompt and schema sent to Claude, and checks on what comes back.
 * Kept free of Firebase so it can be unit-tested.
 */
const FOODS = require("./foods.json");

const FOOD_IDS = new Set(FOODS.map((f) => f.id));

/** Photos bigger than this (base64 characters, about 1.5 MB) are refused; the app sends ~200 KB. */
const MAX_IMAGE_CHARS = 2_000_000;

/** Pro users may scan this many photos a day, which caps what one account can cost. */
const DAILY_SCAN_LIMIT = 40;

const MEALS = ["breakfast", "lunch", "snack", "dinner"];

/** Stable across requests, so it is cached: the food catalog makes up most of it. */
const SYSTEM_PROMPT = [
  "You identify Indian food in a photo for a calorie-tracking app.",
  "List each distinct dish or item you can see. For each one:",
  "- If it matches a food in the catalog below, set food_id to that id and use the catalog's serving; estimate servings from the portion in the photo (e.g. 3 rotis = 3 servings of \"1 roti\"; half a bowl = 0.5).",
  "- If nothing in the catalog matches, set food_id to \"\" and give a short common name, a serving description, and your best per-serving estimate of kcal, protein, carbs and fat in grams.",
  "- For catalog matches, still fill kcal/protein/carbs/fat with your per-serving estimate; the app uses its own catalog values.",
  "- servings must be greater than 0. Round it to the nearest 0.5.",
  "- confidence is how sure you are the item is what you named.",
  "If the photo shows no food, return an empty items list and explain in note. Keep note under 20 words.",
  "",
  "Catalog (id | name | serving | kcal):",
  ...FOODS.map((f) => `${f.id} | ${f.name} | ${f.serving} | ${f.kcal}`),
].join("\n");

/** What Claude must return (structured output). */
const RESULT_SCHEMA = {
  type: "object",
  properties: {
    items: {
      type: "array",
      items: {
        type: "object",
        properties: {
          food_id: { type: "string" },
          name: { type: "string" },
          serving: { type: "string" },
          servings: { type: "number" },
          kcal: { type: "number" },
          protein: { type: "number" },
          carbs: { type: "number" },
          fat: { type: "number" },
          confidence: { type: "string", enum: ["high", "medium", "low"] },
        },
        required: ["food_id", "name", "serving", "servings", "kcal", "protein", "carbs", "fat", "confidence"],
        additionalProperties: false,
      },
    },
    note: { type: "string" },
  },
  required: ["items", "note"],
  additionalProperties: false,
};

function userPrompt(meal) {
  const m = MEALS.includes(meal) ? meal : "meal";
  return `This is the user's ${m}. What food is in this photo, and how much?`;
}

/** Checks the photo the app sent; returns an error message, or null when it's fine. */
function checkImage(image) {
  if (typeof image !== "string" || image.length < 100) return "No photo received.";
  if (image.length > MAX_IMAGE_CHARS) return "The photo is too large.";
  if (!/^[A-Za-z0-9+/=]+$/.test(image.slice(0, 1000))) return "The photo isn't valid.";
  return null;
}

const clamp = (n, lo, hi) => Math.min(hi, Math.max(lo, Number.isFinite(n) ? n : lo));

/** Cleans Claude's answer: unknown catalog ids become custom items, numbers are kept in sane ranges. */
function sanitize(result) {
  const items = (Array.isArray(result && result.items) ? result.items : []).slice(0, 12).map((it) => {
    const id = FOOD_IDS.has(it.food_id) ? it.food_id : "";
    return {
      foodId: id,
      name: String(it.name || "Food").slice(0, 60),
      serving: String(it.serving || "1 serving").slice(0, 40),
      servings: Math.round(clamp(Number(it.servings), 0.5, 20) * 2) / 2,
      kcal: Math.round(clamp(Number(it.kcal), 0, 3000)),
      protein: Math.round(clamp(Number(it.protein), 0, 300) * 10) / 10,
      carbs: Math.round(clamp(Number(it.carbs), 0, 500) * 10) / 10,
      fat: Math.round(clamp(Number(it.fat), 0, 300) * 10) / 10,
      confidence: ["high", "medium", "low"].includes(it.confidence) ? it.confidence : "low",
    };
  });
  return { items, note: String((result && result.note) || "").slice(0, 200) };
}

/** Pro as the server sees it: a paid Razorpay period, a Play plan reported by the app, free Pro, or an admin. */
function isPro(user, now = Date.now()) {
  if (!user) return false;
  return (user.razorpayUntil || 0) > now || !!user.planId || user.compPro === true || user.role === "admin";
}

/** Today's key for the daily limit, in India time. */
function scanDay(now = Date.now()) {
  return new Date(now + 5.5 * 3_600_000).toISOString().slice(0, 10);
}

/**
 * Request options that depend on the model: effort and server-side refusal fallback exist on
 * the current Opus/Sonnet/Fable models; Haiku rejects both.
 */
function modelOptions(model) {
  if (/haiku/.test(model)) return {};
  const opts = { output_config_effort: "low" };
  if (/^claude-(opus-5|sonnet-5-5|fable-5)/.test(model)) {
    opts.betas = ["server-side-fallback-2026-07-01"];
    opts.fallbacks = "default";
  }
  return opts;
}

/** The full Messages API request for one photo. */
function buildRequest(model, image, meal) {
  const opts = modelOptions(model);
  const request = {
    model,
    max_tokens: 16000,
    output_config: { format: { type: "json_schema", schema: RESULT_SCHEMA } },
    system: [{ type: "text", text: SYSTEM_PROMPT, cache_control: { type: "ephemeral" } }],
    messages: [
      {
        role: "user",
        content: [
          { type: "image", source: { type: "base64", media_type: "image/jpeg", data: image } },
          { type: "text", text: userPrompt(meal) },
        ],
      },
    ],
  };
  if (opts.output_config_effort) request.output_config.effort = opts.output_config_effort;
  if (opts.betas) request.betas = opts.betas;
  if (opts.fallbacks) request.fallbacks = opts.fallbacks;
  return request;
}

module.exports = { SYSTEM_PROMPT, RESULT_SCHEMA, buildRequest, RESULT_SCHEMA, DAILY_SCAN_LIMIT, userPrompt, checkImage, sanitize, isPro, scanDay };
