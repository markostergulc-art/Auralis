from pathlib import Path
import sys
r=Path(__file__).resolve().parents[1]
h=(r/'manual_apk/assets/index.html').read_text()
k=(r/'app/src/main/java/com/marko/auralis/audio/mix/AudioMixer.kt').read_text()
checks={
 'float_internal_mixer': 'Float32Array(totalFrames)' in h and 'FloatArray(frames * 2)' in k,
 'separate_lr': 'left=new Float32Array' in h and 'right=new Float32Array' in h,
 'pulse_waveform_unchanged': 'Math.exp(-0.6*i)' in h,
 'final_ceiling': 'const g=peak>.95?.95/peak:1' in h and 'ceiling / peak' in k and 'pcm[i] * gain' in k,
 'mix_peak_diag': 'diagMixPeak' in h and 'lastMixPeak' in h,
 'active_layer_diag': "[t('diagLayers'),activeLayerNames().join(' + ')||t('none')]" in h,
 'no_nan_guard_by_clamp': 'Math.abs(left[f])' in h,
 'unit_test_added': (r/'app/src/test/java/com/marko/auralis/audio/dsp/AudioMixerTest.kt').exists(),
}
for k,v in checks.items(): print(('PASS' if v else 'FAIL'),k)
f=[k for k,v in checks.items() if not v]
print(f'RESULT {len(checks)-len(f)}/{len(checks)}')
sys.exit(bool(f))
