import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SubHeader } from '@/components/SubHeader';
import { BlueScreen, PrimaryButton } from '@/components/ui';
import type { ActivityLevel, DietPref, Region, WeightGoal } from '@/lib/diet-types';
import { tapFeedback } from '@/lib/feedback';
import { ACTIVITY, bmi, bmiLabel, GOALS, macroTargets, recommendedCalories } from '@/lib/nutrition';
import { formatWeight } from '@/lib/units';
import { useDiet } from '@/store/DietProvider';
import { useHydration } from '@/store/HydrationProvider';
import { colors, radius, type } from '@/theme';

const PREFS: { key: DietPref; label: string; emoji: string }[] = [
  { key: 'veg', label: 'Veg', emoji: '🥦' },
  { key: 'egg', label: 'Egg', emoji: '🥚' },
  { key: 'nonveg', label: 'Non-veg', emoji: '🍗' },
];

const REGIONS: { key: Region; label: string }[] = [
  { key: 'all', label: 'All India' },
  { key: 'north', label: 'North' },
  { key: 'south', label: 'South' },
  { key: 'west', label: 'West' },
  { key: 'east', label: 'East' },
];

export default function DietProfileScreen() {
  const router = useRouter();
  const { settings } = useHydration();
  const { profile, updateProfile } = useDiet();

  const [heightCm, setHeightCm] = useState(profile.heightCm);
  const [age, setAge] = useState(profile.age);
  const [activity, setActivity] = useState<ActivityLevel>(profile.activity);
  const [goal, setGoal] = useState<WeightGoal>(profile.goal);
  const [pref, setPref] = useState<DietPref>(profile.pref);
  const [region, setRegion] = useState<Region>(profile.region);
  const [custom, setCustom] = useState(profile.calorieGoalIsCustom);
  const [manual, setManual] = useState(profile.calorieGoal);

  const suggested = recommendedCalories(settings.weightKg, settings.gender, { heightCm, age, activity, goal });
  const target = custom ? manual : suggested;
  const macros = macroTargets(target, settings.weightKg, goal);
  const bmiValue = bmi(settings.weightKg, heightCm);
  const bmiInfo = bmiLabel(bmiValue);

  const pick = <T,>(set: (v: T) => void) => (v: T) => {
    tapFeedback(settings);
    set(v);
  };

  const save = () => {
    updateProfile({
      configured: true,
      heightCm,
      age,
      activity,
      goal,
      pref,
      region,
      calorieGoalIsCustom: custom,
      calorieGoal: target,
    });
    tapFeedback(settings, 'success');
    router.back();
  };

  return (
    <BlueScreen>
      <SubHeader title="Diet Profile" />
      <ScrollView contentContainerStyle={styles.body} showsVerticalScrollIndicator={false}>
        <Pressable style={styles.bodyRow} onPress={() => router.push('/body')}>
          <Text style={styles.bodyText}>
            {settings.gender === 'male' ? '👨' : settings.gender === 'female' ? '👩' : '🧑'}{'  '}
            {formatWeight(settings.weightKg, settings.unit)} · BMI {bmiValue.toFixed(1)}{' '}
            <Text style={{ color: bmiInfo.color }}>●</Text> {bmiInfo.label}
          </Text>
          <Text style={styles.edit}>Edit ›</Text>
        </Pressable>

        <View style={styles.pair}>
          <Stepper label="Height" value={`${heightCm} cm`} onChange={(d) => setHeightCm((h) => clamp(h + d, 120, 220))} />
          <Stepper label="Age" value={`${age} yrs`} onChange={(d) => setAge((a) => clamp(a + d, 12, 100))} />
        </View>

        <Text style={styles.label}>Goal</Text>
        <View style={styles.cardRow}>
          {GOALS.map((g) => (
            <Pressable key={g.key} onPress={() => pick(setGoal)(g.key)} style={[styles.tile, goal === g.key && styles.tileActive]}>
              <Text style={styles.tileEmoji}>{g.emoji}</Text>
              <Text style={[styles.tileLabel, goal === g.key && styles.tileLabelActive]}>{g.label}</Text>
            </Pressable>
          ))}
        </View>

        <Text style={styles.label}>Activity</Text>
        <View style={styles.list}>
          {ACTIVITY.map((a, i) => (
            <Pressable
              key={a.key}
              onPress={() => pick(setActivity)(a.key)}
              style={[styles.listRow, i < ACTIVITY.length - 1 && styles.listDivider]}
            >
              <View style={{ flex: 1 }}>
                <Text style={styles.listTitle}>{a.label}</Text>
                <Text style={styles.listHint}>{a.hint}</Text>
              </View>
              <View style={[styles.radio, activity === a.key && styles.radioOn]} />
            </Pressable>
          ))}
        </View>

        <Text style={styles.label}>Food preference</Text>
        <View style={styles.cardRow}>
          {PREFS.map((p) => (
            <Pressable key={p.key} onPress={() => pick(setPref)(p.key)} style={[styles.tile, pref === p.key && styles.tileActive]}>
              <Text style={styles.tileEmoji}>{p.emoji}</Text>
              <Text style={[styles.tileLabel, pref === p.key && styles.tileLabelActive]}>{p.label}</Text>
            </Pressable>
          ))}
        </View>

        <Text style={styles.label}>Cuisine for meal plans</Text>
        <View style={styles.chips}>
          {REGIONS.map((r) => (
            <Pressable key={r.key} onPress={() => pick(setRegion)(r.key)} style={[styles.chip, region === r.key && styles.chipActive]}>
              <Text style={[styles.chipText, region === r.key && styles.chipTextActive]}>{r.label}</Text>
            </Pressable>
          ))}
        </View>

        <View style={styles.result}>
          <Text style={styles.resultLabel}>Daily calorie target</Text>
          <View style={styles.resultRow}>
            {custom ? (
              <Pressable style={styles.miniStep} onPress={() => setManual((m) => clamp(m - 50, 1000, 4500))}>
                <Text style={styles.miniGlyph}>−</Text>
              </Pressable>
            ) : null}
            <Text style={styles.resultValue}>{target} kcal</Text>
            {custom ? (
              <Pressable style={styles.miniStep} onPress={() => setManual((m) => clamp(m + 50, 1000, 4500))}>
                <Text style={styles.miniGlyph}>+</Text>
              </Pressable>
            ) : null}
          </View>
          <Text style={styles.resultMacros}>
            Protein {macros.protein} g · Carbs {macros.carbs} g · Fat {macros.fat} g
          </Text>
          <Text
            style={styles.resultToggle}
            onPress={() => {
              if (!custom) setManual(suggested);
              setCustom(!custom);
            }}
          >
            {custom ? `Use suggested (${suggested} kcal)` : 'Set my own target'}
          </Text>
        </View>

        <PrimaryButton label="Save" onPress={save} />
      </ScrollView>
    </BlueScreen>
  );
}

const clamp = (v: number, lo: number, hi: number) => Math.max(lo, Math.min(hi, v));

function Stepper({ label, value, onChange }: { label: string; value: string; onChange: (delta: number) => void }) {
  return (
    <View style={styles.stepper}>
      <Text style={styles.stepLabel}>{label}</Text>
      <View style={styles.stepRow}>
        <Pressable style={styles.miniStep} onPress={() => onChange(-1)}>
          <Text style={styles.miniGlyph}>−</Text>
        </Pressable>
        <Text style={styles.stepValue}>{value}</Text>
        <Pressable style={styles.miniStep} onPress={() => onChange(1)}>
          <Text style={styles.miniGlyph}>+</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  body: { paddingHorizontal: 20, paddingBottom: 40 },
  bodyRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.glass,
    borderRadius: radius.lg,
    padding: 14,
    marginTop: 8,
  },
  bodyText: { ...type.body, color: '#FFFFFF', flex: 1, fontWeight: '700' },
  edit: { ...type.small, color: colors.mutedOnBlue },
  pair: { flexDirection: 'row', gap: 12, marginTop: 14 },
  stepper: { flex: 1, backgroundColor: colors.glass, borderRadius: radius.lg, padding: 12 },
  stepLabel: { ...type.small, color: colors.mutedOnBlue },
  stepRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 8 },
  stepValue: { ...type.title, color: '#FFFFFF' },
  miniStep: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: 'rgba(255,255,255,0.22)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  miniGlyph: { fontSize: 20, color: '#FFFFFF', fontWeight: '700', marginTop: -2 },
  label: { ...type.title, color: colors.mutedOnBlue, marginTop: 22, marginBottom: 10 },
  cardRow: { flexDirection: 'row', gap: 10 },
  tile: {
    flex: 1,
    height: 88,
    borderRadius: radius.lg,
    backgroundColor: colors.glass,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
  },
  tileActive: { backgroundColor: '#FFFFFF' },
  tileEmoji: { fontSize: 26 },
  tileLabel: { ...type.small, color: 'rgba(255,255,255,0.85)' },
  tileLabelActive: { color: colors.brand },
  list: { backgroundColor: colors.glass, borderRadius: radius.lg, paddingHorizontal: 14 },
  listRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: 12 },
  listDivider: { borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.hairline },
  listTitle: { ...type.title, color: '#FFFFFF' },
  listHint: { ...type.small, color: colors.mutedOnBlue, fontWeight: '500', marginTop: 2 },
  radio: { width: 20, height: 20, borderRadius: 10, borderWidth: 2, borderColor: 'rgba(255,255,255,0.5)' },
  radioOn: { borderWidth: 6, borderColor: '#FFFFFF' },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: { paddingHorizontal: 16, height: 38, borderRadius: radius.pill, backgroundColor: colors.glass, justifyContent: 'center' },
  chipActive: { backgroundColor: '#FFFFFF' },
  chipText: { ...type.small, color: 'rgba(255,255,255,0.85)' },
  chipTextActive: { color: colors.brand },
  result: { backgroundColor: colors.glass, borderRadius: radius.lg, padding: 18, marginTop: 24, marginBottom: 24, alignItems: 'center' },
  resultLabel: { ...type.small, color: colors.mutedOnBlue },
  resultRow: { flexDirection: 'row', alignItems: 'center', gap: 16, marginTop: 6 },
  resultValue: { fontSize: 32, fontWeight: '800', color: '#FFFFFF' },
  resultMacros: { ...type.small, color: colors.mutedOnBlue, marginTop: 6 },
  resultToggle: { ...type.small, color: '#FFFFFF', textDecorationLine: 'underline', marginTop: 12 },
});
