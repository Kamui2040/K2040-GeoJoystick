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

## Current repository state

- Remote `main`: `b0153a6c3ba1940f636d3930b99ebe1af4a68f59`
- Recovery branch: `recovery/pre-reinstall-maintenance-20260804`
- Exact recovery checkpoint: `0d8da0833240a85faaf3b18bf56871089f975642`
- Recovered checkpoint tree: `b85093723cbbbb37a3240e7aaf9e2078d32e4f63`
- Recovered records: 60
- Backup state: preserved and unchanged
- Working tree at checkpoint: clean
- Push/publication: not performed

The checkpoint reproduces the exact validated pre-reinstall maintenance tree. It includes source, resource, manifest, parser, build-tooling, metadata, and public-documentation work. Detailed recovery evidence remains private and is not a public build dependency.

Draft PR #4 (`docs/autonomy-governance-alignment`, head `8980e4ce131e5ec9fbfe2874180715b4dc4a9c7f`) remains open and unmerged. It overlaps the recovery branch in governance documentation and workflow removal, but contains machine-specific paths and superseded execution boundaries. Do not merge it into the recovery branch. Keep it open only until a validated replacement branch/PR exists, then close it without rewriting public history.

## Current source and toolchain baseline

Verify live files before release or publication.

- Android Gradle Plugin: 8.12.3
- Gradle: 8.13
- Java source/target and required runtime: 17
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

Accepted workstation baseline from 2026-08-03: PowerShell 7.6.4, Git 2.55.0, GitHub CLI 2.97.0, Python 3.12.10 x64, JDK 17.0.20, Android API 35, Build Tools 35.0.0, ADB 37.0.1, and 7-Zip 26.02.

Repository-scoped Gradle/AGP task discovery and the recovered branch's build/test/lint behavior still require validation on the reinstalled workstation.

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

- Re-run repository-owned parser, debug, release-lint, and unsigned-release validation after the workstation reinstall.
- Distinguish provider readiness, successful publish, active service, notification, overlay, and UI state throughout lifecycle reconciliation.
- Reassess app-op loss, Developer Options changes, provider removal, reboot, and process death.
- Continue hostile-input hardening for shared links, redirects, WebView messages, and external data.
- Reassess foreground-service, notification, overlay, exported-component, permission, and app-op behavior at each target-SDK change.
- Keep signing, installation, device QA, F-Droid reproducibility, store publication, public merge/release, and final signoff as separate evidence gates.

## Next accepted stages

1. Commit this sanitized governance reconciliation on the recovery branch.
2. Run a separate local repository/toolchain and build validation using repository-owned entry points; do not query or depend on GitHub Actions.
3. Reconcile any validated build findings in a focused follow-up commit.
4. Push the non-default branch and open a replacement draft PR only after local validation is accepted.
5. Close superseded draft PR #4 after the replacement PR exists.
6. Keep `main` merge, tag, release, store actions, signing, installation, and final signoff separate.

## Public documentation policy

Public changelogs contain only changes that affect people using GeoJoystick. Internal workflow preferences, machine-specific paths, device-specific QA notes, large logs, and private release/recovery evidence remain outside public Git.
