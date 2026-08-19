# F-Droid preparation and publication notes

GeoJoystick is intended to remain suitable for F-Droid and similar FLOSS Android repositories.

## FLOSS/build baseline

- Application code: `GPL-3.0-only`
- No ads, analytics, tracking, accounts, subscriptions, paid features, billing, or proprietary updater
- No proprietary Android dependencies in the Gradle build
- Public canonical source repository on GitHub
- JDK 17 / Android SDK Platform 36 / Android SDK Build-Tools 35.0.0 maintained baseline
- Gradle 8.13 / Android Gradle Plugin 8.12.3
- post-v0.1.4 release configuration disables AGP VCS metadata and pins Build-Tools 35.0.0 for future reproducibility
- existing F-Droid installations use the established GeoJoystick developer signing identity; do not switch to an unrelated repository signing key without a valid Android-recognized signing migration

## Current public F-Droid state

The public F-Droid package page currently lists **GeoJoystick 0.1.3 (`versionCode 103`)** as the suggested version. Do not claim v0.1.4 is available on F-Droid until version 104 is actually public.

F-Droid automatic update processing created fdroiddata MR **!46016** for v0.1.4 / 104. Its live production source branch is `fdroid/checkupdates-bot-fdroiddata:com.k2040.geojoystick` at commit:

`2974d67243aa1affbde77963db48a4e0ec7bc849`

The production metadata at that commit uses:

```yaml
Binaries: https://github.com/Kamui2040/K2040-GeoJoystick/releases/download/v%v/GeoJoystick-v%v.apk
AllowedAPKSigningKeys: e0a833050d7c8fce7ddce85b2a86561304456d87b67bd6be1577d8f657e16778
CurrentVersion: 0.1.4
CurrentVersionCode: 104
```

The v104 build record points to exact release source commit `0c3ae37501660300e4f23c45aeb07cffb68e62f9`, installs Java 17 from Debian bookworm, and builds with Gradle. The production metadata passes schema validation, YAML 1.2, lint, checkupdates, source checks, and `fdroid rewritemeta`.

## Canonical GitHub v0.1.4 APK

The currently published canonical developer APK is:

- file: `GeoJoystick-v0.1.4.apk`
- size: `1,754,890` bytes
- SHA-256: `2f6ce92f8b3bbe33dde16e1aef0254a35c939a5382ac108d1a580a0eb05c83d0`
- production signer SHA-256: `e0a833050d7c8fce7ddce85b2a86561304456d87b67bd6be1577d8f657e16778`
- v1: false
- v2: true
- v3: true
- v4: false
- embedded release revision: `0c3ae37501660300e4f23c45aeb07cffb68e62f9`

The canonical APK was replaced on 2026-08-19 under explicit maintainer approval after a controlled signing diagnosis. The replacement preserves package/version, accepted payload, exact release revision, permanent signing identity, and alignment; contains no generated JAR-signing `.SF` / `.RSA` / `MANIFEST.MF` material; and passes the retained F-Droid `apksigcopier` path with byte-for-byte identity.

Superseded v0.1.4 APK hashes:

- `450ca89ca53e875c4a1d6efa27928924a72b8324d762c6525f2471c80dfa1f3e`
- `f768d9ed09aa16d51a585470804374b52972f34ffb778f615b580de466b1d312`

## Developer-signing root cause

Controlled A/B testing established that relying on implicit Build-Tools 35 `apksigner` scheme defaults can generate JAR-signing metadata even when verifier output reports v1 as false. F-Droid signature-copy then fails with an APK Signature Scheme v3 `CHUNKED_SHA512` digest mismatch.

The validated GeoJoystick production signing path explicitly sets:

- v1 `false`
- v2 `true`
- v3 `true`
- v4 `false`
- `--alignment-preserved`

With those settings, no generated JAR-signing material is present and the copied-signature output verifies and is byte-identical to the signed input.

## Canonical-reference fdroiddata proof

A diagnostic fdroiddata branch changed only the v104 build-specific `binary:` URL to the canonical GitHub release APK:

- branch: `geojoystick-v104-canonical-reference-test`
- commit: `bcb24ac45aa8c15786a1da703cab547c2e08a19d`
- pipeline: `2773265250`
- exact `fdroid build` job: `15987693203`

That `fdroid build` job succeeded. It proved F-Droid independently built v104 from the exact release commit, retrieved the canonical GitHub APK, verified the APK signature state, successfully verified the copied-signature APK, and reported that the built binary matched the supplied developer binary. The prior `CHUNKED_SHA512` mismatch was absent.

The aggregate diagnostic pipeline failed only because `fdroid rewritemeta` reformatted the temporary build-specific `binary:` field into multiline YAML. That formatting-only failure is not part of the production metadata path.

## Production postbuild defect

The exact production MR commit was transferred unchanged to the maintainer fork as branch `geojoystick-v104-production-exact-test` and run through fdroiddata CI:

- exact production commit: `2974d67243aa1affbde77963db48a4e0ec7bc849`
- pipeline: `2773342977`
- `fdroid rewritemeta`: success
- `fdroid build`: failed

The production v104 metadata contains this additional block:

```yaml
srclibs:
  - reproducible-apk-tools@v0.3.0
postbuild:
  - mv $$OUT$$ unaligned.apk
  - $$reproducible-apk-tools$$/zipalign.py --page-size 4 --pad-like-apksigner
    --replace unaligned.apk $$OUT$$
```

The exact `fdroid build` trace shows that the postbuild runs after Gradle produces the unsigned release APK and immediately before F-Droid retrieves the canonical developer APK and performs copied-signature verification. With that postbuild present, the copied-signature APK fails v3 verification with `CHUNKED_SHA512 digest mismatch`.

## Conclusive production A/B

A controlled A/B removed **only those six metadata lines** from the exact production commit while preserving all other production fields:

- branch: `geojoystick-v104-production-no-postbuild-test`
- candidate commit: `38ad65480c0c25510e8aa1ed68fadc3b853ea30b`
- pipeline: `2773367289`
- `fdroid build` job: `15988436842` — success
- `fdroid rewritemeta` job: `15988436847` — success
- aggregate pipeline: success

The A/B proved:

- canonical global `Binaries:` URL unchanged
- permanent signing-key restriction unchanged
- exact v104 source commit unchanged
- Java 17 setup unchanged
- Gradle build unchanged
- `CurrentVersion` / `CurrentVersionCode` unchanged
- copied-signature APK verifies
- F-Droid-built binary matches the canonical developer APK
- `CHUNKED_SHA` mismatch is absent

Conclusion: **the remaining production v0.1.4 F-Droid failure is caused by the v104 `reproducible-apk-tools` APK-realignment postbuild. Removing that six-line block fixes production developer-binary verification.**

This is independent of the earlier developer-signing defect: the canonical APK is now correctly signed, and the production fdroiddata postbuild was subsequently modifying the unsigned APK into a byte layout incompatible with copying that canonical v2/v3 signature.

No additional APK rebuild, production signing, release-asset replacement, or device QA is required by the verified result.

## Remaining production action

MR !46016 and its bot source branch remain unchanged. The maintainer account can inspect the upstream MR but cannot retry pipelines in `fdroid/fdroiddata`; the retry endpoint returned `403 Forbidden`.

The minimal production fdroiddata correction is therefore:

1. remove only the v104 `srclibs` block for `reproducible-apk-tools@v0.3.0`;
2. remove only the v104 `postbuild` block that moves and realigns `$$OUT$$`;
3. retain the existing global `Binaries`, `AllowedAPKSigningKeys`, source commit, Java 17 setup, Gradle configuration, and CurrentVersion fields;
4. rerun the production fdroiddata checks;
5. keep Issue #38 open until version 104 is actually accepted and published.

Production F-Droid submission changes and human-facing upstream communication remain approval-gated.

## Reproducible-release requirements

For future releases intended for F-Droid developer-binary verification:

- build from a fresh clone at the exact release tag/commit;
- use the maintained JDK, Gradle, Android Gradle Plugin, SDK Platform, and Build-Tools versions;
- use an isolated/fresh Gradle user home for reproducibility evidence;
- keep AGP VCS metadata disabled in maintained release builds;
- compare unsigned outputs before production signing;
- set every relevant `apksigner` scheme explicitly rather than relying on defaults;
- verify certificate and v1/v2/v3/v4 state after signing;
- inspect `META-INF` for unexpected JAR-signing material;
- run the current F-Droid signature-copy compatibility gate against the exact unsigned APK and require the copied result to verify; require byte identity where expected;
- do not add an fdroiddata postbuild that changes the unsigned APK layout unless the developer APK was produced from that exact transformed unsigned layout and the signature-copy path is independently proven;
- never replace a published canonical APK silently; replacement requires explicit maintainer approval and recorded old/new hashes.

## Store metadata safety

- Keep all store screenshots synthetic/sanitized; never restore authentic precise-location screenshots.
- The map picker uses bundled HTML/CSS/JavaScript and downloads OpenStreetMap tiles only when opened.
- Preserve visible OpenStreetMap attribution and the ODbL notice.
- Keep F-Droid reproducibility evidence distinct from GitHub release/signing evidence.

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
