# Project Context

## Purpose

GeoJoystick is an open-source Android mock-location joystick intended for emulator and developer testing. It provides a floating joystick, speed presets, manual coordinates, an internal map picker, map-link coordinate import, and a persistent foreground service through Android's standard mock-location provider flow.

## Current public release

- Version: 0.1.3
- Version code: 103
- Canonical repository: https://github.com/Kamui2040/K2040-GeoJoystick
- F-Droid submission MR !42238 is merged into `fdroid/fdroiddata` master; the store may still show v0.1.2 until the v0.1.3 metadata/build update is processed.
- Official APKPure listing is live at https://apkpure.com/p/com.k2040.geojoystick.
- GitHub Releases remains the canonical source for release notes and published release assets.
- No APKPure installation smoke test is recorded in this repository yet.
- Current `main` now builds with compileSdk/targetSdk 36 after Issue #11; this development baseline does not itself publish or replace any existing release artifact.

GeoJoystick remains ad-free, account-free, analytics-free, tracking-free, subscription-free, and free of mandatory proprietary services. Saved coordinates, favorites, and settings remain local unless the user explicitly performs a documented external action. The map uses OpenStreetMap only when opened, with required attribution preserved.

## Upstream relationship

The project reuses and simplifies architecture and movement concepts from `ZCShou/GoGoGo`, baseline commit `de0d596190c57b8ca71481f60ce6b9e50af5107f`, under GPL-3.0-only. The Baidu SDK, embedded signing configuration, updater, logging stack, history database, and legacy permissions are intentionally not carried over.

Public descriptions should present GeoJoystick as a mock-location utility for emulator and developer testing, not as game tooling, cheating software, anti-detection tooling, or a bypass utility.

## Licensing and provenance

- Application code remains `GPL-3.0-only`.
- K2040-authored GPL code carries the separate GPLv3 section 7(b) attribution-preservation term only where the source file explicitly marks that term as applicable.
- Original K2040 artwork and UI artwork with established project provenance is `CC-BY-4.0`, including the bundled K2040 avatar, the GeoJoystick waving gecko mascot, and the simplified full-color launcher emblem.
- GoGoGo-derived and other third-party code/assets/data retain their own controlling licences, notices, and attribution.
- OpenStreetMap data remains © OpenStreetMap contributors under ODbL 1.0.
- F-Droid metadata includes `AntiFeatures: TetheredNet` because the optional map picker uses OpenStreetMap services.

## Accepted development integration on main

The accepted reconciliation, Issue #13 onboarding/About/licensing work, Issue #16 restore-last-position follow-up, and Issues #15/#20/#21 UI redesign are integrated into `main` through squash-merged PR #14.

- PR #14 merge commit: `2864c7711b256214493362c03915a60c53fa589e`
- validated PR #14 head before squash: `adb804e14543654ad5d638dd89e2b80dc0df300c`
- validated base `main`: `0dcd335fd4ebb6810a0dc2ea82dc7854ed3ed4b4`
- PR #23 UI redesign squash on the integration branch: `454624a65562fe8c52dd482426144ea71135b0c4`
- final physically accepted pre-integration runtime candidate: `9c83e7bd8e35362d65c51db8310550a06923def5`
- accepted/final-validation debug APK SHA-256: `01966f8d426452750acec9b9819859a72218442899be1553f711d9c0d707f455`

Issues #9, #10, #11, #13, #15, #16, #20, #21, and #22 are closed as completed.

### Lifecycle/state behavior

Issue #9 physical-device lifecycle/state reconciliation is accepted. Accepted coverage includes fresh/unset and invalid-input behavior, successful-publication gating and last-active persistence, mock-location app-op/manual deselection reconciliation, provider/publication failure cleanup, process-death and reboot stale-state behavior, restore-last-position behavior, all three user-visible stop paths, and a final inactive state. Android mock-location-app selection remained manual and user-controlled throughout.

Issue #16 prevents detached Settings-page coordinate fields from overwriting a restored draft. Local validation and physical-device regression confirmed normal home-to-Settings draft persistence and restore-last-position behavior without unintended service startup.

### Onboarding, About, licensing, and provenance presentation

Issue #13 is accepted and integrated. Continue remains the only onboarding acknowledgement path, incoming shared intents remain deferred until acknowledgement, and About/Changelog/License & usage/Sources/GPL/CC BY 4.0 artwork/OpenStreetMap-ODbL information remain maintained in-app surfaces with the authorship/provenance boundaries above.

Focused English/German physical-device QA passed. A synthetic plain-coordinate `ACTION_SEND` remained deferred until Continue and was processed only afterward. Simulation remained inactive and Android's mock-location selection was unchanged during that accepted QA.

### Accepted UI redesign

- Issue #15: overlay glyphs use the accepted 10/11/12dp visual sizes while existing 48dp interaction targets remain; expanded speed/action frames are visually inset; compact `+`/`−` remain small and borderless; direction guides are lighter; compact mode removes the outer panel/frame entirely so only the joystick and `+` remain visible; expanding restores the full panel.
- Issue #20: Status is collapsed by default with reduced vertical padding; the Status label and chevron remain vertically centered in a fixed header position while details expand below; `Simulation`, Start, and Stop share one centered horizontal row with preserved interaction targets and existing provider/lifecycle methods.
- Issue #21: Settings secondary descriptions are removed; Mock location, Overlay permission, Restore last position, and Overlay high contrast show explicit state text with green/red state styling while remaining understandable without color alone; Settings refreshes after returning from Android settings.

Physical-device visual QA accepted the complete redesign on 2026-08-15. Signer-safe installation preserved app data and the user-controlled Android mock-location selection.

### Accessibility and device-scale behavior

Issue #10 is accepted and closed. Reusable deterministic device-scale/accessibility automation was added through squash-merged PR #24.

- PR #24 QA-tooling merge commit: `baf1eb79c4f60be5858850d6d6deb54931f426fb`
- PR #25 accessibility runtime-fix merge commit: `167937578c1d3410bb67a74d20feb69d168b9f14`
- validated PR #25 head before squash: `6afeef4504dddca68111c85827f56ddf9d5b07ec`
- accepted PR #25 debug APK SHA-256: `88260a65f4d5488c2845a280ebf094efb3c471d7d4c4230616137a4717523038`

The maintained harness covers an 18-scenario matrix across English/German, System/Light/Dark appearance, representative font scales through 2.0x, and baseline/stress display density. It preserves and restores app preferences, font scale, density, and inactive simulation state; uses synthetic coordinates for the overlay phase; and keeps private QA device identity out of Git.

The complete refined run separated UIAutomator viewport/hierarchy artifacts from two genuine defects. PR #25 enlarged the clickable Home About/avatar target to 48dp while preserving its approximately 40dp visual footprint, and made the About body vertically scrollable while retaining the fixed identity/close row. Focused device QA confirmed a 48.0x48.0dp About target and English/German About navigation reachability at 2.0x font scale. Signer-safe replacement preserved app data, mock-location selection, overlay permission, and inactive simulation state. Human visual QA accepted normal-scale presentation, About scrolling/fixed close behavior, Theme/Language dialogs, and large-font About usability.

### Mascot and launcher identity

Issue #22 is accepted and integrated through squash-merged PR #26.

- PR #26 mascot/icon merge commit: `cfb5d7162d75c60573dcf24cc7b3730a1b887868`
- validated PR #26 head before squash: `61894232c7831dbf9f81bc71fac680fd85067a23`
- accepted PR #26 debug APK SHA-256: `470a67059c1723f5a0e9654cce83ad3687cd7fc2a5a502a4b111b469d99e44cc`

The home-header About entry now uses the user-approved waving gecko interacting with an arcade joystick. Tapping it continues to open the existing About card, which retains the K2040 avatar; Welcome also retains the K2040 avatar. The full-color launcher/app-drawer artwork uses the separately reviewed and approved simplified gecko + joystick map-pin emblem. The existing adaptive-icon wiring and themed monochrome artwork remain unchanged.

The accepted launcher artwork is stored as a lossless WebP resource after the original indexed PNG exposed an AAPT2 release-resource compilation incompatibility. The replacement preserves the approved 384×384 artwork pixel-for-pixel while using an Android-compatible resource container. `NOTICE.md` records both accepted GeoJoystick assets under the existing K2040 CC BY 4.0 provenance framework.

Local and physical validation on the exact PR #26 head passed `git diff --check`, the `LocationLinkParser` regression harness, `tools/build.py`, `testDebugUnitTest`, `lintRelease`, `assembleRelease` including release AAPT2 resource processing, and signer-safe replacement installation. Installation preserved app data, the user-controlled mock-location app-op state, overlay permission, and inactive simulation state. Human visual QA accepted the in-app mascot, About tap-through/K2040 avatar, and centered/unclipped app-drawer symbol. The primary local checkout remained untouched and temporary QA artifacts were cleaned.

### Android 16 / API 36 development baseline

Issue #11 is accepted and integrated through squash-merged PR #28.

- PR #28 API 36 merge commit: `ded38f66d6fd04f9a1c390c974f3d696f0eb73df`
- validated PR #28 head before squash: `47d776484b65becb45a4c1b3b5fecf1cf9747614`
- accepted PR #28 debug APK SHA-256: `942a2dd644ed196850d0b194e9916ab66b0a1cc0abd3a100a4ad6fc3e6385136`

The maintained development build baseline is now JDK 17, Android SDK Platform 36, `compileSdk 36`, `targetSdk 36`, `minSdk 27`, AGP 8.12.3, and Gradle 8.13. Build Tools 35.0.0 or a newer compatible stable version remain accepted; the validated migration used Build Tools 36.0.0. `tools/build.py` now checks/installs Platform 36 rather than Platform 35.

The Android 16 review found no required change to GeoJoystick's standard Android mock-location test-provider flow, `specialUse` foreground-service declaration, notification permission handling, overlay permission model, backup/data-extraction exclusions, exported-component boundaries, or optional OpenStreetMap/link-resolution network behavior. The migration added no proprietary dependency, API key, root/Shizuku requirement, concealment mechanism, or non-standard location injection.

MainActivity currently uses Android's activity-scoped temporary `android:enableOnBackInvokedCallback="false"` compatibility setting because its custom nested About/license/settings navigation still relies on legacy `onBackPressed()`. `lint.xml` ignores only `GestureBackNavigation` for `MainActivity.java`, because that lint check does not inspect the activity-scoped manifest opt-out. Issue #27 remains open to replace this temporary compatibility path with supported predictive-back handling. `MapActivity` remains on normal Android back behavior.

Final exact-head API 36 validation passed `git diff --check`, the `LocationLinkParser` regression harness, Platform 36 debug build, `testDebugUnitTest`, `lintRelease`, and unsigned `assembleRelease`. Signer-safe replacement installation preserved app data, mock-location app-op state, and overlay permission. Physical Android 16/API 36 regression confirmed Settings -> Home, About -> Home, and nested License -> About -> Home Back behavior; synthetic-coordinate foreground simulation start/publication and a user-visible stop path; invalid-input start remaining inactive; byte-for-byte preference restoration; and a final inactive simulation state. The primary local checkout remained untouched and temporary QA artifacts were cleaned.

## Final integration validation

The complete PR #14 tree was validated locally before merge as one unit rather than relying only on component validation:

- exact PR head/base pair verified before validation
- full PR `git diff --check`: PASS
- accepted PR #23 UI tree unchanged by squash/integration: PASS
- obsolete GitHub Actions workflow absent from the maintained tree
- dependency-free `LocationLinkParser` regression: PASS
- JDK 17 / Android SDK Platform 35 / Build Tools 36.0.0 debug build: PASS
- package identity `com.k2040.geojoystick` 0.1.3 (103): PASS
- `:app:testDebugUnitTest`: NO-SOURCE at this boundary
- `:app:lintRelease`: PASS
- `:app:assembleRelease`: PASS
- tracked source and primary checkout preserved
- temporary validation worktrees cleaned
- device not modified by the final integration build pass
- no GitHub Actions jobs were dispatched or used as integration-validation evidence

Issue #10's later runtime fix was separately validated on its exact source head with `git diff --check`, parser regression, JDK 17 / SDK 35 / Build Tools 36.0.0 debug build, `testDebugUnitTest`, `lintRelease`, `assembleRelease`, signer-safe physical installation, focused structural QA, state restoration, and human visual acceptance. GitHub Actions were not used as validation evidence for that workflow.

Issue #22's mascot/icon integration was separately validated on its exact source head with provider-side scope checks, `git diff --check`, parser regression, JDK 17 / SDK 35 / Build Tools 36.0.0 debug build, `testDebugUnitTest`, `lintRelease`, `assembleRelease`, successful release AAPT2 processing of the lossless launcher resource, signer-safe physical installation, state preservation, and human visual acceptance. GitHub Actions were not used as validation evidence for that workflow.

Issue #11's API 36 migration was separately validated on its exact source head with Platform 36, Build Tools 36.0.0, parser regression, debug/release builds, lint, signer-safe installation, target-36 Back compatibility, synthetic start/publication/stop, invalid-input reconciliation, and complete state restoration. No GitHub Actions jobs were dispatched or used as validation evidence.

No additional Android build is required for this documentation-only post-merge context update.

## Publication boundary

The accepted development integration is now on `main`, but it is not a production release action. Production signing, reproducibility claims, tags/releases, F-Droid/store publication, deployments, announcements, repository-visibility changes, and comparable public actions remain separate explicit approval gates.
