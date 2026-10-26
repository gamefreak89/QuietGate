# QuietGate editions — 0.12-alpha

Both editions share the same maintained MIT-licensed protection, catalog, incident-export, and About code. Gradle product flavors select the integration at compile time, so the FOSS APK contains no dormant Google contact implementation.

| | Google edition | FOSS edition |
| --- | --- | --- |
| App label | QuietGate | QuietGate FOSS |
| Application ID | org.open.quietgate | org.open.quietgate.foss |
| Device contacts | Yes | Yes |
| Optional Google account connection | Yes | Absent |
| Manual/automatic Google contact refresh | Yes | Absent |
| Google Play services libraries | Included | Absent |
| Internet permission | Present | Absent |
| Catalog, JSON, incident ZIPs, quiet alerts | Yes | Yes |
| Shizuku and Wi-Fi readiness/grace | Required | Required |

The Google edition preserves the existing app ID, signing certificate, and Google OAuth registration relationship. The FOSS edition has a different ID, storage sandbox, and Shizuku provider authority. Both can be installed side by side, but each requires independent contacts, notification, and Shizuku setup. Do not enable both notification listeners simultaneously: both can observe and attempt to handle the same Meet call. Keep protection enabled in the edition you intend to use.

There is no automatic migration of Google accounts, secrets, settings, or histories between editions. JSON catalog import/export can transfer selected history explicitly; exported personal data is unencrypted.

## Build

Use JDK 17, Gradle 8.9, Android SDK/build tools 35. From the project root:

```
gradle assembleFossDebug assembleGoogleDebug
gradle lintFossDebug lintGoogleDebug
gradle assembleFossRelease assembleGoogleRelease
```

Debug APKs are development-signed. Release tasks produce unsigned, non-debuggable APKs until a protected release-signing configuration is supplied. No signing key is included in source. A phone is not needed for compilation, dependency inspection, manifest inspection, or APK packaging. Real-device permission/UI/call/background tests require a device or emulator; actual Meet call verification requires a suitable live setup.

Google code, manifest entries, and account-specific help/notices live in app/src/google. The small edition adapter and FOSS disclosures live in app/src/foss. Main source has no reference to Google implementation classes. Google-specific historical instrumentation sources live in app/src/androidTestGoogle and are not compiled into the FOSS test APK.

The FOSS build resolves only reviewed MIT Shizuku 13.1.5 modules and Apache-2.0 AndroidX Annotation 1.3.0. verifyFossDependencies runs before FOSS builds and rejects unreviewed runtime coordinates. The dependency allowlist requires an explicit license review when adding or upgrading libraries. It is not a vulnerability-database scan.

Release packages include the complete dual-edition source project. That source includes the project's own MIT-licensed Google integration code, build declarations, and notices for that separate edition; the proprietary Google binaries are fetched only when building the Google flavor. They are not present in the FOSS APK.

## FOSS scope

The FOSS edition's bundled application code and runtime libraries use free/open-source licenses. Google Meet remains an external proprietary app/service, and Android/SDK components have their own terms. Source availability requires public hosting or source distribution; the current private GitHub repository is not publicly browsable. No F-Droid acceptance or certification is implied.
