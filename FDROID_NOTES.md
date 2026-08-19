# F-Droid preparation and publication notes

GeoJoystick is intended to remain suitable for F-Droid and similar FLOSS Android repositories.

## FLOSS/build baseline

- Application code: `GPL-3.0-only`
- No ads, analytics, tracking, accounts, subscriptions, paid features, billing, or proprietary updater
- No proprietary Android dependencies in the Gradle build
- Public canonical source repository on GitHub
- Command-line build path through `build.bat` / `tools/build.py`
- JDK 17 / Android SDK Platform 36 / Android SDK Build-Tools 35.0.0 development baseline
- Gradle 8.13 / Android Gradle Plugin 8.12.3
- Release APK generation disables AGP VCS metadata in the maintained post-v0.1.4 build configuration so output does not depend on whether `.git` is present or usable
- Fastlane-style metadata under `fastlane/metadata/android/`
- Accepted synthetic/sanitized English and German store screenshots
- Public-safe screenshot provenance and hashes in `fastlane/metadata/android/SCREENSHOT_PROVENANCE.md`
- Current store icon is a 512×512 32-bit RGBA PNG exported directly from the accepted gecko launcher artwork (`SHA-256 6bf5a6fd69fb88f1a18e65177addc9ff64abe090bc812e5e0241bb5495383074`)
- Optional Ko-fi support link only; the app remains fully functional without it

## Current public F-Droid state

The public F-Droid package page currently lists **GeoJoystick 0.1.3 (`versionCode 103`)** as the suggested version:

https://f-droid.org/packages/com.k2040.geojoystick/

The page uses the accepted store presentation and discloses the optional OpenStreetMap-backed map picker with the network-service anti-feature note.

The currently published F-Droid versions were distributed under the established GeoJoystick developer signing identity. Preserve Android update continuity: do not switch future F-Droid builds for the same package to an unrelated repository signing key unless a valid Android-recognized signing migration is explicitly proven.

## v0.1.4 canonical GitHub artifact

The canonical GitHub release is **v0.1.4 (`versionCode 104`)** from source commit:

`0c3ae37501660300e4f23c45aeb07cffb68e62f9`

The currently published GitHub APK is:

- `GeoJoystick-v0.1.4.apk`
- size: `1,754,890` bytes
- SHA-256: `2f6ce92f8b3bbe33dde16e1aef0254a35c939a5382ac108d1a580a0eb05c83d0`
- production signer SHA-256: `e0a833050d7c8fce7ddce85b2a86561304456d87b67bd6be1577d8f657e16778`
- APK Signature Scheme v1: disabled
- APK Signature Scheme v2: enabled
- APK Signature Scheme v3: enabled
- APK Signature Scheme v4: disabled
- embedded Git revision: `0c3ae37501660300e4f23c45aeb07cffb68e62f9`

On 2026-08-19 the previously published v0.1.4 APK (`SHA-256 450ca89ca53e875c4a1d6efa27928924a72b8324d762c6525f2471c80dfa1f3e`) was replaced under explicit maintainer approval with the corrected F-Droid-compatible artifact. The replacement was re-downloaded from GitHub and verified byte-for-byte against the accepted candidate. It preserves package/version, accepted `classes.dex`, exact release revision, permanent signing identity, and `zipalign -c 4`, contains no generated JAR-signing `.SF` / `.RSA` / `MANIFEST.MF` material, and passes the exact retained F-Droid `apksigcopier` signature-copy path with byte identity.

The still earlier v0.1.4 APK (`SHA-256 f768d9ed09aa16d51a585470804374b52972f34ffb778f615b580de466b1d312`) also remains superseded. The release tag, source commit, source history, and release notes were not changed.

## v0.1.4 F-Droid investigation

F-Droid automatic update processing created fdroiddata merge request **!46016** for v0.1.4 / 104. The original binary verification failed. Early comparison showed Android version-control metadata differences and a reported `classes.dex` mismatch; a later fresh source/toolchain diagnosis reproduced the version-control metadata difference but not the reported `classes.dex` difference.

The accepted source/build diagnostics established:

- exact release source commit: `0c3ae37501660300e4f23c45aeb07cffb68e62f9`
- JDK 17.0.20
- Gradle 8.13
- Android SDK Platform 36
- Android SDK Build-Tools 35.0.0
- accepted `classes.dex` SHA-256: `edfc89a65c655a971cf0700614ce4688ddc4156db634003e4acc20de3ce61a8b`
- exact F-Droid unsigned v0.1.4 diagnostic APK SHA-256: `a7f7d0b1d04bf6bbc15fa352830054d2b7c16c9509e9d60aea26e467a8d7fc1f`

A controlled fdroiddata fork CI test on 2026-08-19 confirmed that F-Droid can independently build v0.1.4 from the exact release commit. An attempted metadata shape that preserved historical developer-binary verification for old builds while making only v104 independently signed was invalid under the current fdroiddata schema, and ordinary F-Droid signing is not an acceptable migration path for this package because existing F-Droid users already receive developer-signed GeoJoystick updates.

A later diagnostic branch first reproduced the known signature-copy failure because its v104 build-specific `binary:` field still pointed at an obsolete GitLab test APK. That failed reference was SHA-256 `f5c9c7dc680bdb2ca48f4f7529e956211da66875ff029dbb307bb647bb973aea`, size `1,763,281` bytes. The failed branch `geojoystick-v104-repro-ci-test` remains preserved at commit `b777dcc1bbbf32d8491a3e6e5dff262499c684c8` as diagnostic evidence.

A separate diagnostic branch then changed only the v104 `binary:` URL to the canonical GitHub release URL. Provider-side validation proved a one-file, one-line metadata delta. The test state was:

- branch: `geojoystick-v104-canonical-reference-test`
- commit: `bcb24ac45aa8c15786a1da703cab547c2e08a19d`
- pipeline: `2773265250`
- exact `fdroid build` job: `15987693203`

The exact `fdroid build` job **succeeded** even though the aggregate diagnostic pipeline ended failed because of another job that has not yet been classified. The successful build job proved:

- F-Droid built `com.k2040.geojoystick:104` from exact source commit `0c3ae37501660300e4f23c45aeb07cffb68e62f9`;
- F-Droid actually retrieved `https://github.com/Kamui2040/K2040-GeoJoystick/releases/download/v0.1.4/GeoJoystick-v0.1.4.apk`;
- the supplied reference APK verified with v1 `false`, v2 `true`, v3 `true`, v3.1 `false`, and v4 `false`;
- the copied-signature APK verified successfully;
- F-Droid reported `compared built binary to supplied reference binary successfully`;
- F-Droid reported `success: com.k2040.geojoystick` and `1 build succeeded`;
- the previous v3 `CHUNKED_SHA512` digest mismatch was absent.

This closes the GeoJoystick v0.1.4 technical reproducible developer-binary verification gate against the actual canonical GitHub release APK.

## Confirmed signing root cause

The reproducible-binary failure was isolated to the manual `apksigner` invocation used for the earlier published developer APK.

Controlled A/B testing used the same exact unsigned APK, Build-Tools 35 `apksigner`, and the same disposable RSA-4096 key:

- with implicit apksigner scheme defaults, `apksigner verify` reported v1 `false`, v2 `true`, and v3 `true`, but the signed APK contained generated JAR-signing files (`META-INF/*.SF`, `META-INF/*.RSA`, and `META-INF/MANIFEST.MF`);
- F-Droid's vendored `apksigcopier.do_copy` completed on that APK, but the copied output failed verification with an APK Signature Scheme v3 `CHUNKED_SHA512 digest mismatch`;
- with `--v1-signing-enabled false` explicitly supplied, the generated JAR-signing files were absent and the copied APK verified and was byte-for-byte identical to the signed input.

The same explicit-scheme test was repeated with the permanent GeoJoystick production key on the exact F-Droid unsigned APK. It passed all gates:

- production certificate SHA-256 remained `e0a833050d7c8fce7ddce85b2a86561304456d87b67bd6be1577d8f657e16778`;
- v1 `false`, v2 `true`, v3 `true`, v4 `false` were explicitly selected and verified;
- no generated JAR-signing `.SF` / `.RSA` / `MANIFEST.MF` files were present;
- `zipalign -c 4` passed;
- F-Droid's vendored signature-copy path completed successfully;
- copied output verified;
- copied output was byte-for-byte identical.

That exact validated permanent-key artifact is now the canonical published GitHub v0.1.4 APK with SHA-256:

`2f6ce92f8b3bbe33dde16e1aef0254a35c939a5382ac108d1a580a0eb05c83d0`

Root cause: **relying on implicit apksigner signing-scheme defaults generated JAR-signing metadata that breaks the F-Droid reproducible-binary signature-copy path, even though verifier output reports v1 as disabled.**

## Current v0.1.4 F-Droid status

Issue #38 remains open because v0.1.4 has not yet completed the production F-Droid publication path:

https://github.com/Kamui2040/K2040-GeoJoystick/issues/38

The technical reproducibility work is complete. Verified evidence now includes the exact source build, corrected developer-signing recipe, canonical GitHub reference artifact, post-upload re-download verification, local signature-copy byte identity, and successful fdroiddata `fdroid build` developer-binary comparison against the actual canonical GitHub URL.

Remaining work:

1. classify the unrelated job responsible for the aggregate diagnostic pipeline failure so it is not confused with the successful GeoJoystick `fdroid build` result;
2. inspect MR `!46016` and its exact current production metadata/source branch before any mutation;
3. make only the minimal production fdroiddata correction required to use the now-verified canonical developer-binary path while preserving the established GeoJoystick signing identity;
4. do not mark v0.1.4 available on F-Droid until version 104 is actually published on the public package page.

No additional APK rebuild, production signing, or diagnostic signing is required by the currently verified reproducibility result.

## Reproducible-release requirements

For production releases intended for F-Droid reproducible developer-binary verification:

- build from a completely fresh Git clone at the exact release tag/commit;
- use the project-pinned JDK, Gradle, Android Gradle Plugin, Android SDK Platform, and Build-Tools versions;
- use a fresh or isolated Gradle user home so stale build caches cannot affect evidence;
- do not depend on `.git` presence for maintained release APK contents; AGP VCS metadata is disabled in the current release configuration;
- compare unsigned release outputs from the intended reproducible build contexts before signing;
- keep production signing separate and perform it only after the unsigned reproducibility gate passes;
- when invoking `apksigner`, set all relevant signing schemes explicitly rather than relying on defaults; for the currently validated GeoJoystick path use v1 `false`, v2 `true`, v3 `true`, and v4 `false`;
- after signing, verify the expected certificate and reported scheme state and inspect `META-INF` for unexpected generated JAR-signing `.SF`, `.RSA`/`.DSA`/`.EC`, or `MANIFEST.MF` material;
- run the current F-Droid `apksigcopier`/signature-copy compatibility gate against the exact unsigned APK and require the copied output to verify; when exact reproducibility is expected, require byte-for-byte identity as well;
- never replace a published canonical APK silently; artifact replacement requires explicit maintainer approval and must be recorded with old/new hashes and validation evidence.

## Store metadata safety

- Keep all store screenshots synthetic/sanitized; never restore historical screenshots containing authentic precise location material.
- The WebView map picker uses bundled HTML/CSS/JavaScript and does not load remote JavaScript. It downloads OpenStreetMap tile images only when the picker is opened.
- Preserve visible OpenStreetMap attribution and the applicable ODbL notice.
- Confirm source, tag, release inputs, signing identity, and fdroiddata metadata before any future update submission.
- Keep F-Droid build/reproducibility evidence distinct from the developer-signed GitHub release artifact evidence.

## Current fdroiddata identity fields

```yaml
Categories:
  - Navigation
  - System
License: GPL-3.0-only
AuthorName: K2040
Donate: https://ko-fi.com/k2040
AutoName: GeoJoystick
Summary: Floating mock-location joystick for Android emulator testing
```

## Public repository

https://github.com/Kamui2040/K2040-GeoJoystick
