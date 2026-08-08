# Project Context

Last verified: 2026-08-08

## Identity and public release

GeoJoystick is a GPL-3.0-only Android mock-location joystick for emulator and developer testing through Android Developer Options and a visible foreground service.

- Repository: `Kamui2040/K2040-GeoJoystick`
- Default branch: `main`
- Package: `com.k2040.geojoystick`
- Current public version: 0.1.3 (`versionCode 103`)
- F-Droid: `https://f-droid.org/packages/com.k2040.geojoystick/`
- GitHub Releases: `https://github.com/Kamui2040/K2040-GeoJoystick/releases`
- Additional package listing: `https://apkpure.com/p/com.k2040.geojoystick`

GitHub Releases is canonical for developer-published assets and release notes. F-Droid is the official FLOSS source-built distribution and declares `TetheredNet` for user-initiated OpenStreetMap access.

## Repository state

- Remote `main`: `b0153a6c3ba1940f636d3930b99ebe1af4a68f59`
- Recovery/integration baseline: `recovery/pre-reinstall-maintenance-20260804` at `3e10ba220d80accfe7200092ceb97ea55cd4a44e`
- Current Feedback 5 source branch: `ui/welcome-version-changelog-feedback5-20260808`
- Current Feedback 5 source commit: `f127cbfbd5cd48a35360ff756896bb532e507896`
- Current Feedback 5 validation branch: `maintenance/signing-persistence-feedback5-20260808`
- Current Feedback 5 validation commit: `f4a650fbef4ac012b21b81d693319f4472514a5f`
- Current consolidated non-default development branch: `integration/current-development-20260808`
- Draft PR #5 remains open, draft, unmerged, and points at the older recovery line; it is not the current Feedback 5 development line.
- Temporary comparison PR #6 was closed unmerged after confirming it was not an appropriate integration path.
- `main`, tags, releases, F-Droid submission state, and store publication were not changed by the Feedback UI work.

The current development branch was created from the exact Feedback 5 validation branch. Before this context reconciliation, GitHub comparison reported it identical to `f4a650fbef4ac012b21b81d693319f4472514a5f`. The validation branch is exactly one commit ahead of the Feedback 5 source branch and adds only `tools/build_signed.py`.

Do not update this document solely to record the commit created by a documentation-only reconciliation. Verify mutable branch tips live when resuming work.

## Stable corrective baseline

The accepted recovery line provides the product-safety and runtime baseline:

- no implicit real-world/default coordinates;
- complete finite in-range latitude, longitude and altitude required before simulation start;
- selected, draft/saved, starting, provider-ready, published and active state kept distinct;
- active state requires successful publication through a ready mock provider;
- last-active coordinates persist only after successful publication;
- failure clears active state without erasing saved user data;
- null/restart and unsupported service actions remain inactive and non-sticky;
- map-link input is restricted to supported HTTPS hosts, bounded redirects/content and public destinations;
- app-internal state broadcasts are package-scoped and signature-protected;
- notification text does not disclose exact coordinates;
- bundled map uses no remote JavaScript or secret API key and preserves OpenStreetMap attribution;
- launcher artwork and monochrome icon use the accepted GeoJoystick joystick identity;
- public source contains zero required GitHub Actions workflows for the PC workflow.

Important accepted baseline commits include:

- corrective implementation: `f8c8a45d798dc676d377548cd8470c19e6cce02c`
- release-signing implementation: `498abe66702ce9f7c03fc8f206bfdfaf32623544`
- launcher-icon implementation: `5b33902411efdb58f8df707a93bc191aa6e88959`
- launcher-icon/context recovery head: `3e10ba220d80accfe7200092ceb97ea55cd4a44e`

## Current UI development line

The Feedback 5 source is twenty commits ahead of the recovery head and changes the current UI layer while preserving the corrective runtime baseline. Relative to the recovery head, the Feedback 5 validation line changes exactly these eight paths:

- `app/src/main/java/com/k2040/geojoystick/GeoSettings.java`
- `app/src/main/java/com/k2040/geojoystick/GeoUi.java`
- `app/src/main/java/com/k2040/geojoystick/JoystickOverlay.java`
- `app/src/main/java/com/k2040/geojoystick/JoystickView.java`
- `app/src/main/java/com/k2040/geojoystick/MainActivity.java`
- `app/src/main/java/com/k2040/geojoystick/MapActivity.java`
- `app/src/main/res/values/styles.xml`
- `tools/build_signed.py`

The current UI includes the accepted redesign work plus the latest welcome/changelog changes:

- centered Welcome card over a dimmed in-activity backdrop;
- compact Welcome layout with About and Thanks expandable rows;
- neutral Cancel and Continue actions with the same visual treatment;
- Cancel closes the task without acknowledging Welcome;
- Continue records acknowledgement and proceeds to the app;
- version number displayed directly below the `GeoJoystick` Welcome title without a `Version` prefix;
- Welcome navigation row named `Changelog` / `Änderungsprotokoll`;
- Changelog opens as a centered card rather than a full-screen page;
- Changelog card title is the current version number and the changelog body follows below it;
- larger accessible overlay controls and the current dark navy/electric-blue visual system.

Feedback 4's centered changelog-card presentation and associated Welcome behavior were physically reviewed and accepted before Feedback 5 changed the version presentation.

## Feedback 5 validation

### Signed build

Accepted local build/sign validation for source `f127cbfbd5cd48a35360ff756896bb532e507896` plus validation helper `f4a650fbef4ac012b21b81d693319f4472514a5f`:

- build/sign child process: pass;
- release lint/build: pass;
- package/version: `com.k2040.geojoystick` 0.1.3 (`103`);
- unsigned APK SHA-256 before cleanup: `a833f7b3a80325263f0df4208c6e62ebed13bc95575f7e47a439d7b7ff0853e6`;
- signed QA APK SHA-256: `d39fdcf60b32d58fe1b619383ed55be99ccf834953dd8de2a90f0211ddcfaf8f`;
- signer certificate SHA-256: `e0a833050d7c8fce7ddce85b2a86561304456d87b67bd6be1577d8f657e16778`;
- signature schemes: v2 only;
- APK alignment: pass;
- generated build residue removed after validation;
- signed QA APK was retained only for the then-pending device gate.

### Device installation

The exact signed Feedback 5 APK was fresh-installed on the designated Android QA device. Installation identity passed and no simulation or mock-location selection automation was performed.

The subsequent automated UI QA stopped safely during selector validation because the UI hierarchy contained two exact `GeoJoystick` text nodes while the harness expected exactly one. This is a QA-harness ambiguity, not evidence of a product UI defect. The Feedback 5 version-placement and changelog-card presentation therefore remain not yet visually accepted.

Do not rerun the failed automation unchanged. Future QA should select by a stable semantic relationship, bounds/content-description combination, or another unambiguous UI contract and should keep automation failure distinct from application failure.

## Signing and private material

Public/F-Droid builds remain independent of private signing material. Maintainer signing uses the external release identity whose accepted certificate SHA-256 is:

`e0a833050d7c8fce7ddce85b2a86561304456d87b67bd6be1577d8f657e16778`

`tools/build_signed.py` is an optional maintainer helper that reads signing credentials from a separate external properties file referenced by `GEOJOYSTICK_SIGNING_PROPERTIES`. It does not store credential values in Git. Signing keys, credential values and recovery material must remain outside Git and ordinary Google Drive.

## Build requirements

Verify the live environment before use. The accepted source baseline requires:

- Android Gradle Plugin 8.12.3
- Gradle 8.13
- Java/JDK 17
- minSdk 27
- compileSdk 35
- targetSdk 35
- preferred Android Build Tools 35.0.0
- Python 3 for repository-owned build/test tooling

The project command-line build path is authoritative; Android Studio is optional. A new workstation or operating system must revalidate exact toolchain paths and versions rather than inheriting prior machine state.

## Remaining gates

- Complete visual/interaction QA of Feedback 5 version placement and changelog card.
- Re-run remaining physical-device lifecycle checks: app-op loss, Developer Options changes, provider removal, reboot and process death.
- Validate truthful foreground-service, notification, overlay and UI-active state with synthetic coordinates, finishing inactive.
- Complete accessibility and device-scale touch-target review where still outstanding.
- Continue the separate `tools/build.py` hardening scope for downloads, extraction, subprocess timeouts and deterministic tool discovery.
- Replace store screenshots with sanitized synthetic fixtures after source/device acceptance.
- Treat API 36 compile/target migration as a separate reviewed scope.
- Reconcile the current development line into an appropriate review branch only after the remaining gates are completed or explicitly deferred.
- Do not merge `main`, create public tags/releases, publish APKs, or submit stores without explicit user signoff.

## Resume point

For a clean future checkout, treat `integration/current-development-20260808` as the current non-default development line and verify its live tip before work. `main` remains the public stable branch and is intentionally not advanced by this reconciliation.

## Public documentation policy

Public documentation contains user-facing behavior, contribution rules, security/privacy terms, attribution, release/F-Droid maintenance and current project state. Machine-specific paths, device identifiers, authentic coordinates, private QA, large logs, credential values and internal operator/transport details remain outside public Git.
