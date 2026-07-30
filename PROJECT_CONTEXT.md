# Project Context

Last verified: 2026-07-30

## Identity

- Product: **GeoJoystick**
- Public canonical repository: `Kamui2040/K2040-GeoJoystick`
- Package: `com.k2040.geojoystick`
- Default branch: `main`
- Licence: GPL-3.0-only
- MAIN_PC checkout: `D:\Projects\Android\K2040-GeoJoystick\repo`
- Private Drive workspace: `Projects/Android/K2040-GeoJoystick`

This document owns mutable project state. Durable product, safety, privacy and workflow rules belong in `AGENTS.md`; public privacy disclosure belongs in `PRIVACY.md`; F-Droid procedure belongs in `FDROID_NOTES.md`.

## Current release and distribution

- Current public version: **0.1.3** (`versionCode 103`)
- GitHub release line: `v0.1.3`
- F-Droid publishes 0.1.3/103 and retains 0.1.2/102
- F-Droid anti-feature disclosure: `TetheredNet`, due to user-initiated OpenStreetMap tile access
- APKPure listing is live for the same package
- GitHub Releases remains canonical for release notes and developer-published reference assets
- F-Droid is the official FLOSS source-built distribution

Published F-Droid 0.1.3 evidence is tied to source commit `e19b1ee13216b2de4f4cd890f00b2adabddd802f`, the accepted v2-only signer process and copied-signature/allowed-signer verification. It does not automatically validate later commits.

## Product baseline

GeoJoystick is a transparent Android mock-location joystick for emulator and developer testing. The public release includes:

- standard Developer Options mock-app selection;
- GPS/network test-provider publication through a foreground service;
- manual coordinates and altitude;
- OpenStreetMap picker and map-link coordinate extraction;
- movable compact/expanded overlay;
- walk, run, bike-style and custom speed presets;
- hold, pause, hide and stop;
- favorites and optional last-position restoration;
- overlay opacity, contrast and position reset;
- System/Light/Dark appearance;
- System/English/German language;
- no ads, billing, accounts, analytics, tracking, subscriptions or updater.

## Current technical evidence

Verify live values before changes. The last audited release line used Java 17, compile SDK 35, target SDK 32, minimum SDK 27 and Gradle 8.13. `app/build.gradle`, the manifest and repository build tooling remain authoritative.

The app uses direct Java/Android framework code, bundled map HTML/JavaScript, OpenStreetMap tiles and no declared app dependency or identified native binary. `NOTICE.md` records the GPL derivative relationship to `ZCShou/GoGoGo` baseline `de0d596190c57b8ca71481f60ce6b9e50af5107f`.

## Verified evidence and limits

Recorded evidence includes F-Droid build/metadata/reproducibility checks, public-release installation and standard mock-location smoke testing, physical-device validation of the dark-dialog fix and historical GitHub Actions builds for v0.1.3.

Historical Actions results are evidence only. GitHub Actions is no longer authorized for the PC workflow and the tracked workflow is removed by the current governance-alignment branch.

Broader lifecycle gaps remain for provider-ready/publish acknowledgement, app-op loss, Developer Options changes, reboot/process death and exact UI/service reconciliation. Link resolution and the WebView bridge also require future hostile-input hardening. These are implementation tasks, not claims of current failure in every path.

## Current maintenance state

- Branch: `docs/autonomy-governance-alignment`
- Draft pull request: `#4` — **Align governance with autonomous maintenance**
- Base: `main`

The pull request scope is limited to:

- self-contained autonomous repository rules;
- current F-Droid/release documentation;
- public README links/disclosures;
- removal of `.github/workflows/android-build.yml`;
- no source, manifest, dependency, build-tool, signing, package, device or release change.

Remaining gates:

- MAIN_PC review of the branch and `git diff --check`;
- optional local Markdown/link checks;
- user approval before merge to `main`;
- implementation of a repository-owned unsigned-release/lint mode remains separate and requires local build validation;
- lifecycle, hostile-input, permission/target-SDK and reproducibility maintenance remain separate tasks;
- signing, installation, store submission, public tag/release and final signoff remain manual.

## Public links

- Source: `https://github.com/Kamui2040/K2040-GeoJoystick`
- Releases: `https://github.com/Kamui2040/K2040-GeoJoystick/releases`
- F-Droid: `https://f-droid.org/packages/com.k2040.geojoystick/`
- APKPure: `https://apkpure.com/p/com.k2040.geojoystick`
- Privacy: `PRIVACY.md`
