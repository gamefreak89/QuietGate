# QuietGate distribution privacy review

This is a documentation and packaging supplement for existing builds. APK bytes, signing identities, permissions, and app behavior are unchanged.

## Scope and findings

Reviewed all five named branches: 16 reachable commits, 16 source trees, and 184 distinct file blobs before cleanup. Author and committer identities used the repository account handle with GitHub noreply addresses. No personal email, local-user path, device serial, or credential pattern was found in those source snapshots. Example contact addresses in test fixtures use example.invalid; third-party license attribution is preserved.

Matched all 35 uploaded assets across four releases to exact local SHA-256 copies. Inspected source/documentation archives, archive paths/comments/extra fields, nested archives, and APK contents. No local usernames/paths, device serials, private keys, or credential patterns were found by the targeted inspection. APKs verified with a generic Android development-certificate subject. No signing key is distributed. No real catalogs, contacts, backup data, UI dumps, or screenshots were found in these release assets.

One verification document named a device family and described an operator's display settings. Current documentation now uses generic test-device/accessibility descriptions. Documentation ZIP entries carried local source-file modification times; replacement ZIPs use fixed timestamps, no comments or extra fields, and a neutral creator-platform value. Historical APK and source ZIP entry timestamps were already fixed.

The repository had four pull requests, no issue/review comments or reviews, and zero listed Actions workflow runs. Source, PR text, and release descriptions were checked. No Actions log deletion is indicated by the run inventory. This review covers repository and release surfaces, not unrelated account/profile data or unrelated local files.

## Changes

- Generalized verification prose without removing meaningful test results or limitations.
- Added a release privacy preflight for local-user paths, private identifiers supplied only through the environment, common credential patterns, and prohibited private files inside archives. It reports file/rule names, not matched private values.
- Expanded private-file exclusions and ignore rules.
- Normalized documentation ZIP metadata for future packaging.
- Prepared explicitly named privacy-sanitized source/documentation replacements and replacement checksums. No silent replacement of existing checksum-bearing archives is intended.

## Release replacement map

For 0.11-alpha and 0.12-alpha, only documentation ZIP metadata changes; their APK and source archives remain unchanged. For 0.13-alpha, the verification Markdown, documentation ZIPs, and source archive are sanitized. Replacement files include privacy-sanitized in their names. SHA256SUMS-privacy-sanitized.txt supersedes the old checksum asset and covers the final release asset set, excluding itself. This supplement accompanies each affected release.

Original files are retained only in private local working storage for recovery. Removing superseded GitHub assets is a separate final step because GitHub does not offer a normal undelete operation for release assets. Until that removal is confirmed, the original assets remain attached and the cleanup is incomplete for those historical downloads.

## Remaining identity and retention limits

The repository URL, account handle, GitHub noreply attribution, project dates, and public signing-certificate fingerprint can link project activity to the hosting account. They are not a device serial or local-machine identity. This cleanup does not rename/transfer the account, change its profile, change signing keys, or make the repository public.

A corrected source commit does not erase copies already downloaded or GitHub-retained commit/diff/edit-history caches. Removing sensitive cached history, if required, can need GitHub Support. This review is a targeted minimization audit, not a guarantee of anonymity or an independent security certification.
