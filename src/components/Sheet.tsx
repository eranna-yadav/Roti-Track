import React from 'react';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { colors, radius, type } from '@/theme';

type Props = {
  visible: boolean;
  onClose: () => void;
  title?: string;
  children: React.ReactNode;
};

/** A dimmed bottom sheet — the app's standard container for pickers. */
export function Sheet({ visible, onClose, title, children }: Props) {
  const insets = useSafeAreaInsets();
  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose} statusBarTranslucent>
      <View style={styles.backdrop}>
        <Pressable style={StyleSheet.absoluteFill} onPress={onClose} />
        <View style={[styles.sheet, { paddingBottom: insets.bottom + 20 }]}>
          <View style={styles.grabber} />
          {title ? <Text style={styles.title}>{title}</Text> : null}
          {children}
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: 'rgba(9,14,28,0.55)', justifyContent: 'flex-end' },
  sheet: {
    backgroundColor: '#FFFFFF',
    borderTopLeftRadius: 30,
    borderTopRightRadius: 30,
    paddingHorizontal: 20,
    paddingTop: 10,
  },
  grabber: {
    alignSelf: 'center',
    width: 44,
    height: 5,
    borderRadius: 3,
    backgroundColor: '#E2E8F2',
    marginBottom: 14,
  },
  title: { ...type.h2, color: colors.ink, marginBottom: 14 },
});

export const sheetStyles = StyleSheet.create({
  optionList: { backgroundColor: '#F5F7FB', borderRadius: radius.lg, overflow: 'hidden' },
  option: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    paddingHorizontal: 14,
    height: 56,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#E7ECF4',
  },
  optionLabel: { ...type.title, color: colors.ink, flex: 1 },
  optionMeta: { ...type.small, color: colors.muted },
  radioOuter: {
    width: 22,
    height: 22,
    borderRadius: 11,
    borderWidth: 2,
    borderColor: '#CBD5E5',
    alignItems: 'center',
    justifyContent: 'center',
  },
  radioOuterActive: { borderColor: colors.brand },
  radioInner: { width: 12, height: 12, borderRadius: 6, backgroundColor: colors.brand },
});

export function Radio({ selected }: { selected: boolean }) {
  return (
    <View style={[sheetStyles.radioOuter, selected && sheetStyles.radioOuterActive]}>
      {selected ? <View style={sheetStyles.radioInner} /> : null}
    </View>
  );
}
