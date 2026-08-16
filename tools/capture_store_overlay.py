#!/usr/bin/env python3
"""Capture the real GeoJoystick overlay over a debug-only neutral background.

This companion runs after capture_store_screenshots.py has produced the normal
localized store set. It starts GeoJoystick only with that harness's fixed
synthetic coordinates, captures the expanded system overlay over a blank
debug-only GeoJoystick activity, stops simulation through MainActivity's visible
Stop control, and restores the complete app preference file byte-for-byte.

The overlay window itself is intentionally not discovered through UIAutomator:
TYPE_APPLICATION_OVERLAY accessibility exposure is device/OEM dependent. The
neutral activity exists only in app/src/debug and is excluded from release builds.
"""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import shutil
import tempfile
import time
import xml.etree.ElementTree as ET

import _qa_accessibility_device_impl as impl
import capture_store_screenshots as core


PACKAGE = core.PACKAGE
PREFS_PATH = core.PREFS_PATH
BACKUP_PATH = "cache/geojoystick_issue12_overlay_prefs_backup.xml"
NEUTRAL_ACTIVITY = ".NeutralCaptureActivity"
OVERLAY_PROVENANCE = "overlay-screenshot-provenance.json"
OVERLAY_FILENAME = "05-overlay.png"
NEUTRAL_BACKGROUND = "#ECEFF1"

OVERLAY_LABELS = {
    "en-US": {
        "start": "Start simulation",
        "main_stop": "Stop simulation",
    },
    "de-DE": {
        "start": "Simulation starten",
        "main_stop": "Simulation stoppen",
    },
}


class OverlayCaptureError(RuntimeError):
    pass


def wait_simulation(adb: core.SafeAdb, expected: bool, timeout: float = 8.0) -> None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if core.simulation_active(adb) is expected:
            return
        time.sleep(0.25)
    raise OverlayCaptureError(
        "GeoJoystick simulation did not become "
        + ("active" if expected else "inactive")
    )


def current_locale(adb: core.SafeAdb) -> str:
    try:
        root = ET.fromstring(core.read_app_file(adb, PREFS_PATH))
    except ET.ParseError:
        return "en-US"
    for node in root:
        if node.tag == "string" and node.attrib.get("name") == "app_language":
            return "de-DE" if (node.text or "") == "de" else "en-US"
    return "en-US"


def stop_via_main(
    adb: core.SafeAdb,
    locale: str,
    *,
    allow_force_stop: bool,
) -> None:
    if not core.simulation_active(adb):
        return

    labels = core.LOCALES[locale]
    stop_label = OVERLAY_LABELS[locale]["main_stop"]
    ui_error: BaseException | None = None

    try:
        adb.launch()
        core.home_ready(adb, labels)
        stop = core.require_node(
            adb,
            lambda item: item.clickable and item.desc == stop_label,
            stop_label,
            scroll=True,
            attempts=10,
        )
        adb.tap(stop.bounds)
        wait_simulation(adb, False)
        return
    except BaseException as exc:
        ui_error = exc

    if allow_force_stop:
        adb.force_stop()
        try:
            wait_simulation(adb, False, timeout=3.0)
            return
        except BaseException as force_error:
            raise OverlayCaptureError(
                f"MainActivity stop failed: {ui_error}; "
                f"force-stop recovery also failed: {force_error}"
            ) from ui_error

    raise OverlayCaptureError(
        f"MainActivity stop failed: {ui_error}"
    ) from ui_error


def base_manifest(output: Path, source_revision: str) -> dict[str, object]:
    path = output / "screenshot-provenance.json"
    if not path.is_file():
        raise OverlayCaptureError(
            "normal screenshot-provenance.json is missing; run the base capture first"
        )
    data = json.loads(path.read_text(encoding="utf-8"))
    if data.get("source_revision") != source_revision:
        raise OverlayCaptureError("base screenshot provenance revision mismatch")
    if data.get("package") != PACKAGE:
        raise OverlayCaptureError("base screenshot provenance package mismatch")
    if data.get("locales") != ["en-US", "de-DE"]:
        raise OverlayCaptureError(f"unexpected base locales: {data.get('locales')!r}")
    if data.get("theme") not in {"light", "dark"}:
        raise OverlayCaptureError("base screenshot theme is invalid")
    screenshots = data.get("screenshots")
    if not isinstance(screenshots, list) or len(screenshots) != 8:
        raise OverlayCaptureError("base screenshot set must contain exactly 8 images")
    return data


def ensure_output_clear(output: Path) -> None:
    if (output / OVERLAY_PROVENANCE).exists():
        raise OverlayCaptureError("overlay provenance already exists; refusing overwrite")
    for locale in OVERLAY_LABELS:
        path = output / locale / "images" / "phoneScreenshots" / OVERLAY_FILENAME
        if path.exists():
            raise OverlayCaptureError(f"overlay screenshot already exists: {path}")


def capture_one(
    adb: core.SafeAdb,
    locale: str,
    theme: str,
    staging: Path,
) -> dict[str, object]:
    labels = core.LOCALES[locale]
    start_label = OVERLAY_LABELS[locale]["start"]

    core.write_sanitized_state(adb, labels["language"], theme)
    adb.launch()
    core.home_ready(adb, labels)

    start = core.require_node(
        adb,
        lambda item: item.clickable and item.desc == start_label,
        start_label,
        scroll=True,
        attempts=10,
    )
    adb.tap(start.bounds)
    wait_simulation(adb, True)

    adb.shell(
        "am",
        "start",
        "-W",
        "-n",
        f"{PACKAGE}/{NEUTRAL_ACTIVITY}",
        timeout=30.0,
    )
    core.wait_activity(adb, NEUTRAL_ACTIVITY)

    # The overlay is composed by WindowManager and is expected in screencap even
    # when UIAutomator does not expose its accessibility nodes.
    time.sleep(0.8)
    destination = staging / locale / "images" / "phoneScreenshots" / OVERLAY_FILENAME
    metadata = core.capture_png(adb, destination)
    metadata.update(
        {
            "locale": locale,
            "screen": "overlay",
            "path": f"{locale}/images/phoneScreenshots/{OVERLAY_FILENAME}",
        }
    )

    stop_via_main(adb, locale, allow_force_stop=False)
    return metadata


def validate_overlay_tree(root: Path) -> dict[str, object]:
    provenance_path = root / OVERLAY_PROVENANCE
    if not provenance_path.is_file():
        raise OverlayCaptureError(f"{OVERLAY_PROVENANCE} is missing")

    data = json.loads(provenance_path.read_text(encoding="utf-8"))
    if data.get("schema") != 1:
        raise OverlayCaptureError("unsupported overlay provenance schema")
    if data.get("package") != PACKAGE:
        raise OverlayCaptureError("overlay provenance package mismatch")
    if data.get("capture_kind") != "real Android system overlay over debug-only neutral background":
        raise OverlayCaptureError("overlay capture kind mismatch")

    if data.get("neutral_background") != {
        "kind": "GeoJoystick debug-only blank activity",
        "color": NEUTRAL_BACKGROUND,
        "included_in_release": False,
    }:
        raise OverlayCaptureError("neutral-background provenance mismatch")

    screenshots = data.get("screenshots")
    if not isinstance(screenshots, list) or len(screenshots) != 2:
        raise OverlayCaptureError("overlay provenance must list exactly 2 screenshots")

    expected_paths = {
        f"{locale}/images/phoneScreenshots/{OVERLAY_FILENAME}"
        for locale in OVERLAY_LABELS
    }
    actual_paths: set[str] = set()
    dimensions: set[tuple[int, int]] = set()

    for item in screenshots:
        relative = item.get("path")
        if not isinstance(relative, str):
            raise OverlayCaptureError("overlay screenshot path is invalid")
        path = root / relative
        if not path.is_file():
            raise OverlayCaptureError(f"overlay screenshot missing: {relative}")
        payload = path.read_bytes()
        width, height = core.png_size(payload)
        digest = core.sha256_bytes(payload)
        if digest != item.get("sha256"):
            raise OverlayCaptureError(f"overlay screenshot hash mismatch: {relative}")
        if width != item.get("width") or height != item.get("height"):
            raise OverlayCaptureError(f"overlay screenshot dimensions mismatch: {relative}")
        dimensions.add((width, height))
        actual_paths.add(relative)

    if actual_paths != expected_paths:
        raise OverlayCaptureError(
            f"overlay screenshot set mismatch: {sorted(actual_paths)!r}"
        )
    if len(dimensions) != 1:
        raise OverlayCaptureError(
            f"overlay screenshot dimensions differ: {sorted(dimensions)!r}"
        )

    return {
        "count": 2,
        "dimensions": next(iter(dimensions)),
        "revision": data.get("source_revision"),
    }


def recover_command(args: argparse.Namespace) -> int:
    adb = core.SafeAdb(args.adb, args.serial, PACKAGE)
    core.verify_identity(adb, args)
    locale = current_locale(adb)

    if core.simulation_active(adb):
        stop_via_main(adb, locale, allow_force_stop=True)

    if adb.run_as_probe(f"test -f {BACKUP_PATH}"):
        adb.force_stop()
        adb.run_as(f"cp {BACKUP_PATH} {PREFS_PATH}")
        restored = core.read_app_file(adb, PREFS_PATH)
        backup = core.read_app_file(adb, BACKUP_PATH)
        if restored != backup:
            raise OverlayCaptureError("recovered preferences differ from overlay backup")
        adb.run_as(f"rm -f {BACKUP_PATH}")

    if core.simulation_active(adb):
        raise OverlayCaptureError("simulation remains active after recovery")
    if adb.run_as_probe(f"test -e {BACKUP_PATH}"):
        raise OverlayCaptureError("overlay backup remains after recovery")

    adb.launch()
    print("PASS: overlay capture recovery complete")
    print("PASS: simulation inactive")
    print("PASS: stale overlay preference backup absent")
    return 0


def capture_command(args: argparse.Namespace) -> int:
    repo = Path(__file__).resolve().parents[1]
    if core.git_tracked_status(repo):
        raise OverlayCaptureError(
            "tracked repository files are modified; overlay capture requires clean source"
        )
    source_revision = core.git_revision(repo)

    output = Path(args.output_dir).expanduser().resolve()
    if not output.is_dir():
        raise OverlayCaptureError("base screenshot output directory is missing")
    base = base_manifest(output, source_revision)
    ensure_output_clear(output)

    theme = str(base["theme"])
    locales = list(base["locales"])

    adb = core.SafeAdb(args.adb, args.serial, PACKAGE)
    core.verify_identity(adb, args)
    if core.simulation_active(adb):
        raise OverlayCaptureError(
            "GeoJoystick simulation is active; run overlay recover before capture"
        )
    if not adb.run_as_probe(f"test -f {PREFS_PATH}"):
        raise OverlayCaptureError("GeoJoystick preference file is unavailable")
    if adb.run_as_probe(f"test -e {BACKUP_PATH}"):
        raise OverlayCaptureError("stale Issue #12 overlay preference backup exists")

    version_name, version_code = core.app_version(adb)
    if version_name != base.get("version_name") or version_code != base.get("version_code"):
        raise OverlayCaptureError("installed app version differs from base screenshot capture")

    original_font_scale = adb.shell("settings", "get", "system", "font_scale") or "1.0"
    original_prefs = core.read_app_file(adb, PREFS_PATH)
    adb.force_stop()
    adb.run_as(f"cp {PREFS_PATH} {BACKUP_PATH}")
    if core.read_app_file(adb, BACKUP_PATH) != original_prefs:
        raise OverlayCaptureError("overlay preference backup does not match live preferences")

    staging = Path(tempfile.mkdtemp(prefix="geojoystick-store-overlay."))
    primary_error: BaseException | None = None
    recovery_error: BaseException | None = None
    screenshots: list[dict[str, object]] = []
    active_locale = locales[0]

    try:
        adb.shell("settings", "put", "system", "font_scale", "1.0")
        time.sleep(0.3)
        for locale in locales:
            active_locale = locale
            screenshots.append(capture_one(adb, locale, theme, staging))
    except BaseException as exc:
        primary_error = exc
    finally:
        try:
            if core.simulation_active(adb):
                stop_via_main(adb, active_locale, allow_force_stop=True)
            adb.force_stop()
            if adb.run_as_probe(f"test -f {BACKUP_PATH}"):
                adb.run_as(f"cp {BACKUP_PATH} {PREFS_PATH}")
                restored = core.read_app_file(adb, PREFS_PATH)
                if restored != original_prefs:
                    raise OverlayCaptureError(
                        "restored preferences differ from original overlay backup"
                    )
                adb.run_as(f"rm -f {BACKUP_PATH}")
            adb.shell("settings", "put", "system", "font_scale", original_font_scale)
            adb.shell("rm", "-f", core.UI_DUMP_PATH, check=False)
            if core.simulation_active(adb):
                raise OverlayCaptureError("simulation remains active after overlay recovery")
            adb.launch()
            if adb.run_as_probe(f"test -e {BACKUP_PATH}"):
                raise OverlayCaptureError("overlay preference backup residue remains")
        except BaseException as exc:
            recovery_error = exc

    if primary_error is not None or recovery_error is not None:
        shutil.rmtree(staging, ignore_errors=True)
        if primary_error is not None and recovery_error is not None:
            raise OverlayCaptureError(
                f"{primary_error}; recovery also failed: {recovery_error}"
            ) from primary_error
        if primary_error is not None:
            raise primary_error
        raise recovery_error  # type: ignore[misc]

    provenance = {
        "schema": 1,
        "capture_kind": "real Android system overlay over debug-only neutral background",
        "package": PACKAGE,
        "version_name": version_name,
        "version_code": version_code,
        "source_revision": source_revision,
        "theme": theme,
        "locales": locales,
        "synthetic_location": {
            "latitude": core.SYNTHETIC_LATITUDE,
            "longitude": core.SYNTHETIC_LONGITUDE,
            "altitude_m": core.SYNTHETIC_ALTITUDE,
            "purpose": "fixed non-user overlay store-screenshot test data",
        },
        "neutral_background": {
            "kind": "GeoJoystick debug-only blank activity",
            "color": NEUTRAL_BACKGROUND,
            "included_in_release": False,
        },
        "privacy": {
            "device_identity_recorded": False,
            "authentic_location_history_used": False,
            "app_preferences_restored_after_capture": True,
            "mock_location_selection_changed": False,
            "simulation_started": True,
            "simulation_used_only_synthetic_coordinates": True,
            "simulation_stopped_after_each_capture": True,
        },
        "screenshots": screenshots,
    }
    (staging / OVERLAY_PROVENANCE).write_text(
        json.dumps(provenance, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )

    validate_overlay_tree(staging)
    for locale in locales:
        source = staging / locale / "images" / "phoneScreenshots" / OVERLAY_FILENAME
        destination = output / locale / "images" / "phoneScreenshots" / OVERLAY_FILENAME
        shutil.copy2(source, destination)
    shutil.copy2(staging / OVERLAY_PROVENANCE, output / OVERLAY_PROVENANCE)
    shutil.rmtree(staging, ignore_errors=True)

    validated = validate_overlay_tree(output)
    print(
        f"PASS: captured {validated['count']} neutral-background overlay screenshots "
        f"at {validated['dimensions'][0]}x{validated['dimensions'][1]}"
    )
    print(f"PASS: source revision {source_revision}")
    print(f"OUTPUT: {output}")
    return 0


def validate_command(args: argparse.Namespace) -> int:
    root = Path(args.input_dir).expanduser().resolve()
    result = validate_overlay_tree(root)
    print(
        f"PASS: {result['count']} overlay screenshots, "
        f"{result['dimensions'][0]}x{result['dimensions'][1]}, "
        f"revision {result['revision']}"
    )
    return 0


def add_identity_args(command: argparse.ArgumentParser) -> None:
    command.add_argument("--serial", required=True)
    command.add_argument("--expected-model", required=True)
    command.add_argument("--expected-product", required=True)
    command.add_argument("--expected-device", required=True)
    command.add_argument("--expected-android", required=True)
    command.add_argument("--expected-api", required=True)
    command.add_argument(
        "--adb",
        default=os.path.expanduser("~/Android/Sdk/platform-tools/adb"),
    )


def self_test_command(_args: argparse.Namespace) -> int:
    if set(OVERLAY_LABELS) != set(core.LOCALES):
        raise OverlayCaptureError("overlay/base locale sets differ")
    if NEUTRAL_BACKGROUND != "#ECEFF1":
        raise OverlayCaptureError("unexpected neutral background constant")
    for locale, labels in OVERLAY_LABELS.items():
        if not labels["start"] or not labels["main_stop"]:
            raise OverlayCaptureError(f"overlay labels incomplete for {locale}")
    print("Store overlay screenshot harness self-test: PASS")
    return 0


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(
        description="Capture, recover, or validate neutral-background GeoJoystick overlay screenshots"
    )
    subparsers = result.add_subparsers(dest="command", required=True)

    capture = subparsers.add_parser("capture", help="capture localized overlay screenshots")
    add_identity_args(capture)
    capture.add_argument("--output-dir", required=True)
    capture.set_defaults(func=capture_command)

    recover = subparsers.add_parser(
        "recover",
        help="stop a stale capture simulation and restore any retained overlay preference backup",
    )
    add_identity_args(recover)
    recover.set_defaults(func=recover_command)

    validate = subparsers.add_parser("validate", help="validate overlay screenshots")
    validate.add_argument("--input-dir", required=True)
    validate.set_defaults(func=validate_command)

    self_test = subparsers.add_parser("self-test", help="run device-free overlay checks")
    self_test.set_defaults(func=self_test_command)
    return result


def main() -> int:
    args = parser().parse_args()
    try:
        return int(args.func(args))
    except (
        OverlayCaptureError,
        core.CaptureError,
        impl.QAError,
        OSError,
        ET.ParseError,
    ) as exc:
        print(f"FAIL: {exc}", file=os.sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
