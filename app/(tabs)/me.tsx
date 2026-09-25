import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { Linking, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { AppIcon } from '@/components/Logo';
import { SoundSheet } from '@/components/SoundSheet';
import { ArrowIcon } from '@/components/TabIcons';
import { BlueScreen, CircleButton, GlassCard, Row, SectionTitle, ToggleRow } from '@/components/ui';
import { reminderTimes } from '@/lib/notifications';
import { timeFormatLabel } from '@/lib/time';
import { formatVolume, unitsLabel } from '@/lib/units';
import { useDiet } from '@/store/DietProvider';
import { useHydration } from '@/store/HydrationProvider';
import { colors, radius, type } from '@/theme';

const VERSION = '1.0.0';

export default function MeScreen() {
  const { settings, todayMl, streak, updateSettings } = useHydration();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { profile } = useDiet();
  const [soundOpen, setSoundOpen] = useState(false);

  const reminderSubtitle = settings.reminderEnabled
    ? `${reminderTimes(settings).length} reminders · ${
        settings.reminderMode === 'custom'
          ? 'Custom'
          : settings.reminderMode === 'interval'
            ? `Every ${settings.intervalMinutes} min`
            : 'Smart'
      }`
    : 'Reminder Off';

  return (
    <BlueScreen>
      <ScrollView
        contentContainerStyle={{
          paddingHorizontal: 16,
          paddingTop: insets.top + 8,
          paddingBottom: insets.bottom + 110,
        }}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.header}>
          <View style={styles.avatar}>
            <AppIcon size={46} radius={14} />
          </View>
          <Text style={styles.appName}>Sipwell</Text>
          <View style={{ flex: 1 }} />
          <Pressable style={styles.crown} onPress={() => router.push('/pro')}>
            <Text style={styles.crownGlyph}>👑</Text>
          </Pressable>
        </View>

        <View style={styles.statRow}>
          <Pressable style={styles.statCard} onPress={() => router.push('/(tabs)/history')}>
            <View style={styles.statTop}>
              <Text style={styles.statIcon}>💧</Text>
              <CircleButton size={30} tint="rgba(255,255,255,0.18)">
                <ArrowIcon color="#FFFFFF" />
              </CircleButton>
            </View>
            <View style={styles.statValueRow}>
              <Text style={styles.statValue}>
                {settings.unit === 'metric' ? (todayMl / 1000).toFixed(1) : Math.round(todayMl / 29.5735)}
              </Text>
              <Text style={styles.statUnit}>{settings.unit === 'metric' ? 'L' : 'oz'}</Text>
            </View>
            <Text style={styles.statLabel}>Total Drinking</Text>
          </Pressable>

          <Pressable style={styles.statCard} onPress={() => router.push('/(tabs)/history')}>
            <View style={styles.statTop}>
              <Text style={styles.statIcon}>🔥</Text>
              <CircleButton size={30} tint="rgba(255,255,255,0.18)">
                <ArrowIcon color="#FFFFFF" />
              </CircleButton>
            </View>
            <View style={styles.statValueRow}>
              <Text style={styles.statValue}>{streak}</Text>
              <Text style={styles.statUnit}>Days</Text>
            </View>
            <Text style={styles.statLabel}>Streak</Text>
          </Pressable>
        </View>

        <SectionTitle>Diet & Calories</SectionTitle>
        <GlassCard>
          <Row
            icon={<Text style={styles.rowIcon}>🎯</Text>}
            title="Diet Profile"
            subtitle={profile.configured ? undefined : 'Personalise your calorie target'}
            badge={!profile.configured}
            onPress={() => router.push('/diet-profile')}
          />
          <Row
            icon={<Text style={styles.rowIcon}>🔥</Text>}
            title="Calorie Goal"
            value={`${profile.calorieGoal} kcal`}
            onPress={() => router.push('/diet-profile')}
          />
          <Row
            icon={<Text style={styles.rowIcon}>🍱</Text>}
            title="My Meal Plan"
            onPress={() => router.push('/(tabs)/plan')}
          />
          <Row
            icon={<Text style={styles.rowIcon}>📚</Text>}
            title="Health Articles"
            onPress={() => router.push('/insights')}
            last
          />
        </GlassCard>

        <SectionTitle>Reminder Settings</SectionTitle>
        <GlassCard>
          <Row
            icon={<Text style={styles.rowIcon}>🔔</Text>}
            title="Reminders"
            subtitle={reminderSubtitle}
            badge={!settings.reminderEnabled}
            onPress={() => router.push('/reminders')}
          />
          <Row
            icon={<Text style={styles.rowIcon}>🔊</Text>}
            title="Sounds & Effects"
            onPress={() => setSoundOpen(true)}
          />
          <ToggleRow
            icon={<Text style={styles.rowIcon}>⏩</Text>}
            title="Smart Skip"
            subtitle="Skip a nudge if you just drank"
            value={settings.smartSkip}
            onChange={(v) => updateSettings({ smartSkip: v })}
          />
          <ToggleRow
            icon={<Text style={styles.rowIcon}>🏆</Text>}
            title="Stop when goal achieved"
            value={settings.stopWhenGoalAchieved}
            onChange={(v) => updateSettings({ stopWhenGoalAchieved: v })}
            last
          />
        </GlassCard>

        <SectionTitle>Drink</SectionTitle>
        <GlassCard>
          <Row
            icon={<Text style={styles.rowIcon}>💧</Text>}
            title="Daily Goal"
            value={formatVolume(settings.goalMl, settings.unit)}
            onPress={() => router.push('/goal')}
          />
          <Row
            icon={<Text style={styles.rowIcon}>🧍</Text>}
            title="Gender & Weight"
            onPress={() => router.push('/body')}
          />
          <Row
            icon={<Text style={styles.rowIcon}>🍸</Text>}
            title="Drinks"
            badge={!settings.pro}
            onPress={() => router.push('/drinks')}
            last
          />
        </GlassCard>

        <SectionTitle>General</SectionTitle>
        <GlassCard>
          <Row
            icon={<Text style={styles.rowIcon}>🥛</Text>}
            title="Units"
            value={unitsLabel(settings)}
            onPress={() => router.push('/units')}
          />
          <Row
            icon={<Text style={styles.rowIcon}>📅</Text>}
            title="First Day Of Week"
            value={settings.firstDayOfWeek === 0 ? 'Sunday' : 'Monday'}
            onPress={() => updateSettings({ firstDayOfWeek: settings.firstDayOfWeek === 0 ? 1 : 0 })}
          />
          <Row
            icon={<Text style={styles.rowIcon}>🕐</Text>}
            title="A Day Starts At"
            value={settings.dayStartsAt}
            onPress={() => router.push('/day-start')}
          />
          <Row
            icon={<Text style={styles.rowIcon}>⏰</Text>}
            title="Time Format"
            value={timeFormatLabel(settings.timeFormat)}
            onPress={() =>
              updateSettings({
                timeFormat:
                  settings.timeFormat === 'system' ? '12h' : settings.timeFormat === '12h' ? '24h' : 'system',
              })
            }
          />
          <Row
            icon={<Text style={styles.rowIcon}>🌐</Text>}
            title="Language Options"
            value={settings.language}
            last
          />
        </GlassCard>

        <SectionTitle>Support</SectionTitle>
        <GlassCard>
          <Row
            icon={<Text style={styles.rowIcon}>✍️</Text>}
            title="Feedback"
            onPress={() => Linking.openURL('mailto:hello@sipwell.app?subject=Sipwell%20feedback')}
          />
          <Row icon={<Text style={styles.rowIcon}>⭐</Text>} title="Rate Us" onPress={() => router.push('/rate')} />
          <Row icon={<Text style={styles.rowIcon}>🛡️</Text>} title="Privacy Policy" onPress={() => router.push('/legal?doc=privacy')} />
          <Row icon={<Text style={styles.rowIcon}>📄</Text>} title="Terms of use" onPress={() => router.push('/legal?doc=terms')} last />
        </GlassCard>

        <Text style={styles.version}>Version {VERSION}</Text>
      </ScrollView>

      <SoundSheet visible={soundOpen} onClose={() => setSoundOpen(false)} />
    </BlueScreen>
  );
}

const styles = StyleSheet.create({
  header: { flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 20 },
  avatar: {
    width: 52,
    height: 52,
    borderRadius: 26,
    backgroundColor: 'rgba(255,255,255,0.16)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  appName: { fontSize: 26, fontWeight: '800', color: '#FFFFFF' },
  crown: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(255,255,255,0.18)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  crownGlyph: { fontSize: 20 },
  statRow: { flexDirection: 'row', gap: 12 },
  statCard: { flex: 1, backgroundColor: colors.glass, borderRadius: radius.lg, padding: 16 },
  statTop: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  statIcon: { fontSize: 24 },
  statValueRow: { flexDirection: 'row', alignItems: 'flex-end', gap: 4, marginTop: 14 },
  statValue: { fontSize: 34, fontWeight: '800', color: '#FFFFFF', letterSpacing: -1 },
  statUnit: { fontSize: 18, fontWeight: '700', color: '#FFFFFF', marginBottom: 4 },
  statLabel: { ...type.small, color: colors.mutedOnBlue, marginTop: 2 },
  rowIcon: { fontSize: 18 },
  version: { ...type.small, color: 'rgba(255,255,255,0.5)', textAlign: 'center', marginTop: 30 },
});
