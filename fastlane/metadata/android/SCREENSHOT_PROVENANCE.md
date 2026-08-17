# Store screenshot provenance

## Version 0.1.3 accepted store set

The accepted GeoJoystick store screenshot set for version 0.1.3 was captured from a real Android device at source revision `a158a94c5d4dbc341a45bbee381f1c1d79822391` using the maintained Issue #12 capture harness.

### Synthetic capture state

- Package: `com.k2040.geojoystick`
- Version: `0.1.3` (`versionCode 103`)
- Locales: `en-US`, `de-DE`
- Synthetic coordinates only: latitude `51.234567`, longitude `10.123456`, altitude `123.0 m`
- No authentic location history or private favorites were used.
- App preferences and font scale were restored after capture.
- Mock-location app selection was not changed.

### System UI sanitation

The source screenshots were real Android `screencap` PNGs at `1080x2392`. The canonical device reported a `116 px` status-bar inset and a `120 px` navigation-bar inset. The maintained sanitizer removed only those device-reported system-bar rows, producing the accepted `1080x2156` screenshots. Private notifications and other System UI state were not modified on-device.

The overlay screenshots use the real GeoJoystick `TYPE_APPLICATION_OVERLAY` over the debug-only neutral `#ECEFF1` capture activity. The helper is absent from release builds. The capture harness waits an additional 4 seconds before overlay `screencap` so transient start UI has expired.

### Accepted screenshot SHA-256

#### en-US

- `01-home.png`: `55503fe6b43c3637dc1891ba494bf1d2c4265eff897b65455cb2e24c1db9809c`
- `02-map.png`: `f21cd79ac294bd213a44a1acc21f6b61bfdb26bbe8eaefb89a6ab396bcbabcea`
- `03-settings.png`: `59dba9b397053cba2f9ce4a5a03b1553e109b924d9c277c13f8bab51a1f4a374`
- `04-about.png`: `e63d97a4fc9a02b960aee26b489c788798c3bbc288b5d4c0f479dc5ce0d58930`
- `05-overlay.png`: `ddf57702487c7fd193c30ac210393b64c0e85dbacf5fa793656de0403117b3c8`

#### de-DE

- `01-home.png`: `e68e014e6f7b625b415a93ae84ed33ef2bd02f9f6b7026839fca5e27f754ad59`
- `02-map.png`: `b10256c153a5f541bf4c82c01ef464d9842869d924782ea27a88261df1f9862d`
- `03-settings.png`: `dac1f384ff749924ee78424c96097bb84c104e9d7cbdbaa29965be7a6b30db0b`
- `04-about.png`: `872c274ec175d102af6046f1b050a415286f0a87206fc4c60a08b396ccc286ec`
- `05-overlay.png`: `ddf57702487c7fd193c30ac210393b64c0e85dbacf5fa793656de0403117b3c8`

### Visual acceptance

All ten accepted screenshots were reviewed after sanitation. The final set contains no unrelated notification icons or navigation controls, no transient start toast, no authentic/private location data, and no device identifier or local path. English/German localization is consistent, synthetic coordinates are visible where intended, and OpenStreetMap attribution remains visible on both map screenshots.

## Version 0.1.4 accepted store sets

The accepted GeoJoystick version 0.1.4 English/Dark and German/Dark store screenshot sets use the exact installed debug APK built from source revision `0c3ae37501660300e4f23c45aeb07cffb68e62f9` (`versionCode 104`). The English set was captured at that revision. The German set used maintained capture tooling from documentation-only main revision `50398f7c0509243cb7b9b2826d571c0b85e4f04c`; that revision did not change the installed runtime APK. Both sets passed human visual acceptance on 2026-08-17.

### Synthetic capture state

- Package: `com.k2040.geojoystick`
- Version: `0.1.4` (`versionCode 104`)
- Locales: `en-US`, `de-DE`
- Appearance: Dark
- Synthetic coordinates only: latitude `51.234567`, longitude `10.123456`, altitude `123.0 m`
- No authentic location history or private favorites were used.
- App preferences were restored byte-for-byte after capture.
- Font scale was restored after capture.
- Android mock-location app selection and overlay permission were preserved.
- Simulation was inactive before capture and stopped after the synthetic overlay capture.

### Capture and sanitation

For each locale, Main, Settings, and About are genuine Android `screencap` captures from the maintained screenshot harness. The Map image is a genuine Android `screencap` with the real expanded GeoJoystick `TYPE_APPLICATION_OVERLAY` running over the real `MapActivity`; it is not composited and does not use the debug-only neutral overlay background.

The map uses the same fixed synthetic coordinates as the maintained store harness. The live overlay was positioned below the map instruction card so the map controls, instructions, and OpenStreetMap attribution remain readable. Capture waited for the localized map content and exact `© OpenStreetMap contributors` attribution before taking each map screenshot.

All raw captures were `1080x2392`. Android reported a `116 px` status-bar inset and a `120 px` navigation-bar inset. The maintained sanitizer removed only those device-reported system-bar rows, producing the final `1080x2156` images. No app content was composited or AI-generated.

### Accepted screenshot SHA-256

#### en-US

- `01-main.png`: `edbc6d06f7b1e1273ea49c506ec47a1c94c6d035028554fccdae0190e393c7dc`
- `02-settings.png`: `2797dfff2b0eb7f6d0cd1b20ee66fec6fe11a83c28a350f35b34d46940b7390a`
- `03-about.png`: `8d2e784211aeda881c0be5a32d3751facdc891a483a2f2dfce08d7d838ed62f0`
- `04-map-overlay.png`: `55fdd727387f9bc9b2b6a19872f999e228f686a93b86ff531d6288e577e54d28`

#### de-DE

- `01-main.png`: `f2fb2f7dbcbeb2889f7107b7d7c1e44645938e500636c71f2e2d3b0855b79e61`
- `02-settings.png`: `4955c5cdefc9839c5702b226fbf7c3cec99530b4fa4f4eead1ff49be810aa527`
- `03-about.png`: `2c48a8067e484cd77d9cdbcb9f32a3213fd72a30cea8c4cb0be91093f6c916aa`
- `04-map-overlay.png`: `d46901bd866656c5e72458468da0f373b042cf85df857b8e9916d780e17002e7`

### Visual acceptance and Fastlane state

Both final four-image sets passed human visual review after deterministic system-bar sanitation. Main, Settings, and About are readable and unclipped. The Map images preserve the localized instruction card, location controls, zoom controls, OpenStreetMap attribution, and the complete expanded live overlay without overlap of the instruction card.

The accepted four-image sets replace the previous five-image Fastlane phone screenshot sets for both `en-US` and `de-DE`. No unrelated System UI, authentic/private location data, device identifier, or machine-specific path is present in the final images.

This provenance records asset creation, visual acceptance, and Fastlane metadata state only. It does not represent a store, F-Droid, release, signing, or publication submission.
