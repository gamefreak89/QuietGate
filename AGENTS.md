# QuietGate standing release requirements

For every future release, include a version-specific README/disclosure in BOTH the local release folder and GitHub release assets. Put a readable summary in GitHub release notes and maintain the repository front-page README.

Required topics: release version and included APK/source/documents; requested permissions and purpose; privacy/data collection/storage/retention/export/deletion; Shizuku/root/Wi-Fi requirements; FOSS/project and dependency licenses; languages and honest AI-assistance disclosure; changelog; actual verification performed and untested limitations; signing/build type; checksum file.

Do not claim universal blocking, authenticated caller identities, independently confirmed rejection, root improvements, or test coverage that has not been demonstrated. Do not include real call catalogs, contacts, credentials, signing keys, device identifiers, personal test details, or SDK paths in releases.

Before packaging, update RELEASE-README.md and CHANGELOG.md for the exact app/build.gradle version. tools/release.py must refuse missing, empty or incomplete required documentation before creating output. A documentation correction to an existing release should be a labeled supplement: do not silently replace its APK, source or existing checksum-bearing archive.

No new tests unless the user requests testing/verification. A daily release-doc reminder may notify about missing documentation; it does not authorize automatic publication or visibility changes.
