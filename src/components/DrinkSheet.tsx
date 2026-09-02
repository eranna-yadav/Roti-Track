import { useRouter } from 'expo-router';
import React, { useEffect, useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { CUP_PRESETS_ML, DRINKS, drinkById } from '@/data/drinks';
import { playDrinkSound, tapFeedback } from '@/lib/feedback';
import { formatCup, formatVolumeShort, fromDisplayVolume, smallVolumeUnit } from '@/lib/units';
import { useHydration } from '@/store/HydrationProvider';
import { colors, radius, type } from '@/theme';
import { Sheet } from './Sheet';
import { PrimaryButton } from './ui';

type Props = {
  visible: boolean;
  onClose: () => void;
  /** Fires once a drink has been logged, so the screen can celebrate. */
  onLogged?: (ml: number, drinkId: string) => void;
  /** Pre-selects a drink and amount for editing an existing record. */
  initial?: { amountMl: number; drinkId: string };
  /** When set, saves back into this entry instead of creating a new one. */
  editingId?: string;
};

/** The add/edit drink sheet: pick a drink, dial the amount, save. */
export function DrinkSheet({ visible, onClose, onLogged, initial, editingId }: Props) {
  const { settings, addDrink, updateEntry } = useHydration();
  const router = useRouter();

  const [drinkId, setDrinkId] = useState(initial?.drinkId ?? settings.favouriteDrinkId);
  const [amountMl, setAmountMl] = useState(initial?.amountMl ?? settings.defaultCupMl);

  useEffect(() => {
    if (!visible) return;
    setDrinkId(initial?.drinkId ?? settings.favouriteDrinkId);
    setAmountMl(initial?.amountMl ?? settings.defaultCupMl);
  }, [visible, initial?.drinkId, initial?.amountMl, settings.favouriteDrinkId, settings.defaultCupMl]);

  const drink = drinkById(drinkId);
  const step = settings.unit === 'metric' ? 50 : Math.round(fromDisplayVolume(1, 'imperial'));

  const presets = useMemo(() => CUP_PRESETS_ML, []);

  const pickDrink = (id: string) => {
    const d = drinkById(id);
    if (d.premium && !settings.pro) {
      onClose();
      router.push('/pro');
      return;
    }
    tapFeedback(settings);
    setDrinkId(id);
    setAmountMl(d.defaultMl);
  };

  const save = () => {
    if (editingId) {
      updateEntry(editingId, { amountMl, drinkId });
    } else {
      addDrink(amountMl, drinkId);
      void playDrinkSound(settings);
    }
    tapFeedback(settings, 'success');
    onLogged?.(amountMl, drinkId);
    onClose();
  };

  return (
    <Sheet visible={visible} onClose={onClose} title={editingId ? 'Edit Record' : 'Add a Drink'}>
      <View style={styles.tip}>
        <Text style={styles.tipEmoji}>💡</Text>
        <Text style={styles.tipText}>
          Drink <Text style={styles.tipStrong}>0.2 - 0.3 L</Text> of water each time to stay well-hydrated.
        </Text>
      </View>

      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.drinkStrip}>
        {DRINKS.map((d) => {
          const active = d.id === drinkId;
          const locked = d.premium && !settings.pro;
          return (
            <Pressable key={d.id} onPress={() => pickDrink(d.id)} style={styles.drinkItem}>
              <View style={[styles.drinkBubble, active && styles.drinkBubbleActive]}>
                <Text style={styles.drinkEmoji}>{d.emoji}</Text>
                {locked ? <Text style={styles.lock}>🔒</Text> : null}
              </View>
              <Text style={[styles.drinkName, active && styles.drinkNameActive]} numberOfLines={1}>
                {d.name}
              </Text>
            </Pressable>
          );
        })}
      </ScrollView>

      <View style={styles.amountRow}>
        <Pressable
          onPress={() => {
            tapFeedback(settings);
            setAmountMl((v) => Math.max(step, v - step));
          }}
          style={styles.stepper}
        >
          <Text style={styles.stepperText}>−</Text>
        </Pressable>
        <View style={styles.amountBox}>
          <Text style={styles.amountValue}>{formatVolumeShort(amountMl, settings.unit)}</Text>
          <Text style={styles.amountSub}>
            {formatCup(amountMl, settings.unit)} of {drink.name.toLowerCase()}
          </Text>
        </View>
        <Pressable
          onPress={() => {
            tapFeedback(settings);
            setAmountMl((v) => Math.min(3000, v + step));
          }}
          style={styles.stepper}
        >
          <Text style={styles.stepperText}>+</Text>
        </Pressable>
      </View>

      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ marginBottom: 18 }}>
        {presets.map((ml) => {
          const active = ml === amountMl;
          return (
            <Pressable
              key={ml}
              onPress={() => {
                tapFeedback(settings);
                setAmountMl(ml);
              }}
              style={[styles.chip, active && styles.chipActive]}
            >
              <Text style={[styles.chipText, active && styles.chipTextActive]}>
                {formatCup(ml, settings.unit)}
              </Text>
            </Pressable>
          );
        })}
      </ScrollView>

      {drink.hydration !== 1 ? (
        <Text style={styles.hydrationNote}>
          Counts as {Math.round(drink.hydration * 100)}% hydration ·{' '}
          {formatVolumeShort(Math.round(amountMl * drink.hydration), settings.unit)} toward your goal
        </Text>
      ) : null}

      <PrimaryButton label={editingId ? 'Save Changes' : `Add ${formatVolumeShort(amountMl, settings.unit)}`} onPress={save} />
      <Text style={styles.unitHint}>Amounts in {smallVolumeUnit(settings.unit)}</Text>
    </Sheet>
  );
}

const styles = StyleSheet.create({
  tip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    backgroundColor: '#EEF3FF',
    borderRadius: radius.lg,
    padding: 12,
    marginBottom: 16,
  },
  tipEmoji: { fontSize: 18 },
  tipText: { ...type.small, color: colors.inkSoft, flex: 1, lineHeight: 18 },
  tipStrong: { color: colors.brand, fontWeight: '800' },
  drinkStrip: { marginBottom: 18 },
  drinkItem: { width: 74, alignItems: 'center' },
  drinkBubble: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: '#F2F5FB',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: 'transparent',
  },
  drinkBubbleActive: { borderColor: colors.brand, backgroundColor: '#E7EFFF' },
  drinkEmoji: { fontSize: 26 },
  lock: { position: 'absolute', right: 2, bottom: 2, fontSize: 12 },
  drinkName: { ...type.tiny, fontSize: 11, letterSpacing: 0, color: colors.muted, marginTop: 6 },
  drinkNameActive: { color: colors.brand },
  amountRow: { flexDirection: 'row', alignItems: 'center', gap: 14, marginBottom: 16 },
  stepper: {
    width: 52,
    height: 52,
    borderRadius: 26,
    backgroundColor: '#F2F5FB',
    alignItems: 'center',
    justifyContent: 'center',
  },
  stepperText: { fontSize: 26, fontWeight: '700', color: colors.brand, marginTop: -2 },
  amountBox: { flex: 1, alignItems: 'center' },
  amountValue: { fontSize: 34, fontWeight: '800', color: colors.ink, letterSpacing: -0.5 },
  amountSub: { ...type.small, color: colors.muted, marginTop: 2 },
  chip: {
    paddingHorizontal: 16,
    height: 40,
    borderRadius: radius.pill,
    backgroundColor: '#F2F5FB',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 8,
  },
  chipActive: { backgroundColor: colors.brand },
  chipText: { ...type.small, color: colors.inkSoft },
  chipTextActive: { color: '#FFFFFF' },
  hydrationNote: { ...type.small, color: colors.muted, textAlign: 'center', marginBottom: 12 },
  unitHint: { ...type.small, color: '#B7C2D4', textAlign: 'center', marginTop: 10 },
});
