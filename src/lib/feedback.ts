import { createAudioPlayer, setAudioModeAsync, type AudioPlayer } from 'expo-audio';
import * as Haptics from 'expo-haptics';
import { Platform } from 'react-native';
import { soundById } from '@/data/sounds';
import type { Settings } from './types';

let player: AudioPlayer | null = null;
let loadedId: string | null = null;
let audioModeReady = false;

async function ensureAudioMode() {
  if (audioModeReady || Platform.OS === 'web') return;
  try {
    await setAudioModeAsync({ playsInSilentMode: true, shouldPlayInBackground: false });
    audioModeReady = true;
  } catch {
    /* audio mode is a nicety, not a requirement */
  }
}

/** Plays the selected effect. `soundIdOverride` lets the picker preview a row. */
export async function playDrinkSound(
  s: Pick<Settings, 'soundEnabled' | 'soundId' | 'volume'>,
  soundIdOverride?: string
) {
  const id = soundIdOverride ?? s.soundId;
  if (!s.soundEnabled && !soundIdOverride) return;
  try {
    await ensureAudioMode();
    if (loadedId !== id || !player) {
      player?.remove();
      player = createAudioPlayer(soundById(id).module);
      loadedId = id;
    }
    player.volume = Math.max(0, Math.min(1, s.volume));
    player.seekTo(0);
    player.play();
  } catch {
    /* a missing audio device shouldn't break logging a drink */
  }
}

export function stopSound() {
  try {
    player?.pause();
  } catch {
    /* ignore */
  }
}

export function tapFeedback(s: Pick<Settings, 'vibration'>, style: 'light' | 'medium' | 'success' = 'light') {
  if (!s.vibration || Platform.OS === 'web') return;
  try {
    if (style === 'success') Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
    else
      Haptics.impactAsync(
        style === 'medium' ? Haptics.ImpactFeedbackStyle.Medium : Haptics.ImpactFeedbackStyle.Light
      );
  } catch {
    /* ignore */
  }
}
