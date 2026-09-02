import React, { useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { HydrationChart, type Point } from '@/components/charts';
import { DrinkSheet } from '@/components/DrinkSheet';
import { PencilIcon, PlusIcon, TrashIcon } from '@/components/TabIcons';
import { BlueScreen, GlassCard, Segmented } from '@/components/ui';
import { drinkById } from '@/data/drinks';
import {
  addDays, dateToKey, daysInMonth, formatMonthLabel, formatWeekLabel, keyToDate,
  MONTHS, startOfDay, startOfMonth, startOfWeek, WEEKDAY_LETTERS,
} from '@/lib/date';
import { tapFeedback } from '@/lib/feedback';
import { formatClock } from '@/lib/time';
import { formatVolume, formatVolumeShort, toDisplayVolume, volumeUnit } from '@/lib/units';
import { useHydration } from '@/store/HydrationProvider';
import { colors, radius, type } from '@/theme';

type Range = 'day' | 'week' | 'month';

export default function HistoryScreen() {
  const { settings, entriesForDay, totalForDayKey, removeEntry, effectiveMl } = useHydration();
  const insets = useSafeAreaInsets();

  const [range, setRange] = useState<Range>('day');
  /** How many periods back from today we are looking. */
  const [offset, setOffset] = useState(0);
  const [adding, setAdding] = useState(false);
  const [editing, setEditing] = useState<{ id: string; amountMl: number; drinkId: string } | null>(null);

  const unit = settings.unit;
  const toDisp = (ml: number) => toDisplayVolume(ml, unit);
  const goalDisp = toDisp(settings.goalMl);

  // Chart axis tops out just above the goal so the dashed line always sits inside.
  const view = useMemo(() => buildView(range, offset), [range, offset]);

  const { points, total, average, label, xTicks, xMin, xMax, variant } = useMemo(() => {
    if (range === 'day') {
      const key = dateToKey(view.start);
      const dayEntries = entriesForDay(key);
      const pts: Point[] = dayEntries.map((e) => {
        const d = new Date(e.ts);
        return {
          x: d.getHours() + d.getMinutes() / 60,
          value: toDisp(effectiveMl(e)),
          label: formatClock(d, settings.timeFormat),
        };
      });
      const sum = totalForDayKey(key);
      return {
        points: pts,
        total: sum,
        average: sum,
        label: offset === 0 ? 'Today' : offset === 1 ? 'Yesterday' : `${MONTHS[view.start.getMonth()]} ${view.start.getDate()}`,
        xTicks: [0, 4, 8, 12, 16, 20, 24].map((h) => ({ at: h, label: String(h) })),
        xMin: 0,
        xMax: 24,
        variant: 'scatter' as const,
      };
    }

    if (range === 'week') {
      const days = Array.from({ length: 7 }, (_, i) => addDays(view.start, i));
      const totals = days.map((d) => totalForDayKey(dateToKey(d)));
      const pts: Point[] = days.map((d, i) => ({
        x: i,
        value: toDisp(totals[i]),
        label: WEEKDAY_LETTERS[d.getDay()],
      }));
      const sum = totals.reduce((a, b) => a + b, 0);
      const logged = totals.filter((t) => t > 0).length;
      return {
        points: pts,
        total: sum,
        average: logged ? sum / logged : 0,
        label: formatWeekLabel(view.start),
        xTicks: days.map((d, i) => ({ at: i, label: WEEKDAY_LETTERS[d.getDay()] })),
        xMin: -0.5,
        xMax: 6.5,
        variant: 'bar' as const,
      };
    }

    const n = daysInMonth(view.start);
    const totals = Array.from({ length: n }, (_, i) =>
      totalForDayKey(dateToKey(new Date(view.start.getFullYear(), view.start.getMonth(), i + 1)))
    );
    const pts: Point[] = totals.map((ml, i) => ({
      x: i + 1,
      value: toDisp(ml),
      label: String(i + 1),
      caption: `Day ${i + 1}`,
    }));
    const sum = totals.reduce((a, b) => a + b, 0);
    const logged = totals.filter((t) => t > 0).length;
    return {
      points: pts,
      total: sum,
      average: logged ? sum / logged : 0,
      label: formatMonthLabel(view.start),
      xTicks: [1, 6, 11, 16, 21, 26, 31].filter((d) => d <= n).map((d) => ({ at: d, label: String(d) })),
      xMin: 0.5,
      xMax: n + 0.5,
      variant: 'bar' as const,
    };
  }, [range, view.start, offset, entriesForDay, totalForDayKey, effectiveMl, settings.timeFormat, unit]);

  const peak = Math.max(goalDisp, ...points.map((p) => p.value));
  const yMax = niceMax(peak * 1.25);
  const yTicks = Array.from({ length: 5 }, (_, i) => trim(((yMax / 4) * i).toFixed(2)));

  const records = range === 'day' ? entriesForDay(dateToKey(view.start)) : [];

  return (
    <BlueScreen>
      <View style={{ paddingTop: insets.top + 6 }}>
        <Segmented
          options={[
            { key: 'day', label: 'DAY' },
            { key: 'week', label: 'WEEK' },
            { key: 'month', label: 'MONTH' },
          ]}
          value={range}
          onChange={(k) => {
            setRange(k as Range);
            setOffset(0);
          }}
        />
      </View>

      <ScrollView
        contentContainerStyle={{ paddingHorizontal: 16, paddingBottom: insets.bottom + 110 }}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.periodRow}>
          <Pressable hitSlop={16} onPress={() => setOffset((o) => o + 1)}>
            <Text style={styles.arrow}>◀</Text>
          </Pressable>
          <Text style={styles.periodLabel}>{label}</Text>
          <Pressable hitSlop={16} disabled={offset === 0} onPress={() => setOffset((o) => Math.max(0, o - 1))}>
            <Text style={[styles.arrow, offset === 0 && { opacity: 0.25 }]}>▶</Text>
          </Pressable>
        </View>

        <GlassCard style={{ paddingVertical: 18 }}>
          <View style={styles.totalRow}>
            <View>
              <Text style={styles.totalLabel}>Total</Text>
              <Text style={styles.totalValue}>{formatVolume(total, unit)}</Text>
            </View>
            <View style={{ alignItems: 'flex-end' }}>
              <Text style={styles.totalLabelMuted}>{range === 'day' ? 'Goal' : 'Average'}</Text>
              <Text style={styles.totalValueMuted}>
                {formatVolume(range === 'day' ? settings.goalMl : average, unit)}
              </Text>
            </View>
          </View>

          <HydrationChart
            points={points}
            goal={goalDisp}
            yMax={yMax}
            yTicks={yTicks}
            xTicks={xTicks}
            xMin={xMin}
            xMax={xMax}
            unitLabel={volumeUnit(unit)}
            variant={variant}
            formatValue={(v) => `${range === 'day' ? '+' : ''}${trim(v.toFixed(2))} ${volumeUnit(unit)}`}
          />
        </GlassCard>

        {range === 'day' ? (
          <>
            <View style={styles.recordsHeader}>
              <Text style={styles.recordsTitle}>Records</Text>
              <Pressable onPress={() => setAdding(true)} hitSlop={12}>
                <PlusIcon color="#FFFFFF" size={26} />
              </Pressable>
            </View>

            {records.length === 0 ? (
              <GlassCard style={styles.empty}>
                <Text style={styles.emptyText}>No drinks logged yet. Tap + to add one.</Text>
              </GlassCard>
            ) : (
              records.map((e) => {
                const d = drinkById(e.drinkId);
                return (
                  <GlassCard key={e.id} style={styles.record}>
                    <Text style={styles.recordEmoji}>{d.emoji}</Text>
                    <View style={{ flex: 1 }}>
                      <Text style={styles.recordValue}>{formatVolumeShort(e.amountMl, unit)}</Text>
                      <Text style={styles.recordTime}>
                        {formatClock(new Date(e.ts), settings.timeFormat)}
                        {d.hydration !== 1 ? ` · ${d.name}` : ''}
                      </Text>
                    </View>
                    <Pressable
                      hitSlop={10}
                      style={styles.recordAction}
                      onPress={() => setEditing({ id: e.id, amountMl: e.amountMl, drinkId: e.drinkId })}
                    >
                      <PencilIcon color="#FFFFFF" size={20} />
                    </Pressable>
                    <Pressable
                      hitSlop={10}
                      style={styles.recordAction}
                      onPress={() => {
                        tapFeedback(settings);
                        removeEntry(e.id);
                      }}
                    >
                      <TrashIcon color="#FFFFFF" size={20} />
                    </Pressable>
                  </GlassCard>
                );
              })
            )}
          </>
        ) : null}
      </ScrollView>

      <DrinkSheet visible={adding} onClose={() => setAdding(false)} />
      <DrinkSheet
        visible={editing !== null}
        onClose={() => setEditing(null)}
        editingId={editing?.id}
        initial={editing ? { amountMl: editing.amountMl, drinkId: editing.drinkId } : undefined}
      />
    </BlueScreen>
  );
}

/** Start date for the visible period, `offset` periods back from now. */
function buildView(range: Range, offset: number) {
  const today = startOfDay(new Date());
  if (range === 'day') return { start: addDays(today, -offset) };
  if (range === 'week') return { start: addDays(startOfWeek(today, 0), -7 * offset) };
  const m = startOfMonth(today);
  return { start: new Date(m.getFullYear(), m.getMonth() - offset, 1) };
}

/** Rounds an axis top to a friendly number so the tick labels stay readable. */
function niceMax(v: number): number {
  if (v <= 0) return 1;
  const mag = Math.pow(10, Math.floor(Math.log10(v)));
  const norm = v / mag;
  const step = norm <= 1.2 ? 1.2 : norm <= 2 ? 2 : norm <= 2.5 ? 2.5 : norm <= 5 ? 5 : 10;
  return step * mag;
}

const trim = (s: string) => (s.includes('.') ? s.replace(/0+$/, '').replace(/\.$/, '') : s);

const styles = StyleSheet.create({
  periodRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 18,
    paddingHorizontal: 6,
  },
  arrow: { color: 'rgba(255,255,255,0.75)', fontSize: 20 },
  periodLabel: { ...type.h2, color: '#FFFFFF' },
  totalRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 22 },
  totalLabel: { ...type.title, color: '#FFFFFF' },
  totalValue: { fontSize: 40, fontWeight: '800', color: '#FFFFFF', letterSpacing: -1 },
  totalLabelMuted: { ...type.title, color: colors.mutedOnBlue },
  totalValueMuted: { fontSize: 28, fontWeight: '800', color: '#FFFFFF' },
  recordsHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 26,
    marginBottom: 12,
    paddingHorizontal: 4,
  },
  recordsTitle: { ...type.h2, color: '#FFFFFF' },
  record: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 14,
    marginBottom: 10,
    borderRadius: radius.lg,
  },
  recordEmoji: { fontSize: 26 },
  recordValue: { ...type.h2, fontSize: 20, color: '#FFFFFF' },
  recordTime: { ...type.small, color: colors.mutedOnBlue, marginTop: 2 },
  recordAction: { padding: 6 },
  empty: { paddingVertical: 26, alignItems: 'center', borderRadius: radius.lg },
  emptyText: { ...type.body, color: colors.mutedOnBlue },
});
