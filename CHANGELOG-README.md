# QuietGate 0.12-alpha changelog

## Two editions

The Google edition retains the existing app ID and optional connected Google contacts. QuietGate FOSS uses a separate app ID and device contacts only. Its compiled APK excludes Google libraries, account/refresh code, Google-only notices/help, and Internet permission. Both editions share the redesigned About pages, quiet alerts, call catalog, JSON files, and incident ZIPs.

FOSS runtime dependencies are limited to reviewed MIT Shizuku modules and Apache-2.0 AndroidX Annotation. The build rejects unreviewed runtime dependencies. Release packaging checks the edition marker, current changelog, and absence of Google descriptors in the FOSS APK. Complete source supports both editions; Google binaries are not bundled in the FOSS APK.

Both editions still work with the external proprietary Google Meet app/service, require independent permission/Shizuku setup, and retain Wi-Fi/grace readiness behavior. Do not enable both listeners at once. The repository is private and F-Droid review has not happened. A phone is not needed to build; device checks for 0.12-alpha are pending.

The following entries describe the shared 0.11-alpha interface/security work retained in both editions.

## Interface

- Replaced the long About dialog with a short introduction and separate buttons for purpose, permissions, privacy, operation, contacts, Google refresh, catalog import/export, incident reports, FOSS, licenses, changelog, and recent decisions.
- Removed reference paragraphs and decision history from the dashboard. They remain available in About.
- Rewrote help text with clear punctuation, short paragraphs, and consistent terminology.
- Corrected corrupted punctuation and separated complete, unchanged legal notices.
- Added system-bar and display-cutout spacing and flexible button heights.
- Added confirmation before clearing local history and rejection fingerprints.

## Security and correctness

- Added explicit Android 12+ cloud-backup and device-transfer exclusions. Relying only on allowBackup=false can leave device-transfer behavior dependent on the manufacturer.

- Require an exact notification-listener component match when reporting notification access.
- Before invoking a queued decline, recheck that the original Meet notification is still active with the same timestamp and decline action. Require the action's PendingIntent creator to be Meet. If Android cannot confirm it, skip the action.
- These are defensive fixes from source review. No active exploitation or independently verified exploit is claimed. Android notifications can still change after the final check.

## Licensing and privacy

QuietGate's own code and original assets remain MIT-licensed and free to use, inspect, modify, and redistribute under the license terms. The Google integration includes dependencies with separate terms, so the complete build is not entirely FOSS. The repository was private when inspected; no public-access certification is claimed.

No new permission or dependency was added. Caller matching remains local. Exports can contain personal data and remain unencrypted.

## Verification

See DEVICE-VERIFICATION.md and SECURITY-REVIEW.md for actual checks and limitations. This README accompanies the APK as a standalone release asset and inside the documentation archive. GitHub release assets are a flat list; folders are supplied inside ZIP files.
