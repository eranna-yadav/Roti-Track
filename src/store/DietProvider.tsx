import AsyncStorage from '@react-native-async-storage/async-storage';
import React, {
  createContext, useCallback, useContext, useEffect, useMemo, useRef, useState,
} from 'react';
import { builtinFoodById, FOODS } from '@/data/foods';
import { SLOTS } from '@/data/mealPlans';
import { dayKey } from '@/lib/date';
import type { DietProfile, DietState, Food, MealEntry, MealSlot } from '@/lib/diet-types';
import { macroTargets, recommendedCalories } from '@/lib/nutrition';
import { planMeal, type PlannedMeal } from '@/lib/planner';
import { useHydration } from './HydrationProvider';

const KEY = 'sipwell:diet:v1';

const defaultProfile: DietProfile = {
  configured: false,
  heightCm: 160,
  age: 30,
  activity: 'light',
  goal: 'maintain',
  pref: 'veg',
  region: 'all',
  calorieGoal: 1800,
  calorieGoalIsCustom: false,
};

const emptyState: DietState = { profile: defaultProfile, meals: [], customFoods: [], planShuffles: {} };

export type Totals = { kcal: number; protein: number; carbs: number; fat: number };
const ZERO: Totals = { kcal: 0, protein: 0, carbs: 0, fat: 0 };

type Ctx = {
  ready: boolean;
  profile: DietProfile;
  meals: MealEntry[];
  customFoods: Food[];
  todayKey: string;
  macros: { protein: number; carbs: number; fat: number };

  foodById: (id: string) => Food | undefined;
  allFoods: Food[];
  recentFoods: Food[];

  mealsForDay: (day: string) => MealEntry[];
  totalsForDay: (day: string) => Totals;
  planForDay: (day: string) => PlannedMeal[];

  logFood: (food: Food, servings: number, slot: MealSlot, day: string, planKey?: string) => void;
  logPlannedMeal: (day: string, meal: PlannedMeal) => void;
  removeMeal: (id: string) => void;
  swapPlannedMeal: (day: string, slot: MealSlot) => void;
  addCustomFood: (food: Omit<Food, 'id' | 'category' | 'unit' | 'emoji'> & { emoji?: string }) => Food;
  removeCustomFood: (id: string) => void;
  updateProfile: (patch: Partial<DietProfile>) => void;
  resetDiet: () => Promise<void>;
};

const DietContext = createContext<Ctx | null>(null);

const uid = () => `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;

function makeEntry(food: Food, servings: number, slot: MealSlot, day: string, planKey?: string): MealEntry {
  return {
    id: uid(),
    day,
    ts: Date.now(),
    slot,
    foodId: food.id,
    name: food.name,
    emoji: food.emoji,
    serving: food.serving,
    servings,
    kcal: Math.round(food.kcal * servings),
    protein: Math.round(food.protein * servings * 10) / 10,
    carbs: Math.round(food.carbs * servings * 10) / 10,
    fat: Math.round(food.fat * servings * 10) / 10,
    planKey,
  };
}

export function DietProvider({ children }: { children: React.ReactNode }) {
  const { settings } = useHydration();
  const [state, setState] = useState<DietState>(emptyState);
  const [ready, setReady] = useState(false);
  const loaded = useRef(false);

  useEffect(() => {
    let alive = true;
    AsyncStorage.getItem(KEY)
      .then((raw) => {
        if (!alive || !raw) return;
        const parsed = JSON.parse(raw) as Partial<DietState>;
        setState({
          profile: { ...defaultProfile, ...(parsed.profile ?? {}) },
          meals: Array.isArray(parsed.meals) ? parsed.meals : [],
          customFoods: Array.isArray(parsed.customFoods) ? parsed.customFoods : [],
          planShuffles: parsed.planShuffles ?? {},
        });
      })
      .catch(() => {})
      .finally(() => {
        if (!alive) return;
        loaded.current = true;
        setReady(true);
      });
    return () => {
      alive = false;
    };
  }, []);

  useEffect(() => {
    if (loaded.current) void AsyncStorage.setItem(KEY, JSON.stringify(state)).catch(() => {});
  }, [state]);

  // Weight and gender live in the hydration settings; keep the calorie target
  // following them (and the profile) until the user sets it by hand.
  const { weightKg, gender } = settings;
  const { heightCm, age, activity, goal, calorieGoalIsCustom } = state.profile;
  useEffect(() => {
    if (!ready || calorieGoalIsCustom) return;
    const next = recommendedCalories(weightKg, gender, { heightCm, age, activity, goal });
    setState((prev) =>
      prev.profile.calorieGoal === next ? prev : { ...prev, profile: { ...prev.profile, calorieGoal: next } }
    );
  }, [ready, weightKg, gender, heightCm, age, activity, goal, calorieGoalIsCustom]);

  const customById = useMemo(() => new Map(state.customFoods.map((f) => [f.id, f])), [state.customFoods]);
  const foodById = useCallback((id: string) => builtinFoodById(id) ?? customById.get(id), [customById]);

  const allFoods = useMemo(() => [...state.customFoods, ...FOODS], [state.customFoods]);

  const recentFoods = useMemo(() => {
    const seen = new Set<string>();
    const out: Food[] = [];
    for (let i = state.meals.length - 1; i >= 0 && out.length < 8; i--) {
      const id = state.meals[i].foodId;
      if (seen.has(id)) continue;
      seen.add(id);
      const f = foodById(id);
      if (f) out.push(f);
    }
    return out;
  }, [state.meals, foodById]);

  const todayKey = dayKey(Date.now(), settings.dayStartsAt);

  const mealsForDay = useCallback(
    (day: string) => state.meals.filter((m) => m.day === day).sort((a, b) => a.ts - b.ts),
    [state.meals]
  );

  const totalsForDay = useCallback(
    (day: string) =>
      state.meals.reduce<Totals>(
        (t, m) =>
          m.day === day
            ? { kcal: t.kcal + m.kcal, protein: t.protein + m.protein, carbs: t.carbs + m.carbs, fat: t.fat + m.fat }
            : t,
        ZERO
      ),
    [state.meals]
  );

  const { calorieGoal, pref, region } = state.profile;
  const planForDay = useCallback(
    (day: string) =>
      SLOTS.map((s) =>
        planMeal(day, s.key, state.planShuffles[`${day}|${s.key}`] ?? 0, calorieGoal, pref, region, foodById)
      ),
    [state.planShuffles, calorieGoal, pref, region, foodById]
  );

  const logFood = useCallback<Ctx['logFood']>((food, servings, slot, day, planKey) => {
    const entry = makeEntry(food, servings, slot, day, planKey);
    setState((prev) => ({ ...prev, meals: [...prev.meals, entry] }));
  }, []);

  const logPlannedMeal = useCallback<Ctx['logPlannedMeal']>((day, meal) => {
    const planKey = `${day}|${meal.slot}`;
    const entries = meal.items.map((i) => makeEntry(i.food, i.servings, meal.slot, day, planKey));
    setState((prev) => ({ ...prev, meals: [...prev.meals, ...entries] }));
  }, []);

  const removeMeal = useCallback((id: string) => {
    setState((prev) => ({ ...prev, meals: prev.meals.filter((m) => m.id !== id) }));
  }, []);

  const swapPlannedMeal = useCallback((day: string, slot: MealSlot) => {
    const k = `${day}|${slot}`;
    setState((prev) => ({ ...prev, planShuffles: { ...prev.planShuffles, [k]: (prev.planShuffles[k] ?? 0) + 1 } }));
  }, []);

  const addCustomFood = useCallback<Ctx['addCustomFood']>((input) => {
    const food: Food = { ...input, id: `custom-${uid()}`, emoji: input.emoji || '🍽️', category: 'custom', unit: 'portion' };
    setState((prev) => ({ ...prev, customFoods: [food, ...prev.customFoods] }));
    return food;
  }, []);

  const removeCustomFood = useCallback((id: string) => {
    setState((prev) => ({ ...prev, customFoods: prev.customFoods.filter((f) => f.id !== id) }));
  }, []);

  const updateProfile = useCallback((patch: Partial<DietProfile>) => {
    setState((prev) => ({ ...prev, profile: { ...prev.profile, ...patch } }));
  }, []);

  const resetDiet = useCallback(async () => {
    await AsyncStorage.removeItem(KEY).catch(() => {});
    setState(emptyState);
  }, []);

  const macros = useMemo(
    () => macroTargets(state.profile.calorieGoal, weightKg, state.profile.goal),
    [state.profile.calorieGoal, state.profile.goal, weightKg]
  );

  const value = useMemo<Ctx>(
    () => ({
      ready,
      profile: state.profile,
      meals: state.meals,
      customFoods: state.customFoods,
      todayKey,
      macros,
      foodById,
      allFoods,
      recentFoods,
      mealsForDay,
      totalsForDay,
      planForDay,
      logFood,
      logPlannedMeal,
      removeMeal,
      swapPlannedMeal,
      addCustomFood,
      removeCustomFood,
      updateProfile,
      resetDiet,
    }),
    [ready, state.profile, state.meals, state.customFoods, todayKey, macros, foodById, allFoods, recentFoods,
     mealsForDay, totalsForDay, planForDay, logFood, logPlannedMeal, removeMeal, swapPlannedMeal,
     addCustomFood, removeCustomFood, updateProfile, resetDiet]
  );

  return <DietContext.Provider value={value}>{children}</DietContext.Provider>;
}

export function useDiet(): Ctx {
  const ctx = useContext(DietContext);
  if (!ctx) throw new Error('useDiet must be used inside <DietProvider>');
  return ctx;
}
