import type { Settings, TimeFormat } from './types';

export const pad2 = (n: number) => String(n).padStart(2, '0');

export const toHM = (d: Date) => `${pad2(d.getHours())}:${pad2(d.getMinutes())}`;

export function parseHM(hm: string): { h: number; m: number } {
  const [h, m] = hm.split(':').map((n) => parseInt(n, 10));
  return { h: Number.isFinite(h) ? h : 0, m: Number.isFinite(m) ? m : 0 };
}

export const hmToMinutes = (hm: string) => {
  const { h, m } = parseHM(hm);
  return h * 60 + m;
};

export const minutesToHM = (mins: number) => {
  const m = ((mins % 1440) + 1440) % 1440;
  return `${pad2(Math.floor(m / 60))}:${pad2(m % 60)}`;
};

const uses24h = (fmt: TimeFormat) => {
  if (fmt === '24h') return true;
  if (fmt === '12h') return false;
  // 'system' — probe the current locale
  try {
    const s = new Intl.DateTimeFormat(undefined, { hour: 'numeric' }).format(new Date(2020, 0, 1, 13));
    return !/AM|PM/i.test(s);
  } catch {
    return false;
  }
};

/** "01:00 PM" or "13:00" depending on the user's Time Format setting. */
export function formatTime(hm: string, fmt: TimeFormat = 'system'): string {
  const { h, m } = parseHM(hm);
  if (uses24h(fmt)) return `${pad2(h)}:${pad2(m)}`;
  const suffix = h >= 12 ? 'PM' : 'AM';
  const h12 = h % 12 === 0 ? 12 : h % 12;
  return `${pad2(h12)}:${pad2(m)} ${suffix}`;
}

export const formatClock = (d: Date, fmt: TimeFormat = 'system') => formatTime(toHM(d), fmt);

/** "30 min left" / "2 h 05 min left" */
export function formatCountdown(mins: number): string {
  if (mins <= 0) return 'due now';
  if (mins < 60) return `${mins} min left`;
  const h = Math.floor(mins / 60);
  return `${h} h ${pad2(mins % 60)} min left`;
}

export const timeFormatLabel = (fmt: TimeFormat) =>
  fmt === 'system' ? 'Follow The System' : fmt === '12h' ? '12-hour' : '24-hour';

export const settingsTimeFormat = (s: Pick<Settings, 'timeFormat'>) => s.timeFormat;
