from pathlib import Path
r=Path(__file__).resolve().parents[1]
h=(r/'manual_apk/assets/index.html').read_text();d=(r/'app/src/main/java/com/marko/auralis/model/AudioDiagnostics.kt').read_text();e=(r/'app/src/main/java/com/marko/auralis/audio/engine/ImpulseAudioEngine.kt').read_text()
checks={
'route actual':"t('diagRoute')" in h and 'lastRouteType' in h,
'safety gain actual':"t('diagSafety')" in h and 'routeSafetyGain' in h,
'active layers':"t('diagLayers')" in h and 'activeLayerNames()' in h,
'mix peak/gain':"t('diagMixPeak')" in h and "t('diagMixGain')" in h,
'breath actual':"t('diagBreath')" in h and '60/c' in h,
'binaural lr actual':"t('diagBinaural')" in h and 'state.binauralCarrier+state.binauralBeat' in h,
'monaural actual':"t('diagMonaural')" in h,
'iso actual':"t('diagIso')" in h,
'itd requested actual':"t('diagItd')" in h and 'f*1e6/sr' in h,
'leadlag actual':"t('diagLeadLag')" in h,
'native diagnostic fields':'safetyOutputGain' in d and 'audioRouteType' in d and 'focusOutputGain' in d,
'native runtime route':'audioRouteType = routed?.type ?: 0' in e,
}
for k,v in checks.items():print(('PASS' if v else 'FAIL'),k)
print('RESULT',sum(checks.values()),'/',len(checks))
raise SystemExit(0 if all(checks.values()) else 1)
