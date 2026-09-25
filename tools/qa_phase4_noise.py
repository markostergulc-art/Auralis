from pathlib import Path
import sys
r=Path(__file__).resolve().parents[1]
h=(r/'manual_apk/assets/index.html').read_text(); k=(r/'app/src/main/java/com/marko/auralis/audio/generators/NoiseGenerator.kt').read_text()
checks={
 'three_noise_types': all(v in h for v in ['value="white"','value="pink"','value="brown"']),
 'procedural_no_assets': 'mixNoise(left,right,sr,totalFrames)' in h and 'no prerecorded loop' in h,
 'pink_filter_real': '.99886*b0L' in h and '0.0555179' in k,
 'brown_integrator': 'brownL=(brownL+.02*wL)/1.02' in h and '(brown + 0.02*white) / 1.02' in k,
 'white_direct_random': "state.noiseType==='pink'" in h and "state.noiseType==='brown'" in h,
 'loop_crossfade': 'seamCrossfade(nl,nr,sr)' in h,
 'level_bounded_25': 'noiseLevel=Math.round(clamp(Number(state.noiseLevel)||12,0,25))' in h,
 'info_en_de_hr': all(x in h for x in ["title:'White Noise'","title:'Weißes Rauschen'","title:'Bijeli šum'","title:'Pink Noise'","title:'Ružičasti šum'","title:'Brown Noise'","title:'Smeđi šum'"]),
 'pink_limitation_specific': 'phase-locked pink-noise pulses' in h,
 'brown_no_false_evidence': 'Online popularity of brown noise is not evidence' in h,
 'kotlin_generator': 'enum class Type { WHITE, PINK, BROWN }' in k,
}
for k,v in checks.items():print(('PASS' if v else 'FAIL'),k)
f=[k for k,v in checks.items() if not v];print(f'RESULT {len(checks)-len(f)}/{len(checks)}');sys.exit(bool(f))
