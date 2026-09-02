import { useRouter } from 'expo-router';
import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { type } from '@/theme';

/** Back arrow + title bar used by every pushed settings screen. */
export function SubHeader({
  title,
  right,
  dark,
}: {
  title: string;
  right?: React.ReactNode;
  dark?: boolean;
}) {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const fg = dark ? '#0B1220' : '#FFFFFF';
  return (
    <View style={[styles.bar, { paddingTop: insets.top + 6 }]}>
      <Pressable onPress={() => router.back()} hitSlop={16} style={styles.back}>
        <Text style={[styles.glyph, { color: fg }]} allowFontScaling={false}>
          ‹
        </Text>
      </Pressable>
      <Text style={[styles.title, { color: fg }]} numberOfLines={1}>
        {title}
      </Text>
      <View style={styles.right}>{right}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingBottom: 10,
    gap: 6,
  },
  back: { width: 36, height: 40, justifyContent: 'center' },
  glyph: { fontSize: 34, marginTop: -6 },
  title: { ...type.h2, flex: 1 },
  right: { minWidth: 44, alignItems: 'flex-end' },
});
