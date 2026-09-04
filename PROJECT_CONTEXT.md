# Project Context

Updated: 2026-09-04

## Purpose

GeoJoystick is an open-source Android mock-location joystick for emulator and developer testing.

It uses Android's standard mock-location provider flow and manual Developer Options selection. It does not attempt to conceal or bypass mock-location status.

The app is ad-free, account-free, analytics-free, tracking-free, subscription-free, and free of mandatory proprietary services. Saved coordinates, favorites, and settings remain local unless the user explicitly invokes a documented external action.

## Current canonical release

The current canonical production release is **GeoJoystick v0.1.5 (`versionCode 105`)**.

- Package: `com.k2040.geojoystick`
- Tag: `v0.1.5`
- Release source commit: `05762c49662ed4f280e3f42ebcfc7e25d1a2a5d5`
- APK: `GeoJoystick-v0.1.5.apk`
- APK SHA-256: `bdf43cbdde6af2d96dac3c9a68818d79a7090d1f16e8265d83fc2fbcc1f9350b`

GitHub Releases is the authoritative source for published developer APKs and release notes. F-Droid and other storefronts manage their own downstream publication schedules.

## Accepted product state

v0.1.5 includes:

- strict latitude, longitude, and altitude validation
- built-in OpenStreetMap picker with visible attribution
- two-finger pinch-to-zoom, map panning, zoom controls, and deliberate tap-to-place behavior
- supported map-link coordinate import with bounded resolution when required
- optional explicit-submit place/address search through Android's geocoding implementation
- fail-closed handling for malformed, ambiguous, invalid, unsupported, or failed external location input
- floating joystick with expanded and compact layouts
- walk, run, bike-style, and custom speed presets
- hold, pause, hide, and stop controls
- explicit active and failure state handling
- optional restore of the last successfully published position
- five named favorite-location slots
- overlay size, opacity, high-contrast, and reset-position controls
- System/Light/Dark appearance
- System plus English, German, French, Spanish, Italian, Dutch, Danish, Swedish, Norwegian Bokmål, Polish, Turkish, Ukrainian, Russian, Korean, Simplified Chinese, Traditional Chinese, and Arabic UI languages
- responsive handling for enlarged text, narrow layouts, long translations, and RTL content
- first-run onboarding plus About, Changelog, License & usage, and Sources information
- Android 8.1 / API 27 minimum and Android 16 / API 36 target support
- no concealment of Android mock-location status

## Network and privacy model

GeoJoystick has no developer-operated app server, account system, analytics, advertising, or telemetry.

Network activity is optional and tied to explicit features: OpenStreetMap tiles when the map is opened; device geocoding when a place/address search is submitted; bounded HTTPS resolution for supported map links when local parsing is insufficient; and external links opened by the user. `PRIVACY.md` owns the detailed disclosure.

## Build baseline

Current maintained baseline:

- JDK 17
- Android SDK Platform 36
- `compileSdk 36`
- `targetSdk 36`
- `minSdk 27`
- Android SDK Build-Tools 35.0.0
- Android Gradle Plugin 8.12.3
- Gradle 8.13
- maintained build entry point: `python3 tools/build.py`

Public builds must remain reproducible from public source without private infrastructure. `FDROID_NOTES.md` owns the detailed reproducibility and developer-signing requirements.

## Current development state

No post-v0.1.5 runtime release is accepted on `main`. Documentation, metadata, governance, and future development can advance independently without changing the canonical v0.1.5 release identity.

Public issues and pull requests may track technical bugs, compatibility work, localization, reproducibility, and other contributor-relevant changes. Maintainer-only planning, private QA evidence, signing material, and local workspace state do not belong in this file.

## Licensing and provenance

- Application code: `GPL-3.0-only`
- marked K2040-authored GPL material retains its documented GPLv3 section 7(b) attribution term
- established K2040 artwork: `CC-BY-4.0`
- GoGoGo-derived and other third-party material retains its controlling licences and notices
- OpenStreetMap data: © OpenStreetMap contributors, ODbL 1.0

`NOTICE.md` owns the detailed provenance and attribution scope.
