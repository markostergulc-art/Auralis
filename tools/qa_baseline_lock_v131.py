from pathlib import Path
import re, sys
root=Path(__file__).resolve().parents[1]
html=(root/'manual_apk/assets/index.html').read_text()
config=(root/'app/src/main/java/com/marko/auralis/model/AudioConfig.kt').read_text()
engine=(root/'app/src/main/java/com/marko/auralis/audio/engine/ImpulseAudioEngine.kt').read_text()
pulse=(root/'app/src/main/java/com/marko/auralis/audio/dsp/PulseMath.kt').read_text()
builder=(root/'manual_apk/build_manual_apk.py').read_text()
checks={
 'package': 'com.marko.auralis' in builder,
 'release_version': 'versionCode' in builder and 'versionName' in builder,
 'pulse_rate_1_6': 'clamp(Number(state.rate)||2,1,6)' in html and 'coerceIn(1.0f, 6.0f)' in config,
 'pulse_duration_40_90': 'clamp(Number(state.durationUs)||55,40,90)' in html and 'coerceIn(40, 90)' in config,
 'random_timing_0_50': 'clamp(Number(state.randomTiming)||0,0,50)' in html and 'coerceIn(0, 50)' in config,
 'random_dynamics_0_50': 'clamp(Number(state.dynamics)||0,0,50)' in html and 'randomDynamicsPercent = randomDynamicsPercent.coerceIn(0, 50)' in config,
 'stereo_delay_1_100_ms': 'clamp(Number(state.stereoDelayUs)||30,1,100)' in html and 'stereoDelayMs = stereoDelayMs.coerceIn(1, 100)' in config,
 'huawei_waveform': 'Math.exp(-0.6*i)' in html and 'exp(-0.6 * i)' in pulse,
 'preset_update': 'data-update' in html and 'updatePreset' in (root/'app/src/main/java/com/marko/auralis/data/SettingsRepository.kt').read_text(),
 'languages': all(x in html for x in ["en:{", "de:{", "hr:{"]),
 'native_bridge': 'startPcm' in builder and 'getNativeSampleRate' in builder,
 'no_internet_permission': 'android.permission.INTERNET' not in builder and 'android.permission.INTERNET' not in (root/'app/src/main/AndroidManifest.xml').read_text(),
 'signing_material': (root/'manual_apk/keys/BrainRelaxSoundPerception_release.p12').exists(),
}
for k,v in checks.items(): print(('PASS' if v else 'FAIL'), k)
failed=[k for k,v in checks.items() if not v]
print(f'RESULT {len(checks)-len(failed)}/{len(checks)}')
sys.exit(1 if failed else 0)
