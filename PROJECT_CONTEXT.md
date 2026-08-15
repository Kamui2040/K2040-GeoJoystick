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

GeoJoystick remains ad-free, account-free, analytics-free, tracking-free, subscription-free, and free of mandatory proprietary services. Saved coordinates, favorites, and settings remain local unless the user explicitly performs a documented external action. The map uses OpenStreetMap only when opened, with required attribution preserved.

## Upstream relationship

The project reuses and simplifies architecture and movement concepts from `ZCShou/GoGoGo`, baseline commit `de0d596190c57b8ca71481f60ce6b9e50af5107f`, under GPL-3.0-only. The Baidu SDK, embedded signing configuration, updater, logging stack, history database, and legacy permissions are intentionally not carried over.

Public descriptions should present GeoJoystick as a mock-location utility for emulator and developer testing, not as game tooling, cheating software, anti-detection tooling, or a bypass utility.

## Licensing and provenance

- Application code remains `GPL-3.0-only`.
- K2040-authored GPL code carries the separate GPLv3 section 7(b) attribution-preservation term only where the source file explicitly marks that term as applicable.
- Original K2040 artwork and UI artwork with established project provenance is `CC-BY-4.0`, including the bundled K2040 avatar.
- GoGoGo-derived and other third-party code/assets/data retain their own controlling licences, notices, and attribution.
- OpenStreetMap data remains © OpenStreetMap contributors under ODbL 1.0.
- F-Droid metadata includes `AntiFeatures: TetheredNet` because the optional map picker uses OpenStreetMap services.

## Accepted development integration

The maintained development line is `feat/onboarding-about-card-13`, tracked by draft PR #14 against `main`.

The branch contains the accepted reconciliation reconstructed from current public-safe `main` plus the maintained runtime/UI/state/input work. It retains the clean Linux bootstrap fix and current repository governance, removes the obsolete PC GitHub Actions workflow and obsolete unsanitized phone screenshots, and includes the maintained parser regression harness.

### Accepted lifecycle/state work

Issue #9 physical-device lifecycle/state reconciliation is accepted and closed. Accepted coverage includes fresh/unset and invalid-input behavior, successful-publication gating and last-active persistence, mock-location app-op/manual deselection reconciliation, provider/publication failure cleanup, process-death and reboot stale-state behavior, restore-last-position behavior, all three user-visible stop paths, and a final inactive state. Android mock-location-app selection remained manual and user-controlled throughout.

Issue #16 fixed the restore-last-position detached-coordinate overwrite found during Issue #9 QA. PR #17 is merged into `feat/onboarding-about-card-13`; local validation and physical-device regression passed, including normal home-to-Settings draft persistence and restore-last-position behavior without unintended service startup.

### Accepted Issue #13 onboarding/About/licensing work

Issue #13 implementation and physical-device QA are accepted. The maintained onboarding uses Continue as the only acknowledgement path and defers incoming shared intents until acknowledgement. About, Changelog, License & usage, Sources, GPL, CC BY 4.0 artwork, and OpenStreetMap/ODbL information are presented as maintained in-app surfaces with the authorship/provenance boundaries above.

Fresh JDK 17 source/build validation and focused English/German physical-device QA passed for the accepted Issue #13 runtime. Incoming synthetic plain-coordinate `ACTION_SEND` was confirmed deferred until Continue and processed only afterward. The app language was restored to System default, simulation remained inactive, and Android's mock-location selection was unchanged.

### Accepted Issues #15, #20, and #21 UI redesign

The first Issue #15 sizing attempt in PR #19 was rejected and remains closed/unmerged. The final redesign was implemented in PR #23 and physically accepted on 2026-08-15.

Accepted final UI behavior:

- Issue #15: overlay glyphs use the accepted 10/11/12dp visual sizes while existing 48dp interaction targets remain; expanded speed/action frames are visually inset; compact `+`/`−` remain small and borderless; direction guides are lighter; compact mode removes the outer panel/frame entirely so only the joystick and `+` remain visible; expanding restores the full panel.
- Issue #20: Status is collapsed by default with reduced vertical padding; the Status label and chevron remain vertically centered in a fixed header position while details expand below; `Simulation`, Start, and Stop share one centered horizontal row with preserved interaction targets and existing provider/lifecycle methods.
- Issue #21: Settings secondary descriptions are removed; Mock location, Overlay permission, Restore last position, and Overlay high contrast show explicit state text with green/red state styling while remaining understandable without color alone; Settings refreshes after returning from Android settings.

Final accepted runtime candidate before integration:

- commit: `9c83e7bd8e35362d65c51db8310550a06923def5`
- debug APK SHA-256: `01966f8d426452750acec9b9819859a72218442899be1553f711d9c0d707f455`
- JDK 17 / Android SDK Platform 35 / Build Tools 36.0.0 debug build: PASS
- `git diff --check`: PASS
- static UI invariants: PASS
- `:app:testDebugUnitTest`: NO-SOURCE at this boundary
- `:app:lintRelease`: PASS
- `:app:assembleRelease`: PASS
- signer-safe installation preserved app data and the user-controlled Android mock-location selection
- physical-device visual QA: PASS
- GitHub Actions were not queried or used

PR #23 was squash-merged into `feat/onboarding-about-card-13` as `454624a65562fe8c52dd482426144ea71135b0c4`. Issues #15, #20, and #21 are closed as completed.

## Current PR #14 gate

PR #14 remains open, draft, and unmerged. The former #15/#20/#21 blockers are cleared.

The remaining development gate is a fresh local validation of the complete integrated PR #14 head after the PR #23 squash merge and this documentation update. That validation must treat the whole current integration tree as the unit under test rather than relying only on the individually validated component revisions.

Do not merge PR #14 into `main` until that final integration validation passes. GitHub Actions must not be queried or used for this PC/Linux validation unless explicitly approved.

## Current mascot work

Issue #22 separately tracks a dedicated GeoJoystick mascot: a gecko interacting with an arcade-style joystick, optionally incorporating the existing GeoJoystick symbol. The intended style follows the soft, rounded, hand-drawn/pastel K2040 mascot family. Artwork must be project-owned/original or otherwise explicitly redistributable, provenance/licensing must remain auditable, and final visual approval is required before integration. Issue #22 does not itself authorize replacing the app icon and is not a blocker for PR #14 unless explicitly coupled later.

## Publication boundary

Production signing, reproducibility claims, tags/releases, F-Droid/store publication, deployments, announcements, repository-visibility changes, and comparable public actions remain separate approval gates and are not authorized by development acceptance.