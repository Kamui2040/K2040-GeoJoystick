# Project Context

Last verified: 2026-08-04

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
- Draft PR #5: open, draft, mergeable, unmerged, targeting `main`
- Draft PR #4: closed unmerged as superseded

The commit containing this document is a documentation-only state reconciliation that follows the validated corrective implementation commit. Verify the live branch and PR head before publication-sensitive work.

PR #5 contains the accepted corrective implementation, but its description still refers to the earlier unvalidated four-file patch. Keep it draft until the description is reconciled and the remaining physical-device gates are completed or explicitly deferred.

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

The branch was clean after the corrective commit and push. This documentation-only reconciliation must also leave the branch clean after its own commit and push.

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

The source and test inputs have not changed since the accepted 13-path Android validation. This `PROJECT_CONTEXT.md` reconciliation is documentation-only and does not alter build inputs.

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

Private canonical evidence:

`G:\My Drive\Projects\Android\K2040-GeoJoystick\Reports\DeepReview\BuildValidation-Current`

No release signing, installation, physical-device QA, store submission, tag, public release, PR merge or `main` update was performed. No GitHub Actions or cloud CI endpoint was queried or used. Generated APKs remain private and uncommitted.

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

- Reconcile draft PR #5's stale description with the published corrective commit and accepted validation evidence.
- Reconcile app-op loss, Developer Options changes, provider removal, reboot/process-death behavior and truthful UI state on a physical device.
- Review overlay touch-target sizing and accessibility at device scale as a separate UI change.
- Harden `tools/build.py` ZIP extraction, downloads, subprocess timeouts and JDK ranking; make `build.bat` independent of caller working directory.
- Replace store screenshots with sanitized synthetic fixtures after source/device acceptance and decide the final store-icon corner treatment.
- Treat API 36 compile/target migration as a separate permission, foreground-service, notification, overlay, app-op, F-Droid and device-validation change.
- Keep signing, installation, device QA, reproducibility, store publication, public merge/release and final signoff as separate gates.

## Next accepted stages

1. Commit and fast-forward push this documentation-only state reconciliation.
2. Update draft PR #5 with the final branch head, accepted validation evidence and remaining device gates.
3. Keep PR #5 draft until physical-device QA is completed or explicitly deferred by the user.
4. Perform signing, installation and physical-device QA separately.
5. Do not merge `main`, tag, publish or submit to stores without user signoff.

## Public documentation policy

Public documentation contains user-facing behavior, contribution rules, security/privacy terms, attribution, release/F-Droid maintenance and current project state. Machine-specific paths, device identifiers, authentic coordinates, private QA, large logs and internal workflow history remain outside public Git.
