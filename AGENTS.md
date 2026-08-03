# Repository Instructions

## Authority and scope

These instructions apply to the public `Kamui2040/K2040-GeoJoystick` repository.

Host and safety requirements and the current user request override this file. `PROJECT_CONTEXT.md` owns mutable project state such as versions, commit references, pull requests, release/F-Droid status, toolchain evidence, defects, validation, and remaining gates. Live source, metadata, tests, store, and device evidence override stale documentation.

- Product: **GeoJoystick**
- Package: `com.k2040.geojoystick`
- Default branch: `main`
- Licence: GPL-3.0-only

Keep private planning, QA, diagnostics, backups, and credentials outside public Git. Private storage must never be a build, installation, or runtime dependency.

## Authorized maintenance

Safe reversible work is authorized on non-default branches: inspect, fetch, edit, stage, commit, rebase or merge non-default branches, maintain documentation and metadata, preserve evidence, perform bounded cleanup, and prepare draft pull requests or issues.

Preserve unrelated work and keep `main` stable. Explicit user involvement remains required for unavailable workstation or physical-device execution, local build/test/lint handoffs, secrets or signing, installation, irreversible actions, public default-branch merge or publication, store submission, public tags/releases/announcements, and final signoff.

## Product and safety contract

GeoJoystick is a transparent Android mock-location joystick for emulator and developer testing through Android Developer Options and Android's standard mock-location mechanism.

Preserve manual coordinates and altitude, map and map-link selection, floating joystick modes, speed presets, hold/pause/hide/stop controls, favorites, optional restoration, overlay settings, appearance, English/German localization, and truthful foreground-service behavior.

Never add concealment, anti-detection, integrity or attestation bypasses, app-specific bypasses, nonstandard injection or spoofing, account automation, ban evasion, root, Shizuku, accessibility abuse, anti-forensics, or misleading simulation state.

Do not add advertisements, analytics, tracking, telemetry, billing, subscriptions, paid entitlements, accounts, proprietary mandatory cloud services, or a mandatory network dependency.

## State and data integrity

Keep selected, saved/recent, and active coordinates; provider readiness; successful publication; service; notification; overlay; and UI state distinct. Selection or a start request does not prove active simulation.

Active state requires ready providers and a successful valid publish. Failure clears active state and reports an error. Reconcile app-op loss, Developer Options changes, provider removal, process death, and reboot. Never silently restart at stale or default coordinates.

Validate coordinates, altitude, locale, parsing, identity, and persistence. Reject malformed, non-finite, out-of-range, ambiguous, or unsupported input. Do not substitute a real-world default after failure.

Version future import/export, migration, and restore formats. Validate in isolated staging before atomic replacement; failure preserves valid locations, settings, and runtime state.

## Android, permissions, and hostile input

Use least privilege. The user selects GeoJoystick in Developer Options; never bypass or automate that flow. Do not add real/background location, storage, accessibility, VPN, root, Shizuku, or device-admin access without an approved need and review.

Foreground service and notification state must truthfully represent active work and stop reliably. Reassess app-op, foreground-service, notification, overlay, exported-component, share/intent, URI, and target-SDK behavior when affected.

Treat external text, coordinates, links, redirects, files, archives, intents, QR data, WebView messages, responses, images, and databases as hostile. Bound sizes, redirects, paths, nesting, and timeouts; allowlist formats, schemes, hosts, and destinations; block traversal, overwrite, executable content, private-network access where applicable, leakage, and exhaustion.

The bundled map must use no remote script or secret key. Preserve OpenStreetMap attribution and usage-policy compliance. Keep network use explicit, optional, disclosed, bounded, and replaceable.

## Privacy, FLOSS, and rights

Coordinates, history, settings, device identifiers, and QA evidence remain local/private unless sanitized. Use synthetic locations and redact logs and screenshots.

Public Git must remain sanitized, independently buildable, FLOSS/F-Droid suitable, and complete for source builds: source, build inputs, tests, licences/notices, translations, safe assets/fixtures, and store metadata.

Prefer maintained FLOSS dependencies and open formats. Review provenance, licences, transitive dependencies, native binaries, network behavior, and reproducibility before adoption.

Preserve attribution to `ZCShou/GoGoGo`, baseline commit `de0d596190c57b8ca71481f60ce6b9e50af5107f`, under GPL-3.0-only. Do not reintroduce Baidu SDK/native binaries, embedded signing material, legacy updater/logging/history/tracking components, or unlicensed/proprietary assets.

## Workflow, validation, and cleanup

Read the project and shared error/failure dictionaries before scripts, repository mutation, or handoff. Update them with confirmed failures and prevention rules without duplicating existing entries.

Never create, trigger, monitor, query, require, or depend on GitHub Actions or other cloud CI for the PC workflow. Inspect tracked workflow triggers before pushes or publication-sensitive work.

Use repository-owned tooling and deterministic PowerShell/Python. User-facing PowerShell begins with `Clear-Host` and a descriptive `$Host.UI.RawUI.WindowTitle`. Never terminate the user's interactive shell.

Validate exact paths, identity, encoding, permissions, lifecycle, hostile input, privacy, licences, dependencies, and changed files. Run `git diff --check`. Keep large diagnostics private. Separate source, build, device, signing, store, and release evidence, and never claim a check that was not performed.

Before ADB work, enumerate devices and target the intended serial explicitly. Use synthetic coordinates and sanitized evidence. Installation and final physical-device signoff remain separate.

Clean continuously but only after preserving accepted evidence. Remove only verified obsolete project-scoped items. Preserve active work, unresolved evidence, user data, backups, releases, secrets, unrelated changes, and Git history.
