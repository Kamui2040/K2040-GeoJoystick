# Project Context

Updated: 2026-08-19

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
- APK SHA-256: `2f6ce92f8b3bbe33dde16e1aef0254a35c939a5382ac108d1a580a0eb05c83d0`
- Production signer certificate SHA-256: `e0a833050d7c8fce7ddce85b2a86561304456d87b67bd6be1577d8f657e16778`
- APK Signature Scheme v1: disabled
- APK Signature Scheme v2: enabled
- APK Signature Scheme v3: enabled
- APK Signature Scheme v4: disabled
- Embedded release VCS revision: `0c3ae37501660300e4f23c45aeb07cffb68e62f9`

GitHub Releases remains the authoritative source for published developer-signed artifacts and release notes.

On 2026-08-19 the v0.1.4 release APK was replaced under explicit maintainer approval with the corrected F-Droid-compatible developer-signed artifact produced from the exact accepted F-Droid unsigned v0.1.4 input. The replacement preserves the established production certificate, package/version, accepted `classes.dex`, exact release revision, and alignment; explicitly uses v1 `false`, v2 `true`, v3 `true`, v4 `false`; contains no generated JAR-signing metadata; and passes the exact retained F-Droid signature-copy path with byte-for-byte identity. The published asset was re-downloaded after replacement and independently verified. The tag, source commit, source history, and release notes were unchanged.

The immediately superseded v0.1.4 APK had SHA-256 `450ca89ca53e875c4a1d6efa27928924a72b8324d762c6525f2471c80dfa1f3e`. The earlier superseded v0.1.4 APK had SHA-256 `f768d9ed09aa16d51a585470804374b52972f34ffb778f615b580de466b1d312`.

After the v0.1.4 release source, `main` received documentation/store-metadata work and reproducibility hardening from PR #43. That hardening pins Build-Tools 35.0.0 for the maintained build path and disables AGP VCS metadata in release builds. No post-release Android runtime behavior change is implied by those changes.

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
- Android SDK Build-Tools exactly 35.0.0 for the maintained reproducible release path

The repository-owned bootstrap remains the supported build path. Public builds must remain independent of private credentials, signing material, or private infrastructure.

The repository itself contains no production-signing helper or private signing configuration. `tools/build.py` currently builds debug APKs only. Production signing remains an external maintainer-controlled step and must follow the verified F-Droid-compatible explicit-scheme procedure recorded in `FDROID_NOTES.md`.

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

v0.1.4 is published and remains the canonical release. The canonical APK is now SHA-256 `2f6ce92f8b3bbe33dde16e1aef0254a35c939a5382ac108d1a580a0eb05c83d0`, size `1,754,890` bytes. The exact published bytes were re-downloaded after replacement and re-verified for package/version, production signer, explicit signing-scheme state, accepted `classes.dex`, release revision, absence of generated JAR-signing metadata, alignment, and F-Droid signature-copy byte identity.

### K2040 Android Projects

The GeoJoystick Android Projects page is published for v0.1.4. It uses the exact accepted English/German 1024×500 promotional banners rather than reconstructed artwork, and the landing-page Latest / Project Updates entry uses the same localized banner set.

### F-Droid

The public F-Droid package page currently exposes **0.1.3 (`versionCode 103`)** as the suggested version.

The published F-Droid versions use the established GeoJoystick developer signing identity. Preserve Android update continuity; switching version 104 to an unrelated F-Droid repository signing key is not an acceptable migration path unless a valid Android-recognized signing transition is explicitly proven.

F-Droid automatic update processing created fdroiddata MR `!46016` for v0.1.4 / 104. The original developer-binary verification failed. The technical source/build and developer-signing diagnosis is now closed:

- exact F-Droid source build succeeds from release commit `0c3ae37501660300e4f23c45aeb07cffb68e62f9`;
- exact F-Droid unsigned v0.1.4 diagnostic APK SHA-256: `a7f7d0b1d04bf6bbc15fa352830054d2b7c16c9509e9d60aea26e467a8d7fc1f`;
- implicit Build-Tools 35 `apksigner` scheme defaults were confirmed to generate JAR-signing material that breaks F-Droid signature-copy verification despite verifier output reporting v1 as false;
- the validated production path explicitly uses v1 `false`, v2 `true`, v3 `true`, v4 `false` with `--alignment-preserved`;
- the exact canonical GitHub APK now uses that corrected path and passes the retained local F-Droid signature-copy verification with byte identity;
- diagnostic branch `geojoystick-v104-canonical-reference-test` at commit `bcb24ac45aa8c15786a1da703cab547c2e08a19d` changed only the v104 build-specific `binary:` URL to the canonical GitHub release URL;
- pipeline `2773265250` exact `fdroid build` job `15987693203` succeeded: F-Droid built v104 from the exact release commit, retrieved the canonical GitHub APK, verified its v1/v2/v3/v4 state, successfully verified the copied-signature APK, and reported that the built binary matched the supplied reference binary;
- the earlier v3 `CHUNKED_SHA512` mismatch was absent.

The aggregate diagnostic pipeline was failed by another job, but the exact GeoJoystick `fdroid build` job is independently verified successful. That aggregate failure must be classified separately before it is used as production CI evidence.

The next downstream step is to inspect MR `!46016` and its exact production metadata/source branch, classify the unrelated aggregate pipeline failure, then apply only the minimal production fdroiddata correction required to use the verified developer-binary path. Issue #38 remains open until the production F-Droid path is complete and version 104 is public.

Do not claim v0.1.4 is available on F-Droid until version 104 is actually published on the public package page.

### APKPure

The official APKPure package listing exists for `com.k2040.geojoystick`. The maintainer submitted an earlier v0.1.4 APK through the authenticated publisher flow, and **v0.1.4 has not been published yet**. The APKPure console currently does not permit replacing or updating the submitted binary while that upload is still awaiting review. Once the review finishes, use the current canonical APK at the first permitted publisher action and do not mark the channel complete until that artifact is public.

### OpenAPK

The maintainer submitted GeoJoystick to OpenAPK. Treat this as **submitted/pending review or publication** until a public listing is independently verified. Determine whether OpenAPK stores an independent earlier binary or mirrors the canonical source before changing anything.

### Uptodown

Uptodown previously approved and published the then-canonical v0.1.4 / 104 package. The GitHub canonical APK was replaced again on 2026-08-19 for F-Droid compatibility, so Uptodown must now be treated as a separately published earlier v0.1.4 artifact until its served bytes are independently compared or an approved replacement is performed. Do not assume the Uptodown package changed automatically.

Durable downstream tracker: GitHub Issue #37, **Complete GeoJoystick v0.1.4 downstream publication**.

## F-Droid/reproducibility boundary

Keep developer-signed GitHub release evidence separate from F-Droid reproducibility evidence.

Verified on 2026-08-19:

- exact v0.1.4 F-Droid source build succeeds from the tagged release commit;
- current F-Droid unsigned diagnostic APK SHA-256 is `a7f7d0b1d04bf6bbc15fa352830054d2b7c16c9509e9d60aea26e467a8d7fc1f`;
- implicit `apksigner` signing-scheme defaults are the confirmed cause of the reproducible-binary signature-copy failure;
- the validated GeoJoystick signing recipe explicitly sets v1 `false`, v2 `true`, v3 `true`, v4 `false` and uses `--alignment-preserved` with Build-Tools 35;
- the canonical published GitHub APK SHA-256 `2f6ce92f8b3bbe33dde16e1aef0254a35c939a5382ac108d1a580a0eb05c83d0` passes signer, scheme, `META-INF`, alignment, release-content, post-download, and local signature-copy byte-identity gates;
- the exact fdroiddata diagnostic `fdroid build` job also verifies the copied-signature APK and reports the F-Droid-built binary matches the canonical GitHub developer APK;
- ordinary F-Droid repository signing is not the current migration path because existing F-Droid installations already rely on the GeoJoystick developer signing identity.

No further APK rebuild or signing is required by the currently verified v0.1.4 reproducibility result.

`FDROID_NOTES.md` owns the detailed F-Droid-specific state and reproducible-signing requirements. Any future production signing, release-asset replacement, tag/release mutation, or store/F-Droid submission beyond the already approved work remains an explicit maintainer-controlled publication action unless the current task has already authorized that exact action.

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

The current published v0.1.4 GitHub APK was validated after publication for package/version, permanent signer, explicit v1 `false` / v2 `true` / v3 `true` / v4 `false` scheme state, accepted `classes.dex`, exact release revision, absence of generated JAR-signing metadata, `zipalign -c 4`, and exact retained F-Droid signature-copy byte identity. Its SHA-256 is `2f6ce92f8b3bbe33dde16e1aef0254a35c939a5382ac108d1a580a0eb05c83d0`.

The fdroiddata canonical-reference diagnostic independently proved that F-Droid's own v104 `fdroid build` path retrieves that exact canonical GitHub reference, builds from the exact v0.1.4 source commit, successfully verifies the copied-signature APK, and reports that the built APK matches the supplied developer binary. The exact job succeeded even though an unrelated job caused the aggregate diagnostic pipeline to fail.

No Android runtime source changed during this diagnosis or artifact replacement, so no physical-device QA was required or performed.

Documentation-only maintenance does not require an Android rebuild when runtime source, dependencies, and build configuration are unchanged. Any future runtime, build-system, signing, mock-provider, location-input, or network behavior change requires scope-appropriate validation before release claims.

GitHub Actions are not used as PC/Linux validation evidence unless explicitly approved for a separate workflow.

## Publication boundary

Production signing, new tags/releases, future release-asset replacement, F-Droid/store submissions, deployments, announcements, and comparable publication actions remain explicit maintainer-controlled gates unless the current task has already authorized the specific action.
