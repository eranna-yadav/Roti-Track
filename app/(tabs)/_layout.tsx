import { Tabs } from 'expo-router';
import React from 'react';
import { Platform, StyleSheet, type ViewStyle } from 'react-native';
import { ClockIcon, DropIcon, ListIcon, PersonIcon } from '@/components/TabIcons';
import { colors } from '@/theme';

/**
 * The bar rides directly on each screen's own background: transparent over the
 * water on Today, solid blue on History/Me, and pale on Insights. React
 * Navigation applies the *focused* screen's options to the bar, so each tab
 * carries its own palette.
 */
const BLUE_BOTTOM = '#123BC6';

const barBase: ViewStyle = {
  position: 'absolute',
  borderTopWidth: 0,
  elevation: 0,
  height: Platform.OS === 'ios' ? 86 : 72,
};

const onBlue = {
  tabBarStyle: { ...barBase, backgroundColor: BLUE_BOTTOM },
  tabBarActiveTintColor: '#FFFFFF',
  tabBarInactiveTintColor: 'rgba(255,255,255,0.55)',
};

const onWater = {
  ...onBlue,
  tabBarStyle: { ...barBase, backgroundColor: 'transparent' },
};

const onLight = {
  tabBarStyle: { ...barBase, backgroundColor: colors.lavender },
  tabBarActiveTintColor: colors.brand,
  tabBarInactiveTintColor: '#93A0B8',
};

export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarBackground: () => null,
        tabBarLabelStyle: styles.label,
        tabBarItemStyle: { paddingTop: 6 },
        sceneStyle: { backgroundColor: colors.brand },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{ ...onWater, title: 'Today', tabBarIcon: ({ color }) => <DropIcon color={color} /> }}
      />
      <Tabs.Screen
        name="history"
        options={{
          ...onBlue,
          title: 'History',
          tabBarIcon: ({ color }) => <ClockIcon color={color} hole={BLUE_BOTTOM} />,
        }}
      />
      <Tabs.Screen
        name="insights"
        options={{
          ...onLight,
          title: 'Insights',
          tabBarIcon: ({ color }) => <ListIcon color={color} hole={colors.lavender} />,
        }}
      />
      <Tabs.Screen
        name="me"
        options={{
          ...onBlue,
          title: 'Me',
          tabBarIcon: ({ color }) => <PersonIcon color={color} hole={BLUE_BOTTOM} />,
        }}
      />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  label: { fontSize: 13, fontWeight: '700', marginTop: 2 },
});
