from pathlib import Path
import re,sys
ROOT=Path(__file__).resolve().parents[1]
files=[ROOT/'manual_apk/assets/index.html',ROOT/'app/src/main/java/com/marko/auralis/content/FeatureInfoCatalog.kt',ROOT/'app/src/main/java/com/marko/auralis/content/LearnCatalog.kt']
text='\n'.join(p.read_text(errors='ignore') for p in files)
low=text.lower()
checks=[]
def ck(n,c,d=''): checks.append((n,bool(c),d))
# Strong prohibited affirmative medical/pseudoscientific claims.
prohibited=[
 r'(?<!not )(?<!no )cures? anxiety',r'(?<!not )treats? ptsd',r'(?<!not )treats? anxiety',
 r'(?<!not )heals? dna',r'(?<!not )repairs? dna',r'(?<!not )regenerates? cells',
 r'(?<!not )activates? the vagus nerve',
 r'induces? theta brainwaves?',r'puts? (?:the |your )?brain into theta',
 r'synchroni[sz]es? (?:the |your )?brain with (?:the )?earth',
 r'clinically proven to (?:relax|heal|treat|cure)',r'healing frequency',r'frequency of the universe'
]
for pat in prohibited:
    hits=re.findall(pat,low)
    ck('No unsupported claim: '+pat,not hits,str(hits[:3]))
ck('Vagus claim explicitly limited','does not directly stimulate the vagus nerve' in low)
# Required caveats around sensitive/alternative topics.
requirements={
 'Binaural caveat': ['binaural','headphones','entrainment','inconsistent'],
 '432 distinction': ['432','a4','pure'],
 '528 limitation': ['528','dna','not established'],
 'Schumann distinction': ['7.83','electromagnetic','not physically equivalent'],
 'Bilateral/EMDR limitation': ['bilateral','emdr','not equivalent'],
 '40 Hz research limitation': ['40','research only'],
 'Safe listening limitation': ['app gain is not calibrated spl'],
}
for name,terms in requirements.items(): ck(name,all(t in low for t in terms),','.join(t for t in terms if t not in low))
# Evidence taxonomy and alternative approaches remain explicitly represented.
for token in ['EVIDENCE SUPPORTED','EMERGING EVIDENCE','EXPERIMENTAL','TRADITIONAL / EXPLORATORY','RESEARCH ONLY']:
    ck('Evidence class '+token,token.lower() in low)
for token in ['432','528','solfeggio','7.83','singing bowl','sound bath','humming']:
    ck('Alternative retained '+token,token in low)
# The app must identify non-medical positioning in all three locales.
for phrase in ['not a medical device','kein medizinprodukt','nije medicinski uređaj']:
    ck('Non-medical positioning '+phrase,phrase in low)
# INFO content must remain concrete: units/mechanisms, not only generic wellness prose.
for phrase in ['200 hz','206 hz','power-per-octave','4 s inhale','6 s exhale','20.83','earth-ionosphere','lead/lag']:
    ck('Concrete educational content '+phrase,phrase in low)
passed=sum(ok for _,ok,_ in checks)
for n,ok,d in checks: print(('PASS' if ok else 'FAIL')+' - '+n+((' :: '+d) if d and not ok else ''))
print(f'RESULT {passed}/{len(checks)}')
sys.exit(0 if passed==len(checks) else 1)
