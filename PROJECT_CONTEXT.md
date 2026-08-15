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

## Accepted development integration on main

The accepted reconciliation, Issue #13 onboarding/About/licensing work, Issue #16 restore-last-position follow-up, and Issues #15/#20/#21 UI redesign are integrated into `main` through squash-merged PR #14.

- PR #14 merge commit: `2864c7711b256214493362c03915a60c53fa589e`
- validated PR #14 head before squash: `adb804e14543654ad5d638dd89e2b80dc0df300c`
- validated base `main`: `0dcd335fd4ebb6810a0dc2ea82dc7854ed3ed4b4`
- PR #23 UI redesign squash on the integration branch: `454624a65562fe8c52dd482426144ea71135b0c4`
- final physically accepted pre-integration runtime candidate: `9c83e7bd8e35362d65c51db8310550a06923def5`
- accepted/final-validation debug APK SHA-256: `01966f8d426452750acec9b9819859a72218442899be1553f711d9c0d707f455`

Issues #9, #13, #15, #16, #20, and #21 are closed as completed.

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

No additional Android build is required for this documentation-only post-merge context update.

## Current mascot work

Issue #22 separately tracks a dedicated GeoJoystick mascot: a gecko interacting with an arcade-style joystick, optionally incorporating the existing GeoJoystick symbol. The intended style follows the soft, rounded, hand-drawn/pastel K2040 mascot family. Artwork must be project-owned/original or otherwise explicitly redistributable, provenance/licensing must remain auditable, and final visual approval is required before integration. Issue #22 does not itself authorize replacing the app icon.

## Publication boundary

The accepted development integration is now on `main`, but it is not a production release action. Production signing, reproducibility claims, tags/releases, F-Droid/store publication, deployments, announcements, repository-visibility changes, and comparable public actions remain separate explicit approval gates.
