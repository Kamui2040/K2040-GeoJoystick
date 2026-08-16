# Store screenshot provenance

The accepted GeoJoystick store screenshot set for version 0.1.3 was captured from a real Android device at source revision `a158a94c5d4dbc341a45bbee381f1c1d79822391` using the maintained Issue #12 capture harness.

## Synthetic capture state

- Package: `com.k2040.geojoystick`
- Version: `0.1.3` (`versionCode 103`)
- Locales: `en-US`, `de-DE`
- Synthetic coordinates only: latitude `51.234567`, longitude `10.123456`, altitude `123.0 m`
- No authentic location history or private favorites were used.
- App preferences and font scale were restored after capture.
- Mock-location app selection was not changed.

## System UI sanitation

The source screenshots were real Android `screencap` PNGs at `1080x2392`. The canonical device reported a `116 px` status-bar inset and a `120 px` navigation-bar inset. The maintained sanitizer removed only those device-reported system-bar rows, producing the accepted `1080x2156` screenshots. Private notifications and other System UI state were not modified on-device.

The overlay screenshots use the real GeoJoystick `TYPE_APPLICATION_OVERLAY` over the debug-only neutral `#ECEFF1` capture activity. The helper is absent from release builds. The capture harness waits an additional 4 seconds before overlay `screencap` so transient start UI has expired.

## Accepted screenshot SHA-256

### en-US

- `01-home.png`: `55503fe6b43c3637dc1891ba494bf1d2c4265eff897b65455cb2e24c1db9809c`
- `02-map.png`: `f21cd79ac294bd213a44a1acc21f6b61bfdb26bbe8eaefb89a6ab396bcbabcea`
- `03-settings.png`: `59dba9b397053cba2f9ce4a5a03b1553e109b924d9c277c13f8bab51a1f4a374`
- `04-about.png`: `e63d97a4fc9a02b960aee26b489c788798c3bbc288b5d4c0f479dc5ce0d58930`
- `05-overlay.png`: `ddf57702487c7fd193c30ac210393b64c0e85dbacf5fa793656de0403117b3c8`

### de-DE

- `01-home.png`: `e68e014e6f7d625b415a93ae84ed33ef2bd02f9f6b7026839fca5e27f754ad59`
- `02-map.png`: `b10256c153a5f541bf4c82c01ef464d9842869d924782ea27a88261df1f9862d`
- `03-settings.png`: `dac1f384ff749924ee78424c96097bb84c104e9d7cbdbaa29965be7a6b30db0b`
- `04-about.png`: `872c274ec175d102af6046f1b050a415286f0a87206fc4c60a08b396ccc286ec`
- `05-overlay.png`: `ddf57702487c7fd193c30ac210393b64c0e85dbacf5fa793656de0403117b3c8`

## Visual acceptance

All ten accepted screenshots were reviewed after sanitation. The final set contains no unrelated notification icons or navigation controls, no transient start toast, no authentic/private location data, and no device identifier or local path. English/German localization is consistent, synthetic coordinates are visible where intended, and OpenStreetMap attribution remains visible on both map screenshots.

This provenance records asset creation only. It does not represent a store, F-Droid, release, or publication submission.
