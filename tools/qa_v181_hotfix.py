from pathlib import Path
import zipfile, struct, hashlib, zlib, re, subprocess, sys, json, tempfile
ROOT=Path(__file__).resolve().parents[1]
APK=ROOT/'manual_apk/out/BrainRelaxSoundPerception_v1.8.1.apk'
HTML=ROOT/'manual_apk/assets/index.html'
checks=[]
def ck(name, cond, detail=''): checks.append((name,bool(cond),detail))
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
            b=man[p]
            return ((((b&0x7f)<<8)|man[p+1],p+2) if b&0x80 else (b,p+1))
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
    ck('DEX magic', d[:8]==b'dex\n035\0')
    ck('DEX SHA-1', d[12:32]==hashlib.sha1(d[32:]).digest())
    ck('DEX Adler32', struct.unpack_from('<I',d,8)[0]==(zlib.adler32(d[12:])&0xffffffff))
    ss,so=struct.unpack_from('<II',d,56); out=[]
    for i in range(ss):
        o=struct.unpack_from('<I',d,so+i*4)[0]; _,p=uleb(d,o); end=d.index(0,p); out.append(d[p:end].decode())
    return out

with zipfile.ZipFile(APK) as z:
    ck('APK ZIP integrity', z.testzip() is None)
    attrs,mstrings=parse_axml(z.read('AndroidManifest.xml'))
    ck('Package preserved',attrs.get('manifest.package')=='com.marko.auralis')
    ck('Version 1.8.1',attrs.get('manifest.versionName')=='1.8.1')
    ck('VersionCode 13',attrs.get('manifest.versionCode')==13)
    ck('minSdk 26',attrs.get('uses-sdk.minSdkVersion')==26)
    ck('targetSdk 36',attrs.get('uses-sdk.targetSdkVersion')==36)
    ck('No INTERNET permission','android.permission.INTERNET' not in mstrings)
    ck('Embedded HTML exact',z.read('assets/index.html')==HTML.read_bytes())
    ds=dex_strings(z.read('classes.dex'))
    ck('Native Back routed to JS','javascript:handleAndroidBack()' in ds)
    ck('Old WebView history Back removed','canGoBack' not in ds and 'goBack' not in ds)

html=HTML.read_text()
js=html.split('<script>',1)[1].rsplit('</script>',1)[0]
Path('/tmp/brain181_check.js').write_text(js)
r=subprocess.run(['node','--check','/tmp/brain181_check.js'],capture_output=True,text=True)
ck('JavaScript syntax',r.returncode==0,r.stderr.strip())
ck('Balanced Random removed from active UI','Balanced Random' not in html and 'value="balanced"' not in html)
ck('Stereo only Random L/R','<option value="random_lr"' in html)
ck('Random Left slider','id="leftDelay" type="range" min="1" max="100"' in html and 'data-lock="randomLeftMs"' in html)
ck('Random Right slider','id="rightDelay" type="range" min="1" max="100"' in html and 'data-lock="randomRightMs"' in html)
ck('Independent delay DSP','follower===0?state.randomLeftMs:state.randomRightMs' in html)
ck('Old delay migrates to both','legacy=Number(old.stereoDelayUs)||30' in html and 'randomLeftMs:Number(old.randomLeftMs)||legacy' in html and 'randomRightMs:Number(old.randomRightMs)||legacy' in html)
ck('Preset migration old delay','const legacy=Number(x&&x.stereoDelayUs)||30' in html)
ck('Back closes open windows to home',"open.forEach(m=>m.classList.remove('open'))" in html)
ck('Double Back threshold 2s','now-lastAndroidBackAt<=2000' in html and 'Android.exitApp' in html)
ck('Huawei waveform retained','Math.exp(-0.6*i)' in html and 'baseAmp=.75' in html)
ck('40 built-ins retained',len(re.findall(r'\"id\":\"[^\"]+\"',html.split('const BUILTIN_PRESETS=',1)[1].split(';\nconst BIFAVKEY',1)[0]))==40)
ck('EN labels',all(x in html for x in ["randomLeft:'Random Left'","randomRight:'Random Right'"]))
ck('DE labels',all(x in html for x in ["randomLeft:'Zufällig links'","randomRight:'Zufällig rechts'"]))
ck('HR labels',all(x in html for x in ["randomLeft:'Nasumično lijevo'","randomRight:'Nasumično desno'"]))

# Full-source static consistency
cfg=(ROOT/'app/src/main/java/com/marko/auralis/model/AudioConfig.kt').read_text()
app=(ROOT/'app/src/main/java/com/marko/auralis/ui/BrainRelaxApp.kt').read_text()
repo=(ROOT/'app/src/main/java/com/marko/auralis/data/SettingsRepository.kt').read_text()
eng=(ROOT/'app/src/main/java/com/marko/auralis/audio/engine/ImpulseAudioEngine.kt').read_text()
ck('Full source no Balanced enum','BALANCED_RANDOM' not in cfg and 'enum class StereoMode { RANDOM_LR }' in cfg)
ck('Full source two delay fields','randomLeftDelayMs: Int = 30' in cfg and 'randomRightDelayMs: Int = 30' in cfg)
ck('Full source legacy migration','legacyStereoDelayMs' in repo and 'legacyStereoDelayUs' in repo)
ck('Full source independent follower delay','delayedChannel == PulseChannel.LEFT' in eng and 'config.randomLeftDelayMs' in eng and 'config.randomRightDelayMs' in eng)
ck('Compose Back always intercepted','BackHandler(enabled = true)' in app)
ck('Compose double Back exit','now - lastBackAt <= 2000L' in app and '?.finish()' in app)

# Kotlin model/scheduler compilation
k=subprocess.run(['kotlinc',str(ROOT/'app/src/main/java/com/marko/auralis/model/AudioConfig.kt'),str(ROOT/'app/src/main/java/com/marko/auralis/audio/scheduler/StereoPulseScheduler.kt'),'-d','/tmp/brain181_model.jar'],capture_output=True,text=True)
ck('Kotlin AudioConfig/Scheduler compile',k.returncode==0,k.stderr.strip())

# Signature verification
v=subprocess.run(['python3',str(ROOT/'manual_apk/tools/apk_v2v3_verify.py'),str(APK)],capture_output=True,text=True); vo=v.stdout+v.stderr
ck('V2 signature','V2 (True' in vo)
ck('V3 signature','V3 (True' in vo)
m=re.search(r"V2 \(True, '([0-9a-f]+)'\)",vo); cert=m.group(1) if m else ''
ck('Signing continuity',cert=='e5ec9e69cb9ce0f2a034077f09dcaaf0a5ab6df123c72a34c7f8c848e58df22b',cert)
j=subprocess.run(['jarsigner','-verify',str(APK)],capture_output=True,text=True)
ck('V1/JAR signature',j.returncode==0 and 'jar verified' in (j.stdout+j.stderr).lower())

passed=sum(ok for _,ok,_ in checks)
for n,ok,d in checks: print(('PASS' if ok else 'FAIL'),'-',n,(d if d and not ok else ''))
print(f'RESULT {passed}/{len(checks)}')
sys.exit(0 if passed==len(checks) else 1)
