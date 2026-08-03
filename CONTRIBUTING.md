# Contributing

GeoJoystick is a GPL-3.0-only Android mock-location utility for emulator and developer testing.

## Project boundaries

Contributions must preserve Android's standard mock-location flow and visible foreground-service operation. Do not add concealment, integrity or attestation bypasses, ban evasion, game-specific automation, root or Shizuku requirements, accessibility abuse, injection, tracking, advertisements, accounts, billing, subscriptions, or paid entitlements.

Keep coordinates, favorites, and settings local. Network activity must remain limited to explicit map display and supported map-link actions.

## Implementation

Prefer direct Java and Android framework APIs and deterministic Python scripts. New dependencies must be FLOSS, compatible with F-Droid, and justified by a net reduction in complexity.

Keep `MapActivity` and `MockLocationService` non-exported. Validate external text, links, WebView navigation, JavaScript bridge coordinates, numeric ranges, and longitude normalization. Stopping the service must remove test providers, overlay state, and the foreground notification.

Every user-facing text change requires English and German review.

## Build and checks

From the repository root:

- `build.bat` builds the debug APK.
- `build.bat --release` runs release lint and builds the unsigned release APK.
- `build.bat --all` runs both build variants and release lint.
- `python tools/test_location_link_parser.py` runs the dependency-free parser checks.

Before proposing a change, run applicable tests, `git diff --check`, and review the complete changed and untracked scope.

Do not commit credentials, signing material, `local.properties`, APKs, app bundles, private coordinates, private screenshots, logs, caches, or generated build output.
