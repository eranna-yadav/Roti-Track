import { useRouter } from 'expo-router';
import React, { useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { DietMark } from '@/components/diet';
import { CheckIcon } from '@/components/TabIcons';
import { CircleButton, LightScreen } from '@/components/ui';
import { DIET_TIPS } from '@/data/dietTips';
import { slotMeta } from '@/data/mealPlans';
import { addDays, dateToKey, keyToDate } from '@/lib/date';
import { tapFeedback } from '@/lib/feedback';
import { formatServings } from '@/lib/planner';
import { useDiet } from '@/store/DietProvider';
import { useHydration } from '@/store/HydrationProvider';
import { colors, radius, shadow, type } from '@/theme';

const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const PREF_LABEL = { veg: 'Vegetarian', egg: 'Eggetarian', nonveg: 'Non-vegetarian' } as const;
const REGION_LABEL = { all: 'All India', north: 'North Indian', south: 'South Indian', west: 'West Indian', east: 'East Indian' } as const;

export default function PlanScreen() {
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { settings } = useHydration();
  const { profile, todayKey, planForDay, mealsForDay, logPlannedMeal, swapPlannedMeal } = useDiet();

  const [day, setDay] = useState(todayKey);
  const days = useMemo(
    () => Array.from({ length: 7 }, (_, i) => addDays(keyToDate(todayKey), i)),
    [todayKey]
  );

  const plan = planForDay(day);
  const planTotal = plan.reduce((s, m) => s + m.kcal, 0);
  const loggedKeys = new Set(mealsForDay(day).map((m) => m.planKey).filter(Boolean));
  const isToday = day === todayKey;
  const tip = DIET_TIPS[keyToDate(day).getDate() % DIET_TIPS.length];

  return (
    <LightScreen tint={colors.lavender}>
      <ScrollView
        contentContainerStyle={{ paddingTop: insets.top + 10, paddingBottom: insets.bottom + 110 }}
        showsVerticalScrollIndicator={false}
      >
        <View style={[styles.header, styles.gutter]}>
          <Text style={styles.screenTitle}>DIET PLAN</Text>
          <CircleButton tint="#FFFFFF" onPress={() => router.push('/diet-profile')}>
            <Text style={{ fontSize: 18 }}>⚙️</Text>
          </CircleButton>
        </View>

        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={{ gap: 8, paddingHorizontal: 16, paddingVertical: 14 }}
        >
          {days.map((d) => {
            const key = dateToKey(d);
            const active = key === day;
            return (
              <Pressable
                key={key}
                onPress={() => {
                  tapFeedback(settings);
                  setDay(key);
                }}
                style={[styles.dayChip, active && styles.dayChipActive]}
              >
                <Text style={[styles.dayName, active && { color: '#FFFFFF' }]}>
                  {key === todayKey ? 'Today' : WEEKDAYS[d.getDay()]}
                </Text>
                <Text style={[styles.dayNum, active && { color: '#FFFFFF' }]}>{d.getDate()}</Text>
              </Pressable>
            );
          })}
        </ScrollView>

        <View style={styles.gutter}>
          <View style={[styles.summary, shadow.card]}>
            <View style={{ flex: 1 }}>
              <Text style={styles.summaryLabel}>Planned for the day</Text>
              <Text style={styles.summaryValue}>
                {planTotal} <Text style={styles.summaryUnit}>/ {profile.calorieGoal} kcal</Text>
              </Text>
              <View style={styles.tags}>
                <Tag text={PREF_LABEL[profile.pref]} />
                <Tag text={REGION_LABEL[profile.region]} />
              </View>
            </View>
            <Text style={{ fontSize: 46 }}>🍱</Text>
          </View>

          {plan.map((meal) => {
            const meta = slotMeta(meal.slot);
            const logged = loggedKeys.has(`${day}|${meal.slot}`);
            return (
              <View key={meal.slot} style={[styles.card, shadow.card]}>
                <View style={styles.mealHead}>
                  <Text style={styles.mealEmoji}>{meta.emoji}</Text>
                  <View style={{ flex: 1 }}>
                    <Text style={styles.mealSlot}>{meta.label.toUpperCase()} · {meta.time}</Text>
                    <Text style={styles.mealName}>{meal.name}</Text>
                  </View>
                  <Text style={styles.mealKcal}>{meal.kcal}<Text style={styles.mealKcalUnit}> kcal</Text></Text>
                </View>

                {meal.items.map((i) => (
                  <View key={i.food.id} style={styles.item}>
                    <DietMark diet={i.food.diet} size={11} />
                    <Text style={styles.itemName} numberOfLines={1}>
                      {i.food.emoji} {i.food.name}
                    </Text>
                    <Text style={styles.itemQty} numberOfLines={1}>
                      {formatServings(i.servings)} × {i.food.serving.replace(/^1 /, '').replace(/\s*\(.*\)$/, '')}
                    </Text>
                  </View>
                ))}

                <Text style={styles.macros}>
                  P {meal.protein} g · C {meal.carbs} g · F {meal.fat} g
                </Text>

                <View style={styles.actions}>
                  <Pressable
                    style={({ pressed }) => [styles.swapBtn, pressed && { opacity: 0.6 }]}
                    onPress={() => {
                      tapFeedback(settings);
                      swapPlannedMeal(day, meal.slot);
                    }}
                    disabled={logged}
                  >
                    <Text style={[styles.swapText, logged && { opacity: 0.4 }]}>⇄  Swap</Text>
                  </Pressable>
                  {isToday ? (
                    <Pressable
                      style={({ pressed }) => [styles.logBtn, logged && styles.logBtnDone, pressed && { opacity: 0.8 }]}
                      disabled={logged}
                      onPress={() => {
                        tapFeedback(settings, 'success');
                        logPlannedMeal(day, meal);
                      }}
                    >
                      {logged ? <CheckIcon color={colors.leaf} /> : null}
                      <Text style={[styles.logText, logged && { color: colors.leaf }]}>
                        {logged ? 'Logged' : 'I ate this'}
                      </Text>
                    </Pressable>
                  ) : null}
                </View>
              </View>
            );
          })}

          <View style={[styles.tip, shadow.card]}>
            <Text style={styles.tipTitle}>💡 Tip of the day</Text>
            <Text style={styles.tipBody}>{tip}</Text>
          </View>

          <Pressable style={[styles.card, shadow.card, styles.linkCard]} onPress={() => router.push('/insights')}>
            <Text style={{ fontSize: 26 }}>📚</Text>
            <View style={{ flex: 1 }}>
              <Text style={styles.mealName}>Health articles</Text>
              <Text style={styles.macros}>Indian nutrition, hydration and habits</Text>
            </View>
            <Text style={styles.linkArrow}>›</Text>
          </Pressable>

          <Text style={styles.disclaimer}>
            Plans are general guidance built from home-style Indian meals. If you have diabetes, thyroid,
            kidney or heart conditions, or are pregnant, check with a doctor or dietitian first.
          </Text>
        </View>
      </ScrollView>
    </LightScreen>
  );
}

function Tag({ text }: { text: string }) {
  return (
    <View style={styles.tag}>
      <Text style={styles.tagText}>{text}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  gutter: { paddingHorizontal: 16 },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  screenTitle: { fontSize: 30, fontWeight: '900', color: colors.ink, letterSpacing: 0.5 },
  dayChip: {
    width: 58,
    height: 66,
    borderRadius: radius.md,
    backgroundColor: '#FFFFFF',
    alignItems: 'center',
    justifyContent: 'center',
  },
  dayChipActive: { backgroundColor: colors.brand },
  dayName: { ...type.small, color: colors.muted },
  dayNum: { ...type.h2, color: colors.ink, marginTop: 2 },
  summary: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFF4E5',
    borderRadius: radius.lg,
    padding: 16,
    marginBottom: 12,
  },
  summaryLabel: { ...type.small, color: colors.inkSoft },
  summaryValue: { fontSize: 30, fontWeight: '800', color: colors.ink, marginTop: 2 },
  summaryUnit: { ...type.title, color: colors.muted },
  tags: { flexDirection: 'row', gap: 6, marginTop: 8, flexWrap: 'wrap' },
  tag: { backgroundColor: '#FFFFFF', borderRadius: radius.pill, paddingHorizontal: 10, paddingVertical: 4 },
  tagText: { ...type.tiny, color: colors.saffron },
  card: { backgroundColor: '#FFFFFF', borderRadius: radius.lg, padding: 16, marginBottom: 12 },
  mealHead: { flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 8 },
  mealEmoji: { fontSize: 30 },
  mealSlot: { ...type.tiny, color: colors.muted },
  mealName: { ...type.title, color: colors.ink, marginTop: 2 },
  mealKcal: { ...type.title, color: colors.ink },
  mealKcalUnit: { ...type.small, color: colors.muted },
  item: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingVertical: 7,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: colors.divider,
  },
  itemName: { ...type.body, color: colors.ink, flex: 1 },
  itemQty: { ...type.small, color: colors.inkSoft, maxWidth: '45%' },
  macros: { ...type.small, color: colors.muted, fontWeight: '500', marginTop: 6 },
  actions: { flexDirection: 'row', gap: 10, marginTop: 12 },
  swapBtn: {
    flex: 1,
    height: 42,
    borderRadius: radius.pill,
    backgroundColor: '#EEF2FA',
    alignItems: 'center',
    justifyContent: 'center',
  },
  swapText: { ...type.small, color: colors.brand, fontSize: 14 },
  logBtn: {
    flex: 1,
    height: 42,
    borderRadius: radius.pill,
    backgroundColor: colors.leaf,
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'row',
    gap: 6,
  },
  logBtnDone: { backgroundColor: '#E6F6EC' },
  logText: { ...type.small, color: '#FFFFFF', fontSize: 14 },
  tip: { backgroundColor: '#E6F6EC', borderRadius: radius.lg, padding: 16, marginBottom: 12 },
  tipTitle: { ...type.title, color: colors.leaf },
  tipBody: { ...type.body, color: colors.inkSoft, marginTop: 6, lineHeight: 21 },
  linkCard: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  linkArrow: { fontSize: 26, color: colors.muted },
  disclaimer: { ...type.small, color: colors.muted, fontWeight: '500', lineHeight: 18, marginTop: 4 },
});
