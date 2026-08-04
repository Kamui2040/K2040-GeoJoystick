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
- App appearance: System, Light, or Dark
- App language: System, English, or German
- Optional restore of the last active position
- Five compact named favorite-location slots
- Overlay opacity and high-contrast settings
- Reset overlay position from the main screen
- Persistent foreground notification
- No ads, subscriptions, accounts, analytics, tracking, billing, paid entitlements, or updater

## Downloads

- F-Droid: `https://f-droid.org/packages/com.k2040.geojoystick/`
- GitHub Releases: `https://github.com/Kamui2040/K2040-GeoJoystick/releases`
- Additional package listing: `https://apkpure.com/p/com.k2040.geojoystick`

GitHub Releases is canonical for developer-published assets and release notes. F-Droid is the official FLOSS source-built distribution. APKPure is an additional package listing for `com.k2040.geojoystick`; verify release identity against the canonical GitHub/F-Droid sources.

## Build locally

The project uses a small Python bootstrap instead of committing a Gradle wrapper binary.

From the repository root, run:

- `build.bat` — build the debug APK
- `build.bat --release` — run release lint and build the unsigned release APK
- `build.bat --all` — build debug and unsigned release APKs and run release lint

Equivalent Python entry point:

`python tools\build.py`

The bootstrap first runs the dependency-free map-link parser self-test, then locates a suitable JDK and Android SDK, installs missing SDK components when command-line tools are available, downloads Gradle from the official distribution service, verifies the published SHA-256 checksum, and writes outputs to `dist`.

Expected outputs:

- `dist\GeoJoystick-debug.apk`
- `dist\GeoJoystick-release-unsigned.apk`
- `dist\SHA256SUMS.txt`

Signing is intentionally separate. Do not commit APKs, keys, credentials, `local.properties`, or generated output.

## Basic setup

1. Install the APK.
2. Open GeoJoystick and grant **Display over other apps**.
3. In Android Developer options, select **GeoJoystick** as the mock-location app.
4. Choose a location or import a map link.
5. Press **Start overlay**, then use the floating overlay.
6. Use **Settings** for appearance, language, setup actions, overlay opacity, high contrast, restoration, reset position, favorites, and custom speed.

The app uses ordinary Android mock locations and does not conceal that status.

## Map and network note

The picker uses bundled HTML/JavaScript and OpenStreetMap tiles. It does not require an API key or load remote JavaScript. Opening the map and resolving supported HTTPS map links can use the network. Navigation is restricted to the bundled map origin and the approved OpenStreetMap tile host.

F-Droid therefore discloses the map picker under `TetheredNet`.

Do not bulk-download or abusively cache map tiles. Preserve OpenStreetMap attribution and usage-policy compliance.

For F-Droid maintenance and reproducible builds, see `FDROID_NOTES.md`.

## Security

Report potential vulnerabilities through the private route described in `SECURITY.md`. Do not publish sensitive details, private coordinates, shared links, or exploit steps in public issues.

## Support

GeoJoystick is created by **K2040**.

Optional support:

`https://ko-fi.com/k2040`

Donations are entirely optional. They do not unlock features or provide any additional benefits.

The app has no paid features, subscriptions, advertisements, analytics, tracking, accounts, billing, or restricted functionality.

## Licence

GPL-3.0-only. See `LICENSE` and `NOTICE.md`.

## Links

- Source: `https://github.com/Kamui2040/K2040-GeoJoystick`
- Changelog: `CHANGELOG.md`
- Contributing: `CONTRIBUTING.md`
- F-Droid: `https://f-droid.org/packages/com.k2040.geojoystick/`
- Privacy: `PRIVACY.md`
- Security: `SECURITY.md`
- Support: `https://ko-fi.com/k2040`
