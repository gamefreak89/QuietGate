# QuietGate 0.12-alpha — FOSS device-verification supplement

Documentation supplement dated 2026-10-26. The existing 0.12-alpha APKs, source/documentation archives, and checksum files remain unchanged. This document records later checks of the already-built FOSS APK. It intentionally omits device identifiers, account/contact values, call records, and exact session times.

## Artifact and installation

- QuietGate FOSS 0.12-alpha, versionCode 13, package org.open.quietgate.foss installed alongside the existing Google-connected edition without uninstalling or replacing it.
- The installed APK's SHA-256 matched the packaged FOSS artifact: 7c0f32b4bfd27f9701b9aefdba5aa4c2de77a5d015a36cb0588968502afa03d1.
- Installed-package inspection confirmed the expected version and separate permission grants. The FOSS artifact has no Internet permission or Google account/refresh implementation, as already established by the compiled-APK report.

## Permission and protection checks

- Contacts and status-notification permissions were granted through Android's prompts.
- The separate FOSS Shizuku authorization prompt appeared and authorization succeeded.
- The original edition was paused and its notification access temporarily disabled before FOSS notification access was enabled. Only one QuietGate listener was enabled at a time.
- FOSS notification access was enabled through Android Settings. Its dashboard reported the Meet listener connected, Contacts/status permissions allowed, Shizuku authorized and running, and Wi-Fi connected.
- Enabling FOSS protection succeeded. The dashboard reported protection On and status service running. Android service inspection independently reported GuardNotificationService running in the foreground with the specialUse type and its ongoing status notification.
- No fatal crash entry was found for the FOSS process during the checked session. This is not an exhaustive crash or endurance test.

## UI checks

Nineteen navigation/confirmation checks passed: FOSS dashboard without a Google account control; FOSS-specific About title/menu; purpose/limits; permissions; privacy; background operation/grace; contacts/quiet alerts; catalog import/export help; incident help; FOSS disclosure; MIT text; current 0.12-alpha changelog; dependency notices; Shizuku MIT text; Apache 2.0 text; recent decisions; canceled local-history deletion confirmation; catalog opening; and incident-report screen opening without another permission request.

Reference and legal documents loaded. The Google refresh and Google-license controls were absent from FOSS. Screenshots were reviewed for spacing, system-bar contrast, and the edition label. No history/catalog deletion or import/export was performed. Opening the catalog may perform its normal retention pruning.

## State left on the device

- The original Google-connected edition (0.11-alpha) was restored to protection On, notification listener connected, and foreground status service running.
- QuietGate FOSS remains installed. Its Contacts, status-notification, and Shizuku setup grants remain available. Its protection is paused and its notification-listener access is disabled, avoiding duplicate call handling.
- The Google edition was not upgraded to the separate 0.12-alpha build during these checks. Existing app storage was not cleared or migrated, and device contacts were not edited.

## Remaining verification

No real incoming Meet call was placed. Saved-caller allowance, unknown-caller decline, the new live-action guard, and missed-call cleanup still need coordinated live-call verification of FOSS. Granting Contacts is not a functional contact-matching test. Actual JSON import/export, incident ZIP output, caller labels/notes, cross-profile behavior, reboot, battery restrictions, offline grace expiry, long-duration service survival, and Google 0.12-alpha device regression were not tested in this session.

The historical synthetic decline suite was not run because detached test notifications/test-owned PendingIntents do not exercise the requirement for a current Meet-created action. No new instrumentation suite was added or claimed. The observations above are setup/UI/service checks, not independent confirmation of call rejection, caller identity, vulnerability absence, or F-Droid acceptance.

This later standalone supplement is not covered by the original release SHA256SUMS.txt. Its accompanying supplement checksum covers this document only. Repository visibility and release publication status were not changed by verification.
