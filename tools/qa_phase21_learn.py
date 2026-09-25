from pathlib import Path
import re, sys
r=Path(__file__).resolve().parents[1]
h=(r/'manual_apk/assets/index.html').read_text()
k=(r/'app/src/main/java/com/marko/auralis/content/LearnCatalog.kt').read_text()
checks={
 'learn_entry':'data-open="learn"' in h and 'id="learn"' in h,
 'offline_catalog':'const LEARN=' in h and 'https://' not in h[h.index('const LEARN='):h.index('function renderLearn')],
 'ten_articles':all(x in h for x in ['Hz','localizes','Binaural','Monaural','Pink','ITD','432','528','Schumann','Safe']),
 'three_locales':all((f'{lang}:' in h[h.index('const LEARN='):]) for lang in ['en','de','hr']),
 'binaural_concrete':'200 Hz' in h and '206 Hz' in h,
 'itd_concrete':'20.83' in h or '20,83' in h,
 'schumann_limitation':'not physically equivalent' in h or 'nicht physikalisch' in h or 'nije fizički' in h,
 'safe_no_fake_spl':'does not know the calibrated SPL' in h or 'nicht den kalibrierten SPL' in h or 'ne zna kalibrirani SPL' in h,
 'native_catalog':k.count('LearnArticle(')>=10,
}
failed=[]
for n,v in checks.items(): print(('PASS' if v else 'FAIL'),n); failed += ([] if v else [n])
print(f'RESULT {len(checks)-len(failed)}/{len(checks)}')
sys.exit(bool(failed))
