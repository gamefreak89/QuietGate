# QuietGate 0.8-alpha — release README and disclosure

Local development build prepared on 2026-10-24. GitHub publication is separate from local packaging; the existing 0.7-alpha release is unchanged.

## Included files

- QuietGate-0.8-alpha.apk: development/debug-signed Android application, versionCode 9.
- source.zip: clean sources, build files, documentation, existing instrumentation sources and licenses.
- source/: extracted source snapshot.
- SHA256SUMS.txt: SHA-256 hashes of packaged files, excluding the manifest itself.
- RELEASE-README.md: this version-specific disclosure, included in local packaging and intended as a standalone asset if published to GitHub.

## Permissions and setup

READ_CONTACTS compares Meet-exposed identities with device contacts. Android Notification Access receives Meet notifications and invokes explicit Decline/Reject actions; Android grants broad notification access although code filters to Meet. POST_NOTIFICATIONS, FOREGROUND_SERVICE and FOREGROUND_SERVICE_SPECIAL_USE support the visible status notice. ACCESS_NETWORK_STATE checks Wi-Fi; RECEIVE_BOOT_COMPLETED requests timer restoration. The notification listener and Shizuku provider use system permission guards, which do not grant those system privileges to QuietGate.

Running authorized Shizuku and active Wi-Fi are required to enable protection. Either missing starts the 24-hour grace timer; both must recover before expiry. Root is optional and unverified, and does not replace Shizuku authorization. Shizuku does not strengthen rejection or unlock private Meet data. Android's document picker grants access to chosen import/export files and incident attachments. No new permissions in 0.8-alpha: no INTERNET, microphone, recording, system call-log, phone-state, location or overlay permission.

## Privacy and local data

Matching, preferences, generic decisions, short-lived salted rejection fingerprints and the catalog stay local. The catalog keeps 500 newest records within 90 days, pruned on access/write. It stores identities exposed by Meet, timestamps, action status, categories, notes and imported labels. Identities and claims are unverified. Import never changes protection rules. No automatic uploads, advertising, analytics or remote AI.

Incident ZIPs contain only selected retained records, incident title/notes, UTC and epoch timestamps, optional original attachment bytes/names/metadata, readable/JSON reports and checksums. Up to 10 attachments, 10 MiB per file / 25 MiB total. The ZIP is unencrypted. A chosen cloud provider may download attachments or upload exports. Drafts survive normal activity recreation; background export is not guaranteed to survive process termination. Temporary ZIPs in private cache are deleted on normal completion/failure; interruption may leave files until Android or manual cache cleanup. Clear history/catalog removes the app records, not original attachments or exported copies. Uninstall removes app-private data. No real call catalogs, contacts, credentials, private device data, SDK paths or signing keys are packaged.

## FOSS and licenses

QuietGate is MIT licensed. Shizuku libraries are MIT; AndroidX Annotation is Apache-2.0. Full notices are included. Google Meet is an external proprietary service. The repository remains private. No store, F-Droid, FSF or independent security certification is claimed. Official builds remain free; MIT permits separate paid forks under its terms.

## Languages and AI assistance

Java app code, XML resources, Groovy Gradle build files, Python release helper and Markdown documentation. Development was assisted by OpenAI Codex; exact historical model snapshots are unavailable. No runtime LLM or remote AI service is included.

## Changes in this release

Added a catalog entry point for incident export, individual record selection, title and incident notes, optional attachment selection/removal, review before saving, streamed bounded attachment copying, UTC timestamps, imported/source labels, readable/JSON reports and SHA-256 manifest. Private staging completes before destination writing. Errors do not claim success; partial destination deletion is attempted and users are told to remove leftovers. Contact matching, rejection, grace requirements and catalog JSON import/export are unchanged.

## Verification and limitations

See DEVICE-VERIFICATION.md for actual build/static-analysis results. Device functional checks of the incident flow, lifecycle interruptions, file providers and newest catalog flows remain pending; no new test suite or device installation is claimed. Earlier controlled/live-call results apply only to the previously tested core behavior. Calls may ring first, identity is not authenticated and rejection outcomes cannot be independently confirmed. Root and endurance are unverified. Hashes detect changes relative to the manifest; they do not prove authenticity, independent time, legal admissibility or independently verified chain of custody. No caller IP/location is obtained. The debug signing type is for development and is not store certification. MIT includes no warranty.
