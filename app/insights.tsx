import { useRouter } from 'expo-router';
import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { SubHeader } from '@/components/SubHeader';
import { LightScreen } from '@/components/ui';
import { CATEGORIES, type Article, type ArticleCategory } from '@/data/articles';
import { colors, radius, shadow, type } from '@/theme';

export default function InsightsScreen() {
  const insets = useSafeAreaInsets();
  const router = useRouter();

  return (
    <LightScreen tint={colors.lavender}>
      <SubHeader title="Insights" dark />
      <ScrollView
        contentContainerStyle={{ paddingTop: 6, paddingBottom: insets.bottom + 40 }}
        showsVerticalScrollIndicator={false}
      >
        {CATEGORIES.map((cat) => (
          <Category key={cat.id} category={cat} onOpen={(a) => router.push(`/article/${a.id}`)} />
        ))}
      </ScrollView>
    </LightScreen>
  );
}

function Category({
  category,
  onOpen,
}: {
  category: ArticleCategory;
  onOpen: (a: Article) => void;
}) {
  return (
    <View style={{ marginBottom: 26 }}>
      <Text style={styles.categoryTitle}>{category.title}</Text>
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={{ paddingHorizontal: 16, gap: 12 }}
      >
        {category.articles.map((a) => (
          <Pressable
            key={a.id}
            onPress={() => onOpen(a)}
            style={({ pressed }) => [
              styles.card,
              { backgroundColor: category.tint },
              shadow.card,
              pressed && { transform: [{ scale: 0.98 }] },
            ]}
          >
            <View style={styles.cardArt}>
              <Text style={styles.cardEmoji}>{a.emoji}</Text>
            </View>
            <View style={styles.cardFoot}>
              <Text style={[styles.cardTitle, { color: category.fg }]} numberOfLines={3}>
                {a.title}
              </Text>
              <Text style={[styles.cardMeta, { color: category.fg }]}>{a.minutes} min read</Text>
            </View>
          </Pressable>
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  categoryTitle: { ...type.h2, color: colors.ink, paddingHorizontal: 16, marginBottom: 12 },
  card: { width: 168, height: 236, borderRadius: radius.lg, overflow: 'hidden' },
  cardArt: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  cardEmoji: { fontSize: 62 },
  cardFoot: {
    backgroundColor: 'rgba(255,255,255,0.62)',
    paddingHorizontal: 12,
    paddingVertical: 12,
    minHeight: 92,
  },
  cardTitle: { fontSize: 16, fontWeight: '800', lineHeight: 20 },
  cardMeta: { ...type.tiny, opacity: 0.65, marginTop: 6 },
});
