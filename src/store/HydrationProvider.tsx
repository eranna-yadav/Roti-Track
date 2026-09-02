import React, {
  createContext, useCallback, useContext, useEffect, useMemo, useRef, useState,
} from 'react';
import { dayKey, computeStreak } from '@/lib/date';
import { recommendedGoalMl } from '@/lib/goal';
import { rescheduleReminders } from '@/lib/notifications';
import { drinkById } from '@/data/drinks';
import type { AppState, Entry, Settings } from '@/lib/types';
import { defaultSettings } from './defaults';
import { clearState, loadState, saveState } from './storage';

type Ctx = {
  ready: boolean;
  settings: Settings;
  entries: Entry[];

  /** Hydration-weighted ml for the current logical day. */
  todayMl: number;
  /** 0..1, clamped. */
  progress: number;
  streak: number;
  /** Weighted ml an entry contributes (coffee counts less than water). */
  effectiveMl: (e: Entry) => number;

  addDrink: (amountMl: number, drinkId?: string, at?: Date) => Entry;
  updateEntry: (id: string, patch: Partial<Pick<Entry, 'amountMl' | 'ts' | 'drinkId'>>) => void;
  removeEntry: (id: string) => void;
  entriesForDay: (key: string) => Entry[];
  totalForDayKey: (key: string) => number;

  updateSettings: (patch: Partial<Settings>) => void;
  resetAll: () => Promise<void>;
};

const HydrationContext = createContext<Ctx | null>(null);

const uid = () => `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;

export function HydrationProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AppState>({ settings: defaultSettings, entries: [] });
  const [ready, setReady] = useState(false);
  const hydrated = useRef(false);

  useEffect(() => {
    let alive = true;
    loadState().then((loaded) => {
      if (!alive) return;
      setState(loaded);
      hydrated.current = true;
      setReady(true);
    });
    return () => {
      alive = false;
    };
  }, []);

  // Persist after every change, once the initial load has landed.
  useEffect(() => {
    if (hydrated.current) void saveState(state);
  }, [state]);

  // Keep the OS reminder schedule in step with the settings that shape it.
  const scheduleKey = JSON.stringify([
    state.settings.reminderEnabled,
    state.settings.reminderMode,
    state.settings.intervalMinutes,
    state.settings.customTimes,
    state.settings.disabledCustomTimes,
    state.settings.wakeTime,
    state.settings.sleepTime,
    state.settings.soundEnabled,
    state.settings.vibration,
  ]);
  useEffect(() => {
    if (!ready) return;
    void rescheduleReminders(state.settings);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ready, scheduleKey]);

  const effectiveMl = useCallback(
    (e: Entry) => Math.round(e.amountMl * drinkById(e.drinkId).hydration),
    []
  );

  const todayKey = dayKey(Date.now(), state.settings.dayStartsAt);

  const entriesForDay = useCallback(
    (key: string) =>
      state.entries
        .filter((e) => dayKey(e.ts, state.settings.dayStartsAt) === key)
        .sort((a, b) => a.ts - b.ts),
    [state.entries, state.settings.dayStartsAt]
  );

  const totalForDayKey = useCallback(
    (key: string) => entriesForDay(key).reduce((sum, e) => sum + effectiveMl(e), 0),
    [entriesForDay, effectiveMl]
  );

  const todayMl = useMemo(() => totalForDayKey(todayKey), [totalForDayKey, todayKey]);
  const progress = state.settings.goalMl > 0 ? Math.min(1, todayMl / state.settings.goalMl) : 0;
  const streak = useMemo(() => computeStreak(state.entries, state.settings), [state.entries, state.settings]);

  const addDrink = useCallback<Ctx['addDrink']>((amountMl, drinkId = 'water', at) => {
    const entry: Entry = { id: uid(), ts: (at ?? new Date()).getTime(), amountMl, drinkId };
    setState((prev) => ({ ...prev, entries: [...prev.entries, entry] }));
    return entry;
  }, []);

  const updateEntry = useCallback<Ctx['updateEntry']>((id, patch) => {
    setState((prev) => ({
      ...prev,
      entries: prev.entries.map((e) => (e.id === id ? { ...e, ...patch } : e)),
    }));
  }, []);

  const removeEntry = useCallback<Ctx['removeEntry']>((id) => {
    setState((prev) => ({ ...prev, entries: prev.entries.filter((e) => e.id !== id) }));
  }, []);

  const updateSettings = useCallback<Ctx['updateSettings']>((patch) => {
    setState((prev) => {
      const next = { ...prev.settings, ...patch };
      // Body data drives the goal until the user sets one by hand.
      const bodyChanged = patch.weightKg !== undefined || patch.gender !== undefined;
      if (bodyChanged && !next.goalIsCustom) {
        next.goalMl = recommendedGoalMl(next.weightKg, next.gender);
      }
      return { ...prev, settings: next };
    });
  }, []);

  const resetAll = useCallback(async () => {
    await clearState();
    setState({ settings: defaultSettings, entries: [] });
  }, []);

  const value = useMemo<Ctx>(
    () => ({
      ready,
      settings: state.settings,
      entries: state.entries,
      todayMl,
      progress,
      streak,
      effectiveMl,
      addDrink,
      updateEntry,
      removeEntry,
      entriesForDay,
      totalForDayKey,
      updateSettings,
      resetAll,
    }),
    [ready, state.settings, state.entries, todayMl, progress, streak, effectiveMl,
     addDrink, updateEntry, removeEntry, entriesForDay, totalForDayKey, updateSettings, resetAll]
  );

  return <HydrationContext.Provider value={value}>{children}</HydrationContext.Provider>;
}

export function useHydration(): Ctx {
  const ctx = useContext(HydrationContext);
  if (!ctx) throw new Error('useHydration must be used inside <HydrationProvider>');
  return ctx;
}
