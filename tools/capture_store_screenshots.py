#!/usr/bin/env python3
"""Capture sanitized, real-device GeoJoystick store screenshots.

The capture workflow is intentionally device-generic. Maintainer/device identity
values are runtime-only and are never written into the generated provenance.
The app's complete preferences are backed up inside its sandbox, replaced with a
known sanitized synthetic state while the app is stopped, and restored byte-for-
byte before the script exits. The workflow never selects a mock-location app,
never starts GeoJoystick simulation, and never clears app data.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET

import _qa_accessibility_device_impl as impl
from qa_accessibility_device import SafeAdb


PACKAGE = "com.k2040.geojoystick"
MAIN_ACTIVITY = ".MainActivity"
MAP_ACTIVITY = ".MapActivity"
PREFS_PATH = "shared_prefs/geojoystick.xml"
BACKUP_PATH = "cache/geojoystick_issue12_prefs_backup.xml"
UI_DUMP_PATH = "/data/local/tmp/geojoystick_issue12_ui.xml"

SYNTHETIC_LATITUDE = 51.234567
SYNTHETIC_LONGITUDE = 10.123456
SYNTHETIC_ALTITUDE = 123.0

SCREENS = (
    ("01-home.png", "home"),
    ("02-map.png", "map"),
    ("03-settings.png", "settings"),
    ("04-about.png", "about"),
)

LOCALES = {
    "en-US": {
        "language": "en",
        "about": "About GeoJoystick",
        "settings": "Settings",
        "map_label": "Map",
        "map_title": "Choose location",
        "about_close": "Close About",
        "home_status": "Status collapsed. Tap to expand",
    },
    "de-DE": {
        "language": "de",
        "about": "Über GeoJoystick",
        "settings": "Einstellungen",
        "map_label": "Karte",
        "map_title": "Standort wählen",
        "about_close": "Info schließen",
        "home_status": "Status eingeklappt. Zum Öffnen tippen",
    },
}


class CaptureError(RuntimeError):
    pass


def sha256_bytes(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


def raw_long(value: float) -> int:
    return struct.unpack(">q", struct.pack(">d", value))[0]


def png_size(payload: bytes) -> tuple[int, int]:
    if len(payload) < 24 or payload[:8] != b"\x89PNG\r\n\x1a\n":
        raise CaptureError("screencap output is not a PNG")
    width, height = struct.unpack(">II", payload[16:24])
    if width <= 0 or height <= 0:
        raise CaptureError(f"invalid screenshot dimensions: {width}x{height}")
    return width, height


def remove_named(root: ET.Element, name: str) -> None:
    for child in list(root):
        if child.attrib.get("name") == name:
            root.remove(child)


def put_string(root: ET.Element, name: str, value: str) -> None:
    remove_named(root, name)
    node = ET.Element("string", {"name": name})
    node.text = value
    root.append(node)


def put_boolean(root: ET.Element, name: str, value: bool) -> None:
    remove_named(root, name)
    root.append(
        ET.Element(
            "boolean",
            {"name": name, "value": "true" if value else "false"},
        )
    )


def put_int(root: ET.Element, name: str, value: int) -> None:
    remove_named(root, name)
    root.append(ET.Element("int", {"name": name, "value": str(value)}))


def put_long(root: ET.Element, name: str, value: int) -> None:
    remove_named(root, name)
    root.append(ET.Element("long", {"name": name, "value": str(value)}))


def sanitized_preferences(language: str, theme: str) -> bytes:
    if language not in {"en", "de"}:
        raise CaptureError(f"unsupported capture language: {language}")
    if theme not in {"light", "dark"}:
        raise CaptureError(f"unsupported capture theme: {theme}")

    root = ET.Element("map")
    put_boolean(root, "welcome_acknowledged", True)
    put_string(root, "app_language", language)
    put_string(root, "app_appearance", theme)
    put_boolean(root, "restore_last_position", False)

    put_long(root, "manual_latitude", raw_long(SYNTHETIC_LATITUDE))
    put_long(root, "manual_longitude", raw_long(SYNTHETIC_LONGITUDE))
    put_long(root, "manual_altitude", raw_long(SYNTHETIC_ALTITUDE))

    # Store-capture defaults are explicit so hidden maintainer preferences cannot
    # leak into Settings or later UI changes.
    put_int(root, "overlay_opacity_percent", 85)
    put_int(root, "overlay_size_percent", 80)
    put_boolean(root, "overlay_high_contrast", False)
    put_long(root, "overlay_custom_speed", raw_long(5.0))
    put_string(root, "overlay_custom_speed_name", "Demo")

    favorite_offsets = (
        (0.000000, 0.000000, 123.0),
        (0.111111, 0.111111, 150.0),
        (-0.222222, 0.222222, 80.0),
        (0.333333, -0.333333, 40.0),
        (-0.444444, -0.444444, 200.0),
    )
    for slot, (lat_delta, lng_delta, altitude) in enumerate(favorite_offsets, 1):
        put_boolean(root, f"favorite_{slot}_set", True)
        put_string(root, f"favorite_{slot}_name", f"Demo {slot}")
        put_long(
            root,
            f"favorite_{slot}_latitude",
            raw_long(SYNTHETIC_LATITUDE + lat_delta),
        )
        put_long(
            root,
            f"favorite_{slot}_longitude",
            raw_long(SYNTHETIC_LONGITUDE + lng_delta),
        )
        put_long(root, f"favorite_{slot}_altitude", raw_long(altitude))

    payload = ET.tostring(
        root,
        encoding="utf-8",
        xml_declaration=True,
        short_empty_elements=True,
    ) + b"\n"
    return payload


def assert_sanitized_preferences(payload: bytes, language: str, theme: str) -> None:
    root = ET.fromstring(payload)
    names = [child.attrib.get("name", "") for child in root]
    forbidden = {
        "last_latitude",
        "last_longitude",
        "last_altitude",
        "overlay_x",
        "overlay_y",
    }
    leaked = forbidden.intersection(names)
    if leaked:
        raise CaptureError(f"sanitized preferences contain forbidden keys: {sorted(leaked)}")

    def string_value(name: str) -> str | None:
        matches = [
            child
            for child in root
            if child.tag == "string" and child.attrib.get("name") == name
        ]
        return matches[0].text if len(matches) == 1 else None

    if string_value("app_language") != language:
        raise CaptureError("sanitized language did not persist")
    if string_value("app_appearance") != theme:
        raise CaptureError("sanitized theme did not persist")

    favorite_names = {
        string_value(f"favorite_{slot}_name") for slot in range(1, 6)
    }
    if favorite_names != {f"Demo {slot}" for slot in range(1, 6)}:
        raise CaptureError("sanitized favorite names are incomplete")


def git_revision(repo: Path) -> str:
    result = subprocess.run(
        ["git", "-C", str(repo), "rev-parse", "HEAD"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        check=False,
    )
    if result.returncode != 0:
        raise CaptureError(result.stderr.strip() or "could not resolve source revision")
    return result.stdout.strip()


def git_tracked_status(repo: Path) -> str:
    result = subprocess.run(
        ["git", "-C", str(repo), "status", "--porcelain=v1", "--untracked-files=no"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        check=False,
    )
    if result.returncode != 0:
        raise CaptureError(result.stderr.strip() or "could not inspect repository status")
    return result.stdout.strip()


def read_app_file(adb: SafeAdb, path: str) -> bytes:
    return adb.call("exec-out", "run-as", adb.package, "cat", path).stdout


def dump_nodes(adb: SafeAdb) -> list[impl.UiNode]:
    last_detail = "no usable hierarchy"
    try:
        for attempt in range(1, 4):
            adb.shell("rm", "-f", UI_DUMP_PATH, check=False)
            result = adb.call(
                "shell",
                "uiautomator",
                "dump",
                UI_DUMP_PATH,
                check=False,
                timeout=25.0,
            )
            exists = adb.call(
                "shell",
                "test",
                "-s",
                UI_DUMP_PATH,
                check=False,
            ).returncode == 0
            if exists:
                xml_text = adb.shell("cat", UI_DUMP_PATH)
                try:
                    return impl.parse_xml(xml_text)
                except ET.ParseError as exc:
                    last_detail = f"attempt {attempt}: {exc}"
            else:
                stdout = result.stdout.decode("utf-8", errors="replace").strip()
                stderr = result.stderr.decode("utf-8", errors="replace").strip()
                last_detail = stderr or stdout or f"exit {result.returncode}"
            if attempt < 3:
                time.sleep(0.25)
    finally:
        adb.shell("rm", "-f", UI_DUMP_PATH, check=False)
    raise CaptureError(f"uiautomator dump failed: {last_detail}")


def find_node(
    adb: SafeAdb,
    predicate,
    *,
    scroll: bool = False,
    attempts: int = 8,
) -> impl.UiNode | None:
    size = impl.parse_size(adb.shell("wm", "size"))
    for index in range(attempts):
        matches = [
            node
            for node in dump_nodes(adb)
            if node.package == adb.package and predicate(node)
        ]
        if matches:
            return min(matches, key=lambda node: (node.bounds.top, node.bounds.left))
        if scroll and index + 1 < attempts:
            adb.swipe_up(*size)
        else:
            break
    return None


def require_node(adb: SafeAdb, predicate, description: str, **kwargs) -> impl.UiNode:
    node = find_node(adb, predicate, **kwargs)
    if node is None:
        raise CaptureError(f"required UI node not found: {description}")
    return node


def foreground_activity(adb: SafeAdb) -> str:
    text = adb.shell("dumpsys", "activity", "activities", check=False)
    patterns = (
        r"mResumedActivity:.*? ([^\s}]+)",
        r"topResumedActivity=ActivityRecord\{[^ ]+ [^ ]+ ([^\s}]+)",
    )
    for pattern in patterns:
        match = re.search(pattern, text)
        if match:
            return match.group(1)
    return ""


def wait_activity(adb: SafeAdb, suffix: str, timeout: float = 5.0) -> None:
    deadline = time.monotonic() + timeout
    expected = f"{adb.package}/{suffix}"
    while time.monotonic() < deadline:
        current = foreground_activity(adb)
        if current == expected:
            return
        time.sleep(0.2)
    raise CaptureError(f"activity did not become foreground: {expected}")


def simulation_active(adb: SafeAdb) -> bool:
    output = adb.shell("dumpsys", "activity", "services", adb.package, check=False)
    return "MockLocationService" in output


def verify_identity(adb: SafeAdb, args: argparse.Namespace) -> None:
    state = adb.text("get-state", check=False)
    if state != "device":
        raise CaptureError(f"device unavailable/unauthorized: {state or 'absent'}")

    checks = (
        ("ro.product.model", args.expected_model),
        ("ro.product.name", args.expected_product),
        ("ro.product.device", args.expected_device),
        ("ro.build.version.release", args.expected_android),
        ("ro.build.version.sdk", args.expected_api),
    )
    for prop, expected in checks:
        actual = adb.shell("getprop", prop)
        if actual != expected:
            raise CaptureError(f"device identity mismatch for {prop}")


def app_version(adb: SafeAdb) -> tuple[str, int]:
    output = adb.shell("dumpsys", "package", adb.package)
    name_match = re.search(r"\bversionName=([^\s]+)", output)
    code_match = re.search(r"\bversionCode=(\d+)", output)
    if not name_match or not code_match:
        raise CaptureError("could not read installed app version")
    return name_match.group(1), int(code_match.group(1))


def collapse_statusbar(adb: SafeAdb) -> None:
    adb.shell("cmd", "statusbar", "collapse", check=False)
    time.sleep(0.15)


def capture_png(adb: SafeAdb, destination: Path) -> dict[str, object]:
    collapse_statusbar(adb)
    payload = adb.call("exec-out", "screencap", "-p", timeout=20.0).stdout
    width, height = png_size(payload)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(payload)
    return {
        "file": destination.name,
        "width": width,
        "height": height,
        "sha256": sha256_bytes(payload),
        "bytes": len(payload),
    }


def home_ready(adb: SafeAdb, labels: dict[str, str]) -> None:
    wait_activity(adb, MAIN_ACTIVITY)
    require_node(
        adb,
        lambda node: node.desc == labels["home_status"],
        labels["home_status"],
        attempts=6,
    )


def open_map(adb: SafeAdb, labels: dict[str, str]) -> None:
    home_ready(adb, labels)
    node = require_node(
        adb,
        lambda item: item.clickable
        and (
            item.text == labels["map_label"]
            or item.text.endswith("\n" + labels["map_label"])
            or item.text.endswith(labels["map_label"])
        ),
        labels["map_label"],
        attempts=6,
    )
    adb.tap(node.bounds)
    wait_activity(adb, MAP_ACTIVITY)

    require_node(
        adb,
        lambda item: item.text == labels["map_title"],
        labels["map_title"],
        attempts=8,
    )
    expected_coords = f"{SYNTHETIC_LATITUDE:.6f}, {SYNTHETIC_LONGITUDE:.6f}"
    require_node(
        adb,
        lambda item: item.text == expected_coords,
        expected_coords,
        attempts=8,
    )

    deadline = time.monotonic() + 8.0
    attribution_found = False
    message_found = False
    while time.monotonic() < deadline:
        nodes = dump_nodes(adb)
        texts = {node.text for node in nodes if node.package == adb.package}
        attribution_found = "© OpenStreetMap contributors" in texts
        message_found = any("Tap the map" in text or "Drag to pan" in text for text in texts)
        if attribution_found and message_found:
            break
        time.sleep(0.5)
    if not attribution_found:
        raise CaptureError("OpenStreetMap attribution is not exposed in the rendered map hierarchy")
    if not message_found:
        raise CaptureError("bundled map content did not become ready")
    time.sleep(1.5)


def open_settings(adb: SafeAdb, labels: dict[str, str]) -> None:
    adb.force_stop()
    adb.launch()
    home_ready(adb, labels)
    node = require_node(
        adb,
        lambda item: item.desc == labels["settings"],
        labels["settings"],
        attempts=6,
    )
    adb.tap(node.bounds)
    require_node(
        adb,
        lambda item: item.desc in {"Back", "Zurück"},
        "Settings back control",
        attempts=6,
    )


def open_about(adb: SafeAdb, labels: dict[str, str]) -> None:
    adb.force_stop()
    adb.launch()
    home_ready(adb, labels)
    node = require_node(
        adb,
        lambda item: item.desc == labels["about"],
        labels["about"],
        attempts=6,
    )
    adb.tap(node.bounds)
    require_node(
        adb,
        lambda item: item.desc == labels["about_close"],
        labels["about_close"],
        attempts=6,
    )


def write_sanitized_state(adb: SafeAdb, language: str, theme: str) -> None:
    payload = sanitized_preferences(language, theme)
    assert_sanitized_preferences(payload, language, theme)
    adb.force_stop()
    adb.write_app_file(PREFS_PATH, payload)
    after = read_app_file(adb, PREFS_PATH)
    if after != payload:
        raise CaptureError("sanitized preference write did not round-trip byte-for-byte")


def capture_locale(
    adb: SafeAdb,
    locale: str,
    theme: str,
    staging: Path,
) -> list[dict[str, object]]:
    labels = LOCALES[locale]
    write_sanitized_state(adb, labels["language"], theme)
    locale_dir = staging / locale / "images" / "phoneScreenshots"
    results: list[dict[str, object]] = []

    adb.launch()
    home_ready(adb, labels)
    home_meta = capture_png(adb, locale_dir / "01-home.png")
    home_meta.update({"locale": locale, "screen": "home"})
    results.append(home_meta)

    open_map(adb, labels)
    map_meta = capture_png(adb, locale_dir / "02-map.png")
    map_meta.update({"locale": locale, "screen": "map"})
    results.append(map_meta)

    open_settings(adb, labels)
    settings_meta = capture_png(adb, locale_dir / "03-settings.png")
    settings_meta.update({"locale": locale, "screen": "settings"})
    results.append(settings_meta)

    open_about(adb, labels)
    about_meta = capture_png(adb, locale_dir / "04-about.png")
    about_meta.update({"locale": locale, "screen": "about"})
    results.append(about_meta)

    return results


def validate_capture_tree(root: Path) -> dict[str, object]:
    manifest_path = root / "screenshot-provenance.json"
    if not manifest_path.is_file():
        raise CaptureError("screenshot-provenance.json is missing")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if manifest.get("schema") != 1:
        raise CaptureError("unsupported screenshot provenance schema")
    if manifest.get("package") != PACKAGE:
        raise CaptureError("provenance package mismatch")
    screenshots = manifest.get("screenshots")
    if not isinstance(screenshots, list) or not screenshots:
        raise CaptureError("provenance screenshot list is empty")

    expected_paths = {
        f"{locale}/images/phoneScreenshots/{filename}"
        for locale in manifest.get("locales", [])
        for filename, _screen in SCREENS
    }
    actual_paths: set[str] = set()
    hashes: set[str] = set()
    dimensions: set[tuple[int, int]] = set()

    for item in screenshots:
        relative = item.get("path")
        if not isinstance(relative, str):
            raise CaptureError("provenance screenshot path is invalid")
        path = root / relative
        if not path.is_file():
            raise CaptureError(f"screenshot missing: {relative}")
        payload = path.read_bytes()
        width, height = png_size(payload)
        digest = sha256_bytes(payload)
        if digest != item.get("sha256"):
            raise CaptureError(f"screenshot hash mismatch: {relative}")
        if width != item.get("width") or height != item.get("height"):
            raise CaptureError(f"screenshot dimensions mismatch: {relative}")
        if digest in hashes:
            raise CaptureError(f"duplicate screenshot bytes detected: {relative}")
        hashes.add(digest)
        dimensions.add((width, height))
        actual_paths.add(relative)

    if actual_paths != expected_paths:
        missing = sorted(expected_paths - actual_paths)
        extra = sorted(actual_paths - expected_paths)
        raise CaptureError(f"screenshot set mismatch; missing={missing}, extra={extra}")
    if len(dimensions) != 1:
        raise CaptureError(f"screenshot dimensions are inconsistent: {sorted(dimensions)}")

    return {
        "count": len(actual_paths),
        "dimensions": next(iter(dimensions)),
        "revision": manifest.get("source_revision"),
    }


def capture_command(args: argparse.Namespace) -> int:
    repo = Path(__file__).resolve().parents[1]
    if git_tracked_status(repo):
        raise CaptureError("tracked repository files are modified; capture requires a clean source tree")
    source_revision = git_revision(repo)

    output = Path(args.output_dir).expanduser().resolve()
    if output.exists():
        if any(output.iterdir()) if output.is_dir() else True:
            raise CaptureError(f"output path already exists and is not empty: {output}")
        output.rmdir()
    output.parent.mkdir(parents=True, exist_ok=True)

    adb = SafeAdb(args.adb, args.serial, PACKAGE)
    verify_identity(adb, args)
    if simulation_active(adb):
        raise CaptureError("GeoJoystick simulation is active; stop it manually before screenshot capture")
    if not adb.run_as_probe(f"test -f {PREFS_PATH}"):
        raise CaptureError("GeoJoystick preference file is unavailable")
    if adb.run_as_probe(f"test -e {BACKUP_PATH}"):
        raise CaptureError("stale Issue #12 preference backup exists")

    version_name, version_code = app_version(adb)
    original_font_scale = adb.shell("settings", "get", "system", "font_scale") or "1.0"
    original_prefs = read_app_file(adb, PREFS_PATH)
    adb.force_stop()
    adb.run_as(f"cp {PREFS_PATH} {BACKUP_PATH}")
    if read_app_file(adb, BACKUP_PATH) != original_prefs:
        raise CaptureError("preference backup does not match live preferences")

    staging = Path(tempfile.mkdtemp(prefix="geojoystick-store-screenshots."))
    primary_error: BaseException | None = None
    screenshots: list[dict[str, object]] = []

    try:
        adb.shell("settings", "put", "system", "font_scale", "1.0")
        time.sleep(0.3)
        for locale in args.locales:
            screenshots.extend(capture_locale(adb, locale, args.theme, staging))

        for item in screenshots:
            locale = str(item["locale"])
            item["path"] = f"{locale}/images/phoneScreenshots/{item.pop('file')}"

        manifest = {
            "schema": 1,
            "capture_kind": "real Android device screenshots with synthetic sanitized app state",
            "package": PACKAGE,
            "version_name": version_name,
            "version_code": version_code,
            "source_revision": source_revision,
            "theme": args.theme,
            "locales": list(args.locales),
            "synthetic_location": {
                "latitude": SYNTHETIC_LATITUDE,
                "longitude": SYNTHETIC_LONGITUDE,
                "altitude_m": SYNTHETIC_ALTITUDE,
                "purpose": "fixed non-user store-screenshot test data",
            },
            "privacy": {
                "device_identity_recorded": False,
                "authentic_location_history_used": False,
                "app_preferences_restored_after_capture": True,
                "mock_location_selection_changed": False,
                "simulation_started": False,
            },
            "screenshots": screenshots,
        }
        (staging / "screenshot-provenance.json").write_text(
            json.dumps(manifest, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        validate_capture_tree(staging)
        shutil.copytree(staging, output)
        validated = validate_capture_tree(output)
        print(
            f"PASS: captured {validated['count']} sanitized screenshots "
            f"at {validated['dimensions'][0]}x{validated['dimensions'][1]}"
        )
        print(f"PASS: source revision {source_revision}")
        print(f"OUTPUT: {output}")
    except BaseException as exc:
        primary_error = exc
    finally:
        recovery_error: BaseException | None = None
        try:
            adb.force_stop()
            if adb.run_as_probe(f"test -f {BACKUP_PATH}"):
                adb.run_as(f"cp {BACKUP_PATH} {PREFS_PATH}")
                restored = read_app_file(adb, PREFS_PATH)
                if restored != original_prefs:
                    raise CaptureError("restored preferences differ from the original backup")
                adb.run_as(f"rm -f {BACKUP_PATH}")
            adb.shell("settings", "put", "system", "font_scale", original_font_scale)
            adb.shell("rm", "-f", UI_DUMP_PATH, check=False)
            if simulation_active(adb):
                raise CaptureError("simulation became active during screenshot capture")
            adb.launch()
            if adb.run_as_probe(f"test -e {BACKUP_PATH}"):
                raise CaptureError("Issue #12 preference backup residue remains")
            print("PASS: original app preferences restored byte-for-byte")
            print(f"PASS: font scale restored to {original_font_scale}")
            print("PASS: simulation remained inactive")
        except BaseException as exc:
            recovery_error = exc
        shutil.rmtree(staging, ignore_errors=True)

        if primary_error is not None:
            if recovery_error is not None:
                raise CaptureError(
                    f"{primary_error}; recovery also failed: {recovery_error}"
                ) from primary_error
            raise primary_error
        if recovery_error is not None:
            raise recovery_error

    return 0


def validate_command(args: argparse.Namespace) -> int:
    root = Path(args.input_dir).expanduser().resolve()
    validated = validate_capture_tree(root)
    print(
        f"PASS: {validated['count']} screenshots, "
        f"{validated['dimensions'][0]}x{validated['dimensions'][1]}, "
        f"revision {validated['revision']}"
    )
    return 0


def self_test_command(_args: argparse.Namespace) -> int:
    private_fixture = b'''<?xml version="1.0" encoding="utf-8"?>\n<map>\n<string name="favorite_1_name">PRIVATE FAVORITE</string>\n<long name="last_latitude" value="123" />\n<string name="app_language">system</string>\n</map>\n'''
    for locale, labels in LOCALES.items():
        for theme in ("light", "dark"):
            payload = sanitized_preferences(labels["language"], theme)
            assert_sanitized_preferences(payload, labels["language"], theme)
            if b"PRIVATE FAVORITE" in payload or b"last_latitude" in payload:
                raise CaptureError("sanitized preference builder retained private fixture data")
            ET.fromstring(payload)
    if b"PRIVATE FAVORITE" not in private_fixture:
        raise CaptureError("self-test fixture is invalid")
    print("Store screenshot harness self-test: PASS")
    return 0


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(
        description="Capture or validate sanitized GeoJoystick store screenshots"
    )
    subparsers = result.add_subparsers(dest="command", required=True)

    capture = subparsers.add_parser("capture", help="capture screenshots from a real device")
    capture.add_argument("--serial", required=True)
    capture.add_argument("--expected-model", required=True)
    capture.add_argument("--expected-product", required=True)
    capture.add_argument("--expected-device", required=True)
    capture.add_argument("--expected-android", required=True)
    capture.add_argument("--expected-api", required=True)
    capture.add_argument("--output-dir", required=True)
    capture.add_argument(
        "--adb",
        default=os.path.expanduser("~/Android/Sdk/platform-tools/adb"),
    )
    capture.add_argument(
        "--theme",
        choices=("light", "dark"),
        default="light",
    )
    capture.add_argument(
        "--locales",
        nargs="+",
        choices=tuple(LOCALES),
        default=list(LOCALES),
    )
    capture.set_defaults(func=capture_command)

    validate = subparsers.add_parser("validate", help="validate a captured screenshot tree")
    validate.add_argument("--input-dir", required=True)
    validate.set_defaults(func=validate_command)

    self_test = subparsers.add_parser("self-test", help="run device-free safety tests")
    self_test.set_defaults(func=self_test_command)

    return result


def main() -> int:
    args = parser().parse_args()
    try:
        return int(args.func(args))
    except (CaptureError, impl.QAError, OSError, subprocess.SubprocessError, ET.ParseError) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
