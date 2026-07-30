# F-Droid maintenance and reproducible-build notes

Last verified: 2026-07-30

## Current status

GeoJoystick is published on F-Droid.

- Package: `com.k2040.geojoystick`
- Licence: GPL-3.0-only
- Current F-Droid version: 0.1.3 (103)
- Initial published version: 0.1.2 (102)
- Accepted anti-feature: `TetheredNet`
- Official listing: https://f-droid.org/packages/com.k2040.geojoystick/
- GitHub Releases remains canonical for release notes and developer-published reference APKs

This is an ongoing maintenance document, not a pre-submission checklist.

## Inclusion baseline

The distributable app remains ad-free, analytics-free, tracking-free, account-free, subscription-free and billing-free. It has no proprietary Android dependency or in-app updater. Core implementation uses direct Android framework APIs.

The map HTML and JavaScript are bundled. OpenStreetMap tiles and supported map-link resolution use the network only after explicit user action. Keep `AntiFeatures: TetheredNet` while that dependency exists, and keep public metadata consistent with `PRIVACY.md`.

## Build discovery and local validation

F-Droid uses standard Gradle discovery rather than the Windows bootstrap. Keep `gradle/wrapper/gradle-wrapper.properties` committed and synchronized with the Gradle version used by `tools/build.py`. Wrapper binaries do not need to be committed solely for discovery.

Before an update, verify from a clean source checkout that the intended Gradle version is discoverable and the release task is available.

The current public Windows bootstrap builds debug only. Before future release work, add and locally validate a repository-owned explicit unsigned-release mode that:

- uses the same official Gradle source and published checksum verification;
- uses the same Java, Android SDK and Build-Tools discovery;
- runs release lint and `assembleRelease` as applicable;
- writes a clearly named unsigned APK and SHA-256 record;
- fails when expected artifacts are missing;
- keeps signing entirely separate.

Do not use GitHub Actions or a global Gradle installation as a fallback. Local build/test/lint execution remains a MAIN_PC gate.

## Metadata requirements

For every update:

- use the exact full source commit;
- ensure the release tag resolves to that commit;
- synchronize version code, version name, changelog, summary, full description, icon, screenshots, licence, NOTICE and privacy disclosure;
- ensure every Fastlane file referenced by fdroiddata is tracked at the source commit;
- preserve `AntiFeatures: TetheredNet` while OpenStreetMap is used;
- preserve the versioned `Binaries` URL and allowed signer fingerprint where the reproducible-binary model is used;
- run the target fdroiddata branch's `fdroid rewritemeta` and `fdroid lint`;
- accept canonical formatter output and verify no required file remains untracked.

Do not hand-format metadata after `rewritemeta` has produced canonical output.

## Reproducible-build model

F-Droid builds the unsigned APK from source and compares it with the developer reference APK by copying the reference signature onto the F-Droid-built payload. The developer reference APK must therefore be signed from the exact successful F-Droid-built unsigned artifact, not from a local rebuild or similarly named file.

For the established GeoJoystick 0.1.3 process:

- use the exact pipeline `tmp/com.k2040.geojoystick_<versionCode>.apk` identified by successful build output;
- preserve alignment and make no APK-content change after signing;
- use v2 signing only, with v1, v3, v3.1 and v4 disabled, unless a later verified process deliberately changes this;
- verify package, version, signing schemes, certificate fingerprint, alignment and hashes;
- require copied-signature and allowed-signer success before claiming reproducibility.

Current recorded allowed signer fingerprint:

`e0a833050d7c8fce7ddce85b2a86561304456d87b67bd6be1577d8f657e16778`

Do not change the signer expectation without an explicit signing-key migration review.

## Update sequence

1. Verify the clean repository, governing files, source, manifest, permissions, dependencies, network behavior, licence, NOTICE, privacy policy, Fastlane metadata, screenshots and icon.
2. Synchronize version and changelog only in an approved release task.
3. Run local syntax/resource/localization checks, `git diff --check`, debug build, unsigned release, release lint, dependency, permission and APK checks.
4. Stop if the repository-owned unsigned-release mode is unavailable; add and validate it locally rather than using cloud CI.
5. After publication approval, create the source commit and tag and record the exact full commit.
6. Update fdroiddata using the exact version, commit, `TetheredNet`, `Binaries` and signer data.
7. Run `fdroid rewritemeta`, `fdroid lint` and applicable build/test checks.
8. Let F-Droid produce the authoritative unsigned APK.
9. Download the successful artifact, select the exact identified `tmp` APK and record its hash/provenance.
10. Sign that exact file with the reviewed scheme, preserving alignment and changing no content.
11. Verify package, version, schemes, signer, alignment and final hash.
12. Prove copied-signature compatibility before upload.
13. Upload the approved signed APK once to the versioned release URL referenced by `Binaries`.
14. Download it anonymously and compare its hash and signer.
15. Require final fdroiddata build, lint, metadata, binary, copied-signature and allowed-signer checks to pass.
16. After publication, verify the official page, version, anti-feature, permissions, source link, build log, reproducibility status and date.
17. Update `PROJECT_CONTEXT.md`, `README.md`, this file and the project/shared failure dictionaries.

## Evidence boundaries

- A local build is not proof of F-Droid reproducibility.
- A normally installable signed APK is not proof of copied-signature compatibility.
- A matching certificate is not proof that the unsigned payload is byte-identical.
- A formatter pass is not proof that all referenced upstream files are tracked.
- A merged fdroiddata change is not proof that a version is already published.

Verify each gate independently and record exact evidence.
