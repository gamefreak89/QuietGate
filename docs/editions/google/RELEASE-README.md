# QuietGate 0.13-alpha Google edition — release disclosure

APK: QuietGate-google-0.13-alpha.apk. App label: QuietGate. Application ID: org.open.quietgate.

This edition preserves optional Google saved-contact connections and manual/automatic refresh using Google Play services Auth 22.0.0. INTERNET supports authorized Google requests. Per-account automatic refresh is off by default, roughly every 12 hours when enabled; Android may delay it and snapshots expire after 24 hours. Up to ten accounts and 20,000 keyed fingerprints/account are supported. Labels/cache/settings are AES-GCM encrypted with Keystore keys in no-backup storage; tokens are not written to QuietGate files/exports. Google receives authorization/refresh requests under its own terms. There is no caller-identifier upload for matching. Complete runtime dependencies are not entirely FOSS. Google Cloud Android OAuth registration and People API setup remain prerequisites.

## Included files

The named edition APK is a development/debug-signed Android build, versionCode 14. source.zip and source/ contain the complete dual-edition project with its MIT-licensed code, build declarations, license notices, existing historical verification sources, and documentation. Google binaries are not embedded in the source archive or FOSS APK. The versioned documentation ZIP, CHANGELOG-README.md, this disclosure, SHA256SUMS.txt, and release.json accompany each APK. Checksums establish integrity, not certification. No signing keys, credentials, contacts, call exports, device identifiers, or SDK paths belong in the bundle.

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

The enable-protection popup now explains purpose, operation, limitations, required setup, and edition-specific optional access in short, scrollable sections. Google-only OAuth consent appears only in the Google edition. No permissions, dependencies, or protection-policy changes. See REQUIREMENTS-REVIEW.md and CHANGELOG.md.

## Verification and limitations

Both development APK builds and Android lint completed. Compiled APK inspection checks edition identity, permissions, Google type references, notices, and current changelog. See ARTIFACT-INSPECTION.json and DEVICE-VERIFICATION.md for exact results and device observations. No new automated tests were added. The user's successful 0.12-alpha FOSS live-call test is user-reported evidence, not independently observed coverage of this version.

Meet must expose a usable notification and English Decline/Reject action. Calls may ring first; identity and rejection outcomes remain unverified. Android can delay or stop background work. Phone matching uses identical normalized digits. This is not a cellular-call blocker or an independent security audit. No outage, 24-hour endurance, live OAuth, or root verification is claimed.

APKs are development/debug builds signed with the existing private development certificate. Both retain their edition-specific package IDs and private data on normal upgrades. Keys are excluded. No publication, repository-visibility change, or F-Droid submission is implied by these packages.
