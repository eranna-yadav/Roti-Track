import React, { useMemo, useRef } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { pad2, parseHM } from '@/lib/time';
import { colors } from '@/theme';

const ITEM = 62;
const VISIBLE = 5;

function Column({
  values,
  index,
  onChange,
  width,
}: {
  values: string[];
  index: number;
  onChange: (i: number) => void;
  width: number;
}) {
  const ref = useRef<ScrollView>(null);
  const initial = useRef(index * ITEM);
  return (
    <ScrollView
      ref={ref}
      style={{ width, height: ITEM * VISIBLE }}
      contentOffset={{ x: 0, y: initial.current }}
      contentContainerStyle={{ paddingVertical: ITEM * 2 }}
      showsVerticalScrollIndicator={false}
      snapToInterval={ITEM}
      decelerationRate="fast"
      onMomentumScrollEnd={(e) => {
        const i = Math.round(e.nativeEvent.contentOffset.y / ITEM);
        onChange(Math.max(0, Math.min(values.length - 1, i)));
      }}
    >
      {values.map((v, i) => (
        <View key={v} style={styles.item}>
          <Text style={[styles.itemText, i === index && styles.itemTextActive]}>{v}</Text>
        </View>
      ))}
    </ScrollView>
  );
}

/** iOS-style hour / minute / meridiem wheel, used in onboarding and pickers. */
export function TimeWheel({
  value,
  onChange,
  minuteStep = 5,
  use24h = false,
}: {
  value: string; // 'HH:mm'
  onChange: (hm: string) => void;
  minuteStep?: number;
  use24h?: boolean;
}) {
  const { h, m } = parseHM(value);

  const hours = useMemo(
    () => (use24h ? Array.from({ length: 24 }, (_, i) => pad2(i)) : Array.from({ length: 12 }, (_, i) => pad2(i + 1))),
    [use24h]
  );
  const minutes = useMemo(
    () => Array.from({ length: Math.floor(60 / minuteStep) }, (_, i) => pad2(i * minuteStep)),
    [minuteStep]
  );
  const meridiems = ['AM', 'PM'];

  const hourIndex = use24h ? h : ((h % 12 === 0 ? 12 : h % 12) - 1);
  const minuteIndex = Math.min(minutes.length - 1, Math.round(m / minuteStep));
  const meridiemIndex = h >= 12 ? 1 : 0;

  const emit = (hi: number, mi: number, pi: number) => {
    const hh = use24h ? hi : (hi + 1) % 12 + (pi === 1 ? 12 : 0);
    onChange(`${pad2(hh % 24)}:${minutes[mi]}`);
  };

  return (
    <View style={styles.wrap}>
      <View style={styles.selection} pointerEvents="none" />
      <View style={styles.row}>
        <Column
          values={hours}
          index={hourIndex}
          width={96}
          onChange={(i) => emit(i, minuteIndex, meridiemIndex)}
        />
        <Text style={styles.colon}>:</Text>
        <Column
          values={minutes}
          index={minuteIndex}
          width={96}
          onChange={(i) => emit(hourIndex, i, meridiemIndex)}
        />
        {use24h ? null : (
          <Column
            values={meridiems}
            index={meridiemIndex}
            width={90}
            onChange={(i) => emit(hourIndex, minuteIndex, i)}
          />
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { alignItems: 'center', justifyContent: 'center' },
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center' },
  item: { height: ITEM, alignItems: 'center', justifyContent: 'center' },
  itemText: { fontSize: 44, fontWeight: '800', color: '#C7CFDD', letterSpacing: -1 },
  itemTextActive: { color: colors.brand },
  colon: { fontSize: 40, fontWeight: '800', color: colors.brand, marginHorizontal: 2 },
  selection: {
    position: 'absolute',
    left: 0,
    right: 0,
    height: ITEM,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderColor: '#DCE3EF',
  },
});
