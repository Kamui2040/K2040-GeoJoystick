#!/usr/bin/env python3
"""Build and sign GeoJoystick using an external persisted signing-properties file."""

from __future__ import annotations

import os
import platform
import stat
from pathlib import Path

import build as base

SIGNING_PROPERTIES_ENV = "GEOJOYSTICK_SIGNING_PROPERTIES"
MAX_SIGNING_PROPERTIES_BYTES = 16 * 1024
ALLOWED_KEYS = {"storePassword", "keyPassword"}


def is_within(path: Path, parent: Path) -> bool:
    try:
        path.resolve().relative_to(parent.resolve())
        return True
    except ValueError:
        return False


def has_reparse_attribute(path: Path) -> bool:
    attributes = getattr(path.lstat(), "st_file_attributes", 0)
    reparse_flag = getattr(stat, "FILE_ATTRIBUTE_REPARSE_POINT", 0)
    return bool(reparse_flag and attributes & reparse_flag)


def resolve_signing_properties() -> Path:
    raw = os.environ.get(SIGNING_PROPERTIES_ENV, "").strip()
    if not raw:
        base.fail(
            f"{SIGNING_PROPERTIES_ENV} is not configured. "
            "Point it to the dedicated local signing-properties file."
        )

    candidate = Path(os.path.expandvars(raw)).expanduser()
    if not candidate.is_absolute():
        base.fail(f"{SIGNING_PROPERTIES_ENV} must contain an absolute path.")

    try:
        path = candidate.resolve(strict=True)
    except OSError as exception:
        base.fail(f"Signing-properties file could not be resolved: {exception}")

    if not path.is_file() or path.is_symlink() or has_reparse_attribute(path):
        base.fail("Signing-properties path must be a regular non-reparse file.")

    size = path.stat().st_size
    if size <= 0 or size > MAX_SIGNING_PROPERTIES_BYTES:
        base.fail(
            "Signing-properties file size is outside the supported range "
            f"(1..{MAX_SIGNING_PROPERTIES_BYTES} bytes)."
        )

    project_root = base.ROOT.parent.resolve()
    keystore_parent = base.RELEASE_KEYSTORE.parent.resolve()
    if is_within(path, project_root):
        base.fail("Signing-properties file must be stored outside the GeoJoystick project root.")
    if path.parent.resolve() == keystore_parent:
        base.fail("Signing-properties file must not be stored beside the release keystore.")

    return path


def parse_signing_properties(path: Path) -> dict[str, str]:
    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeError as exception:
        base.fail(f"Signing-properties file is not valid UTF-8: {exception}")
    except OSError as exception:
        base.fail(f"Signing-properties file could not be read: {exception}")

    values: dict[str, str] = {}
    for line_number, raw_line in enumerate(text.splitlines(), start=1):
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in raw_line:
            base.fail(f"Signing-properties line {line_number} is malformed.")

        key, value = raw_line.split("=", 1)
        key = key.strip()
        if key not in ALLOWED_KEYS:
            base.fail(f"Unsupported signing-properties key on line {line_number}: {key}")
        if key in values:
            base.fail(f"Duplicate signing-properties key: {key}")
        values[key] = value

    if "storePassword" not in values:
        base.fail("Signing-properties file is missing storePassword.")

    store_password = base.require_signing_password(
        values["storePassword"],
        "Release-keystore password",
    )
    key_password = values.get("keyPassword", "")
    if key_password:
        key_password = base.require_signing_password(
            key_password,
            "Private-key password",
        )
    else:
        key_password = store_password

    return {
        "storePassword": store_password,
        "keyPassword": key_password,
    }


def collect_persisted_signing_session() -> base.ReleaseSigningSession:
    properties_path = resolve_signing_properties()
    values = parse_signing_properties(properties_path)

    store_file = base.RELEASE_KEYSTORE
    if store_file.is_symlink() or not store_file.is_file() or has_reparse_attribute(store_file):
        base.fail("Release keystore must be a regular non-reparse file.")

    actual_store_hash = base.sha256_file(store_file)
    if actual_store_hash != base.EXPECTED_RELEASE_KEYSTORE_SHA256:
        base.fail(
            "Release keystore SHA-256 mismatch: "
            f"expected {base.EXPECTED_RELEASE_KEYSTORE_SHA256}, got {actual_store_hash}"
        )

    print(f"Release signing properties: configured via {SIGNING_PROPERTIES_ENV}")
    print(f"Release keystore SHA-256: {actual_store_hash}")

    return base.ReleaseSigningSession(
        store_file=store_file,
        key_alias=base.EXPECTED_RELEASE_KEY_ALIAS,
        store_password=values["storePassword"],
        key_password=values["keyPassword"],
    )


def main() -> None:
    if platform.system() not in {"Windows", "Linux", "Darwin"}:
        base.fail(f"Unsupported platform: {platform.system()}")

    print("Build mode: signed-release (persisted local credentials)")
    java_home = base.find_java_home()
    base.run_parser_self_test(java_home)
    sdk = base.find_android_sdk()
    build_tools = base.resolve_sdk_components(sdk, java_home)
    base.write_local_properties(sdk)
    gradle = base.ensure_gradle()
    artifacts = base.run_build(
        gradle,
        java_home,
        build_tools,
        "signed-release",
    )

    signing = collect_persisted_signing_session()
    base.verify_release_keystore(signing, java_home)
    artifacts.append(
        base.sign_release_apk(
            artifacts,
            signing,
            sdk,
            build_tools,
            java_home,
        )
    )
    base.publish(artifacts)


if __name__ == "__main__":
    main()
