from pathlib import Path
r=Path(__file__).resolve().parents[1]
h=(r/'manual_apk/assets/index.html').read_text(); c=(r/'app/src/main/java/com/marko/auralis/ui/components/Controls.kt').read_text()
checks={
'modal dialog semantics':"setAttribute('role','dialog')" in h and "setAttribute('aria-modal','true')" in h,
'live timer':'id="timerActive" class="timerActive" aria-live="polite"' in h,
'live diagnostics':'id="diag" class="diag" aria-live="polite"' in h,
'toast status':'role="status" aria-live="polite"' in h,
'slider aria labels':'const labels={rate:' in h,
'play semantics':"setAttribute('aria-label',playing?'Stop':'Play')" in h,
'lock pressed state':"setAttribute('aria-pressed'" in h,
'info target >=46':'.infoBtn{width:46px;height:46px;' in h,
'compose controls >=52':'widthIn(min = 52.dp)' in c,
'compose semantics':'contentDescription' in c,
}
for k,v in checks.items():print(('PASS' if v else 'FAIL'),k)
print('RESULT',sum(checks.values()),'/',len(checks))
raise SystemExit(0 if all(checks.values()) else 1)
