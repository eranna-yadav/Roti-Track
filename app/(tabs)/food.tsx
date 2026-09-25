import { useRouter } from 'expo-router';
import React, { useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { CalorieRing, MacroBar } from '@/components/diet';
import { PlusIcon, TrashIcon } from '@/components/TabIcons';
import { CircleButton, LightScreen } from '@/components/ui';
import { SLOTS } from '@/data/mealPlans';
import { addDays, dateToKey, keyToDate, MONTHS, WEEKDAY_LETTERS } from '@/lib/date';
import { tapFeedback } from '@/lib/feedback';
import { formatServings } from '@/lib/planner';
import { formatVolume } from '@/lib/units';
import { useDiet } from '@/store/DietProvider';
import { useHydration } from '@/store/HydrationProvider';
import { colors, radius, shadow, type } from '@/theme';

export default function FoodScreen() {
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { settings, totalForDayKey } = useHydration();
  const { profile, macros, todayKey, mealsForDay, totalsForDay, removeMeal } = useDiet();

  const [day, setDay] = useState(todayKey);
  const date = keyToDate(day);
  const isToday = day === todayKey;
  const meals = mealsForDay(day);
  const totals = totalsForDay(day);
  const waterMl = totalForDayKey(day);

  const week = useMemo(
    () =>
      Array.from({ length: 7 }, (_, i) => {
        const d = addDays(keyToDate(todayKey), i - 6);
        const key = dateToKey(d);
        return { key, letter: WEEKDAY_LETTERS[d.getDay()], kcal: totalsForDay(key).kcal };
      }),
    [todayKey, totalsForDay]
  );
  const weekMax = Math.max(profile.calorieGoal * 1.2, ...week.map((w) => w.kcal));

  const shift = (n: number) => {
    const next = dateToKey(addDays(date, n));
    if (next > todayKey) return;
    tapFeedback(settings);
    setDay(next);
  };

  const dayLabel = isToday
    ? 'Today'
    : day === dateToKey(addDays(keyToDate(todayKey), -1))
      ? 'Yesterday'
      : `${date.getDate()} ${MONTHS[date.getMonth()]}`;

  return (
    <LightScreen tint={colors.lavender}>
      <ScrollView
        contentContainerStyle={{ paddingTop: insets.top + 10, paddingBottom: insets.bottom + 110, paddingHorizontal: 16 }}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.header}>
          <Text style={styles.screenTitle}>CALORIES</Text>
          <CircleButton tint="#FFFFFF" onPress={() => router.push('/diet-profile')}>
            <Text style={{ fontSize: 18 }}>⚙️</Text>
          </CircleButton>
        </View>

        <View style={styles.dateNav}>
          <Pressable hitSlop={12} onPress={() => shift(-1)}>
            <Text style={styles.navGlyph}>‹</Text>
          </Pressable>
          <Text style={styles.dateLabel}>{dayLabel}</Text>
          <Pressable hitSlop={12} onPress={() => shift(1)} disabled={isToday}>
            <Text style={[styles.navGlyph, isToday && { opacity: 0.25 }]}>›</Text>
          </Pressable>
        </View>

        {!profile.configured ? (
          <Pressable style={[styles.setup, shadow.card]} onPress={() => router.push('/diet-profile')}>
            <Text style={{ fontSize: 26 }}>🎯</Text>
            <View style={{ flex: 1 }}>
              <Text style={styles.setupTitle}>Personalise your target</Text>
              <Text style={styles.setupBody}>Add height, age and activity for an accurate calorie goal.</Text>
            </View>
            <Text style={styles.setupArrow}>›</Text>
          </Pressable>
        ) : null}

        <View style={[styles.card, shadow.card]}>
          <View style={styles.summary}>
            <CalorieRing eaten={totals.kcal} goal={profile.calorieGoal} />
            <View style={styles.summaryStats}>
              <Stat label="Goal" value={`${profile.calorieGoal}`} unit="kcal" color={colors.brand} />
              <Stat label="Eaten" value={`${totals.kcal}`} unit="kcal" color={colors.leaf} />
              <Pressable onPress={() => router.push('/(tabs)')}>
                <Stat
                  label="Water"
                  value={formatVolume(waterMl, settings.unit)}
                  unit={`/ ${formatVolume(settings.goalMl, settings.unit)}`}
                  color={colors.aqua}
                />
              </Pressable>
            </View>
          </View>
          <View style={styles.macros}>
            <MacroBar label="Protein" value={totals.protein} target={macros.protein} color={colors.protein} />
            <MacroBar label="Carbs" value={totals.carbs} target={macros.carbs} color={colors.carbs} />
            <MacroBar label="Fat" value={totals.fat} target={macros.fat} color={colors.fat} />
          </View>
        </View>

        {SLOTS.map((slot) => {
          const items = meals.filter((m) => m.slot === slot.key);
          const kcal = items.reduce((s, m) => s + m.kcal, 0);
          return (
            <View key={slot.key} style={[styles.card, shadow.card, { paddingVertical: 6 }]}>
              <View style={styles.slotHead}>
                <Text style={styles.slotEmoji}>{slot.emoji}</Text>
                <View style={{ flex: 1 }}>
                  <Text style={styles.slotTitle}>{slot.label}</Text>
                  <Text style={styles.slotMeta}>
                    {kcal > 0 ? `${kcal} kcal` : `Suggested ${Math.round(profile.calorieGoal * slot.share)} kcal`}
                  </Text>
                </View>
                <CircleButton
                  size={38}
                  tint={colors.brand}
                  onPress={() => router.push(`/add-food?slot=${slot.key}&day=${day}`)}
                >
                  <PlusIcon color="#FFFFFF" size={20} />
                </CircleButton>
              </View>
              {items.map((m) => (
                <View key={m.id} style={styles.entry}>
                  <Text style={styles.entryEmoji}>{m.emoji}</Text>
                  <View style={{ flex: 1 }}>
                    <Text style={styles.entryName} numberOfLines={1}>{m.name}</Text>
                    <Text style={styles.slotMeta} numberOfLines={1}>
                      {formatServings(m.servings)} × {m.serving}
                    </Text>
                  </View>
                  <Text style={styles.entryKcal}>{m.kcal}</Text>
                  <Pressable
                    hitSlop={10}
                    onPress={() => {
                      tapFeedback(settings);
                      removeMeal(m.id);
                    }}
                  >
                    <TrashIcon color={colors.muted} />
                  </Pressable>
                </View>
              ))}
            </View>
          );
        })}

        <View style={[styles.card, shadow.card]}>
          <Text style={styles.slotTitle}>Last 7 days</Text>
          <View style={styles.week}>
            <View style={[styles.goalLine, { bottom: 22 + (profile.calorieGoal / weekMax) * 110 }]} />
            {week.map((w) => (
              <Pressable key={w.key} style={styles.weekCol} onPress={() => setDay(w.key)}>
                <View
                  style={[
                    styles.weekBar,
                    {
                      height: Math.max(4, (w.kcal / weekMax) * 110),
                      backgroundColor: w.kcal > profile.calorieGoal ? colors.saffron : colors.leaf,
                      opacity: w.kcal === 0 ? 0.2 : w.key === day ? 1 : 0.7,
                    },
                  ]}
                />
                <Text style={[styles.weekLetter, w.key === day && { color: colors.ink }]}>{w.letter}</Text>
              </Pressable>
            ))}
          </View>
          <Text style={styles.slotMeta}>Dashed line is your {profile.calorieGoal} kcal goal.</Text>
        </View>
      </ScrollView>
    </LightScreen>
  );
}

function Stat({ label, value, unit, color }: { label: string; value: string; unit: string; color: string }) {
  return (
    <View style={styles.stat}>
      <View style={[styles.statTick, { backgroundColor: color }]} />
      <View>
        <Text style={styles.statLabel}>{label}</Text>
        <Text style={styles.statValue}>
          {value} <Text style={styles.statUnit}>{unit}</Text>
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  screenTitle: { fontSize: 30, fontWeight: '900', color: colors.ink, letterSpacing: 0.5 },
  dateNav: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 24, marginVertical: 12 },
  navGlyph: { fontSize: 32, color: colors.brand, fontWeight: '600', marginTop: -4 },
  dateLabel: { ...type.title, color: colors.ink, minWidth: 110, textAlign: 'center' },
  setup: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: '#FFF4E5',
    borderRadius: radius.lg,
    padding: 14,
    marginBottom: 12,
  },
  setupTitle: { ...type.title, color: colors.ink },
  setupBody: { ...type.small, color: colors.inkSoft, fontWeight: '500', marginTop: 2 },
  setupArrow: { fontSize: 26, color: colors.saffron },
  card: { backgroundColor: '#FFFFFF', borderRadius: radius.lg, padding: 16, marginBottom: 12 },
  summary: { flexDirection: 'row', alignItems: 'center', gap: 16 },
  summaryStats: { flex: 1, gap: 12 },
  stat: { flexDirection: 'row', gap: 8 },
  statTick: { width: 3, borderRadius: 2 },
  statLabel: { ...type.small, color: colors.muted },
  statValue: { ...type.title, color: colors.ink },
  statUnit: { ...type.small, color: colors.muted },
  macros: { flexDirection: 'row', gap: 14, marginTop: 18 },
  slotHead: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 6 },
  slotEmoji: { fontSize: 26 },
  slotTitle: { ...type.title, color: colors.ink },
  slotMeta: { ...type.small, color: colors.muted, fontWeight: '500', marginTop: 2 },
  entry: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingVertical: 10,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: colors.divider,
  },
  entryEmoji: { fontSize: 22, width: 30, textAlign: 'center' },
  entryName: { ...type.body, color: colors.ink, fontWeight: '700' },
  entryKcal: { ...type.title, color: colors.ink, marginRight: 6 },
  week: { flexDirection: 'row', alignItems: 'flex-end', height: 140, marginTop: 12, marginBottom: 6 },
  weekCol: { flex: 1, alignItems: 'center', justifyContent: 'flex-end' },
  weekBar: { width: 18, borderRadius: 6 },
  weekLetter: { ...type.small, color: colors.muted, marginTop: 6, height: 16 },
  goalLine: {
    position: 'absolute',
    left: 0,
    right: 0,
    borderTopWidth: 1.5,
    borderStyle: 'dashed',
    borderColor: colors.muted,
  },
});
