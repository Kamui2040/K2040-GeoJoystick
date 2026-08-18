# F-Droid preparation and publication notes

GeoJoystick is intended to remain suitable for F-Droid and similar FLOSS Android repositories.

## FLOSS/build baseline

- Application code: `GPL-3.0-only`
- No ads, analytics, tracking, accounts, subscriptions, paid features, billing, or proprietary updater
- No proprietary Android dependencies in the Gradle build
- Public canonical source repository on GitHub
- Command-line build path through `build.bat` / `tools/build.py`
- JDK 17 / Android SDK Platform 36 / Android SDK Build-Tools 35.0.0 development baseline
- Gradle 8.13 / Android Gradle Plugin 8.12.3
- Release APK generation disables AGP VCS metadata in the maintained post-v0.1.4 build configuration so output does not depend on whether `.git` is present or usable
- Fastlane-style metadata under `fastlane/metadata/android/`
- Accepted synthetic/sanitized English and German store screenshots
- Public-safe screenshot provenance and hashes in `fastlane/metadata/android/SCREENSHOT_PROVENANCE.md`
- Current store icon is a 512×512 32-bit RGBA PNG exported directly from the accepted gecko launcher artwork (`SHA-256 6bf5a6fd69fb88f1a18e65177addc9ff64abe090bc812e5e0241bb5495383074`)
- Optional Ko-fi support link only; the app remains fully functional without it

## Current public F-Droid state

The public F-Droid package page currently lists **GeoJoystick 0.1.3 (`versionCode 103`)** as the suggested version:

https://f-droid.org/packages/com.k2040.geojoystick/

The page uses the accepted store presentation and discloses the optional OpenStreetMap-backed map picker with the network-service anti-feature note.

## v0.1.4 update status

The canonical GitHub release is **v0.1.4 (`versionCode 104`)** from source commit:

`0c3ae37501660300e4f23c45aeb07cffb68e62f9`

Current canonical developer-signed APK after the explicitly approved reference-artifact repair on 2026-08-18:

- `GeoJoystick-v0.1.4.apk`
- size: `1,763,281` bytes
- SHA-256: `450ca89ca53e875c4a1d6efa27928924a72b8324d762c6525f2471c80dfa1f3e`
- production signer SHA-256: `e0a833050d7c8fce7ddce85b2a86561304456d87b67bd6be1577d8f657e16778`
- APK Signature Scheme v2: verified
- embedded Git revision: `0c3ae37501660300e4f23c45aeb07cffb68e62f9`

The previous GitHub v0.1.4 APK (`SHA-256 f768d9ed09aa16d51a585470804374b52972f34ffb778f615b580de466b1d312`) has been superseded. The release tag and source commit were not changed.

F-Droid automatic update processing created fdroiddata merge request **!46016**, but its original binary verification failed. The original comparison reported different Android version-control metadata between the former published reference APK and the F-Droid build (`NO_VALID_GIT_FOUND` in the reference APK versus an embedded Git revision in the F-Droid build), together with a `classes.dex` difference.

A fresh reproducibility diagnosis on 2026-08-18 rebuilt the exact v0.1.4 source with JDK 17.0.20, Gradle 8.13, Android SDK Platform 36, and Build-Tools 35.0.0. Comparing uncompressed APK entries showed that `classes.dex`, `AndroidManifest.xml`, `resources.arsc`, and AGP app metadata matched the former published APK; the reproduced source-tree-context difference was `META-INF/version-control-info.textproto`. The original F-Droid `classes.dex` difference was therefore not reproduced by the canonical source/toolchain.

For the repair, the unchanged v0.1.4 source commit was rebuilt from a pristine Git checkout with valid `.git` metadata. The exact unsigned result reproduced the previously validated fresh-Git SHA-256:

`9c68eaf62a8a9543d62e013636deb4f92581dcb7dfa058616f55c5ec16c5b3ce`

The repaired signed artifact preserves the accepted `classes.dex` SHA-256:

`edfc89a65c655a971cf0700614ce4688ddc4156db634003e4acc20de3ce61a8b`

The repaired GitHub asset was re-downloaded after replacement and re-verified for hash, package/version, production signer, APK Signature Scheme v2, alignment, and embedded release revision. No runtime source changed and no device QA was required for this artifact-only repair.

Issue #38 remains open because a downstream F-Droid retry/verification has **not yet been performed**:

https://github.com/Kamui2040/K2040-GeoJoystick/issues/38

Do not describe v0.1.4 as available from F-Droid until the public package page actually publishes version 104 or a later accepted version.

## Reproducible-release requirements

For production releases intended for F-Droid reproducible-binary verification:

- build from a completely fresh Git clone at the exact release tag/commit;
- use JDK 17, Gradle 8.13, Android SDK Platform 36, and Build-Tools 35.0.0;
- use a fresh or isolated Gradle user home so stale build caches cannot affect evidence;
- do not depend on `.git` presence for maintained release APK contents; AGP VCS metadata is disabled in the current release configuration;
- compare an unsigned release from the fresh Git checkout with a second build from source without `.git` and require identical APK content before signing;
- keep production signing separate and perform it only after the unsigned reproducibility gate passes;
- never replace a published canonical APK silently; artifact replacement requires explicit maintainer approval and must be recorded with old/new hashes and validation evidence.

## Store metadata safety

- Keep all store screenshots synthetic/sanitized; never restore historical screenshots containing authentic precise location material.
- The WebView map picker uses bundled HTML/CSS/JavaScript and does not load remote JavaScript. It downloads OpenStreetMap tile images only when the picker is opened.
- Preserve visible OpenStreetMap attribution and the applicable ODbL notice.
- Confirm source, tag, release inputs, signing identity, and fdroiddata metadata before any future update submission.
- Keep F-Droid build/reproducibility evidence distinct from the developer-signed GitHub release artifact evidence.

## Current fdroiddata identity fields

```yaml
Categories:
  - Navigation
  - System
License: GPL-3.0-only
AuthorName: K2040
Donate: https://ko-fi.com/k2040
AutoName: GeoJoystick
Summary: Floating mock-location joystick for Android emulator testing
```

## Public repository

https://github.com/Kamui2040/K2040-GeoJoystick
