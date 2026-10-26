# QuietGate 0.11-alpha source review

Review date: 2026-10-26. This is a scoped source review and build verification, not an independent audit or security certification.

## Areas inspected

Android manifest and component exposure; notification filtering, identifiers, action invocation and quiet alerts; contact lookup; protection lease and alarms; local catalog parsing and retention; incident attachment paths and size limits; Google authorization, HTTPS fetching, encrypted snapshots, generation checks, and background scheduling; private data and release packaging.

## Changes

0. Added explicit exclusions for all supported app-data backup domains under both cloud-backup and device-transfer on Android 12+. The prior manifest relied on allowBackup=false alone; Android documents manufacturer-dependent device-transfer behavior for that flag. This protects the local catalog and settings as well as the already excluded no-backup cache. Source: https://developer.android.com/identity/data/autobackup. Actual migration between devices was not tested.

1. MainActivity previously used a substring search on the enabled-listeners setting. A different component name containing the package string could produce misleading readiness. Detection now parses and compares the exact component.
2. MeetRejectListener retained a decline PendingIntent across asynchronous contact lookup. A call could disappear or update before invocation. The listener now requires the original notification to remain active with the same timestamp and action, and verifies that Meet created the PendingIntent. Failure to confirm skips the action. This narrows the race window but cannot make the check and send atomic.

The review did not establish a remotely exploitable vulnerability. These issues are described as readiness correctness and action hardening rather than assigned invented CVEs or severity scores.

## Existing safeguards inspected

- Notification callbacks filter the Meet package. Private activities/services are not externally exported; notification and job services require Android binding permissions. The Shizuku provider requires its privileged cross-user permission.
- OAuth requests use the read-only contacts scope. People API fetches use fixed HTTPS endpoints, disable redirects, bound response sizes/pages, and never upload a caller identifier for matching.
- Google snapshots use AES-GCM and Keystore-protected keys, bounded account/identifier counts, 24-hour freshness, and generation checks against late completion after removal or settings changes. OAuth tokens are not written to app files.
- JSON import is bounded to 2 MiB and 5,000 input records. Imported claims remain labeled and do not change protection rules.
- Incident attachments use fixed numbered archive paths, not provider-supplied filenames. Attachment counts and byte limits are checked while reading. Exports require a user-selected destination.
- App backup and cleartext traffic are disabled. Catalog and decision data are private but are not encrypted with the Google-cache keys.

## Remaining limits

Development/debug signing permits authorized debugging access and is not a hardened production distribution. Rooted or compromised devices are outside the app-private storage boundary. Call identities are unauthenticated. Google/Meet/Shizuku and transitive dependencies have not received a complete independent audit. No exhaustive dependency vulnerability database scan, penetration test, or endurance test is claimed. Device clock changes affect persisted deadlines and freshness. Exported JSON/ZIP files are unencrypted, and interrupted exports can leave private temporary cache files. A public-source release requires the owner to make source accessible.
