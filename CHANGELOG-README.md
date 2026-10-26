# QuietGate 0.13-alpha changelog

The enable-protection popup now introduces QuietGate as a safeguard against spam and unwanted Google Meet video calls. Short sections explain saved-contact matching, attempted decline actions, limits, and current setup requirements. The content scrolls at large font sizes while the confirmation buttons remain available.

The Google edition explains separate, optional read-only Google Contacts OAuth consent and Internet refresh. FOSS explains device-only contacts with no Google sign-in or Internet permission. There are no new permissions, dependencies, or protection-policy changes.

Both editions still enforce Wi-Fi and authorized Shizuku readiness and the same 24-hour grace period. Source review found that local contact matching and Meet notification-action invocation do not inherently use either as a technical prerequisite. See REQUIREMENTS-REVIEW.md for the distinction and Google refresh network options.

The user reported a successful live call test with 0.12-alpha FOSS. This was not independently observed and does not establish universal blocking or operation without current readiness conditions. See DEVICE-VERIFICATION.md for this version's actual build, inspection, and UI observations. Full version history is in CHANGELOG.md and the in-app Changelog page.

Project code remains MIT-licensed. FOSS bundles reviewed MIT/Apache-2.0 dependencies; Google libraries retain separate terms in the Google edition. Google Meet is an external proprietary service. This is not F-Droid approval or a security certification.

This README is supplied beside each APK and inside its documentation ZIP. GitHub assets are a flat list; the ZIP contains the documentation folder. Development APKs are debug-signed; SHA256SUMS.txt identifies the packaged files.
