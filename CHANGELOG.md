# Changelog

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
