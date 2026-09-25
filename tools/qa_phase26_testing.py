from pathlib import Path
import subprocess,re,sys
ROOT=Path(__file__).resolve().parents[1]
html=(ROOT/'manual_apk/assets/index.html').read_text()
checks=[]
def ck(n,c,d=''): checks.append((n,bool(c),d))
# Existing deterministic regression suites must remain present.
for f in ['qa_baseline_lock_v131.py','qa_phase1_info.py','qa_phase2_mixer.py','qa_phase3_breath.py','qa_phase4_noise.py','qa_phase5_natural.py','qa_phases_6_15.py','qa_phases_16_20.py','qa_phase21_learn.py','qa_phase22_localization.py','qa_phase23_accessibility.py','qa_phase24_diagnostics.py','qa_phase25_performance.py']:
    ck('Test present '+f,(ROOT/'tools'/f).exists())
# Preset v1->v2 compatibility exists in both paths.
ck('Fallback preset schema v2','const PRESET_SCHEMA=2' in html)
ck('Fallback old preset migration defaults','migratePreset' in html and 'schemaVersion' in html)
repo=(ROOT/'app/src/main/java/com/marko/auralis/data/SettingsRepository.kt').read_text()
ck('Native preset legacy schema default','optInt("schemaVersion", 1)' in repo)
ck('Native preset layer default/migration','decodeLayers' in repo and 'SoundLayerConfig()' in repo)
# Run JS parser.
js=html.split('<script>',1)[1].rsplit('</script>',1)[0]
tmp=ROOT/'tools/.phase26_index.js';tmp.write_text(js)
p=subprocess.run(['node','--check',str(tmp)],capture_output=True,text=True); tmp.unlink(missing_ok=True)
ck('JavaScript syntax',p.returncode==0,p.stderr.strip())
# Compile/run pure Kotlin DSP regression.
sources=[
'app/src/main/java/com/marko/auralis/audio/dsp/PulseMath.kt',
'app/src/main/java/com/marko/auralis/audio/scheduler/StereoPulseScheduler.kt',
'app/src/main/java/com/marko/auralis/audio/generators/NoiseGenerator.kt',
'app/src/main/java/com/marko/auralis/audio/generators/AdvancedGenerators.kt',
'app/src/main/java/com/marko/auralis/audio/mix/AudioMixer.kt',
'app/src/main/java/com/marko/auralis/model/SoundLayerConfig.kt',
'app/src/main/java/com/marko/auralis/model/AudioConfig.kt','tools/FinalDspRegression.kt']
jar=ROOT/'tools/.phase26.jar'
c=subprocess.run(['kotlinc',*map(lambda x:str(ROOT/x),sources),'-include-runtime','-d',str(jar)],capture_output=True,text=True)
ck('Kotlin DSP regression compile',c.returncode==0,c.stderr[-1000:])
if c.returncode==0:
    r=subprocess.run(['java','-jar',str(jar)],capture_output=True,text=True)
    ck('Kotlin DSP regression runtime',r.returncode==0 and 'FINAL_DSP_REGRESSION_OK' in r.stdout,(r.stdout+r.stderr)[-1800:])
    detail=r.stdout.strip()
else: detail=''
jar.unlink(missing_ok=True)
passed=sum(ok for _,ok,_ in checks);print('\n'.join(('PASS' if ok else 'FAIL')+' - '+n+((' :: '+d) if d and not ok else '') for n,ok,d in checks));print(f'RESULT {passed}/{len(checks)}');
if detail: print(detail)
sys.exit(0 if passed==len(checks) else 1)
