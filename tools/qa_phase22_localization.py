from pathlib import Path
import re, json, subprocess, tempfile
root=Path(__file__).resolve().parents[1]
s=(root/'manual_apk/assets/index.html').read_text()
# Base I18N object is deliberately three parallel language objects.
start=s.index('const I18N={')
base_end=s.index('\n};',start)+3
frag=s[start:base_end]+'\n'
# Apply every later language extension before FEATURE_INFO. This catches all UI additions through phase 22.
pre=s[:s.index('const FEATURE_INFO=')]
assigns=re.findall(r'Object\.assign\(I18N\.(en|de|hr),(\{.*?\})\);',pre,re.S)
for lang,obj in assigns:
    frag += f'Object.assign(I18N.{lang},{obj});\n'
frag += "console.log(JSON.stringify({en:Object.keys(I18N.en).sort(),de:Object.keys(I18N.de).sort(),hr:Object.keys(I18N.hr).sort()}));\n"
with tempfile.NamedTemporaryFile('w',suffix='.js',delete=False) as f:
    f.write(frag); path=f.name
r=subprocess.run(['node',path],capture_output=True,text=True,check=True)
k=json.loads(r.stdout)
sets={x:set(v) for x,v in k.items()}
allk=set.union(*sets.values())
missing={lang:sorted(allk-v) for lang,v in sets.items()}
for lang in ['en','de','hr']:
    print(lang,'keys',len(sets[lang]),'missing',len(missing[lang]))
    for x in missing[lang]: print('  MISSING',x)
# Every data-i18n key must be present in all languages.
used=set(re.findall(r'data-i18n(?:-opt)?="([A-Za-z0-9_]+)"',s))
used_missing={lang:sorted(used-sets[lang]) for lang in sets}
for lang,x in used_missing.items():
    for key in x: print('USED_MISSING',lang,key)
ok=all(not x for x in missing.values()) and all(not x for x in used_missing.values())
print('RESULT','PASS' if ok else 'FAIL','union',len(allk),'used',len(used))
raise SystemExit(0 if ok else 1)
