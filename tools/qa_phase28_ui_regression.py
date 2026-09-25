from pathlib import Path
import re,sys
ROOT=Path(__file__).resolve().parents[1]
html=(ROOT/'manual_apk/assets/index.html').read_text()
checks=[]
def ck(n,c,d=''): checks.append((n,bool(c),d))
# Main layout/order invariants.
main=re.search(r'<div class="app" id="mainApp">(.*?)</div>\s*\n\s*<div id="volumeWin"',html,re.S)
ck('Main block found',bool(main))
m=main.group(1) if main else ''
order=['id="play"','presetCenter','mainWindows','data-open="soundModes"','class="actions"','data-open="timer"','data-open="presets"','data-open="settings"']
pos=[m.find(x) for x in order]
ck('Main order stable',all(x>=0 for x in pos) and pos==sorted(pos),str(pos))
ck('No app title above Play/Stop','class="title"' not in m)
ck('Volume/Pulse above bottom actions',m.find('data-open="volumeWin"')<m.find('class="actions"') and m.find('data-open="pulseWin"')<m.find('class="actions"'))
ck('Timer/Presets/Settings bottom order',m.find('data-open="timer"')<m.find('data-open="presets"')<m.find('data-open="settings"'))
ck('Current preset centered area','presetCenter' in m and 'id="presetNow"' in m)
# Existing user-requested window details.
ck('Volume icon speaker','data-open="volumeWin"><span class="ico">🔊</span>' in html)
ck('Timer icon enlarged','.action .timerIcon{font-size:2.55rem' in html)
ck('Timer buttons left/center/right','timerBtns' in html and 'class="sheetBtn left"' in html and 'class="sheetBtn center"' in html and 'class="sheetBtn primary right"' in html)
ck('All non-first windows full height','.modal:not(.first) .sheet{height:100vh;height:100dvh' in html)
ck('Window sheets full height','.windowSheet{display:flex;flex-direction:column;min-height:100vh;min-height:100dvh}' in html)
ck('Safe-area top/bottom','env(safe-area-inset-top)' in html and 'env(safe-area-inset-bottom)' in html)
# Theme and readability.
ck('Dark theme tokens','--bg:#081018' in html and '--turq:#38c7c9' in html)
ck('Light theme uses turquoise',':root.light' in html and '--turq:#159b9e' in html and 'orange' not in html.lower())
ck('Readable base font','font-size:17px' in html and '@media (max-width:430px){html,body{font-size:16.5px}' in html)
ck('Touch targets >=46 for key controls','min-height:46px' in html and '.infoBtn{width:46px;height:46px' in html)
# Accessibility/state.
ck('Live timer status','id="timerActive" class="timerActive" aria-live="polite"' in html)
ck('Live diagnostics','id="diag" class="diag" aria-live="polite"' in html)
ck('Live experiment results','id="expResults" aria-live="polite"' in html)
ck('Toast status role','id="toast" class="toast" role="status" aria-live="polite"' in html)
ck('Lock buttons exposed',html.count('class="lockBtn"')>=7)
# First run remains distinct and not forced full-screen sheet behavior.
ck('First language modal','id="langFirst" class="modal first"' in html)
ck('First safety modal','id="safetyFirst" class="modal first"' in html)
# JS syntax.
js=html.split('<script>',1)[1].rsplit('</script>',1)[0];tmp=ROOT/'tools/.phase28.js';tmp.write_text(js)
import subprocess
p=subprocess.run(['node','--check',str(tmp)],capture_output=True,text=True);tmp.unlink(missing_ok=True)
ck('JavaScript syntax',p.returncode==0,p.stderr.strip())
passed=sum(ok for _,ok,_ in checks)
for n,ok,d in checks: print(('PASS' if ok else 'FAIL')+' - '+n+((' :: '+d) if d and not ok else ''))
print(f'RESULT {passed}/{len(checks)}')
sys.exit(0 if passed==len(checks) else 1)
