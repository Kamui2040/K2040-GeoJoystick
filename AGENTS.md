# Repository Rules

## Project scope

GeoJoystick is an open-source Android mock-location utility for emulator and developer testing. Keep the project suitable for public collaboration and independent local builds.

## Product and safety constraints

- Keep the app ad-free, account-free, analytics-free, tracking-free, subscription-free, and free of mandatory proprietary services.
- Preserve Android's standard mock-location provider flow and manual Developer Options selection.
- Do not add concealment, integrity or attestation bypasses, anti-detection mechanisms, game/app-specific bypasses, account automation, ban evasion, root/Shizuku requirements, or other non-standard location injection.
- Treat external links and imported data as untrusted. Validate supported formats, coordinate ranges, schemes, redirects, bounds, and failure paths.
- Never substitute a real-world fallback coordinate when parsing or validation fails.
- Preserve OpenStreetMap attribution and keep network-dependent behavior optional and disclosed.

## Data and privacy

- Keep saved coordinates, favorites, settings, and other user state local unless a user explicitly invokes a documented export/import or network action.
- Never commit credentials, signing material, personal data, real location histories, device identifiers, machine-specific paths, or maintainer-only operational information.
- Use synthetic or deliberately sanitized fixtures and examples.

## Build and validation

- JDK 17 and Android SDK Platform 36 are the current build baseline.
- Use the repository build/bootstrap tooling; do not require a globally installed Gradle.
- Keep Linux, macOS, and Windows build instructions contributor-facing and machine-independent.
- Before a change is considered complete, inspect the changed-file scope and run applicable tests, lint, build checks, and `git diff --check`.
- Keep source, build, runtime/device, signing, reproducibility, and publication evidence distinct.
- If an automated Android runtime-state detector disagrees with verified known-active manual behavior, treat the detector as invalid evidence. Stop detector retries, remove superseded detector logic, and report the affected automated gate as requiring manual acceptance unless a reliable app-owned debug interface exists.
- In RTL layouts, do not manually invert text glyphs that Android mirrors automatically. Validate directional controls and isolate mixed-direction numeric and legal tokens on a real RTL surface before acceptance.
- Do not claim installation, physical-device behavior, signing, reproducibility, or release readiness without corresponding evidence.

## Contributions

- Keep `main` stable and prefer focused, reviewable changes.
- When a script intentionally switches the current worktree to another branch, capture the validation baseline after that switch or compare stable worktree topology. Do not require a pre-switch `git worktree list --porcelain` snapshot to remain byte-identical after the intended branch change.
- Preserve GPL-3.0-only licensing and required third-party attribution and provenance.
- Do not commit generated APK/AAB files, local build caches, signing material, or machine-specific configuration.
- Normal public issues and pull requests may document bugs, implementation work, compatibility changes, localization targets, and other contributor-relevant technical plans.
- Keep public documentation focused on information useful to users and contributors.
