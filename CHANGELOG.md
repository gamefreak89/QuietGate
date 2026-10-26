# Changelog

## 0.12-alpha — 2026-10-26

- Added separate Google and FOSS build flavors with shared protection, catalog, incident-export, and About code.
- Preserved org.open.quietgate for the Google edition; added org.open.quietgate.foss and the QuietGate FOSS label for independent installation and storage.
- Restricted Play services Auth and all Google account/refresh code, components, help, and notices to the Google source set. FOSS has no Google libraries, account connection, refresh jobs, or Internet permission.
- Added FOSS-specific privacy/permissions/license disclosures. Google-specific historical instrumentation sources no longer compile into the FOSS test APK.
- Added a reviewed FOSS runtime-dependency allowlist and compiled-APK inspection of permissions, DEX type references, assets, edition markers, and package identities.
- Added edition-aware release packaging that rejects mismatched or stale APKs and produces separately named APKs and documentation.
- No phone is needed for builds and artifact inspection. This release has not been installed or tested on a device; previous UI/call observations do not verify the new FOSS edition.
- F-Droid preparation is documented; public source, signing/distribution decisions, metadata, external-service review, and actual inclusion review remain pending.

## 0.11-alpha — 2026-10-26

- Added a compact About hub with individual help, privacy, permissions, FOSS, license, changelog, and recent-decision pages.
- Removed long reference text from the main screen; fixed corrupted punctuation and rewrote help with shorter, professional paragraphs.
- Added system-bar/cutout spacing, flexible button heights, and confirmation before local history deletion.
- Tightened notification-access detection to compare the exact listener component.
- Added explicit Android 12+ cloud-backup and device-transfer exclusions for app data, in addition to disabling backup, to protect local catalog/settings during system migration.
- Hardened queued decline actions by checking the current notification, timestamp, action, and Meet PendingIntent creator immediately before invocation. An unavailable or changed action is skipped.
- Added SECURITY-REVIEW.md, a standalone CHANGELOG-README.md, and a versioned documentation ZIP beside the APK for release assets.
- No new permission or third-party dependency. MIT project licensing and separate Google dependency terms remain explicit.
- Verification and remaining limitations are recorded in DEVICE-VERIFICATION.md; prior results are historical.

## 0.10-alpha — 2026-10-24

- Added optional per-account native persisted 12-hour background refresh, 1-hour flex, non-low-battery/network constraints and unmetered Wi-Fi-only option.
- Reuses existing Google contacts.readonly authorization without background consent UI or token persistence.
- Added background-now queueing, last-attempt/status UI and generic reconnect notification.
- Transient background errors retain a still-fresh snapshot while retrying; consent-required pauses and 24-hour matching expiry remain enforced.
- Added stable opaque scheduling metadata to the encrypted cache with backward-compatible defaults, duplicate-ID checks and no loss of existing hashes.
- Disabling/disconnecting/deleting data cancels work; generation checks reject late background completion.
- No new permission/library for scheduling. Tests and live job verification are recorded in the verification document; GitHub publication is deferred.

## 0.9-alpha — 2026-10-24

- Added optional user-chosen, read-only Google saved-contact connections and manual refresh.
- Added encrypted app-private no-backup account snapshots containing keyed identifier fingerprints; no tokens or raw contact lists are persisted.
- Added last-refresh/account status, 24-hour matching expiry, disconnect/cache deletion, Google revocation request and delete-all key erasure.
- Added Google Identity Services Auth 22.0.0 and INTERNET. No Other contacts, Gmail, directory or names request; no QuietGate backend.
- Contact decisions use available device contacts plus fresh completed connected snapshots without network requests during calls.
- Added bounded HTTPS People API fetching, pagination, no partial-list activation and generation checks against late completion after disconnect.
- Google Cloud Android OAuth registration and People API enablement are external prerequisites; live Google authorization/sync/revocation are not verified.

## 0.8-alpha — 2026-10-24

- Added incident ZIP export for individually selected retained catalog records.
- Added incident title/notes and optional user-selected attachments (10 files; 10 MiB each / 25 MiB total).
- Included readable and JSON reports, UTC and epoch timestamps, imported/source labels, attachment names, original bytes, and SHA-256 manifest.
- Export uses a private temporary ZIP before writing the destination; errors do not claim success.
- No new Android permissions or changes to contact matching, rejection or existing JSON import/export.
- Compilation/static analysis status is recorded in DEVICE-VERIFICATION.md. Device functional checks remain pending.

These are feature milestones reconstructed from the available development history. Older binaries are not reconstructed or represented as preserved releases.

## 0.7-alpha — 2026-10-24

- Added versioned JSON export and merge import, including legacy array compatibility.
- Added duplicate detection; existing category/notes edits win on duplicates.
- Added category filters, per-attempt labels and notes.
- Imported claims are labeled and do not change protection rules.
- Limited input to 2 MB / 5,000 records; retained catalog remains 500 newest within 90 days.
- Added local release packaging, privacy/permissions/FOSS documentation and AI disclosure.
- Build compiled and installed; new import/export flows have not received functional testing.

## 0.6-alpha

- Added local identity-bearing catalog with timestamps and decline sent/failed status.
- Added catalog viewing, clearing and initial JSON export.
- No retrospective reconstruction of old caller identities.

## 0.5-alpha

- Added visible in-app countdown and Android notification chronometer.
- Strengthened deadline recovery and queued-action pause checks.
- Added permissions/privacy/license disclosure and local-history clearing.
- Earlier controlled suite: 17 passes; live saved/unsaved caller checks and status recovery observed on tested configurations.

## Earlier alpha milestones

- Notification-driven contact matching and explicit Meet Decline/Reject actions.
- Shizuku/Wi-Fi readiness and 24-hour grace logic.
- Selective missed-call cleanup, including changed notification identities.

## Known limitations

English action labels; best-effort Android/Meet behavior; possible initial ringing; no authenticated caller identity or independently confirmed call outcome. Phone matching uses identical digits after punctuation removal, not general country-code normalization. Google Other contacts may need to be saved as regular contacts. Long-duration endurance and every device/profile/account combination remain unverified.
