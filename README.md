# GeoJoystick

GeoJoystick is an open-source Android mock-location joystick for emulator and developer testing.

It uses Android's standard mock-location provider flow, shows a small floating joystick overlay, and publishes GPS/network test-provider locations while the foreground service is active. It does not attempt to hide or bypass mock-location status.

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
- No ads, subscriptions, accounts, analytics, billing, or updater

## Downloads

- GitHub Releases: https://github.com/Kamui2040/K2040-GeoJoystick/releases
- APKPure: https://apkpure.com/p/com.k2040.geojoystick

GitHub Releases remains the canonical source for release notes and published release assets. The APKPure page is an official store listing for the same app package, `com.k2040.geojoystick`.

## Build on Windows

The project intentionally uses a small Python bootstrap instead of committing a Gradle wrapper binary.

1. Keep the checkout at `D:\Projects\Android\K2040-GeoJoystick\repo`.
2. Run `build.bat`.

The script uses the newest installed stable Build-Tools version 35.0.0 or newer. If Platform 35 or suitable Build-Tools are missing and Android SDK Command-line Tools are installed, it installs the required SDK packages automatically.

The script locates Android Studio's JDK and Android SDK, downloads Gradle 8.13 from the official Gradle distribution service, verifies its published SHA-256 checksum, builds the debug APK, and copies it to:

`dist\GeoJoystick-debug.apk`

## Basic setup

1. Install the APK.
2. Open GeoJoystick and grant **Display over other apps**.
3. In Android Developer options, select **GeoJoystick** as the mock-location app.
4. Choose a location or import a map link.
5. Press **Start overlay**, then use the floating overlay.
6. Use **Settings** for app appearance, language, setup actions, overlay opacity, high contrast, restore-last-position, reset overlay position, favorites, and custom speed.

The app uses ordinary Android mock locations and does not attempt to conceal that status.

## Map note

The built-in picker uses OpenStreetMap tiles and Leaflet. It does not require an API key. Map links can be shared or copied into the app for coordinate extraction.

For F-Droid submission prep, see `FDROID_NOTES.md`.

## Support

GeoJoystick is created by **K2040**.

If you find the app useful, you can support development on Ko-fi:

`https://ko-fi.com/k2040`

The Ko-fi link is optional and the app has no paid features, subscriptions, ads, analytics, accounts, or billing.

## Licence

GPL-3.0-only. See `LICENSE` and `NOTICE.md`.

## Links

Source repository: https://github.com/Kamui2040/K2040-GeoJoystick

Support development: https://ko-fi.com/k2040
