import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SubHeader } from '@/components/SubHeader';
import { BlueScreen, GlassCard } from '@/components/ui';
import { tapFeedback } from '@/lib/feedback';
import { useHydration } from '@/store/HydrationProvider';
import type { UnitSystem } from '@/lib/types';
import { colors, type } from '@/theme';

const OPTIONS: { key: UnitSystem; title: string; blurb: string }[] = [
  { key: 'metric', title: 'L, kg', blurb: 'Litres and millilitres, kilograms' },
  { key: 'imperial', title: 'fl oz, lb', blurb: 'Fluid ounces, pounds' },
];

export default function UnitsScreen() {
  const { settings, updateSettings } = useHydration();
  return (
    <BlueScreen>
      <SubHeader title="Units" />
      <ScrollView contentContainerStyle={{ paddingHorizontal: 16, paddingBottom: 40 }}>
        <GlassCard>
          {OPTIONS.map((o, i) => {
            const active = settings.unit === o.key;
            return (
              <Pressable
                key={o.key}
                onPress={() => {
                  tapFeedback(settings);
                  updateSettings({ unit: o.key });
                }}
                style={[styles.row, i === OPTIONS.length - 1 && { borderBottomWidth: 0 }]}
              >
                <View style={{ flex: 1 }}>
                  <Text style={styles.title}>{o.title}</Text>
                  <Text style={styles.blurb}>{o.blurb}</Text>
                </View>
                {active ? <Text style={styles.check}>✓</Text> : null}
              </Pressable>
            );
          })}
        </GlassCard>
        <Text style={styles.note}>
          Changing units only changes how amounts are displayed. Your history is stored in
          millilitres, so nothing is lost when you switch back.
        </Text>
      </ScrollView>
    </BlueScreen>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 18,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: colors.hairline,
  },
  title: { ...type.h2, fontSize: 20, color: '#FFFFFF' },
  blurb: { ...type.small, color: colors.mutedOnBlue, marginTop: 3 },
  check: { fontSize: 22, color: '#FFFFFF', fontWeight: '800' },
  note: { ...type.small, color: 'rgba(255,255,255,0.55)', lineHeight: 19, marginTop: 20, paddingHorizontal: 4 },
});
