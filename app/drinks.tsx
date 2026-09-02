import { useRouter } from 'expo-router';
import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SubHeader } from '@/components/SubHeader';
import { BlueScreen, GlassCard, SectionTitle } from '@/components/ui';
import { CUP_PRESETS_ML, DRINKS } from '@/data/drinks';
import { tapFeedback } from '@/lib/feedback';
import { formatCup } from '@/lib/units';
import { useHydration } from '@/store/HydrationProvider';
import { colors, radius, type } from '@/theme';

/** Pick the drink and cup size that the Today screen's quick-add button uses. */
export default function DrinksScreen() {
  const { settings, updateSettings } = useHydration();
  const router = useRouter();

  const free = DRINKS.filter((d) => !d.premium);
  const premium = DRINKS.filter((d) => d.premium);

  const choose = (id: string, locked: boolean) => {
    if (locked) {
      router.push('/pro');
      return;
    }
    tapFeedback(settings);
    updateSettings({ favouriteDrinkId: id });
  };

  const renderGrid = (list: typeof DRINKS, locked: boolean) => (
    <View style={styles.grid}>
      {list.map((d) => {
        const active = settings.favouriteDrinkId === d.id;
        return (
          <Pressable
            key={d.id}
            onPress={() => choose(d.id, locked && !settings.pro)}
            style={[styles.tile, active && styles.tileActive]}
          >
            <Text style={styles.tileEmoji}>{d.emoji}</Text>
            <Text style={[styles.tileName, active && { color: colors.brand }]} numberOfLines={1}>
              {d.name}
            </Text>
            <Text style={[styles.tileMeta, active && { color: colors.brand }]}>
              {Math.round(d.hydration * 100)}%
            </Text>
            {locked && !settings.pro ? <Text style={styles.lock}>🔒</Text> : null}
          </Pressable>
        );
      })}
    </View>
  );

  return (
    <BlueScreen>
      <SubHeader title="Drinks" />
      <ScrollView
        contentContainerStyle={{ paddingHorizontal: 16, paddingBottom: 48 }}
        showsVerticalScrollIndicator={false}
      >
        <Text style={styles.blurb}>
          Pick what the quick-add button pours. The percentage is how much of the volume counts
          toward your hydration goal.
        </Text>

        <SectionTitle>Everyday</SectionTitle>
        {renderGrid(free, false)}

        <SectionTitle>Pro Drinks</SectionTitle>
        {renderGrid(premium, true)}

        <SectionTitle>Default Cup</SectionTitle>
        <GlassCard style={{ paddingVertical: 16 }}>
          <View style={styles.chipWrap}>
            {CUP_PRESETS_ML.map((ml) => {
              const active = settings.defaultCupMl === ml;
              return (
                <Pressable
                  key={ml}
                  onPress={() => {
                    tapFeedback(settings);
                    updateSettings({ defaultCupMl: ml });
                  }}
                  style={[styles.chip, active && styles.chipActive]}
                >
                  <Text style={[styles.chipText, active && styles.chipTextActive]}>
                    {formatCup(ml, settings.unit)}
                  </Text>
                </Pressable>
              );
            })}
          </View>
        </GlassCard>
      </ScrollView>
    </BlueScreen>
  );
}

const styles = StyleSheet.create({
  blurb: { ...type.body, color: colors.mutedOnBlue, lineHeight: 21, marginTop: 4 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  tile: {
    width: '30.8%',
    aspectRatio: 0.95,
    borderRadius: radius.lg,
    backgroundColor: colors.glass,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
    paddingHorizontal: 6,
  },
  tileActive: { backgroundColor: '#FFFFFF' },
  tileEmoji: { fontSize: 28 },
  tileName: { ...type.small, color: '#FFFFFF', textAlign: 'center' },
  tileMeta: { ...type.tiny, color: colors.mutedOnBlue },
  lock: { position: 'absolute', top: 8, right: 10, fontSize: 12 },
  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: {
    paddingHorizontal: 14,
    height: 38,
    borderRadius: radius.pill,
    backgroundColor: 'rgba(255,255,255,0.18)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  chipActive: { backgroundColor: '#FFFFFF' },
  chipText: { ...type.small, color: '#FFFFFF' },
  chipTextActive: { color: colors.brand },
});
