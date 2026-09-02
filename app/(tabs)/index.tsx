import { useRouter } from 'expo-router';
import React, { useEffect, useMemo, useState } from 'react';
import {
  Modal, Pressable, StyleSheet, Text, useWindowDimensions, View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { DrinkSheet } from '@/components/DrinkSheet';
import { SoundSheet } from '@/components/SoundSheet';
import { BellIcon, SpeakerIcon } from '@/components/TabIcons';
import { WaveFill } from '@/components/WaveFill';
import { CircleButton, PrimaryButton } from '@/components/ui';
import { drinkById } from '@/data/drinks';
import { dayKey } from '@/lib/date';
import { playDrinkSound, tapFeedback } from '@/lib/feedback';
import { nextReminder } from '@/lib/notifications';
import { formatCountdown, formatTime } from '@/lib/time';
import { formatVolume, formatVolumeShort, heroVolume, volumeUnit } from '@/lib/units';
import { useHydration } from '@/store/HydrationProvider';
import { colors, radius, shadow, type } from '@/theme';

export default function TodayScreen() {
  const { settings, todayMl, progress, streak, addDrink, entriesForDay } = useHydration();
  const insets = useSafeAreaInsets();
  const { height } = useWindowDimensions();
  const router = useRouter();

  const [sheetOpen, setSheetOpen] = useState(false);
  const [soundOpen, setSoundOpen] = useState(false);
  const [celebrate, setCelebrate] = useState<'first' | 'goal' | null>(null);

  const todayKey = dayKey(Date.now(), settings.dayStartsAt);
  const todayCount = entriesForDay(todayKey).length;

  // Recompute the countdown on a minute tick so it stays honest.
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 30_000);
    return () => clearInterval(t);
  }, []);

  const upcoming = useMemo(() => nextReminder(settings, now), [settings, now]);
  const pct = Math.round(progress * 100);

  // Keep a body of water on screen even at 0% — an empty glass reads as a bug,
  // and the DRINK button is designed to sit on blue.
  const waveRegion = height * 0.56;
  const waterLevel = 0.4 + 0.6 * progress;
  const waterLine = waveRegion * waterLevel;
  const favourite = drinkById(settings.favouriteDrinkId);

  const quickAdd = () => {
    const before = todayMl;
    addDrink(settings.defaultCupMl, settings.favouriteDrinkId);
    void playDrinkSound(settings);
    tapFeedback(settings, 'success');
    const after = before + Math.round(settings.defaultCupMl * favourite.hydration);
    if (todayCount === 0) setCelebrate('first');
    else if (before < settings.goalMl && after >= settings.goalMl) setCelebrate('goal');
  };

  return (
    <View style={styles.root}>
      <WaveFill progress={waterLevel} height={waveRegion} />

      <View style={[styles.percentTag, { bottom: waterLine - 14 }]} pointerEvents="none">
        <Text style={styles.percentText}>{pct}%</Text>
      </View>

      <View style={[styles.content, { paddingTop: insets.top + 8, paddingBottom: insets.bottom + 96 }]}>
        <View style={styles.header}>
          <Pressable
            style={styles.streakPill}
            onPress={() => router.push('/(tabs)/history')}
          >
            <Text style={styles.streakEmoji}>🔥</Text>
            <Text style={styles.streakText}>
              {streak}-day streak
            </Text>
          </Pressable>
          <View style={styles.headerActions}>
            <CircleButton tint="#FFFFFF" onPress={() => router.push('/reminders')}>
              <BellIcon color={colors.ink} />
            </CircleButton>
            <CircleButton tint="#FFFFFF" onPress={() => setSoundOpen(true)}>
              <SpeakerIcon color={colors.ink} />
            </CircleButton>
          </View>
        </View>

        <View style={styles.hero}>
          <View style={styles.heroRow}>
            <Text style={styles.heroValue} allowFontScaling={false}>
              {heroVolume(todayMl, settings.unit)}
            </Text>
            <Text style={styles.heroUnit}>{volumeUnit(settings.unit)}</Text>
          </View>

          <View style={styles.statRow}>
            <Pressable style={styles.statCard} onPress={() => router.push('/goal')}>
              <View style={styles.statLabelRow}>
                <View style={[styles.tick, { backgroundColor: colors.brand }]} />
                <Text style={styles.statLabel}>Target</Text>
              </View>
              <View style={styles.statValueRow}>
                <Text style={styles.statValue}>
                  {formatVolume(settings.goalMl, settings.unit)} ({pct}%)
                </Text>
                <Text style={styles.editGlyph}>✎</Text>
              </View>
            </Pressable>

            <Pressable style={styles.statCard} onPress={() => router.push('/reminders')}>
              <View style={styles.statLabelRow}>
                <View style={[styles.tick, { backgroundColor: '#FF8A3D' }]} />
                <Text style={styles.statLabel}>Next Reminder</Text>
              </View>
              <Text style={styles.statValue} numberOfLines={1}>
                {upcoming
                  ? `${formatTime(upcoming.hm, settings.timeFormat)}  (${formatCountdown(upcoming.minutesAway)})`
                  : 'Reminder off'}
              </Text>
            </Pressable>
          </View>
        </View>

        <View style={styles.bottom}>
          <Pressable style={[styles.quickAdd, shadow.float]} onPress={quickAdd}>
            <Text style={styles.quickDrop}>💧</Text>
            <Text style={styles.quickGlass}>🥛</Text>
            <Text style={styles.quickText}>
              +{formatVolumeShort(settings.defaultCupMl, settings.unit)}
            </Text>
          </Pressable>

          <Pressable
            style={({ pressed }) => [styles.drinkBtn, shadow.float, pressed && { transform: [{ scale: 0.98 }] }]}
            onPress={() => {
              tapFeedback(settings, 'medium');
              setSheetOpen(true);
            }}
          >
            <Text style={styles.drinkBtnText}>+ DRINK</Text>
          </Pressable>
        </View>
      </View>

      <DrinkSheet
        visible={sheetOpen}
        onClose={() => setSheetOpen(false)}
        onLogged={(ml, drinkId) => {
          const gained = Math.round(ml * drinkById(drinkId).hydration);
          if (todayCount === 0) setCelebrate('first');
          else if (todayMl < settings.goalMl && todayMl + gained >= settings.goalMl) setCelebrate('goal');
        }}
      />
      <SoundSheet visible={soundOpen} onClose={() => setSoundOpen(false)} />

      <Modal visible={celebrate !== null} transparent animationType="fade" onRequestClose={() => setCelebrate(null)}>
        <View style={styles.celebrateBackdrop}>
          <View style={styles.celebrateCard}>
            <Text style={styles.celebrateEmoji}>{celebrate === 'goal' ? '🏆' : '🎉'}</Text>
            <Text style={styles.celebrateTitle}>
              {celebrate === 'goal' ? 'Goal reached!' : 'Congratulations!'}
            </Text>
            <Text style={styles.celebrateBody}>
              {celebrate === 'goal'
                ? `You hit ${formatVolume(settings.goalMl, settings.unit)} today. Keep the streak alive.`
                : 'You have completed your first drink today'}
            </Text>
            <PrimaryButton
              label={celebrate === 'goal' ? 'Nice' : 'Great'}
              onPress={() => setCelebrate(null)}
              style={{ alignSelf: 'stretch', marginTop: 18 }}
            />
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.skyBg },
  content: { flex: 1, paddingHorizontal: 16 },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  streakPill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 16,
    height: 44,
    borderRadius: radius.pill,
    ...shadow.card,
  },
  streakEmoji: { fontSize: 17 },
  streakText: { ...type.title, color: colors.ink },
  headerActions: { flexDirection: 'row', gap: 10 },
  hero: { alignItems: 'center', marginTop: 34 },
  heroRow: { flexDirection: 'row', alignItems: 'flex-end' },
  heroValue: { ...type.display, color: colors.ink },
  heroUnit: { fontSize: 30, fontWeight: '700', color: colors.ink, marginBottom: 12, marginLeft: 4 },
  statRow: { flexDirection: 'row', gap: 10, marginTop: 22, alignSelf: 'stretch' },
  statCard: {
    flex: 1,
    backgroundColor: '#FFFFFF',
    borderRadius: radius.lg,
    paddingHorizontal: 14,
    paddingVertical: 12,
    ...shadow.card,
  },
  statLabelRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  tick: { width: 3, height: 13, borderRadius: 2 },
  statLabel: { ...type.small, color: colors.muted },
  statValueRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  statValue: { ...type.title, color: colors.ink, marginTop: 4 },
  editGlyph: { color: colors.brand, fontSize: 14, marginTop: 4 },
  bottom: { marginTop: 'auto', alignItems: 'center' },
  percentTag: {
    position: 'absolute',
    left: 0,
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderTopRightRadius: 12,
    borderBottomRightRadius: 12,
    minWidth: 58,
    alignItems: 'center',
  },
  percentText: { ...type.small, color: colors.ink, fontWeight: '800' },
  quickAdd: {
    backgroundColor: '#FFFFFF',
    borderRadius: radius.lg,
    paddingHorizontal: 18,
    paddingTop: 10,
    paddingBottom: 12,
    alignItems: 'center',
    minWidth: 108,
    marginBottom: 18,
  },
  quickDrop: { position: 'absolute', left: 8, top: 6, fontSize: 14 },
  quickGlass: { fontSize: 28 },
  quickText: { ...type.title, color: colors.ink, marginTop: 4 },
  drinkBtn: {
    backgroundColor: '#FFFFFF',
    height: 66,
    borderRadius: radius.pill,
    alignSelf: 'stretch',
    alignItems: 'center',
    justifyContent: 'center',
  },
  drinkBtnText: { fontSize: 22, fontWeight: '800', color: colors.ink, letterSpacing: 0.5 },
  celebrateBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(9,14,28,0.55)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 32,
  },
  celebrateCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 26,
    padding: 26,
    alignItems: 'center',
    alignSelf: 'stretch',
  },
  celebrateEmoji: { fontSize: 52 },
  celebrateTitle: { ...type.h2, color: colors.ink, marginTop: 12 },
  celebrateBody: {
    ...type.body,
    color: colors.inkSoft,
    textAlign: 'center',
    marginTop: 8,
    lineHeight: 22,
  },
});
