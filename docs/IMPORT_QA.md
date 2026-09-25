# Auralis v2.0.1 import QA

Date: 2026-09-25

- Source: Auralis_v2.0.1_source.zip; SHA-256: `6dace6734ebf4ece503c8a7576a84aad5c1b796947a9aa03c5ebeec4dc40c73e`.
- Excluded: `manual_apk/keys/Auralis_release.p12` and `manual_apk/keystore.properties`.
- No copies of the source signing password values detected in retained files.
- Python syntax: 38 files passed AST parsing.
- Embedded JavaScript: Node syntax check passed.
- Application ID: com.marko.auralis; versionName 2.0.1; versionCode 21.
- Original Kotlin, HTML, DSP code and icon assets retained unchanged.
- Updated README, signing documentation and .gitignore for public source distribution.
- Original standalone QA/build reports were not present in the supplied ZIP.
- Signed APK, Gradle build, DSP runtime tests and physical-device tests were not executed in this import.
- No new project license selected; existing repository license is preserved.
