from pathlib import Path
import zipfile, struct, hashlib, zlib, re, subprocess, sys
ROOT=Path(__file__).resolve().parents[1]
APK=ROOT/'manual_apk/out/Auralis_v2.0.1.apk'
HTML=ROOT/'manual_apk/assets/index.html'
ICON=ROOT/'manual_apk/assets/app_icon.png'
checks=[]
def ck(name,cond,detail=''): checks.append((name,bool(cond),detail))
def uleb(buf,off):
    v=0;s=0
    while True:
        b=buf[off];off+=1;v|=(b&0x7f)<<s
        if not b&0x80:return v,off
        s+=7

def parse_axml(man):
    off=8;typ,hs,size=struct.unpack_from('<HHI',man,off);assert typ==1
    sc,sty,flags,start,styles=struct.unpack_from('<IIIII',man,off+8)
    offsets=[struct.unpack_from('<I',man,off+hs+i*4)[0] for i in range(sc)]
    base=off+start;strings=[]
    for o in offsets:
        p=base+o
        def l8(p):
            b=man[p]
            if b&0x80:return ((b&0x7f)<<8)|man[p+1],p+2
            return b,p+1
        _,p=l8(p);bl,p=l8(p);strings.append(man[p:p+bl].decode('utf-8'))
    off+=size;attrs={}
    while off+8<=len(man):
        t,h,s=struct.unpack_from('<HHI',man,off)
        if t==0x0102:
            ns,name=struct.unpack_from('<II',man,off+16);astart,asz,acount=struct.unpack_from('<HHH',man,off+24);elem=strings[name];p=off+16+astart
            for i in range(acount):
                ans,aname,raw,vs,res0,dtype,data=struct.unpack_from('<IIIHBBI',man,p+i*asz)
                attrs[f'{elem}.{strings[aname]}']=strings[raw] if dtype==3 and raw!=0xffffffff else data
        off+=s
    return attrs,strings

def parse_dex(d):
    ck('DEX magic',d[:8]==b'dex\n035\0')
    ck('DEX SHA-1',d[12:32]==hashlib.sha1(d[32:]).digest())
    ck('DEX Adler32',struct.unpack_from('<I',d,8)[0]==(zlib.adler32(d[12:])&0xffffffff))
    ss,so=struct.unpack_from('<II',d,56);ts,to=struct.unpack_from('<II',d,64);ps,po=struct.unpack_from('<II',d,72);ms,mo=struct.unpack_from('<II',d,88);cs,co=struct.unpack_from('<II',d,96)
    strings=[]
    for i in range(ss):
        o=struct.unpack_from('<I',d,so+i*4)[0];_,p=uleb(d,o);end=d.index(0,p);strings.append(d[p:end].decode())
    types=[strings[struct.unpack_from('<I',d,to+i*4)[0]] for i in range(ts)]
    protos=[]
    for i in range(ps):
        shorty,r,params=struct.unpack_from('<III',d,po+i*12);arr=[]
        if params:
            n=struct.unpack_from('<I',d,params)[0];arr=[types[struct.unpack_from('<H',d,params+4+j*2)[0]] for j in range(n)]
        protos.append((types[r],tuple(arr)))
    methods=[]
    for i in range(ms):
        cls,pr,name=struct.unpack_from('<HHI',d,mo+i*8);methods.append((types[cls],strings[name],protos[pr]))
    class_def=struct.unpack_from('<IIIIIIII',d,co);p=class_def[6]
    sf,p=uleb(d,p);inf,p=uleb(d,p);dm,p=uleb(d,p);vm,p=uleb(d,p)
    for _ in range(sf+inf): _,p=uleb(d,p);_,p=uleb(d,p)
    method_code={};idx=0
    for _ in range(dm):
        diff,p=uleb(d,p);idx+=diff;flags,p=uleb(d,p);code,p=uleb(d,p);method_code[methods[idx]]=code
    idx=0
    for _ in range(vm):
        diff,p=uleb(d,p);idx+=diff;flags,p=uleb(d,p);code,p=uleb(d,p);method_code[methods[idx]]=code
    start=[(m,c) for m,c in method_code.items() if m[1]=='startPcm'][0];m,c=start;regs,ins,outs,tries=struct.unpack_from('<HHHH',d,c)
    ck('DEX MainActivity namespace',m[0]=='Lcom/marko/auralis/MainActivity;',m[0])
    ck('startPcm signature',m[2]==('Z',('Ljava/lang/String;','I','I','I')),str(m[2]))
    ck('startPcm ins_size=5',ins==5,f'ins={ins}')
    ck('Back bridge present','javascript:handleAndroidBack()' in strings)
    ck('Native AudioTrack bridge',all(x in strings for x in ['Landroid/media/AudioTrack;','startPcm','stopPcm','setPcmVolume','exitApp']))

with zipfile.ZipFile(APK) as z:
    ck('APK ZIP integrity',z.testzip() is None)
    names=set(z.namelist());ck('Required APK entries',{'AndroidManifest.xml','resources.arsc','classes.dex','assets/index.html','res/drawable/app_icon.png'}<=names)
    for n in ['AndroidManifest.xml','resources.arsc']:
        i=z.getinfo(n);off=i.header_offset+30+len(i.filename.encode())+len(i.extra);ck(f'{n} 4-byte aligned',off%4==0,f'offset={off}')
    attrs,strings=parse_axml(z.read('AndroidManifest.xml'))
    ck('Package com.marko.auralis',attrs.get('manifest.package')=='com.marko.auralis',str(attrs.get('manifest.package')))
    ck('Version 2.0.1',attrs.get('manifest.versionName')=='2.0.1',str(attrs.get('manifest.versionName')))
    ck('VersionCode 21',attrs.get('manifest.versionCode')==21,str(attrs.get('manifest.versionCode')))
    ck('minSdk 26',attrs.get('uses-sdk.minSdkVersion')==26)
    ck('targetSdk 36',attrs.get('uses-sdk.targetSdkVersion')==36)
    ck('Label Auralis',attrs.get('application.label')=='Auralis',str(attrs.get('application.label')))
    ck('Activity Auralis namespace',attrs.get('activity.name')=='com.marko.auralis.MainActivity',str(attrs.get('activity.name')))
    ck('No INTERNET permission','android.permission.INTERNET' not in strings)
    ck('Embedded HTML exact',z.read('assets/index.html')==HTML.read_bytes())
    ck('Embedded icon exact',z.read('assets/app_icon.png')==ICON.read_bytes() and z.read('res/drawable/app_icon.png')==ICON.read_bytes())
    parse_dex(z.read('classes.dex'))
html=HTML.read_text();js=html.split('<script>',1)[1].rsplit('</script>',1)[0];Path('/tmp/auralis_v201.js').write_text(js)
r=subprocess.run(['node','--check','/tmp/auralis_v201.js'],capture_output=True,text=True);ck('JavaScript syntax',r.returncode==0,r.stderr.strip())
ck('Auralis brand title','<title>Auralis</title>' in html and "appTitle:'Auralis'" in html)
ck('Auralis tagline','Sound, Focus &amp; Psychoacoustics' in html)
ck('Auralis storage namespace',all(x in html for x in ["auralis.v2.settings","auralis.v2.presets","auralis.v2.builtinFavorites","auralis.v2.recentBuiltins"]))
ck('Legacy storage migration preserved','LEGACY_KEYS' in html and 'brainrelax.v1.settings' in html)
ck('Balanced Random absent','Balanced Random' not in html and 'value="balanced"' not in html)
ck('Random Left control','id="leftDelay" type="range" min="1" max="100"' in html)
ck('Random Right control','id="rightDelay" type="range" min="1" max="100"' in html)
ck('Huawei waveform','Math.exp(-0.6*i)' in html and 'baseAmp=.75' in html)
ck('Double-back behavior','now-lastAndroidBackAt<=2000' in html and 'Android.exitApp' in html)
ck('40 built-ins',len(re.findall(r'"id":"[^"]+"',html.split('const BUILTIN_PRESETS=',1)[1].split(';\nconst PRESET_PURPOSES',1)[0]))==40)
# Active Kotlin namespace + identifiers
kt=list((ROOT/'app/src/main').rglob('*.kt'))+list((ROOT/'app/src/test').rglob('*.kt'))
ck('No old active Kotlin package',all('com.marko.brainrelax' not in p.read_text(errors='ignore') for p in kt))
ck('AuralisApp present',(ROOT/'app/src/main/java/com/marko/auralis/ui/AuralisApp.kt').exists())
ck('AuralisTheme identifier','fun AuralisTheme' in (ROOT/'app/src/main/java/com/marko/auralis/ui/theme/Theme.kt').read_text())
ck('Auralis service actions','com.marko.auralis.START' in (ROOT/'app/src/main/java/com/marko/auralis/service/PlaybackService.kt').read_text())
# Signing
v=subprocess.run(['python3',str(ROOT/'manual_apk/tools/apk_v2v3_verify.py'),str(APK)],capture_output=True,text=True);vo=v.stdout+v.stderr
ck('V2 signature','V2 (True' in vo);ck('V3 signature','V3 (True' in vo)
m=re.search(r"V2 \(True, '([0-9a-f]+)'\)",vo);cert=m.group(1) if m else ''
ck('Signing identity continuity',cert=='e5ec9e69cb9ce0f2a034077f09dcaaf0a5ab6df123c72a34c7f8c848e58df22b',cert)
j=subprocess.run(['jarsigner','-verify',str(APK)],capture_output=True,text=True);ck('V1/JAR signature',j.returncode==0 and 'jar verified' in (j.stdout+j.stderr).lower())
passed=sum(ok for _,ok,_ in checks)
for name,ok,detail in checks: print(('PASS' if ok else 'FAIL'),'-',name,(detail if detail and not ok else ''))
print(f'RESULT {passed}/{len(checks)}')
sys.exit(0 if passed==len(checks) else 1)
