# Free and open-source licensing

## 0.12-alpha editions

QuietGate FOSS bundles MIT-licensed project code/assets and Shizuku modules, plus Apache-2.0 AndroidX Annotation. Its runtime dependency graph and compiled APK exclude Google Play services and the Google account integration. The Google edition alone includes the Google dependencies listed below. Complete release source contains the project's MIT-licensed implementation for both flavors; building FOSS does not package the proprietary Google binaries.

Google Meet remains a separate proprietary app/service in both editions' operating environment. Public-source availability and official F-Droid acceptance are separate from this APK dependency split. The repository is still private. See EDITIONS.md and F-DROID.md for the exact scope and remaining work.

For 0.11-alpha, the complete MIT text and individual dependency notices are available from separate buttons in About. Source is included in release packages. The repository was private when inspected on 2026-10-26, so public repository access is not claimed.

QuietGate source is offered under MIT; see LICENSE for the complete terms and warranty disclaimer. Recipients may inspect, use, modify and redistribute it under those terms. Original project assets and the included release helper are covered by the project license unless separately noted.

## Dependencies

- Shizuku API/provider/shared/AIDL 13.1.5: MIT, copyright 2021 RikkaW.
- AndroidX Annotation 1.3.0: Apache-2.0.
- Google Play services Auth 22.0.0 and transitive Google libraries: Android Software Development Kit License, as declared by Google's Maven POMs; these are not covered by QuietGate's MIT license. Terms: https://developer.android.com/studio/terms.html . Bundled third-party license notices are included in GOOGLE-AUTH-THIRD-PARTY-LICENSES.txt and accessible from the Google contacts screen.
- Complete third-party notices and license texts are included in app/src/main/assets.
- Gradle, Android Gradle Plugin, Android SDK and JDK are separate build tools with their own licenses. The device supplies Android platform classes.

Shizuku manager/server is an external prerequisite, not bundled. Google Meet, Google Play services and Google account services are proprietary external requirements. The optional Google feature adds bundled Google client libraries; the complete dependency stack is not entirely FOSS. QuietGate's original source remains MIT. No F-Droid compatibility or licensing certification is claimed.

A private GitHub repository restricts who can obtain the source; it does not provide public source availability. You can later publish the repository without changing the included license. This project does not claim F-Droid acceptance, FSF certification, store review, an independent security audit or a reproducible binary build.

No signing keys, real call exports, credentials, contact databases or development caches belong in source distribution.
