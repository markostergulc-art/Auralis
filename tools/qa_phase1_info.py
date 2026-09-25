from pathlib import Path
import sys,re
r=Path(__file__).resolve().parents[1]
h=(r/'manual_apk/assets/index.html').read_text()
model=(r/'app/src/main/java/com/marko/auralis/model/FeatureInfo.kt').read_text()
cat=(r/'app/src/main/java/com/marko/auralis/content/FeatureInfoCatalog.kt').read_text()
checks={
 'feature_info_model': 'data class FeatureInfo' in model and 'EvidenceLevel' in model,
 'five_evidence_levels': all(x in model for x in ['EVIDENCE_SUPPORTED','EMERGING_EVIDENCE','EXPERIMENTAL','TRADITIONAL_EXPLORATORY','RESEARCH_ONLY']),
 'offline_catalog': 'pulse_engine' in cat and 'LocalizedFeatureText' in cat,
 'info_modal': 'id="featureInfo"' in h and 'id="infoBody"' in h,
 'pulse_info_button': 'data-info="pulse_engine"' in h,
 'nine_info_sections': all(k in h for k in ['whatIsThis','whatHappens','howToUse','whatNotice','headphonesSpeaker','startingPoint','evidence','limitation','safety']),
 'three_locales': all(x in h for x in ['en:{info:', 'de:{info:', 'hr:{info:']),
 'non_generic_pulse': '20.83 microseconds' in h and '4 Hz pulse rate does not by itself' in h,
}
for k,v in checks.items(): print(('PASS' if v else 'FAIL'),k)
f=[k for k,v in checks.items() if not v]
print(f'RESULT {len(checks)-len(f)}/{len(checks)}')
sys.exit(bool(f))
