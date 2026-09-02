import React from 'react';
import { Pressable, StyleSheet, Switch, Text, View } from 'react-native';
import { SOUNDS } from '@/data/sounds';
import { playDrinkSound, stopSound, tapFeedback } from '@/lib/feedback';
import { useHydration } from '@/store/HydrationProvider';
import { colors, radius, type } from '@/theme';
import { Radio, Sheet, sheetStyles } from './Sheet';
import { PrimaryButton } from './ui';

/** Sounds & Effects picker — tapping a row previews it at the current volume. */
export function SoundSheet({ visible, onClose }: { visible: boolean; onClose: () => void }) {
  const { settings, updateSettings } = useHydration();

  const setVolume = (delta: number) => {
    const next = Math.round(Math.max(0, Math.min(1, settings.volume + delta)) * 100) / 100;
    updateSettings({ volume: next });
    void playDrinkSound({ ...settings, volume: next }, settings.soundId);
  };

  return (
    <Sheet
      visible={visible}
      onClose={() => {
        stopSound();
        onClose();
      }}
      title="Sounds & Effects"
    >
      <View style={styles.topRow}>
        <Text style={styles.rowLabel}>🔊  Sound effect</Text>
        <Switch
          value={settings.soundEnabled}
          onValueChange={(v) => updateSettings({ soundEnabled: v })}
          trackColor={{ false: '#D7DEEA', true: '#2E8BFF' }}
          thumbColor="#FFFFFF"
        />
      </View>

      <View style={[sheetStyles.optionList, !settings.soundEnabled && { opacity: 0.45 }]}>
        {SOUNDS.map((s, i) => (
          <Pressable
            key={s.id}
            disabled={!settings.soundEnabled}
            onPress={() => {
              updateSettings({ soundId: s.id });
              void playDrinkSound(settings, s.id);
              tapFeedback(settings);
            }}
            style={[sheetStyles.option, i === SOUNDS.length - 1 && { borderBottomWidth: 0 }]}
          >
            <Radio selected={settings.soundId === s.id} />
            <Text style={sheetStyles.optionLabel}>{s.name}</Text>
            <Text style={sheetStyles.optionMeta}>{s.seconds}s</Text>
          </Pressable>
        ))}
      </View>

      <View style={styles.volumeRow}>
        <Pressable onPress={() => setVolume(-0.1)} hitSlop={12}>
          <Text style={styles.volumeGlyph}>−</Text>
        </Pressable>
        <View style={styles.track}>
          <View style={[styles.fill, { width: `${settings.volume * 100}%` }]} />
          <View style={[styles.knob, { left: `${settings.volume * 100}%` }]} />
        </View>
        <Pressable onPress={() => setVolume(0.1)} hitSlop={12}>
          <Text style={styles.volumeGlyph}>+</Text>
        </Pressable>
      </View>

      <View style={styles.topRow}>
        <Text style={styles.rowLabel}>📳  Vibration</Text>
        <Switch
          value={settings.vibration}
          onValueChange={(v) => {
            updateSettings({ vibration: v });
            if (v) tapFeedback({ vibration: true }, 'medium');
          }}
          trackColor={{ false: '#D7DEEA', true: '#2E8BFF' }}
          thumbColor="#FFFFFF"
        />
      </View>

      <PrimaryButton
        label="Done"
        onPress={() => {
          stopSound();
          onClose();
        }}
        style={{ marginTop: 8 }}
      />
    </Sheet>
  );
}

const styles = StyleSheet.create({
  topRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 14,
  },
  rowLabel: { ...type.h2, fontSize: 19, color: colors.ink },
  volumeRow: { flexDirection: 'row', alignItems: 'center', gap: 16, paddingVertical: 20 },
  volumeGlyph: { fontSize: 26, fontWeight: '600', color: colors.inkSoft, width: 22, textAlign: 'center' },
  track: { flex: 1, height: 6, borderRadius: 3, backgroundColor: '#E2E8F2', justifyContent: 'center' },
  fill: { height: 6, borderRadius: 3, backgroundColor: colors.brand },
  knob: {
    position: 'absolute',
    width: 20,
    height: 20,
    borderRadius: 10,
    backgroundColor: colors.brand,
    marginLeft: -10,
  },
});
