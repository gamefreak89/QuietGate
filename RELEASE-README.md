# QuietGate 0.11-alpha — release README and disclosure

Prepared 2026-10-26. Development/debug-signed Android build, versionCode 12. This release reorganizes the interface and hardens notification readiness, queued actions, and data-transfer exclusions. See DEVICE-VERIFICATION.md for actual checks and remaining limits.

## Included files

- QuietGate-0.11-alpha.apk: Android application.
- source.zip and source/: project sources, existing verification tools, licenses, and documentation.
- CHANGELOG-README.md: standalone readable release changes, provided beside the APK.
- QuietGate-0.11-alpha-documentation.zip: a documentation folder containing the changelog, disclosure, security review, and supporting documents.
- SHA256SUMS.txt: hashes for all packaged files except itself.
- release.json: version and build metadata.

GitHub assets are a flat list. Upload the APK, source.zip, CHANGELOG-README.md, RELEASE-README.md, documentation ZIP, and checksum file as separate assets. The ZIP contains the requested documentation folder. No private device data, signing keys, OAuth tokens, or SDK paths belong in the distribution.

## Permissions and setup

READ_CONTACTS reads saved caller identifiers. Android Notification Access is broad, while QuietGate filters its processing to Meet. POST_NOTIFICATIONS and foreground-service declarations support visible status and attention notices. ACCESS_NETWORK_STATE checks Wi-Fi and refresh constraints. INTERNET supports optional Google authorization and manual or explicitly enabled automatic saved-contact refresh. RECEIVE_BOOT_COMPLETED supports lease restoration and native persisted jobs. BIND_JOB_SERVICE and the notification-listener binding permission protect exported system services; they do not grant privileged access to QuietGate.

No new permission or library was added in 0.11-alpha. No microphone, recording, location, phone-state, system call-log, overlay, or GET_ACCOUNTS permission is requested. Document pickers grant access to selected files.

Active Wi-Fi and running, authorized Shizuku are required to enable protection. If either becomes unavailable, a separate 24-hour grace deadline starts. After expiry, restore both and manually enable protection. Root is optional, unverified, and does not strengthen Meet control. Shizuku supplies readiness checks rather than a privileged Meet call-control API.

Google saved contacts are optional and individually authorized using contacts.readonly. Google Cloud Android OAuth registration for the package/signing certificate and People API enablement are external prerequisites. Saved email/phone fields are fetched; Other contacts, Gmail, directories, and names are excluded. Automatic refresh is off by default and runs about every 12 hours when enabled, subject to Android scheduling and network/battery constraints. Its snapshot expires 24 hours after success. See GOOGLE-CONTACTS-SETUP.md.

## Privacy and local data

Caller matching is local. There is no QuietGate backend, advertising, analytics, telemetry, or runtime AI. Google receives authorization and contact-refresh requests under its own terms, but caller identifiers are not uploaded for matching.

Connected-account labels, keyed identifier fingerprints, refresh settings/status, and opaque scheduling metadata are AES-GCM encrypted using Android Keystore keys in no-backup storage. QuietGate does not write OAuth tokens to its files or exports. Android job extras contain opaque references, not account names or tokens. Up to ten accounts and 20,000 fingerprints per account are supported. Generation checks prevent late refresh results from restoring removed or disabled matches.

The call catalog and recent decisions are app-private, but do not use the Google-cache encryption keys. The catalog retains up to 500 newest attempts within 90 days, pruning on access/write. Rejection fingerprints are bounded and short-lived. Clear local history & fingerprints deletes decisions, catalog, and rejection fingerprints after confirmation; connected Google data remains separate.

Disconnect removes local matches and scheduled jobs before requesting Google revocation. Delete-all removes local connected data and keys; remaining Google grants require separate management. Device contacts and Meet history are not deleted. Uninstall removes private app data but may leave Google grants. Explicit Android 12+ cloud-backup and device-transfer exclusions supplement allowBackup=false. Privileged/debug access can still read app data; this is not a production-hardened build.

JSON and incident ZIP exports are unencrypted and may contain caller identities, timestamps, notes, categories, and original attachment bytes/metadata. Cloud file providers may transfer selected files. Connected-account caches, lists, and tokens are excluded from call exports. Incident limits are ten files, 10 MiB per file, and 25 MiB total. Temporary ZIPs are deleted on normal completion/failure, but interruption can leave private cache files. Exports and original attachments require separate deletion. Checksums do not authenticate identities, claims, or timestamps.

## FOSS and licenses

QuietGate's own code and original assets are MIT-licensed and free to use, inspect, modify, and redistribute under those terms, including commercial use with required notices preserved. Release packages include source. The GitHub repository was private when inspected on 2026-10-26; public accessibility requires an explicit visibility decision by the owner.

The optional Google integration includes Google Play services Auth 22.0.0 and transitive libraries with separate Google SDK terms and third-party notices. The complete dependency stack is not entirely FOSS. Shizuku client libraries 13.1.5 are MIT-licensed, and AndroidX components retain separate notices. Android, Meet, Google services, and the external Shizuku manager/server have their own terms. No F-Droid, FSF, Google Play, or independent security certification is claimed. Full license texts remain bundled and accessible from About.

## Languages and AI assistance

The application is written primarily in Java, with XML resources, Gradle build configuration, and Python packaging. UI and documentation are English. AI assisted the implementation, wording, and source review. No AI model runs in the app or makes caller decisions. See AI-DISCLOSURE.md.

## Changes in this release

A concise About hub replaces the combined legal/help dialog. Purpose, permissions, privacy, operation, contacts, Google refresh, catalog files, incident reports, FOSS, complete legal notices, changelog, and recent decisions have separate buttons. The dashboard no longer appends long reference paragraphs. Copy, punctuation, encoding, system-bar spacing, contrast, and button wrapping were improved. History deletion now requires confirmation.

Security hardening compares the exact enabled-listener component, checks that queued Meet decline actions remain current and were created by Meet, and explicitly excludes app data from cloud backup and device transfer. See CHANGELOG.md, CHANGELOG-README.md, and SECURITY-REVIEW.md.

## Verification and limitations

Build, static-analysis, signature, install, and UI verification results are recorded in DEVICE-VERIFICATION.md. Existing historical test results do not establish coverage for these changes. No live caller rejection, renewed Google consent, cross-device migration, exhaustive vulnerability scan, or endurance test is claimed for this release.

Meet caller information is unauthenticated. Calls may ring before an exposed action can be invoked. Android can delay or stop background work. QuietGate cannot independently confirm that a call ended and does not block cellular calls. English Decline/Reject action labels are supported. Final action checks narrow a race window but cannot make Android notification state and PendingIntent.send atomic.

The APK uses the existing development/debug signing certificate to update the installed app without uninstalling it. Keep signing material private. Production distribution requires a protected release-signing plan. SHA-256 checksums establish byte integrity, not security certification.
