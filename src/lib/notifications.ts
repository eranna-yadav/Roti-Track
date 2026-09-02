import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import { hmToMinutes, minutesToHM, parseHM } from './time';
import type { Settings } from './types';

const MESSAGES = [
  'Time for a sip 💧',
  'Your body called — it wants water 🚰',
  'Hydration check! Grab a glass 💙',
  'A little water goes a long way ✨',
  'Stay sharp, stay hydrated 🌊',
];

export function configureNotificationHandler() {
  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldPlaySound: true,
      shouldSetBadge: false,
      shouldShowBanner: true,
      shouldShowList: true,
    }),
  });
}

export async function ensurePermission(): Promise<boolean> {
  if (Platform.OS === 'web') return false;
  const current = await Notifications.getPermissionsAsync();
  if (current.granted) return true;
  if (!current.canAskAgain) return false;
  const asked = await Notifications.requestPermissionsAsync();
  return asked.granted;
}

/** The times of day (HH:mm) reminders should fire for the current settings. */
export function reminderTimes(s: Settings): string[] {
  if (!s.reminderEnabled) return [];
  const wake = hmToMinutes(s.wakeTime);
  const sleep = hmToMinutes(s.sleepTime);
  const end = sleep > wake ? sleep : sleep + 1440;

  if (s.reminderMode === 'custom') {
    return s.customTimes.filter((t) => !s.disabledCustomTimes.includes(t)).sort();
  }

  const step =
    s.reminderMode === 'interval'
      ? Math.max(15, s.intervalMinutes)
      : // 'smart': spread ~8 nudges across the waking window
        Math.max(45, Math.round((end - wake) / 8 / 15) * 15);

  const out: string[] = [];
  for (let t = wake + step; t < end; t += step) out.push(minutesToHM(t));
  return out;
}

/**
 * Rebuilds the whole reminder schedule. Cheap enough to call after any change,
 * and keeps the OS in sync with what the Reminder screen shows.
 */
export async function rescheduleReminders(s: Settings): Promise<string[]> {
  const times = reminderTimes(s);
  if (Platform.OS === 'web') return times;

  await Notifications.cancelAllScheduledNotificationsAsync();
  if (times.length === 0) return times;
  if (!(await ensurePermission())) return times;

  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync('reminders', {
      name: 'Drink reminders',
      importance: Notifications.AndroidImportance.DEFAULT,
      lightColor: '#1B4FF0',
      vibrationPattern: s.vibration ? [0, 220, 120, 220] : [0],
    });
  }

  await Promise.all(
    times.map((hm, i) => {
      const { h, m } = parseHM(hm);
      return Notifications.scheduleNotificationAsync({
        content: {
          title: 'Sipwell',
          body: MESSAGES[i % MESSAGES.length],
          sound: s.soundEnabled,
        },
        trigger: {
          type: Notifications.SchedulableTriggerInputTypes.DAILY,
          hour: h,
          minute: m,
          channelId: 'reminders',
        },
      });
    })
  );
  return times;
}

export async function cancelAllReminders() {
  if (Platform.OS === 'web') return;
  await Notifications.cancelAllScheduledNotificationsAsync();
}

/** Next reminder after `now`, or null when reminders are off / done for today. */
export function nextReminder(s: Settings, now = new Date()): { hm: string; minutesAway: number } | null {
  const times = reminderTimes(s);
  if (times.length === 0) return null;
  const cur = now.getHours() * 60 + now.getMinutes();
  const isWeekend = now.getDay() === 0 || now.getDay() === 6;
  if (s.weekendMode && isWeekend) return null;

  for (const hm of times) {
    const t = hmToMinutes(hm);
    if (t > cur) return { hm, minutesAway: t - cur };
  }
  const first = hmToMinutes(times[0]);
  return { hm: times[0], minutesAway: 1440 - cur + first };
}
