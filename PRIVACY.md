# Privacy policy

Applies to QuietGate 0.7-alpha. Policy revision: 2026-10-24.

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
