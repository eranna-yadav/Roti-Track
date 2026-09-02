import { useLocalSearchParams } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, Text } from 'react-native';
import { SubHeader } from '@/components/SubHeader';
import { LightScreen } from '@/components/ui';
import { colors, type } from '@/theme';

const PRIVACY = {
  title: 'Privacy Policy',
  paragraphs: [
    'Sipwell stores everything you log on your device. Your drink records, goal, weight and reminder schedule live in local storage on the phone and are not uploaded anywhere.',
    'The app does not create an account, does not ask for an email address, and does not contain analytics or advertising SDKs.',
    'Notifications are scheduled by the operating system from the times you configure. The content of those reminders never leaves the device.',
    'Deleting the app removes all of the data it holds. There is no server-side copy to request or erase.',
    'If a future version adds cloud sync, it will be opt-in and this policy will be updated before that ships.',
    'Questions: hello@sipwell.app',
  ],
};

const TERMS = {
  title: 'Terms of use',
  paragraphs: [
    'Sipwell is a hydration tracking tool for general wellbeing. It is not a medical device and does not provide medical advice, diagnosis or treatment.',
    'The daily goal Sipwell suggests is an estimate based on body weight and gender. Your real needs vary with activity, climate, medication and health conditions. Talk to a clinician before making significant changes to your fluid intake, particularly if you have a kidney, heart or liver condition.',
    'Articles in Insights are general information, not personalised guidance.',
    'The app is provided as is, without warranty. You are responsible for how you use the information it presents.',
    'Pro features may be offered as a purchase. Where a payment provider is connected, purchases are handled by the app store and subject to its refund policy.',
  ],
};

export default function LegalScreen() {
  const { doc } = useLocalSearchParams<{ doc?: string }>();
  const content = doc === 'terms' ? TERMS : PRIVACY;

  return (
    <LightScreen tint="#FFFFFF">
      <SubHeader title={content.title} dark />
      <ScrollView contentContainerStyle={styles.body} showsVerticalScrollIndicator={false}>
        <Text style={styles.updated}>Last updated: January 2026</Text>
        {content.paragraphs.map((p, i) => (
          <Text key={i} style={styles.para}>
            {p}
          </Text>
        ))}
      </ScrollView>
    </LightScreen>
  );
}

const styles = StyleSheet.create({
  body: { paddingHorizontal: 20, paddingBottom: 48 },
  updated: { ...type.small, color: colors.muted, marginBottom: 18 },
  para: { ...type.body, color: colors.inkSoft, lineHeight: 24, marginBottom: 16 },
});
