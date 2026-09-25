import React, { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import Svg, { Circle } from 'react-native-svg';
import { SLOTS } from '@/data/mealPlans';
import type { Food, MealSlot } from '@/lib/diet-types';
import { formatServings } from '@/lib/planner';
import { colors, radius, type } from '@/theme';
import { Sheet } from './Sheet';
import { PrimaryButton } from './ui';

/** Ring showing kcal eaten against the target; turns saffron when over. */
export function CalorieRing({ eaten, goal, size = 170 }: { eaten: number; goal: number; size?: number }) {
  const stroke = 14;
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  const pct = goal > 0 ? Math.min(1, eaten / goal) : 0;
  const over = eaten > goal;
  const left = Math.abs(goal - eaten);
  return (
    <View style={{ width: size, height: size, alignItems: 'center', justifyContent: 'center' }}>
      <Svg width={size} height={size} style={StyleSheet.absoluteFill}>
        <Circle cx={size / 2} cy={size / 2} r={r} stroke="#E4EAF6" strokeWidth={stroke} fill="none" />
        <Circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          stroke={over ? colors.saffron : colors.leaf}
          strokeWidth={stroke}
          strokeLinecap="round"
          fill="none"
          strokeDasharray={`${c * pct} ${c}`}
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
        />
      </Svg>
      <Text style={styles.ringValue}>{left}</Text>
      <Text style={styles.ringLabel}>{over ? 'kcal over' : 'kcal left'}</Text>
    </View>
  );
}

export function MacroBar({ label, value, target, color }: { label: string; value: number; target: number; color: string }) {
  const pct = target > 0 ? Math.min(1, value / target) : 0;
  return (
    <View style={{ flex: 1 }}>
      <Text style={styles.macroLabel}>{label}</Text>
      <View style={styles.macroTrack}>
        <View style={[styles.macroFill, { width: `${pct * 100}%`, backgroundColor: color }]} />
      </View>
      <Text style={styles.macroValue}>
        {Math.round(value)}
        <Text style={styles.macroTarget}> / {target} g</Text>
      </Text>
    </View>
  );
}

/** Green square-dot for veg, red for non-veg, as printed on Indian food packs. */
export function DietMark({ diet, size = 14 }: { diet: Food['diet']; size?: number }) {
  const color = diet === 'veg' ? colors.veg : diet === 'egg' ? colors.saffron : colors.nonveg;
  return (
    <View style={[styles.mark, { width: size, height: size, borderColor: color }]}>
      <View style={{ width: size * 0.45, height: size * 0.45, borderRadius: size, backgroundColor: color }} />
    </View>
  );
}

export function FoodRow({
  food,
  onPress,
  right,
}: {
  food: Food;
  onPress?: () => void;
  right?: React.ReactNode;
}) {
  return (
    <Pressable onPress={onPress} style={({ pressed }) => [styles.foodRow, pressed && { opacity: 0.6 }]}>
      <Text style={styles.foodEmoji}>{food.emoji}</Text>
      <View style={{ flex: 1 }}>
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
          <DietMark diet={food.diet} size={12} />
          <Text style={styles.foodName} numberOfLines={1}>{food.name}</Text>
        </View>
        <Text style={styles.foodMeta} numberOfLines={1}>
          {food.serving} · P {food.protein} · C {food.carbs} · F {food.fat}
        </Text>
      </View>
      {right ?? <Text style={styles.foodKcal}>{food.kcal}<Text style={styles.foodKcalUnit}> kcal</Text></Text>}
    </Pressable>
  );
}

/** Pick servings and meal, then log. Used by the food search screen. */
export function LogFoodSheet({
  food,
  initialSlot,
  onClose,
  onLog,
}: {
  food: Food | null;
  initialSlot: MealSlot;
  onClose: () => void;
  onLog: (servings: number, slot: MealSlot) => void;
}) {
  const [servings, setServings] = useState(1);
  const [slot, setSlot] = useState<MealSlot>(initialSlot);

  useEffect(() => {
    if (food) {
      setServings(1);
      setSlot(initialSlot);
    }
  }, [food, initialSlot]);

  if (!food) return null;
  const step = food.unit === 'piece' ? 1 : 0.5;
  const grams = (v: number) => `${Math.round(v * servings * 10) / 10} g`;
  const slotLabel = (k: MealSlot) => (k === 'snack' ? 'Snack' : SLOTS.find((s) => s.key === k)!.label);

  return (
    <Sheet visible={!!food} onClose={onClose}>
      <View style={styles.sheetHead}>
        <Text style={styles.sheetEmoji}>{food.emoji}</Text>
        <View style={{ flex: 1 }}>
          <Text style={styles.sheetTitle}>{food.name}</Text>
          <Text style={styles.foodMeta}>{food.serving} per serving</Text>
        </View>
      </View>

      <View style={styles.stepRow}>
        <Pressable style={styles.stepBtn} onPress={() => setServings((s) => Math.max(step, s - step))}>
          <Text style={styles.stepGlyph}>−</Text>
        </Pressable>
        <View style={{ alignItems: 'center' }}>
          <Text style={styles.stepValue}>{formatServings(servings)}</Text>
          <Text style={styles.foodMeta}>serving{servings === 1 ? '' : 's'}</Text>
        </View>
        <Pressable style={styles.stepBtn} onPress={() => setServings((s) => Math.min(20, s + step))}>
          <Text style={styles.stepGlyph}>+</Text>
        </Pressable>
      </View>

      <View style={styles.nutriRow}>
        <Nutri label="Calories" value={`${Math.round(food.kcal * servings)}`} color={colors.leaf} />
        <Nutri label="Protein" value={grams(food.protein)} color={colors.protein} />
        <Nutri label="Carbs" value={grams(food.carbs)} color={colors.carbs} />
        <Nutri label="Fat" value={grams(food.fat)} color={colors.fat} />
      </View>

      <View style={styles.slotRow}>
        {SLOTS.map((s) => (
          <Pressable
            key={s.key}
            onPress={() => setSlot(s.key)}
            style={[styles.slotChip, slot === s.key && styles.slotChipActive]}
          >
            <Text style={[styles.slotText, slot === s.key && styles.slotTextActive]}>
              {slotLabel(s.key)}
            </Text>
          </Pressable>
        ))}
      </View>

      <PrimaryButton label={`Add to ${slotLabel(slot)}`} onPress={() => onLog(servings, slot)} />
    </Sheet>
  );
}

function Nutri({ label, value, color }: { label: string; value: string; color: string }) {
  return (
    <View style={styles.nutri}>
      <View style={[styles.nutriDot, { backgroundColor: color }]} />
      <Text style={styles.nutriValue}>{value}</Text>
      <Text style={styles.nutriLabel}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  ringValue: { fontSize: 38, fontWeight: '800', color: colors.ink, letterSpacing: -1 },
  ringLabel: { ...type.small, color: colors.muted },
  macroLabel: { ...type.small, color: colors.inkSoft },
  macroTrack: { height: 8, borderRadius: 4, backgroundColor: '#E4EAF6', marginTop: 6, overflow: 'hidden' },
  macroFill: { height: 8, borderRadius: 4 },
  macroValue: { ...type.small, color: colors.ink, marginTop: 6, fontWeight: '800' },
  macroTarget: { color: colors.muted, fontWeight: '600' },
  mark: { borderWidth: 1.5, borderRadius: 3, alignItems: 'center', justifyContent: 'center' },
  foodRow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 12 },
  foodEmoji: { fontSize: 28, width: 36, textAlign: 'center' },
  foodName: { ...type.title, fontSize: 16, color: colors.ink, flexShrink: 1 },
  foodMeta: { ...type.small, color: colors.muted, marginTop: 2, fontWeight: '500' },
  foodKcal: { ...type.title, color: colors.ink },
  foodKcalUnit: { ...type.small, color: colors.muted },
  sheetHead: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  sheetEmoji: { fontSize: 40 },
  sheetTitle: { ...type.h2, color: colors.ink },
  stepRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginVertical: 20, paddingHorizontal: 20 },
  stepBtn: { width: 52, height: 52, borderRadius: 26, backgroundColor: '#EEF2FA', alignItems: 'center', justifyContent: 'center' },
  stepGlyph: { fontSize: 26, fontWeight: '700', color: colors.brand, marginTop: -3 },
  stepValue: { fontSize: 36, fontWeight: '800', color: colors.ink },
  nutriRow: { flexDirection: 'row', backgroundColor: '#F5F7FB', borderRadius: radius.lg, paddingVertical: 14 },
  nutri: { flex: 1, alignItems: 'center' },
  nutriDot: { width: 8, height: 8, borderRadius: 4, marginBottom: 6 },
  nutriValue: { ...type.title, color: colors.ink },
  nutriLabel: { ...type.tiny, color: colors.muted, marginTop: 2 },
  slotRow: { flexDirection: 'row', gap: 8, marginVertical: 18 },
  slotChip: { flex: 1, height: 40, borderRadius: radius.pill, backgroundColor: '#EEF2FA', alignItems: 'center', justifyContent: 'center' },
  slotChipActive: { backgroundColor: colors.brand },
  slotText: { ...type.small, color: colors.inkSoft },
  slotTextActive: { color: '#FFFFFF' },
});
