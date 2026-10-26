# QuietGate 0.9-alpha device verification supplement — 2026-10-24

The existing 0.9-alpha versionCode 10 APK was installed in update mode after a verified private 0.8-alpha APK/app-data backup. Preferences matched the backup immediately after installation. No production-code or APK change was required during testing.

## Results: 57 checks passed, 0 failed

- 24 synthetic local Google-contact checks: optional/setup disclosure, saved-contact-only read scope, consent cancellation, no-account/pending/failed matching behavior, exact email/normalized phone matching, no suffix matches, integrated matcher fallback, AES-GCM private no-backup storage, decrypt/reload, last-success time, refresh pause, stale-generation rejection, expired/future timestamps, multi-account union, removal/late completion, canceled fetch before HTTP, identifier/account limits, catalog-export separation, tamper rejection, key/cache deletion, and bundled notices.
- 16 incident/catalog checks: individual selection and imported provenance, notes/title/UTC/epoch times, attachment selection result/duplicates, background export, unchanged attachment bytes and checksum coverage, safe ZIP paths, invalid/oversized input rejection, real Android local file-picker save, cleared selection and catalog JSON round-trip/duplicate edits.
- 17 core controlled checks: synthetic unknown/missing-identifier rejection actions, saved contact/phone matching, unsaved numbers, ignoring hang-up/other packages, paused/queued-action behavior, selective missed-call cleanup, grace/recovery/simulated expiry, and bundled disclosures.

No real Google account was selected/authorized, no OAuth token was used and no People API request was made. The local Google tests injected synthetic cache fixtures directly; they do not demonstrate live Google access or automatic classification. Core tests exercised the real decision path with synthetic notifications/test-owned actions, not real Meet calls. Time boundaries were simulated through test timestamps; no device-clock change, reboot or 24-hour endurance run occurred.

Original preferences, including catalog/settings, were restored from the private backup and verified byte-for-byte before normal restart. Synthetic account data/Keystore keys, attachments/Downloads ZIPs, test cache files and test APK were removed. Notification access was restored; final UI confirmed protection ON, listener connected, status service running, contacts/notifications allowed, Shizuku authorized/running and Wi-Fi connected. Backups remain private and are not committed or included in releases.

## Still unverified

Google Cloud Android OAuth registration and People API enablement remain absent. Live authorization, download/pagination/error handling against Google, cloud-only matching, remote revocation, Google account removal, real refresh and actual 24-hour expiry remain unverified. Cloud document providers, process death/rotation, low storage, full retention boundaries, reboot recovery, live incoming calls and long-duration operation are not established by this run. These results are not a universal blocking or privacy/security certification.

## Repeating checks safely

Use a consented device, a verified APK/data backup and a plan to restore preferences. Stop/disallow the real notification listener temporarily to avoid concurrent changes, then restore its original access after testing. The Google cache suite refuses an existing connected-contact cache rather than replacing real connections. Do not use tests as a way to authorize Google accounts or fabricate evidence.

Build test APKs with `gradle --init-script tools/device-test-runner.gradle -PquietgateVerificationRunner=org.open.quietgate.GoogleContactsVerification assembleDebugAndroidTest` (quote the whole -P argument in PowerShell). Install the test APK and run its registered instrumentation component. Use the runner values org.open.quietgate.IncidentVerification and org.open.quietgate.DeviceVerification for the other suites. The original core suite can read an existing saved contact locally and changes temporary protection state, so a backup/restoration plan is required. Tests are not run by packaging.

This is a labeled supplement. Original 0.9-alpha APK, source ZIP and checksum-bearing release folder remain unchanged; new synthetic verification sources and this supplement are separate from that original snapshot. No new GitHub release or main-branch merge is performed by these tests.
