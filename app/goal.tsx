import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SubHeader } from '@/components/SubHeader';
import { BlueScreen, PrimaryButton } from '@/components/ui';
import { recommendedGoalMl } from '@/lib/goal';
import { tapFeedback } from '@/lib/feedback';
import { formatVolume, fromDisplayVolume, toDisplayVolume, volumeUnit } from '@/lib/units';
import { useHydration } from '@/store/HydrationProvider';
import { colors, radius, type } from '@/theme';

export default function GoalScreen() {
  const { settings, updateSettings } = useHydration();
  const router = useRouter();
  const [goalMl, setGoalMl] = useState(settings.goalMl);

  const recommended = recommendedGoalMl(settings.weightKg, settings.gender);
  const step = settings.unit === 'metric' ? 50 : Math.round(fromDisplayVolume(2, 'imperial'));
  const presets = settings.unit === 'metric'
    ? [1200, 1500, 1800, 2000, 2500, 3000]
    : [1183, 1479, 1774, 2070, 2366, 2957]; // 40, 50, 60, 70, 80, 100 fl oz

  const save = () => {
    updateSettings({ goalMl, goalIsCustom: goalMl !== recommended });
    tapFeedback(settings, 'success');
    router.back();
  };

  return (
    <BlueScreen>
      <SubHeader title="Daily Goal" />
      <ScrollView contentContainerStyle={styles.body} showsVerticalScrollIndicator={false}>
        <Text style={styles.hero}>{formatVolume(goalMl, settings.unit)}</Text>
        <Text style={styles.sub}>
          {settings.unit === 'metric'
            ? `${goalMl} ml per day`
            : `${Math.round(toDisplayVolume(goalMl, 'imperial'))} fl oz per day`}
        </Text>

        <View style={styles.stepRow}>
          <Pressable style={styles.stepper} onPress={() => setGoalMl((v) => Math.max(500, v - step))}>
            <Text style={styles.stepperGlyph}>−</Text>
          </Pressable>
          <View style={styles.track}>
            <View
              style={[
                styles.fill,
                { width: `${Math.min(100, (goalMl / 4000) * 100)}%` },
              ]}
            />
          </View>
          <Pressable style={styles.stepper} onPress={() => setGoalMl((v) => Math.min(5000, v + step))}>
            <Text style={styles.stepperGlyph}>+</Text>
          </Pressable>
        </View>

        <View style={styles.chipWrap}>
          {presets.map((ml) => (
            <Pressable
              key={ml}
              onPress={() => {
                tapFeedback(settings);
                setGoalMl(ml);
              }}
              style={[styles.chip, goalMl === ml && styles.chipActive]}
            >
              <Text style={[styles.chipText, goalMl === ml && styles.chipTextActive]}>
                {formatVolume(ml, settings.unit)}
              </Text>
            </Pressable>
          ))}
        </View>

        <Pressable style={styles.recommendCard} onPress={() => setGoalMl(recommended)}>
          <Text style={styles.recommendTitle}>Recommended for you</Text>
          <Text style={styles.recommendValue}>{formatVolume(recommended, settings.unit)}</Text>
          <Text style={styles.recommendBody}>
            Based on {settings.weightKg} kg and your profile. Tap to use it.
          </Text>
        </Pressable>

        <Text style={styles.note}>
          Volumes shown in {volumeUnit(settings.unit)}. Change this under Me → Units.
        </Text>

        <PrimaryButton label="Save Goal" onPress={save} style={{ alignSelf: 'stretch' }} />
      </ScrollView>
    </BlueScreen>
  );
}

const styles = StyleSheet.create({
  body: { paddingHorizontal: 20, paddingBottom: 40, alignItems: 'center' },
  hero: { fontSize: 62, fontWeight: '800', color: '#FFFFFF', letterSpacing: -2, marginTop: 20 },
  sub: { ...type.body, color: colors.mutedOnBlue, marginTop: 4, marginBottom: 30 },
  stepRow: { flexDirection: 'row', alignItems: 'center', gap: 16, alignSelf: 'stretch', marginBottom: 26 },
  stepper: {
    width: 52,
    height: 52,
    borderRadius: 26,
    backgroundColor: 'rgba(255,255,255,0.2)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  stepperGlyph: { fontSize: 26, color: '#FFFFFF', fontWeight: '700', marginTop: -3 },
  track: { flex: 1, height: 8, borderRadius: 4, backgroundColor: 'rgba(255,255,255,0.24)' },
  fill: { height: 8, borderRadius: 4, backgroundColor: '#FFFFFF' },
  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 10, justifyContent: 'center', marginBottom: 26 },
  chip: {
    paddingHorizontal: 18,
    height: 44,
    borderRadius: radius.pill,
    backgroundColor: 'rgba(255,255,255,0.16)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  chipActive: { backgroundColor: '#FFFFFF' },
  chipText: { ...type.title, color: 'rgba(255,255,255,0.85)' },
  chipTextActive: { color: colors.brand },
  recommendCard: {
    alignSelf: 'stretch',
    backgroundColor: colors.glass,
    borderRadius: radius.lg,
    padding: 18,
    marginBottom: 20,
  },
  recommendTitle: { ...type.small, color: colors.mutedOnBlue },
  recommendValue: { fontSize: 28, fontWeight: '800', color: '#FFFFFF', marginTop: 4 },
  recommendBody: { ...type.small, color: colors.mutedOnBlue, marginTop: 6, lineHeight: 18 },
  note: { ...type.small, color: 'rgba(255,255,255,0.55)', textAlign: 'center', marginBottom: 22 },
});
