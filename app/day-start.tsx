import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text } from 'react-native';
import { SubHeader } from '@/components/SubHeader';
import { TimeWheel } from '@/components/TimeWheel';
import { LightScreen, PrimaryButton } from '@/components/ui';
import { useHydration } from '@/store/HydrationProvider';
import { colors, type } from '@/theme';

/** "A Day Starts At" — shifts which logical day a late-night drink belongs to. */
export default function DayStartScreen() {
  const { settings, updateSettings } = useHydration();
  const router = useRouter();
  const [value, setValue] = useState(settings.dayStartsAt);

  return (
    <LightScreen tint="#FFFFFF">
      <SubHeader title="A Day Starts At" dark />
      <ScrollView contentContainerStyle={styles.body}>
        <Text style={styles.blurb}>
          Drinks logged before this time count toward the previous day. Useful if you are often up
          past midnight.
        </Text>
        <TimeWheel value={value} onChange={setValue} minuteStep={30} use24h />
        <PrimaryButton
          label="Save"
          onPress={() => {
            updateSettings({ dayStartsAt: value });
            router.back();
          }}
          style={{ alignSelf: 'stretch', marginTop: 30 }}
        />
      </ScrollView>
    </LightScreen>
  );
}

const styles = StyleSheet.create({
  body: { paddingHorizontal: 20, paddingBottom: 40, alignItems: 'center' },
  blurb: { ...type.body, color: colors.muted, lineHeight: 22, marginBottom: 24, textAlign: 'center' },
});
