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
- Never commit credentials, signing material, personal data, real location histories, device identifiers, private QA evidence, machine-specific paths, or other maintainer-only operational information.
- Use synthetic or deliberately sanitized fixtures and examples.

## Build and validation

- JDK 17 and Android SDK Platform 36 are the current build baseline.
- Use the repository build/bootstrap tooling; do not require a globally installed Gradle.
- Keep Linux, macOS, and Windows build instructions contributor-facing and machine-independent.
- Before a change is considered complete, inspect the changed-file scope and run applicable tests/lint/build checks plus `git diff --check`.
- Do not claim installation, physical-device behavior, signing, reproducibility, or release readiness without corresponding evidence.

## Contributions and releases

- Keep `main` stable and prefer focused, reviewable changes.
- Preserve GPL-3.0-only licensing and required third-party attribution/provenance.
- Do not commit generated APK/AAB files, local build caches, private signing material, or machine-specific configuration.
- Starting with v0.1.5, use an F-Droid-first release gate: before any public release tag, production APK, or downstream store publication, freeze the exact release candidate and prove the maintained F-Droid developer-binary path can reproduce the unsigned APK and successfully complete signature-copy verification against the intended developer-signed artifact.
- Keep the established permanent GeoJoystick production signing identity for future releases. Choose signing schemes explicitly from the app's supported Android range and release requirements; do not rely on `apksigner` defaults.
- If the F-Droid-first gate fails, correct the release candidate or build configuration before publication rather than introducing a post-publication workaround.
- Releases, store submissions, signing decisions, and other official publication actions are maintainer-controlled.
