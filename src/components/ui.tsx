import { LinearGradient } from 'expo-linear-gradient';
import React from 'react';
import {
  Pressable, ScrollView, StyleSheet, Switch, Text, View, type ViewStyle,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { colors, radius, shadow, type } from '@/theme';

/* ------------------------------------------------------------------ screens */

/** Saturated blue background used by History, Me and the modal screens. */
export function BlueScreen({ children, style }: { children: React.ReactNode; style?: ViewStyle }) {
  return (
    <LinearGradient colors={['#1B4FF0', '#1846DA', '#123BC6']} style={[{ flex: 1 }, style]}>
      {children}
    </LinearGradient>
  );
}

/** Pale background used by Today and Insights. */
export function LightScreen({
  children,
  tint = colors.skyBg,
  style,
}: {
  children: React.ReactNode;
  tint?: string;
  style?: ViewStyle;
}) {
  return <View style={[{ flex: 1, backgroundColor: tint }, style]}>{children}</View>;
}

export function ScreenScroll({
  children,
  padded = true,
}: {
  children: React.ReactNode;
  padded?: boolean;
}) {
  const insets = useSafeAreaInsets();
  return (
    <ScrollView
      style={{ flex: 1 }}
      contentContainerStyle={{
        paddingHorizontal: padded ? 16 : 0,
        paddingBottom: insets.bottom + 32,
      }}
      showsVerticalScrollIndicator={false}
    >
      {children}
    </ScrollView>
  );
}

/* -------------------------------------------------------------------- text */

export function SectionTitle({ children, dark }: { children: React.ReactNode; dark?: boolean }) {
  return (
    <Text style={[styles.sectionTitle, dark && { color: colors.ink }]} numberOfLines={1}>
      {children}
    </Text>
  );
}

/* ------------------------------------------------------------------- cards */

export function GlassCard({ children, style }: { children: React.ReactNode; style?: ViewStyle }) {
  return <View style={[styles.glassCard, style]}>{children}</View>;
}

export function WhiteCard({ children, style }: { children: React.ReactNode; style?: ViewStyle }) {
  return <View style={[styles.whiteCard, style]}>{children}</View>;
}

/* -------------------------------------------------------------------- rows */

type RowProps = {
  icon?: React.ReactNode;
  title: string;
  subtitle?: string;
  value?: string;
  badge?: boolean;
  onPress?: () => void;
  right?: React.ReactNode;
  last?: boolean;
  dark?: boolean;
};

/** A settings line: icon, label, optional trailing value and chevron. */
export function Row({ icon, title, subtitle, value, badge, onPress, right, last, dark }: RowProps) {
  const fg = dark ? colors.ink : '#FFFFFF';
  const body = (
    <View style={[styles.row, !last && (dark ? styles.rowDividerDark : styles.rowDivider)]}>
      {icon ? <View style={styles.rowIcon}>{icon}</View> : null}
      <View style={{ flex: 1 }}>
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          <Text style={[styles.rowTitle, { color: fg }]} numberOfLines={1}>
            {title}
          </Text>
          {badge ? <View style={styles.dot} /> : null}
        </View>
        {subtitle ? (
          <Text style={[styles.rowSubtitle, dark && { color: colors.muted }]}>{subtitle}</Text>
        ) : null}
      </View>
      {value ? (
        <Text style={[styles.rowValue, dark && { color: colors.muted }]} numberOfLines={2}>
          {value}
        </Text>
      ) : null}
      {right ?? (onPress ? <Chevron dark={dark} /> : null)}
    </View>
  );
  if (!onPress) return body;
  return (
    <Pressable onPress={onPress} android_ripple={{ color: 'rgba(255,255,255,0.12)' }}>
      {({ pressed }) => <View style={{ opacity: pressed ? 0.65 : 1 }}>{body}</View>}
    </Pressable>
  );
}

export function ToggleRow({
  icon,
  title,
  subtitle,
  value,
  onChange,
  badge,
  last,
  dark,
}: {
  icon?: React.ReactNode;
  title: string;
  subtitle?: string;
  value: boolean;
  onChange: (v: boolean) => void;
  badge?: boolean;
  last?: boolean;
  dark?: boolean;
}) {
  return (
    <Row
      icon={icon}
      title={title}
      subtitle={subtitle}
      badge={badge}
      last={last}
      dark={dark}
      right={
        <Switch
          value={value}
          onValueChange={onChange}
          trackColor={{ false: dark ? '#D7DEEA' : 'rgba(255,255,255,0.28)', true: '#2E8BFF' }}
          thumbColor="#FFFFFF"
          ios_backgroundColor={dark ? '#D7DEEA' : 'rgba(255,255,255,0.28)'}
        />
      }
    />
  );
}

export function Chevron({ dark }: { dark?: boolean }) {
  return (
    <Text style={[styles.chevron, dark && { color: colors.muted }]} allowFontScaling={false}>
      ›
    </Text>
  );
}

/* ----------------------------------------------------------------- buttons */

export function PrimaryButton({
  label,
  onPress,
  disabled,
  style,
}: {
  label: string;
  onPress?: () => void;
  disabled?: boolean;
  style?: ViewStyle;
}) {
  return (
    <Pressable onPress={onPress} disabled={disabled} style={({ pressed }) => [
      styles.primaryBtn,
      shadow.card,
      disabled && { opacity: 0.5 },
      pressed && { transform: [{ scale: 0.985 }] },
      style,
    ]}>
      <LinearGradient
        colors={['#3D82FF', '#1B4FF0']}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
        style={StyleSheet.absoluteFill}
      />
      <Text style={styles.primaryBtnText}>{label}</Text>
    </Pressable>
  );
}

export function GhostButton({
  label,
  onPress,
  style,
}: {
  label: string;
  onPress?: () => void;
  style?: ViewStyle;
}) {
  return (
    <Pressable onPress={onPress} style={({ pressed }) => [styles.ghostBtn, pressed && { opacity: 0.7 }, style]}>
      <Text style={styles.ghostBtnText}>{label}</Text>
    </Pressable>
  );
}

/** Small translucent circle used for header actions. */
export function CircleButton({
  children,
  onPress,
  size = 44,
  tint = 'rgba(255,255,255,0.18)',
}: {
  children: React.ReactNode;
  onPress?: () => void;
  size?: number;
  tint?: string;
}) {
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [
        {
          width: size,
          height: size,
          borderRadius: size / 2,
          backgroundColor: tint,
          alignItems: 'center',
          justifyContent: 'center',
        },
        pressed && { opacity: 0.7 },
      ]}
    >
      {children}
    </Pressable>
  );
}

/** Segmented control, used for DAY / WEEK / MONTH and the reminder modes. */
export function Segmented({
  options,
  value,
  onChange,
}: {
  options: { key: string; label: string }[];
  value: string;
  onChange: (key: string) => void;
}) {
  return (
    <View style={styles.segmented}>
      {options.map((o) => {
        const active = o.key === value;
        return (
          <Pressable key={o.key} style={styles.segment} onPress={() => onChange(o.key)}>
            <Text style={[styles.segmentText, active && styles.segmentTextActive]}>{o.label}</Text>
            <View style={[styles.segmentBar, active && styles.segmentBarActive]} />
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  sectionTitle: {
    ...type.h2,
    color: '#FFFFFF',
    marginTop: 26,
    marginBottom: 12,
    marginLeft: 4,
  },
  glassCard: {
    backgroundColor: colors.glass,
    borderRadius: radius.xl,
    paddingHorizontal: 16,
    overflow: 'hidden',
  },
  whiteCard: {
    backgroundColor: colors.card,
    borderRadius: radius.xl,
    paddingHorizontal: 16,
    overflow: 'hidden',
    ...shadow.card,
  },
  row: {
    minHeight: 58,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 12,
  },
  rowDivider: { borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.hairline },
  rowDividerDark: { borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.divider },
  rowIcon: { width: 26, alignItems: 'center' },
  rowTitle: { ...type.title, color: '#FFFFFF' },
  rowSubtitle: { ...type.small, color: colors.mutedOnBlue, marginTop: 2 },
  rowValue: { ...type.title, color: 'rgba(255,255,255,0.9)', maxWidth: 140, textAlign: 'right' },
  chevron: { fontSize: 26, color: 'rgba(255,255,255,0.6)', marginLeft: 2, marginTop: -3 },
  dot: {
    width: 7,
    height: 7,
    borderRadius: 4,
    backgroundColor: '#FF3B30',
    marginLeft: 6,
    marginTop: -8,
  },
  primaryBtn: {
    height: 56,
    borderRadius: radius.pill,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  primaryBtnText: { color: '#FFFFFF', fontSize: 17, fontWeight: '800' },
  ghostBtn: { height: 48, alignItems: 'center', justifyContent: 'center' },
  ghostBtnText: { color: colors.muted, fontSize: 15, fontWeight: '600' },
  segmented: { flexDirection: 'row' },
  segment: { flex: 1, alignItems: 'center' },
  segmentText: {
    ...type.small,
    fontSize: 14,
    letterSpacing: 1,
    color: 'rgba(255,255,255,0.6)',
    paddingVertical: 12,
  },
  segmentTextActive: { color: '#FFFFFF', fontWeight: '800' },
  segmentBar: { height: 3, width: '55%', borderRadius: 2, backgroundColor: 'transparent' },
  segmentBarActive: { backgroundColor: '#FFFFFF' },
});
