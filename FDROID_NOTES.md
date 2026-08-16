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
- Public canonical source repository on GitHub
- Upstream Fastlane-style text metadata under `fastlane/metadata/android/en-US/`
- Accepted synthetic/sanitized phone screenshots under both `fastlane/metadata/android/en-US/images/phoneScreenshots/` and `fastlane/metadata/android/de-DE/images/phoneScreenshots/`
- Public-safe screenshot provenance and accepted SHA-256 hashes in `fastlane/metadata/android/SCREENSHOT_PROVENANCE.md`
- Optional Ko-fi support link only; the app remains fully functional without it

## Current F-Droid / publication state

- F-Droid currently lists GeoJoystick `0.1.3` (`versionCode 103`) as the suggested version.
- The public F-Droid package page currently displays the accepted app screenshots.
- F-Droid currently discloses the OpenStreetMap-backed map picker with its network-service anti-feature note (`Uses OpenStreetMap services for the map picker`).
- Keep the accepted Issue #12 screenshots synthetic and sanitized. Do not restore or reuse the removed historical phone screenshots that contained authentic precise location material.
- The WebView map picker uses local source code and does not load external JavaScript. It downloads map tile images from OpenStreetMap when the picker is opened.
- Confirm the public source repository and required release/tag inputs are reachable before any future fdroiddata update or release submission.

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
