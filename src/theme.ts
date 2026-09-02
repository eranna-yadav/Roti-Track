/** Shared design tokens. The app has two moods: a light "Today/Insights" surface
 *  and a saturated blue "History/Me" surface. */
export const colors = {
  brand: '#1B4FF0',
  brandDeep: '#0E32C4',
  brandBright: '#2E7BFF',
  aqua: '#3FC1FF',
  waterTop: '#7FE3FF',
  waterDeep: '#1E7BEA',

  skyBg: '#E9F0FF',
  lavender: '#E9EEFB',
  card: '#FFFFFF',

  ink: '#0B1220',
  inkSoft: '#3A4762',
  muted: '#8494AE',
  mutedOnBlue: 'rgba(255,255,255,0.66)',

  glass: 'rgba(255,255,255,0.13)',
  glassStrong: 'rgba(255,255,255,0.20)',
  hairline: 'rgba(255,255,255,0.14)',
  divider: '#EDF1F8',

  success: '#22C55E',
  flame: '#FF7A1A',
  danger: '#EF4444',
  gold: '#FFC53D',
} as const;

export const radius = { sm: 10, md: 16, lg: 22, xl: 28, pill: 999 } as const;

export const spacing = (n: number) => n * 4;

export const shadow = {
  card: {
    shadowColor: '#0B1220',
    shadowOpacity: 0.08,
    shadowRadius: 18,
    shadowOffset: { width: 0, height: 8 },
    elevation: 4,
  },
  float: {
    shadowColor: '#0B1220',
    shadowOpacity: 0.16,
    shadowRadius: 24,
    shadowOffset: { width: 0, height: 12 },
    elevation: 10,
  },
} as const;

export const type = {
  display: { fontSize: 78, fontWeight: '800' as const, letterSpacing: -2 },
  h1: { fontSize: 28, fontWeight: '800' as const },
  h2: { fontSize: 22, fontWeight: '800' as const },
  title: { fontSize: 17, fontWeight: '700' as const },
  body: { fontSize: 15, fontWeight: '500' as const },
  small: { fontSize: 13, fontWeight: '600' as const },
  tiny: { fontSize: 11, fontWeight: '700' as const, letterSpacing: 0.6 },
};
