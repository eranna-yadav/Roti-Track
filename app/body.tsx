import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SubHeader } from '@/components/SubHeader';
import { BlueScreen, PrimaryButton } from '@/components/ui';
import { tapFeedback } from '@/lib/feedback';
import { recommendedGoalMl } from '@/lib/goal';
import { formatVolume, formatWeight, kgToLb, lbToKg } from '@/lib/units';
import { useHydration } from '@/store/HydrationProvider';
import type { Gender } from '@/lib/types';
import { colors, radius, type } from '@/theme';

const GENDERS: { key: Gender; label: string; emoji: string }[] = [
  { key: 'female', label: 'Female', emoji: '👩' },
  { key: 'male', label: 'Male', emoji: '👨' },
  { key: 'other', label: 'Other', emoji: '🧑' },
];

export default function BodyScreen() {
  const { settings, updateSettings } = useHydration();
  const router = useRouter();
  const [gender, setGender] = useState<Gender>(settings.gender);
  const [weightKg, setWeightKg] = useState(settings.weightKg);

  const metric = settings.unit === 'metric';
  const nudge = (delta: number) => {
    setWeightKg((kg) => {
      const next = metric ? kg + delta : lbToKg(kgToLb(kg) + delta);
      return Math.round(Math.max(25, Math.min(250, next)) * 10) / 10;
    });
    tapFeedback(settings);
  };

  const projected = recommendedGoalMl(weightKg, gender);

  return (
    <BlueScreen>
      <SubHeader title="Gender & Weight" />
      <ScrollView contentContainerStyle={styles.body} showsVerticalScrollIndicator={false}>
        <Text style={styles.label}>Gender</Text>
        <View style={styles.genderRow}>
          {GENDERS.map((g) => {
            const active = g.key === gender;
            return (
              <Pressable
                key={g.key}
                onPress={() => {
                  tapFeedback(settings);
                  setGender(g.key);
                }}
                style={[styles.genderCard, active && styles.genderCardActive]}
              >
                <Text style={styles.genderEmoji}>{g.emoji}</Text>
                <Text style={[styles.genderLabel, active && styles.genderLabelActive]}>{g.label}</Text>
              </Pressable>
            );
          })}
        </View>

        <Text style={styles.label}>Weight</Text>
        <View style={styles.weightCard}>
          <Pressable style={styles.stepper} onPress={() => nudge(-1)}>
            <Text style={styles.stepperGlyph}>−</Text>
          </Pressable>
          <Text style={styles.weightValue}>{formatWeight(weightKg, settings.unit)}</Text>
          <Pressable style={styles.stepper} onPress={() => nudge(1)}>
            <Text style={styles.stepperGlyph}>+</Text>
          </Pressable>
        </View>

        <View style={styles.projection}>
          <Text style={styles.projectionLabel}>Suggested daily goal</Text>
          <Text style={styles.projectionValue}>{formatVolume(projected, settings.unit)}</Text>
          <Text style={styles.projectionNote}>
            {settings.goalIsCustom
              ? 'Your goal is set manually, so it will not change. Reset it on the Daily Goal screen.'
              : 'Your daily goal follows these numbers automatically.'}
          </Text>
        </View>

        <PrimaryButton
          label="Save"
          onPress={() => {
            updateSettings({ gender, weightKg });
            tapFeedback(settings, 'success');
            router.back();
          }}
        />
      </ScrollView>
    </BlueScreen>
  );
}

const styles = StyleSheet.create({
  body: { paddingHorizontal: 20, paddingBottom: 40 },
  label: { ...type.title, color: colors.mutedOnBlue, marginTop: 22, marginBottom: 12 },
  genderRow: { flexDirection: 'row', gap: 12 },
  genderCard: {
    flex: 1,
    height: 108,
    borderRadius: radius.lg,
    backgroundColor: colors.glass,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  genderCardActive: { backgroundColor: '#FFFFFF' },
  genderEmoji: { fontSize: 32 },
  genderLabel: { ...type.title, color: 'rgba(255,255,255,0.85)' },
  genderLabelActive: { color: colors.brand },
  weightCard: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: colors.glass,
    borderRadius: radius.lg,
    padding: 16,
  },
  stepper: {
    width: 52,
    height: 52,
    borderRadius: 26,
    backgroundColor: 'rgba(255,255,255,0.22)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  stepperGlyph: { fontSize: 26, color: '#FFFFFF', fontWeight: '700', marginTop: -3 },
  weightValue: { fontSize: 36, fontWeight: '800', color: '#FFFFFF', letterSpacing: -1 },
  projection: { backgroundColor: colors.glass, borderRadius: radius.lg, padding: 18, marginTop: 22, marginBottom: 26 },
  projectionLabel: { ...type.small, color: colors.mutedOnBlue },
  projectionValue: { fontSize: 30, fontWeight: '800', color: '#FFFFFF', marginTop: 4 },
  projectionNote: { ...type.small, color: colors.mutedOnBlue, marginTop: 8, lineHeight: 18 },
});
