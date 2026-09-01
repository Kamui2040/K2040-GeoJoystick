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
- Validation harnesses must verify configuration semantics rather than formatting-specific literals. For Gradle version gates, parse or syntax-tolerantly match `versionCode` and `versionName`; equivalent quote or whitespace styles must not be treated as a version change.
- When using `grep -E` or `git grep -E`, use POSIX extended regular-expression syntax only. PCRE-only constructs such as non-capturing groups require an explicitly compatible regex engine and must not be passed to an ERE command.
- Generated Android string resources must escape ASCII apostrophes using Android resource syntax, preserve NFC Unicode, and pass `tools/test_localization_resources.py` before build or commit gates.
- Multi-line Android string resources must encode semantic line breaks with explicit `\\n` escapes. Literal XML whitespace or line breaks are collapsed by Android resource compilation and must not be relied on for user-visible line breaks; changelog-style resources must be validated for this explicitly.
- For device QA that changes network state, verify the effective transport state after mutation before judging offline behavior. A successful `svc`/settings command or a single settings key is not proof that all active network transports are disabled; if true offline state cannot be verified, treat the offline gate as pending rather than as an app failure.
- Keep source, build, runtime/device, signing, reproducibility, and publication evidence distinct.
- If an automated Android runtime-state detector disagrees with verified known-active manual behavior, treat the detector as invalid evidence. Stop detector retries, remove superseded detector logic, and report the affected automated gate as requiring manual acceptance unless a reliable app-owned debug interface exists.
- In RTL layouts, do not manually invert text glyphs that Android mirrors automatically. Validate directional controls and isolate mixed-direction numeric and legal tokens on a real RTL surface before acceptance.
- For manual pinch QA, judge anchoring only against the exact geographic point under the midpoint between the two active touches. A nearby off-centre landmark or the screen-centred `+`/`−` controls are not valid anchor references.
- Do not claim installation, physical-device behavior, signing, reproducibility, or release readiness without corresponding evidence.

## Contributions

- Keep `main` stable and prefer focused, reviewable changes.
- Multi-touch map gestures must derive every rendered center from one stable geographic pinch anchor and the live gesture midpoint. Do not repeatedly re-anchor from center state already mutated by asynchronous pointer events.
- Immediately before any GitHub issue or pull-request comment, review, metadata change, close, or merge mutation, verify the exact target number, title, state, base/head where applicable, and that it belongs to the current work unit. Do not mutate historical items while switching tools or testing an action.
- Before any GitHub write, verify that the operation type itself exactly matches the intended resource mutation. Commenting, assignment, issue metadata, pull-request metadata, review, close, and merge operations are not interchangeable; if the exact operation is unavailable or unclear, stop and resolve that mismatch instead of substituting another write.
- Do not probe repository write capability with placeholder mutations. Verify tool capability and repository state through discovery or read-only operations before any public or tracked write.
- Preflight generated handoff scripts by compiling the exact final file text. Avoid nesting the same triple-quote delimiter when embedding code or fixtures.
- Preflight generated handoff scripts beyond syntax compilation when runtime-only paths matter. Execute a non-mutating self-test or representative path that resolves imports, referenced names, cleanup, and error branches so missing runtime names are caught before handoff.
- Assistant-generated mutation handoffs must complete semantic preflight before the first repository write, arm rollback before that write, and parse fixed-column Git porcelain output without trimming leading whitespace.
- When a validator runs against a shadow or synthetic repository tree, include every repository file and dependency that validator reads. Missing harness fixtures or dependencies are harness failures and must not be reported as product validation failures.
- Top-level script wrappers must not convert successful `SystemExit(0)` into a failure. Prefer `raise SystemExit(main())` without an enclosing `BaseException` catch, or catch `Exception`; if `SystemExit` must be intercepted, preserve exit code 0 as success.
- Do not create placeholder GitHub issues or pull requests as an intermediate tooling step. Prepare the complete intended title and scope before creating the public item.
- After a GitHub contents-API commit advances a branch, refetch each later target file from that branch before the next update instead of reusing an earlier blob SHA.
- When a script intentionally switches the current worktree to another branch, capture the validation baseline after that switch or compare stable worktree topology. Do not require a pre-switch `git worktree list --porcelain` snapshot to remain byte-identical after the intended branch change.
- When a script intentionally creates a commit, do not require the current worktree's porcelain HEAD field to remain byte-identical. Validate stable worktree topology and unrelated worktrees separately, and verify the current worktree against the expected new commit.
- Preserve GPL-3.0-only licensing and required third-party attribution and provenance.
- Do not commit generated APK/AAB files, local build caches, signing material, or machine-specific configuration.
- Normal public issues and pull requests may document bugs, implementation work, compatibility changes, localization targets, and other contributor-relevant changes.
- Keep public documentation focused on information useful to users and contributors.
