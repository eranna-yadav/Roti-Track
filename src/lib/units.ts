import type { Settings, UnitSystem } from './types';

export const ML_PER_FLOZ = 29.5735;
export const KG_PER_LB = 0.45359237;

export const mlToFloz = (ml: number) => ml / ML_PER_FLOZ;
export const flozToMl = (oz: number) => oz * ML_PER_FLOZ;
export const kgToLb = (kg: number) => kg / KG_PER_LB;
export const lbToKg = (lb: number) => lb * KG_PER_LB;

export const volumeUnit = (u: UnitSystem) => (u === 'metric' ? 'L' : 'fl oz');
export const smallVolumeUnit = (u: UnitSystem) => (u === 'metric' ? 'ml' : 'fl oz');
export const weightUnit = (u: UnitSystem) => (u === 'metric' ? 'kg' : 'lb');

/** Display value in the user's unit — litres for metric, fluid ounces otherwise. */
export const toDisplayVolume = (ml: number, u: UnitSystem) =>
  u === 'metric' ? ml / 1000 : mlToFloz(ml);

export const fromDisplayVolume = (value: number, u: UnitSystem) =>
  u === 'metric' ? value * 1000 : flozToMl(value);

/** "1.77 L" / "60 fl oz" */
export function formatVolume(ml: number, u: UnitSystem, digits?: number): string {
  if (u === 'metric') {
    const l = ml / 1000;
    const d = digits ?? (l < 10 ? 2 : 1);
    return `${trimZeros(l.toFixed(d))} L`;
  }
  return `${Math.round(mlToFloz(ml))} fl oz`;
}

/** Compact form used on chart labels and pills: "0.2 L". */
export function formatVolumeShort(ml: number, u: UnitSystem): string {
  if (u === 'metric') return `${trimZeros((ml / 1000).toFixed(2))} L`;
  return `${Math.round(mlToFloz(ml))} oz`;
}

/** Big hero number on the Today screen, without its unit. */
export function heroVolume(ml: number, u: UnitSystem): string {
  return u === 'metric' ? (ml / 1000).toFixed(2) : String(Math.round(mlToFloz(ml)));
}

/** Cup sizes: "200 ml" / "7 fl oz". */
export function formatCup(ml: number, u: UnitSystem): string {
  return u === 'metric' ? `${Math.round(ml)} ml` : `${Math.round(mlToFloz(ml))} fl oz`;
}

export function formatWeight(kg: number, u: UnitSystem): string {
  return u === 'metric' ? `${Math.round(kg)} kg` : `${Math.round(kgToLb(kg))} lb`;
}

export const unitsLabel = (s: Pick<Settings, 'unit'>) => (s.unit === 'metric' ? 'L, kg' : 'fl oz, lb');

function trimZeros(s: string): string {
  return s.includes('.') ? s.replace(/0+$/, '').replace(/\.$/, '') : s;
}
