from pathlib import Path
import subprocess,sys,re
ROOT=Path(__file__).resolve().parents[1]
html=(ROOT/'manual_apk/assets/index.html').read_text()
pulse=(ROOT/'app/src/main/java/com/marko/auralis/audio/dsp/PulseMath.kt').read_text()
engine=(ROOT/'app/src/main/java/com/marko/auralis/audio/engine/ImpulseAudioEngine.kt').read_text()
service=(ROOT/'app/src/main/java/com/marko/auralis/service/PlaybackService.kt').read_text()
routes=(ROOT/'app/src/main/java/com/marko/auralis/service/AudioRouteSafety.kt').read_text()
manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text()
builder=(ROOT/'manual_apk/build_manual_apk.py').read_text()
checks=[]
def ck(n,c,d=''): checks.append((n,bool(c),d))
# Huawei / handset transient compatibility.
ck('Huawei-friendly asymmetric biphasic native','Huawei/handset-friendly asymmetric DC-balanced biphasic transient' in pulse and 'exp(-0.6' in pulse)
ck('Huawei-friendly waveform fallback','Math.exp(-0.6*i)' in html and 'baseAmp=.75' in html)
# Native generic Android path suitable across Pixel/Samsung/Huawei Android APIs.
ck('Native streaming AudioTrack','AudioTrack.MODE_STREAM' in engine and 'AudioFormat.ENCODING_PCM_FLOAT' in engine)
ck('Native AudioTrack initialized check','AudioTrack.STATE_INITIALIZED' in engine)
ck('Native blocking write error handling','AudioTrack.WRITE_BLOCKING' in engine and 'AudioTrack write failed' in engine)
ck('Native sample-rate discovery','AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE' in engine and '48_000' in engine)
ck('Native low-latency compatible fallback','PERFORMANCE_MODE_LOW_LATENCY' in engine and 'PERFORMANCE_MODE_NONE' in engine)
# Fallback manual APK compatibility.
ck('Fallback native sample-rate bridge','getNativeOutputSampleRate' in builder and 'getNativeSampleRate' in builder)
ck('Fallback relax layers capped at 48k','return anyRelaxLayer()?Math.min(sr,48000):sr' in html)
ck('Fallback AudioTrack failure returned to UI','const ok=Android.startPcm' in html and "throw new Error('AudioTrack init failed')" in html)
# Routing / Bluetooth / wired safety.
ck('AudioDeviceCallback present','AudioDeviceCallback' in service and 'onAudioDevicesAdded' in service and 'onAudioDevicesRemoved' in service)
ck('Becoming-noisy stop protection','ACTION_AUDIO_BECOMING_NOISY' in service)
for token in ['TYPE_WIRED_HEADSET','TYPE_WIRED_HEADPHONES','TYPE_BLUETOOTH_A2DP','TYPE_BLUETOOTH_SCO','TYPE_USB_HEADSET','TYPE_HEARING_AID','TYPE_BLE_HEADSET']:
    ck('Route safety '+token,token in routes)
ck('Headphone start gain 50%','HEADPHONE_START_GAIN = 0.50f' in routes)
ck('Fallback route safety 50%','routeSafetyGain=.5' in html and 'getOutputDeviceType' in html)
# GrapheneOS / generic Android independence.
allkt='\n'.join(p.read_text(errors='ignore') for p in (ROOT/'app/src/main/java').rglob('*.kt'))
ck('No Google Play Services dependency','com.google.android.gms' not in allkt)
ck('No manufacturer-specific API dependency',all(x not in allkt.lower() for x in ['huawei.android','samsung.android','com.huawei.','com.samsung.']))
ck('No Internet permission full source','android.permission.INTERNET' not in manifest)
ck('Foreground media playback declared','foregroundServiceType="mediaPlayback"' in manifest and 'FOREGROUND_SERVICE_MEDIA_PLAYBACK' in manifest)
# Compile/run sample-rate/device DSP matrix.
sources=['app/src/main/java/com/marko/auralis/audio/dsp/PulseMath.kt','app/src/main/java/com/marko/auralis/audio/scheduler/StereoPulseScheduler.kt','app/src/main/java/com/marko/auralis/model/AudioConfig.kt','app/src/main/java/com/marko/auralis/audio/generators/AdvancedGenerators.kt','tools/DeviceCompatibilitySmoke.kt']
jar=ROOT/'tools/.device_compat.jar'
c=subprocess.run(['kotlinc',*map(lambda x:str(ROOT/x),sources),'-include-runtime','-d',str(jar)],capture_output=True,text=True)
ck('Device DSP matrix compile',c.returncode==0,c.stderr[-1200:])
detail=''
if c.returncode==0:
    r=subprocess.run(['java','-jar',str(jar)],capture_output=True,text=True)
    ck('44.1/48/96/192 kHz DSP matrix',r.returncode==0 and 'DEVICE_COMPAT_DSP_OK' in r.stdout,(r.stdout+r.stderr)[-2000:])
    detail=r.stdout.strip()
jar.unlink(missing_ok=True)
passed=sum(ok for _,ok,_ in checks)
for n,ok,d in checks: print(('PASS' if ok else 'FAIL')+' - '+n+((' :: '+d) if d and not ok else ''))
print(f'RESULT {passed}/{len(checks)}')
if detail: print(detail)
print('PHYSICAL_DEVICE_EXECUTION: NOT RUN (no ADB-connected Huawei/Pixel/Samsung device in this environment)')
sys.exit(0 if passed==len(checks) else 1)
