# Google saved-contact setup for QuietGate 0.9-alpha

Status: the native client code builds, but no Google Cloud project/OAuth registration/People API enablement or successful live account connection is claimed. This is optional; the installed 0.8-alpha and device-contact matching do not need Cloud setup. Other contacts is not implemented.

## Register this Android build

1. In a Google Cloud project you control, enable **People API**.
2. Configure **Google Auth Platform** branding/audience/data access. Use accurate app identity, a support contact and appropriate privacy/homepage details. A private repository alone is not a publicly accessible privacy policy. Do not claim Google verification. For testing, explicitly add the consenting test users as required by your project's audience settings. Wider distribution may require Google's OAuth verification and current policy review.
3. Create an **Android OAuth client**, not a web client. Package: `org.open.quietgate`.
4. Register the **SHA-1 certificate fingerprint** of the APK you will install. Current development/debug build: `4A:5D:83:84:7B:F7:8F:FF:75:00:6B:19:B5:E4:5E:12:1F:F5:41:C6`. This is public certificate metadata, not a signing key. Confirm against the exact APK with `apksigner verify --print-certs`; a different signing key or future Play signing requires its own matching registration. Never publish or share the private signing key.
5. Configure only `https://www.googleapis.com/auth/contacts.readonly` for saved-contact access. Do not add contacts write, Gmail, directory, profile or contacts.other.readonly scopes for this feature.

Android Google Identity Services identifies this native app from the package and signing certificate. This flow does not need an embedded web client secret, a service-account key, an API key, a backend, a refresh token or a server authorization code. Do not paste credentials into source or chat. Setup must be completed in your own authorized Google Cloud account.

## Connect and verify

Open **Connected Google saved contacts (optional)** and review the disclosure. Choose one account, approve Google's saved-contact permission and wait for a complete refresh. The screen should show matching active and the last-success UTC time. Repeat explicitly for each account. No silent connection to all logged-in accounts occurs.

Use consenting test accounts and synthetic saved contacts to verify cloud-only emails/numbers, exclusion of Other contacts, denied/canceled grants, empty and paginated lists, network/API errors, expiry, local deletion, remote revocation and reconnect. No successful live authorization/sync/revocation is claimed until that has been performed. Do not assume the 0.8-alpha incident tests cover this feature. Requests fetch email/phone fields only, from CONTACT sources. Matching is offline; names/suffixes are never used to infer identity.

Snapshots require manual refresh and stop allowing matches after 24 hours. Offline edits or grant/account removal outside QuietGate are not immediately reflected. Refresh failure pauses cloud matching. Limits are ten selected accounts and 20,000 unique identifier fingerprints per account; oversized/incomplete downloads are not used partially. Google request timeouts, quotas, policies and service availability still apply.

## Disconnect and privacy

Disconnect deletes local matching data and requests Google revocation. If remote revocation cannot be confirmed, remove QuietGate from https://myaccount.google.com/connections . Google revocation affects all app scopes for that account. Delete-all removes local data and keys; grants must be managed separately. Device/synced contacts are never deleted. Contact caches/account labels/tokens are not incident or catalog exports. No QuietGate server receives your contact list; Google necessarily sees authorization/refresh requests. INTERNET and the Google client library are new in 0.9-alpha.

Official references: https://developer.android.com/identity/authorization and https://developers.google.com/people/api/rest/v1/people.connections/list . Follow the current console wording and requirements if they differ from these steps.
