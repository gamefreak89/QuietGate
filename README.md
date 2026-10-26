# QuietGate 0.13-alpha

Distribution privacy review: see [the cleanup supplement](PRIVACY-CLEANUP.md) for audit scope, sanitized download names, checksums, and retention limits.

Updated enable-protection summaries explain purpose, limits, current setup requirements, and edition-specific optional access. See [the requirements review](REQUIREMENTS-REVIEW.md), [current changelog](CHANGELOG-README.md), and [verification](DEVICE-VERIFICATION.md).

The user reported a successful 0.12-alpha FOSS live-call test. This is user-reported evidence; no independently observed call result is claimed.

## Historical 0.12-alpha notes

# QuietGate 0.12-alpha

Later verification update: QuietGate FOSS installed alongside the existing edition and passed permission setup, foreground-status-service, and UI/document checks. The original edition was restored active; FOSS is left installed but paused with notification access disabled. See [the standalone device-verification supplement](DEVICE-VERIFICATION-0.12-alpha-SUPPLEMENT.md). This does not establish live-call behavior. Earlier no-device statements describe the initial build stage, and existing release archives are preserved.

## Choose an edition

**QuietGate (Google edition)** keeps the existing app ID and optional Google saved-contact connection/refresh. **QuietGate FOSS** uses a separate app ID, reads device contacts, and contains no Google client library, account integration, refresh job, or Internet permission. Both keep the organized About hub, quiet alerts, local catalog, JSON import/export, incident reports, and MIT-licensed project code.

Both can be installed side by side, but each needs its own permissions and Shizuku authorization. Enable protection and notification access in only the edition you intend to use to avoid handling the same call twice. The Google edition updates the existing package; the FOSS edition starts with separate private data.

See [the edition comparison and build commands](EDITIONS.md), [F-Droid preparation](F-DROID.md), and [the current changelog](CHANGELOG-README.md). A phone is unnecessary for compilation and APK inspection. No 0.12-alpha device testing, F-Droid submission, public-repository conversion, or security certification is claimed.

The FOSS edition's bundled code/runtime libraries are MIT/Apache-2.0. Google Meet remains a separate proprietary app/service; this does not make the entire phone or ecosystem FOSS. The repository remains private; official F-Droid submission needs public, up-to-date source and review. The sections below describe retained earlier milestones.

## A cleaner interface

The dashboard now contains protection status and controls. Open **About, privacy, permissions & licenses** for separate pages covering purpose, limitations, access, privacy, background operation, contacts, Google refresh, catalog files, incident reports, free and open-source licensing, complete legal notices, the changelog, and recent decisions. Long paragraphs no longer fill the bottom of the dashboard.

Version 0.11-alpha also fixes corrupted punctuation, respects system bars and display cutouts, supports wrapping button labels, and confirms local-history deletion. The source review tightened notification-access detection and rechecks queued Meet decline actions before invoking them. See [the release changelog](CHANGELOG-README.md) and [scoped security review](SECURITY-REVIEW.md).

QuietGate's own code and original assets are MIT-licensed. Release source can be inspected, modified, and redistributed under those terms. Google integration dependencies retain separate terms, so this build is not entirely FOSS. The GitHub repository was private when inspected on 2026-10-26; public accessibility must not be inferred from its license.

The release bundle includes a standalone changelog README and a documentation ZIP beside the APK. GitHub release assets are a flat list; a documentation folder is available inside the ZIP. Verification status and signing details are in RELEASE-README.md and DEVICE-VERIFICATION.md. The following feature sections describe earlier milestones.

## Automatic saved-contact refresh (0.10-alpha)

Open **Connected Google saved contacts (optional)** and enable **Automatic refresh every ~12 hours** for each already connected account. The default policy is **Unmetered Wi-Fi only**; turning it off permits any connected network, including mobile data. Manual Refresh remains available. The controls display last successful refresh, last attempt, cached identifier count and background status. Update refresh status reloads that display without making a network request.

Native Android JobScheduler registers one persisted periodic job per opted-in account (12 hours, 1-hour flex), plus optional initial/background-now work. Jobs require non-low battery and suitable networking. Wi-Fi-only additionally checks that the current network is Wi-Fi and unmetered before making requests. Android can delay jobs; force-stop, battery controls, offline periods, consent expiry or quotas can prevent refresh. No exact-time or continuous-coverage guarantee. Existing snapshots stop allowing matches 24 hours after success.

Background refresh reuses the previously granted read-only scope and never launches or approves Google consent. A transient error preserves a still-fresh snapshot and retries with exponential backoff (initial 15 minutes), but never extends its 24-hour lifetime. If renewed consent is needed, that account is paused, its automatic work is canceled, and a generic notification directs the user to reconnect. Manual refresh retains its previous pause-on-failure behavior. Disconnect/delete cancels jobs; disabling auto invalidates in-flight background commits without deleting an otherwise fresh snapshot.

Auto options, stable opaque references/job IDs, last-attempt time and status are encrypted with the account cache. Android's persisted job extras contain only opaque references, not account emails/contact values/tokens. No additional SDK or Android permission is added; existing INTERNET/ACCESS_NETWORK_STATE/RECEIVE_BOOT_COMPLETED support the feature, and BIND_JOB_SERVICE protects its service. There is no QuietGate backend or caller-identifier upload. Automatic refresh is opt-in; no new Google accounts or Other contacts are connected automatically.

This stage is local: back up, install and verify first. GitHub/source/release publication is deferred until the user's later direction. Verification results and limits are documented separately; historical sections below refer to the earlier versions.

## Optional connected Google saved contacts (0.9-alpha)

Open **Connected Google saved contacts (optional)**. Choose each account through Android's account picker, approve Google's read-only saved-contact scope, and refresh its saved emails/phone numbers. This requires a configured Google Cloud project: see [Google contacts setup](GOOGLE-CONTACTS-SETUP.md). No Cloud project, OAuth registration or successful live authorization is claimed by this build.

Google Other contacts, directories, Gmail and contact names are not requested. The scope permits reading saved contacts; actual People API requests limit fields to email addresses and phone numbers and sources to CONTACT. No caller identifier is sent to Google. Matching uses local keyed fingerprints in an AES-GCM encrypted, app-private no-backup file; Android Keystore protects its keys. Tokens are handled by Google Play services, used in memory for refresh, and never written to QuietGate files or exports. There is no QuietGate backend.

Refresh is manual and pauses the account's cloud matching until the full bounded fetch succeeds. A snapshot allows matches for 24 hours using the device clock; expired, failed or pending snapshots are not used. Google-side edits/revocation/account removal are not immediately visible offline. Refresh or expiry updates their effect. Up to 10 chosen accounts and 20,000 unique identifier fingerprints per account; incomplete/oversized fetches never activate partial lists. Disconnect deletes the local account cache and requests Google revocation; failures show how to revoke manually. Delete-all erases local account data and encryption keys, with Google grants managed separately. Device contacts and their existing matching remain available.

**INTERNET is new in this version**, for Google authorization/manual refresh. No analytics, ads, automatic contact upload or cloud matching service is added. Connected-account labels and contact caches are excluded from catalog/incident exports; actual call records can still include caller identities. The included Google Play services library uses Google's SDK terms, so the complete dependency stack is not entirely FOSS. The app source remains MIT. Live Google authorization, download and revocation await external OAuth/API setup and verification.

## Incident reports (0.8-alpha)

Open **Rejected-call catalog > Create incident report (ZIP)**. Select individual retained attempts, add a title and incident notes, optionally select up to 10 existing attachments, review the contents, and choose a destination through Android's document picker. No new permissions are requested.

The ZIP includes a readable `report.txt`, structured `report.json`, original attachment bytes under numbered `attachments/NN.bin` paths, `README.txt`, and `SHA256SUMS.txt`. Original filenames are listed in both reports. UTC timestamps use the device clock; original epoch milliseconds and imported-record labels are preserved. Attachments belong to the report as a whole and are limited to 10 MiB each / 25 MiB total. Reports contain only selected retained records, including their category and notes. Catalog edits/expiry/clearing are resolved again before export starts.

Hashes detect subsequent changes relative to a retained manifest; they do not authenticate identities, prove a recording's origin, verify timestamps or establish legal admissibility or an independently verified chain of custody. The app obtains no caller IP address or location. The ZIP is unencrypted and may contain personal data and embedded attachment metadata. Cloud document providers may upload selected files. Clear catalog does not delete exports or original attachments. A private temporary ZIP is removed on normal completion/failure; interrupted processes can leave a cache file until Android clears it or the user clears app cache. Draft selection/notes survive normal activity recreation, but export work is not guaranteed to survive process termination. Device functional verification is pending.

QuietGate is a separate Android app that watches Google Meet notifications and checks exposed caller emails or phone numbers against contacts on this device. An unknown caller is declined only when Meet provides an explicit `Decline` or `Reject` notification action.

## Originality and free distribution

QuietGate is an independently developed project maintained in **[gamefreak89/QuietGate](https://github.com/gamefreak89/QuietGate)**. Its development history, source and AI-assistance disclosure are documented here. “Original” identifies this project's origin; it does not claim that every idea is unique or that third-party dependencies were written by this project. QuietGate is not an official Google or Shizuku product.

**Official QuietGate builds are free. No purchase, subscription, activation fee or payment is required to use the official app.**

If someone offers a reproduction or repackaged copy for money while claiming it is the official QuietGate, **do not pay for it or install it**. Use only downloads linked from this repository's [official Releases page](https://github.com/gamefreak89/QuietGate/releases). Avoid unverified APKs, payment-to-unlock offers and copies with unclear provenance or modifications. This repository is currently private, so authorized testers need access to view its releases.

**License clarification:** QuietGate uses the MIT license, which permits modification, redistribution and sale when its terms are followed. A paid fork is not automatically unauthorized or malicious. Such forks are separate from the free official build; this notice is a distribution and impersonation warning, not a prohibition on rights granted by [LICENSE](LICENSE).

## What is included in 0.7-alpha

- Android development/debug APK with Meet notification-based contact matching, best-effort decline actions, status notification and 24-hour grace countdown.
- Local rejected-call catalog with JSON import/export, categories, filters and notes (500 newest records within 90 days).
- Complete Java/XML source, build configuration, instrumentation source and release packaging helper.
- Privacy policy, permission explanations, MIT/third-party licenses, AI-assistance disclosure, changelog and verification limitations.
- GitHub release assets: APK, source.zip, complete release-folder ZIP, checksums and a standalone release README/disclosure.

### Permissions at a glance

Contacts access checks caller identities. Notification Access reads Meet call notices and invokes available actions; Android grants broad notification access even though the code filters to Meet. Notification permission and foreground-service declarations support the status notice. Network-state access checks Wi-Fi; boot access requests restoration; Shizuku authorization checks readiness. Import/export uses files explicitly selected through Android's picker. See [PERMISSIONS.md](PERMISSIONS.md) for the full breakdown.

INTERNET supports optional Google authorization/manual saved-contact refresh. No microphone, conversation recording, Android call-log, phone-state, location or overlay permission is requested. Catalog exports contain caller identities and notes; selecting a cloud document provider may upload through that provider. See [PRIVACY.md](PRIVACY.md).

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

Device contact matching uses Android's contact provider, which can aggregate device, SIM and synced-account entries without an account filter. Optionally, fresh local snapshots from individually connected Google accounts add saved-contact matches. Emails match ignoring case; phone matches require identical digit sequences after supported punctuation is removed. No suffix or name-only matches are accepted; no email-to-phone identity link is inferred.

QuietGate rejects calls only with Meet's explicit `Decline` or `Reject` action. Optional quiet mode uses `cancelNotification()` separately, only for a non-ongoing `missed_call` notification associated with a recent rejection attempt. It first matches the notification key or tag (15-minute window). Meet may change both, so an exact single-caller email/phone title can also match a salted caller fingerprint for two minutes. A new incoming call clears the prior identity-only window for that caller before its decision, preserving later calls that are allowed or cannot be rejected. Active calls, group summaries, messages, and unmatched missed-call alerts remain visible. The bounded local store keeps at most 128 hashed fingerprints; raw identities are not stored there, and expired entries are pruned on new records. Existing unrelated notifications are not bulk-cleared. Android may briefly display or sound an alert before the listener can dismiss it. Meet's internal history remains unchanged.

The contact rule applies to all receiving Google accounts handled by Meet in this Android profile. No destination-account filter is used. Cloud saved-contact access is optional and separately authorized per chosen account. Separate work profiles are not independently covered by this installation.

## Limitations

This remains best effort: Android has no public API for screening an incoming Meet call before it rings. Meet may post no usable notification, omit caller identifiers, or omit a decline/reject action. The call may ring first. An invoked action is logged as unconfirmed because the app cannot verify that Meet ended the call. The first build recognizes English `Decline` and `Reject` action labels.

## Permissions

- `READ_CONTACTS`: compares an exposed caller email/number with this device's contacts.
- Android Notification Access: receives Meet notifications and invokes a supplied decline action.
- `ACCESS_NETWORK_STATE`: checks whether Wi-Fi is the current active network for the grace timer.
- `RECEIVE_BOOT_COMPLETED`: restores the local grace timer after reboot.
- Shizuku authorization: checks that Shizuku/Sui is running and has authorized QuietGate.

INTERNET supports optional Google contact authorization/refresh. No microphone, audio capture, call log, phone state, location or overlay permission is requested.

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
