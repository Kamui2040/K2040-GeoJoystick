# F-Droid maintenance notes

Last verified: 2026-08-04

## Published package

- Package: `com.k2040.geojoystick`
- Licence: GPL-3.0-only
- Current F-Droid version: 0.1.3 (103)
- Initial F-Droid version: 0.1.2 (102)
- fdroiddata merge request: !42238
- Listing: `https://f-droid.org/packages/com.k2040.geojoystick/`
- Source: `https://github.com/Kamui2040/K2040-GeoJoystick`

## Inclusion and disclosure baseline

- No ads, analytics, tracking, accounts, billing, subscriptions, or paid features
- No proprietary Android dependencies or in-app updater
- Direct Android framework implementation
- Fastlane metadata under `fastlane/metadata/android/en-US/`
- Optional Ko-fi donations do not unlock features or benefits
- `AntiFeatures: TetheredNet` while the map picker retrieves OpenStreetMap tiles

The map HTML and JavaScript are bundled. No remote script or API key is used. Network access is limited to explicit map display and supported map-link resolution.

## Source build

F-Droid uses standard Gradle discovery. Keep `gradle/wrapper/gradle-wrapper.properties` synchronized with the Gradle version used by `tools/build.py`.

Local build entry points:

- `build.bat` — debug APK
- `build.bat --release` — unsigned release APK plus release lint
- `build.bat --all` — debug and unsigned release APKs plus release lint
- `build.bat --signed-release` — unsigned release plus an explicitly requested, verified signed APK
- `python tools/build.py` — equivalent Python entry point

The local bootstrap downloads Gradle from the official distribution service and verifies its published SHA-256 checksum. Maintainer signing is available only through the explicit `--signed-release` mode and the canonical external `..\secrets\geojoystick-release.jks` keystore. Passwords are entered through hidden prompts for the current process and are not written to files or logs. The bootstrap verifies the accepted keystore identity and certificate fingerprints before signing and verifies the resulting APK independently. The unsigned release output is always retained. Public and F-Droid source builds do not depend on private storage.

## Update metadata

For each version:

- use the full immutable source commit;
- ensure the release tag resolves to that commit;
- synchronize version code, version name, public changelog, icon, screenshots, licence, notice, privacy policy, and security policy;
- keep `AntiFeatures: TetheredNet` while OpenStreetMap services are used;
- keep the versioned reference-binary URL and allowed signing certificate synchronized;
- run canonical `fdroid rewritemeta` and `fdroid lint`;
- verify every Fastlane file referenced by fdroiddata is tracked at the source commit.

## Reproducible reference APK

The developer reference APK must be signed from the exact unsigned APK produced by the corresponding F-Droid build. Do not substitute a local rebuild or modify the APK after signing. The explicitly signed local release is not by itself F-Droid reproducibility evidence; use it as a reference asset only after the unsigned payload has been proven identical to the corresponding F-Droid build.

For the currently accepted process:

- preserve the unsigned APK's alignment;
- use v2 signing only unless F-Droid's verified process changes;
- make no post-signing content changes;
- verify package, version, certificate fingerprint, signing schemes, alignment, and SHA-256;
- require copied-signature and allowed-signer verification before publishing the reference asset.

Current allowed signing-certificate fingerprint:

`e0a833050d7c8fce7ddce85b2a86561304456d87b67bd6be1577d8f657e16778`

Published v0.1.3 source commit:

`e19b1ee13216b2de4f4cd890f00b2adabddd802f`

## Evidence boundaries

A successful local build does not by itself prove reproducibility. A matching certificate does not prove an identical unsigned payload. Verify source, unsigned artifact, signature, public download, and F-Droid checks independently.
