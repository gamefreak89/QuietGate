# QuietGate 0.7-alpha — release README and disclosure

Documentation supplement dated 2026-10-24. The previously published APK and source archives are unchanged.

## Included files

- QuietGate-0.7-alpha.apk: development/debug-signed Android app.
- source.zip: clean source snapshot with build files and license notices.
- QuietGate-release-0.7-alpha.zip: full versioned folder with extracted source, APK, documentation and metadata.
- SHA256SUMS.txt: checksums for the original versioned folder contents.
- RELEASE-README.md: this standalone documentation supplement; it is not covered by the original checksum file.

## Permissions and setup

- READ_CONTACTS: compare Meet-exposed caller email/number with regular device Contacts.
- Android Notification Access: receive notifications, filter to Meet and use its supplied Decline/Reject action. Android grants broad access even though the code filters to Meet.
- POST_NOTIFICATIONS, FOREGROUND_SERVICE and FOREGROUND_SERVICE_SPECIAL_USE: visible status service and countdown.
- ACCESS_NETWORK_STATE: check whether active connectivity is Wi-Fi.
- RECEIVE_BOOT_COMPLETED: request background/timer restoration after reboot, subject to Android policy.
- Shizuku authorization: readiness check, not a stronger call-control API. Running authorized Shizuku and Wi-Fi are required to enable protection. Either missing starts the 24-hour grace period; both must recover before expiry to reset it. Root is optional and untested, and does not replace Shizuku authorization.
- Android document picker: access only selected import/export documents without broad storage permission.

The system binds the listener using BIND_NOTIFICATION_LISTENER_SERVICE. The Shizuku provider's system permission guard does not grant QuietGate that system privilege. No INTERNET, microphone, recording, system call-log, phone-state, location or overlay permission is requested.

## Privacy and local data

Matching, settings and history stay on the device. The catalog stores Meet-exposed identities, timestamps, action results, categories and notes, with 500 newest records within 90 days; pruning occurs on read/write. Generic decisions and short-lived salted fingerprints are also retained locally. Identities and hashes are not anonymous or independently authenticated. Clear catalog/history removes the app copy; exported copies must be deleted separately. No automatic uploads, advertising or analytics. A chosen cloud file provider can upload exported JSON. No real contacts, call exports, signing keys or private device data are included in this release.

## FOSS and licenses

QuietGate source is MIT licensed. Shizuku libraries are MIT; AndroidX Annotation is Apache-2.0. Full notices are included. Google Meet is an external proprietary service; a private repository does not make source publicly available. No F-Droid, FSF, store or independent security certification is claimed.

## Languages and AI assistance

Java application code, XML resources, Groovy Gradle build files, Python release helper and Markdown documentation. Development was assisted by OpenAI Codex; exact historical model snapshots are unavailable. Earlier supplied specifications were described as Gemini-assisted. No LLM or remote AI executes inside QuietGate.

## Changes in this release

JSON merge import/export, duplicate detection, imported-record labels, category filtering and editable notes. Local retention remains bounded. Imported records and category opinions never change contact matching or rejection rules.

## Verification and limitations

0.7-alpha compiled and installed; the newest catalog import/export flows have not received functional testing. Earlier core behavior passed 17 controlled tests plus saved/unsaved live-call checks in tested setups. A call may ring before rejection. Sending a Decline action is not proof the call ended; alert disappearance is not proof of rejection. Android/Meet versions, language, profiles and battery policies affect behavior. Root and long-duration endurance remain unverified. MIT includes a no-warranty disclaimer. Checksums establish integrity, not safety or authenticity.
