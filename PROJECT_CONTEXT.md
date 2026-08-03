# Project Context

## Purpose

GeoJoystick is an open-source Android mock-location joystick for emulator and developer testing. It uses Android's standard Developer Options mock-location flow and a visible foreground service.

## Current public release

- Version: 0.1.3
- Version code: 103
- Package: `com.k2040.geojoystick`
- Licence: GPL-3.0-only
- Source: `https://github.com/Kamui2040/K2040-GeoJoystick`
- F-Droid: `https://f-droid.org/packages/com.k2040.geojoystick/`
- APKPure: `https://apkpure.com/p/com.k2040.geojoystick`

GitHub Releases is the canonical source for developer-published release assets and release notes.

## Product baseline

- Manual latitude, longitude, and altitude
- OpenStreetMap-based map picker with bundled HTML and JavaScript
- Coordinate import from supported copied or shared map links
- Floating joystick with compact and expanded modes
- Walk, run, bike-style, and custom speed presets
- Hold, pause, hide, and stop controls
- Named favorites and optional position restoration
- Overlay opacity, contrast, and position settings
- System, Light, and Dark appearance
- System, English, and German language
- Persistent foreground notification with a reliable stop action
- No ads, billing, subscriptions, accounts, analytics, tracking, or updater

GeoJoystick does not conceal Android mock-location status and is not presented as game, cheating, anti-detection, integrity-bypass, or ban-evasion tooling.

## Current source baseline

Verify exact values from the live build and manifest files before release.

- Minimum SDK: 27
- Compile SDK: 35
- Target SDK: 35
- Java source and target compatibility: 17
- Gradle distribution: 8.13
- Preferred Android Build-Tools: 35.0.0
- `MainActivity`: exported for launcher and reviewed `text/plain` sharing
- `MapActivity`: non-exported
- `MockLocationService`: non-exported
- Cleartext traffic: disabled
- App backup: disabled

The map picker retrieves OpenStreetMap tiles only when opened. Supported HTTPS map links may use the network when the user explicitly imports them. F-Droid therefore declares `TetheredNet`.

## Upstream relationship

The project is informed by `ZCShou/GoGoGo`, baseline commit `de0d596190c57b8ca71481f60ce6b9e50af5107f`, under GPL-3.0-only.

The Baidu SDK, related native binaries, embedded signing, updater, logging stack, history database, and legacy permissions are not included.

## Public documentation policy

Public changelogs contain only changes that affect people using GeoJoystick. Internal workflow preferences, machine-specific setup, device-specific QA notes, long build logs, and private release evidence are maintained outside the public repository.
