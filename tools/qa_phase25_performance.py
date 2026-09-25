from pathlib import Path
import sys
r=Path(__file__).resolve().parents[1]
h=(r/'manual_apk/assets/index.html').read_text()
k=(r/'app/src/main/java/com/marko/auralis/audio/mix/AudioMixer.kt').read_text()
e=(r/'app/src/main/java/com/marko/auralis/audio/engine/ImpulseAudioEngine.kt').read_text()
checks={
 'payload_cache': 'payloadCache' in h and 'function audioPayload()' in h,
 'audio_signature_excludes_volume_timer': "const AUDIO_STATE_KEYS=['rate','balance'" in h and "'volume'" not in h[h.index('const AUDIO_STATE_KEYS='):h.index('function audioSignature')],
 'start_uses_cached_payload': 'const payload=audioPayload(),pcm=payload.pcm' in h and 'Android.startPcm(payload.b64' in h,
 'volume_no_pcm_rebuild': 'function setVolume(v)' in h and 'Android.setPcmVolume((state.volume/100)*routeSafetyGain)' in h,
 'inactive_generators_early_return': all(x in h for x in ["if(!state.ambientEnabled)return","if(!state.binauralEnabled)return","if(!state.monauralEnabled)return","if(!state.isoEnabled)return","if(!state.itdEnabled)return","if(!state.motionEnabled)return","if(!state.bilateralEnabled)return","if(state.altMode==='off')return"]),
 'sample_domain_scheduler': 'frame=Math.round(p*interval+jitter)' in h and 'actualDelayFrames(sr,requestedDelay)' in h,
 'web_timer_not_audio_scheduler': 'setInterval(()=>{if(playing)applyRouteSafety()' in h and 'frame=Math.round(p*interval+jitter)' in h,
 'advanced_render_cap': 'Math.min(sr,48000)' in h,
 'native_preallocated_pcm': 'val pcm = FloatArray(chunkFrames * 2)' in e and 'while (running.get()' in e,
 'native_reuses_pulse_shape': 'var activePulse = PulseMath.bipolarPulse(pulseSamples)' in e,
 'mixer_block_gain_not_clipping': 'ceiling / peak' in k and 'pcm[i] * gain' in k and 'coerceIn(-ceiling, ceiling)' not in k,
}
fail=[]
for n,v in checks.items():
 print(('PASS' if v else 'FAIL'),n)
 if not v: fail.append(n)
print(f'RESULT {len(checks)-len(fail)}/{len(checks)}')
sys.exit(bool(fail))
