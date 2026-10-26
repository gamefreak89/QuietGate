# Creating a local release

## 0.12-alpha and later: select the edition

Build with `gradle assembleFossDebug assembleGoogleDebug`. Package using `python tools/release.py --edition foss --apk app/build/outputs/apk/foss/debug/app-foss-debug.apk` and the corresponding `--edition google --apk app/build/outputs/apk/google/debug/app-google-debug.apk`. Output is version/edition-specific. Each edition has its own RELEASE-README.md under docs/editions; packaging selects it automatically. Keep CHANGELOG.md synchronized with the bundled changelog before building. Neither packaging nor builds install an APK or publish a release.

The remaining commands below are retained historical guidance; unflavored APK paths apply only to releases before 0.12-alpha.

1. Update versionName and increment versionCode in app/build.gradle.
2. Update CHANGELOG.md and DEVICE-VERIFICATION.md honestly; do not carry forward old results as validation of new features.
3. Build using JDK 17, Gradle 8.9 and SDK/build tools 35: `gradle assembleDebug`.
4. From this project directory run:

```powershell
python tools/release.py --apk app/build/outputs/apk/debug/app-debug.apk
```

The helper creates a new sibling `QuietGate-releases/<version>/` folder containing an APK, clean source.zip, extracted source/, README, privacy/permissions/FOSS/AI/changelog/verification documents, release metadata and SHA256SUMS.txt. Existing version folders are refused; change the version for a new release. Use `--output PATH` to choose the archive root.

The helper does not build, sign, run tests, publish, import live app data, or verify that an arbitrary supplied APK matches the source version. Supply the just-built APK from this source tree. Checksums establish file integrity, not authenticity or correctness.

Current deliverables are development/debug signed. Keep the existing development signing key private if updates must install over the tested app. A different key may require uninstalling and losing app-private data; export data first. For production, establish a protected release-signing process. Never commit signing keys or passwords.

## GitHub releases

For 0.11-alpha and later, upload CHANGELOG-README.md as a standalone asset beside the APK and include the versioned documentation ZIP. GitHub assets do not support folders; the ZIP contains the documentation folder. Keep CHANGELOG.md identical to the bundled app/src/main/assets/CHANGELOG.md before building. Packaging checks their equality and requires the new changelog and security-review documents.

Push reviewed source to the private repository first. A tag/release can later carry the versioned APK, source.zip, documentation and checksum file. No automatic publication workflow is enabled. Source folders and local release bundles are separate so binaries and private call catalogs do not enter source history accidentally.

## Mandatory disclosure for every release

Follow AGENTS.md. Update RELEASE-README.md with the exact version. Include it in the local folder and as a standalone GitHub release asset; summarize permissions, included files, privacy, licensing and known limitations directly in release notes. Maintain the front-page README. Packaging refuses missing/empty docs, absent disclosure sections, or a version mismatch before creating output. Existing release corrections are labeled documentation supplements.
