# Project Context

Last verified: 2026-08-04

## Identity and public release

GeoJoystick is an open-source Android mock-location joystick for emulator and developer testing through Android's standard Developer Options mock-location flow and a visible foreground service.

- Public repository: `Kamui2040/K2040-GeoJoystick`
- Default branch: `main`
- Package: `com.k2040.geojoystick`
- Licence: GPL-3.0-only
- Current public version: 0.1.3 (`versionCode 103`)
- F-Droid: `https://f-droid.org/packages/com.k2040.geojoystick/`
- GitHub Releases: `https://github.com/Kamui2040/K2040-GeoJoystick/releases`
- APKPure: `https://apkpure.com/p/com.k2040.geojoystick`

GitHub Releases is canonical for release notes and developer-published assets. F-Droid is the official FLOSS source-built distribution. F-Droid declares `TetheredNet` for user-initiated OpenStreetMap access.

## Current repository and recovery state

- Remote `main`, verified 2026-08-04: `b0153a6c3ba1940f636d3930b99ebe1af4a68f59`
- Recovery branch: `recovery/pre-reinstall-maintenance-20260804`
- Exact recovery checkpoint: `0d8da0833240a85faaf3b18bf56871089f975642`
- Recovered checkpoint tree: `b85093723cbbbb37a3240e7aaf9e2078d32e4f63`
- Governance reconciliation baseline: `e9d8dedba5b224c98a8f4cd4bc4c4c827c7fd4da`
- Locally validated baseline tree: `0c00f48c928b4b5111d7b2c4114cd6db5b6455df`
- Backup state: preserved and unchanged
- Initial validated branch publication: `6d7bbeb0354003db3a145f6972c08293ab60fe88`
- Active replacement draft PR: #5 from `recovery/pre-reinstall-maintenance-20260804` to `main`
- Superseded draft PR #4: closed unmerged

The recovery checkpoint reproduces the exact validated pre-reinstall maintenance tree. The governance baseline adds sanitized repository rules and mutable-state documentation without replacing the recovered implementation. Detailed recovery and validation evidence remains private and is not a build, installation, or runtime dependency.

Draft PR #5 (`recovery/pre-reinstall-maintenance-20260804` to `main`) is the active validated replacement. Draft PR #4 (`docs/autonomy-governance-alignment`) was closed unmerged after PR #5 was confirmed. Do not merge PR #4 or rewrite public history.

## Current source and toolchain baseline

Verify live files before release or publication.

- Android Gradle Plugin: 8.12.3
- Gradle: 8.13
- Java source, target, and required runtime: 17
- Minimum SDK: 27
- Compile SDK: 35
- Target SDK: 35
- Preferred Android Build Tools: 35.0.0
- `MainActivity`: exported for launcher and reviewed `text/plain` sharing
- `MapActivity`: non-exported
- `MockLocationService`: non-exported
- Cleartext traffic: disabled
- App backup: disabled
- Android Studio: optional; command-line repository tooling is authoritative

Accepted workstation baseline: PowerShell 7.6.4, Git 2.55.0, GitHub CLI 2.97.0, Python 3.12.10 x64, JDK 17.0.20, Android API 35, Build Tools 35.0.0, ADB 37.0.1, and 7-Zip 26.02.

## Accepted local validation

The recovered branch was validated on the reinstalled MAIN_PC against commit `e9d8dedba5b224c98a8f4cd4bc4c4c827c7fd4da` and tree `0c00f48c928b4b5111d7b2c4114cd6db5b6455df`.

Accepted results:

- repository identity, branch, commit, tree, and clean working state: pass;
- Gradle 8.13 distribution checksum and bootstrap: pass;
- Gradle launcher JVM and daemon JVM bound to JDK 17.0.20: pass;
- dependency-free `LocationLinkParser` self-test: pass;
- `:app:clean`: pass with `UP-TO-DATE` recorded truthfully;
- `:app:assembleDebug`: executed successfully;
- debug APK package/version identity and signature verification: pass;
- `:app:lintRelease`: executed successfully;
- release lint: zero fatal issues, zero errors, and two warnings;
- retained warning IDs: `GradleDependency` and `OldTargetApi`;
- stale empty untracked `mipmap-anydpi-v26` directory: removed without a tracked source change;
- `ObsoleteSdkInt`: absent from the fresh lint report;
- `:app:lintVitalRelease`: executed successfully, not skipped or absent;
- `:app:assembleRelease`: executed successfully;
- unsigned release APK package/version identity, ZIP/CRC integrity, and unsigned state: pass;
- debug and unsigned release artifacts remained unchanged during the final lint rerun;
- `git diff --check`: pass;
- final working tree before this documentation update: clean.

The two retained lint warnings reflect the accepted API 35 baseline. Compile/target SDK migration to API 36 requires a separate permission, foreground-service, notification, overlay, app-op, compatibility, F-Droid, and device-validation review; the warnings are not treated as permanently waived.

No APK was signed for release, installed, submitted to a store, published, or tested on a physical device during this recovery validation. Generated APKs and detailed logs remain local/private and are not committed.

## Product baseline

- Manual latitude, longitude, and altitude
- Bundled OpenStreetMap picker with no remote JavaScript
- Supported copied/shared map-link coordinate import
- Floating compact and expanded joystick modes
- Walk, run, bike-style, and custom speed presets
- Hold, pause, hide, and stop controls
- Favorites and optional position restoration
- Overlay opacity, contrast, and position settings
- System/Light/Dark appearance
- System/English/German language
- Persistent foreground notification with a reliable stop action
- No ads, billing, subscriptions, accounts, analytics, tracking, telemetry, or updater

GeoJoystick does not conceal Android mock-location status and is not game, cheating, anti-detection, integrity-bypass, or ban-evasion tooling.

## Known implementation and validation gaps

- Distinguish provider readiness, successful publish, active service, notification, overlay, and UI state throughout lifecycle reconciliation.
- Reassess app-op loss, Developer Options changes, provider removal, reboot, and process death.
- Continue hostile-input hardening for shared links, redirects, WebView messages, and external data.
- Perform the API 36 compile/target SDK migration only as a separate reviewed change.
- Reassess foreground-service, notification, overlay, exported-component, permission, and app-op behavior at each target-SDK change.
- Keep signing, installation, device QA, F-Droid reproducibility, store publication, public merge/release, and final signoff as separate evidence gates.

## Next accepted stages

1. Review draft PR #5 against `main` and keep it in draft until its source and documentation scope is accepted.
2. Re-run relevant local validation gates for any later source, build-tool, dependency, manifest, resource, or target-SDK change.
3. Keep `main` merge, tags, releases, signing, installation, store actions, and final signoff separate.
4. Continue lifecycle, hostile-input, permission, API 36, and reproducibility maintenance as focused follow-up work.

## Public documentation policy

Public changelogs contain only changes that affect people using GeoJoystick. Internal workflow preferences, machine-specific paths, device-specific QA notes, large logs, and private release/recovery evidence remain outside public Git.
