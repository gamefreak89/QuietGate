# Privacy policy

Applies to QuietGate 0.9-alpha. Policy revision: 2026-10-24.

## Core protection and device contacts

Android Notification Access is broad; QuietGate filters its processing to Meet. It reads exposed caller emails/numbers and compares them locally against Android Contacts, including synced entries available in this profile. Identities and contact matches are not authentication. No conversation recording, microphone, location, phone state or system call-log access is requested. Shizuku remains a readiness condition, not a stronger Meet interception API.

## Optional Google saved-contact connections

Connections are off until the user explicitly chooses an account and authorizes read-only saved-contact access. No silent account enumeration or GET_ACCOUNTS permission. Each account is separately chosen. Google receives the authorization request and manual People API refresh. Its OAuth permission permits reading saved contacts, while QuietGate requests only emailAddresses and phoneNumbers from CONTACT sources. Names, Gmail, Other contacts and directories are not requested. No caller identifier is sent to Google for matching. No QuietGate server receives contact lists. No analytics, ads or remote AI is added.

INTERNET is required for this optional feature. Calls do not trigger Google requests. Full refresh is user-initiated, paginated and bounded; failed/incomplete lists are not activated. A completed snapshot permits local matches for 24 hours based on the device clock. Refresh attempts pause that account's matching until successful; errors keep it paused. Offline changes, revocation or removal of an account outside QuietGate may not be reflected until refresh/expiry. Expiry prevents matching; encrypted stale metadata/fingerprints can remain until refresh, disconnect or deletion. Up to 10 accounts and 20,000 unique identifier fingerprints per account.

Only the selected account label, random connection generation, status, last-success time and keyed contact fingerprints are persisted, encrypted with AES-GCM in app-private getNoBackupFilesDir storage. Android Keystore protects the AES and HMAC keys. Raw downloaded contact details exist transiently in process memory and are not written to QuietGate files. Google Play services manages OAuth grants/tokens in its own protected storage; QuietGate holds access tokens only in memory and requests no refresh token or server auth code. No app file, incident report or catalog export contains tokens or the connected contact cache. Encryption and no-backup storage are safeguards, not a guarantee against a compromised OS or deliberately privileged extraction; custom debug backups may copy encrypted bytes without exportable Keystore keys.

Disconnect deletes that account's local cache before requesting Google revocation. A failure to revoke is disclosed with manual Google Account instructions. Google revocation affects all app scopes granted for that account. Delete-all erases local account cache and its keys; Google grants must then be managed separately. Device/synced Google contacts are not deleted. Clearing call history/catalog does not disconnect accounts; use the dedicated controls. Uninstall removes app-private data and keys but does not necessarily revoke Google-side grants. No cloud matching is claimed working until Google Cloud OAuth/API setup and live verification are completed.

## History and incident exports

Settings include protection/grace deadlines; local history keeps up to 30 generic decisions and 128 bounded rejection fingerprints. The catalog stores up to 500 newest attempts within 90 days, pruned on read/write, with exposed identities, time, action status, categories, notes and imported labels. Hashes/fingerprints are pseudonymous, not anonymous. Labels/imports are unverified claims and never change trust rules.

User-started catalog JSON and selected-record incident ZIP exports can contain caller identities, timestamps, categories and notes. Connected-account labels/contact lists/fingerprints are not added to exports. An actual call record can contain an identity also present in a contact list. ZIPs include title/notes, UTC/epoch times, readable/JSON reports, hashes and optional original attachment bytes/names/embedded metadata (10 files, 10 MiB each / 25 MiB total). ZIPs are unencrypted; hashes do not prove authenticity. Chosen cloud document providers can download selected files or upload exports under their permissions. Choose local storage to avoid those transfers.

Drafts use activity memory/saved state. Temporary ZIPs use private cache and are deleted on normal success/failure; interruption can leave cache files until Android or manual cache cleanup. Clearing catalog/history removes app records, not original attachments or exports; delete those separately. Android backup is disabled, and the Google match cache uses no-backup storage.

## Reporting issues and limits

Do not post real contacts, caller exports, account labels, credentials or private logs in public issues. Request a private reporting route without including sensitive data. No security/store/F-Droid certification is claimed. Meet behavior, Android restrictions and device clocks affect operation; calls can ring first and rejection outcomes remain unconfirmed.
