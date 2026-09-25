# Auralis â€” manual APK / signing

This directory contains the SDK-independent fallback APK builder included with v2.0.1.
The fallback APK uses an offline local WebView for UI and a native Android `AudioTrack` bridge for PCM16 playback.

## Critical signing rule

`keys/Auralis_release.p12` is the persistent sideload release identity for Auralis v2.x.
Do not regenerate or replace this key if future APKs must install as an update over existing Auralis v2.x installations.
`keystore.properties` contains private signing credentials. Keep both files private and backed up.

## Build

Requirements: Python 3, Java/JDK `jarsigner`, Python `cryptography` package.

```bash
./build_release.sh
```

Output:

```text
out/Auralis_v2.0.1.apk
```

The build sequence is deliberately:

1. build unsigned APK;
2. V1/JAR sign;
3. 4-byte align STORED entries;
4. add APK Signature Scheme V2/V3;
5. verify signatures.

For a Play Store/production build, prefer the full Gradle/Android SDK project under `app/` and keep the same application ID only if migration/signing strategy is intentionally managed.

## Local credentials (not included)

Restore the existing private key to `keys/Auralis_release.p12` and create the ignored `keystore.properties` locally with shell assignments for `KEYSTORE`, `STORE_PASSWORD`, and `KEY_ALIAS`. Use your existing values; no credentials are published here. The original private files remain in your source ZIP.
