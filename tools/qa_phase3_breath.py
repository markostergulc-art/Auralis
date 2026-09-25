from pathlib import Path
import sys
r=Path(__file__).resolve().parents[1]
h=(r/'manual_apk/assets/index.html').read_text()
k=(r/'app/src/main/java/com/marko/auralis/audio/generators/BreathPacingGenerator.kt').read_text()
checks={
 'sound_modes_entry': 'data-open="soundModes"' in h,
 'pulse_layer_toggle': 'id="pulseEnabled"' in h,
 'breath_layer_toggle': 'id="breathEnabled"' in h,
 'breath_ranges': all(x in h for x in ['min="3" max="8"','min="3" max="10"','min="0" max="3"']),
 'default_4_6': 'breathIn:4,breathOut:6' in h,
 'deterministic_audio_timing': 'mixBreath(left,right,sr,totalFrames)' in h and 'const t=(f/sr)%cycle' in h,
 'no_ui_timer_audio': 'setInterval' not in h[h.index('function mixBreath'):h.index('function shape')],
 'cycle_aligned_buffer': 'cycle*Math.max(1,Math.ceil(20/cycle))' in h,
 'info_three_languages': all(x in h for x in ["title:'Breath Pacing'","title:'Atemrhythmus'","title:'Vođenje disanja'"]),
 'specific_6_bpm_explanation': '6 breaths per minute' in h and '6 Atemzüge pro Minute' in h and '6 udaha u minuti' in h,
 'kotlin_generator': 'BreathPacingGenerator' in k and '180.0 + 90.0' in k,
 'existing_pulse_optional_not_removed': 'addPulseFloat' in h and 'Math.exp(-0.6*i)' in h,
}
for k,v in checks.items():print(('PASS' if v else 'FAIL'),k)
f=[k for k,v in checks.items() if not v]
print(f'RESULT {len(checks)-len(f)}/{len(checks)}')
sys.exit(bool(f))
