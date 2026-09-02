import React, { useMemo, useState } from 'react';
import { Alert, Platform, Pressable, ScrollView, StyleSheet, Switch, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Sheet } from '@/components/Sheet';
import { SubHeader } from '@/components/SubHeader';
import { TimeWheel } from '@/components/TimeWheel';
import { BlueScreen, PrimaryButton } from '@/components/ui';
import { tapFeedback } from '@/lib/feedback';
import { ensurePermission, reminderTimes } from '@/lib/notifications';
import { formatTime } from '@/lib/time';
import { useHydration } from '@/store/HydrationProvider';
import type { ReminderMode } from '@/lib/types';
import { colors, radius, shadow, type } from '@/theme';

const MODES: { key: ReminderMode; title: string; blurb: string }[] = [
  { key: 'smart', title: 'Smart Mode', blurb: 'Sipwell spreads reminders across your waking hours' },
  { key: 'interval', title: 'Interval Mode', blurb: 'A steady nudge every so often' },
  { key: 'custom', title: 'Custom Mode', blurb: 'Customize all reminders by yourself' },
];

const INTERVALS = [30, 45, 60, 90, 120, 180];

export default function RemindersScreen() {
  const { settings, updateSettings } = useHydration();
  const insets = useSafeAreaInsets();
  const [addingTime, setAddingTime] = useState<string | null>(null);

  const times = useMemo(() => reminderTimes(settings), [settings]);

  const toggleEnabled = async (v: boolean) => {
    if (v && Platform.OS !== 'web') {
      const granted = await ensurePermission();
      if (!granted) {
        Alert.alert(
          'Notifications are off',
          'Turn on notifications for Sipwell in your device settings so reminders can reach you.'
        );
      }
    }
    updateSettings({ reminderEnabled: v });
  };

  const toggleCustomTime = (t: string) => {
    tapFeedback(settings);
    const muted = settings.disabledCustomTimes.includes(t)
      ? settings.disabledCustomTimes.filter((x) => x !== t)
      : [...settings.disabledCustomTimes, t];
    updateSettings({ disabledCustomTimes: muted });
  };

  const removeCustomTime = (t: string) => {
    updateSettings({
      customTimes: settings.customTimes.filter((x) => x !== t),
      disabledCustomTimes: settings.disabledCustomTimes.filter((x) => x !== t),
    });
  };

  const addCustomTime = (t: string) => {
    if (settings.customTimes.includes(t)) return;
    updateSettings({ customTimes: [...settings.customTimes, t].sort() });
  };

  return (
    <BlueScreen>
      <SubHeader
        title="Reminder"
        right={
          <Switch
            value={settings.reminderEnabled}
            onValueChange={toggleEnabled}
            trackColor={{ false: 'rgba(255,255,255,0.28)', true: '#2E8BFF' }}
            thumbColor="#FFFFFF"
          />
        }
      />

      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        style={styles.modeScroller}
        contentContainerStyle={styles.modeStrip}
      >
        {MODES.map((m) => {
          const active = settings.reminderMode === m.key;
          return (
            <Pressable
              key={m.key}
              onPress={() => updateSettings({ reminderMode: m.key })}
              style={[styles.modeCard, active && styles.modeCardActive]}
            >
              <Text style={[styles.modeTitle, active && styles.modeTitleActive]}>{m.title}</Text>
              <View style={[styles.modeCheck, active && styles.modeCheckActive]}>
                <Text style={styles.modeCheckGlyph}>✓</Text>
              </View>
            </Pressable>
          );
        })}
      </ScrollView>

      <View style={[styles.panel, { paddingBottom: insets.bottom + 24 }]}>
        <ScrollView showsVerticalScrollIndicator={false}>
          <View style={styles.panelHead}>
            <Text style={styles.panelTitle}>
              {MODES.find((m) => m.key === settings.reminderMode)?.title.replace(' Mode', '')}
            </Text>
            <Text style={styles.panelInfo}>ⓘ</Text>
          </View>
          <Text style={styles.panelBlurb}>
            {MODES.find((m) => m.key === settings.reminderMode)?.blurb}
          </Text>

          {settings.reminderMode === 'interval' ? (
            <View style={styles.chipWrap}>
              {INTERVALS.map((mins) => {
                const active = settings.intervalMinutes === mins;
                return (
                  <Pressable
                    key={mins}
                    onPress={() => {
                      tapFeedback(settings);
                      updateSettings({ intervalMinutes: mins });
                    }}
                    style={[styles.chip, active && styles.chipActive]}
                  >
                    <Text style={[styles.chipText, active && styles.chipTextActive]}>
                      {mins < 60 ? `${mins} min` : `${mins / 60} h`}
                    </Text>
                  </Pressable>
                );
              })}
            </View>
          ) : null}

          {settings.reminderMode === 'custom' ? (
            <View style={styles.chipWrap}>
              {settings.customTimes.map((t) => {
                const muted = settings.disabledCustomTimes.includes(t);
                return (
                  <Pressable
                    key={t}
                    onPress={() => toggleCustomTime(t)}
                    onLongPress={() => removeCustomTime(t)}
                    style={[styles.chip, !muted && styles.chipActive]}
                  >
                    <Text style={[styles.chipText, !muted && styles.chipTextActive]}>
                      {formatTime(t, settings.timeFormat)}
                    </Text>
                  </Pressable>
                );
              })}
              <Pressable style={styles.addChip} onPress={() => setAddingTime('09:00')}>
                <Text style={styles.addGlyph}>+</Text>
              </Pressable>
            </View>
          ) : null}

          {settings.reminderMode === 'smart' ? (
            <View style={styles.chipWrap}>
              {times.map((t) => (
                <View key={t} style={[styles.chip, styles.chipActive]}>
                  <Text style={[styles.chipText, styles.chipTextActive]}>
                    {formatTime(t, settings.timeFormat)}
                  </Text>
                </View>
              ))}
            </View>
          ) : null}

          <Text style={styles.helpLink}>Can't receive reminders?</Text>
          <Text style={styles.helpBody}>
            Reminders need notification permission. On Android, also allow Sipwell to run in the
            background so nudges arrive on time.
          </Text>

          <View style={styles.divider} />

          <View style={styles.weekendRow}>
            <Text style={styles.weekendLabel}>Reminder Weekend Mode</Text>
            <Switch
              value={settings.weekendMode}
              onValueChange={(v) => updateSettings({ weekendMode: v })}
              trackColor={{ false: '#D7DEEA', true: '#2E8BFF' }}
              thumbColor="#FFFFFF"
            />
          </View>
          <Text style={styles.weekendHint}>Pause reminders on Saturday and Sunday.</Text>

          <Text style={styles.summary}>
            {settings.reminderEnabled
              ? `${times.length} reminder${times.length === 1 ? '' : 's'} scheduled between ${formatTime(
                  settings.wakeTime,
                  settings.timeFormat
                )} and ${formatTime(settings.sleepTime, settings.timeFormat)}.`
              : 'Reminders are currently off.'}
          </Text>
        </ScrollView>
      </View>

      <Sheet visible={addingTime !== null} onClose={() => setAddingTime(null)} title="Add a reminder">
        {addingTime !== null ? (
          <>
            <TimeWheel
              value={addingTime}
              onChange={setAddingTime}
              minuteStep={5}
              use24h={settings.timeFormat === '24h'}
            />
            <PrimaryButton
              label="Add"
              onPress={() => {
                addCustomTime(addingTime);
                setAddingTime(null);
              }}
              style={{ marginTop: 16 }}
            />
          </>
        ) : null}
      </Sheet>
    </BlueScreen>
  );
}

const styles = StyleSheet.create({
  // A horizontal scroller inside a column would otherwise stretch to fill it.
  modeScroller: { flexGrow: 0, flexShrink: 0 },
  modeStrip: { paddingHorizontal: 16, gap: 12, paddingBottom: 18 },
  modeCard: {
    width: 178,
    height: 120,
    borderRadius: radius.lg,
    backgroundColor: 'rgba(255,255,255,0.16)',
    padding: 16,
    justifyContent: 'space-between',
  },
  modeCardActive: { backgroundColor: '#FFFFFF', ...shadow.card },
  modeTitle: { fontSize: 22, fontWeight: '800', color: 'rgba(255,255,255,0.75)', lineHeight: 26 },
  modeTitleActive: { color: colors.brand },
  modeCheck: {
    alignSelf: 'flex-end',
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: 'rgba(255,255,255,0.22)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  modeCheckActive: { backgroundColor: '#0A93FF' },
  modeCheckGlyph: { color: '#FFFFFF', fontSize: 14, fontWeight: '800' },
  panel: {
    flex: 1,
    backgroundColor: '#FFFFFF',
    borderTopLeftRadius: 30,
    borderTopRightRadius: 30,
    paddingHorizontal: 20,
    paddingTop: 22,
  },
  panelHead: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  panelTitle: { ...type.h2, color: colors.ink },
  panelInfo: { color: colors.muted, fontSize: 16 },
  panelBlurb: { ...type.body, color: colors.muted, marginTop: 4, marginBottom: 18 },
  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  chip: {
    paddingHorizontal: 18,
    height: 46,
    borderRadius: radius.pill,
    backgroundColor: '#EFF2F7',
    alignItems: 'center',
    justifyContent: 'center',
  },
  chipActive: { backgroundColor: colors.brand },
  chipText: { fontSize: 16, fontWeight: '800', color: '#9AA6BA' },
  chipTextActive: { color: '#FFFFFF' },
  addChip: {
    width: 84,
    height: 46,
    borderRadius: radius.pill,
    backgroundColor: '#EFF2F7',
    alignItems: 'center',
    justifyContent: 'center',
  },
  addGlyph: { fontSize: 22, color: colors.inkSoft, marginTop: -2 },
  helpLink: {
    ...type.body,
    color: colors.muted,
    textDecorationLine: 'underline',
    textAlign: 'center',
    marginTop: 24,
  },
  helpBody: { ...type.small, color: '#A9B4C6', textAlign: 'center', marginTop: 8, lineHeight: 18 },
  divider: { height: StyleSheet.hairlineWidth, backgroundColor: colors.divider, marginVertical: 22 },
  weekendRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#F4F7FC',
    borderRadius: radius.lg,
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  weekendLabel: { ...type.h2, fontSize: 19, color: colors.ink },
  weekendHint: { ...type.small, color: colors.muted, marginTop: 8, marginLeft: 4 },
  summary: { ...type.small, color: colors.muted, marginTop: 22, marginBottom: 20, lineHeight: 19 },
});
