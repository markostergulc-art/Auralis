from pathlib import Path
import zipfile, struct, hashlib, zlib, re, subprocess, json, sys
ROOT=Path(__file__).resolve().parents[1]
APK=ROOT/'manual_apk/out/BrainRelaxSoundPerception_v1.2.0.apk'
HTML=ROOT/'manual_apk/assets/index.html'
OLD=Path('/mnt/data/BrainRelaxSoundPerception_v1.1.0.apk')
checks=[]
def ck(name, cond, detail=''):
    checks.append((name,bool(cond),detail))
    if not cond: print('FAIL',name,detail)

def uleb(buf, off):
    val=0;shift=0
    while True:
        b=buf[off];off+=1;val|=(b&0x7f)<<shift
        if not b&0x80:return val,off
        shift+=7

def parse_axml(man):
    # string pool after 8-byte XML header
    off=8
    typ,hs,size=struct.unpack_from('<HHI',man,off); assert typ==1
    sc,sty,flags,start,styles=struct.unpack_from('<IIIII',man,off+8)
    offsets=[struct.unpack_from('<I',man,off+hs+i*4)[0] for i in range(sc)]
    base=off+start
    strings=[]
    for o in offsets:
        p=base+o
        # UTF-8 length twice
        def l8(p):
            b=man[p]
            if b&0x80:return ((b&0x7f)<<8)|man[p+1],p+2
            return b,p+1
        _,p=l8(p); bl,p=l8(p); strings.append(man[p:p+bl].decode('utf-8'))
    off += size
    attrs={}
    while off+8<=len(man):
        t,h,s=struct.unpack_from('<HHI',man,off)
        if t==0x0102: # start element
            ns,name=struct.unpack_from('<II',man,off+16)
            attr_start,attr_size,attr_count=struct.unpack_from('<HHH',man,off+24)
            elem=strings[name]
            p=off+16+attr_start
            for i in range(attr_count):
                ans,aname,raw,vs,res0,dtype,data=struct.unpack_from('<IIIHBBI',man,p+i*attr_size)
                key=f'{elem}.{strings[aname]}'
                if dtype==3 and raw!=0xffffffff: val=strings[raw]
                else: val=data
                attrs[key]=val
        off+=s
    return attrs,strings

def parse_dex(d):
    assert d[:8]==b'dex\n035\0'
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
            n=struct.unpack_from('<I',d,params)[0]
            arr=[types[struct.unpack_from('<H',d,params+4+j*2)[0]] for j in range(n)]
        protos.append((types[r],tuple(arr)))
    methods=[]
    for i in range(ms):
        cls,pr,name=struct.unpack_from('<HHI',d,mo+i*8); methods.append((types[cls],strings[name],protos[pr]))
    class_def=struct.unpack_from('<IIIIIIII',d,co)
    class_data_off=class_def[6]
    p=class_data_off
    sf,p=uleb(d,p); inf,p=uleb(d,p); dm,p=uleb(d,p); vm,p=uleb(d,p)
    # skip fields
    for _ in range(sf+inf):
        _,p=uleb(d,p); _,p=uleb(d,p)
    method_code={}; idx=0
    for _ in range(dm):
        diff,p=uleb(d,p); idx+=diff; flags,p=uleb(d,p); code,p=uleb(d,p); method_code[methods[idx]]=code
    idx=0
    for _ in range(vm):
        diff,p=uleb(d,p); idx+=diff; flags,p=uleb(d,p); code,p=uleb(d,p); method_code[methods[idx]]=code
    start=[(m,c) for m,c in method_code.items() if m[1]=='startPcm'][0]
    m,c=start; regs,ins,outs,tries=struct.unpack_from('<HHHH',d,c)
    ck('startPcm signature', m[2]==('Z',('Ljava/lang/String;','I','I','I')),str(m[2]))
    ck('startPcm ins_size=5', ins==5,f'ins={ins}')
    ck('exitApp present', any(m[1]=='exitApp' for m in method_code))
    ck('Native AudioTrack bridge present', all(x in strings for x in ['Landroid/media/AudioTrack;','startPcm','stopPcm','setPcmVolume','getCpuTimeMs','getMemoryPssKb','exitApp']))

with zipfile.ZipFile(APK) as z:
    ck('APK ZIP integrity', z.testzip() is None)
    names=set(z.namelist()); ck('Required APK entries', {'AndroidManifest.xml','resources.arsc','classes.dex','assets/index.html'}<=names)
    for n in ['AndroidManifest.xml','resources.arsc']:
        i=z.getinfo(n); data_off=i.header_offset+30+len(i.filename.encode())+len(i.extra); ck(f'{n} 4-byte aligned',data_off%4==0,f'offset={data_off}')
    man=z.read('AndroidManifest.xml'); attrs,strings=parse_axml(man)
    ck('Package', attrs.get('manifest.package')=='com.marko.auralis',str(attrs.get('manifest.package')))
    ck('Version name', attrs.get('manifest.versionName')=='1.2.0',str(attrs.get('manifest.versionName')))
    ck('Version code', attrs.get('manifest.versionCode')==4,str(attrs.get('manifest.versionCode')))
    ck('minSdk 26', attrs.get('uses-sdk.minSdkVersion')==26,str(attrs.get('uses-sdk.minSdkVersion')))
    ck('targetSdk 36', attrs.get('uses-sdk.targetSdkVersion')==36,str(attrs.get('uses-sdk.targetSdkVersion')))
    ck('No INTERNET permission', 'android.permission.INTERNET' not in strings)
    ck('Embedded HTML exact source', z.read('assets/index.html')==HTML.read_bytes())
    parse_dex(z.read('classes.dex'))

html=HTML.read_text()
js=html.split('<script>',1)[1].split('</script>',1)[0]
Path('/tmp/brain_v120_qa.js').write_text(js)
node=subprocess.run(['node','--check','/tmp/brain_v120_qa.js'],capture_output=True,text=True)
ck('JavaScript syntax',node.returncode==0,node.stderr.strip())
ck('Centered title', '.title{text-align:center' in html)
ck('Larger base font', 'font-size:17px' in html)
ck('Language first-run window', all(x in html for x in ['data-lang-first="en"','data-lang-first="de"','data-lang-first="hr"','langFirst']))
ck('Safety acceptance flow', all(x in html for x in ['safetyFirst','declineSafety','acceptSafety','Yes, I Accept']))
ck('Main/Volume/Pulse windows', all(f'id="{x}"' in html for x in ['volumeWin','pulseWin']))
ck('Current preset displayed', 'id="presetNow"' in html)
ck('All seven regulator locks', html.count('data-lock=')==7,f'count={html.count("data-lock=")}')
ck('Strict Alternate removed', 'Strict alternate' not in html and 'value="alternate"' not in html)
ck('Stereo modes retained', 'value="balanced"' in html and 'value="random_lr"' in html)
ck('Random L/R delay 1-50', 'id="delay" type="range" min="1" max="50"' in html)
ck('Pulse window controls', all(x in html for x in ['id="rate"','id="random"','id="dur"']))
ck('Volume window controls', all(x in html for x in ['id="vol"','id="balance"','id="dyn"']))
ck('Turquoise Light theme', '--turq:#159b9e' in html and '--turq2:#0f8587' in html)
ck('No orange theme token', 'orange' not in html.lower())
ck('CPU and memory diagnostics', "t('diagCpu')" in html and "t('diagMemory')" in html)
ck('Timer/Presets/Settings equal icon class', html.count('class="actionIcon"')==3)
# i18n parity
m=re.search(r'const I18N=(\{.*?\});\nlet state',html,re.S); Path('/tmp/i18n_qa.js').write_text('const I18N='+m.group(1)+'; console.log(JSON.stringify(I18N));')
i18n=json.loads(subprocess.check_output(['node','/tmp/i18n_qa.js'],text=True)); keys=set(re.findall(r'data-i18n="([^"]+)"',html))|set(re.findall(r'data-i18n-opt="([^"]+)"',html))|set(re.findall(r"t\('([^']+)'\)",html))
ck('EN/DE/HR dictionaries', set(i18n)=={'en','de','hr'})
ck('87-key translation parity', all(keys<=set(v) for v in i18n.values()) and len(keys)==87 and len({frozenset(v) for v in i18n.values()})==1,f'refs={len(keys)}')
# cert continuity
if OLD.exists():
    verifier=ROOT/'manual_apk/tools/apk_v2v3_verify.py'
    def cert(path):
        o=subprocess.check_output(['python3',str(verifier),str(path)],text=True)
        m=re.search(r"V2 \(True, '([0-9a-f]+)'\)",o); return m.group(1) if m else None
    oldc,newc=cert(OLD),cert(APK); ck('Signing continuity v1.1.0 -> v1.2.0',oldc==newc and oldc is not None,newc or '')
# v1 jar signature
j=subprocess.run(['jarsigner','-verify',str(APK)],capture_output=True,text=True)
ck('V1/JAR signature',j.returncode==0 and 'jar verified' in (j.stdout+j.stderr).lower())
# v2/v3 verifier
v=subprocess.run(['python3',str(ROOT/'manual_apk/tools/apk_v2v3_verify.py'),str(APK)],capture_output=True,text=True)
vo=v.stdout+v.stderr
ck('V2 signature','V2 (True' in vo)
ck('V3 signature','V3 (True' in vo)

passed=sum(1 for _,ok,_ in checks if ok); total=len(checks)
for name,ok,detail in checks: print(('PASS' if ok else 'FAIL'),'-',name,detail if detail and not ok else '')
print(f'RESULT {passed}/{total}')
sys.exit(0 if passed==total else 1)
