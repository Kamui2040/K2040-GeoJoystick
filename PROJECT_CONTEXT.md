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
- APK size: `1,763,281` bytes
- APK SHA-256: `450ca89ca53e875c4a1d6efa27928924a72b8324d762c6525f2471c80dfa1f3e`
- Production signer certificate SHA-256: `e0a833050d7c8fce7ddce85b2a86561304456d87b67bd6be1577d8f657e16778`
- Embedded release VCS revision: `0c3ae37501660300e4f23c45aeb07cffb68e62f9`

GitHub Releases remains the authoritative source for published developer-signed artifacts and release notes.

On 2026-08-18 the v0.1.4 release APK was rebuilt from a pristine Git checkout at the unchanged v0.1.4 tag/source commit to correct the source/VCS-metadata side of the original F-Droid mismatch. The replacement preserved the established production certificate and accepted application payload. Later reproducibility testing on 2026-08-19 established that the manual signing invocation used for that published replacement still relied on implicit `apksigner` signing-scheme defaults and is not compatible with the required F-Droid reproducible developer-binary signature-copy path. The canonical GitHub artifact above therefore remains published and valid as the current release, but it is **not yet the accepted F-Droid reference artifact**. Replacing it again is a separate explicit publication action.

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

v0.1.4 is published and remains the canonical release. The current published APK is the artifact identified above by SHA-256 `450ca89ca53e875c4a1d6efa27928924a72b8324d762c6525f2471c80dfa1f3e`.

A corrected F-Droid-compatible permanent-key diagnostic APK was produced on 2026-08-19 from the exact accepted F-Droid unsigned v0.1.4 input using explicit signing schemes. Its SHA-256 is `2f6ce92f8b3bbe33dde16e1aef0254a35c939a5382ac108d1a580a0eb05c83d0`. It is diagnostic evidence only and is **not** a published release artifact.

### K2040 Android Projects

The GeoJoystick Android Projects page is published for v0.1.4. It uses the exact accepted English/German 1024×500 promotional banners rather than reconstructed artwork, and the landing-page Latest / Project Updates entry uses the same localized banner set.

### F-Droid

The public F-Droid package page currently exposes **0.1.3 (`versionCode 103`)** as the suggested version.

The published F-Droid versions use the established GeoJoystick developer signing identity. Preserve Android update continuity; switching version 104 to an unrelated F-Droid repository signing key is not an acceptable migration path unless a valid Android-recognized signing transition is explicitly proven.

F-Droid automatic update processing created fdroiddata MR `!46016` for v0.1.4 / 104. The original developer-binary verification failed. The source/build side is now substantially resolved:

- a controlled fdroiddata fork CI test on 2026-08-19 successfully built v0.1.4 from the exact release commit using the normal F-Droid source-build path;
- exact F-Droid unsigned diagnostic APK SHA-256: `a7f7d0b1d04bf6bbc15fa352830054d2b7c16c9509e9d60aea26e467a8d7fc1f`;
- an attempted mixed metadata shape that kept historical developer-binary verification while making only v104 independently F-Droid-signed was rejected by current fdroiddata schema and is not the production direction.

The developer-binary verification root cause is confirmed. Controlled A/B testing showed that Build-Tools 35 `apksigner` with implicit signing-scheme defaults can generate JAR-signing `META-INF/*.SF`, `META-INF/*.RSA`, and `META-INF/MANIFEST.MF` files even though `apksigner verify` reports v1 as `false`. F-Droid's vendored signature-copy path then fails with an APK Signature Scheme v3 `CHUNKED_SHA512 digest mismatch`. With v1 `false`, v2 `true`, v3 `true`, and v4 `false` supplied explicitly, those JAR-signing files are absent and the signature-copy output verifies byte-for-byte.

The permanent GeoJoystick production key was used in an explicitly approved diagnostic signing test against the exact F-Droid unsigned APK. The corrected diagnostic preserved the established production certificate, contained no generated JAR-signing files, passed `zipalign -c 4`, survived the exact F-Droid signature-copy path, and reproduced byte-for-byte after signature copying. This closes the technical signing diagnosis but does not itself publish or submit v0.1.4.

Issue #38 remains open until the actual production reference artifact and downstream F-Droid publication path are completed.

Do not claim v0.1.4 is available on F-Droid until version 104 is actually published on the public package page.

### APKPure

The official APKPure package listing exists for `com.k2040.geojoystick`. The maintainer submitted the original v0.1.4 APK through the authenticated publisher flow, and **v0.1.4 has not been published yet**. The APKPure console currently does not permit replacing or updating the submitted binary while that upload is still awaiting review. Once the review finishes, use the then-current accepted canonical APK at the first permitted publisher action and do not mark the channel complete until that artifact is public.

### OpenAPK

The maintainer submitted GeoJoystick to OpenAPK. Treat this as **submitted/pending review or publication** until a public listing is independently verified. After the canonical APK is updated where required, determine whether OpenAPK stores an independent old binary or mirrors the canonical source before changing anything.

### Uptodown

Uptodown has approved and publicly published the **currently canonical v0.1.4 (`versionCode 104`) package**. A future approved replacement of the canonical GitHub APK for F-Droid compatibility would require a separate channel-specific continuity/replacement decision; do not assume the already-published Uptodown artifact changes automatically.

Durable downstream tracker: GitHub Issue #37, **Complete GeoJoystick v0.1.4 downstream publication**.

## F-Droid/reproducibility boundary

Keep developer-signed GitHub release evidence separate from F-Droid reproducibility evidence.

Verified on 2026-08-19:

- exact v0.1.4 F-Droid source build succeeds from the tagged release commit;
- current F-Droid unsigned diagnostic APK SHA-256 is `a7f7d0b1d04bf6bbc15fa352830054d2b7c16c9509e9d60aea26e467a8d7fc1f`;
- implicit `apksigner` signing-scheme defaults are the confirmed cause of the reproducible-binary signature-copy failure;
- the validated GeoJoystick signing recipe explicitly sets v1 `false`, v2 `true`, v3 `true`, v4 `false` and uses `--alignment-preserved` with Build-Tools 35;
- the corrected permanent-key diagnostic APK SHA-256 `2f6ce92f8b3bbe33dde16e1aef0254a35c939a5382ac108d1a580a0eb05c83d0` passes signer, scheme, `META-INF`, alignment, signature-copy verification, and byte-identity gates;
- ordinary F-Droid repository signing is not the current migration path because existing F-Droid installations already rely on the GeoJoystick developer signing identity.

`FDROID_NOTES.md` owns the detailed F-Droid-specific state and reproducible-signing requirements. Any future production signing beyond the already approved diagnostic, artifact replacement, tag/release mutation, or store/F-Droid submission remains an explicit maintainer-controlled publication action unless the current task has already authorized that exact action.

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

The current published v0.1.4 GitHub APK remains independently valid for package/version, production signer, alignment, accepted application payload, and embedded release revision, but its implicit-default signing structure is not accepted for F-Droid reproducible developer-binary verification.

The corrected permanent-key diagnostic used the exact accepted F-Droid unsigned v0.1.4 APK and Build-Tools 35 with `--alignment-preserved` and explicit v1 `false`, v2 `true`, v3 `true`, v4 `false`. It preserved the permanent certificate SHA-256 `e0a833050d7c8fce7ddce85b2a86561304456d87b67bd6be1577d8f657e16778`, generated no JAR-signing metadata, passed `zipalign -c 4`, passed the exact retained F-Droid signature-copy path, and reproduced byte-for-byte with SHA-256 `2f6ce92f8b3bbe33dde16e1aef0254a35c939a5382ac108d1a580a0eb05c83d0`.

No Android runtime source changed during this diagnosis, so no physical-device QA was required or performed.

Documentation-only maintenance does not require an Android rebuild when runtime source, dependencies, and build configuration are unchanged. Any future runtime, build-system, signing, mock-provider, location-input, or network behavior change requires scope-appropriate validation before release claims.

GitHub Actions are not used as PC/Linux validation evidence unless explicitly approved for a separate workflow.

## Publication boundary

Production signing beyond the already approved diagnostic, new tags/releases, release-asset replacement, F-Droid/store submissions, deployments, announcements, and comparable publication actions remain explicit maintainer-controlled gates unless the current task has already authorized the specific action.
