# GeoJoystick

GeoJoystick is an open-source Android mock-location joystick for emulator and developer testing.

It uses Android's standard mock-location provider flow, shows a floating joystick overlay, and publishes GPS/network test-provider locations while the foreground service is active. It does not attempt to hide or bypass mock-location status.

## Included

- Manual latitude, longitude, and altitude entry
- Built-in OpenStreetMap tile-based picker with no external JavaScript dependency
- Import of coordinates from copied/shared map links
- Floating joystick over other apps
- Expanded and compact overlay modes
- Walk, run, bike-style, and user-defined custom speed presets
- Hold, pause, hide, and stop controls
- Saved overlay mode and speed preset between starts
- App appearance setting: System, Light, or Dark
- App language setting: System, English, or German
- Optional restore of the last active position
- Five compact named favorite-location slots
- Overlay opacity and high-contrast settings
- Reset overlay position from the main screen
- Persistent foreground notification
- No ads, subscriptions, accounts, analytics, tracking, billing, paid entitlements, or updater

## Downloads

- F-Droid: https://f-droid.org/packages/com.k2040.geojoystick/
- GitHub Releases: https://github.com/Kamui2040/K2040-GeoJoystick/releases
- APKPure: https://apkpure.com/p/com.k2040.geojoystick

GitHub Releases is the canonical source for release notes and developer-published release assets. F-Droid is the official FLOSS source-built distribution. APKPure is an official listing for the same package, `com.k2040.geojoystick`.

## Build on Windows

The project intentionally uses a small Python bootstrap instead of committing a Gradle wrapper binary.

1. Keep the checkout at `D:\Projects\Android\K2040-GeoJoystick\repo`.
2. Run `build.bat`.

The current public build script locates Android Studio's JDK and Android SDK, downloads Gradle 8.13 from the official Gradle distribution service, verifies its published SHA-256 checksum, builds the debug APK, and copies it to:

`dist\GeoJoystick-debug.apk`

The current public local bootstrap is debug-only. Maintainer release validation also requires a repository-owned local unsigned-release and release-lint mode. GitHub Actions is not used as a substitute. See `FDROID_NOTES.md` and `PROJECT_CONTEXT.md` for the current maintenance state.

## Basic setup

1. Install the APK.
2. Open GeoJoystick and grant **Display over other apps**.
3. In Android Developer options, select **GeoJoystick** as the mock-location app.
4. Choose a location or import a map link.
5. Press **Start overlay**, then use the floating overlay.
6. Use **Settings** for app appearance, language, setup actions, overlay opacity, high contrast, restore-last-position, reset overlay position, favorites, and custom speed.

The app uses ordinary Android mock locations and does not attempt to conceal that status.

## Map and network note

The built-in picker uses bundled Leaflet code and OpenStreetMap tiles. It does not require an API key or load remote JavaScript. Opening the map and resolving supported map links can use the network.

F-Droid therefore discloses the map picker under `TetheredNet`. Do not bulk-download or abusively cache map tiles. Preserve OpenStreetMap attribution and usage-policy compliance.

For F-Droid maintenance and reproducible-build requirements, see `FDROID_NOTES.md`.

## Privacy

GeoJoystick stores coordinates, favorites and preferences locally. Network access occurs only for explicit map display or map-link resolution. See [PRIVACY.md](PRIVACY.md) for the complete disclosure.

## Support

GeoJoystick is created by **K2040**.

Support development on Ko-fi:

https://ko-fi.com/k2040

The Ko-fi link is optional. The app has no paid features, subscriptions, advertisements, analytics, accounts, billing, or restricted functionality.

## Licence

GPL-3.0-only. See `LICENSE` and `NOTICE.md`.

## Links

- Source repository: https://github.com/Kamui2040/K2040-GeoJoystick
- F-Droid: https://f-droid.org/packages/com.k2040.geojoystick/
- Privacy policy: [PRIVACY.md](PRIVACY.md)
- Support development: https://ko-fi.com/k2040
