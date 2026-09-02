import React, { useEffect } from 'react';
import { StyleSheet, View } from 'react-native';
import Animated, {
  Easing, useAnimatedStyle, useSharedValue, withRepeat, withTiming,
} from 'react-native-reanimated';
import Svg, { Defs, LinearGradient, Path, Stop } from 'react-native-svg';
import { colors } from '@/theme';

const W = 900;
const H = 120;
const CREST = 42;

/** One period of a sine-ish crest, repeated twice so the strip can scroll seamlessly. */
function crest(amp: number) {
  const q = W / 8;
  return (
    `M0 ${H / 2} ` +
    `q ${q} ${-amp} ${q * 2} 0 t ${q * 2} 0 t ${q * 2} 0 t ${q * 2} 0 ` +
    `V ${H} H 0 Z`
  );
}

type Props = {
  /** 0..1 — how full the container is. */
  progress: number;
  height: number;
  /** Seconds for one full horizontal loop. */
  duration?: number;
};

/**
 * The water body on the Today screen: two offset wave strips drifting at
 * different speeds, with the whole layer rising as the goal fills up.
 */
export function WaveFill({ progress, height, duration = 7 }: Props) {
  const drift1 = useSharedValue(0);
  const drift2 = useSharedValue(0);
  const level = useSharedValue(progress);

  useEffect(() => {
    drift1.value = 0;
    drift1.value = withRepeat(withTiming(1, { duration: duration * 1000, easing: Easing.linear }), -1, false);
    drift2.value = 0;
    drift2.value = withRepeat(
      withTiming(1, { duration: duration * 1600, easing: Easing.linear }),
      -1,
      false
    );
  }, [drift1, drift2, duration]);

  useEffect(() => {
    level.value = withTiming(progress, { duration: 900, easing: Easing.out(Easing.cubic) });
  }, [level, progress]);

  // The crest rides on top of the flat body, so the water surface ends up at
  // roughly `height * progress` above the bottom of the container.
  const crestHeight = CREST;
  const travel = height;

  const bodyStyle = useAnimatedStyle(() => ({
    transform: [{ translateY: travel * (1 - level.value) }],
  }));
  const strip1 = useAnimatedStyle(() => ({ transform: [{ translateX: -drift1.value * (W / 2) }] }));
  const strip2 = useAnimatedStyle(() => ({ transform: [{ translateX: drift2.value * (W / 2) - W / 2 }] }));

  return (
    <View style={[StyleSheet.absoluteFill, { overflow: 'hidden' }]} pointerEvents="none">
      <Animated.View style={[styles.body, { height: height + crestHeight }, bodyStyle]}>
        <Animated.View style={[styles.strip, strip2]}>
          <Svg width={W} height={crestHeight} viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none">
            <Path d={crest(78)} fill={colors.brandBright} opacity={0.5} />
          </Svg>
        </Animated.View>
        <Animated.View style={[styles.strip, strip1]}>
          <Svg width={W} height={crestHeight} viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none">
            <Defs>
              <LinearGradient id="wf" x1="0" y1="0" x2="0" y2="1">
                <Stop offset="0" stopColor={colors.brandBright} />
                <Stop offset="1" stopColor={colors.brand} />
              </LinearGradient>
            </Defs>
            <Path d={crest(62)} fill="url(#wf)" />
          </Svg>
        </Animated.View>
        <View style={styles.fill} />
      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  body: { position: 'absolute', left: 0, right: 0, bottom: 0 },
  strip: { position: 'absolute', top: 0, left: 0, width: W },
  fill: { flex: 1, marginTop: CREST - 1, backgroundColor: colors.brand },
});
