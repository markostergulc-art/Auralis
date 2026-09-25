from pathlib import Path
import re,json,hashlib,sys
root=Path(__file__).resolve().parents[1]
html=(root/'manual_apk/assets/index.html').read_text(encoding='utf-8')
base=Path('/mnt/data/brain_v191/base')
checks=[]
def ck(name,cond): checks.append((name,bool(cond)))
# version
ck('versionName 1.9.1','versionName = "1.9.1"' in (root/'app/build.gradle.kts').read_text())
ck('versionCode 15','versionCode = 15' in (root/'app/build.gradle.kts').read_text())
# catalog/purpose
m=re.search(r'const BUILTIN_PRESETS=(\[.*?\]);\nconst PRESET_PURPOSES=',html,re.S); ck('built-in catalog parsable',m is not None)
arr=json.loads(m.group(1)) if m else []
pm=re.search(r'const PRESET_PURPOSES=(\{.*?\});\nconst ',html,re.S); ck('purpose catalog present',pm is not None)
pur=json.loads(pm.group(1)) if pm else {}
ck('40 presets',len(arr)==40)
ck('40 purpose records',len(pur)==40)
ck('all IDs covered',set(x['id'] for x in arr)==set(pur))
for lang in ('en','de','hr'):
 ck(f'all {lang} intended+why',all(lang in pur[x['id']] and len(pur[x['id']][lang])==2 and min(map(len,pur[x['id']][lang]))>=45 for x in arr))
ck('Intended use localization keys',all(x in html for x in ["intendedUse:'Intended use'","intendedUse:'Gedacht für'","intendedUse:'Namjena'"]))
ck('Why may help localization keys',all(x in html for x in ["whyThisMayHelp:'Why this may help'","whyThisMayHelp:'Warum dies hilfreich sein könnte'","whyThisMayHelp:'Zašto bi ovo moglo pomoći'"]))
ck('Details includes intended use',"[t('intendedUse'),pp[0]]" in html)
ck('Details includes why may help',"[t('whyThisMayHelp'),pp[1]]" in html)
ck('Cards show intended purpose','${presetPurpose(p)[0]}' in html)
# filters/autohide
ck('old sticky toolbar overridden','position:relative!important' in html and '.presetToolbar' in html)
ck('filter area exists','id="presetFilterArea"' in html)
ck('filter reveal exists','id="presetFilterReveal"' in html)
ck('autohide both directions','delta=Math.abs(y-presetScrollY)' in html and 'delta>=10' in html)
ck('top restores','if(y<=8)setPresetFiltersHidden(false)' in html)
ck('RAF throttling','requestAnimationFrame' in html)
ck('hidden accessibility',"tabs.setAttribute('aria-hidden',hidden?'true':'false')" in html and 'b.tabIndex=hidden?-1:0' in html)
ck('filter does not use fixed/sticky override','.presetToolbar{position:relative!important' in html)
# spacing
for token in ['--sp-s:8px','--sp-m:12px','--sp-l:16px','--sp-xl:24px']:
 ck('spacing '+token,token in html)
ck('button gap >=8','builtinActions{gap:10px!important' in html)
ck('field group spacing','.section>.field+.field{margin-top:var(--sp-m)!important}' in html)
ck('filter chip gap','gap:var(--sp-s)!important' in html and '.presetTabs' in html)
ck('touch height 48','.presetTab{min-height:48px!important' in html)
# unchanged sound/presets
if base.exists():
 same=True
 for p in (base/'app/src/main/java/com/marko/auralis/audio').rglob('*'):
  if p.is_file():
   q=root/p.relative_to(base)
   same &= q.exists() and hashlib.sha256(p.read_bytes()).digest()==hashlib.sha256(q.read_bytes()).digest()
 ck('native audio byte-identical',same)
 bhtml=(base/'manual_apk/assets/index.html').read_text(encoding='utf-8')
 bm=re.search(r'const BUILTIN_PRESETS=(\[.*?\]);\nconst ',bhtml,re.S)
 barr=json.loads(bm.group(1))
 core=lambda a:[{k:x[k] for k in ('id','category','evidence','minutes','headphones','cfg')} for x in a]
 ck('preset numeric/config unchanged',core(arr)==core(barr))
# no obvious medical claims in purpose strings
bad=['cures ','treats heart','treats lung','repairs dna','heals dna','guarantees sleep']
flat=json.dumps(pur,ensure_ascii=False).lower()
ck('no prohibited purpose claim',not any(x in flat for x in bad))
# source parity file
ck('Kotlin purpose catalog', (root/'app/src/main/java/com/marko/auralis/model/BuiltInPresetPurposeCatalog.kt').exists())
passed=sum(v for _,v in checks)
for n,v in checks: print(('PASS' if v else 'FAIL')+' - '+n)
print(f'RESULT {passed}/{len(checks)}')
sys.exit(0 if passed==len(checks) else 1)
