# Project Context

Last verified: 2026-08-06

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

## Repository and pull-request state

- Remote `main`: `b0153a6c3ba1940f636d3930b99ebe1af4a68f59`
- Recovery branch: `recovery/pre-reinstall-maintenance-20260804`
- Recovery checkpoint: `0d8da0833240a85faaf3b18bf56871089f975642`
- Recovery checkpoint tree: `b85093723cbbbb37a3240e7aaf9e2078d32e4f63`
- Governance reconciliation: `e9d8dedba5b224c98a8f4cd4bc4c4c827c7fd4da`
- Validation-record commit: `6d7bbeb0354003db3a145f6972c08293ab60fe88`
- Prior PR-state documentation commit: `900940a97de4a4239d1752f76be4051e37dd9f76`
- Validated corrective implementation commit: `f8c8a45d798dc676d377548cd8470c19e6cce02c`
- Validated corrective implementation tree: `f3ddad345231215497a45e02f447de26cf0e3ede`
- Release-signing branch: `maintenance/release-signing-20260806`
- Validated release-signing implementation commit: `498abe66702ce9f7c03fc8f206bfdfaf32623544`
- Validated release-signing implementation tree: `7d10f6ea3806e75ed7926b986917ae6bc7bea720`
- Release-signing context reconciliation milestone: `9bd417fe28966b5e7411282da23e3034cb0ce351`, tree `105a42ca610f68f3d908158d7ad60ce90e535b71`
- Draft PR #5: open, draft, mergeable, unmerged, targeting `main`; its recovery head branch was fast-forwarded through the release-signing context reconciliation milestone and its description was reconciled on 2026-08-06
- Draft PR #4: closed unmerged as superseded

The release-signing implementation and context-reconciliation commits are durable public milestone references. Obtain mutable branch and PR tips from live GitHub state instead of updating this document solely to chase its own documentation commit.

PR #5 now includes the accepted corrective implementation, validated release-signing workflow and reconciled project context. Keep it draft until the remaining physical-device gates are completed or explicitly deferred.

## Published corrective scope

The validated corrective implementation commit contains these thirteen reviewed paths:

- `AGENTS.md`
- `PROJECT_CONTEXT.md`
- `README.md`
- `app/build.gradle`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/assets/map.html`
- `app/src/main/java/com/k2040/geojoystick/JoystickOverlay.java`
- `app/src/main/java/com/k2040/geojoystick/LocationLinkParser.java`
- `app/src/main/java/com/k2040/geojoystick/MainActivity.java`
- `app/src/main/java/com/k2040/geojoystick/MapActivity.java`
- `app/src/main/java/com/k2040/geojoystick/MockLocationService.java`
- `app/src/main/res/values/strings.xml`
- `tools/test_location_link_parser.py`

The branch was clean after the corrective commit and push. The later release-signing and documentation milestones also left their non-default branches clean and synchronized.

The accepted implementation:

- opens the map without assigning a default selection or coordinates;
- keeps the map unselected until the user taps it and requires altitude only when starting simulation;
- preserves partial coordinate drafts without persisting them as a valid active position;
- consumes shared intents once and rejects stale out-of-order link-import results;
- rejects non-finite and out-of-range coordinates, altitude and custom-speed input;
- removes Berlin and other unconfirmed runtime defaults from the map, activity and overlay flow;
- stops service instances created by inactive, unsupported or failed actions;
- separates attempted movement coordinates from the last successfully published position;
- marks simulation active only after valid coordinates, ready providers and successful publication;
- persists last-active coordinates only after successful publication;
- clears active state and reports provider or publication failure without erasing saved user data;
- package-scopes and signature-protects the internal state broadcast, with an explicitly non-exported Android 13+ receiver;
- removes exact coordinates from the foreground notification and localizes its status and actions;
- removes the restrictive OpenStreetMap tile referrer policy and identifies the WebView map client;
- restricts map-link resolution to supported HTTPS hosts, bounded redirects/content, public destinations and unambiguous coordinate formats;
- enables generated `BuildConfig` for the app version used by the map client identifier;
- removes the confirmed unused notification-channel resource;
- extends the dependency-free parser test harness for the new boundaries.

The corrective Android source and parser-test inputs remain unchanged since the accepted 13-path Android validation. The later release-signing milestone changes only `tools/build.py`, `README.md` and `FDROID_NOTES.md`; it does not alter Android runtime behavior.

## Toolchain baseline

Verify live files and executables before use.

- Android Gradle Plugin: 8.12.3
- Gradle: 8.13
- Java source/target/runtime: 17
- Minimum SDK: 27
- Compile SDK: 35
- Target SDK: 35
- Preferred Build Tools: 35.0.0
- PowerShell: 7.6.4
- Git: 2.55.0
- GitHub CLI: 2.97.0
- Python: 3.12.10 x64
- JDK: 17.0.20
- ADB: 37.0.1
- Android Studio: optional; repository command-line tooling is authoritative

## Accepted local validation

The accepted corrective validation was performed on 2026-08-04 against the thirteen-path implementation state based on `900940a97de4a4239d1752f76be4051e37dd9f76` and was then committed without build-input changes as `f8c8a45d798dc676d377548cd8470c19e6cce02c`, tree `f3ddad345231215497a45e02f447de26cf0e3ede`. `PROJECT_CONTEXT.md` was reconciled after validation; source and build inputs were unchanged.

Accepted results:

- exact repository identity, origin, branch, `HEAD`, tree and single-worktree state: pass;
- zero staged and untracked paths before and after validation: pass;
- tracked GitHub workflow count: zero;
- `git diff --check`: pass;
- Python 3.12.10 x64: verified;
- JDK and `javac` 17.0.20: verified;
- Gradle 8.13: verified;
- Android API 35 and Build Tools 35.0.0: verified;
- `LocationLinkParser` self-test: pass;
- generated debug and release `BuildConfig`: pass;
- debug Java compilation and `:app:assembleDebug`: pass;
- release Java compilation and `:app:assembleRelease`: pass;
- `:app:lintRelease`: pass with zero fatal issues, zero errors and exactly `GradleDependency: 1` plus `OldTargetApi: 1`;
- `:app:lintVitalRelease`: legitimately skipped because no vital work was required;
- debug APK identity: `com.k2040.geojoystick`, version 0.1.3 (`103`), minSdk 27, targetSdk 35;
- debug APK ZIP/CRC, alignment and signature verification: pass;
- unsigned release APK identity, ZIP/CRC, alignment and unsigned-state verification: pass;
- generated `.gradle`, build, `dist` and `local.properties` output cleanup after success: pass;
- final repository state: thirteen modified paths, zero staged, zero untracked, zero ignored and zero generated residue.

Private canonical evidence is retained outside public Git under `BuildValidation-Current`.

That 2026-08-04 corrective validation did not perform release signing, installation, physical-device QA, store submission, tagging, public release, PR merge or a `main` update. No GitHub Actions or cloud CI endpoint was queried or used.

## Accepted release-signing integration and validation

The separate release-signing milestone was validated and published on 2026-08-06 as commit `498abe66702ce9f7c03fc8f206bfdfaf32623544`, tree `7d10f6ea3806e75ed7926b986917ae6bc7bea720`.

The accepted workflow:

- adds an explicit local `--signed-release` mode while preserving the unsigned release path used for public/F-Droid builds;
- reads keystore and private-key passwords interactively and does not persist them in files, Git, reports or command-line arguments;
- keeps the release keystore outside the repository and independently verifies its known file identity, alias and certificate fingerprint;
- signs the exact unsigned release artifact with APK Signature Scheme v2 only;
- independently verifies package `com.k2040.geojoystick`, version 0.1.3 (`103`), minSdk 27, targetSdk 35, signer identity and APK alignment;
- preserves a clean public source tree and removes generated APKs and build output after validation.

Accepted results:

- `LocationLinkParser` self-test: pass;
- release lint, `lintVitalRelease` handling and release assembly: pass;
- unsigned release APK SHA-256: `d6e886af513ebcfce195b2e0676a75522045b4f1d1267a7ae8c766f413e1ed01`;
- signed validation APK SHA-256: `039f39c1e79a8519f2ced66f9a944a6d2482c4b8d58b387579cabc9e1fb77d94`;
- accepted signer certificate SHA-256: `e0a833050d7c8fce7ddce85b2a86561304456d87b67bd6be1577d8f657e16778`;
- signature schemes: v1 false, v2 true, v3 false and v4 false;
- signed APK alignment: pass;
- generated APK/output cleanup: pass;
- final repository state after commit and push: clean, one worktree, zero tracked workflows, synchronized upstream.

Private canonical evidence is retained outside public Git under `ReleaseSigningIntegration-Current`, `ReleaseSigningValidation-Current`, `ReleaseSigningCommit-Current` and `ReleaseSigningContextReconciliation-Current`.

No APK was retained or published. Draft PR #5's head branch and description were reconciled after validation; installation, physical-device QA, store submission, tag, release, PR merge and `main` update were not performed. No GitHub Actions workflow was created, dispatched, rerun or used for validation.

## Product baseline

- Manual latitude, longitude and altitude
- Bundled OpenStreetMap picker with no remote JavaScript
- Supported copied/shared map-link import
- Floating compact and expanded joystick modes
- Walk, run, bike-style and custom speed presets
- Hold, pause, hide and stop controls
- Favorites and optional position restoration
- Overlay opacity, contrast and position settings
- System/Light/Dark appearance
- System/English/German language
- Truthful foreground notification and stop action
- No ads, billing, subscriptions, accounts, analytics, tracking, telemetry or updater

GeoJoystick does not conceal Android mock-location status and is not game, cheating, anti-detection, integrity-bypass or ban-evasion tooling.

## Remaining gaps

- Keep draft PR #5 in draft until physical-device QA is completed or explicitly deferred.
- Reconcile app-op loss, Developer Options changes, provider removal, reboot/process-death behavior and truthful UI state on a physical device.
- Review overlay touch-target sizing and accessibility at device scale as a separate UI change.
- Continue the separate `tools/build.py` hardening scope for ZIP extraction, downloads, subprocess timeouts and JDK ranking; make `build.bat` independent of caller working directory.
- Replace store screenshots with sanitized synthetic fixtures after source/device acceptance and decide the final store-icon corner treatment.
- Treat API 36 compile/target migration as a separate permission, foreground-service, notification, overlay, app-op, F-Droid and device-validation change.
- Keep installation, device QA, reproducibility, store publication, public merge/release and final signoff as separate gates.

## Next accepted stages

1. Synchronize the clean MAIN_PC repository with the reconciled non-default remote branches before further local work.
2. Perform installation and physical-device QA separately using synthetic coordinates and an explicitly selected ADB device.
3. Keep PR #5 draft until the remaining physical-device gates are completed or explicitly deferred.
4. Continue build-script hardening, store-asset replacement and API 36 migration only as separate reviewed scopes.
5. Do not merge `main`, tag, publish or submit to stores without user signoff.

## Public documentation policy

Public documentation contains user-facing behavior, contribution rules, security/privacy terms, attribution, release/F-Droid maintenance and current project state. Machine-specific paths, device identifiers, authentic coordinates, private QA, large logs and internal workflow history remain outside public Git.
