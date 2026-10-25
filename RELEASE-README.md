# QuietGate 0.9-alpha — release README and disclosure

Development build prepared 2026-10-24. Optional Google saved-contact client code is implemented; live activation depends on external Google Cloud setup. The phone's verified 0.8-alpha build is not replaced by packaging this version.

## Included files

- QuietGate-0.9-alpha.apk: development/debug-signed Android application, versionCode 10.
- source.zip and source/: app sources, resources, existing instrumentation sources, helper and documentation.
- SHA256SUMS.txt: hashes of packaged files except itself.
- RELEASE-README.md: version-specific standalone disclosure for this local build and any later GitHub release assets.
- GOOGLE-CONTACTS-SETUP.md: Android OAuth/People API setup prerequisites, public package/certificate metadata and verification checklist. No client secret or private key is included.

## Permissions and setup

READ_CONTACTS matches device/synced contacts. Notification Access is broad but app processing filters to Meet and its exposed Decline/Reject actions. POST_NOTIFICATIONS and foreground-service declarations support the visible status notice. ACCESS_NETWORK_STATE checks Wi-Fi; RECEIVE_BOOT_COMPLETED requests restoration. Running authorized Shizuku and active Wi-Fi remain required to enable protection; either missing starts the 24-hour grace period. Root is optional/unverified and does not replace Shizuku or strengthen rejection. Component system permission guards are not grants of those privileges to QuietGate. Document pickers grant chosen import/export/attachment access.

New INTERNET and Google Play services Auth 22.0.0 support optional per-account Google authorization and manual saved-contact refresh. Users choose each account with Android's chooser; no GET_ACCOUNTS or silent enumeration. Requested OAuth scope: contacts.readonly. Google permits reading saved contacts; actual fetches request only email/phone fields from CONTACT sources. No Other contacts, Gmail, directory, names, contacts-write or profile scopes are requested. No microphone, recording, location, call-log, phone-state or overlay permission. Google Cloud Android OAuth registration, consent/audience settings and People API enablement are prerequisites; this build does not claim them configured. Wider availability can require Google review. See setup guide.

## Privacy and local data

No QuietGate backend, ads, analytics or remote AI. Google receives explicit authorization/manual refresh requests; no caller identifier is sent for matching. Google Play services manages tokens; QuietGate uses an access token only in memory and writes no tokens/server auth codes to app files or exports. Only chosen account labels, generations/status/last-success timestamps and keyed contact fingerprints are stored, AES-GCM encrypted under Android Keystore keys in app-private no-backup storage. No raw contact list or names are persisted. Limits: ten accounts, 20,000 identifier fingerprints/account, bounded paginated responses. Partial/failed/oversized lists never become active. Refresh is manual and pauses matching until completion; snapshots stop allowing matches after 24 hours using the device clock. Offline external edits/revocations/removal may be reflected only on refresh or expiry. Stale encrypted entries remain until refresh/disconnect/deletion.

Disconnect deletes local account matches before requesting Google revocation and reports failure with manual instructions. Google revocation affects all app grants for that account. Delete-all erases local account data and keys; remaining grants require separate management. Device Google contacts are not deleted. Clear catalog/history does not disconnect accounts. Uninstall removes private app data but may not revoke Google grants. Compromised/rooted environments or deliberate privileged extraction are outside these safeguards; custom debug backups may copy encrypted bytes, but Keystore keys are not exported by QuietGate.

Core catalog keeps 500 newest attempts within 90 days, pruned on access/write, with unverified caller identities, time, action, labels/notes and imported status. Generic decisions and temporary pseudonymous fingerprints stay local. User-started catalog JSON and selected incident ZIPs can include actual caller identities/notes, timestamps and optional original attachments/metadata. They do not include connected-account labels/caches/tokens. Incident limit: ten files, 10 MiB each / 25 MiB total. ZIPs are unencrypted; document providers can upload/download selected data. Private temporary ZIPs are deleted on normal completion/failure; process interruption can leave cache files. Exports/original attachments must be deleted separately. No personal backup files, contacts, catalog data, credentials, signing keys, device identifiers or SDK paths are packaged.

## FOSS and licenses

QuietGate's original source is MIT; Shizuku libraries are MIT; AndroidX/open-source transitives have their own notices. Google Play services Auth/transitive Google libraries are under Google's Android SDK License; full bundled third-party notices are included and accessible in the app. The complete dependency stack is not entirely FOSS. Google Meet/Google services and Shizuku are external requirements. See FOSS.md and asset notices. Official builds remain free; MIT permits separate forks under its terms. No F-Droid, FSF, store or independent security certification is claimed.

## Languages and AI assistance

Java app/instrumentation code, XML resources, Groovy Gradle configuration, Python packaging helper and Markdown docs. Developed with OpenAI Codex assistance; exact historical model snapshots are not claimed. No runtime LLM or remote AI is included.

## Changes in this release

Optional Google saved-contact account chooser, read-only authorization, manual paginated HTTPS refresh, encrypted local keyed match snapshots, expiry/failure handling, local-cache deletion and remote revocation request. Device contact matching can fall back to a completed fresh connected snapshot, without network access during incoming calls. Other contacts remains unimplemented. Added INTERNET, Google Auth dependency, license notices and setup/privacy documentation. Incident and catalog data/export retention rules remain unchanged.

## Verification and limitations

Google client code compiled and Android lint passed; see DEVICE-VERIFICATION.md for final artifact checks. Live Google authorization, contact downloading, remote revocation, cloud matching, expiry and lifecycle/device behavior are not verified because the Cloud OAuth/API setup is not present. No new cloud test suite or device installation is claimed. Earlier 0.8-alpha device verification covers the incident/catalog build only, not this Google feature. Root/endurance/cross-device behavior remain unverified. Calls may ring first, exposed identifiers/contact matches are not authentication, and rejection outcomes remain unconfirmed. Hashes are integrity comparisons, not proof of authentic incident claims or independent chain of custody. Debug signing is not store certification; MIT has no warranty.
