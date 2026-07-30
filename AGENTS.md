# Repository Instructions

## Authority and identity

These instructions apply to the entire public `Kamui2040/K2040-GeoJoystick` repository.

Authority: host/safety and the current request, then active GeoJoystick Project Instructions, this file, `PROJECT_CONTEXT.md`, routed task documents, and live source/build/store/device evidence. This file is self-contained for tools that cannot see ChatGPT settings. Repository evidence overrides memory.

- Product: **GeoJoystick**
- Package: `com.k2040.geojoystick`
- Default branch: `main`
- Licence: GPL-3.0-only
- Canonical Windows checkout: `D:\Projects\Android\K2040-GeoJoystick\repo`, with `.git` directly inside `repo`
- Private workspace: Google Drive `Projects/Android/K2040-GeoJoystick`; it is never a build or runtime dependency

`PROJECT_CONTEXT.md` is the sole compact owner of mutable versions, releases, issues/PRs, store status, toolchains, defects and validation evidence. Verify live files before relying on snapshots.

## Autonomous maintenance

Safe reversible maintenance is authorized through available tools: fetch/pull, focused branches/worktrees, repository edits, staging, commits, pushes, non-default-branch rebases/merges, draft pull requests/issues, documentation, metadata, approved assets, evidence and bounded cleanup. Preserve unrelated changes and keep `main` stable.

User involvement is required for unavailable MAIN_PC/device execution, local build/test/lint, signing, installation or physical-device ADB, subjective QA, secret/identity entry, irreversible action, public default-branch merge/publication, store submission, public tag/release/announcement and final signoff. Local execution is an execution boundary, not a permission request.

## Product and safety contract

GeoJoystick is a transparent Android mock-location utility for emulator and developer testing through Android Developer Options and standard test-provider APIs.

Preserve manual coordinates/altitude, map and map-link selection, floating joystick modes, speed presets, hold/pause/hide/stop, favorites, optional restoration, overlay settings, appearance, English/German localization and truthful foreground-service behavior.

Never add concealment, anti-detection, integrity/attestation bypass, game-specific bypasses or automation, account automation, ban evasion, injection/hooking, root, Shizuku, accessibility abuse, certificate spoofing, anti-forensics or abnormal persistence. Do not misrepresent simulated location status.

No advertisements, billing, subscriptions, paid entitlements, accounts, analytics, tracking, telemetry, crash-reporting service, proprietary updater or mandatory cloud/network service.

## State and data integrity

Keep selected, saved/recent and active coordinates; provider readiness; successful publish; service; notification; overlay; and UI state distinct. Selection or a start request does not prove active simulation. Active state requires ready providers and successful valid publication; failures clear active state and report truthfully.

Reconcile app-op loss, Developer Options changes, process death, reboot and provider removal. Never silently restart at stale or default coordinates. Stop removes test providers, timers, movement, overlay and notification without erasing saved user settings.

Validate all numeric input for finiteness and supported ranges. Never substitute a real-world default after parsing failure. Future import/export/migration formats require versioning, stable identity, isolated staging, complete validation and atomic promotion; failure preserves existing valid state.

## Android, permissions and hostile input

Use least privilege. The user selects GeoJoystick as the mock-location app; never bypass that flow. Reassess app-op, foreground-service, notification, overlay, exported-component and target-SDK behavior when affected.

Treat shared text, clipboard data, intents, links, redirects, WebView messages, files, archives, images and databases as hostile. Bound sizes and redirects; allowlist schemes, hosts, destinations and formats; block private-network access where applicable, traversal, overwrite, executable content, resource exhaustion and data leakage.

Keep the bundled map implementation free of remote scripts and secret API keys. Preserve OpenStreetMap attribution, usage-policy compliance and F-Droid `TetheredNet` disclosure. Do not bulk-download, aggressively prefetch or abusively cache tiles. Keep the JavaScript bridge minimal and validate every coordinate crossing it.

## Privacy, FLOSS and rights

Coordinates, favorites, settings, device IDs and QA evidence stay local/private unless sanitized. Use synthetic locations and redact logs/screenshots. `PRIVACY.md` owns the public disclosure.

Public Git must remain sanitized, independently buildable and complete: source, build inputs, tests, licence/notice, translations, safe assets/fixtures and store metadata. Prefer direct Java/Android APIs and maintained FLOSS dependencies. Review transitive licences, native binaries, network behavior, provenance and reproducibility before adding dependencies.

Preserve attribution to `ZCShou/GoGoGo`, baseline commit `de0d596190c57b8ca71481f60ce6b9e50af5107f`, under GPL-3.0-only. Do not reintroduce Baidu SDK/native binaries, embedded signing, legacy updater/logging/history/tracking components or unlicensed/proprietary assets.

## Workflow and validation

Read the GeoJoystick and shared error/failure dictionaries before scripts, repository mutations or handoffs. Update them with confirmed failures and prevention rules.

GitHub Actions/cloud CI is prohibited for the PC workflow. Do not create, trigger, monitor, query, require or depend on it. Required validation uses repository-owned local tooling. Historical workflow results are dated evidence only.

Use PowerShell/Python for deterministic tasks and repository-aware tooling for complex implementation or review when useful. Before Codex, verify `codex --help`, sandbox, network, approvals, model and reasoning settings; use the lowest-cost capable configuration.

User-facing PowerShell starts with:

```powershell
Clear-Host
$Host.UI.RawUI.WindowTitle = 'GeoJoystick - <task>'
```

Validate exact paths, encoding, permissions, scope, rollback, licences, privacy and changed files. Run `git diff --check`; keep large diagnostics in private Drive. Separate static, build, device, signing, F-Droid and release evidence. Never claim a check that was not performed.

Local builds/tests/lint use repository-owned scripts and remain a user execution boundary. Before ADB action run `adb devices -l`, target the intended serial with `-s`, use synthetic coordinates and review sanitized evidence. Installation and final device signoff remain manual unless explicitly authorized.

## Cleanup and handoff

Clean continuously across branches/worktrees, staging, build/dist outputs, imports, scripts, ADB/QA, backups and Drive. Promote useful evidence first; remove only verified obsolete project-scoped items and prefer reversible Drive actions. Preserve active work, unresolved evidence, accepted QA, manifests, user data, backups, releases, secrets, unrelated changes and Git history.

Handoffs state scope, changes, validation, unvalidated areas, evidence/cleanup disposition and remaining local/manual gates.
