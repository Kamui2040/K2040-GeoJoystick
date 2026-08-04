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
- Published branch head and local `HEAD`: `900940a97de4a4239d1752f76be4051e37dd9f76`
- Published head tree: `a6714d3f6a301ea9248a468da531ee17546d4eaa`
- Draft PR #5: open, draft, unmerged, targeting `main`
- Draft PR #4: closed unmerged as superseded

PR #5 remains draft and not merge-ready. Its published head and description do not yet contain the accepted corrective implementation and validation record. The validated local tree must be committed and pushed intentionally, then the PR description must be reconciled before review readiness is reconsidered.

## Current local working tree

Exactly thirteen intentional unstaged files are modified:

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

The accepted corrective validation was performed on 2026-08-04 against local `HEAD` `900940a97de4a4239d1752f76be4051e37dd9f76`, tree `a6714d3f6a301ea9248a468da531ee17546d4eaa`, and the thirteen-path unstaged implementation state that preceded this documentation-only reconciliation.

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

- Commit and push the accepted thirteen-path tree to draft PR #5, then reconcile its stale description.
- Reconcile app-op loss, Developer Options changes, provider removal, reboot/process-death behavior and truthful UI state on a physical device.
- Review overlay touch-target sizing and accessibility at device scale as a separate UI change.
- Harden `tools/build.py` ZIP extraction, downloads, subprocess timeouts and JDK ranking; make `build.bat` independent of caller working directory.
- Replace store screenshots with sanitized synthetic fixtures after source/device acceptance and decide the final store-icon corner treatment.
- Treat API 36 compile/target migration as a separate permission, foreground-service, notification, overlay, app-op, F-Droid and device-validation change.
- Keep signing, installation, device QA, reproducibility, store publication, public merge/release and final signoff as separate gates.

## Next accepted stages

1. Reconcile this documentation-only status update and rerun exact repository checks plus `git diff --check`.
2. Commit the accepted thirteen-path tree intentionally on `recovery/pre-reinstall-maintenance-20260804`.
3. Push the non-default branch and update draft PR #5 with the new commit, validation evidence and remaining device gates.
4. Perform signing, installation and physical-device QA separately.
5. Do not merge `main`, tag, publish or submit to stores without user signoff.

## Public documentation policy

Public documentation contains user-facing behavior, contribution rules, security/privacy terms, attribution, release/F-Droid maintenance and current project state. Machine-specific paths, device identifiers, authentic coordinates, private QA, large logs and internal workflow history remain outside public Git.
