# Sound effects

These five WAV files are synthesised rather than sampled, so the repository
carries no third-party audio licences.

- `water-drop-1` / `water-drop-2` — a pitch-bent sine with an exponential decay
  over a short low-frequency body, one droplet and then a small cascade.
- `water-flowing-1/2/3` — band-passed white noise with a slow amplitude wobble
  and randomly placed droplets on top, at increasing body and length.

The generator that produced them is a short Python script using only the
standard library (`wave`, `math`, `random`); regenerate them by re-running it if
you want different timbres.
