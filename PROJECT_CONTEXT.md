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

The focused development line is `integration/feedback5-accepted-reconciliation-20260810`. It is reconstructed from current `main` plus the accepted runtime/UI source rather than by merging the migration branch wholesale. This keeps the newer public-safe `main` governance and clean-Linux bootstrap fix, imports the accepted runtime/state/input/UI changes, removes the legacy PC GitHub Actions workflow, and removes the obsolete unsanitized phone screenshots from the maintained tree.

Validation performed locally before the reconciliation commit:

- `git diff --check`: PASS
- dependency-free `LocationLinkParser` self-test: PASS
- repository bootstrap/debug build with JDK 17: PASS
- `:app:lintRelease`: PASS
- unsigned release assembly: PASS
- `:app:testDebugUnitTest`: NO-SOURCE; accepted because no Java/Kotlin unit-test sources exist
- debug APK identity: PASS for `com.k2040.geojoystick` 0.1.3 (103)

The rebuilt debug APK is a new artifact boundary and is retained only for the next physical-device QA gate. Historical Feedback 5 APK hashes do not validate this rebuild. Signing, reproducibility, release/F-Droid/store validation, tags, releases, and publication remain separate future gates.

Remaining development gates include physical-device lifecycle/state reconciliation, truthful service/notification/overlay/UI state under permission/provider/process transitions, remaining accessibility/device-scale review, and later API 36 compile/target migration review where applicable.

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

Issue #13 implementation and device QA are accepted. The remaining boundary is repository review/merge of the focused development line. Release signing, reproducibility, tags, releases, F-Droid/store publication, and other publication actions remain separate gates and are not authorized by this development acceptance.
