# GeoJoystick

GeoJoystick is an open-source Android mock-location joystick for emulator and developer testing.

It uses Android's standard mock-location provider flow, shows a small floating joystick overlay, and publishes GPS/network test-provider locations while the foreground service is active. It does not attempt to hide or bypass mock-location status.

## Included

- Manual latitude, longitude, and altitude entry
- Built-in OpenStreetMap tile-based picker using bundled HTML/CSS/JavaScript, with no remote JavaScript or API key
- Import of coordinates from copied/shared supported map links
- Floating joystick over other apps
- Expanded and compact overlay modes
- Walk, run, bike-style, and user-defined custom speed presets
- Hold, pause, hide, and stop controls
- Saved overlay mode and speed preset between starts
- App appearance setting: System, Light, or Dark
- App language setting: System, English, or German
- Optional restore of the last successfully published position
- Five compact named favorite-location slots
- Overlay opacity and high-contrast settings
- Reset overlay position from the main screen
- Foreground notification while the simulation service is running
- First-run onboarding plus About, Changelog, License & usage, and Sources information
- No ads, subscriptions, accounts, analytics, billing, or updater

## Current release

The current canonical release is **GeoJoystick v0.1.4** (`versionCode 104`).

- Release: https://github.com/Kamui2040/K2040-GeoJoystick/releases/tag/v0.1.4
- APK: https://github.com/Kamui2040/K2040-GeoJoystick/releases/download/v0.1.4/GeoJoystick-v0.1.4.apk

GitHub Releases is the authoritative source for published release notes and developer-signed APKs. Development `main` may contain later documentation or store-metadata changes that are not a new runtime release.

## Downloads and project pages

- GitHub Releases: https://github.com/Kamui2040/K2040-GeoJoystick/releases
- F-Droid: https://f-droid.org/packages/com.k2040.geojoystick/
- APKPure: https://apkpure.com/p/com.k2040.geojoystick
- K2040 Android Projects: https://kamui2040.github.io/K2040-Android-Releases/apps/geojoystick/

Downstream stores may update on a different schedule from the canonical GitHub release. Mutable publication status is tracked in `PROJECT_CONTEXT.md` and the repository issues rather than duplicated here.

## Build locally

The project intentionally uses a small Python bootstrap instead of committing a Gradle wrapper binary. The bootstrap supports Linux, macOS, and Windows.

Requirements:

- JDK 17
- Android SDK Platform 36
- Android SDK Build-Tools 35.0.0 or newer compatible stable version
- Python 3

On Linux or macOS:

```sh
JAVA_HOME=/path/to/jdk17 python3 tools/build.py
```

On Windows, either run:

```text
build.bat
```

or invoke the bootstrap directly with Python.

The bootstrap locates the Android SDK and JDK, downloads Gradle 8.13 from the official Gradle distribution service when needed, verifies the published SHA-256 checksum, selects a compatible installed stable Build-Tools version, builds the debug APK, and copies it to:

```text
dist/GeoJoystick-debug.apk
```

A matching SHA-256 is written to `dist/SHA256SUMS.txt`.

## Basic setup

1. Install the APK.
2. Open GeoJoystick and grant **Display over other apps**.
3. In Android Developer options, select **GeoJoystick** as the mock-location app.
4. Enter coordinates, choose a location on the map, or import a supported map link.
5. Press **Start simulation**, then use the floating overlay.
6. Use **Settings** for appearance, language, setup actions, overlay opacity, high contrast, restore-last-position, reset overlay position, favorites, and custom speed.

The app uses ordinary Android mock locations and does not attempt to conceal that status.

## Network and map note

The built-in picker loads OpenStreetMap map tiles only when the map is used. Its HTML, CSS, and JavaScript are bundled with the app; it does not load remote JavaScript and does not require an API key. OpenStreetMap attribution is preserved in the picker.

Coordinate import accepts supported HTTPS links from Google Maps, Apple Maps, and OpenStreetMap. Coordinates embedded directly in a supported link are parsed locally. When a supported link needs resolution, the app performs a bounded HTTPS request with redirect, size, timeout, host, and public-address checks; unsupported or invalid input is rejected rather than replaced with a fallback location.

For F-Droid-specific state and reproducibility notes, see `FDROID_NOTES.md`.

## Support

GeoJoystick is created by **K2040**.

If you find the app useful, you can support development on Ko-fi:

`https://ko-fi.com/k2040`

The Ko-fi link is optional and the app has no paid features, subscriptions, ads, analytics, accounts, or billing.

## Licence

- Application code: `GPL-3.0-only`; see `LICENSE`.
- K2040-authored code explicitly marked in its source file: `GPL-3.0-only` plus the narrowly scoped GPLv3 section 7(b) attribution-preservation term in `LICENSES/GPL-3.0-Section-7b-K2040.txt`.
- Original artwork and UI artwork authored by K2040 and identified by project provenance: `CC-BY-4.0`; see `LICENSES/CC-BY-4.0.txt` and `NOTICE.md`.
- Third-party code, assets, dependencies, and data retain their own controlling licences, notices, and attribution.

See `NOTICE.md` for the current marked §7(b) scope, GoGoGo provenance, K2040 artwork attribution, and OpenStreetMap licensing information.

## Links

Source repository: https://github.com/Kamui2040/K2040-GeoJoystick

Support development: https://ko-fi.com/k2040
