# Project Context

Updated: 2026-08-25

## Purpose

GeoJoystick is an open-source Android mock-location joystick for emulator and developer testing.

It uses Android's standard mock-location provider flow and manual Developer Options selection. It does not attempt to conceal or bypass mock-location status.

The app remains ad-free, account-free, analytics-free, tracking-free, subscription-free, and free of mandatory proprietary services. Saved coordinates, favorites, and settings remain local unless the user explicitly invokes a documented external action.

## Current canonical release

The current canonical production release is **GeoJoystick v0.1.4 (`versionCode 104`)**.

- Tag: `v0.1.4`
- Release source commit: `0c3ae37501660300e4f23c45aeb07cffb68e62f9`
- APK: `GeoJoystick-v0.1.4.apk`
- APK SHA-256: `2f6ce92f8b3bbe33dde16e1aef0254a35c939a5382ac108d1a580a0eb05c83d0`

GitHub Releases is the authoritative source for published developer APKs and release notes.

GeoJoystick v0.1.4 has also been published on ONE Store. Availability may vary by supported region or distribution channel.

## Accepted product state

v0.1.4 includes:

- strict latitude, longitude, and altitude validation
- built-in OpenStreetMap picker
- supported map-link coordinate import
- floating joystick with expanded and compact layouts
- walk, run, bike-style, and custom speed presets
- hold, pause, hide, and stop controls
- explicit active and failure state handling
- optional restore of the last successfully published position
- five named favorite-location slots
- overlay opacity, high-contrast controls, and reset-position action
- System/Light/Dark appearance
- System/English/German language selection
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

Additional UI localization is being developed for a future release.

Normal public issues and pull requests may track technical bugs, compatibility work, localization expansion, reproducibility improvements, and other contributor-relevant changes.

## Licensing and provenance

- Application code: `GPL-3.0-only`
- applicable K2040-authored material retains its documented attribution terms
- established K2040 artwork: `CC-BY-4.0`
- third-party material retains its controlling licences and notices
- OpenStreetMap data: © OpenStreetMap contributors, ODbL 1.0

`NOTICE.md` owns detailed provenance and attribution scope.
