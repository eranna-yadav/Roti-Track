import { Redirect } from 'expo-router';
import React from 'react';
import { useHydration } from '@/store/HydrationProvider';

/** Entry gate: first launch goes to onboarding, everyone else to Today. */
export default function Index() {
  const { settings } = useHydration();
  return <Redirect href={settings.onboarded ? '/(tabs)' : '/onboarding'} />;
}
