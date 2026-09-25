from pathlib import Path
import sys, re
r=Path(__file__).resolve().parents[1]
h=(r/'manual_apk/assets/index.html').read_text()
k=(r/'app/src/main/java/com/marko/auralis/audio/generators/AdvancedGenerators.kt').read_text()
sc=(r/'app/src/main/java/com/marko/auralis/model/SessionProtocols.kt').read_text()
mc=(r/'app/src/main/java/com/marko/auralis/model/SoundLayerConfig.kt').read_text()
phases={
6:{'ambient_ui':'ambientEnabled' in h,'ambient_dsp':'function mixAmbient' in h and 'fun ambient(' in k,'ambient_controls':all(x in h for x in ['ambientBrightness','ambientMovement','ambientDensity','ambientLevel']),'ambient_info':all(x in h for x in ["title:'Procedural Ambient'","title:'Prozedurales Ambient'","title:'Proceduralni ambijent'"]),'ambient_not_generic':'No chord or tuning used here is claimed to have a unique biological effect.' in h},
7:{'binaural_ui':'binauralEnabled' in h,'binaural_range':'min="100" max="600"' in h and 'min="1" max="12"' in h,'binaural_dsp':'function mixBinaural' in h and 'fun binaural(' in k,'binaural_separate_lr':'state.binauralCarrier+state.binauralBeat' in h,'binaural_info':all(x in h for x in ["title:'Binaural Beats'","title:'Binaurale Beats'","title:'Binauralni beatovi'"]),'binaural_limitation':'does not prove that the whole brain enters a 6 Hz theta state' in h},
8:{'monaural_ui':'monauralEnabled' in h,'monaural_dsp':'function mixMonaural' in h and 'fun monaural(' in k,'physical_mix':'Math.sin(p1)+Math.sin(p2)' in h,'depth_control':'monauralDepth' in h,'monaural_info':all(x in h for x in ["title:'Monaural Beats'","title:'Monaurale Beats'","title:'Monauralni beatovi'"])},
9:{'iso_ui':'isoEnabled' in h,'iso_1_12_ui':'id="isoRate" type="number" min="1" max="12"' in h,'iso_dsp':'function mixIso' in h and 'fun isochronic(' in k,'soft_envelope':'isoSoft' in h and 's*sinus' in k,'iso_info':all(x in h for x in ["title:'Isochronic Modulation'","title:'Isochrone Modulation'","title:'Izokronična modulacija'"])},
10:{'itd_ui':'itdEnabled' in h,'itd_range':'id="itdUs" type="range" min="20" max="800"' in h,'itd_quantization':'Math.round(sr*state.itdUs/1e6)' in h,'actual_itd_ui':'itdActual' in h,'itd_dsp':'function mixItd' in h and 'fun spatialItd(' in k,'leadlag_preserved':'stereoDelayUs' in h and '1,100' in h,'itd_info':'ITD is a well-established cue for horizontal sound localization' in h},
11:{'motion_ui':'motionEnabled' in h,'cycle_4_30':'id="motionCycle" type="range" min="4" max="30"' in h,'motion_dsp':'function mixMotion' in h and 'fun smoothStereo(' in k,'sine_pan':'Math.sin(2*Math.PI*t/state.motionCycle)' in h,'motion_info':"title:'Slow Stereo Motion'" in h and "title:'Sporo stereo kretanje'" in h},
12:{'bilateral_ui':'bilateralEnabled' in h,'rate_range':'id="bilateralRate" type="number" min="0.5" max="4"' in h,'bilateral_dsp':'function mixBilateral' in h and 'fun bilateral(' in k,'smoothed_crossfade':'Math.tanh' in h and 'tanh(' in k,'emdr_limitation':'alternating audio alone is not equivalent to EMDR therapy' in h},
13:{'alt_modes':all(x in h for x in ['432pure','432ambient','528pure','528ambient','solfeggio','schumann','bowl','soundbath','humming']),'solfeggio_set':all(f'<option>{x}</option>' in h or f'<option selected>{x}</option>' in h for x in [174,285,396,417,528,639,741,852,963]),'alt_dsp':'function mixAlternative' in h and 'object ExperimentalSoundGenerator' in k,'schumann_is_modulation':'7.83*t' in h and 'mod783' in k,'bowl_modal':all(x in h for x in ['331.7','512.3','734.1','1012.5']),'alt_three_languages':all(x in h for x in ["432-Stimmung","432 tuning","7,83-Hz-Experiment","7,83 Hz eksperimentalna modulacija"]),'no_dna_claim':'DNA repair or cellular regeneration are not established' in h},
14:{'research_ui':'researchMode' in h,'40hz_option':'value="40hz"' in h,'research_40_dsp':'2*Math.PI*40*t' in h and 'gamma40' in k,'pulse_shapes':all(x in h for x in ['huawei','soft','gaussian','sine']),'extended_durations':all(f'value="{x}"' in h for x in [50,100,250,500,1000,2000,5000]),'normal_pulse_preserved':"state.researchMode==='pulse'?researchShape" in h,'research_info':'40 Hz can evoke measurable gamma-frequency responses' in h},
15:{'session_ui':'sessionProtocol' in h and 'applySession' in h,'eight_protocols':all(x in h for x in ['calm','deepRelax','focusedCalm','spatialCalm','pulseExplore','binaural6','exp432','exp528']),'no_autoplay':'does not start playback automatically' in h,'apply_logic':('switch(state.sessionProtocol)' in h or 'switch(id)' in h) and 'applySessionProtocol' in h,'source_protocols':'SessionProtocol("calm"' in sc and 'SessionProtocol("528_experimental"' in sc,'session_info':'named combination itself has not been clinically validated' in h},
}
failed=[]
for phase,checks in phases.items():
 print(f'PHASE {phase}')
 for n,v in checks.items():
  print(('PASS' if v else 'FAIL'),n)
  if not v: failed.append((phase,n))
 print(f'RESULT {sum(checks.values())}/{len(checks)}')
print('SOUND_LAYER_CONFIG', 'PASS' if 'fun sanitized()' in mc else 'FAIL')
if 'fun sanitized()' not in mc: failed.append(('model','sanitized'))
print('TOTAL',sum(len(v) for v in phases.values())+1-len(failed),'/',sum(len(v) for v in phases.values())+1)
sys.exit(bool(failed))
