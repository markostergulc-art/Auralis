from pathlib import Path
import json,re,sys,subprocess
ROOT=Path(__file__).resolve().parents[1]
html=(ROOT/'manual_apk/assets/index.html').read_text()
presets=json.loads((ROOT/'docs/builtin_presets_v180.json').read_text())
checks=[]
def ck(n,c,d=''): checks.append((n,bool(c),d))
ck('Preset count >= 36',len(presets)>=36,str(len(presets)))
ck('Preset count exactly 40',len(presets)==40,str(len(presets)))
ids=[p['id'] for p in presets]; ck('Unique preset IDs',len(ids)==len(set(ids)))
expected={'focus','relax','breathing','sleep','spatial','beats','experimental'}
ck('All required categories',expected<=set(p['category'] for p in presets))
valid_ev={'supported','emerging','experimental','traditional','research'}
valid_hp={'optional','recommended','required'}
for p in presets:
    pid=p['id']; c=p['cfg']
    ck(pid+' evidence',p['evidence'] in valid_ev,p['evidence'])
    ck(pid+' headphones',p['headphones'] in valid_hp,p['headphones'])
    ck(pid+' duration',5<=p['minutes']<=60,str(p['minutes']))
    for lang in ('en','de','hr'):
        x=p['text'].get(lang,{})
        ck(pid+' '+lang+' name',bool(x.get('name','').strip()))
        ck(pid+' '+lang+' subtitle',len(x.get('subtitle','').strip())>=20,x.get('subtitle',''))
    # Parameter ranges mirror sanitize().
    ranges={
      'rate':(1,6),'volume':(0,100),'balance':(-100,100),'dynamics':(0,50),'randomTiming':(0,50),'durationUs':(40,90),'stereoDelayUs':(1,100),
      'breathIn':(3,8),'breathOut':(3,10),'breathPause':(0,3),'breathLevel':(0,25),'noiseLevel':(0,25),'naturalLevel':(0,25),
      'ambientBrightness':(0,100),'ambientMovement':(0,100),'ambientDensity':(0,100),'ambientLevel':(0,25),
      'binauralCarrier':(100,600),'binauralBeat':(1,12),'binauralLevel':(0,20),'monauralCarrier':(100,600),'monauralBeat':(1,12),'monauralDepth':(0,100),'monauralLevel':(0,20),
      'isoCarrier':(100,600),'isoRate':(1,12),'isoDepth':(0,100),'isoSoft':(0,100),'isoLevel':(0,20),'itdUs':(20,800),'itdLevel':(0,20),
      'motionCycle':(4,30),'motionDepth':(0,100),'motionLevel':(0,20),'bilateralRate':(.5,4),'bilateralSoft':(0,100),'bilateralLevel':(0,20),'altLevel':(0,20)
    }
    for k,(lo,hi) in ranges.items():
        if k in c: ck(pid+' '+k,lo<=float(c[k])<=hi,str(c[k]))
    if 'noiseType' in c: ck(pid+' noise enum',c['noiseType'] in {'off','white','pink','brown'},c['noiseType'])
    if 'naturalType' in c: ck(pid+' nature enum',c['naturalType'] in {'off','rain','ocean','stream','wind','forest'},c['naturalType'])
    if 'altMode' in c: ck(pid+' alt enum',c['altMode'] in {'off','432pure','432ambient','528pure','528ambient','solfeggio','schumann','bowl','soundbath','humming'},c['altMode'])
    if 'solfeggioHz' in c: ck(pid+' solfeggio enum',c['solfeggioHz'] in {174,285,396,417,528,639,741,852,963},str(c['solfeggioHz']))
    ck(pid+' does not set timer','timerMinutes' not in c)
# Required concrete presets.
for pid in ['focused_calm','deep_relax','cardiac_calm_breathing','hrv_breathing_46','respiratory_ease','sleep_preparation','spatial_itd_lab','binaural_6','monaural_6','ambient_432','resonance_528','sound_bath','schumann_783','solfeggio_528']:
    ck('Required preset '+pid,pid in ids)
# UI/behavior contracts.
for token in ['builtInPresetList','presetTabs','renderBuiltIns','openBuiltInInfo','applyBuiltIn','copyBuiltIn','favSet','Save as My Preset','currentPreset.startsWith(\'@\')']:
    ck('Built-in UI '+token,token in html)
ck('Built-in application resets unspecified audio layers','state={...defaults,...p.cfg,timerMinutes:timer,theme,language,currentPreset:\'@\'+p.id}' in html)
ck('Built-in preserves user timer','const timer=state.timerMinutes' in html)
ck('Built-in read-only separation','builtInPresets' in html and 'myPresets' in html)
ck('Personal Update retained',"data-update" in html and 'presetSnapshot(name)' in html)
# Specific scientific caveats in all locales are embedded.
for phrase in ['does not measure heart rhythm','misst keinen Herzrhythmus','ne mjeri srčani ritam','not treatment for asthma','keine Behandlung von Asthma','ne liječenje astme','does not prove that the brain enters a 6 Hz theta state','not physically equivalent to electromagnetic Schumann resonance','does not claim DNA repair']:
    ck('Caveat '+phrase,phrase.lower() in html.lower())
# No unsafe names/subtitles.
joined='\n'.join((p['text'][l]['name']+' '+p['text'][l]['subtitle']).lower() for p in presets for l in ('en','de','hr'))
for phrase in ['heart healing','lung healing','dna repair','anxiety treatment','ptsd therapy','theta induction','vagus nerve activation','cellular repair']:
    ck('No medical preset label '+phrase,phrase not in joined)
# Script parses.
js=html.split('<script>',1)[1].rsplit('</script>',1)[0]; tmp=ROOT/'tools/.v180.js';tmp.write_text(js)
r=subprocess.run(['node','--check',str(tmp)],capture_output=True,text=True);tmp.unlink(missing_ok=True)
ck('JavaScript syntax',r.returncode==0,r.stderr[-1000:])
# Native catalog has same 40 IDs and compiles with model layer.
k=(ROOT/'app/src/main/java/com/marko/auralis/model/BuiltInPresetCatalog.kt').read_text()
ck('Native catalog all IDs',all(('id="'+x+'"') in k for x in ids))
jar=ROOT/'tools/.v180catalog.jar';r=subprocess.run(['kotlinc',str(ROOT/'app/src/main/java/com/marko/auralis/model/AudioConfig.kt'),str(ROOT/'app/src/main/java/com/marko/auralis/model/SoundLayerConfig.kt'),str(ROOT/'app/src/main/java/com/marko/auralis/model/BuiltInPresetCatalog.kt'),'-d',str(jar)],capture_output=True,text=True);jar.unlink(missing_ok=True)
ck('Native catalog compile',r.returncode==0,r.stderr[-1000:])
# Version contract.
ck('HTML v1.8.0','v1.8.0' in html)
ck('Gradle versionCode 12','versionCode = 12' in (ROOT/'app/build.gradle.kts').read_text())
ck('Manual manifest versionCode 12','data_val=12' in (ROOT/'manual_apk/build_manual_apk.py').read_text())
passed=sum(ok for _,ok,_ in checks)
for n,ok,d in checks: print(('PASS' if ok else 'FAIL')+' - '+n+((' :: '+d) if d and not ok else ''))
print(f'RESULT {passed}/{len(checks)}')
sys.exit(0 if passed==len(checks) else 1)
