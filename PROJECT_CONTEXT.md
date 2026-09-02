# Project Context

Updated: 2026-09-02

## Purpose

GeoJoystick is an open-source Android mock-location joystick for emulator and developer testing.

It uses Android's standard mock-location provider flow and manual Developer Options selection. It does not attempt to conceal or bypass mock-location status.

The app remains ad-free, account-free, analytics-free, tracking-free, subscription-free, and free of mandatory proprietary services. Saved coordinates, favorites, and settings remain local unless the user explicitly invokes a documented external action.

## Current canonical release

The current canonical production release is **GeoJoystick v0.1.5 (`versionCode 105`)**.

- Tag: `v0.1.5`
- Release source commit: `05762c49662ed4f280e3f42ebcfc7e25d1a2a5d5`
- APK: `GeoJoystick-v0.1.5.apk`
- APK SHA-256: `f8aa3edde469941993450511c1996501f782aeca7f6b6ee17cb5a4498859f2c0`

GitHub Releases is the authoritative source for published developer APKs and release notes.

Downstream stores and F-Droid may update on a different schedule from the canonical GitHub release.

## Accepted product state

v0.1.5 includes:

- strict latitude, longitude, and altitude validation
- built-in OpenStreetMap picker
- supported map-link coordinate import
- optional explicit-submit place/address search with fail-closed handling
- two-finger pinch-to-zoom in the map picker
- floating joystick with expanded and compact layouts
- walk, run, bike-style, and custom speed presets
- hold, pause, hide, and stop controls
- explicit active and failure state handling
- optional restore of the last successfully published position
- five named favorite-location slots
- overlay opacity, high-contrast controls, and reset-position action
- System/Light/Dark appearance
- System plus English, German, French, Spanish, Italian, Dutch, Danish, Swedish, Norwegian Bokmål, Polish, Turkish, Ukrainian, Russian, Korean, Simplified Chinese, Traditional Chinese, and Arabic UI languages
- responsive fixes for enlarged text, narrow layouts, and localized content
- first-run onboarding
- About, Changelog, License & usage, and Sources information
- Android 16 / API 36 compatibility
- no concealment of Android mock-location status

Invalid, missing, malformed, non-finite, out-of-range, ambiguous, or unsupported coordinate input fails closed.

## Build baseline

Current maintained development baseline:

- JDK 17
- Android SDK Platform 36
- `compileSdk 36`
- `targetSdk 36`
- `minSdk 27`
- Android Gradle Plugin 8.12.3
- Gradle 8.13
- Android SDK Build-Tools 35.0.0

Public builds must remain reproducible from public source without private infrastructure.

## Current development

GeoJoystick v0.1.5 is published on GitHub from the exact frozen source commit `05762c49662ed4f280e3f42ebcfc7e25d1a2a5d5`.

The reproducible unsigned release APK SHA-256 is `2b170f39504f4cae64eb4bda2b519615f2cf3bae929c32cc9c97921bffd54991`. Integrated host and physical API 36 QA passed on the corresponding release-candidate state. The production APK uses the same signing certificate as v0.1.4, is signed with v1=false, v2=true, v3=true, v4=false and `--alignment-preserved`, and passes byte-for-byte F-Droid signature-copy reconstruction with `apksigcopier` 1.1.1.

The `v0.1.5` tag is public and points to the frozen release source, so the existing F-Droid tag-based update path has been triggered. F-Droid publication and other storefront updates remain downstream distribution state and may appear later.

Normal public issues and pull requests may track technical bugs, compatibility work, localization expansion, reproducibility improvements, and other contributor-relevant changes.

## Licensing and provenance

- Application code: `GPL-3.0-only`
- applicable K2040-authored material retains its documented attribution terms
- established K2040 artwork: `CC-BY-4.0`
- third-party material retains its controlling licences and notices
- OpenStreetMap data: © OpenStreetMap contributors, ODbL 1.0

`NOTICE.md` owns detailed provenance and attribution scope.
