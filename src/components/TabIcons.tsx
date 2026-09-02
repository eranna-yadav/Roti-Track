import React from 'react';
import type { ColorValue } from 'react-native';
import Svg, { Circle, Path, Rect } from 'react-native-svg';

type P = { color: ColorValue; size?: number };
/** Icons that punch their detail out of a solid glyph need to know what sits
 *  behind them, since the tab bar rides on different backgrounds per screen. */
type Punched = P & { hole?: ColorValue };

export const DropIcon = ({ color, size = 24 }: P) => (
  <Svg width={size} height={size} viewBox="0 0 24 24">
    <Path
      d="M12 3.2s6 6.4 6 10.1a6 6 0 1 1-12 0C6 9.6 12 3.2 12 3.2Z"
      fill={color}
    />
  </Svg>
);

export const ClockIcon = ({ color, size = 24, hole = '#1B4FF0' }: Punched) => (
  <Svg width={size} height={size} viewBox="0 0 24 24">
    <Circle cx="12" cy="12" r="9" fill={color} />
    <Path d="M12 7v5.4l3.4 2" stroke={hole} strokeWidth={2} strokeLinecap="round" fill="none" />
  </Svg>
);

export const ListIcon = ({ color, size = 24, hole = '#1B4FF0' }: Punched) => (
  <Svg width={size} height={size} viewBox="0 0 24 24">
    <Rect x="3.5" y="3.5" width="17" height="17" rx="4" fill={color} />
    <Path
      d="M7.5 9h9M7.5 12.5h9M7.5 16h5.5"
      stroke={hole}
      strokeWidth={1.9}
      strokeLinecap="round"
    />
  </Svg>
);

export const PersonIcon = ({ color, size = 24, hole = '#1B4FF0' }: Punched) => (
  <Svg width={size} height={size} viewBox="0 0 24 24">
    <Circle cx="12" cy="12" r="9.2" fill={color} />
    <Circle cx="12" cy="9.6" r="3.1" fill={hole} />
    <Path d="M5.9 19.2a6.4 6.4 0 0 1 12.2 0" fill={hole} />
  </Svg>
);

export const BellIcon = ({ color, size = 22 }: P) => (
  <Svg width={size} height={size} viewBox="0 0 24 24">
    <Path
      d="M12 3a5.6 5.6 0 0 0-5.6 5.6v3.1L5 15.2h14l-1.4-3.5V8.6A5.6 5.6 0 0 0 12 3Z"
      fill={color}
    />
    <Path d="M9.8 17.2a2.3 2.3 0 0 0 4.4 0" stroke={color} strokeWidth={1.9} strokeLinecap="round" fill="none" />
  </Svg>
);

export const SpeakerIcon = ({ color, size = 22 }: P) => (
  <Svg width={size} height={size} viewBox="0 0 24 24">
    <Path d="M4 9.4h3.4L12 5.4v13.2L7.4 14.6H4Z" fill={color} />
    <Path
      d="M15.2 9.2a4 4 0 0 1 0 5.6M17.8 6.8a7.5 7.5 0 0 1 0 10.4"
      stroke={color}
      strokeWidth={1.8}
      strokeLinecap="round"
      fill="none"
    />
  </Svg>
);

export const PencilIcon = ({ color, size = 18 }: P) => (
  <Svg width={size} height={size} viewBox="0 0 24 24">
    <Path
      d="M4 17.2 15.1 6.1l2.8 2.8L6.8 20H4v-2.8ZM16.6 4.6l1.4-1.4a1.4 1.4 0 0 1 2 0l.8.8a1.4 1.4 0 0 1 0 2l-1.4 1.4-2.8-2.8Z"
      fill={color}
    />
  </Svg>
);

export const TrashIcon = ({ color, size = 18 }: P) => (
  <Svg width={size} height={size} viewBox="0 0 24 24">
    <Path
      d="M6 7.5h12l-1 12.1a1.6 1.6 0 0 1-1.6 1.4H8.6A1.6 1.6 0 0 1 7 19.6L6 7.5ZM4.5 5.6h15M9.6 5.6V4.4c0-.6.5-1.1 1.1-1.1h2.6c.6 0 1.1.5 1.1 1.1v1.2"
      stroke={color}
      strokeWidth={1.8}
      strokeLinecap="round"
      fill="none"
    />
  </Svg>
);

export const PlusIcon = ({ color, size = 22 }: P) => (
  <Svg width={size} height={size} viewBox="0 0 24 24">
    <Path d="M12 5v14M5 12h14" stroke={color} strokeWidth={2.4} strokeLinecap="round" />
  </Svg>
);

export const ArrowIcon = ({ color, size = 16 }: P) => (
  <Svg width={size} height={size} viewBox="0 0 24 24">
    <Path d="M7 17 17 7M9 7h8v8" stroke={color} strokeWidth={2.2} strokeLinecap="round" strokeLinejoin="round" fill="none" />
  </Svg>
);

export const CheckIcon = ({ color, size = 16 }: P) => (
  <Svg width={size} height={size} viewBox="0 0 24 24">
    <Path d="m5 12.5 4.6 4.5L19 7" stroke={color} strokeWidth={2.6} strokeLinecap="round" strokeLinejoin="round" fill="none" />
  </Svg>
);
