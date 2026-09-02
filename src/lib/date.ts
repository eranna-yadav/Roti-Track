import { hmToMinutes, pad2 } from './time';
import type { Entry, Settings } from './types';

export const DAY_MS = 86_400_000;

/** Key for the logical day an instant belongs to, honouring "A Day Starts At". */
export function dayKey(ts: number, dayStartsAt = '00:00'): string {
  const shifted = new Date(ts - hmToMinutes(dayStartsAt) * 60_000);
  return `${shifted.getFullYear()}-${pad2(shifted.getMonth() + 1)}-${pad2(shifted.getDate())}`;
}

export const dateToKey = (d: Date) => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;

export function keyToDate(key: string): Date {
  const [y, m, d] = key.split('-').map(Number);
  return new Date(y, m - 1, d);
}

export const startOfDay = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate());

export function addDays(d: Date, n: number): Date {
  const c = new Date(d);
  c.setDate(c.getDate() + n);
  return c;
}

export function startOfWeek(d: Date, firstDay: 0 | 1): Date {
  const s = startOfDay(d);
  const diff = (s.getDay() - firstDay + 7) % 7;
  return addDays(s, -diff);
}

export const startOfMonth = (d: Date) => new Date(d.getFullYear(), d.getMonth(), 1);
export const daysInMonth = (d: Date) => new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate();

export const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sept', 'Oct', 'Nov', 'Dec'];
export const WEEKDAY_LETTERS = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

export const formatDayLabel = (d: Date) => `${MONTHS[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`;
export const formatMonthLabel = (d: Date) => `${MONTHS[d.getMonth()]} ${d.getFullYear()}`;

export function formatWeekLabel(start: Date): string {
  const end = addDays(start, 6);
  const sameMonth = start.getMonth() === end.getMonth();
  const left = `${MONTHS[start.getMonth()]} ${start.getDate()}`;
  const right = sameMonth ? `${end.getDate()}` : `${MONTHS[end.getMonth()]} ${end.getDate()}`;
  return `${left} - ${right},${end.getFullYear()}`;
}

/** Effective (hydration-weighted) ml drunk on a given logical day. */
export function totalForDay(entries: Entry[], key: string, dayStartsAt: string, weighted: (e: Entry) => number): number {
  return entries.reduce((sum, e) => (dayKey(e.ts, dayStartsAt) === key ? sum + weighted(e) : sum), 0);
}

/** Consecutive logical days ending today that have at least one entry. */
export function computeStreak(entries: Entry[], settings: Pick<Settings, 'dayStartsAt'>): number {
  if (entries.length === 0) return 0;
  const keys = new Set(entries.map((e) => dayKey(e.ts, settings.dayStartsAt)));
  const today = new Date(Date.now() - hmToMinutes(settings.dayStartsAt) * 60_000);
  let streak = 0;
  for (let i = 0; i < 3650; i++) {
    if (!keys.has(dateToKey(addDays(today, -i)))) {
      // today not logged yet shouldn't break a streak that ran through yesterday
      if (i === 0) continue;
      break;
    }
    streak++;
  }
  return streak;
}
