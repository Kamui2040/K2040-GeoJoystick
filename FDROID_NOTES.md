# F-Droid and reproducible builds

GeoJoystick is intended to remain suitable for F-Droid and similar FLOSS Android repositories.

## Build baseline

- Application code: `GPL-3.0-only`
- No ads, analytics, tracking, accounts, subscriptions, paid features, billing, or proprietary updater
- No proprietary Android dependencies in the Gradle build
- JDK 17
- Android SDK Platform 36
- Android SDK Build-Tools 35.0.0
- Gradle 8.13
- Android Gradle Plugin 8.12.3
- maintained build entry point: `python3 tools/build.py`

The maintained build configuration pins inputs required for reproducibility and avoids environment-dependent release metadata.

## Update detection and downstream state

The upstream F-Droid reference uses tag-based version detection:

```yaml
AutoUpdateMode: Version
UpdateCheckMode: Tags
```

The file under `fdroid/` is a contributor reference only. Production metadata in `fdroid/fdroiddata`, and the public F-Droid listing generated from it, are authoritative for the version currently published by F-Droid. Do not copy a volatile downstream `CurrentVersion` value into this repository as if it were canonical project state.

The canonical upstream release remains independently defined by the public GeoJoystick tag and GitHub release.

## Current canonical release evidence

GeoJoystick v0.1.5 uses:

- tag: `v0.1.5`
- source commit: `05762c49662ed4f280e3f42ebcfc7e25d1a2a5d5`
- developer APK: `GeoJoystick-v0.1.5.apk`
- developer APK SHA-256: `bdf43cbdde6af2d96dac3c9a68818d79a7090d1f16e8265d83fc2fbcc1f9350b`
- reproducible unsigned APK SHA-256: `9aa1514db585c226e3fba195f91a001d9ea46306c57108f961aa2b4a6b60c1cb`

## Reproducibility requirements

For releases intended for F-Droid developer-binary verification:

1. Build from the exact public release tag or commit.
2. Use the maintained pinned JDK, Gradle, Android Gradle Plugin, SDK Platform, and Build-Tools versions.
3. Avoid environment-dependent build inputs or generated release metadata.
4. Compare reproducible unsigned outputs before developer signing.
5. Do not transform, realign, repack, or otherwise rewrite the unsigned APK after the reproducible build unless that transformed layout is itself part of the proven build path and the corresponding developer APK is signed from that exact layout.
6. With Android SDK Build-Tools 35.0.0, sign the exact reproducible unsigned APK using:

   ```text
   apksigner --alignment-preserved --v1-signing-enabled false --v2-signing-enabled true --v3-signing-enabled true --v4-signing-enabled false
   ```

   Do not rely on `apksigner` signing-scheme defaults.
7. Verify the signature-copy path against the intended developer APK. For v0.1.5, the final developer APK was verified byte-for-byte against both `apksigcopier` 1.1.1 and the exact F-Droid signature-copy implementation used by the downstream build job.

## APK layout and signing rationale

F-Droid developer-binary verification depends on the unsigned APK retaining the byte layout expected by the developer-signed artifact. A post-build APK realignment or other ZIP rewrite can therefore break signature-copy reconstruction even when entry payloads are unchanged.

For developer-binary releases, matching decompressed APK entries is not sufficient. Different DEFLATE implementations can encode identical entry payloads into different compressed bytes, changing offsets and the bytes covered by APK Signature Schemes v2/v3. The canonical developer APK must therefore be signed from an unsigned artifact that is byte-identical to the F-Droid-equivalent build.

Android SDK Build-Tools 35.0.0 `apksigner` can also rewrite Android ZIP alignment extra fields unless `--alignment-preserved` is used. GeoJoystick pins the release signing schemes and preserves alignment explicitly so a future tool default cannot silently change the canonical developer APK layout or signature set.

These requirements affect reproducibility and developer signing only; they do not change GeoJoystick runtime behavior.

## Metadata and screenshot safety

- Store screenshots and examples must use synthetic or deliberately sanitized data.
- Do not publish authentic saved locations, favorites, device identifiers, private QA data, or machine-specific paths.
- Preserve OpenStreetMap attribution where required.
- `fastlane/metadata/android/SCREENSHOT_PROVENANCE.md` owns the provenance and hashes for repository screenshot assets.

## Repository reference

The upstream metadata template is `fdroid/com.k2040.geojoystick.yml.template`. Production metadata in `fdroid/fdroiddata` remains authoritative for F-Droid publication.
