# F-Droid preparation notes

This repository is being prepared for F-Droid or similar FLOSS Android repositories.

## Ready / intended

- GPL-3.0-only source license
- No ads
- No analytics or tracking SDKs
- No accounts
- No subscriptions or paid features
- No proprietary Android dependencies in the Gradle build
- Command-line build path through `build.bat` / `tools/build.py`
- Upstream Fastlane-style metadata under `fastlane/metadata/android/en-US/`
- Optional Ko-fi support link only; the app remains fully functional without it

## Before F-Droid submission

- Publish the canonical source repository.
- Add real release tags, e.g. `v0.1.17` for `versionName '0.1.17-public-preview'` / `versionCode 17`.
- Capture neutral app screenshots and place them under `fastlane/metadata/android/en-US/images/phoneScreenshots/`.
- The WebView map picker uses local source code and does not load external JavaScript. It downloads map tile images from OpenStreetMap when the picker is opened.
- Review whether OpenStreetMap tile and map-link-resolution network usage needs an F-Droid metadata note.
- Confirm the public source repository is reachable before submitting fdroiddata metadata.

## Draft fdroiddata fields

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
