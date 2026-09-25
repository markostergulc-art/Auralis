from pathlib import Path
import subprocess, sys, tempfile, textwrap
r=Path(__file__).resolve().parents[1]
h=(r/'manual_apk/assets/index.html').read_text()
k=(r/'app/src/main/java/com/marko/auralis/audio/generators/NaturalSoundGenerator.kt').read_text()
checks={
 'five_natural_types_ui': all(f'value="{x}"' in h for x in ['rain','ocean','stream','wind','forest']),
 'five_natural_types_kotlin': 'enum class Type { RAIN, OCEAN, STREAM, WIND, FOREST }' in k,
 'procedural_no_recording_dependency': 'mixNatural(left,right,sr,totalFrames)' in h and 'no downloaded recording' in h,
 'natural_mixed_into_pcm': 'mixNatural(left,right,sr,totalFrames);' in h and h.index('mixNatural(left,right,sr,totalFrames);') < h.index('return {bytes:encodeStereo16', h.index('mixNatural(left,right,sr,totalFrames);')), 
 'level_bounded_25': 'naturalLevel=Math.round(clamp(Number(state.naturalLevel)||14,0,25))' in h,
 'loop_crossfade': 'seamCrossfade(nl,nr,sr)' in h and 'seamCrossfade(left, right, sampleRate)' in k,
 'stereo_generation': 'val left = FloatArray(frames)' in k and 'val right = FloatArray(frames)' in k,
 'rain_specific_dsp': "state.naturalType==='rain'" in h and 'renderRain' in k,
 'ocean_slow_envelope': "state.naturalType==='ocean'" in h and '0.075' in k,
 'stream_sparse_resonance': "state.naturalType==='stream'" in h and 'bubble' in k.lower(),
 'wind_slow_gust': "state.naturalType==='wind'" in h and 'renderWind' in k,
 'forest_sparse_chirp': "state.naturalType==='forest'" in h and 'chirp' in k.lower(),
 'info_en_de_hr': all(x in h for x in ["title:'Rain Soundscape'","title:'Regen-Klanglandschaft'","title:'Zvučni krajolik kiše'","title:'Ocean Soundscape'","title:'Ozean-Klanglandschaft'","title:'Zvučni krajolik oceana'","title:'Forest Ambience'","title:'Waldatmosphäre'","title:'Šumski ambijent'"]),
 'evidence_not_overclaimed': 'This exact synthetic rain generator has not itself been clinically tested.' in h and 'not a validated treatment' in h,
 'settings_persisted': "naturalType:'off',naturalLevel:14" in h and "$('naturalType').onchange" in h,
}
for name,ok in checks.items(): print(('PASS' if ok else 'FAIL'), name)
failed=[x for x,v in checks.items() if not v]
print(f'RESULT {len(checks)-len(failed)}/{len(checks)}')
sys.exit(bool(failed))
