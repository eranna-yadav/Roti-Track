"""Synthesises the water sounds in app/src/main/res/raw (mono 16-bit WAV, 22.05 kHz).

Run: python3 tools/make_sounds.py
"""
import math
import random
import struct
import wave
from pathlib import Path

RATE = 22050
OUT = Path(__file__).resolve().parent.parent / "app/src/main/res/raw"


def drop(buf, at, pitch=1.0, gain=0.8):
    """A single drip: a short sine whose pitch glides up, as a bubble does when it pops."""
    start = int(at * RATE)
    n = int(0.16 * RATE)
    phase = 0.0
    for i in range(n):
        t = i / RATE
        f = (450 + 1500 * (1 - math.exp(-t * 40))) * pitch
        phase += 2 * math.pi * f / RATE
        env = (1 - math.exp(-t * 900)) * math.exp(-t * 28)
        j = start + i
        if j < len(buf):
            buf[j] += gain * env * math.sin(phase)


def echo(buf, delay=0.07, decay=0.3, taps=3):
    """A little room tail so drops don't sound dry."""
    d = int(delay * RATE)
    out = buf[:]
    for k in range(1, taps + 1):
        g = decay ** k
        for i in range(k * d, len(buf)):
            out[i] += g * buf[i - k * d]
    return out


def stream(buf, low, high, bubbles, pitch_range, seed, level=0.35):
    """Running water: band-passed noise that swells and dips, with bubbles on top."""
    rnd = random.Random(seed)
    lp1 = lp2 = 0.0
    a_hi = 1 - math.exp(-2 * math.pi * high / RATE)
    a_lo = 1 - math.exp(-2 * math.pi * low / RATE)
    mod_phases = [rnd.random() * 6.28 for _ in range(3)]
    total = len(buf)
    for i in range(total):
        x = rnd.uniform(-1, 1)
        lp1 += a_hi * (x - lp1)          # remove hiss above `high`
        lp2 += a_lo * (lp1 - lp2)        # remove rumble below `low`
        band = lp1 - lp2
        t = i / RATE
        swell = 0.75 + 0.25 * sum(math.sin(2 * math.pi * r * t + p) for r, p in zip((0.7, 1.9, 4.3), mod_phases)) / 3
        buf[i] += level * 3.2 * band * swell
    seconds = total / RATE
    for _ in range(int(bubbles * seconds)):
        drop(buf, rnd.uniform(0, seconds - 0.2), rnd.uniform(*pitch_range), rnd.uniform(0.08, 0.3))


def finish(buf, name, fade_in=0.02, fade_out=0.4):
    n = len(buf)
    for i in range(int(fade_in * RATE)):
        buf[i] *= i / (fade_in * RATE)
    fo = int(fade_out * RATE)
    for i in range(fo):
        buf[n - 1 - i] *= i / fo
    peak = max(abs(v) for v in buf) or 1
    scale = 0.89 / peak
    OUT.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUT / f"{name}.wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(v * scale * 32767)) for v in buf))


def silence(seconds):
    return [0.0] * int(seconds * RATE)


# Water drop 1 (1 s): one drip and a smaller one after it.
b = silence(1.0)
drop(b, 0.02, 1.0, 0.9)
drop(b, 0.38, 1.25, 0.35)
finish(echo(b), "water_drop_1", fade_out=0.15)

# Water drop 2 (3 s): a tap dripping into a full glass.
b = silence(3.0)
for at, p, g in [(0.02, 1.0, 0.9), (0.55, 0.85, 0.7), (1.05, 1.15, 0.6), (1.5, 0.95, 0.8), (1.95, 1.3, 0.45), (2.4, 0.9, 0.7)]:
    drop(b, at, p, g)
finish(echo(b), "water_drop_2", fade_out=0.3)

# Water flowing 1 (5 s): a gentle trickle.
b = silence(5.0)
stream(b, 900, 4500, bubbles=9, pitch_range=(1.1, 1.8), seed=1, level=0.25)
finish(b, "water_flowing_1", fade_in=0.3, fade_out=0.8)

# Water flowing 2 (7 s): pouring into a glass.
b = silence(7.0)
stream(b, 300, 2500, bubbles=6, pitch_range=(0.7, 1.2), seed=2, level=0.4)
finish(b, "water_flowing_2", fade_in=0.4, fade_out=1.0)

# Water flowing 3 (9 s): a small stream over stones.
b = silence(9.0)
stream(b, 500, 3500, bubbles=14, pitch_range=(0.8, 1.6), seed=3, level=0.35)
finish(b, "water_flowing_3", fade_in=0.6, fade_out=1.2)
