from pathlib import Path
import zipfile, struct, hashlib, zlib, re, subprocess, sys
ROOT=Path(__file__).resolve().parents[1]
APK=ROOT/'manual_apk/out/BrainRelaxSoundPerception_v1.9.0.apk'
HTML=ROOT/'manual_apk/assets/index.html'
BASEZIP=Path('/mnt/data/brain_ux_impl/BrainRelaxSoundPerception_v1.8.1_source.zip')
checks=[]
def ck(name,cond,detail=''): checks.append((name,bool(cond),detail))
def uleb(buf,off):
    v=0;s=0
    while True:
        b=buf[off];off+=1;v|=(b&0x7f)<<s
        if not b&0x80:return v,off
        s+=7
def parse_axml(man):
    off=8; typ,hs,size=struct.unpack_from('<HHI',man,off); assert typ==1
    sc,sty,flags,start,styles=struct.unpack_from('<IIIII',man,off+8)
    offsets=[struct.unpack_from('<I',man,off+hs+i*4)[0] for i in range(sc)]
    base=off+start; strings=[]
    for o in offsets:
        p=base+o
        def l8(p):
            b=man[p]; return ((((b&0x7f)<<8)|man[p+1],p+2) if b&0x80 else (b,p+1))
        _,p=l8(p); bl,p=l8(p); strings.append(man[p:p+bl].decode())
    off+=size; attrs={}
    while off+8<=len(man):
        t,h,s=struct.unpack_from('<HHI',man,off)
        if t==0x0102:
            ns,name=struct.unpack_from('<II',man,off+16); astart,asz,acount=struct.unpack_from('<HHH',man,off+24); elem=strings[name]; p=off+16+astart
            for i in range(acount):
                ans,aname,raw,vs,res0,dtype,data=struct.unpack_from('<IIIHBBI',man,p+i*asz)
                attrs[f'{elem}.{strings[aname]}']=strings[raw] if dtype==3 and raw!=0xffffffff else data
        off+=s
    return attrs,strings
def dex_strings(d):
    ck('DEX magic',d[:8]==b'dex\n035\0')
    ck('DEX SHA-1',d[12:32]==hashlib.sha1(d[32:]).digest())
    ck('DEX Adler32',struct.unpack_from('<I',d,8)[0]==(zlib.adler32(d[12:])&0xffffffff))
    ss,so=struct.unpack_from('<II',d,56);out=[]
    for i in range(ss):
        o=struct.unpack_from('<I',d,so+i*4)[0];_,p=uleb(d,o);end=d.index(0,p);out.append(d[p:end].decode())
    return out
with zipfile.ZipFile(APK) as z:
    ck('APK ZIP integrity',z.testzip() is None)
    attrs,mstrings=parse_axml(z.read('AndroidManifest.xml'))
    ck('Package preserved',attrs.get('manifest.package')=='com.marko.auralis')
    ck('Version 1.9.0',attrs.get('manifest.versionName')=='1.9.0')
    ck('VersionCode 14',attrs.get('manifest.versionCode')==14)
    ck('minSdk 26',attrs.get('uses-sdk.minSdkVersion')==26)
    ck('targetSdk 36',attrs.get('uses-sdk.targetSdkVersion')==36)
    ck('No INTERNET permission','android.permission.INTERNET' not in mstrings)
    ck('Embedded HTML exact',z.read('assets/index.html')==HTML.read_bytes())
    ds=dex_strings(z.read('classes.dex'))
    ck('Back bridge preserved','javascript:handleAndroidBack()' in ds)
    ck('Old WebView history back absent','canGoBack' not in ds and 'goBack' not in ds)
html=HTML.read_text()
js=html.split('<script>',1)[1].rsplit('</script>',1)[0]
Path('/tmp/brain190_check.js').write_text(js)
r=subprocess.run(['node','--check','/tmp/brain190_check.js'],capture_output=True,text=True)
ck('JavaScript syntax',r.returncode==0,r.stderr.strip())
# UX structure
for needle,name in [
    ('data-goal="relax"','Goal Relax'),('data-goal="focus"','Goal Focus'),('data-goal="breathing"','Goal Breathe'),('data-goal="sleep"','Goal Sleep'),
    ('data-primary="home"','Bottom nav Home'),('data-primary="presets"','Bottom nav Presets'),('data-primary="explore"','Bottom nav Explore'),('data-primary="mySound"','Bottom nav My Sound'),
    ('id="miniPlayer"','Persistent mini-player'),('id="presetSearch"','Preset search'),('id="mySound"','My Sound page')]: ck(name,needle in html)
ck('Preset filters wrap','.presetTabs{display:flex!important;flex-wrap:wrap!important;overflow:visible!important' in html)
ck('Preset filters min 48px','min-height:48px!important' in html)
ck('Selected filter filled','background:var(--turq)!important' in html and ".presetTab.active:before{content:'✓'" in html)
ck('8 filter groups', "const cats=[['all','allPresets'],['focus','focusCategory'],['relax','relaxCategory'],['breathing','breathingCategory'],['sleep','sleepCategory'],['spatial','spatialCategory'],['beats','beatsCategory'],['experimental','experimentalCategory']]" in html)
ck('Preset cards Start + Details',"<div class=\"builtinActions\"><button data-bstart=\"${p.id}\">${t('start')}</button><button data-binfo=\"${p.id}\">${t('details')}</button>" in html and 'id=\"detailsCopy\"' in html)
ck('Diagnostics progressive disclosure','advancedDisclosure' in html and 'Audio Diagnostics' in html)
ck('Favorites UX key localized',all(x in html for x in ["favorites:'Favorites'","favorites:'Favoriten'","favorites:'Favoriti'"]))
ck('Version shown EN/DE/HR',html.count('v1.9.0')>=3)
# Existing v1.8.1 hotfix behavior preserved
ck('Balanced Random remains removed','Balanced Random' not in html and 'value="balanced"' not in html)
ck('Random Left regulator retained','id="leftDelay" type="range" min="1" max="100"' in html)
ck('Random Right regulator retained','id="rightDelay" type="range" min="1" max="100"' in html)
ck('Independent delay DSP','follower===0?state.randomLeftMs:state.randomRightMs' in html)
ck('Double back exit retained','now-lastAndroidBackAt<=2000' in html and 'Android.exitApp' in html)
ck('Huawei waveform retained','Math.exp(-0.6*i)' in html and 'baseAmp=.75' in html)
# Built-ins unchanged exactly from baseline fallback HTML
with zipfile.ZipFile(BASEZIP) as z:
    old=z.read('manual_apk/assets/index.html').decode()
    def catalog(s): return s.split('const BUILTIN_PRESETS=',1)[1].split(';\nconst BIFAVKEY',1)[0]
    ck('40 built-ins retained',len(re.findall(r'"id":"[^\"]+"',catalog(html)))==40)
    ck('Built-in preset definitions bit-identical',catalog(html)==catalog(old))
    # native audio source must remain byte-identical
    audio=[n for n in z.namelist() if n.startswith('app/src/main/java/com/marko/auralis/audio/') and not n.endswith('/')]
    same=True; changed=[]
    for n in audio:
        p=ROOT/n
        if not p.exists() or hashlib.sha256(p.read_bytes()).digest()!=hashlib.sha256(z.read(n)).digest(): same=False;changed.append(n)
    ck('Native audio package unchanged',same,','.join(changed))
    n='app/src/main/java/com/marko/auralis/model/BuiltInPresetCatalog.kt';p=ROOT/n
    ck('Native built-in preset catalog unchanged',p.exists() and hashlib.sha256(p.read_bytes()).digest()==hashlib.sha256(z.read(n)).digest())
# Compose mirror
home=(ROOT/'app/src/main/java/com/marko/auralis/ui/screens/HomeScreen.kt').read_text()
pres=(ROOT/'app/src/main/java/com/marko/auralis/ui/screens/PresetsScreen.kt').read_text()
app=(ROOT/'app/src/main/java/com/marko/auralis/ui/BrainRelaxApp.kt').read_text()
ck('Compose goal-first home',all(x in home for x in ['GoalButton("Relax"','GoalButton("Focus"','GoalButton("Breathe"','GoalButton("Sleep Prep"']))
ck('Compose preset FilterChip','FilterChip(' in pres)
ck('Compose preset chips 48dp','Modifier.weight(1f).height(48.dp)' in pres)
ck('Compose preset Start Details',all(x in pres for x in ['Text("Start")','Text("Details")']))
ck('Compose category routing','presetCategory' in app and 'onGoal = {' in app)
ck('Compose double-back preserved','now - lastBackAt <= 2000L' in app)
# version full source
build=(ROOT/'app/build.gradle.kts').read_text()
ck('Full source version 1.9.0','versionName = "1.9.0"' in build and 'versionCode = 14' in build)
# signature
v=subprocess.run(['python3',str(ROOT/'manual_apk/tools/apk_v2v3_verify.py'),str(APK)],capture_output=True,text=True);vo=v.stdout+v.stderr
ck('V2 signature','V2 (True' in vo);ck('V3 signature','V3 (True' in vo)
m=re.search(r"V2 \(True, '([0-9a-f]+)'\)",vo);cert=m.group(1) if m else ''
ck('Signing continuity',cert=='e5ec9e69cb9ce0f2a034077f09dcaaf0a5ab6df123c72a34c7f8c848e58df22b',cert)
j=subprocess.run(['jarsigner','-verify',str(APK)],capture_output=True,text=True)
ck('V1/JAR signature',j.returncode==0 and 'jar verified' in (j.stdout+j.stderr).lower())
passed=sum(ok for _,ok,_ in checks)
for n,ok,d in checks: print(('PASS' if ok else 'FAIL'),'-',n,(d if d and not ok else ''))
print(f'RESULT {passed}/{len(checks)}')
sys.exit(0 if passed==len(checks) else 1)
