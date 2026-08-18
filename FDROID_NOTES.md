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
- Release APK generation disables AGP VCS metadata so output does not depend on whether `.git` is present or usable
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

F-Droid automatic update processing created fdroiddata merge request **!46016**, but binary verification failed. The original F-Droid comparison reported different Android version-control metadata between the published reference APK and the F-Droid build (`NO_VALID_GIT_FOUND` in the reference APK versus an embedded Git revision in the F-Droid build), together with a `classes.dex` difference.

A fresh reproducibility diagnosis on 2026-08-18 rebuilt the exact v0.1.4 source twice with JDK 17.0.20, Gradle 8.13, Android SDK Platform 36, and Build-Tools 35.0.0: once from a fresh Git checkout and once from the GitHub tag archive without `.git`. Comparing uncompressed APK entries against the published v0.1.4 APK showed that `classes.dex`, `AndroidManifest.xml`, `resources.arsc`, and AGP app metadata are byte-identical in all three APKs. The only reproduced content difference is `META-INF/version-control-info.textproto`:

- published v0.1.4 APK: `NO_VALID_GIT_FOUND`
- fresh Git checkout: embedded revision `0c3ae37501660300e4f23c45aeb07cffb68e62f9`
- GitHub source archive: `NO_SUPPORTED_VCS_FOUND`

The original F-Droid `classes.dex` difference is therefore not reproduced by the canonical source/build baseline. Issue #38 remains open until a hardened build is validated and a downstream F-Droid build verifies successfully.

Durable investigation tracker:

https://github.com/Kamui2040/K2040-GeoJoystick/issues/38

Do not describe v0.1.4 as available from F-Droid until the build/reproducibility problem is resolved and the public package page actually publishes version 104 or a later accepted version.

## Reproducible-release requirements

Before a future production release intended for F-Droid reproducible-binary verification:

- build from a completely fresh Git clone at the exact release tag/commit;
- use JDK 17, Gradle 8.13, Android SDK Platform 36, and Build-Tools 35.0.0;
- use a fresh or isolated Gradle user home so stale build caches cannot affect evidence;
- do not depend on `.git` presence for APK contents; AGP VCS metadata is disabled in the maintained build configuration;
- compare an unsigned release from the fresh Git checkout with a second build from source without `.git` and require identical APK content before signing;
- keep production signing separate and perform it only after the unsigned reproducibility gate passes;
- never replace a published canonical APK silently; any artifact replacement or new release remains an explicit maintainer-controlled publication action.

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
