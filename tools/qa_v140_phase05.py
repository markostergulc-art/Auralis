from pathlib import Path
import zipfile,struct,hashlib,zlib,re,subprocess,sys
ROOT=Path(__file__).resolve().parents[1]
APK=ROOT/'manual_apk/out/BrainRelaxSoundPerception_v1.4.0.apk'
HTML=ROOT/'manual_apk/assets/index.html'
EXPECTED_CERT='e5ec9e69cb9ce0f2a034077f09dcaaf0a5ab6df123c72a34c7f8c848e58df22b'
checks=[]
def ck(name,cond,detail=''): checks.append((name,bool(cond),detail))
def uleb(b,o):
 v=0;s=0
 while 1:
  x=b[o];o+=1;v|=(x&127)<<s
  if not x&128:return v,o
  s+=7
def parse_axml(man):
 off=8;typ,hs,size=struct.unpack_from('<HHI',man,off);assert typ==1
 sc,sty,flags,start,styles=struct.unpack_from('<IIIII',man,off+8)
 offsets=[struct.unpack_from('<I',man,off+hs+i*4)[0] for i in range(sc)];base=off+start;strings=[]
 for q in offsets:
  p=base+q
  def l8(p):
   x=man[p]
   return ((((x&127)<<8)|man[p+1],p+2) if x&128 else (x,p+1))
  _,p=l8(p);bl,p=l8(p);strings.append(man[p:p+bl].decode())
 off+=size;attrs={}
 while off+8<=len(man):
  t,h,s=struct.unpack_from('<HHI',man,off)
  if t==0x0102:
   ns,name=struct.unpack_from('<II',man,off+16);ast,asz,ac=struct.unpack_from('<HHH',man,off+24);elem=strings[name];p=off+16+ast
   for i in range(ac):
    ans,aname,raw,vs,res0,dtype,data=struct.unpack_from('<IIIHBBI',man,p+i*asz);key=f'{elem}.{strings[aname]}'
    attrs[key]=strings[raw] if dtype==3 and raw!=0xffffffff else data
  off+=s
 return attrs,strings
def parse_dex(d):
 ck('DEX magic',d[:8]==b'dex\n035\0');ck('DEX SHA-1',d[12:32]==hashlib.sha1(d[32:]).digest());ck('DEX Adler32',struct.unpack_from('<I',d,8)[0]==(zlib.adler32(d[12:])&0xffffffff))
 ck('Native AudioTrack bridge strings',all(x in d for x in [b'AudioTrack',b'startPcm',b'stopPcm',b'getNativeSampleRate']))
with zipfile.ZipFile(APK) as z:
 ck('APK ZIP integrity',z.testzip() is None)
 names=set(z.namelist());ck('Required APK entries',{'AndroidManifest.xml','resources.arsc','classes.dex','assets/index.html'}<=names)
 for n in ['AndroidManifest.xml','resources.arsc']:
  i=z.getinfo(n);off=i.header_offset+30+len(i.filename.encode())+len(i.extra);ck(f'{n} 4-byte aligned',off%4==0,str(off))
 attrs,strings=parse_axml(z.read('AndroidManifest.xml'))
 ck('Package',attrs.get('manifest.package')=='com.marko.auralis',str(attrs.get('manifest.package')))
 ck('Version name 1.4.0',attrs.get('manifest.versionName')=='1.4.0',str(attrs.get('manifest.versionName')))
 ck('Version code 8',attrs.get('manifest.versionCode')==8,str(attrs.get('manifest.versionCode')))
 ck('minSdk 26',attrs.get('uses-sdk.minSdkVersion')==26)
 ck('targetSdk 36',attrs.get('uses-sdk.targetSdkVersion')==36)
 ck('No INTERNET permission','android.permission.INTERNET' not in strings)
 ck('Embedded HTML exact source',z.read('assets/index.html')==HTML.read_bytes())
 parse_dex(z.read('classes.dex'))
# JS syntax
html=HTML.read_text();js=html.split('<script>',1)[1].split('</script>',1)[0];Path('/tmp/brain_v140.js').write_text(js)
r=subprocess.run(['node','--check','/tmp/brain_v140.js'],capture_output=True,text=True);ck('JavaScript syntax',r.returncode==0,r.stderr.strip())
# checkpoint feature checks
for name,cond in {
 'Sound Modes entry':'data-open="soundModes"' in html,
 'Info framework':'id="featureInfo"' in html and 'FEATURE_INFO' in html,
 'Breath Pacing':'id="breathEnabled"' in html and 'mixBreath(' in html,
 'White/Pink/Brown Noise':all(v in html for v in ['value="white"','value="pink"','value="brown"']) and 'mixNoise(' in html,
 'Rain/Ocean/Stream/Wind/Forest':all(v in html for v in ['value="rain"','value="ocean"','value="stream"','value="wind"','value="forest"']) and 'mixNatural(' in html,
 'Protective mix gain':'const g=peak>.95?.95/peak:1' in html,
 'Huawei waveform retained':'Math.exp(-0.6*i)' in html and 'baseAmp=.75' in html,
 'Old Pulse ranges retained':all(x in html for x in ['min="1" max="6"','id="dur" type="range" min="40" max="90"','id="delay" type="range" min="1" max="100"']),
 'Preset Update retained':'data-update="${i}"' in html,
 'EN/DE/HR new content':all(x in html for x in ["title:'Breath Pacing'","title:'Atemrhythmus'","title:'Vođenje disanja'","title:'Pink Noise'","title:'Ružičasti šum'","title:'Rain Soundscape'","title:'Zvučni krajolik kiše'"]),
}.items(): ck(name,cond)
# phase scripts
for script in ['qa_baseline_lock_v131.py','qa_phase1_info.py','qa_phase2_mixer.py','qa_phase3_breath.py','qa_phase4_noise.py','qa_phase5_natural.py']:
 q=subprocess.run(['python3',str(ROOT/'tools'/script)],capture_output=True,text=True);ck('Self-check '+script,q.returncode==0,q.stdout[-500:])
# pure DSP scripts
q=subprocess.run([str(ROOT/'tools/run-pure-dsp-smoke.sh')],capture_output=True,text=True);ck('Existing pure DSP smoke',q.returncode==0 and 'PURE_DSP_TESTS_OK' in q.stdout,q.stdout+q.stderr)
# phase 0-5 generators compile smoke
cmd=['kotlinc',str(ROOT/'app/src/main/java/com/marko/auralis/audio/generators/BreathPacingGenerator.kt'),str(ROOT/'app/src/main/java/com/marko/auralis/audio/generators/NoiseGenerator.kt'),str(ROOT/'app/src/main/java/com/marko/auralis/audio/generators/NaturalSoundGenerator.kt'),str(ROOT/'app/src/main/java/com/marko/auralis/audio/mix/AudioMixer.kt'),str(ROOT/'tools/Phase0To5Smoke.kt'),'-include-runtime','-d','/tmp/brain_phase05_release.jar']
q=subprocess.run(cmd,capture_output=True,text=True); ok=q.returncode==0
if ok:
 j=subprocess.run(['java','-jar','/tmp/brain_phase05_release.jar'],capture_output=True,text=True);ok=j.returncode==0 and 'PHASE_0_TO_5_PURE_DSP_SMOKE_OK' in j.stdout
ck('Phase 0-5 pure DSP compile/smoke',ok,(q.stdout+q.stderr)[:1000])
# signatures
v=subprocess.run(['python3',str(ROOT/'manual_apk/tools/apk_v2v3_verify.py'),str(APK)],capture_output=True,text=True);vo=v.stdout+v.stderr
ck('V2 signature','V2 (True' in vo);ck('V3 signature','V3 (True' in vo);m=re.search(r"V2 \(True, '([0-9a-f]+)'\)",vo);ck('Signing continuity',bool(m and m.group(1)==EXPECTED_CERT),m.group(1) if m else '')
j=subprocess.run(['jarsigner','-verify',str(APK)],capture_output=True,text=True);ck('V1/JAR signature',j.returncode==0 and 'jar verified' in (j.stdout+j.stderr).lower())
passed=sum(ok for _,ok,_ in checks);total=len(checks)
for n,ok,d in checks:print(('PASS' if ok else 'FAIL'),'-',n,(' :: '+d if d and not ok else ''))
print(f'RESULT {passed}/{total}')
sys.exit(0 if passed==total else 1)
