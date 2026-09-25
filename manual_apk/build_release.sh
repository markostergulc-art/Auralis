#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
set -a
source keystore.properties
set +a
python3 build_manual_apk.py
UNSIGNED="out/Auralis_v2.0.1_unsigned.apk"
V1="out/Auralis_v2.0.1_v1.apk"
ALIGNED="out/Auralis_v2.0.1_v1_aligned.apk"
FINAL="out/Auralis_v2.0.1.apk"
jarsigner -keystore "$KEYSTORE" -storetype PKCS12 -storepass "$STORE_PASSWORD" \
  -signedjar "$V1" "$UNSIGNED" "$KEY_ALIAS"
python3 tools/zipalign4_fallback.py "$V1" "$ALIGNED"
python3 tools/apk_v2v3_sign.py "$ALIGNED" "$FINAL" --p12 "$KEYSTORE" --password "$STORE_PASSWORD"
python3 tools/apk_v2v3_verify.py "$FINAL"
jarsigner -verify "$FINAL"
sha256sum "$FINAL"
