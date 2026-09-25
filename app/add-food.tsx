import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useMemo, useState } from 'react';
import { FlatList, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { FoodRow, LogFoodSheet } from '@/components/diet';
import { SubHeader } from '@/components/SubHeader';
import { TrashIcon } from '@/components/TabIcons';
import { LightScreen } from '@/components/ui';
import { FOOD_CATEGORIES } from '@/data/foods';
import type { Food, FoodCategory, MealSlot } from '@/lib/diet-types';
import { tapFeedback } from '@/lib/feedback';
import { useDiet } from '@/store/DietProvider';
import { useHydration } from '@/store/HydrationProvider';
import { colors, radius, type } from '@/theme';

const RANK = { veg: 0, egg: 1, nonveg: 2 } as const;
type Filter = 'recent' | 'custom' | FoodCategory;

export default function AddFoodScreen() {
  const params = useLocalSearchParams<{ slot?: MealSlot; day?: string }>();
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { settings } = useHydration();
  const { allFoods, recentFoods, customFoods, profile, todayKey, logFood, removeCustomFood } = useDiet();

  const slot: MealSlot = params.slot ?? 'breakfast';
  const day = params.day ?? todayKey;

  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState<Filter>(recentFoods.length ? 'recent' : 'breakfast');
  const [showAll, setShowAll] = useState(false);
  const [picked, setPicked] = useState<Food | null>(null);
  const [added, setAdded] = useState(0);

  const results = useMemo(() => {
    const dietOk = (f: Food) => showAll || RANK[f.diet] <= RANK[profile.pref];
    const q = query.trim().toLowerCase();
    if (q) return allFoods.filter((f) => dietOk(f) && f.name.toLowerCase().includes(q));
    if (filter === 'recent') return recentFoods.filter(dietOk);
    if (filter === 'custom') return customFoods;
    return allFoods.filter((f) => f.category === filter && dietOk(f));
  }, [query, filter, allFoods, recentFoods, customFoods, profile.pref, showAll]);

  const chips: { key: Filter; label: string }[] = [
    ...(recentFoods.length ? [{ key: 'recent' as const, label: 'Recent' }] : []),
    ...FOOD_CATEGORIES,
    { key: 'custom', label: 'My foods' },
  ];

  return (
    <LightScreen tint="#FFFFFF">
      <SubHeader
        title="Add food"
        dark
        right={
          added > 0 ? (
            <Pressable onPress={() => router.back()} hitSlop={10}>
              <Text style={styles.done}>Done ({added})</Text>
            </Pressable>
          ) : null
        }
      />

      <View style={styles.searchWrap}>
        <Text style={styles.searchIcon}>🔍</Text>
        <TextInput
          value={query}
          onChangeText={setQuery}
          placeholder="Search dal, roti, idli, paneer…"
          placeholderTextColor={colors.muted}
          style={styles.search}
          autoCorrect={false}
          returnKeyType="search"
        />
        {query ? (
          <Pressable onPress={() => setQuery('')} hitSlop={10}>
            <Text style={styles.clear}>✕</Text>
          </Pressable>
        ) : null}
      </View>

      {!query ? (
        <View>
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={{ gap: 8, paddingHorizontal: 16, paddingVertical: 12 }}
          >
            {chips.map((c) => (
              <Pressable
                key={c.key}
                onPress={() => setFilter(c.key)}
                style={[styles.chip, filter === c.key && styles.chipActive]}
              >
                <Text style={[styles.chipText, filter === c.key && styles.chipTextActive]}>{c.label}</Text>
              </Pressable>
            ))}
          </ScrollView>
        </View>
      ) : null}

      {profile.pref !== 'nonveg' ? (
        <Pressable style={styles.toggle} onPress={() => setShowAll((v) => !v)}>
          <Text style={styles.toggleText}>
            {showAll ? 'Showing all foods' : `Showing ${profile.pref === 'veg' ? 'vegetarian' : 'veg + egg'} foods`}
          </Text>
          <Text style={styles.toggleLink}>{showAll ? 'Filter' : 'Show all'}</Text>
        </Pressable>
      ) : null}

      <FlatList
        data={results}
        keyExtractor={(f) => f.id}
        keyboardShouldPersistTaps="handled"
        contentContainerStyle={{ paddingHorizontal: 16, paddingBottom: insets.bottom + 100 }}
        ItemSeparatorComponent={() => <View style={styles.sep} />}
        renderItem={({ item }) => (
          <FoodRow
            food={item}
            onPress={() => setPicked(item)}
            right={
              filter === 'custom' && !query ? (
                <Pressable hitSlop={10} onPress={() => removeCustomFood(item.id)}>
                  <TrashIcon color={colors.muted} />
                </Pressable>
              ) : undefined
            }
          />
        )}
        ListEmptyComponent={
          <Text style={styles.empty}>
            {filter === 'custom' && !query
              ? 'No custom foods yet. Add your own recipes below.'
              : 'No match. Try another spelling, or add it as a custom food.'}
          </Text>
        }
      />

      <Pressable
        style={[styles.customBtn, { bottom: insets.bottom + 20 }]}
        onPress={() => router.push(`/custom-food?slot=${slot}&day=${day}&name=${encodeURIComponent(query)}`)}
      >
        <Text style={styles.customText}>＋ Create custom food</Text>
      </Pressable>

      <LogFoodSheet
        food={picked}
        initialSlot={slot}
        onClose={() => setPicked(null)}
        onLog={(servings, s) => {
          if (!picked) return;
          logFood(picked, servings, s, day);
          tapFeedback(settings, 'success');
          setAdded((n) => n + 1);
          setPicked(null);
        }}
      />
    </LightScreen>
  );
}

const styles = StyleSheet.create({
  done: { ...type.title, color: colors.brand },
  searchWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginHorizontal: 16,
    paddingHorizontal: 14,
    height: 50,
    borderRadius: radius.pill,
    backgroundColor: '#F1F4FA',
  },
  searchIcon: { fontSize: 16 },
  search: { flex: 1, ...type.body, color: colors.ink, paddingVertical: 0 },
  clear: { fontSize: 16, color: colors.muted },
  chip: { paddingHorizontal: 14, height: 36, borderRadius: radius.pill, backgroundColor: '#F1F4FA', justifyContent: 'center' },
  chipActive: { backgroundColor: colors.brand },
  chipText: { ...type.small, color: colors.inkSoft },
  chipTextActive: { color: '#FFFFFF' },
  toggle: { flexDirection: 'row', justifyContent: 'space-between', paddingHorizontal: 18, paddingBottom: 4 },
  toggleText: { ...type.small, color: colors.muted, fontWeight: '500' },
  toggleLink: { ...type.small, color: colors.brand },
  sep: { height: StyleSheet.hairlineWidth, backgroundColor: colors.divider },
  empty: { ...type.body, color: colors.muted, textAlign: 'center', marginTop: 40, paddingHorizontal: 20 },
  customBtn: {
    position: 'absolute',
    left: 16,
    right: 16,
    height: 52,
    borderRadius: radius.pill,
    backgroundColor: colors.ink,
    alignItems: 'center',
    justifyContent: 'center',
  },
  customText: { color: '#FFFFFF', fontSize: 16, fontWeight: '800' },
});
