import AsyncStorage from '@react-native-async-storage/async-storage';
import type { AppState, Entry, Settings } from '@/lib/types';
import { defaultSettings } from './defaults';

const KEY = 'sipwell:state:v1';

export async function loadState(): Promise<AppState> {
  try {
    const raw = await AsyncStorage.getItem(KEY);
    if (!raw) return { settings: defaultSettings, entries: [] };
    const parsed = JSON.parse(raw) as Partial<AppState>;
    return {
      // merge so settings added in a later version still get a value
      settings: { ...defaultSettings, ...(parsed.settings ?? {}) } as Settings,
      entries: Array.isArray(parsed.entries) ? (parsed.entries as Entry[]) : [],
    };
  } catch {
    return { settings: defaultSettings, entries: [] };
  }
}

export async function saveState(state: AppState): Promise<void> {
  try {
    await AsyncStorage.setItem(KEY, JSON.stringify(state));
  } catch {
    // storage is best-effort; a failed write shouldn't break the session
  }
}

export async function clearState(): Promise<void> {
  try {
    await AsyncStorage.removeItem(KEY);
  } catch {
    /* ignore */
  }
}
