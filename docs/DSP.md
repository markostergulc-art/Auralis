# DSP design notes

## Event timing

The requested pulse-rate value is interpreted as the total number of impulse events per second, not per channel. At 2.0 events/s the scheduler therefore advances approximately every 0.5 seconds and assigns each event to a single channel.

The scheduler stores event locations as absolute PCM frame numbers. Compose recomposition, Android `Handler`, wall-clock timers and `Thread.sleep()` do not determine the audio event position.

## Stereo invariant

Each `PulseEvent` contains one `PulseChannel`: `LEFT` or `RIGHT`. There is no dual-channel event type. Event frame numbers are strictly monotonic and spaced by at least `minimumStereoSeparationFrames`.

The default `ALTERNATE` mode produces L/R/L/R. `BALANCED_RANDOM` permits stochastic ordering but forces the cumulative channel-count difference to remain within two events.

## Pulse shape

A one-sample unipolar/DC spike was intentionally avoided. The engine emits a very short zero-mean bipolar transient:

- 2 samples: `[+1, -1]`
- 3 samples: `[+0.5, -1, +0.5]`
- longer durations: one normalized discrete sine cycle with numerical mean removed

This keeps the transient extremely short while avoiding accumulated DC offset in the generated PCM stream.

## Duration quantization

`samples = round(sampleRate * requestedMicroseconds / 1,000,000)` with a minimum of two samples for the bipolar design. The actual duration is `samples * 1,000,000 / sampleRate`.

Consequently, the UI never claims arbitrary microsecond precision. Diagnostics exposes the real sample count and duration.

## Amplitude

Digital pulse peak is capped at 0.75 before user-volume scaling. Random Dynamics reduces individual event amplitude stochastically; it never raises an event above the configured master level. At 100% dynamics the current implementation ranges from 25% to 100% of the master pulse amplitude.
