import { useLocalSearchParams, useRouter } from 'expo-router';
import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { LightScreen } from '@/components/ui';
import { articleById, categoryOf } from '@/data/articles';
import { colors, radius, type } from '@/theme';

export default function ArticleScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const insets = useSafeAreaInsets();
  const router = useRouter();

  const article = articleById(String(id));
  const category = categoryOf(String(id));

  if (!article) {
    return (
      <LightScreen tint={colors.lavender}>
        <View style={{ paddingTop: insets.top + 20, paddingHorizontal: 16 }}>
          <Text style={styles.body}>That article is no longer available.</Text>
        </View>
      </LightScreen>
    );
  }

  return (
    <LightScreen tint="#FFFFFF">
      <ScrollView
        contentContainerStyle={{ paddingBottom: insets.bottom + 40 }}
        showsVerticalScrollIndicator={false}
      >
        <View style={[styles.hero, { backgroundColor: category?.tint ?? colors.lavender, paddingTop: insets.top + 12 }]}>
          <Pressable onPress={() => router.back()} hitSlop={14} style={styles.back}>
            <Text style={[styles.backGlyph, { color: category?.fg ?? colors.ink }]}>‹</Text>
          </Pressable>
          <Text style={styles.heroEmoji}>{article.emoji}</Text>
          <Text style={[styles.heroTitle, { color: category?.fg ?? colors.ink }]}>{article.title}</Text>
          <Text style={[styles.heroMeta, { color: category?.fg ?? colors.muted }]}>
            {category?.title} · {article.minutes} min read
          </Text>
        </View>

        <View style={styles.content}>
          <Text style={styles.summary}>{article.summary}</Text>
          {article.body.map((p, i) => (
            <Text key={i} style={styles.body}>
              {p}
            </Text>
          ))}
          <Text style={styles.disclaimer}>
            General wellbeing information, not medical advice. Talk to a clinician about your own
            health needs.
          </Text>
        </View>
      </ScrollView>
    </LightScreen>
  );
}

const styles = StyleSheet.create({
  hero: { paddingHorizontal: 20, paddingBottom: 28, borderBottomLeftRadius: 28, borderBottomRightRadius: 28 },
  back: { width: 40, height: 40, justifyContent: 'center' },
  backGlyph: { fontSize: 34, marginTop: -6 },
  heroEmoji: { fontSize: 58, marginTop: 8 },
  heroTitle: { fontSize: 30, fontWeight: '900', lineHeight: 36, marginTop: 12 },
  heroMeta: { ...type.small, opacity: 0.7, marginTop: 8 },
  content: { paddingHorizontal: 20, paddingTop: 22 },
  summary: { ...type.title, color: colors.ink, lineHeight: 24, marginBottom: 18 },
  body: { ...type.body, color: colors.inkSoft, lineHeight: 25, marginBottom: 16 },
  disclaimer: {
    ...type.small,
    color: colors.muted,
    lineHeight: 19,
    marginTop: 12,
    padding: 14,
    backgroundColor: '#F4F7FC',
    borderRadius: radius.md,
  },
});
