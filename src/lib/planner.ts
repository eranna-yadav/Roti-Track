import { SLOTS, TEMPLATES, type MealTemplate } from '@/data/mealPlans';
import type { DietPref, Food, MealSlot, Region } from './diet-types';

export type PlannedItem = { food: Food; servings: number };
export type PlannedMeal = {
  slot: MealSlot;
  name: string;
  target: number;
  items: PlannedItem[];
  kcal: number;
  protein: number;
  carbs: number;
  fat: number;
};

const DIET_RANK = { veg: 0, egg: 1, nonveg: 2 } as const;

/** Deterministic 32-bit hash so a day's plan is stable across launches. */
function hash(s: string): number {
  let h = 2166136261;
  for (let i = 0; i < s.length; i++) {
    h ^= s.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return h >>> 0;
}

function templateRank(t: MealTemplate, foodById: (id: string) => Food | undefined): number {
  return t.items.reduce((max, [id]) => Math.max(max, DIET_RANK[foodById(id)?.diet ?? 'veg']), 0);
}

function candidates(
  slot: MealSlot,
  pref: DietPref,
  region: Region,
  foodById: (id: string) => Food | undefined
): MealTemplate[] {
  const allowed = TEMPLATES[slot].filter((t) => templateRank(t, foodById) <= DIET_RANK[pref]);
  if (region === 'all') return allowed;
  const local = allowed.filter((t) => t.region === region || t.region === 'any');
  return local.length >= 2 ? local : allowed;
}

const roundTo = (n: number, step: number) => Math.max(step, Math.round(n / step) * step);

/** Scales a template's servings so the meal lands near `target` kcal. */
function scale(t: MealTemplate, target: number, foodById: (id: string) => Food | undefined): PlannedItem[] {
  const resolved = t.items
    .map(([id, servings, fixed]) => ({ food: foodById(id), servings, fixed: !!fixed }))
    .filter((i): i is { food: Food; servings: number; fixed: boolean } => !!i.food);

  const fixedKcal = resolved.filter((i) => i.fixed).reduce((s, i) => s + i.food.kcal * i.servings, 0);
  const flexKcal = resolved.filter((i) => !i.fixed).reduce((s, i) => s + i.food.kcal * i.servings, 0);
  const factor = flexKcal > 0 ? Math.min(2, Math.max(0.5, (target - fixedKcal) / flexKcal)) : 1;

  return resolved.map(({ food, servings, fixed }) => ({
    food,
    servings: fixed ? servings : roundTo(servings * factor, food.unit === 'piece' ? 1 : 0.5),
  }));
}

export function planMeal(
  day: string,
  slot: MealSlot,
  shuffle: number,
  dailyKcal: number,
  pref: DietPref,
  region: Region,
  foodById: (id: string) => Food | undefined
): PlannedMeal {
  const meta = SLOTS.find((s) => s.key === slot)!;
  const target = Math.round(dailyKcal * meta.share);
  const pool = candidates(slot, pref, region, foodById);
  const template = pool[(hash(`${day}:${slot}`) + shuffle) % pool.length];
  const items = scale(template, target, foodById);
  const sum = (k: 'kcal' | 'protein' | 'carbs' | 'fat') =>
    Math.round(items.reduce((s, i) => s + i.food[k] * i.servings, 0));
  return {
    slot,
    name: template.name,
    target,
    items,
    kcal: sum('kcal'),
    protein: sum('protein'),
    carbs: sum('carbs'),
    fat: sum('fat'),
  };
}

export function formatServings(n: number): string {
  if (n === 0.5) return '½';
  if (Number.isInteger(n)) return String(n);
  if (Number.isInteger(n * 2)) return `${Math.floor(n)}½`;
  return n.toFixed(1);
}
