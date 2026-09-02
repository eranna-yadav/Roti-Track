import { recommendedGoalMl } from '@/lib/goal';
import type { Settings } from '@/lib/types';

export const DEFAULT_CUSTOM_TIMES = [
  '06:30', '08:00', '09:30', '11:00', '12:30', '14:00',
  '15:30', '17:00', '18:30', '20:00', '21:30', '23:00',
];

export const defaultSettings: Settings = {
  onboarded: false,
  pro: false,

  gender: 'female',
  weightKg: 55,
  wakeTime: '07:00',
  sleepTime: '22:30',

  goalMl: recommendedGoalMl(55, 'female'),
  goalIsCustom: false,
  defaultCupMl: 200,
  favouriteDrinkId: 'water',

  unit: 'metric',
  firstDayOfWeek: 0,
  dayStartsAt: '00:00',
  timeFormat: 'system',
  language: 'English',

  reminderEnabled: false,
  reminderMode: 'interval',
  intervalMinutes: 90,
  customTimes: DEFAULT_CUSTOM_TIMES,
  disabledCustomTimes: ['06:30', '23:00'],
  weekendMode: false,
  smartSkip: true,
  stopWhenGoalAchieved: true,

  soundEnabled: true,
  soundId: 'flow3',
  volume: 0.5,
  vibration: true,
};
