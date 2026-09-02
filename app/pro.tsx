import { LinearGradient } from 'expo-linear-gradient';
import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { DropMark } from '@/components/Logo';
import { PrimaryButton } from '@/components/ui';
import { tapFeedback } from '@/lib/feedback';
import { useHydration } from '@/store/HydrationProvider';
import { colors, radius, shadow, type } from '@/theme';

const PERKS = [
  { icon: '🚫', title: 'Ad-Free Experience', body: 'No interruptions. Stay focused.' },
  { icon: '🥤', title: 'Unlock 13 Premium Drinks', body: 'Log juice, milk, beer and more with the right hydration weighting.' },
  { icon: '📊', title: 'Full History & Insights', body: 'Every day you have tracked, not just the last week.' },
  { icon: '✨', title: 'More Features', body: 'Enjoy exclusive benefits, with more coming soon.' },
];

const PLANS = [
  { id: 'lifetime', title: 'Lifetime', blurb: 'One-time payment, lifetime access.', price: '₹290.00', ribbon: '98% Positive Reviews' },
  { id: 'yearly', title: 'Yearly', blurb: 'Billed once a year.', price: '₹190.00', ribbon: 'Best Value' },
];

export default function ProScreen() {
  const { settings, updateSettings } = useHydration();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const [plan, setPlan] = useState(PLANS[0].id);

  /**
   * There is no billing SDK wired up in this build, so purchase and restore
   * flip the local entitlement and say so plainly.
   */
  const subscribe = () => {
    tapFeedback(settings, 'success');
    updateSettings({ pro: true });
    Alert.alert(
      'Sipwell Pro unlocked',
      'This build has no payment provider connected, so Pro has been unlocked locally for you to try.',
      [{ text: 'Nice', onPress: () => router.back() }]
    );
  };

  return (
    <LinearGradient colors={['#2E7BFF', '#1B4FF0', '#0E32C4']} style={{ flex: 1 }}>
      <View style={[styles.topBar, { paddingTop: insets.top + 8 }]}>
        <Pressable style={styles.restore} onPress={() => Alert.alert('Restore', 'No previous purchase found on this device.')}>
          <Text style={styles.restoreText}>Restore</Text>
        </Pressable>
        <Pressable style={styles.close} onPress={() => router.back()} hitSlop={12}>
          <Text style={styles.closeGlyph}>✕</Text>
        </Pressable>
      </View>

      <ScrollView
        contentContainerStyle={{ paddingHorizontal: 22, paddingBottom: insets.bottom + 32 }}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.badge}>
          <DropMark size={92} />
          <View style={styles.proTag}>
            <Text style={styles.proTagText}>PRO</Text>
          </View>
        </View>

        <Text style={styles.title}>Unlimited Access</Text>

        <View style={{ marginTop: 26 }}>
          {PERKS.map((p, i) => (
            <View key={p.title} style={styles.perkRow}>
              <View style={styles.perkIcon}>
                <Text style={{ fontSize: 18 }}>{p.icon}</Text>
              </View>
              <View style={{ flex: 1 }}>
                <Text style={styles.perkTitle}>{p.title}</Text>
                <Text style={styles.perkBody}>{p.body}</Text>
              </View>
              {i < PERKS.length - 1 ? <View style={styles.perkConnector} /> : null}
            </View>
          ))}
        </View>

        <View style={{ marginTop: 26, gap: 12 }}>
          {PLANS.map((p) => {
            const active = plan === p.id;
            return (
              <Pressable key={p.id} onPress={() => setPlan(p.id)} style={[styles.plan, active && styles.planActive]}>
                {p.ribbon ? (
                  <View style={styles.ribbon}>
                    <Text style={styles.ribbonText}>{p.ribbon}</Text>
                  </View>
                ) : null}
                <View style={{ flex: 1 }}>
                  <Text style={styles.planTitle}>{p.title}</Text>
                  <Text style={styles.planBlurb}>{p.blurb}</Text>
                </View>
                <Text style={styles.planPrice}>{p.price}</Text>
              </Pressable>
            );
          })}
        </View>

        <PrimaryButton
          label={settings.pro ? 'Pro is active' : 'Subscribe'}
          onPress={subscribe}
          disabled={settings.pro}
          style={{ marginTop: 24 }}
        />

        <Text style={styles.legal}>
          By continuing you agree to our Terms of use and Privacy policy. Subscriptions renew
          automatically unless cancelled at least 24 hours before the period ends.
        </Text>
      </ScrollView>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  topBar: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 20, paddingBottom: 8 },
  restore: {
    backgroundColor: 'rgba(255,255,255,0.2)',
    paddingHorizontal: 18,
    height: 38,
    borderRadius: radius.pill,
    justifyContent: 'center',
  },
  restoreText: { ...type.small, color: '#FFFFFF' },
  close: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: 'rgba(255,255,255,0.2)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  closeGlyph: { color: '#FFFFFF', fontSize: 17, fontWeight: '700' },
  badge: { alignItems: 'center', marginTop: 14 },
  proTag: {
    marginTop: 10,
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 20,
    paddingVertical: 6,
    borderRadius: radius.pill,
    ...shadow.card,
  },
  proTagText: { fontSize: 18, fontWeight: '900', color: colors.brand, letterSpacing: 1 },
  title: { fontSize: 38, fontWeight: '900', color: '#FFFFFF', textAlign: 'center', marginTop: 20 },
  perkRow: { flexDirection: 'row', gap: 14, alignItems: 'flex-start', marginBottom: 20 },
  perkIcon: {
    width: 42,
    height: 42,
    borderRadius: 21,
    backgroundColor: 'rgba(255,255,255,0.9)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  perkConnector: {
    position: 'absolute',
    left: 20,
    top: 42,
    width: 2,
    height: 22,
    backgroundColor: 'rgba(255,255,255,0.35)',
  },
  perkTitle: { ...type.title, color: '#FFFFFF' },
  perkBody: { ...type.small, color: 'rgba(255,255,255,0.78)', marginTop: 3, lineHeight: 18 },
  plan: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(255,255,255,0.9)',
    borderRadius: radius.lg,
    padding: 18,
    borderWidth: 3,
    borderColor: 'transparent',
  },
  planActive: { backgroundColor: '#FFFFFF', borderColor: '#FFD84D' },
  ribbon: {
    position: 'absolute',
    top: -12,
    alignSelf: 'center',
    left: 0,
    right: 0,
    marginHorizontal: 'auto',
    backgroundColor: '#FF3B57',
    paddingHorizontal: 14,
    paddingVertical: 4,
    borderRadius: radius.pill,
    alignItems: 'center',
    maxWidth: 200,
  },
  ribbonText: { ...type.tiny, color: '#FFFFFF' },
  planTitle: { ...type.h2, color: colors.ink },
  planBlurb: { ...type.small, color: colors.muted, marginTop: 3 },
  planPrice: { fontSize: 24, fontWeight: '900', color: colors.ink },
  legal: {
    ...type.small,
    color: 'rgba(255,255,255,0.7)',
    textAlign: 'center',
    lineHeight: 19,
    marginTop: 18,
  },
});
