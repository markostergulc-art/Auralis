from pathlib import Path
import re,subprocess,sys,zipfile,hashlib
ROOT=Path(__file__).resolve().parents[1]
APK=ROOT/'manual_apk/out/BrainRelaxSoundPerception_v1.7.0.apk'
checks=[]
def ck(n,c,d=''): checks.append((n,bool(c),d))
ck('Final APK exists',APK.exists() and APK.stat().st_size>20000,str(APK))
gradle=(ROOT/'app/build.gradle.kts').read_text(); html=(ROOT/'manual_apk/assets/index.html').read_text(); builder=(ROOT/'manual_apk/build_manual_apk.py').read_text()
ck('Gradle versionName 1.7.0','versionName = "1.7.0"' in gradle)
ck('Gradle versionCode 11','versionCode = 11' in gradle)
ck('Fallback UI version 1.7.0','v1.7.0' in html and 'v1.6.0' not in html)
ck('Manual manifest version 1.7.0',"'1.7.0'" in builder and 'data_val=11' in builder)
ck('Release notes present',(ROOT/'RELEASE_NOTES_v1.7.0.md').exists())
ck('Scientific inventory present',(ROOT/'SCIENTIFIC_FEATURE_INVENTORY_v1.7.0.md').exists())
ck('Localization report present',(ROOT/'LOCALIZATION_REPORT_v1.7.0.txt').exists())
for f in ['PHASE_26_REPORT.txt','PHASE_27_REPORT.txt','PHASE_28_REPORT.txt','PHASE_29_REPORT.txt','PHASES_26_30_REPORT.md']:
    ck('Phase release artifact '+f,(ROOT/f).exists())
ck('Persistent signing key present',(ROOT/'manual_apk/keys/BrainRelaxSoundPerception_release.p12').exists())
ck('Keystore properties present',(ROOT/'manual_apk/keystore.properties').exists())
if APK.exists():
    with zipfile.ZipFile(APK) as z:
        ck('APK ZIP integrity',z.testzip() is None)
        ck('Embedded HTML exact',z.read('assets/index.html')==(ROOT/'manual_apk/assets/index.html').read_bytes())
    v=subprocess.run(['python3',str(ROOT/'manual_apk/tools/apk_v2v3_verify.py'),str(APK)],capture_output=True,text=True); out=v.stdout+v.stderr
    ck('V2 signature verified','V2 (True' in out,out[-1000:])
    ck('V3 signature verified','V3 (True' in out,out[-1000:])
    m=re.search(r"V2 \(True, '([0-9a-f]+)'\)",out);cert=m.group(1) if m else ''
    ck('Signing certificate continuity',cert=='e5ec9e69cb9ce0f2a034077f09dcaaf0a5ab6df123c72a34c7f8c848e58df22b',cert)
    j=subprocess.run(['jarsigner','-verify',str(APK)],capture_output=True,text=True)
    ck('V1/JAR signature',j.returncode==0 and 'jar verified' in (j.stdout+j.stderr).lower())
passed=sum(ok for _,ok,_ in checks)
for n,ok,d in checks: print(('PASS' if ok else 'FAIL')+' - '+n+((' :: '+d) if d and not ok else ''))
print(f'RESULT {passed}/{len(checks)}')
if APK.exists(): print('APK_SHA256 '+hashlib.sha256(APK.read_bytes()).hexdigest())
sys.exit(0 if passed==len(checks) else 1)
