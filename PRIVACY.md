# Privacy policy

Applies to QuietGate 0.8-alpha. Policy revision: 2026-10-24.

## Incident report exports

Users can select retained records and optional existing files for a ZIP containing caller identities, categories, notes, imported/source labels, UTC and epoch timestamps, incident title/notes and attachment hashes. Attachments keep their original bytes and embedded metadata; original filenames are included. Up to 10 attachments, 10 MiB each / 25 MiB total. No new collection permissions, recording or location access are added. The file picker grants access to chosen files; a cloud provider may download selected attachments or upload the export under that provider's permissions. The ZIP is not encrypted. SHA-256 detects changes relative to its manifest and does not authenticate the contents. Keep an independent manifest copy.

Drafts remain in activity memory/saved state during normal recreation. A temporary ZIP is written to app-private cache and deleted on normal success or failure. Process interruption may leave that file until Android cache cleanup or manual app-cache clearing. Export work is not guaranteed to survive process termination. Clear catalog/history does not delete original attachments or exported files; delete those separately. Uninstalling removes app-private data. Selected records are re-read before export to honor clearing, edits and expiry.

## What stays on the phone

QuietGate reads notifications delivered through Android Notification Access and processes Google Meet call identifiers to compare them with the device Contacts provider. Android grants broad notification access; this implementation filters processing to Meet. It does not download cloud contacts or inspect other devices.

Local app-private storage contains protection settings, a saved grace deadline, up to 30 generic decision entries with timestamps, up to 128 rejection fingerprints, and the optional-to-clear call catalog. The catalog records Meet-exposed email addresses or phone numbers, attempt times, action status, categories, notes and whether records were imported. These identities are not authenticated. Caller identifiers may be unavailable or misleading.

The catalog retains the newest 500 records within 90 days and prunes on access/write; this is not a guaranteed background deletion schedule. Fingerprint matching expires after its narrow matching window, with stored entries pruned on new records. Salted hashes are pseudonymous, not anonymous. No recipient Google account or device hardware identifier is added to catalog exports.

## Transmission and sharing

The app requests no INTERNET permission and has no automatic upload, advertising, analytics or remote AI feature. JSON export is initiated by the user through Android's document picker. Exported files include caller identities, timestamps, categories and notes. A selected cloud document provider can upload the file using that provider's permissions. Choose local storage for local archives. Imported files are treated as unverified claims and never alter contact matching or blocking rules.

## Control and deletion

Use Catalog > Clear to remove the app's catalog. Clear local history & fingerprints also removes the catalog, decision entries and rejection fingerprints. Exported copies must be deleted separately. Uninstalling normally removes app-private storage. Android backup is disabled in the manifest; this does not promise protection against a rooted device, OS-level access or externally copied exports.

## Access not requested

No microphone, audio recording, system call log, phone state, precise location, overlay or internet permission is requested. The app does not record conversations. Shizuku authorization is checked for readiness; it does not provide a Meet interception API.

## Limitations

Meet notifications and Android background behavior can change. A call may ring before a decline action is invoked. The app cannot independently confirm that a decline action ended a call. Category labels are user opinions, not verified fraud findings. No security certification or universal blocking guarantee is claimed.

## Reporting a privacy issue

Do not post real contacts, call catalogs or private logs in public issues. Use the repository's private reporting route if available; otherwise request a private contact channel without including sensitive data. No maintainer personal email is embedded in this project.
