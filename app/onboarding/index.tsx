import { useRouter } from 'expo-router';
import React, { useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { DropMark } from '@/components/Logo';
import { TimeWheel } from '@/components/TimeWheel';
import { LightScreen, PrimaryButton } from '@/components/ui';
import { tapFeedback } from '@/lib/feedback';
import { recommendedGoalMl } from '@/lib/goal';
import { ensurePermission } from '@/lib/notifications';
import { formatTime } from '@/lib/time';
import { formatVolume, formatWeight, kgToLb, lbToKg } from '@/lib/units';
import { useHydration } from '@/store/HydrationProvider';
import type { Gender, UnitSystem } from '@/lib/types';
import { colors, radius, shadow, type } from '@/theme';

type Step = 'welcome' | 'units' | 'gender' | 'weight' | 'wake' | 'sleep' | 'goal';
const ORDER: Step[] = ['welcome', 'units', 'gender', 'weight', 'wake', 'sleep', 'goal'];

export default function Onboarding() {
  const { settings, updateSettings } = useHydration();
  const insets = useSafeAreaInsets();
  const router = useRouter();

  const [step, setStep] = useState<Step>('welcome');
  const [unit, setUnit] = useState<UnitSystem>(settings.unit);
  const [gender, setGender] = useState<Gender>(settings.gender);
  const [weightKg, setWeightKg] = useState(settings.weightKg);
  const [wakeTime, setWakeTime] = useState(settings.wakeTime);
  const [sleepTime, setSleepTime] = useState(settings.sleepTime);

  const index = ORDER.indexOf(step);
  const goal = useMemo(() => recommendedGoalMl(weightKg, gender), [weightKg, gender]);

  const next = () => {
    tapFeedback(settings);
    if (index < ORDER.length - 1) setStep(ORDER[index + 1]);
  };
  const back = () => {
    if (index > 0) setStep(ORDER[index - 1]);
  };

  const finish = async () => {
    const granted = await ensurePermission();
    updateSettings({
      unit,
      gender,
      weightKg,
      wakeTime,
      sleepTime,
      goalMl: goal,
      goalIsCustom: false,
      reminderEnabled: granted,
      onboarded: true,
    });
    tapFeedback(settings, 'success');
    router.replace('/(tabs)');
  };

  if (step === 'welcome') {
    return (
      <View style={styles.welcome}>
        <View style={{ flex: 1 }} />
        <DropMark size={132} />
        <Text style={styles.welcomeTitle}>SIPWELL</Text>
        <Text style={styles.welcomeTagline}>Keep hydrated for a healthy life</Text>
        <View style={{ flex: 1 }} />
        <Pressable style={styles.startBtn} onPress={next}>
          <Text style={styles.startText}>Get Started</Text>
        </Pressable>
        <Text style={[styles.skip, { marginBottom: insets.bottom + 20 }]} onPress={finish}>
          Skip setup
        </Text>
      </View>
    );
  }

  const nudgeWeight = (delta: number) => {
    tapFeedback(settings);
    setWeightKg((kg) => {
      const nextKg = unit === 'metric' ? kg + delta : lbToKg(kgToLb(kg) + delta);
      return Math.round(Math.max(25, Math.min(250, nextKg)) * 10) / 10;
    });
  };

  return (
    <LightScreen tint="#FFFFFF">
      <View style={[styles.topBar, { paddingTop: insets.top + 8 }]}>
        <Pressable onPress={back} hitSlop={14} style={{ width: 40 }}>
          <Text style={styles.backGlyph}>‹</Text>
        </Pressable>
        <View style={{ flex: 1 }} />
        <Pressable
          onPress={step === 'goal' ? finish : next}
          style={styles.nextBtn}
        >
          <Text style={styles.nextText}>{step === 'goal' ? 'Done' : 'Next'}</Text>
        </Pressable>
      </View>

      <View style={styles.progressTrack}>
        <View style={[styles.progressFill, { width: `${(index / (ORDER.length - 1)) * 100}%` }]} />
      </View>

      <ScrollView contentContainerStyle={styles.body} showsVerticalScrollIndicator={false}>
        {step === 'units' ? (
          <>
            <Text style={styles.question}>Which units do you use?</Text>
            <Hint emoji="📏" text="You can change this any time under Me → Units." />
            <View style={styles.choiceRow}>
              {(['metric', 'imperial'] as UnitSystem[]).map((u) => (
                <Pressable
                  key={u}
                  onPress={() => {
                    tapFeedback(settings);
                    setUnit(u);
                  }}
                  style={[styles.choiceCard, unit === u && styles.choiceCardActive]}
                >
                  <Text style={[styles.choiceTitle, unit === u && styles.choiceTitleActive]}>
                    {u === 'metric' ? 'L, kg' : 'fl oz, lb'}
                  </Text>
                  <Text style={styles.choiceBlurb}>{u === 'metric' ? 'Metric' : 'Imperial'}</Text>
                </Pressable>
              ))}
            </View>
          </>
        ) : null}

        {step === 'gender' ? (
          <>
            <Text style={styles.question}>Tell us about you</Text>
            <Hint emoji="💧" text="Body data lets Sipwell suggest a realistic daily target." />
            <View style={styles.choiceRow}>
              {(['female', 'male', 'other'] as Gender[]).map((g) => (
                <Pressable
                  key={g}
                  onPress={() => {
                    tapFeedback(settings);
                    setGender(g);
                  }}
                  style={[styles.choiceCard, gender === g && styles.choiceCardActive]}
                >
                  <Text style={styles.choiceEmoji}>
                    {g === 'female' ? '👩' : g === 'male' ? '👨' : '🧑'}
                  </Text>
                  <Text style={[styles.choiceTitle, gender === g && styles.choiceTitleActive]}>
                    {g[0].toUpperCase() + g.slice(1)}
                  </Text>
                </Pressable>
              ))}
            </View>
          </>
        ) : null}

        {step === 'weight' ? (
          <>
            <Text style={styles.question}>What do you weigh?</Text>
            <Hint emoji="⚖️" text="Roughly 35 ml of water per kilogram is a good starting point." />
            <View style={styles.weightRow}>
              <Pressable style={styles.stepper} onPress={() => nudgeWeight(-1)}>
                <Text style={styles.stepperGlyph}>−</Text>
              </Pressable>
              <Text style={styles.weightValue}>{formatWeight(weightKg, unit)}</Text>
              <Pressable style={styles.stepper} onPress={() => nudgeWeight(1)}>
                <Text style={styles.stepperGlyph}>+</Text>
              </Pressable>
            </View>
          </>
        ) : null}

        {step === 'wake' ? (
          <>
            <Text style={styles.question}>When do you usually start a day?</Text>
            <Hint emoji="☀️" text="A glass of water on waking rehydrates you after the night." />
            <TimeWheel value={wakeTime} onChange={setWakeTime} minuteStep={5} />
          </>
        ) : null}

        {step === 'sleep' ? (
          <>
            <Text style={styles.question}>When do you usually end a day?</Text>
            <Hint
              emoji="🌙"
              text="Drinking water 1 hour before sleep will keep you hydrated during your sweet dream"
            />
            <TimeWheel value={sleepTime} onChange={setSleepTime} minuteStep={5} />
          </>
        ) : null}

        {step === 'goal' ? (
          <>
            <Text style={styles.question}>Your daily goal</Text>
            <Hint emoji="🎯" text="You can fine-tune this later on the Daily Goal screen." />
            <View style={styles.goalCard}>
              <Text style={styles.goalValue}>{formatVolume(goal, unit)}</Text>
              <Text style={styles.goalBlurb}>
                Based on {formatWeight(weightKg, unit)} and your waking hours of{' '}
                {formatTime(wakeTime, settings.timeFormat)} –{' '}
                {formatTime(sleepTime, settings.timeFormat)}.
              </Text>
            </View>
            <PrimaryButton label="Start Tracking" onPress={finish} style={{ alignSelf: 'stretch', marginTop: 30 }} />
          </>
        ) : null}
      </ScrollView>
    </LightScreen>
  );
}

function Hint({ emoji, text }: { emoji: string; text: string }) {
  return (
    <View style={styles.hint}>
      <Text style={styles.hintEmoji}>{emoji}</Text>
      <Text style={styles.hintText}>{text}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  welcome: { flex: 1, backgroundColor: colors.brand, alignItems: 'center' },
  welcomeTitle: {
    fontSize: 42,
    fontWeight: '900',
    color: '#FFFFFF',
    letterSpacing: 3,
    marginTop: 30,
  },
  welcomeTagline: { ...type.title, color: 'rgba(255,255,255,0.82)', marginTop: 10 },
  startBtn: {
    alignSelf: 'stretch',
    marginHorizontal: 24,
    height: 58,
    borderRadius: radius.pill,
    backgroundColor: '#FFFFFF',
    alignItems: 'center',
    justifyContent: 'center',
    ...shadow.float,
  },
  startText: { fontSize: 18, fontWeight: '800', color: colors.brand },
  skip: { ...type.body, color: 'rgba(255,255,255,0.8)', marginTop: 18 },
  topBar: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingBottom: 12 },
  backGlyph: { fontSize: 34, color: colors.ink, marginTop: -6 },
  nextBtn: {
    paddingHorizontal: 22,
    height: 40,
    borderRadius: radius.pill,
    borderWidth: 1.5,
    borderColor: colors.brand,
    alignItems: 'center',
    justifyContent: 'center',
  },
  nextText: { ...type.title, color: colors.brand },
  progressTrack: {
    height: 5,
    backgroundColor: '#E8EDF6',
    marginHorizontal: 20,
    borderRadius: 3,
    overflow: 'hidden',
  },
  progressFill: { height: 5, backgroundColor: colors.brand, borderRadius: 3 },
  body: { paddingHorizontal: 22, paddingBottom: 50 },
  question: { fontSize: 34, fontWeight: '900', color: colors.ink, lineHeight: 40, marginTop: 26 },
  hint: {
    flexDirection: 'row',
    gap: 12,
    backgroundColor: '#EEF3FF',
    borderRadius: radius.lg,
    padding: 16,
    marginTop: 20,
    marginBottom: 26,
  },
  hintEmoji: { fontSize: 22 },
  hintText: { ...type.body, color: colors.ink, flex: 1, lineHeight: 22 },
  choiceRow: { flexDirection: 'row', gap: 12 },
  choiceCard: {
    flex: 1,
    minHeight: 118,
    borderRadius: radius.lg,
    backgroundColor: '#F4F7FC',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    padding: 12,
  },
  choiceCardActive: { backgroundColor: colors.brand, ...shadow.card },
  choiceEmoji: { fontSize: 30 },
  choiceTitle: { ...type.h2, fontSize: 20, color: colors.ink, textAlign: 'center' },
  choiceTitleActive: { color: '#FFFFFF' },
  choiceBlurb: { ...type.small, color: colors.muted },
  weightRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 20 },
  stepper: {
    width: 58,
    height: 58,
    borderRadius: 29,
    backgroundColor: '#F2F5FB',
    alignItems: 'center',
    justifyContent: 'center',
  },
  stepperGlyph: { fontSize: 28, fontWeight: '700', color: colors.brand, marginTop: -3 },
  weightValue: { fontSize: 44, fontWeight: '800', color: colors.ink, letterSpacing: -1 },
  goalCard: { backgroundColor: '#EEF3FF', borderRadius: radius.xl, padding: 24, alignItems: 'center' },
  goalValue: { fontSize: 52, fontWeight: '900', color: colors.brand, letterSpacing: -2 },
  goalBlurb: { ...type.body, color: colors.inkSoft, textAlign: 'center', marginTop: 10, lineHeight: 22 },
});
