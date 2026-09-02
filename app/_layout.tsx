import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import React, { useEffect } from 'react';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { configureNotificationHandler } from '@/lib/notifications';
import { HydrationProvider, useHydration } from '@/store/HydrationProvider';

void SplashScreen.preventAutoHideAsync().catch(() => {});
configureNotificationHandler();

function Routes() {
  const { ready } = useHydration();

  useEffect(() => {
    if (ready) void SplashScreen.hideAsync().catch(() => {});
  }, [ready]);

  if (!ready) return null;

  return (
    <Stack screenOptions={{ headerShown: false, animation: 'slide_from_right' }}>
      <Stack.Screen name="index" />
      <Stack.Screen name="onboarding" />
      <Stack.Screen name="(tabs)" />
      <Stack.Screen name="pro" options={{ presentation: 'modal', animation: 'slide_from_bottom' }} />
    </Stack>
  );
}

export default function RootLayout() {
  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <HydrationProvider>
          <StatusBar style="auto" />
          <Routes />
        </HydrationProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
