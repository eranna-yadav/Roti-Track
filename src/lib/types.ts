export type Gender = 'male' | 'female' | 'other';
export type UnitSystem = 'metric' | 'imperial';
export type ReminderMode = 'smart' | 'interval' | 'custom';
export type TimeFormat = 'system' | '12h' | '24h';

/** A single logged drink. Volumes are always stored in millilitres. */
export type Entry = {
  id: string;
  /** epoch ms */
  ts: number;
  /** raw volume drunk, in ml */
  amountMl: number;
  drinkId: string;
};

export type Drink = {
  id: string;
  name: string;
  emoji: string;
  /** 1 = pure water. Coffee/alcohol hydrate less than they add to the glass. */
  hydration: number;
  color: string;
  defaultMl: number;
  premium: boolean;
};

export type Settings = {
  onboarded: boolean;
  pro: boolean;

  gender: Gender;
  weightKg: number;
  wakeTime: string; // 'HH:mm'
  sleepTime: string; // 'HH:mm'

  /** Daily target in ml. Recomputed from body data unless the user overrides it. */
  goalMl: number;
  goalIsCustom: boolean;
  defaultCupMl: number;
  favouriteDrinkId: string;

  unit: UnitSystem;
  firstDayOfWeek: 0 | 1;
  dayStartsAt: string; // 'HH:mm'
  timeFormat: TimeFormat;
  language: string;

  reminderEnabled: boolean;
  reminderMode: ReminderMode;
  intervalMinutes: number;
  customTimes: string[]; // 'HH:mm', sorted
  disabledCustomTimes: string[]; // subset of customTimes that are muted
  weekendMode: boolean;
  smartSkip: boolean;
  stopWhenGoalAchieved: boolean;

  soundEnabled: boolean;
  soundId: string;
  volume: number; // 0..1
  vibration: boolean;
};

export type AppState = {
  settings: Settings;
  entries: Entry[];
};
