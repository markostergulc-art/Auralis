from pathlib import Path
import re, subprocess, tempfile, json, sys
root=Path(__file__).resolve().parents[1]
html=(root/'manual_apk/assets/index.html').read_text()
m=re.search(r'<script>\s*(.*?)\s*</script>', html, re.S)
if not m: raise SystemExit('FAIL - script not found')
js=m.group(1)
# Inject inside the existing IIFE, just before the terminal closure.
needle='})();'
pos=js.rfind(needle)
if pos<0: raise SystemExit('FAIL - IIFE end not found')
inject=r'''
// QA injection: render every built-in preset through production makePcm().
renderSeconds=()=>2;
const __qa=[];
for(const p of BUILTIN_PRESETS){
  state={...defaults,...p.cfg,language:'en',timerMinutes:0};
  sanitize();
  const pcm=makePcm();
  const dv=new DataView(pcm.bytes.buffer,pcm.bytes.byteOffset,pcm.bytes.byteLength);
  let peak=0,sumSq=0,sum=0,count=0;
  for(let i=0;i<pcm.bytes.byteLength;i+=2){
    const x=dv.getInt16(i,true)/32767;
    if(!Number.isFinite(x))throw new Error('nonfinite '+p.id);
    peak=Math.max(peak,Math.abs(x)); sumSq+=x*x; sum+=x; count++;
  }
  const rms=Math.sqrt(sumSq/count), dc=sum/count;
  const ok=Number.isFinite(rms)&&Number.isFinite(dc)&&peak<=0.951&&peak>0&&rms>0&&Math.abs(dc)<0.08;
  __qa.push({id:p.id,peak,rms,dc,mixPeak:lastMixPeak,mixGain:lastMixGain,ok});
}
process.stdout.write(JSON.stringify(__qa));
'''
js=js[:pos]+inject+js[pos:]
# Browser/Android stubs. DOM objects accept all initialization/binding operations.
stub=r'''
const __store=new Map();
global.localStorage={getItem:k=>(__store.has(k)?__store.get(k):null),setItem:(k,v)=>__store.set(k,String(v)),removeItem:k=>__store.delete(k)};
const __cls={add(){},remove(){},contains(){return false},toggle(){return false}};
function __el(){return {value:'0',checked:false,disabled:false,textContent:'',innerHTML:'',children:[],style:{},classList:__cls,setAttribute(){},addEventListener(){},querySelectorAll(){return []},focus(){}}}
global.document={hidden:false,documentElement:{lang:'',dataset:{},classList:__cls},body:__el(),getElementById(){return __el()},querySelectorAll(){return []},addEventListener(){}};
global.window={Android:{getNativeSampleRate(){return 48000},getOutputDeviceType(){return 2},getCpuTimeMs(){return 0},getPssKb(){return 0}},addEventListener(){}};
global.Android=window.Android;
global.matchMedia=()=>({matches:false,addEventListener(){}});
global.setInterval=()=>0;global.clearInterval=()=>{};global.setTimeout=(fn)=>{if(fn)fn();return 0};global.clearTimeout=()=>{};
'''
with tempfile.TemporaryDirectory() as td:
    p=Path(td)/'qa.js'; p.write_text(stub+'\n'+js)
    r=subprocess.run(['node',str(p)],text=True,capture_output=True,timeout=120)
    if r.returncode:
        print(r.stdout); print(r.stderr,file=sys.stderr); raise SystemExit(r.returncode)
    rows=json.loads(r.stdout)
failed=[x for x in rows if not x['ok']]
for x in rows:
    print(f"{'PASS' if x['ok'] else 'FAIL'} - {x['id']}: peak={x['peak']:.4f} rms={x['rms']:.5f} dc={x['dc']:+.6f} premix={x['mixPeak']:.4f} gain={x['mixGain']:.4f}")
print(f'RESULT {len(rows)-len(failed)}/{len(rows)}')
if failed: raise SystemExit(1)
