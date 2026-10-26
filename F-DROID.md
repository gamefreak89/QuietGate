# F-Droid preparation

QuietGate FOSS is a candidate build flavor, not an accepted F-Droid listing. No submission, repository visibility change, or publication was performed by creating this flavor.

## Completed in 0.12-alpha

- Separate foss flavor with app ID org.open.quietgate.foss and label QuietGate FOSS.
- Google Play services dependency restricted to googleImplementation.
- Google account/refresh classes and component declarations confined to app/src/google; no Internet permission in the merged FOSS manifest.
- FOSS-specific privacy, permissions, help, and MIT/Apache-2.0 dependency notices.
- Source and build instructions, compile-time dependency allowlist, edition-specific release disclosures, and artifact checks.

## Still required before submission

1. Make the real, up-to-date source publicly accessible, including MIT and dependency notices. The repository is currently private.
2. Resolve production signing/distribution policy and publish a source tag corresponding to the exact intended release. Debug APKs are development artifacts, not the proposed official-store binary.
3. Provide complete listing metadata, icon, screenshots, and version-specific short changelogs. Build the foss release variant from source using an acceptable toolchain.
4. Have F-Droid review external dependencies/anti-features: QuietGate's purpose specifically involves proprietary Google Meet. A FOSS APK does not turn Meet into free software or guarantee acceptance.
5. Agree on scanner handling of the separate Google flavor. A FOSS-only source checkout can remove app/src/google and app/src/androidTestGoogle and omit the googleImplementation declaration. Merely not executing the Google feature does not remove proprietary libraries; the selected FOSS build must be checked.
6. Verify the FOSS edition's independent permission setup, UI, saved/unknown caller behavior, exports, and background readiness on real devices. Existing Google-edition results are not FOSS-device verification.

Official guidance: https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/ and https://f-droid.org/docs/Inclusion_Policy/.

Neo Store is a repository client. A separately maintained, signed F-Droid-compatible repository can distribute APKs independently of acceptance in the official f-droid.org catalog. No such repository has been deployed by this change.
