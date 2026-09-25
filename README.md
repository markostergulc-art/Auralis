# Auralis v2.0.1

Experimental auditory-relaxation and psychoacoustic exploration platform. It is **not a medical device** and does not claim to diagnose, treat, prevent or cure medical conditions.

## Platform

- Full Android source: Kotlin / Jetpack Compose / Material 3
- Installable fallback release: offline WebView UI + native Android `AudioTrack` PCM bridge
- `applicationId`: `com.marko.auralis`
- `minSdk 26`, `targetSdk 36`
- Offline-first; fallback APK declares no `INTERNET` permission
- EN / DE / HR UI and educational content
- Persistent signing identity retained for Auralis v2.x upgrade-compatible sideload releases

## Core pulse engine

- Pulse Rate: **1.0–6.0 events/s**
- Pulse Duration: **40–90 µs requested**, sample-quantized at runtime
- Random Pulse Timing: **0–50%**
- Random Dynamics: **0–50%**
- Balance: **-100% .. 0 .. +100%**
- Stereo behavior: Random Left / Right channel
- Independent Random Left delay: **1–100 ms**
- Independent Random Right delay: **1–100 ms**
- Huawei/handset-friendly asymmetric DC-balanced biphasic transient retained

## Sound architecture implemented through Phase 30

### Core / Relax
- Existing pulse engine
- Breath Pacing (4/6 default plus configurable inhale/exhale/pause)
- Procedural White / Pink / Brown Noise
- Procedural Rain / Ocean / Stream / Wind / Forest soundscapes
- Procedural Ambient layer

### Beats
- Binaural Beats
- Monaural Beats
- Isochronic modulation

### Spatial
- physiological-scale ITD (20–800 µs, sample-quantized)
- Stereo Lead/Lag
- Slow Stereo Motion
- Bilateral alternating audio

### Experimental / Alternative
- 432 Hz pure and 432-tuned ambient
- 528 Hz pure / harmonic
- Solfeggio frequency bank
- 7.83 Hz amplitude modulation (explicitly described as audio modulation, not the Earth's EM Schumann field)
- Synthetic Singing Bowl
- Sound Bath synthesis
- Humming / OM-style breathing guide

### Research
- 40 Hz auditory modulation
- alternate pulse shapes
- extended pulse-duration experiments

### Personal experimentation
- Preset schema v2 with migration from legacy pulse-only presets
- My Sound Experiment: randomized 2–5 protocol N-of-1 sessions
- pre/post subjective ratings stored locally only
- results shown as **Your observed response**, not clinical effectiveness

## Scientific information model

Every major method has localized EN/DE/HR information covering:

- What is this?
- What happens in the sound?
- How to use it
- What you may notice
- Headphones or speaker?
- Recommended starting point
- Evidence category
- Important limitation
- Safety

Evidence labels: **Evidence Supported**, **Emerging Evidence**, **Experimental**, **Traditional / Exploratory**, and **Research Only**.

## Hearing safety

The app does not invent dB SPL values. App gain is not calibrated acoustic SPL. Playback uses conservative transient levels, route-aware safety gain, headphone-route handling in the full native source, and clear safe-listening information.

## Diagnostics

Diagnostics include sample rate, requested/actual pulse duration, sample count, Lead/Lag, requested/actual ITD, active layers, mix peak/protective gain, beat/modulation parameters, breathing cycle, CPU and PSS memory usage, output route and safety gain where available.

## Validation

Final Phases 26–30 add:

- numeric DSP regression and sample-rate matrix tests
- White/Pink/Brown spectral-slope checks
- binaural frequency measurement
- ITD frame-offset checks
- mixer ceiling/headroom tests
- scientific-content claim audit
- UI regression checks
- defensive Huawei / Pixel-GrapheneOS / Samsung / Bluetooth compatibility checks
- APK binary/signing/integrity validation

Physical ADB execution on Huawei P40, Pixel/GrapheneOS and Samsung hardware was **not available** in the build environment. Device compatibility therefore combines defensive implementation and multi-sample-rate DSP validation, not a claim of physical device execution.

## SDK-independent release build

```bash
cd manual_apk
./build_release.sh
```

This produces the signed V1/V2/V3 APK without a local Android SDK/Gradle installation.

## Signing

Private signing material is deliberately excluded from this repository and the GitHub-safe ZIP. Restore your existing key and local `manual_apk/keystore.properties` privately before running the release script. Never generate a replacement key for an upgrade-compatible release. See [signing instructions](manual_apk/README_SIGNING.md).

## Native Android build

Use JDK 17 and Android SDK 36. This source archive includes Gradle configuration and wrapper properties, but no `gradlew`, `gradlew.bat`, or wrapper JAR. Install Gradle 8.13 locally, then run `gradle :app:assembleDebug` or generate the wrapper locally. Dependency resolution requires network access. A native Gradle build was not verified during this import.

## Import verification

See `docs/IMPORT_QA.md` for checks performed on this source. Historical QA scripts are included under `tools/`; the separate original v2.0.1 QA/build report files were not supplied with the archive. Their prior reported results have not been reproduced by this import.
