#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${TMPDIR:-/tmp}/brain-relax-dsp-test.jar"
kotlinc \
  "$ROOT/app/src/main/java/com/marko/auralis/model/AudioConfig.kt" \
  "$ROOT/app/src/main/java/com/marko/auralis/audio/dsp/PulseMath.kt" \
  "$ROOT/app/src/main/java/com/marko/auralis/audio/scheduler/StereoPulseScheduler.kt" \
  "$ROOT/tools/PureDspSmokeTest.kt" \
  -include-runtime -d "$OUT"
java -jar "$OUT"
