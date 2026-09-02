import React, { useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import Svg, { Circle, Defs, G, Line, LinearGradient, Rect, Stop } from 'react-native-svg';
import { colors, type } from '@/theme';

const AXIS = 'rgba(255,255,255,0.22)';
const LABEL = 'rgba(255,255,255,0.72)';

export type Point = { x: number; value: number; label: string; caption?: string };

type ChartProps = {
  points: Point[];
  /** Dashed goal line, in the same unit as `value`. */
  goal: number;
  /** Y-axis tick labels, bottom to top. */
  yTicks: string[];
  yMax: number;
  xTicks: { at: number; label: string }[];
  /** Domain of x, inclusive. */
  xMin: number;
  xMax: number;
  unitLabel: string;
  /** Formats a bubble value, e.g. "0.2 L". */
  formatValue: (v: number) => string;
  variant: 'scatter' | 'bar';
  height?: number;
};

const PAD = { left: 42, right: 14, top: 26, bottom: 30 };

/**
 * One chart engine for all three History ranges: a scatter of individual
 * drinks for DAY, and bars for WEEK / MONTH.
 */
export function HydrationChart({
  points, goal, yTicks, yMax, xTicks, xMin, xMax, unitLabel, formatValue, variant, height = 320,
}: ChartProps) {
  const [width, setWidth] = useState(320);
  const [active, setActive] = useState<number | null>(null);

  const plotW = Math.max(1, width - PAD.left - PAD.right);
  const plotH = Math.max(1, height - PAD.top - PAD.bottom);

  const xOf = (x: number) => PAD.left + ((x - xMin) / Math.max(1e-6, xMax - xMin)) * plotW;
  const yOf = (v: number) => PAD.top + plotH - (Math.min(v, yMax) / Math.max(1e-6, yMax)) * plotH;

  const barWidth = useMemo(() => {
    const slots = Math.max(1, points.length);
    return Math.max(4, Math.min(26, (plotW / slots) * 0.42));
  }, [plotW, points.length]);

  // Auto-highlight the single non-empty column so a sparse chart still reads.
  const nonEmpty = points.filter((p) => p.value > 0);
  const highlighted = active ?? (nonEmpty.length === 1 ? points.indexOf(nonEmpty[0]) : null);

  return (
    <View onLayout={(e) => setWidth(e.nativeEvent.layout.width)}>
      <Text style={styles.unit}>Unit({unitLabel})</Text>
      <View style={{ height }}>
        <Svg width={width} height={height}>
          <Defs>
            <LinearGradient id="barfill" x1="0" y1="0" x2="0" y2="1">
              <Stop offset="0" stopColor="#FFFFFF" stopOpacity={0.95} />
              <Stop offset="1" stopColor="#FFFFFF" stopOpacity={0.55} />
            </LinearGradient>
          </Defs>

          {/* horizontal grid + y labels */}
          {yTicks.map((label, i) => {
            const v = (yMax / (yTicks.length - 1)) * i;
            const y = yOf(v);
            return (
              <G key={`y${i}`}>
                <Line x1={PAD.left} y1={y} x2={width - PAD.right} y2={y} stroke={AXIS} strokeWidth={1} />
              </G>
            );
          })}

          {/* goal line */}
          {goal > 0 && goal <= yMax ? (
            <Line
              x1={PAD.left}
              y1={yOf(goal)}
              x2={width - PAD.right}
              y2={yOf(goal)}
              stroke="rgba(255,255,255,0.85)"
              strokeWidth={1.5}
              strokeDasharray="5 5"
            />
          ) : null}

          {variant === 'bar'
            ? points.map((p, i) =>
                p.value > 0 ? (
                  <Rect
                    key={`b${i}`}
                    x={xOf(p.x) - barWidth / 2}
                    y={yOf(p.value)}
                    width={barWidth}
                    height={Math.max(2, PAD.top + plotH - yOf(p.value))}
                    rx={Math.min(barWidth / 2, 7)}
                    fill="url(#barfill)"
                  />
                ) : null
              )
            : points.map((p, i) => (
                <Circle key={`c${i}`} cx={xOf(p.x)} cy={yOf(p.value)} r={6} fill="#FFFFFF" />
              ))}
        </Svg>

        {/* y tick labels */}
        {yTicks.map((label, i) => {
          const v = (yMax / (yTicks.length - 1)) * i;
          return (
            <Text key={`yl${i}`} style={[styles.yLabel, { top: yOf(v) - 8 }]}>
              {label}
            </Text>
          );
        })}

        {/* x tick labels */}
        {xTicks.map((t, i) => (
          <Text key={`xl${i}`} style={[styles.xLabel, { left: xOf(t.at) - 20, top: height - PAD.bottom + 8 }]}>
            {t.label}
          </Text>
        ))}

        {/* value bubble */}
        {highlighted !== null && points[highlighted] && points[highlighted].value > 0 ? (
          <View
            pointerEvents="none"
            style={[
              styles.bubble,
              {
                left: Math.max(2, Math.min(width - 96, xOf(points[highlighted].x) - 44)),
                top: Math.max(0, yOf(points[highlighted].value) - (points[highlighted].caption ? 62 : 44)),
              },
            ]}
          >
            {points[highlighted].caption ? (
              <Text style={styles.bubbleCaption}>{points[highlighted].caption}</Text>
            ) : null}
            <Text style={styles.bubbleValue}>{formatValue(points[highlighted].value)}</Text>
          </View>
        ) : null}

        {/* touch targets */}
        <View style={StyleSheet.absoluteFill} pointerEvents="box-none">
          {points.map((p, i) => (
            <Pressable
              key={`t${i}`}
              onPress={() => setActive(active === i ? null : i)}
              style={{
                position: 'absolute',
                left: xOf(p.x) - Math.max(14, barWidth),
                top: PAD.top,
                width: Math.max(28, barWidth * 2),
                height: plotH,
              }}
            />
          ))}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  unit: { ...type.small, color: '#FFFFFF', marginBottom: 4 },
  yLabel: { position: 'absolute', left: 0, width: 38, textAlign: 'right', color: LABEL, fontSize: 12, fontWeight: '600' },
  xLabel: { position: 'absolute', width: 40, textAlign: 'center', color: LABEL, fontSize: 12, fontWeight: '600' },
  bubble: {
    position: 'absolute',
    minWidth: 88,
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: 10,
    backgroundColor: '#0B1220',
    alignItems: 'center',
  },
  bubbleCaption: { color: 'rgba(255,255,255,0.6)', fontSize: 12, fontWeight: '700' },
  bubbleValue: { color: '#FFFFFF', fontSize: 15, fontWeight: '800' },
});

export const chartColors = colors;
