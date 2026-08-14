# Project Context

## Purpose

GeoJoystick is an open-source Android mock-location joystick intended for emulator and developer testing. It provides a floating joystick, speed presets, manual coordinates, an internal map picker, map-link coordinate import, and a persistent foreground service.

## Current milestone

Version 0.1.3 public release

- Public release: v0.1.3
- Version code: 103
- F-Droid submission MR !42238 has been merged into fdroid/fdroiddata master
- F-Droid currently publishes v0.1.2 until the v0.1.3 metadata/build update is processed
- Official APKPure listing is live: https://apkpure.com/p/com.k2040.geojoystick
- Standard Android Developer Options mock-location provider
- Foreground service publishing GPS and network test-provider locations
- Movable overlay joystick with expanded and compact modes
- Walk, run, bike-style, and user-defined custom speed presets
- Hold, pause, hide, stop, saved overlay mode, saved speed, overlay opacity, high contrast, and reset overlay position
- App settings for System/Light/Dark appearance and System/English/German language
- Optional restore-last-position behavior and five named favorite-location slots
- OpenStreetMap tile-based picker with no API key and no external JavaScript dependency
- Coordinate extraction from full or shortened map links
- In-app About / Support section with K2040 avatar and Ko-fi link
- No ads, billing, accounts, analytics, subscriptions, or updater

## Upstream relationship

The project reuses and simplifies the architecture and movement approach of `ZCShou/GoGoGo`, baseline commit `de0d596190c57b8ca71481f60ce6b9e50af5107f`, under GPL-3.0-only. The Baidu SDK, embedded signing configuration, updater, logging stack, history database, and legacy permissions are intentionally not carried over.

## Release positioning

Public descriptions should present GeoJoystick as a mock-location utility for emulator and developer testing. Do not market it as game tooling, cheating software, anti-detection tooling, or a bypass utility.

## F-Droid / FLOSS status

The app is GPL-3.0-only, has no ads, no analytics, no accounts, and uses direct Android framework code. Upstream Fastlane-style metadata exists under `fastlane/metadata/android/en-US/`.

The F-Droid submission has been merged and the first store listing is live. The final F-Droid metadata includes `AntiFeatures: TetheredNet` because the map picker uses OpenStreetMap services.

## Other distribution

The official APKPure listing is live at https://apkpure.com/p/com.k2040.geojoystick. GitHub Releases remains the canonical source for release notes and published release assets. No APKPure installation smoke test is recorded in this repository yet.

## Validation status

- F-Droid MR build/test/rewritemeta pipeline passed after the TetheredNet metadata update
- Reproducible-build issue was fixed by signing the F-Droid CI-built unsigned APK with the release key using v2 signing and preserved alignment
- Physical-phone smoke test confirmed the public release installs and standard mock location works
- Dark-dialog fix was validated on a physical phone
- GitHub Actions successfully built both debug and unsigned release APKs for v0.1.3
- Dedicated GeoJoystick store icon included for v0.1.3

## Current build

- Version: 0.1.3
- Version code: 103
- Baseline: public / F-Droid release line
- Release changes: dark-theme dialog fix and dedicated store-listing icon

## Canonical public repository

https://github.com/Kamui2040/K2040-GeoJoystick

## Current accepted development reconciliation

Feedback 5 visual presentation was accepted on 2026-08-10 from sanitized physical-device captures. The accepted Welcome presentation shows the bare current version directly below the GeoJoystick title, the Changelog row, separate neutral Cancel/Continue actions, and no visible clipping or overlap. Changelog opens as a centered dimmed-background card headed by the current version.

The focused development line was reconstructed from current `main` plus the accepted runtime/UI source rather than by merging the migration branch wholesale. The maintained integration now lives on `feat/onboarding-about-card-13`, tracked by draft PR #14. This keeps the newer public-safe `main` governance and clean-Linux bootstrap fix, imports the accepted runtime/state/input/UI changes, removes the legacy PC GitHub Actions workflow, and removes the obsolete unsanitized phone screenshots from the maintained tree.

Validation performed locally before the reconciliation commit:

- `git diff --check`: PASS
- dependency-free `LocationLinkParser` self-test: PASS
- repository bootstrap/debug build with JDK 17: PASS
- `:app:lintRelease`: PASS
- unsigned release assembly: PASS
- `:app:testDebugUnitTest`: NO-SOURCE; accepted because no Java/Kotlin unit-test sources exist at that validation boundary
- debug APK identity: PASS for `com.k2040.geojoystick` 0.1.3 (103)

Issue #9 physical-device lifecycle/state reconciliation is accepted and closed as completed as of 2026-08-14. Accepted coverage includes fresh/unset and invalid-input behavior, successful-publication gating and last-active persistence, mock-location app-op/manual deselection reconciliation, provider/publication failure cleanup, process-death and reboot stale-state behavior, restore-last-position behavior, all three user-visible stop paths, and a final inactive state. The accepted final stop-path run verified the main-screen Stop, overlay Stop/X, and notification Stop paths independently; each ended with mock providers, overlay, service, and foreground notification inactive. Android mock-location-app selection remained manual/user-controlled throughout.

The remaining integration blocker is Issue #15 physical-device visual acceptance for overlay symbol sizing. Signing, reproducibility, release/F-Droid/store validation, tags, releases, and publication remain separate future gates.

## Current Issue #13 development

The focused branch is `feat/onboarding-about-card-13`.

The current Issue #13 source revision separates licence scope by authorship/provenance:

- application code remains `GPL-3.0-only`;
- K2040-authored GPL code carries the separate GPLv3 section 7(b) attribution-preservation term only where the source file explicitly marks that term as applicable;
- original K2040 artwork/UI artwork established by project provenance is `CC-BY-4.0`, including the bundled K2040 avatar;
- GoGoGo-derived and other third-party code/assets/data retain their own controlling licences, notices, and attribution; and
- OpenStreetMap data remains © OpenStreetMap contributors under ODbL 1.0.

The Issue #13 About card no longer contains the placeholder Thanks/credits section. Onboarding and License & usage now present GPL application code, K2040 artwork under CC BY 4.0, and OpenStreetMap/ODbL as distinct maintained scopes. The GPL detail explains the narrow K2040 section 7(b) boundary instead of applying it to upstream material.

Fresh JDK 17 source/build validation and focused physical-device QA completed successfully for the Issue #13 runtime source through `419ebbca90fe6bc5ec2c9a2dbb7bdc8cfdeff993`. The validated debug artifact was installed through the verified signing boundary; fresh English and German onboarding, License & usage and nested licence details, About, Sources, and Changelog were accepted without clipping at the designated QA scale. Continue remained the only onboarding acknowledgement path, acknowledgement persisted, and a synthetic plain-coordinate `ACTION_SEND` remained deferred until Continue and was processed only afterward. The app language was restored to System default through GeoJoystick's own settings UI, simulation remained inactive, and the QA flow did not change Android's mock-location selection.

Issue #13 implementation and device QA are accepted. Draft PR #14 carries Issue #13 together with the accepted development reconciliation and remains unmerged pending Issue #15 visual acceptance.

## Issue #16 restore-last-position follow-up

Issue #16 was found during Issue #9 physical-device QA: detached home-page coordinate fields could overwrite a restored draft while Settings was active. PR #17 added a focused guard so `saveVisibleCoordinates()` writes only while the home page owns the live coordinate editors.

Local validation passed with JDK 17, Android SDK Platform 35 and compatible Build-Tools 36.0.0, including debug build, lint, unit-test task behavior, and unsigned release assembly. Signer-safe in-place device installation preserved app data. Physical-device regression confirmed that normal home-to-Settings manual-draft persistence still works, enabling Restore last position replaces the draft with the previously successful last-active coordinates, `last_*` remains unchanged, and no simulation/service starts as a side effect. PR #17 is merged into `feat/onboarding-about-card-13` and Issue #16 is closed as completed.

## Current Issue #15 visual QA

Issue #15 tracks the remaining overlay visual regression: control/speed glyphs are oversized relative to their 48dp interaction targets. The intended fix keeps the existing touch targets while reducing rendered glyph size and preserving selected/inactive states, opacity, high-contrast behavior, overlay scaling, accessibility labels, compact/expanded behavior, and joystick-direction legibility. Physical-device visual acceptance is required before draft PR #14 can be considered ready to merge.

Release signing, reproducibility, tags, releases, F-Droid/store publication, deployments, announcements, and other publication actions remain separate approval gates and are not authorized by development acceptance.
