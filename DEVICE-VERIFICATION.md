# QuietGate verification — 0.13-alpha

Both development editions compiled. FOSS lint: 0 errors/12 warnings; Google lint: 0 errors/13 warnings. Compiled-APK inspection passed for both; FOSS has zero Google type references and no Internet permission. Both signatures match the existing development certificate. No new automated tests were added. See ARTIFACT-INSPECTION.json and REQUIREMENTS-REVIEW.md.

Both 0.13-alpha development APKs were installed as in-place updates on an Android test device. Both enable dialogs were inspected visually and through the Android UI hierarchy with large-text accessibility settings. Purpose, matching, limits, readiness requirements, edition-specific access, and confirmation controls were present. The Google dialog scrolls to its complete footer; FOSS content and buttons fit and remain reachable. Google consent text appears only in the Google dialog; FOSS explains that Google account and Internet access are absent.

The Google enable dialog was canceled. FOSS protection was temporarily paused to inspect its dialog and then re-enabled. The FOSS dashboard confirmed enabled protection, a connected listener, running status service, and satisfied setup requirements. The Google edition remained paused. Existing grants and private data were retained; no contacts or catalogs were opened or cleared, no OAuth consent or exports were performed. Notification-listener grants were left unchanged; only FOSS protection was enabled during verification.

The user reported a successful 0.12-alpha FOSS live-call test. No new live call, OAuth authorization, connectivity outage, 24-hour endurance, or root behavior was independently tested for 0.13-alpha.

## Historical verification

# QuietGate verification — 0.12-alpha editions

Later on-device FOSS setup, UI, and foreground-service checks are documented in [the labeled 0.12-alpha supplement](DEVICE-VERIFICATION-0.12-alpha-SUPPLEMENT.md). The no-device statements below describe the earlier build stage. Existing release archives remain unchanged; live-call verification is still pending.

## Current 0.12-alpha scope — no device required or used

Final local results: assembleFossDebug, assembleGoogleDebug, assembleFossRelease, assembleGoogleRelease, and both existing Android-test APK targets completed successfully. lintFossDebug reported 0 errors/12 warnings; lintGoogleDebug reported 0 errors/13 warnings. Warnings concern localization, obsolete API checks/resources, and icon metadata; no lint error was disabled. Both development APK signatures verified and matched the existing development certificate. Unsigned release artifacts were inspected as non-debuggable. DEX/manifest/asset inspection passed for both build types, including zero Google type references in FOSS and retained integration in Google. ARTIFACT-INSPECTION.json contains the packaged development APK hashes, permissions, and results. No physical device or emulator was used.

The two editions were compiled and inspected on the computer. Neither 0.12-alpha edition was installed on a phone. No contacts, app history, permissions, notification listeners, Shizuku grants, or phone settings were changed. Existing historical test sources were compiled for the appropriate flavors; no new instrumentation tests were added or executed.

FOSS runtime dependency resolution contains five reviewed artifacts: Shizuku api, provider, aidl, and shared 13.1.5 (MIT), plus AndroidX Annotation 1.3.0 (Apache-2.0). The build-time allowlist rejects other runtime artifacts. The compiled FOSS APK was checked for its distinct app ID, absent Internet permission, absent Google DEX type references and integration types, and absent Google-only help/license assets. The Google APK retains its original app ID, Internet permission, and integration. Final build/lint/artifact results are recorded in the edition release disclosure and inspection report.

The first Google-flavor lint run inspected its overlay manifest, which initially omitted the shared foreground-service declaration. The declaration was repeated in the overlay with the same specialUse type; both packaged manifests retain the shared service and type. No lint error was suppressed. FOSS includes an explicit remove rule for Internet permission; the manifest merger warns when there is no declaration to remove, which is expected.

Real-device FOSS permission setup, UI, saved/unknown caller flows, export/import, background recovery, and interactions with Meet remain pending. A new app ID means old Google-edition grants do not apply to FOSS. The historical synthetic decline suite uses detached notifications/test-owned PendingIntents and is not current coverage for the live Meet-created-action guard.

## Historical 0.11-alpha verification

## Current release checks — 2026-10-26

- Gradle assembleDebug and lintDebug completed successfully with JDK 17, Gradle 8.9, and Android SDK/build tools 35. Android lint reported 0 errors and 14 warnings, primarily localization, obsolete API guards/resources, and launcher-icon metadata. The earlier backup-rules warning was resolved. The local Android analytics-settings write warning did not prevent the build.
- APK signing verification passed. The new APK's certificate matched the installed development build. The app updated in place from versionCode 11 to 12 without uninstalling or clearing app data.
- Device package metadata confirmed versionName 0.11-alpha. The dashboard continued to report protection enabled, contacts and notification access allowed, listener connected, status service running, and Wi-Fi/Shizuku ready after installation.
- On-device navigation opened purpose, permissions, privacy, operation, contacts, Google refresh, catalog help, incident help, FOSS, MIT License, and changelog pages. Documents loaded; the in-app changelog showed the current release. Back navigation returned to About.
- Separate dependency notices, Google notices, Shizuku MIT text, and Apache 2.0 text loaded on device. Recent decisions opened from About. The local-history confirmation was checked using Cancel; no history was deleted.
- Screenshots were visually inspected for the About hub, dashboard, and system-bar spacing/contrast. Help no longer fills the bottom of the dashboard. Buttons use flexible heights and text remains selectable on reference pages.

The current source review and defensive fixes are in SECURITY-REVIEW.md. No new instrumentation tests were added or run. The historical synthetic decline suite uses test-owned PendingIntents and detached notifications; it no longer exercises the new requirement for a live Meet-created action and must not be presented as current regression coverage. Live incoming-call checks of that guard remain pending, as do renewed Google consent, multi-device migration, rotation/large-font stress testing, and long-duration background operation. These results are not a vulnerability-free certification.

## Historical 0.5-alpha verification

This distributable report intentionally omits operator names, local paths, phone models/identifiers, account addresses, IP addresses and exact test-session timestamps.

## Automated device verification

17 controlled tests passed: unknown email rejection action; missing identifier handling; saved email allowance; saved phone allowance after punctuation removal; unknown phone rejection action; avoiding Hang up; ignoring other packages; pause; queued-action cancellation after pause; selective missed-call cleanup; unrelated/contact alert preservation; quiet toggle and retention; changed notification identity matching with caller/group/expiry safeguards; grace start/reset; 24-hour expiry; late recovery requiring manual enable; bundled purpose/permission/license documents.

These tests use synthetic notification objects and a test-owned PendingIntent. Existing contact values are checked locally without printing or distributing them. Expiry is simulated without changing the device clock. They do not substitute for a real incoming-call test.

## Observed integration behavior

Earlier live tests allowed a saved calling email and rejected an unsaved email. Calls to two receiving Google accounts were tested. A missed-call alert initially remained when Meet changed its notification identity; the corrected fallback then removed the alert, confirmed by the operator and by no remaining Meet notification. These observations apply to that tested setup, not all devices or future Meet releases.

The current build adds an on-screen hours/minutes/seconds timer and Android's native notification countdown. Recovery before the deadline cancels the countdown. Recovery after expiry leaves protection off until the user reconnects both requirements and explicitly enables it.

An actual Wi-Fi disconnect showed the app's hours/minutes/seconds countdown. Android's notification record confirmed both showChronometer and chronometerCountDown enabled during grace, then both disabled after Wi-Fi/Shizuku recovery. The phone's clock and the Shizuku service were not changed for this check.

## Privacy and distribution

The APK contains no operator/device test data. Build caches, generated logs, local SDK paths and signing keys are excluded from the source archive. Required package/version metadata and a generic Android development signing certificate remain. QuietGate's MIT license and third-party license notices are included in source and APK assets. Open-source licensing does not imply store certification or independent security certification.

The app processes caller identifiers locally. Its local decision timestamps, saved deadline, settings and salted rejection fingerprints are runtime data, not source-distribution data. The in-app Clear local history & fingerprints control removes the decision/fingerprint store without deleting contacts or changing the protection setting.

## Limits

Initial ringing/popups can precede listener handling. Action invocation remains unconfirmed within the app; notification dismissal alone does not prove a call ended. Separate work profiles, every receiving account, other devices and long-duration endurance have not all been individually verified. Android may defer background alarms; the deadline is checked before call actions and on app resume as well.

## Follow-up verification

All 17 controlled tests passed again with zero failures. Following an actual reboot and restoration of Wi-Fi and authorized Shizuku, protection, the foreground status service and the Meet listener were running; the countdown was cleared. A subsequent saved-caller call rang normally after the calling email was saved as a regular contact. Google Other contacts entries had not been returned by either the app lookup or an independent exact query; saving regular contacts resolved this observed mismatch. This confirms that scenario, not universal device compatibility.

The installed APK matched the distributable APK byte for byte. Final checked state: protection enabled, quiet cleanup enabled, listener connected, foreground status service running, no active grace deadline, status notice Checks ready. The temporary test package was removed. Actual uninterrupted 24-hour endurance and live call behavior on a second device remain unverified.

## 0.6-alpha catalog addition

The new local catalog compiled and the APK was installed as an update. The earlier 17-test and live-call results describe the preceding 0.5 behavior; the new catalog, export and clear flows have not yet received device functional testing. No tests were run for this feature addition.

## 0.7-alpha catalog organization

Compiled successfully and installed as an update. Added bounded JSON import, full catalog export, duplicate detection, imported-entry labels, category filters and editable notes. No new automated or device functional tests were run for these import/export flows. Existing controlled-test results apply to the earlier core call-screening behavior.
# 0.8-alpha incident export verification — 2026-10-24

- `assembleDebug lintDebug` completed successfully with Android SDK/build-tools 35, Gradle 8.9 and JDK 17.
- Lint: 0 errors, 18 warnings (localization, existing backup/icon/obsolete-version concerns). Compilation also reports existing Shizuku annotation/deprecated-API warnings.
- The existing instrumentation source received a targeted lint suppression for the receiver overload used only below API 33; API 33+ already uses RECEIVER_NOT_EXPORTED. No new tests were added or executed.
- `apksigner verify --print-certs` succeeded for the new APK and archived 0.7-alpha APK. Their SHA-256 signing-certificate digests match. This supports update compatibility; no device installation is claimed.
- Source review covered individual record selection, re-reading for edits/expiry/clearing, imported labels, UTC/epoch timestamps, attachment limits, safe numbered ZIP paths, streamed hashing, checksum coverage, background I/O, error reporting and private temporary-file cleanup on normal completion/failure.
- Device functional checks remain pending: file-picker grants across recreation/process death, local/cloud providers, cancellation, unreadable/oversized attachments, low storage, report correctness/hash comparison, interrupted exports, and existing catalog JSON flows. Static analysis and compilation do not demonstrate those behaviors.
- No phone data or real caller identities were collected for this change. No new runtime permissions were added.

# 0.9-alpha optional Google contacts — 2026-10-24

- `assembleDebug lintDebug` completed successfully for versionCode 10 / 0.9-alpha with Google Play services Auth 22.0.0. Lint reports 0 errors and 16 warnings; compilation notes deprecated Android UI APIs.
- `apksigner verify --print-certs` passed. The signing certificate matches the development certificate used for 0.7/0.8-alpha, and its public SHA-1 is documented for Android OAuth registration.
- Reviewed the merged manifest: INTERNET is the new app permission; no GET_ACCOUNTS, microphone, phone-state, call-log, location, overlay, ads identifier or analytics-provider declaration was added.
- Source review covered explicit account choice/scope checking, fixed-host HTTPS requests with redirects disabled, field/source-limited pagination, response/identifier/account bounds, no partial activation, local-only caller matching, 24-hour expiry, failed-refresh pause, encrypted no-backup cache, Keystore-protected keys, token non-persistence, exports staying separate, and generation checks preventing late reconnect after disconnect/deletion.
- Full Google AAR third-party notices are bundled unchanged; Google client licensing is disclosed separately from MIT app code.
- No new cloud tests, device installation, real-account authorization, download, matching, refresh expiry or remote revocation verification were performed. External Google Cloud Android OAuth registration and People API enablement are absent. Code compilation/static analysis is not evidence of live Google integration working.
- Earlier 0.8-alpha incident/catalog verification is historical and does not cover the Google connection feature. The installed phone build remains 0.8-alpha.

# 0.10-alpha automatic refresh verification — 2026-10-24

- `assembleDebug`, `assembleDebugAndroidTest` and `lintDebug` passed. Lint: 0 errors, 18 warnings. APK signature verified against the same development certificate used by prior installed builds.
- Installed versionCode 11 / 0.10-alpha after a verified private APK/app-data backup. Matching backup copies are also kept in app-private storage on the phone; no Keystore keys were exported.
- 56 automated device checks passed, 0 failed: 19 new scheduling/state/migration checks, 4 live-job assertions across existing connected accounts, 16 incident/catalog regressions and 17 controlled core regressions.
- Migration was compared against the original encrypted backup using the existing app Keystore key on-device. Original account identity/generation/status/success dates and contact hash sets were preserved. New auto defaults were off until explicitly enabled under user authorization.
- New checks cover 12-hour/1-hour-flex persisted job policy, battery/network constraints, opaque extras, cancelable auto consent disclosure, encrypted options reload, background retention/retry, enable/disable/cancel, renewed-consent state, late callbacks, reconnect scheduling, periodic/one-off cancellation and preservation of other live connections. Synthetic data was individually removed; no clear-all/key deletion was performed against real account data.
- The user-authorized connected accounts were opted into automatic refresh with unmetered Wi-Fi only. Android's actual periodic job service was triggered immediately for verification; both refreshed successfully through real Google authorization/People API using existing grants, with no consent UI and no token persistence. Persisted periodic jobs remained registered after completion. The populated list retained its expected identifier count; counts/account labels are not included in this release document.
- Separately, the on-phone background-now button queued a one-off job without a forced scheduler command. Its success/updated timestamps were confirmed in the app. This manual UI check is additional to the 56 automated assertions.
- Incident regression exercised actual local Android file-picker ZIP saving. Core regression used synthetic notifications/test-owned decline actions and simulated grace timestamps, not real incoming Meet calls.
- Original core preferences/catalog were restored from the private backup and verified byte-for-byte before normal restart; new user-approved auto settings and freshly updated Google snapshots were retained. Test APK, synthetic entries/jobs/files and test baseline were removed. Normal notification access and protection readiness were restored.
- Limits: no elapsed 12-hour/24-hour endurance run, actual reboot, Doze, battery restriction, offline recovery, real grant revocation/expiry or attention-notification delivery test; consent-required transitions were synthetic. No live incoming Meet calls or cloud document-provider checks. Job persistence flags and immediate execution do not prove every future Android scheduling condition. Snapshot expiry remains unchanged at 24 hours.
- This stage is local only. No repository/source push, PR change, tag, GitHub release or visibility change was performed. Private backups, actual account identifiers/counts, credentials, device identifiers and call catalogs are excluded from release packaging.

