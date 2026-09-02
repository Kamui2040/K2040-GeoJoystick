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

The maintained build configuration pins inputs required for reproducibility and avoids environment-dependent build metadata.

## Update detection

The production F-Droid metadata currently uses:

```yaml
AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 0.1.4
CurrentVersionCode: 104
```

The public `v0.1.5` tag now points to the verified v0.1.5 release source, so the existing tag-based update path has been triggered. Production metadata in `fdroid/fdroiddata` remains authoritative and may continue to show v0.1.4 until F-Droid processes the update.

## Current canonical source and APK

GeoJoystick v0.1.5 uses:

- source commit: `05762c49662ed4f280e3f42ebcfc7e25d1a2a5d5`
- APK: `GeoJoystick-v0.1.5.apk`
- APK SHA-256: `f8aa3edde469941993450511c1996501f782aeca7f6b6ee17cb5a4498859f2c0`
- reproducible unsigned APK SHA-256: `2b170f39504f4cae64eb4bda2b519615f2cf3bae929c32cc9c97921bffd54991`

## Reproducibility requirements

For releases intended for F-Droid developer-binary verification:

- build from the exact public release tag or commit;
- use the maintained JDK, Gradle, Android Gradle Plugin, SDK Platform, and Build-Tools versions;
- avoid environment-dependent build inputs;
- compare unsigned outputs before signing;
- with Android SDK Build-Tools 35.0.0, sign the exact reproducible unsigned APK using `apksigner --alignment-preserved --v1-signing-enabled false --v2-signing-enabled true --v3-signing-enabled true --v4-signing-enabled false`; do not rely on signing-scheme defaults;
- do not transform an unsigned APK after the reproducible build unless the corresponding developer APK is produced from that exact transformed layout;
- verify the F-Droid signature-copy path against the intended developer APK.

## v0.1.4 compatibility finding

During v0.1.4 reproducibility work, an additional metadata-side APK realignment step was found to make the unsigned APK layout incompatible with developer-binary signature-copy verification.

The technical requirement is that the unsigned APK used for comparison retain the byte layout expected by the corresponding signed developer APK. Any additional post-build APK transformation therefore requires its own reproducibility proof.

This finding concerns build reproducibility only and does not change GeoJoystick runtime behavior.

## Build-Tools 35 signing compatibility finding

During v0.1.5 release-candidate verification, default `apksigner` from Android SDK Build-Tools 35.0.0 rewrote Android ZIP alignment extra fields on stored APK entries. The APK entry payloads remained identical, but F-Droid-style signature-copy reconstruction was not byte-for-byte identical.

Signing the exact frozen unsigned APK with `apksigner --alignment-preserved` kept the pre-sign ZIP local records byte-identical. `apksigcopier` 1.1.1 then reconstructed the signed APK byte-for-byte and its unsigned comparison passed. GeoJoystick also pins v1=false, v2=true, v3=true, and v4=false for the developer APK so later `apksigner` defaults cannot silently change the release signature set. This requirement affects developer signing only; it does not change application source, the reproducible unsigned build, runtime behavior, or the F-Droid build recipe.

## Store metadata safety

Store screenshots and examples must use synthetic or deliberately sanitized data. OpenStreetMap attribution must remain visible where required.

## Repository reference

The metadata file under `fdroid/` is a contributor reference. Production metadata in `fdroid/fdroiddata` remains authoritative.
