# QuietGate 0.12-alpha FOSS edition — release disclosure

APK: QuietGate-foss-0.12-alpha.apk. App label: QuietGate FOSS. Application ID: org.open.quietgate.foss.

This edition uses contacts already available through Android. It contains no Google Play services library, OAuth/account connection, Google contact cache/refresh job, or Internet permission. Only MIT/Apache-2.0 application/runtime dependencies are included. Google Meet remains an external proprietary app. The complete source project also contains the separately buildable Google flavor, whose libraries are excluded from this APK.

## Included files

The named edition APK is a development/debug-signed Android build, versionCode 13. source.zip and source/ contain the complete dual-edition project with its MIT-licensed code, build declarations, license notices, existing historical verification sources, and documentation. Google binaries are not embedded in the source archive or FOSS APK. The versioned documentation ZIP, CHANGELOG-README.md, this disclosure, SHA256SUMS.txt, and release.json accompany each APK. Checksums establish integrity, not certification. No signing keys, credentials, contacts, call exports, device identifiers, or SDK paths belong in the bundle.

## Permissions and setup

Both editions use READ_CONTACTS for local matching, broad Android notification access filtered to Meet, POST_NOTIFICATIONS and foreground-service declarations for status, ACCESS_NETWORK_STATE for Wi-Fi readiness, RECEIVE_BOOT_COMPLETED for restoration, and the Shizuku API authorization. Notification-listener binding and privileged Shizuku provider permissions protect their components. Root is optional and unverified; it does not strengthen Meet control. Shizuku is a readiness condition, not a privileged Meet call-control API.

Active Wi-Fi and running, authorized Shizuku are still needed to enable protection. Losing either starts a saved 24-hour grace timer. After expiry, restore both and manually enable protection. The FOSS app ID needs independent grants; no grants or private data migrate automatically. Installations can coexist, but enable notification access/protection in only one edition to avoid handling the same call twice.

Document pickers grant access to selected catalog imports, report attachments, and export destinations. No broad storage, microphone, recording, location, phone-state, system call-log, or overlay permission is requested. See PERMISSIONS.md and EDITIONS.md for edition differences.

## Privacy and local data

Caller matching stays on-device. There is no QuietGate backend, telemetry, analytics, advertising, or runtime AI. Both editions keep private call records, decision history, and bounded rejection fingerprints. Call catalogs retain 500 newest attempts within 90 days, pruned on access/write; identities and action outcomes are unverified. Decision history is limited to 30 events. Local catalog/preferences are private but not additionally encrypted by the app. Backup is disabled and explicit cloud/device-transfer exclusions apply. Rooted/compromised devices and authorized development debugging are outside that boundary.

JSON and incident ZIP exports are unencrypted and can contain identities, timestamps, categories, notes, and original attachment bytes/metadata. A cloud document provider can transfer selected files using its own network access, including from the FOSS edition. Incident limits remain ten files, 10 MiB/file, and 25 MiB total. Temporary ZIPs are deleted on normal completion/failure; interrupted work can leave private cache files. Exported copies and original attachments require separate deletion. Hashes do not establish authenticated identity, independently verified time, or chain of custody.

Clear local history & fingerprints requires confirmation and removes this edition's decisions, catalog, and rejection fingerprints. It does not delete device contacts, Meet history, exports, or another edition's private storage. Uninstall removes the selected edition's private data. The Google edition's separate account cache and grants require its dedicated disconnect/delete controls; Google-side grants can survive uninstall.

## FOSS and licenses

Project code and original assets are MIT-licensed and free to use, inspect, modify, and redistribute with required notices preserved. Commercial use is permitted under MIT. Shizuku 13.1.5 client modules are MIT and AndroidX Annotation 1.3.0 is Apache-2.0. Full texts and notices are bundled. Google dependencies belong only to the Google edition and have separate Google SDK terms. The FOSS APK excludes those dependencies.

Google Meet is a separate proprietary app/service used by both editions. Android, the external Shizuku manager/server, and build tools have their own licenses/terms. FOSS licensing of bundled code is not a claim that the whole phone/ecosystem is FOSS, vulnerability-free, certified, or accepted by F-Droid. The source repository remains private; public current source and F-Droid review are still required. See FOSS.md and F-DROID.md.

## Languages and AI assistance

Application code is primarily Java, with XML resources and Gradle configuration. Packaging and APK inspection use Python. UI and documentation are English. AI assisted implementation, documentation, and source review. No AI model executes in the app or makes caller decisions. See AI-DISCLOSURE.md.

## Changes in this release

Two compile-time flavors share the existing protection, quiet alerts, catalog, JSON files, incident reports, organized About pages, and license/changelog navigation. Google integration classes, manifests, help, notices, and historical verification sources are restricted to the Google source set. FOSS has its own adapter, label, app ID, private storage, and accurate disclosures. Build-time dependency allowlisting rejects unreviewed FOSS compile/runtime artifacts. APK inspection checks actual package/permissions, DEX type references, edition markers, and license/help assets. Packaging rejects wrong-edition or stale-changelog APKs.

## Verification and limitations

Final computer checks passed: both development APKs, both unsigned non-debuggable release APKs, and the existing edition-specific test APKs compiled. FOSS lint: 0 errors/12 warnings. Google lint: 0 errors/13 warnings. Both development signatures matched the existing certificate. DEX/manifest/asset inspection passed for both editions and build types, with zero Google type references or Internet permission in FOSS. See ARTIFACT-INSPECTION.json for the packaged development APK hashes/permissions. No phone or emulator was used.

Builds and artifact inspection run on the computer and do not require a phone. See DEVICE-VERIFICATION.md and the accompanying edition-inspection report for actual results. Existing instrumentation sources were compiled but not run; no new instrumentation tests were added. Neither 0.12-alpha edition was installed on a device. UI, permission setup, real caller behavior, exports, background readiness, coexistence, and endurance remain unverified for these new flavors. Earlier 0.11 device observations are historical and do not verify FOSS.

Meet must expose a usable notification and English Decline/Reject action. Calls may ring first; caller identity and call-ending outcomes are unconfirmed. Android can stop/delay background work. Final action checks cannot make notification state and PendingIntent invocation atomic. Phone matching uses identical normalized digits, not general country-code normalization. This is not a cellular-call blocker or an independent security audit.

Development APKs are debug-signed with the existing private development certificate. The Google edition's app ID is unchanged and the FOSS edition is separate. Unsigned, non-debuggable release builds can be produced with assembleFossRelease/assembleGoogleRelease, but are not installable until signed. Production distribution requires a protected signing policy; keys are excluded. No release was published, repository made public, or F-Droid submission performed by building these packages.
