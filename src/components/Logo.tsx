import React from 'react';
import Svg, { ClipPath, Defs, G, LinearGradient, Path, Rect, Stop } from 'react-native-svg';

/** The Sipwell droplet: a white drop half-filled with water, plus a sparkle. */
export function DropMark({ size = 96 }: { size?: number }) {
  return (
    <Svg width={size} height={size} viewBox="0 0 1024 1024">
      <Defs>
        <LinearGradient id="mwater" x1="0" y1="0" x2="0" y2="1">
          <Stop offset="0" stopColor="#7FE3FF" />
          <Stop offset="1" stopColor="#1E7BEA" />
        </LinearGradient>
        <LinearGradient id="msheen" x1="0" y1="0" x2="0" y2="1">
          <Stop offset="0" stopColor="#FFFFFF" stopOpacity={0.75} />
          <Stop offset="1" stopColor="#FFFFFF" stopOpacity={0} />
        </LinearGradient>
        <ClipPath id="mclip">
          <Path d="M512 168 C512 168 762 432 762 604 A250 250 0 0 1 262 604 C262 432 512 168 512 168 Z" />
        </ClipPath>
      </Defs>
      <Path
        d="M512 168 C512 168 762 432 762 604 A250 250 0 0 1 262 604 C262 432 512 168 512 168 Z"
        fill="#FFFFFF"
      />
      <G clipPath="url(#mclip)">
        <Path d="M240 606 q 68 -58 136 0 t 136 0 t 136 0 t 136 0 v 300 h -544 z" fill="url(#mwater)" />
        <Path
          d="M240 646 q 68 -52 136 0 t 136 0 t 136 0 t 136 0 v 300 h -544 z"
          fill="#0E8FE0"
          opacity={0.32}
        />
      </G>
      <Path
        d="M512 214 C512 214 400 340 372 452 C356 516 396 560 430 546 C462 532 436 470 470 396 C492 348 528 292 512 214 Z"
        fill="url(#msheen)"
      />
      <Path d="M770 244 l 27 68 68 27 -68 27 -27 68 -27 -68 -68 -27 68 -27 z" fill="#FFFFFF" />
    </Svg>
  );
}

/** The full app tile — the same mark on its gradient square. */
export function AppIcon({ size = 96, radius = 22 }: { size?: number; radius?: number }) {
  const r = (radius / size) * 1024;
  return (
    <Svg width={size} height={size} viewBox="0 0 1024 1024">
      <Defs>
        <LinearGradient id="itile" x1="0" y1="0" x2="1" y2="1">
          <Stop offset="0" stopColor="#2E7BFF" />
          <Stop offset="0.55" stopColor="#1B4FF0" />
          <Stop offset="1" stopColor="#0E32C4" />
        </LinearGradient>
        <LinearGradient id="iwater" x1="0" y1="0" x2="0" y2="1">
          <Stop offset="0" stopColor="#7FE3FF" />
          <Stop offset="1" stopColor="#20A6F5" />
        </LinearGradient>
        <ClipPath id="iclip">
          <Path d="M512 168 C512 168 762 432 762 604 A250 250 0 0 1 262 604 C262 432 512 168 512 168 Z" />
        </ClipPath>
      </Defs>
      <Rect width={1024} height={1024} rx={r * 1.5} fill="url(#itile)" />
      <G opacity={0.15}>
        <Path d="M-40 812 q 150 -80 300 0 t 300 0 t 300 0 t 300 0 v 260 h -1200 z" fill="#FFFFFF" />
      </G>
      <Path
        d="M512 168 C512 168 762 432 762 604 A250 250 0 0 1 262 604 C262 432 512 168 512 168 Z"
        fill="#FFFFFF"
      />
      <G clipPath="url(#iclip)">
        <Path d="M240 610 q 68 -58 136 0 t 136 0 t 136 0 t 136 0 v 300 h -544 z" fill="url(#iwater)" />
      </G>
      <Path d="M756 250 l 26 66 66 26 -66 26 -26 66 -26 -66 -66 -26 66 -26 z" fill="#FFFFFF" />
    </Svg>
  );
}
