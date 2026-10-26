# QuietGate

QuietGate is an Android app that acts as a best-effort safeguard against spam and unwanted Google Meet video calls for the recipient. It watches Meet notifications, compares an exposed caller email address or phone number with contacts on the device, and attempts Meet's explicit **Decline** or **Reject** action when the caller is not matched.

QuietGate is free and open-source software under the MIT License. Active alpha development began on October 24, 2026. The project currently has two editions:

- **QuietGate FOSS** — device-contact matching only. It contains no Google sign-in, Google contact libraries, or Internet permission.
- **QuietGate Google** — includes optional, user-initiated Google Contacts authorization and refresh. Google services and their libraries have separate terms.

Both editions share the local protection logic, catalog, privacy disclosures, and user interface. They can be installed side by side with separate app data.

## What it does

- Monitors Google Meet notifications after the user grants Android Notification Access.
- Matches exposed caller emails or phone numbers against contacts available on the device.
- Attempts Meet's available Decline/Reject action for an unmatched caller.
- Provides a quiet-alert option, local rejected-call history, categories, notes, and JSON/ZIP export.
- Shows readiness for contacts, notification access, Wi-Fi, Shizuku, and the foreground status service.
- Uses a 24-hour grace period when the current protection requirements become unavailable.

## What it cannot guarantee

QuietGate is not a cellular call blocker and does not have a private Google Meet call-control API. Meet must expose a usable notification, caller identifier, and Decline/Reject action. A call may ring briefly; Android or Meet may delay, omit, or change the notification; and the app cannot independently verify that Meet ended the call. Do not rely on QuietGate as your only protection.

The app does not provide caller authentication, determine whether a caller is legitimate or malicious, access other apps' private data, or guarantee background survival. Device and manufacturer power-management policies can stop or delay work.

## Requirements

- Android 10 (API 29) or newer.
- Google Meet installed for the feature to be useful.
- Contacts access.
- Android Notification Access.
- Notification permission and foreground-service access where Android requires them.
- Shizuku installed, running, and authorized to QuietGate.
- Active Wi-Fi while protection is enabled. If Wi-Fi or authorized Shizuku becomes unavailable, the 24-hour grace policy starts.

Wi-Fi and Shizuku are current readiness requirements for fully enabled protection in both editions. They are not a promise that Meet will expose a controllable call action.

## Permissions and special access

Shared permissions/access include:

- `READ_CONTACTS` for local caller matching.
- `ACCESS_NETWORK_STATE` for Wi-Fi readiness.
- `RECEIVE_BOOT_COMPLETED` to restore the local lease after reboot.
- `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, and Android 14+ special-use foreground-service access for the visible status service.
- Android Notification Access to read Meet notifications and invoke supplied actions.
- Shizuku authorization to check readiness.

The Google edition additionally uses `INTERNET` for optional Google Contacts authorization and refresh. No microphone, audio recording, phone-state, call-log, location, or overlay permission is requested. Google OAuth is optional and is not used by the FOSS edition.

## Privacy and data

Caller matching stays local. QuietGate has no backend, telemetry, advertising, or runtime AI. Local catalogs can contain caller identifiers, timestamps, categories, and notes. JSON and incident ZIP exports are unencrypted and may contain personal data; delete exported copies separately. The Google edition stores its optional account cache in app-private storage and provides disconnect/delete controls.

Read [PRIVACY.md](PRIVACY.md), [PERMISSIONS.md](PERMISSIONS.md), and [FOSS.md](FOSS.md) before installing. See [AI-DISCLOSURE.md](AI-DISCLOSURE.md) for the transparent AI-assisted "vibe coding" disclosure.

## Installation and releases

Development APKs and release documentation are available on the [Releases page](https://github.com/gamefreak89/QuietGate/releases). These are alpha, development/debug-signed builds, not production-hardened or store-certified releases. Verify the published privacy-sanitized checksums before installing.

The source tree contains the Java/XML Android project, tests, build files, license notices, and release tooling. Build instructions are in the repository documentation. No signing keys, credentials, private contacts, device identifiers, or local SDK paths belong in source or release bundles.

## Feedback and responsible reports

Please use [GitHub Issues](https://github.com/gamefreak89/QuietGate/issues) for:

- reproducible bugs, with the app version, edition, Android version, device model, steps to reproduce, expected behavior, and actual behavior;
- feature requests and usability feedback;
- documentation corrections.

Do not post phone numbers, email addresses, call exports, OAuth tokens, screenshots containing private notifications, signing material, or other personal data in a public issue. For a security-sensitive vulnerability, do not publish exploit details immediately in an issue; use GitHub's private vulnerability-reporting option if enabled, or contact the maintainer privately through the repository account.

Issue reports are community feedback, not a promise that every request will be implemented. Please search existing issues first and keep reports focused.

## Development transparency

Both editions were developed through an AI-assisted conversational "vibe coding" workflow using OpenAI Codex. The maintainer directed the work, reviewed changes, made release/privacy decisions, and remains responsible for testing and publication. AI assistance is not a security audit, privacy certification, license determination, or correctness guarantee. No AI model runs inside QuietGate.

## License

MIT. See [LICENSE](LICENSE).

## Project documentation

- [AI assistance and vibe coding](AI-DISCLOSURE.md)
- [Privacy policy](PRIVACY.md)
- [Permissions](PERMISSIONS.md)
- [FOSS and dependency licensing](FOSS.md)
- [Changelog](CHANGELOG.md)
- [Release process](RELEASING.md)
- [Verification status](DEVICE-VERIFICATION.md)
