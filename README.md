# QuietGate 0.7-alpha

QuietGate is a separate Android app that watches Google Meet notifications and checks exposed caller emails or phone numbers against contacts on this device. An unknown caller is declined only when Meet provides an explicit `Decline` or `Reject` notification action.

## What is included in 0.7-alpha

- Android development/debug APK with Meet notification-based contact matching, best-effort decline actions, status notification and 24-hour grace countdown.
- Local rejected-call catalog with JSON import/export, categories, filters and notes (500 newest records within 90 days).
- Complete Java/XML source, build configuration, instrumentation source and release packaging helper.
- Privacy policy, permission explanations, MIT/third-party licenses, AI-assistance disclosure, changelog and verification limitations.
- GitHub release assets: APK, source.zip, complete release-folder ZIP, checksums and a standalone release README/disclosure.

### Permissions at a glance

Contacts access checks caller identities. Notification Access reads Meet call notices and invokes available actions; Android grants broad notification access even though the code filters to Meet. Notification permission and foreground-service declarations support the status notice. Network-state access checks Wi-Fi; boot access requests restoration; Shizuku authorization checks readiness. Import/export uses files explicitly selected through Android's picker. See [PERMISSIONS.md](PERMISSIONS.md) for the full breakdown.

No INTERNET, microphone, conversation recording, Android call-log, phone-state, location or overlay permission is requested. Catalog exports contain caller identities and notes; selecting a cloud document provider may upload through that provider. See [PRIVACY.md](PRIVACY.md).

**Alpha disclosure:** rejection can occur after ringing; caller identity and call-ending outcomes are not authenticated or independently confirmed. New catalog import/export flows have not yet received functional testing. No security/store/F-Droid certification is claimed.

## Shizuku and root requirements

> **QuietGate currently requires Shizuku to be running and authorized when protection is enabled. Root is optional; root alone does not replace Shizuku authorization.**

- **Without root:** start Shizuku using a supported debugging method and authorize QuietGate. Shizuku generally needs restarting after a device reboot.
- **With root:** Shizuku can be started using root, but QuietGate still checks its running/authorized state. Rooted configurations have not been verified in our device tests.
- **Wi-Fi:** the current build also requires active Wi-Fi to enable protection. Losing either Wi-Fi or authorized Shizuku starts the 24-hour grace countdown, including on rooted devices.
- **Call blocking remains best effort:** neither root nor Shizuku guarantees interception before ringing or successful rejection. QuietGate uses the explicit Decline/Reject action exposed by Meet.

See the [official Shizuku setup guide](https://shizuku.rikka.app/guide/setup/) for startup methods.

## Background behavior

The Android notification-listener service is system-bound after the user grants Notification Access. It can receive notification events after the launcher activity is swiped out of Recents. While protection is enabled, QuietGate also shows an ongoing low-priority foreground-service notification. It reports listener connection, Wi-Fi/Shizuku readiness, and the grace countdown. Android 13+ notification permission is required for the notice to appear normally in the notification drawer. Android or a phone maker can still restrict or stop background components; the foreground service improves visibility but cannot guarantee survival.

## Shizuku and the quiet lease

QuietGate integrates the official Shizuku API for authorization status. Shizuku is not used to control Google Meet. It does not expose a Meet call-screening API, unlock other apps' private data, or make a decline action more reliable. The actual rejection attempt remains a notification action supplied by Meet.

When enabled, protection requires the user to grant contacts access, Notification Access, and Shizuku authorization. A 24-hour grace timer starts whenever either Wi-Fi or the authorized Shizuku service becomes unavailable. Restoring both before the timer expires resets it. If either remains unavailable for 24 hours, protection switches off. The timer state is local and survives reboot; its alarm is quiet. Android may defer alarms while the device is idle, so the app also checks and expires the lease on app resume and before handling each Meet notification.

Contact matching is only against the Android contact provider on this phone. That provider can aggregate device, SIM, and synced-account entries; this version checks the address book available on the phone without filtering by account type. It does not search Google accounts or other devices independently. Emails match exactly apart from case; phone numbers match only when their digit sequences are identical after punctuation is removed. Names alone are not accepted as a match.

QuietGate rejects calls only with Meet's explicit `Decline` or `Reject` action. Optional quiet mode uses `cancelNotification()` separately, only for a non-ongoing `missed_call` notification associated with a recent rejection attempt. It first matches the notification key or tag (15-minute window). Meet may change both, so an exact single-caller email/phone title can also match a salted caller fingerprint for two minutes. A new incoming call clears the prior identity-only window for that caller before its decision, preserving later calls that are allowed or cannot be rejected. Active calls, group summaries, messages, and unmatched missed-call alerts remain visible. The bounded local store keeps at most 128 hashed fingerprints; raw identities are not stored there, and expired entries are pruned on new records. Existing unrelated notifications are not bulk-cleared. Android may briefly display or sound an alert before the listener can dismiss it. Meet's internal history remains unchanged.

The contact rule applies to all receiving Google accounts handled by Meet in this Android profile. No destination-account filter or Google account permission is used. The app checks the device Contacts provider, including synced entries available there; it does not query cloud address books or independently cover a separate work profile.

## Limitations

This remains best effort: Android has no public API for screening an incoming Meet call before it rings. Meet may post no usable notification, omit caller identifiers, or omit a decline/reject action. The call may ring first. An invoked action is logged as unconfirmed because the app cannot verify that Meet ended the call. The first build recognizes English `Decline` and `Reject` action labels.

## Permissions

- `READ_CONTACTS`: compares an exposed caller email/number with this device's contacts.
- Android Notification Access: receives Meet notifications and invokes a supplied decline action.
- `ACCESS_NETWORK_STATE`: checks whether Wi-Fi is the current active network for the grace timer.
- `RECEIVE_BOOT_COMPLETED`: restores the local grace timer after reboot.
- Shizuku authorization: checks that Shizuku/Sui is running and has authorized QuietGate.

No internet, microphone, audio capture, call log, phone state, location, or overlay permission is requested.

`POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, and Android 14+'s `FOREGROUND_SERVICE_SPECIAL_USE` support the user-visible protection status. The foreground notification is deliberately worded as best-effort: it does not prove that a particular Meet call was intercepted or rejected.

## Build

The accompanying `QuietGate-source.zip` contains the full application sources, original vector assets, tests, build scripts and license notices. It excludes local SDK paths, generated caches, private runtime data and signing keys. Install JDK 17, Gradle 8.9 and Android SDK/build-tools 35. Set `ANDROID_HOME` to your own SDK installation and run `gradle assembleDebug`; the debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Maven Central and Google's Maven repository supply dependencies. A development build is not a store-certified release.

The app and notification use the same saved 24-hour deadline. The in-app timer updates every second; the shade uses Android's native countdown chronometer. Either missing requirement starts grace, and both must recover before the deadline to reset it. After expiry, reconnect both and manually enable protection. A late reconnect cannot bypass the expired deadline.

The About, privacy, permissions & licenses screen contains the purpose, access explanations, limitations and complete license texts. Clear local history & fingerprints erases the local decision/fingerprint store without editing contacts or changing protection. Source distribution under MIT permits use, inspection, modification and redistribution; no F-Droid, FSF or security certification is claimed.

## License

MIT. See `LICENSE`.

Rejected-call catalog (0.6-alpha): stores Meet-exposed caller identities, attempt timestamps, and decline sent/failed status locally. These identities are not authenticated; action success is unconfirmed. Up to 500 entries from 90 days are retained, pruned on catalog access or write. No past identities can be reconstructed. JSON export uses the Android document picker and may contain personal data. Catalog Clear and Clear local history remove the app copy, not exports. No added permissions or automatic uploads. Google Other contacts may need saving as regular device contacts.

Catalog import/export (0.7-alpha): use Rejected-call catalog to import JSON, export JSON, filter categories, or tap an attempt to edit its category and notes. Categories: Uncategorized, Suspected spam, Suspected scam, Business / service, Known caller, Other. Labels are user assessments, never verified findings or allow/block rules. Imports merge rather than replace; existing edits win for duplicates identified by record ID or time/identity/action. Imports are labeled and do not affect contacts, rejection fingerprints or protection. Current versioned JSON and legacy QuietGate array exports are accepted, up to 2 MB / 5,000 input records. Invalid, expired and future-dated records are skipped. The local store retains only 500 newest records within 90 days; export for longer archival storage. Export is the full retained catalog, including all categories, caller identities and notes. Imports are untrusted claims, not proof of received calls. Android's document picker lets you choose local storage or a provider; selecting cloud storage may upload via that provider. No automatic sharing or app internet permission.

## Project and release documentation

- [Privacy policy](PRIVACY.md)
- [Permissions](PERMISSIONS.md)
- [FOSS and dependency licensing](FOSS.md)
- [Languages and AI assistance](AI-DISCLOSURE.md)
- [Changelog](CHANGELOG.md)
- [Verification status](DEVICE-VERIFICATION.md)
- [Create a versioned local release](RELEASING.md)
- [GitHub setup](GITHUB-SETUP.md)

Primary language: Java. No runtime AI model or remote AI service is included.
