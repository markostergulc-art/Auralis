# Auralis 1.0.0

Initial implementation.

- Native Kotlin / Jetpack Compose Material 3 app.
- Android 16 target, Android 8.0 minimum.
- PCM_FLOAT `AudioTrack` stereo impulse engine.
- 1.0–4.0 total impulses/s control with slider, +/- and numeric entry.
- 40–70 µs requested pulse duration with actual sample-domain duration diagnostics.
- DC-balanced bipolar pulse generator.
- Alternating and balanced-random stereo scheduling with no simultaneous L/R event frame.
- 0–100% app volume in exact 1% steps.
- 0–100% Random Dynamics amplitude variation.
- Timer up to 12 h 59 min.
- Foreground background playback, audio focus and route-change handling.
- Personal local presets and DataStore persistence.
- Light/dark/system theme and accessibility-oriented Compose UI.
- No Internet, analytics, account, microphone or recording requirement.
- Pure DSP smoke test with a 5,000,000-event scheduler validation.
