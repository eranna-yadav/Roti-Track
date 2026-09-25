export type DietPref = 'veg' | 'egg' | 'nonveg';
export type Region = 'all' | 'north' | 'south' | 'west' | 'east';
export type ActivityLevel = 'sedentary' | 'light' | 'moderate' | 'active' | 'athlete';
export type WeightGoal = 'lose' | 'maintain' | 'gain';
export type MealSlot = 'breakfast' | 'lunch' | 'snack' | 'dinner';

export type FoodCategory =
  | 'breakfast' | 'breads' | 'rice' | 'dal' | 'sabzi' | 'nonveg'
  | 'dairy' | 'snacks' | 'sweets' | 'beverages' | 'fruits' | 'custom';

/** Nutrition is per one serving as described by `serving`. */
export type Food = {
  id: string;
  name: string;
  emoji: string;
  category: FoodCategory;
  serving: string;
  kcal: number;
  protein: number;
  carbs: number;
  fat: number;
  diet: 'veg' | 'egg' | 'nonveg';
  /** Pieces are counted whole; portions can be halved. */
  unit: 'piece' | 'portion';
};

/**
 * One logged food. Nutrition is snapshotted at log time so editing or deleting
 * a custom food never rewrites history.
 */
export type MealEntry = {
  id: string;
  /** logical day key, 'YYYY-MM-DD' */
  day: string;
  ts: number;
  slot: MealSlot;
  foodId: string;
  name: string;
  emoji: string;
  serving: string;
  servings: number;
  kcal: number;
  protein: number;
  carbs: number;
  fat: number;
  /** Set when the entry came from "Log this meal" on the plan. */
  planKey?: string;
};

export type DietProfile = {
  configured: boolean;
  heightCm: number;
  age: number;
  activity: ActivityLevel;
  goal: WeightGoal;
  pref: DietPref;
  region: Region;
  /** Daily calorie target. Follows the profile unless set by hand. */
  calorieGoal: number;
  calorieGoalIsCustom: boolean;
};

export type DietState = {
  profile: DietProfile;
  meals: MealEntry[];
  customFoods: Food[];
  /** `${day}|${slot}` → how many times the user tapped Swap. */
  planShuffles: Record<string, number>;
};
