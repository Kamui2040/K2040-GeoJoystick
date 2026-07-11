# Project Context

## Purpose

GeoJoystick is an open-source Android mock-location joystick intended for emulator and developer testing. It provides a floating joystick, speed presets, manual coordinates, an internal map picker, map-link coordinate import, and a persistent foreground service.

## Current milestone

Version 0.1.3 release candidate

- Current public release: v0.1.2
- Release candidate: v0.1.3
- Version code: 103
- F-Droid submission MR !42238 has been merged into fdroid/fdroiddata master
- F-Droid currently publishes v0.1.2 from app commit `154292316bf10b02cb3c2cf89476fa3c103a64dd`
- Standard Android Developer Options mock-location provider
- Foreground service publishing GPS and network test-provider locations
- Movable overlay joystick with expanded and compact modes
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

## F-Droid / FLOSS status

The app is GPL-3.0-only, has no ads, no analytics, no accounts, and uses direct Android framework code. Upstream Fastlane-style metadata exists under `fastlane/metadata/android/en-US/`.

The F-Droid submission has been merged and the first store listing is live. The final F-Droid metadata includes `AntiFeatures: TetheredNet` because the map picker uses OpenStreetMap services.

## Validation status

- F-Droid MR build/test/rewritemeta pipeline passed after the TetheredNet metadata update
- Reproducible-build issue was fixed by signing the F-Droid CI-built unsigned APK with the release key using v2 signing and preserved alignment
- Physical-phone smoke test confirmed the public release installs and standard mock location works
- Dark-dialog fix was validated on a physical phone and merged into `main`
- Dedicated GeoJoystick store icon prepared for v0.1.3

## Current build

- Version: 0.1.3
- Version code: 103
- Baseline: public / F-Droid release line
- Release changes: dark-theme dialog fix and dedicated store-listing icon

## Canonical public repository

https://github.com/Kamui2040/K2040-GeoJoystick
