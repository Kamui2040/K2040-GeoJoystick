# GeoJoystick

GeoJoystick is an open-source Android mock-location joystick for emulator and developer testing.

It uses Android's standard mock-location provider flow and manual Developer Options selection. While simulation is active, a foreground service publishes user-selected test locations and a floating joystick can move them. GeoJoystick does not attempt to conceal or bypass Android mock-location status.

## Features

- Manual latitude, longitude, and altitude entry
- Built-in OpenStreetMap picker with bundled HTML/CSS/JavaScript and visible attribution
- Two-finger pinch-to-zoom, drag-to-pan, zoom controls, and deliberate tap-to-place behavior
- Optional explicit-submit place/address search through Android's geocoding implementation
- Import of coordinates from supported map links, with local parsing first and bounded HTTPS resolution when required
- Fail-closed validation for malformed, ambiguous, unsupported, non-finite, or out-of-range location input
- Floating joystick with expanded and compact layouts
- Walk, run, bike-style, and user-defined custom speed presets
- Hold, pause, hide, and stop controls
- Optional restore of the last successfully published position
- Five named favorite-location slots
- Overlay size, opacity, high-contrast, and reset-position controls
- System, Light, and Dark appearance
- System language plus English, German, French, Spanish, Italian, Dutch, Danish, Swedish, Norwegian Bokmål, Polish, Turkish, Ukrainian, Russian, Korean, Simplified Chinese, Traditional Chinese, and Arabic
- Responsive layouts for enlarged text, narrow screens, longer translations, and RTL content
- First-run onboarding plus About, Changelog, License & usage, and Sources information
- Android 8.1 / API 27 minimum and Android 16 / API 36 target support
- No ads, accounts, analytics, tracking, subscriptions, billing, or proprietary updater

## Current release

The current canonical production release is **GeoJoystick v0.1.5** (`versionCode 105`, package `com.k2040.geojoystick`).

- Release source: `05762c49662ed4f280e3f42ebcfc7e25d1a2a5d5`
- APK: `GeoJoystick-v0.1.5.apk`
- APK SHA-256: `f8aa3edde469941993450511c1996501f782aeca7f6b6ee17cb5a4498859f2c0`
- Release: https://github.com/Kamui2040/K2040-GeoJoystick/releases/tag/v0.1.5
- Direct APK: https://github.com/Kamui2040/K2040-GeoJoystick/releases/download/v0.1.5/GeoJoystick-v0.1.5.apk

GitHub Releases is the authoritative source for published developer APKs and release notes. `main` can contain later documentation, metadata, or development changes without representing a new runtime release.

## Downloads and project pages

- GitHub Releases: https://github.com/Kamui2040/K2040-GeoJoystick/releases
- F-Droid: https://f-droid.org/packages/com.k2040.geojoystick/
- APKPure: https://apkpure.com/p/com.k2040.geojoystick
- ONE Store: https://m.onestore.net/en-us/apps/appsDetail?prodId=0001008367
- K2040 Android Projects: https://kamui2040.github.io/K2040-Android-Releases/apps/geojoystick/

Downstream stores and F-Droid update on their own schedules. Check each distribution page for the version currently available there.

## Build locally

The maintained build entry point is:

```sh
python3 tools/build.py
```

The current pinned build baseline is:

- JDK 17
- Android SDK Platform 36
- Android SDK Build-Tools 35.0.0
- Android Gradle Plugin 8.12.3
- Gradle 8.13
- Python 3

`tools/build.py` locates the Android SDK and a suitable JDK 17 installation, obtains the pinned Gradle distribution when necessary, verifies its published SHA-256 checksum, and uses the pinned Android SDK inputs. A normal invocation builds the debug APK and writes:

```text
dist/GeoJoystick-debug.apk
dist/SHA256SUMS.txt
```

For the reproducibility/signing requirements used by release work and F-Droid developer-binary verification, see `FDROID_NOTES.md`.

Maintainer release validation is performed on Linux/Bazzite. The public source remains machine-independent, but other host environments are not part of the maintained release-validation path.

## Basic setup

1. Install the APK.
2. Open GeoJoystick and grant **Display over other apps**.
3. In Android Developer Options, select **GeoJoystick** under **Select mock location app**.
4. Enter coordinates, choose a point on the map, import a supported map link, or optionally submit a place/address search.
5. Press **Start simulation** and use the floating joystick.
6. Use **Settings** for appearance, language, overlay controls, restore-last-position, favorites, and custom speed.

The app uses ordinary Android mock locations and does not attempt to conceal that status.

## Network and privacy

GeoJoystick has no developer-operated server, account system, analytics, advertising, or telemetry. Saved coordinates, favorites, and settings stay in the app's private local storage unless the user explicitly invokes a documented external action.

Network access is feature-driven and user initiated:

- Opening the map downloads OpenStreetMap tiles. The map UI itself is bundled with the app and does not load remote JavaScript or require an API key.
- Place/address search runs only when **Search** is submitted and uses Android's geocoding implementation, which may use a network service depending on the device.
- Supported map links are parsed locally when possible. A link that requires resolution can trigger a bounded HTTPS request with redirect, host, public-address, size, and timeout checks.
- External project, licence, support, or map links open in another installed app or browser.

Invalid or failed external input leaves the current location selection unchanged; GeoJoystick never substitutes a fallback real-world coordinate.

See `PRIVACY.md` for the maintained privacy and network disclosure.

## Licence and attribution

- Application code: `GPL-3.0-only`; see `LICENSE`.
- K2040-authored GPL code explicitly marked in its source file also carries the narrowly scoped GPLv3 section 7(b) attribution-preservation term in `LICENSES/GPL-3.0-Section-7b-K2040.txt`.
- Original artwork and UI artwork authored by K2040 and identified by project provenance: `CC-BY-4.0`; see `LICENSES/CC-BY-4.0.txt` and `NOTICE.md`.
- GoGoGo-derived and other third-party code, assets, dependencies, and data retain their controlling licences, notices, and attribution.
- OpenStreetMap data is © OpenStreetMap contributors and licensed under ODbL 1.0.

`NOTICE.md` owns the detailed provenance and attribution scope.

## Support and links

Source repository: https://github.com/Kamui2040/K2040-GeoJoystick

Issue tracker: https://github.com/Kamui2040/K2040-GeoJoystick/issues

Privacy policy: https://github.com/Kamui2040/K2040-GeoJoystick/blob/main/PRIVACY.md

Optional support: https://ko-fi.com/k2040

Donations are optional and do not unlock features or provide additional app benefits.
