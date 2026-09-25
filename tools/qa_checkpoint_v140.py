from pathlib import Path
import zipfile, struct, hashlib, zlib, re, subprocess, sys
ROOT=Path(__file__).resolve().parents[1]
APK=ROOT/'manual_apk/out/BrainRelaxSoundPerception_v1.4.0.apk'
HTML=ROOT/'manual_apk/assets/index.html'
checks=[]
def ck(name, cond, detail=''):
    checks.append((name,bool(cond),detail))

def uleb(buf, off):
    val=0;shift=0
    while True:
        b=buf[off];off+=1;val|=(b&0x7f)<<shift
        if not b&0x80:return val,off
        shift+=7

def parse_axml(man):
    off=8; typ,hs,size=struct.unpack_from('<HHI',man,off); assert typ==1
    sc,sty,flags,start,styles=struct.unpack_from('<IIIII',man,off+8)
    offsets=[struct.unpack_from('<I',man,off+hs+i*4)[0] for i in range(sc)]
    base=off+start; strings=[]
    for o in offsets:
        p=base+o
        def l8(p):
            b=man[p]
            if b&0x80:return ((b&0x7f)<<8)|man[p+1],p+2
            return b,p+1
        _,p=l8(p); bl,p=l8(p); strings.append(man[p:p+bl].decode('utf-8'))
    off += size; attrs={}
    while off+8<=len(man):
        t,h,s=struct.unpack_from('<HHI',man,off)
        if t==0x0102:
            ns,name=struct.unpack_from('<II',man,off+16); attr_start,attr_size,attr_count=struct.unpack_from('<HHH',man,off+24)
            elem=strings[name]; p=off+16+attr_start
            for i in range(attr_count):
                ans,aname,raw,vs,res0,dtype,data=struct.unpack_from('<IIIHBBI',man,p+i*attr_size)
                key=f'{elem}.{strings[aname]}'; attrs[key]=strings[raw] if dtype==3 and raw!=0xffffffff else data
        off+=s
    return attrs,strings

def parse_dex(d):
    ck('DEX magic',d[:8]==b'dex\n035\0')
    ck('DEX SHA-1', d[12:32]==hashlib.sha1(d[32:]).digest())
    ck('DEX Adler32', struct.unpack_from('<I',d,8)[0]==(zlib.adler32(d[12:])&0xffffffff))
    ss,so=struct.unpack_from('<II',d,56); ts,to=struct.unpack_from('<II',d,64); ps,po=struct.unpack_from('<II',d,72); ms,mo=struct.unpack_from('<II',d,88); cs,co=struct.unpack_from('<II',d,96)
    strings=[]
    for i in range(ss):
        o=struct.unpack_from('<I',d,so+i*4)[0]; _,p=uleb(d,o); end=d.index(0,p); strings.append(d[p:end].decode())
    types=[strings[struct.unpack_from('<I',d,to+i*4)[0]] for i in range(ts)]
    protos=[]
    for i in range(ps):
        shorty,r,params=struct.unpack_from('<III',d,po+i*12); arr=[]
        if params:
            n=struct.unpack_from('<I',d,params)[0]; arr=[types[struct.unpack_from('<H',d,params+4+j*2)[0]] for j in range(n)]
        protos.append((types[r],tuple(arr)))
    methods=[]
    for i in range(ms):
        cls,pr,name=struct.unpack_from('<HHI',d,mo+i*8); methods.append((types[cls],strings[name],protos[pr]))
    class_def=struct.unpack_from('<IIIIIIII',d,co); p=class_def[6]
    sf,p=uleb(d,p); inf,p=uleb(d,p); dm,p=uleb(d,p); vm,p=uleb(d,p)
    for _ in range(sf+inf): _,p=uleb(d,p); _,p=uleb(d,p)
    method_code={}; idx=0
    for _ in range(dm):
        diff,p=uleb(d,p); idx+=diff; _,p=uleb(d,p); code,p=uleb(d,p); method_code[methods[idx]]=code
    idx=0
    for _ in range(vm):
        diff,p=uleb(d,p); idx+=diff; _,p=uleb(d,p); code,p=uleb(d,p); method_code[methods[idx]]=code
    m,c=[(m,c) for m,c in method_code.items() if m[1]=='startPcm'][0]
    regs,ins,outs,tries=struct.unpack_from('<HHHH',d,c)
    ck('startPcm signature',m[2]==('Z',('Ljava/lang/String;','I','I','I')))
    ck('startPcm ins_size=5',ins==5,f'ins={ins}')
    ck('Native diagnostics bridge',all(x in strings for x in ['startPcm','stopPcm','setPcmVolume','getNativeSampleRate','getCpuTimeMs','getMemoryPssKb']))

with zipfile.ZipFile(APK) as z:
    ck('APK ZIP integrity',z.testzip() is None)
    names=set(z.namelist()); ck('Required APK entries',{'AndroidManifest.xml','resources.arsc','classes.dex','assets/index.html'}<=names)
    for n in ['AndroidManifest.xml','resources.arsc']:
        i=z.getinfo(n); data_off=i.header_offset+30+len(i.filename.encode())+len(i.extra); ck(f'{n} 4-byte aligned',data_off%4==0,f'offset={data_off}')
    attrs,strings=parse_axml(z.read('AndroidManifest.xml'))
    ck('Package',attrs.get('manifest.package')=='com.marko.auralis')
    ck('Version name',attrs.get('manifest.versionName')=='1.4.0',str(attrs.get('manifest.versionName')))
    ck('Version code',attrs.get('manifest.versionCode')==8,str(attrs.get('manifest.versionCode')))
    ck('minSdk 26',attrs.get('uses-sdk.minSdkVersion')==26)
    ck('targetSdk 36',attrs.get('uses-sdk.targetSdkVersion')==36)
    ck('No INTERNET permission','android.permission.INTERNET' not in strings)
    ck('Embedded HTML exact source',z.read('assets/index.html')==HTML.read_bytes())
    parse_dex(z.read('classes.dex'))

html=HTML.read_text()
js=html.split('<script>',1)[1].rsplit('</script>',1)[0]
Path('/tmp/brain_v140_check.js').write_text(js)
p=subprocess.run(['node','--check','/tmp/brain_v140_check.js'],capture_output=True,text=True)
ck('JavaScript syntax',p.returncode==0,p.stderr.strip())
ck('Huawei waveform retained','Math.exp(-0.6*i)' in html and 'baseAmp=.75' in html)
ck('Pulse 1-6 retained','id="rate" type="range" min="1" max="6"' in html)
ck('Pulse duration 40-90 us retained','id="dur" type="range" min="40" max="90"' in html)
ck('Lead/Lag 1-100 ms retained','id="delay" type="range" min="1" max="100"' in html)
ck('Breath 4/6 defaults','breathIn:4,breathOut:6' in html)
ck('White/Pink/Brown procedural',all(x in html for x in ["value=\"white\"","value=\"pink\"","value=\"brown\"",'mixNoise(left,right,sr,totalFrames)']))
ck('Five natural soundscapes',all(x in html for x in ["value=\"rain\"","value=\"ocean\"","value=\"stream\"","value=\"wind\"","value=\"forest\"",'mixNatural(left,right,sr,totalFrames)']))
ck('Float mixer path','new Float32Array(totalFrames)' in html and 'encodeStereo16(left,right)' in html)
ck('Info framework',all(x in html for x in ['whatIsThis','whatHappens','howToUse','whatNotice','headphonesSpeaker','startingPoint','evidence','limitation','safety']))
ck('Evidence badges',all(x in html for x in ['evidenceSupported','emergingEvidence','experimental','traditionalExploratory','researchOnly']))
ck('Three languages', all(x in html for x in ['Object.assign(I18N.en','Object.assign(I18N.de','Object.assign(I18N.hr']) and all(x in html for x in ['data-lang-first="en"','data-lang-first="de"','data-lang-first="hr"']))
ck('No medical overclaim',all(x not in html.lower() for x in ['cures anxiety','heals dna','treats ptsd','clinically proven to cure']))
ck('CPU + memory diagnostics',"t('diagCpu')" in html and "t('diagMemory')" in html)
ck('Preset update retained','data-update="${i}"' in html)

v=subprocess.run(['python3',str(ROOT/'manual_apk/tools/apk_v2v3_verify.py'),str(APK)],capture_output=True,text=True); vo=v.stdout+v.stderr
ck('V2 signature','V2 (True' in vo)
ck('V3 signature','V3 (True' in vo)
m=re.search(r"V2 \(True, '([0-9a-f]+)'\)",vo); cert=m.group(1) if m else ''
ck('Signing continuity certificate',cert=='e5ec9e69cb9ce0f2a034077f09dcaaf0a5ab6df123c72a34c7f8c848e58df22b',cert)
j=subprocess.run(['jarsigner','-verify',str(APK)],capture_output=True,text=True); ck('V1/JAR signature',j.returncode==0 and 'jar verified' in (j.stdout+j.stderr).lower())

passed=sum(1 for _,ok,_ in checks if ok); total=len(checks)
for name,ok,detail in checks: print(('PASS' if ok else 'FAIL'),'-',name,detail if detail and not ok else '')
print(f'RESULT {passed}/{total}')
sys.exit(0 if passed==total else 1)
