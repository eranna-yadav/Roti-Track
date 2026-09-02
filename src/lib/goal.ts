import type { Gender } from './types';

/**
 * Rough intake target: ~35 ml per kg of body weight, nudged by gender, then
 * rounded to the nearest 10 ml so the number reads cleanly.
 */
export function recommendedGoalMl(weightKg: number, gender: Gender): number {
  const perKg = gender === 'male' ? 36 : gender === 'female' ? 33 : 34.5;
  const raw = weightKg * perKg;
  return Math.round(Math.min(5000, Math.max(800, raw)) / 10) * 10;
}
