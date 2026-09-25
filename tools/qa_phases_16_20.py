from pathlib import Path
root=Path(__file__).resolve().parents[1]
html=(root/'manual_apk/assets/index.html').read_text()
repo=(root/'app/src/main/java/com/marko/auralis/data/SettingsRepository.kt').read_text()
preset=(root/'app/src/main/java/com/marko/auralis/model/Preset.kt').read_text()
exp=(root/'app/src/main/java/com/marko/auralis/model/ExperimentModels.kt').read_text()
svc=(root/'app/src/main/java/com/marko/auralis/service/PlaybackService.kt').read_text()
eng=(root/'app/src/main/java/com/marko/auralis/audio/engine/ImpulseAudioEngine.kt').read_text()
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
builder=(root/'manual_apk/build_manual_apk.py').read_text()
checks={
'16 schema v2':'CURRENT_SCHEMA = 2' in preset and 'schemaVersion:PRESET_SCHEMA' in html,
'16 complete layer preset':'put("layers", encodeLayers(preset.layers))' in repo and 'ambientBrightness:state.ambientBrightness' in html and 'altMode:state.altMode' in html,
'16 v1 migration':'schemaVersion:Number(x&&x.schemaVersion)||1' in html,
'17 experiment local':'brainrelax.v1.experimentResults' in html and 'selected.length<2||selected.length>5' in html,
'17 random/blind':'Math.floor(Math.random()*selected.length)' in html and 'hiddenProtocol' in html,
'18 min sessions':'MIN_SESSIONS = 5' in exp and 'g.length<5' in html,
'18 no clinical claim':'not proof of clinical effectiveness' in html,
'19 no fake SPL':'not a calibrated dB SPL measurement' in html,
'19 route safety':'setSafetyGain' in eng and 'HEADPHONE_START_GAIN' in svc,
'19 manual route bridge':'getOutputDeviceType' in builder,
'20 media FGS':'FOREGROUND_SERVICE_MEDIA_PLAYBACK' in manifest and 'foregroundServiceType="mediaPlayback"' in manifest,
'20 focus after FGS':svc.find('startAsForeground()') < svc.find('if (!requestAudioFocus())'),
'20 duck':'AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK' in svc and 'setFocusGain(0.20f)' in svc,
'20 noisy':'ACTION_AUDIO_BECOMING_NOISY' in svc,
'20 route callback':'registerAudioDeviceCallback' in svc,
}
for k,v in checks.items(): print(('PASS' if v else 'FAIL'), '-', k)
print(f'RESULT {sum(checks.values())}/{len(checks)}')
if not all(checks.values()): raise SystemExit(1)
