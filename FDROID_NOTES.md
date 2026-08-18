# F-Droid preparation and publication notes

GeoJoystick is intended to remain suitable for F-Droid and similar FLOSS Android repositories.

## FLOSS/build baseline

- Application code: `GPL-3.0-only`
- No ads, analytics, tracking, accounts, subscriptions, paid features, billing, or proprietary updater
- No proprietary Android dependencies in the Gradle build
- Public canonical source repository on GitHub
- Command-line build path through `build.bat` / `tools/build.py`
- JDK 17 / Android SDK Platform 36 development baseline
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

Published developer-signed APK:

- `GeoJoystick-v0.1.4.apk`
- SHA-256: `f768d9ed09aa16d51a585470804374b52972f34ffb778f615b580de466b1d312`

F-Droid automatic update processing created fdroiddata merge request **!46016**, but binary verification failed. The reported comparison shows different Android version-control metadata between the published reference APK and the F-Droid build (`NO_VALID_GIT_FOUND` in the reference APK versus an embedded Git revision in the F-Droid build), together with a `classes.dex` difference.

Durable investigation tracker:

https://github.com/Kamui2040/K2040-GeoJoystick/issues/38

Do not describe v0.1.4 as available from F-Droid until the build/reproducibility problem is resolved and the public package page actually publishes version 104.

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
