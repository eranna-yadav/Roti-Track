#!/usr/bin/env python3
"""Synthesise the app's water sound effects into assets/sounds.

Standard library only — run with `python3 scripts/generate-sounds.py`.
"""
import math, random, struct, wave, os
SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'assets', 'sounds')
random.seed(7)

def write(name, samples):
    path = os.path.join(OUT, name)
    with wave.open(path, 'w') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        frames = bytearray()
        for s in samples:
            v = max(-1.0, min(1.0, s))
            frames += struct.pack('<h', int(v * 32000))
        w.writeframes(bytes(frames))
    print(name, round(len(samples)/SR, 2), 's')

def drop(buf, start, f0=1350.0, decay=26.0, gain=0.62, bend=2.4):
    """A single droplet: fast upward pitch bend with exponential decay."""
    n = int(0.42 * SR)
    phase = 0.0
    for i in range(n):
        t = i / SR
        f = f0 * (1 + bend * t)
        phase += 2 * math.pi * f / SR
        env = math.exp(-decay * t)
        idx = start + i
        if idx < len(buf):
            buf[idx] += gain * env * (math.sin(phase) + 0.25 * math.sin(2 * phase))

def plop(buf, start, gain=0.3):
    """Low body thump under a droplet."""
    n = int(0.18 * SR)
    phase = 0.0
    for i in range(n):
        t = i / SR
        f = 210 * math.exp(-9 * t) + 90
        phase += 2 * math.pi * f / SR
        idx = start + i
        if idx < len(buf):
            buf[idx] += gain * math.exp(-22 * t) * math.sin(phase)

def stream(seconds, gain=0.2, cutoff=0.32, bubbles=True):
    """Filtered noise that reads as running water, with a few bubbles on top."""
    n = int(seconds * SR)
    buf = [0.0] * n
    lp = 0.0; hp = 0.0; prev = 0.0
    for i in range(n):
        white = random.uniform(-1, 1)
        lp += cutoff * (white - lp)              # low-pass
        hp = 0.92 * (hp + lp - prev); prev = lp  # high-pass -> band of "hiss"
        t = i / SR
        wobble = 0.75 + 0.25 * math.sin(2 * math.pi * 0.7 * t) * math.sin(2 * math.pi * 0.23 * t)
        buf[i] = gain * hp * wobble
    if bubbles:
        k = int(seconds * 3)
        for _ in range(k):
            at = random.randint(0, max(0, n - int(0.5 * SR)))
            drop(buf, at, f0=random.uniform(900, 2100), decay=random.uniform(24, 40),
                 gain=random.uniform(0.12, 0.3), bend=random.uniform(1.5, 3.5))
    # fade in/out
    f = int(0.12 * SR)
    for i in range(f):
        buf[i] *= i / f
        buf[n - 1 - i] *= i / f
    return buf

# Water drop 1 — 1s, single clean droplet
b = [0.0] * int(1.0 * SR)
plop(b, 0, 0.34); drop(b, 0, 1420, 24, 0.6, 2.6)
write('water-drop-1.wav', b)

# Water drop 2 — 3s, a small cascade of droplets
b = [0.0] * int(3.0 * SR)
for at, f0, g in [(0.05, 1500, 0.6), (0.62, 1180, 0.5), (1.28, 1720, 0.45), (2.05, 1320, 0.4)]:
    s = int(at * SR)
    plop(b, s, 0.24 * g / 0.6); drop(b, s, f0, 25, g, 2.4)
write('water-drop-2.wav', b)

# Water flowing 1/2/3 — 5s, 7s, 9s streams of increasing body
write('water-flowing-1.wav', stream(5, gain=0.20, cutoff=0.30))
write('water-flowing-2.wav', stream(7, gain=0.24, cutoff=0.24))
write('water-flowing-3.wav', stream(9, gain=0.27, cutoff=0.18))
