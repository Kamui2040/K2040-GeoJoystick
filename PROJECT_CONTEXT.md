# Project Context

Updated: 2026-08-18

## Purpose

GeoJoystick is an open-source Android mock-location joystick for emulator and developer testing. It uses Android's standard mock-location provider flow and manual Developer Options selection. Public descriptions must not present it as game tooling, cheating software, anti-detection tooling, or a bypass utility.

The app remains ad-free, account-free, analytics-free, tracking-free, subscription-free, and free of mandatory proprietary services. Saved coordinates, favorites, and settings remain local unless the user explicitly performs a documented external action. The built-in map uses OpenStreetMap only when opened, with required attribution preserved.

## Current canonical release

The current canonical production release is **GeoJoystick v0.1.4 (`versionCode 104`)**.

- Tag: `v0.1.4`
- Release source commit: `0c3ae37501660300e4f23c45aeb07cffb68e62f9`
- Release: https://github.com/Kamui2040/K2040-GeoJoystick/releases/tag/v0.1.4
- APK: `GeoJoystick-v0.1.4.apk`
- APK size: `1,754,890` bytes
- APK SHA-256: `f768d9ed09aa16d51a585470804374b52972f34ffb778f615b580de466b1d312`
- Production signer certificate SHA-256: `e0a833050d7c8fce7ddce85b2a86561304456d87b67bd6be1577d8f657e16778`
- APK Signature Scheme v2: verified

GitHub Releases remains the authoritative source for published developer-signed artifacts and release notes.

After the v0.1.4 release source, `main` received only documentation/store-metadata work through the accepted screenshot and store-icon corrections; no Android runtime source changed in those post-release commits. A new production release is therefore not implied by later `main` documentation or Fastlane asset updates.

## v0.1.4 accepted product state

v0.1.4 includes the previously accepted lifecycle/state reconciliation plus the later UI, onboarding, accessibility, mascot, store-metadata, and Android 16 work.

Current accepted behavior includes:

- manual latitude, longitude, and altitude entry with strict validation
- built-in OpenStreetMap picker
- supported map-link coordinate import with bounded network resolution where needed
- floating joystick with expanded and compact layouts
- walk, run, bike-style, and custom speed presets
- hold, pause, hide, and stop controls
- explicit start/active/failure state reconciliation
- optional restore of the last successfully published position
- five named favorite-location slots
- overlay opacity, high-contrast controls, and reset-position action
- System/Light/Dark appearance and System/English/German language selection
- first-run onboarding with Continue as the acknowledgement path
- About, Changelog, License & usage, Sources, GPL, artwork, and OpenStreetMap/ODbL information surfaces
- Android 16 / API 36 back-navigation compatibility
- no concealment of Android mock-location status

Invalid, missing, malformed, non-finite, out-of-range, ambiguous, or unsupported coordinate input fails closed. Parse or validation failure must never substitute a real-world fallback coordinate.

## Build baseline

Current maintained development baseline:

- JDK 17
- Android SDK Platform 36
- `compileSdk 36`
- `targetSdk 36`
- `minSdk 27`
- Android Gradle Plugin 8.12.3
- Gradle 8.13
- Build Tools 35.0.0 or a newer compatible stable version

The repository-owned bootstrap remains the supported build path. Public builds must remain independent of private credentials, signing material, or private infrastructure.

## Store assets and presentation

The accepted current Fastlane/store presentation uses sanitized real-device screenshots with synthetic coordinates only.

- English screenshots: Main, Settings, About, Map + overlay
- German screenshots: Main, Settings, About, Map + overlay
- Screenshot provenance and accepted hashes: `fastlane/metadata/android/SCREENSHOT_PROVENANCE.md`
- Current store icon: 512×512 32-bit RGBA PNG exported directly from the accepted gecko launcher artwork
- Store icon SHA-256: `6bf5a6fd69fb88f1a18e65177addc9ff64abe090bc812e5e0241bb5495383074`

Historical screenshots containing authentic precise location material remain removed and must not be restored.

## Downstream publication state

### GitHub

v0.1.4 is published and is the canonical release.

### K2040 Android Projects

The GeoJoystick Android Projects page is published for v0.1.4. It uses the exact accepted English/German 1024×500 promotional banners rather than reconstructed artwork, and the landing-page Latest / Project Updates entry uses the same localized banner set.

### F-Droid

The public F-Droid package page currently exposes **0.1.3 (`versionCode 103`)** as the suggested version.

F-Droid automatic update processing created fdroiddata MR `!46016` for v0.1.4 / 104, but binary verification failed because the published reference APK and the F-Droid build differ. The reported comparison includes different Android version-control metadata (`NO_VALID_GIT_FOUND` in the reference APK versus an embedded Git revision in the F-Droid build) and a `classes.dex` difference.

Durable defect tracker: GitHub Issue #38, **F-Droid Build Failed**.

Do not claim v0.1.4 is available on F-Droid until the reproducibility/build issue is resolved and version 104 is actually published.

### APKPure

The official APKPure package listing exists for `com.k2040.geojoystick`. The maintainer submitted the v0.1.4 update through the authenticated publisher flow. Treat v0.1.4 as **submitted/pending verification**, not confirmed live, until the public listing is checked successfully.

### OpenAPK

The maintainer submitted GeoJoystick to OpenAPK. Treat this as **submitted/pending review or publication** until a public listing is independently verified.

### Uptodown

The earlier v0.1.3 submission was rejected for low quality and the existing app entry currently prevents a new APK upload. The maintainer refreshed the English/German store descriptions and licensing information for v0.1.4 and submitted a support request asking Uptodown to reopen the existing entry for a new v0.1.4 review. Treat Uptodown as **pending support/review**, not a live v0.1.4 channel.

Durable downstream tracker: GitHub Issue #37, **Complete GeoJoystick v0.1.4 downstream publication**.

## F-Droid/reproducibility boundary

Keep developer-signed GitHub release evidence separate from F-Droid reproducibility evidence. The GitHub v0.1.4 artifact is already published and must not be silently replaced merely to satisfy downstream verification.

`FDROID_NOTES.md` owns the current F-Droid-specific state. Issue #38 owns unresolved remediation work.

## Licensing and provenance

- Application code remains `GPL-3.0-only`.
- K2040-authored GPL code carries the separate GPLv3 section 7(b) attribution-preservation term only where the source file explicitly marks it as applicable.
- Original K2040 artwork and UI artwork with established project provenance is `CC-BY-4.0`, including the bundled K2040 avatar, the GeoJoystick mascot, and the simplified gecko + joystick launcher identity.
- GoGoGo-derived and other third-party code/assets/data retain their own controlling licences, notices, and attribution.
- OpenStreetMap data remains © OpenStreetMap contributors under ODbL 1.0.
- F-Droid metadata discloses the OpenStreetMap-backed map picker with the applicable network-service anti-feature note.

`NOTICE.md` owns detailed provenance and attribution scope.

## Upstream relationship

GeoJoystick reuses and simplifies architecture and movement concepts from `ZCShou/GoGoGo`, baseline commit `de0d596190c57b8ca71481f60ce6b9e50af5107f`, under GPL-3.0-only. The Baidu SDK, embedded signing configuration, updater, logging stack, history database, and legacy permissions are intentionally not carried over.

## Current durable work

Open public issues currently requiring follow-up:

- #37 — Complete GeoJoystick v0.1.4 downstream publication
- #38 — F-Droid Build Failed

Completed implementation/QA history remains available through closed issues and merged PRs. This file intentionally keeps only current state and durable boundaries rather than repeating the full historical integration chronology.

## Validation boundary

The v0.1.4 production release was created from a separately validated source/release workflow. Later accepted store screenshot and store icon changes are presentation metadata only and do not constitute runtime/device validation of a new release.

Documentation-only maintenance does not require an Android rebuild when runtime source, dependencies, and build configuration are unchanged. Any future runtime, build-system, signing, mock-provider, location-input, or network behavior change requires scope-appropriate validation before release claims.

GitHub Actions are not used as PC/Linux validation evidence unless explicitly approved for a separate workflow.

## Publication boundary

Production signing, new tags/releases, F-Droid/store submissions, deployments, announcements, and comparable publication actions remain explicit maintainer-controlled gates unless the current task has already authorized the specific action.
