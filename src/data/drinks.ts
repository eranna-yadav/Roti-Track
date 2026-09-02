import type { Drink } from '@/lib/types';

/** Free tier gets water + the basics; the rest sit behind Sipwell Pro. */
export const DRINKS: Drink[] = [
  { id: 'water', name: 'Water', emoji: '💧', hydration: 1.0, color: '#3FC1FF', defaultMl: 200, premium: false },
  { id: 'sparkling', name: 'Sparkling Water', emoji: '🫧', hydration: 1.0, color: '#67D3FF', defaultMl: 250, premium: false },
  { id: 'tea', name: 'Tea', emoji: '🍵', hydration: 0.95, color: '#7BC47F', defaultMl: 240, premium: false },
  { id: 'coffee', name: 'Coffee', emoji: '☕', hydration: 0.8, color: '#A9744F', defaultMl: 150, premium: false },

  { id: 'milk', name: 'Milk', emoji: '🥛', hydration: 0.9, color: '#E8EEF7', defaultMl: 250, premium: true },
  { id: 'juice', name: 'Juice', emoji: '🧃', hydration: 0.85, color: '#FFB020', defaultMl: 250, premium: true },
  { id: 'smoothie', name: 'Smoothie', emoji: '🥤', hydration: 0.8, color: '#FF7BAC', defaultMl: 300, premium: true },
  { id: 'coconut', name: 'Coconut Water', emoji: '🥥', hydration: 0.95, color: '#C8B79A', defaultMl: 250, premium: true },
  { id: 'lemonade', name: 'Lemonade', emoji: '🍋', hydration: 0.85, color: '#FFD84D', defaultMl: 250, premium: true },
  { id: 'sports', name: 'Sports Drink', emoji: '🏃', hydration: 0.92, color: '#4ADE80', defaultMl: 330, premium: true },
  { id: 'soda', name: 'Soda', emoji: '🥤', hydration: 0.7, color: '#8B5CF6', defaultMl: 330, premium: true },
  { id: 'energy', name: 'Energy Drink', emoji: '⚡', hydration: 0.6, color: '#F97316', defaultMl: 250, premium: true },
  { id: 'protein', name: 'Protein Shake', emoji: '💪', hydration: 0.8, color: '#94A3B8', defaultMl: 300, premium: true },
  { id: 'hotchoc', name: 'Hot Chocolate', emoji: '🍫', hydration: 0.8, color: '#7B4B2A', defaultMl: 240, premium: true },
  { id: 'beer', name: 'Beer', emoji: '🍺', hydration: 0.4, color: '#F5B301', defaultMl: 330, premium: true },
  { id: 'wine', name: 'Wine', emoji: '🍷', hydration: 0.2, color: '#9F1239', defaultMl: 150, premium: true },
  { id: 'spirits', name: 'Spirits', emoji: '🥃', hydration: -0.2, color: '#C2833B', defaultMl: 40, premium: true },
];

export const drinkById = (id: string): Drink => DRINKS.find((d) => d.id === id) ?? DRINKS[0];

/** Cup presets offered in the drink sheet, in ml. */
export const CUP_PRESETS_ML = [100, 150, 200, 250, 300, 350, 500, 750, 1000];
