# QuietGate 0.5-alpha verification

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

