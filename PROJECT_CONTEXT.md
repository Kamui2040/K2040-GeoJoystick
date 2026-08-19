# Project Context

Updated: 2026-08-19

## Purpose

GeoJoystick is an open-source Android mock-location joystick for emulator and developer testing. It uses Android's standard mock-location provider flow and manual Developer Options selection. Public descriptions must not present it as game tooling, cheating software, anti-detection tooling, or a bypass utility.

The app remains ad-free, account-free, analytics-free, tracking-free, subscription-free, and free of mandatory proprietary services. Saved coordinates, favorites, and settings remain local unless the user explicitly performs a documented external action. The built-in map uses OpenStreetMap only when opened, with required attribution preserved.

## Current canonical release

The current canonical production release is **GeoJoystick v0.1.4 (`versionCode 104`)**.

- Tag: `v0.1.4`
- Release source commit: `0c3ae37501660300e4f23c45aeb07cffb68e62f9`
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

On 2026-08-19 the v0.1.4 release APK was replaced under explicit maintainer approval with the corrected F-Droid-compatible developer-signed artifact. The replacement preserves package/version, accepted `classes.dex`, exact release revision, permanent signing identity, and alignment; explicitly uses v1 `false`, v2 `true`, v3 `true`, v4 `false`; contains no generated JAR-signing metadata; and passes the retained F-Droid `apksigcopier` signature-copy path with byte-for-byte identity. The published asset was re-downloaded after replacement and independently verified.

Superseded v0.1.4 APK hashes remain recorded for audit:

- `450ca89ca53e875c4a1d6efa27928924a72b8324d762c6525f2471c80dfa1f3e`
- `f768d9ed09aa16d51a585470804374b52972f34ffb778f615b580de466b1d312`

Post-release PR #43 hardened future release reproducibility by pinning Android Build-Tools 35.0.0 and disabling environment-dependent AGP VCS metadata. No Android runtime behavior change is implied by that post-release work.

## Accepted product state

v0.1.4 includes:

- strict manual latitude, longitude, and altitude validation
- built-in OpenStreetMap picker
- supported map-link coordinate import with bounded network resolution where required
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

Public builds must remain independent of private credentials, signing material, or private infrastructure. Production signing remains external maintainer-controlled work and must follow the verified explicit-scheme procedure in `FDROID_NOTES.md`.

## F-Droid v0.1.4 state

The public F-Droid package page still exposes **0.1.3 (`versionCode 103`)** as the suggested version. Do not claim v0.1.4 is available on F-Droid until version 104 is actually public.

F-Droid automatic update processing created fdroiddata MR **!46016** for v0.1.4 / 104. The current production MR source branch remains `fdroid/checkupdates-bot-fdroiddata:com.k2040.geojoystick` at commit `2974d67243aa1affbde77963db48a4e0ec7bc849`.

The production metadata at that commit is otherwise correct and was verified byte-for-byte:

- global `Binaries:` points to `https://github.com/Kamui2040/K2040-GeoJoystick/releases/download/v%v/GeoJoystick-v%v.apk`
- `AllowedAPKSigningKeys` remains `e0a833050d7c8fce7ddce85b2a86561304456d87b67bd6be1577d8f657e16778`
- v104 source commit is `0c3ae37501660300e4f23c45aeb07cffb68e62f9`
- `CurrentVersion` / `CurrentVersionCode` are `0.1.4` / `104`
- metadata passes schema, lint, YAML 1.2, checkupdates, source checks, and `fdroid rewritemeta`

### Developer APK signing diagnosis

The earlier canonical APK failure was isolated to implicit Build-Tools 35 `apksigner` defaults. Explicitly setting v1 `false`, v2 `true`, v3 `true`, v4 `false` and using `--alignment-preserved` produces the current canonical APK. That APK passes local `apksigcopier` verification and byte identity.

A diagnostic fdroiddata branch using the canonical GitHub APK directly also proved F-Droid can build v104 from the exact release commit and successfully verify the copied signature against the canonical APK:

- branch: `geojoystick-v104-canonical-reference-test`
- commit: `bcb24ac45aa8c15786a1da703cab547c2e08a19d`
- pipeline: `2773265250`
- exact `fdroid build` job: `15987693203`
- result: success; copied-signature verification passed; built binary matched canonical developer APK; prior `CHUNKED_SHA512` failure absent

The aggregate failure on that diagnostic branch was only `fdroid rewritemeta` rejecting the temporary build-specific `binary:` field formatting. That was a test-branch formatting issue, not a GeoJoystick build failure.

### Production metadata defect isolated

The exact production MR commit was transferred unchanged to the maintainer fork as `geojoystick-v104-production-exact-test` and tested at pipeline `2773342977`. `fdroid rewritemeta` passed, but `fdroid build` failed after executing the v104 production postbuild:

```yaml
srclibs:
  - reproducible-apk-tools@v0.3.0
postbuild:
  - mv $$OUT$$ unaligned.apk
  - $$reproducible-apk-tools$$/zipalign.py --page-size 4 --pad-like-apksigner
    --replace unaligned.apk $$OUT$$
```

The trace proved the postbuild ran immediately before developer-binary verification and the copied-signature APK then failed with an APK Signature Scheme v3 `CHUNKED_SHA512` digest mismatch.

A controlled A/B removed **only those six metadata lines** while preserving the production global `Binaries`, signing-key restriction, v104 source commit, Java setup, Gradle build, and CurrentVersion fields:

- test branch: `geojoystick-v104-production-no-postbuild-test`
- candidate commit: `38ad65480c0c25510e8aa1ed68fadc3b853ea30b`
- pipeline: `2773367289`
- `fdroid build` job: `15988436842` — success
- `fdroid rewritemeta` job: `15988436847` — success
- aggregate pipeline: success
- copied-signature APK: verified
- built binary: matched canonical developer APK
- `CHUNKED_SHA` mismatch: absent

This A/B is conclusive: **the remaining production F-Droid defect is the v104 `reproducible-apk-tools` APK-realignment postbuild. Removing that six-line block fixes the production developer-binary verification without changing the source build or canonical APK.**

No additional APK rebuild, production signing, release-asset replacement, or device QA is required by this result.

### Remaining F-Droid action

Production MR !46016 and the bot source branch remain unchanged. The maintainer account can inspect but cannot retry the upstream `fdroid/fdroiddata` pipeline (`403 Forbidden`). The next production correction is therefore limited to removing the six-line v104 `srclibs`/`postbuild` realignment block from the fdroiddata submission and rerunning the production F-Droid checks.

That production F-Droid submission / upstream human communication remains approval-gated. Issue #38 stays open until version 104 is accepted and public.

## Other downstream publication state

### APKPure

The authenticated publisher flow has an earlier v0.1.4 submission still awaiting review. The console did not permit replacing the pending binary at last check. At the first permitted publisher action, use the current canonical APK.

### OpenAPK

GeoJoystick was submitted. Treat it as pending review/publication until a public listing is independently verified.

### Uptodown

Uptodown previously published the then-canonical v0.1.4 APK. Because the canonical GitHub APK was later replaced for F-Droid compatibility, Uptodown must be treated as serving a separately published earlier v0.1.4 artifact until independently compared or explicitly replaced.

Durable downstream tracker: GitHub Issue #37, **Complete GeoJoystick v0.1.4 downstream publication**.

## Store assets and privacy

Current Fastlane/store presentation uses sanitized real-device screenshots with synthetic coordinates only. Historical screenshots containing authentic precise location material remain removed and must not be restored.

- English screenshots: Main, Settings, About, Map + overlay
- German screenshots: Main, Settings, About, Map + overlay
- screenshot provenance: `fastlane/metadata/android/SCREENSHOT_PROVENANCE.md`
- current 512×512 store icon SHA-256: `6bf5a6fd69fb88f1a18e65177addc9ff64abe090bc812e5e0241bb5495383074`

## Licensing and provenance

- Application code: `GPL-3.0-only`
- applicable K2040 GPL files retain the separate GPLv3 section 7(b) attribution-preservation term where explicitly marked
- original K2040 artwork/UI artwork with established provenance: `CC-BY-4.0`
- GoGoGo-derived and other third-party material retain their controlling licences/notices
- OpenStreetMap data: © OpenStreetMap contributors, ODbL 1.0

`NOTICE.md` owns detailed provenance and attribution scope.

## Current durable work

Open public issues requiring follow-up:

- #37 — Complete GeoJoystick v0.1.4 downstream publication
- #38 — F-Droid Build Failed

## Validation boundary

The current GitHub APK is validated for package/version, permanent signer, explicit v1 `false` / v2 `true` / v3 `true` / v4 `false`, accepted `classes.dex`, exact release revision, absence of generated JAR-signing metadata, `zipalign -c 4`, post-download identity, and retained F-Droid signature-copy compatibility.

The exact F-Droid production metadata A/B now independently proves that removing only the six-line APK-realignment postbuild makes the canonical developer-binary path pass F-Droid CI in full.

No Android runtime source changed during this diagnosis, so no physical-device QA was required or performed. GitHub Actions were not used as PC/Linux validation evidence.

## Publication boundary

Production signing, new tags/releases, future release-asset replacement, F-Droid/store submissions, deployments, announcements, and comparable publication actions remain explicit maintainer-controlled gates unless the current task has already authorized the specific action.
