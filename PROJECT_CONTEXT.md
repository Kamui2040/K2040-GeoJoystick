# Project Context

## Purpose

GeoJoystick is an open-source Android mock-location joystick intended for emulator and developer testing. It provides a floating joystick, speed presets, manual coordinates, an internal map picker, map-link coordinate import, and a persistent foreground service.

## Current milestone

Version 0.1.0 F-Droid release candidate

- Standard Android Developer Options mock-location provider
- Foreground service publishing GPS and network test-provider locations
- Movable half-size overlay joystick with expanded and compact modes
- Walk, run, bike-style, and user-defined custom speed presets
- Hold, pause, hide, stop, saved overlay mode, saved speed, overlay opacity, high contrast, and reset overlay position
- App settings for System/Light/Dark appearance and System/English/German language
- Optional restore-last-position behavior and five named favorite-location slots
- OpenStreetMap tile-based picker with no API key and no external JavaScript dependency
- Coordinate extraction from full or shortened map links
- In-app About / Support section with K2040 avatar and Ko-fi link
- No ads, billing, accounts, analytics, subscriptions, or updater

## Upstream relationship

The project reuses and simplifies the architecture and movement approach of `ZCShou/GoGoGo`, baseline commit `de0d596190c57b8ca71481f60ce6b9e50af5107f`, under GPL-3.0-only. The Baidu SDK, embedded signing configuration, updater, logging stack, history database, and legacy permissions are intentionally not carried over.

## Release positioning

Public descriptions should present GeoJoystick as a mock-location utility for emulator and developer testing. Do not market it as game tooling, cheating software, anti-detection tooling, or a bypass utility.

## F-Droid / FLOSS preparation

The app is GPL-3.0-only, has no ads, no analytics, no accounts, and uses direct Android framework code. Upstream Fastlane-style metadata has been added under `fastlane/metadata/android/en-US/`.

Before submitting to F-Droid, capture clean screenshots from a neutral Android/emulator setup and review `FDROID_NOTES.md`, especially the map picker's web assets and network-service metadata considerations.

## Validation status

- Overlay v0.1.15 visuals were tested successfully in BlueStacks
- v0.1.16 persistence changes were prepared as the prior baseline
- Java syntax was checked by parsing with `javac`; Android framework symbols are unavailable in this environment
- Full Gradle/APK build requires a local Android SDK installation

## Current build

- Version: 0.1.25-overlay-drag-spacing-preview
- Change: fixes the license dialog to match the selected app theme and refreshes the overlay control icons with cleaner custom line/silhouette-style drawing for walk, run, bike, and hold/lock states.

## Canonical public repository

https://github.com/Kamui2040/K2040-GeoJoystick
