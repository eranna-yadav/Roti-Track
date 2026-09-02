import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';
import { LightScreen, PrimaryButton } from '@/components/ui';
import { tapFeedback } from '@/lib/feedback';
import { useHydration } from '@/store/HydrationProvider';
import { colors, type } from '@/theme';

const BLURBS = [
  'Sorry to hear that',
  'We can do better',
  'Thanks — what would help?',
  'Glad it is working for you',
  'The best we could have',
];

export default function RateScreen() {
  const { settings } = useHydration();
  const router = useRouter();
  const [stars, setStars] = useState(0);

  return (
    <LightScreen tint="rgba(9,14,28,0.55)">
      <Pressable style={{ flex: 1 }} onPress={() => router.back()} />
      <View style={styles.card}>
        <Text style={styles.mascot}>🌟</Text>
        <Text style={styles.title}>Thanks for using Sipwell!</Text>
        <Text style={styles.body}>We really appreciate you taking the time to rate us.</Text>

        {stars > 0 ? (
          <View style={styles.bubble}>
            <Text style={styles.bubbleText}>{BLURBS[stars - 1]}</Text>
          </View>
        ) : null}

        <View style={styles.stars}>
          {[1, 2, 3, 4, 5].map((n) => (
            <Pressable
              key={n}
              hitSlop={6}
              onPress={() => {
                tapFeedback(settings);
                setStars(n);
              }}
            >
              <Text style={[styles.star, n <= stars && styles.starActive]}>{n <= stars ? '★' : '☆'}</Text>
            </Pressable>
          ))}
        </View>

        <PrimaryButton
          label="Rate"
          disabled={stars === 0}
          onPress={() => {
            Alert.alert('Thank you!', 'Your rating helps other people find Sipwell.', [
              { text: 'Close', onPress: () => router.back() },
            ]);
          }}
          style={{ alignSelf: 'stretch', marginTop: 22 }}
        />
      </View>
    </LightScreen>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#FFFFFF',
    borderTopLeftRadius: 30,
    borderTopRightRadius: 30,
    padding: 26,
    paddingBottom: 40,
    alignItems: 'center',
  },
  mascot: { fontSize: 60 },
  title: { fontSize: 26, fontWeight: '900', color: colors.ink, textAlign: 'center', marginTop: 14 },
  body: { ...type.body, color: colors.inkSoft, textAlign: 'center', marginTop: 10, lineHeight: 22 },
  bubble: {
    alignSelf: 'flex-end',
    backgroundColor: '#DDE7FF',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 16,
    marginTop: 20,
  },
  bubbleText: { ...type.title, color: colors.brand },
  stars: { flexDirection: 'row', gap: 14, marginTop: 12 },
  star: { fontSize: 46, color: '#CBD5E5' },
  starActive: { color: '#FFC53D' },
});
