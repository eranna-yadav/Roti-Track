import type { ActivityLevel, DietProfile, WeightGoal } from './diet-types';
import type { Gender } from './types';

export const ACTIVITY: { key: ActivityLevel; label: string; hint: string; factor: number }[] = [
  { key: 'sedentary', label: 'Sedentary', hint: 'Desk job, little exercise', factor: 1.2 },
  { key: 'light', label: 'Lightly active', hint: 'Walks, exercise 1–3 days a week', factor: 1.375 },
  { key: 'moderate', label: 'Moderately active', hint: 'Exercise 3–5 days a week', factor: 1.55 },
  { key: 'active', label: 'Very active', hint: 'Hard exercise 6–7 days a week', factor: 1.725 },
  { key: 'athlete', label: 'Athlete', hint: 'Physical job or twice-a-day training', factor: 1.9 },
];

export const GOALS: { key: WeightGoal; label: string; emoji: string; delta: number }[] = [
  { key: 'lose', label: 'Lose weight', emoji: '📉', delta: -500 },
  { key: 'maintain', label: 'Stay fit', emoji: '⚖️', delta: 0 },
  { key: 'gain', label: 'Gain weight', emoji: '💪', delta: 300 },
];

/** Mifflin–St Jeor resting energy, in kcal/day. */
export function bmr(weightKg: number, heightCm: number, age: number, gender: Gender): number {
  const base = 10 * weightKg + 6.25 * heightCm - 5 * age;
  return base + (gender === 'male' ? 5 : gender === 'female' ? -161 : -78);
}

export function tdee(weightKg: number, p: Pick<DietProfile, 'heightCm' | 'age' | 'activity'>, gender: Gender) {
  const factor = ACTIVITY.find((a) => a.key === p.activity)?.factor ?? 1.375;
  return bmr(weightKg, p.heightCm, p.age, gender) * factor;
}

/** Daily target: maintenance ± the goal delta, floored at a safe minimum, rounded to 10. */
export function recommendedCalories(
  weightKg: number,
  gender: Gender,
  p: Pick<DietProfile, 'heightCm' | 'age' | 'activity' | 'goal'>
): number {
  const delta = GOALS.find((g) => g.key === p.goal)?.delta ?? 0;
  const floor = gender === 'male' ? 1500 : 1200;
  const raw = Math.max(floor, tdee(weightKg, p, gender) + delta);
  return Math.round(Math.min(4500, raw) / 10) * 10;
}

/**
 * Macro targets in grams. Split follows ICMR-NIN's guidance for Indian diets
 * (carb-forward), nudged toward protein when losing weight. Protein never drops
 * below 0.8 g/kg.
 */
export function macroTargets(kcal: number, weightKg: number, goal: WeightGoal) {
  const split = goal === 'lose' ? { p: 0.25, f: 0.25 } : { p: 0.2, f: 0.25 };
  const protein = Math.max(Math.round(weightKg * 0.8), Math.round((kcal * split.p) / 4));
  const fat = Math.round((kcal * split.f) / 9);
  const carbs = Math.max(0, Math.round((kcal - protein * 4 - fat * 9) / 4));
  return { protein, carbs, fat };
}

export function bmi(weightKg: number, heightCm: number): number {
  const m = heightCm / 100;
  return m > 0 ? weightKg / (m * m) : 0;
}

/** Asian-Indian BMI cut-offs (lower than the WHO defaults). */
export function bmiLabel(value: number): { label: string; color: string } {
  if (value < 18.5) return { label: 'Underweight', color: '#3FA9F5' };
  if (value < 23) return { label: 'Healthy', color: '#22C55E' };
  if (value < 25) return { label: 'Overweight', color: '#FFB020' };
  return { label: 'Obese', color: '#EF4444' };
}

export const round1 = (n: number) => Math.round(n * 10) / 10;
