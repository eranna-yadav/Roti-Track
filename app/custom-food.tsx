import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SubHeader } from '@/components/SubHeader';
import { LightScreen, PrimaryButton } from '@/components/ui';
import type { MealSlot } from '@/lib/diet-types';
import { tapFeedback } from '@/lib/feedback';
import { useDiet } from '@/store/DietProvider';
import { useHydration } from '@/store/HydrationProvider';
import { colors, radius, type } from '@/theme';

const num = (s: string) => {
  const n = parseFloat(s.replace(',', '.'));
  return Number.isFinite(n) && n >= 0 ? n : 0;
};

export default function CustomFoodScreen() {
  const params = useLocalSearchParams<{ slot?: MealSlot; day?: string; name?: string }>();
  const router = useRouter();
  const { settings } = useHydration();
  const { addCustomFood, logFood, todayKey } = useDiet();

  const [name, setName] = useState(params.name ?? '');
  const [serving, setServing] = useState('1 bowl');
  const [kcal, setKcal] = useState('');
  const [protein, setProtein] = useState('');
  const [carbs, setCarbs] = useState('');
  const [fat, setFat] = useState('');
  const [nonveg, setNonveg] = useState(false);

  // If only macros are filled in, derive calories from them (4/4/9 kcal per g).
  const derived = Math.round(num(protein) * 4 + num(carbs) * 4 + num(fat) * 9);
  const calories = kcal ? num(kcal) : derived;
  const valid = name.trim().length > 0 && calories > 0;

  const save = (andLog: boolean) => {
    const food = addCustomFood({
      name: name.trim(),
      serving: serving.trim() || '1 serving',
      kcal: Math.round(calories),
      protein: num(protein),
      carbs: num(carbs),
      fat: num(fat),
      diet: nonveg ? 'nonveg' : 'veg',
    });
    if (andLog) logFood(food, 1, params.slot ?? 'breakfast', params.day ?? todayKey);
    tapFeedback(settings, 'success');
    router.back();
  };

  return (
    <LightScreen tint="#FFFFFF">
      <SubHeader title="Custom food" dark />
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView contentContainerStyle={styles.body} keyboardShouldPersistTaps="handled">
          <Field label="Name" value={name} onChange={setName} placeholder="e.g. Mom's rajma" />
          <Field label="Serving size" value={serving} onChange={setServing} placeholder="1 bowl, 2 pieces, 100 g" />
          <Field
            label="Calories (kcal)"
            value={kcal}
            onChange={setKcal}
            placeholder={derived ? `${derived} (from macros)` : '0'}
            numeric
          />
          <View style={styles.row}>
            <Field label="Protein (g)" value={protein} onChange={setProtein} placeholder="0" numeric flex />
            <Field label="Carbs (g)" value={carbs} onChange={setCarbs} placeholder="0" numeric flex />
            <Field label="Fat (g)" value={fat} onChange={setFat} placeholder="0" numeric flex />
          </View>

          <Text style={styles.label}>Type</Text>
          <View style={styles.row}>
            {[false, true].map((nv) => (
              <Text
                key={String(nv)}
                onPress={() => setNonveg(nv)}
                style={[styles.choice, nonveg === nv && styles.choiceActive]}
              >
                {nv ? '🔴 Non-veg' : '🟢 Veg'}
              </Text>
            ))}
          </View>

          <PrimaryButton label="Save & add to meal" onPress={() => save(true)} disabled={!valid} style={{ marginTop: 28 }} />
          <Text style={[styles.secondary, !valid && { opacity: 0.4 }]} onPress={valid ? () => save(false) : undefined}>
            Save to My foods only
          </Text>
        </ScrollView>
      </KeyboardAvoidingView>
    </LightScreen>
  );
}

function Field({
  label, value, onChange, placeholder, numeric, flex,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  numeric?: boolean;
  flex?: boolean;
}) {
  return (
    <View style={flex ? { flex: 1 } : undefined}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        value={value}
        onChangeText={onChange}
        placeholder={placeholder}
        placeholderTextColor={colors.muted}
        keyboardType={numeric ? 'decimal-pad' : 'default'}
        style={styles.input}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  body: { paddingHorizontal: 20, paddingBottom: 60 },
  label: { ...type.small, color: colors.inkSoft, marginTop: 16, marginBottom: 8 },
  input: {
    height: 50,
    borderRadius: radius.md,
    backgroundColor: '#F1F4FA',
    paddingHorizontal: 14,
    ...type.body,
    color: colors.ink,
  },
  row: { flexDirection: 'row', gap: 10 },
  choice: {
    flex: 1,
    textAlign: 'center',
    paddingVertical: 12,
    borderRadius: radius.md,
    backgroundColor: '#F1F4FA',
    overflow: 'hidden',
    ...type.title,
    color: colors.inkSoft,
  },
  choiceActive: { backgroundColor: '#E6ECFF', color: colors.brand },
  secondary: { ...type.title, color: colors.brand, textAlign: 'center', marginTop: 18 },
});
