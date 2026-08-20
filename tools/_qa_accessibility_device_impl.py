#!/usr/bin/env python3
"""Deterministic accessibility/device-scale QA for GeoJoystick.

This harness is intentionally device-generic. Maintainer/device identity values are
provided at runtime and are never stored in the repository. It uses only synthetic
coordinates, does not change Android's selected mock-location app, does not clear
app data, and restores the app preference file plus system font/density settings.

The harness performs structural/accessibility checks with uiautomator. Human visual
review is still required for subjective contrast, spacing, and typography.
"""

from __future__ import annotations

import argparse
import hashlib
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Sequence


PACKAGE_DEFAULT = "com.k2040.geojoystick"
MAIN_ACTIVITY = ".MainActivity"
MAP_ACTIVITY = ".MapActivity"
SERVICE = ".MockLocationService"
ACTION_STOP = "com.k2040.geojoystick.action.STOP"
PREFS_PATH = "shared_prefs/geojoystick.xml"
BACKUP_PATH = "cache/geojoystick_issue10_prefs_backup.xml"
STATE_PATH = "cache/geojoystick_issue10_restore_state.txt"
UI_DUMP_PATH = "/data/local/tmp/geojoystick_issue10_ui.xml"
SYNTHETIC_COORDS = ("12.345678", "45.678901", "42.0")
ROOT = Path(__file__).resolve().parents[1]
RESOURCE_ROOT = ROOT / "app/src/main/res"
EXPLICIT_LANGUAGES = ("en", "de", "fr", "es", "it", "nl", "da", "sv", "nb")
SUPPORTED_LANGUAGES = ("system",) + EXPLICIT_LANGUAGES


def localized_text(language: str, resource_name: str) -> tuple[str, ...]:
    if language not in SUPPORTED_LANGUAGES:
        raise QAError(f"unsupported QA scenario language: {language}")
    # System mode intentionally accepts any supported rendered catalog; the harness does not
    # assume a device locale. Explicit modes use exactly their requested catalog.
    languages = EXPLICIT_LANGUAGES if language == "system" else (language,)
    values = []
    for current in languages:
        directory = "values" if current == "en" else f"values-{current}"
        root = ET.parse(RESOURCE_ROOT / directory / "strings.xml").getroot()
        value = next((item.text or "" for item in root.findall("string")
                      if item.attrib.get("name") == resource_name), "")
        if value:
            values.append(value)
    return tuple(values)


def scenario_expectations(language: str, theme: str) -> dict[str, tuple[str, ...]]:
    if language not in SUPPORTED_LANGUAGES:
        raise QAError(f"unsupported QA scenario language: {language}")
    if theme not in {"system", "light", "dark"}:
        raise QAError(f"unsupported QA scenario theme: {theme}")
    language_value = {
        "system": "language_system_default", "en": "language_english",
        "de": "language_german", "fr": "language_french", "es": "language_spanish",
        "it": "language_italian", "nl": "language_dutch", "da": "language_danish",
        "sv": "language_swedish", "nb": "language_norwegian_bokmal",
    }[language]
    return {
        "settings": localized_text(language, "ui_026"),
        "language_title": localized_text(language, "ui_046"),
        "language_value": localized_text(language, language_value),
        "theme_title": localized_text(language, "ui_045"),
        "theme_value": localized_text(language, {"system": "ui_096", "light": "ui_097", "dark": "ui_098"}[theme]),
    }


class QAError(RuntimeError):
    pass


@dataclass(frozen=True)
class Bounds:
    left: int
    top: int
    right: int
    bottom: int

    @property
    def width(self) -> int:
        return max(0, self.right - self.left)

    @property
    def height(self) -> int:
        return max(0, self.bottom - self.top)

    @property
    def area(self) -> int:
        return self.width * self.height

    @property
    def center(self) -> tuple[int, int]:
        return ((self.left + self.right) // 2, (self.top + self.bottom) // 2)


@dataclass(frozen=True)
class UiNode:
    path: tuple[int, ...]
    text: str
    desc: str
    class_name: str
    package: str
    clickable: bool
    enabled: bool
    bounds: Bounds
    child_count: int


@dataclass
class Snapshot:
    width: int
    height: int
    density: int
    nodes: list[UiNode]


@dataclass(frozen=True)
class Scenario:
    language: str
    theme: str
    font_scale: float
    density: int

    @property
    def key(self) -> str:
        return (
            f"{self.language}/{self.theme}"
            f"/font={self.font_scale:.2f}/density={self.density}"
        )


@dataclass(frozen=True)
class Finding:
    scenario: str
    screen: str
    code: str
    detail: str


class Adb:
    def __init__(self, adb: str, serial: str, package: str) -> None:
        self.adb = adb
        self.serial = serial
        self.package = package

    def call(
        self,
        *args: str,
        timeout: float = 20.0,
        check: bool = True,
        input_data: bytes | None = None,
    ) -> subprocess.CompletedProcess:
        result = subprocess.run(
            [self.adb, "-s", self.serial, *args],
            input=input_data,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=timeout,
            check=False,
        )
        if check and result.returncode != 0:
            stderr = result.stderr.decode("utf-8", errors="replace").strip()
            stdout = result.stdout.decode("utf-8", errors="replace").strip()
            message = stderr or stdout or f"exit {result.returncode}"
            raise QAError(f"adb {' '.join(args[:3])}: {message}")
        return result

    def text(
        self,
        *args: str,
        timeout: float = 20.0,
        check: bool = True,
    ) -> str:
        result = self.call(*args, timeout=timeout, check=check)
        return result.stdout.decode("utf-8", errors="replace").replace("\r", "").strip()

    def shell(
        self,
        *args: str,
        timeout: float = 20.0,
        check: bool = True,
    ) -> str:
        return self.text("shell", *args, timeout=timeout, check=check)

    def probe_shell(self, *args: str, timeout: float = 20.0) -> bool:
        return self.call("shell", *args, timeout=timeout, check=False).returncode == 0

    def run_as(self, script: str, check: bool = True) -> str:
        return self.shell("run-as", self.package, "sh", "-c", script, check=check)

    def run_as_probe(self, script: str) -> bool:
        return self.probe_shell("run-as", self.package, "sh", "-c", script)

    def force_stop(self) -> None:
        self.shell("am", "force-stop", self.package)

    def launch(self) -> None:
        self.shell(
            "am",
            "start",
            "-W",
            "-n",
            f"{self.package}/{MAIN_ACTIVITY}",
            timeout=30.0,
        )
        time.sleep(0.45)

    def tap(self, bounds: Bounds) -> None:
        x, y = bounds.center
        self.shell("input", "tap", str(x), str(y))
        time.sleep(0.25)

    def swipe_up(self, width: int, height: int) -> None:
        x = width // 2
        self.shell(
            "input", "swipe",
            str(x), str(int(height * 0.78)),
            str(x), str(int(height * 0.30)),
            "280",
        )
        time.sleep(0.20)

    def swipe_down(self, width: int, height: int) -> None:
        x = width // 2
        self.shell(
            "input", "swipe",
            str(x), str(int(height * 0.30)),
            str(x), str(int(height * 0.78)),
            "280",
        )
        time.sleep(0.20)

    def press_back(self) -> None:
        self.shell("input", "keyevent", "KEYCODE_BACK")
        time.sleep(0.25)

    def stop_simulation(self) -> None:
        self.shell(
            "am",
            "startservice",
            "-n",
            f"{self.package}/{SERVICE}",
            "-a",
            ACTION_STOP,
            check=False,
        )
        time.sleep(0.45)


def parse_bounds(raw: str) -> Bounds:
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", raw or "")
    if not match:
        return Bounds(0, 0, 0, 0)
    return Bounds(*(int(value) for value in match.groups()))


def parse_size(raw: str) -> tuple[int, int]:
    candidates = re.findall(r"(?:Physical|Override) size:\s*(\d+)x(\d+)", raw)
    if candidates:
        width, height = candidates[-1]
        return int(width), int(height)
    match = re.search(r"(\d+)x(\d+)", raw)
    if not match:
        raise QAError(f"Could not parse wm size: {raw!r}")
    return int(match.group(1)), int(match.group(2))


def parse_density(raw: str) -> tuple[int, int | None]:
    physical_match = re.search(r"Physical density:\s*(\d+)", raw)
    if not physical_match:
        match = re.search(r"(\d+)", raw)
        if not match:
            raise QAError(f"Could not parse wm density: {raw!r}")
        return int(match.group(1)), None
    physical = int(physical_match.group(1))
    override_match = re.search(r"Override density:\s*(\d+)", raw)
    return physical, int(override_match.group(1)) if override_match else None


def parse_xml(xml_text: str) -> list[UiNode]:
    root = ET.fromstring(xml_text)
    result: list[UiNode] = []

    def visit(element: ET.Element, path: tuple[int, ...]) -> None:
        attrs = element.attrib
        if element.tag == "node":
            result.append(
                UiNode(
                    path=path,
                    text=attrs.get("text", ""),
                    desc=attrs.get("content-desc", ""),
                    class_name=attrs.get("class", ""),
                    package=attrs.get("package", ""),
                    clickable=attrs.get("clickable", "false") == "true",
                    enabled=attrs.get("enabled", "true") == "true",
                    bounds=parse_bounds(attrs.get("bounds", "")),
                    child_count=len(element),
                )
            )
        for index, child in enumerate(element):
            visit(child, path + (index,))

    visit(root, ())
    return result


def is_ancestor(left: tuple[int, ...], right: tuple[int, ...]) -> bool:
    return len(left) < len(right) and right[: len(left)] == left


def overlap_ratio(a: Bounds, b: Bounds) -> float:
    left = max(a.left, b.left)
    top = max(a.top, b.top)
    right = min(a.right, b.right)
    bottom = min(a.bottom, b.bottom)
    area = max(0, right - left) * max(0, bottom - top)
    denominator = min(a.area, b.area)
    return area / denominator if denominator else 0.0


def symbol_only(text: str) -> bool:
    cleaned = "".join(character for character in text if not character.isspace())
    return bool(cleaned) and not any(character.isalnum() for character in cleaned)


def build_scenarios(base_density: int, stress_density: int) -> list[Scenario]:
    scenarios: list[Scenario] = []
    for language in SUPPORTED_LANGUAGES:
        for theme in ("system", "light", "dark"):
            scenarios.append(Scenario(language, theme, 1.00, base_density))
        for theme in ("light", "dark"):
            scenarios.append(Scenario(language, theme, 1.30, base_density))
        scenarios.append(Scenario(language, "system", 2.00, base_density))
        for theme in ("light", "dark"):
            scenarios.append(Scenario(language, theme, 1.00, stress_density))
        scenarios.append(Scenario(language, "dark", 1.30, stress_density))
    return scenarios


class Harness:
    def __init__(self, args: argparse.Namespace) -> None:
        self.args = args
        self.adb = Adb(args.adb, args.serial, args.package)
        self.package = args.package
        self.width = 0
        self.height = 0
        self.physical_density = 0
        self.original_density_override: int | None = None
        self.original_font_scale = ""
        self.restore_prepared = False
        self.findings: list[Finding] = []

    def verify_identity(self) -> None:
        state = self.adb.text("get-state")
        if state != "device":
            raise QAError(f"ADB state is {state!r}, expected 'device'")
        checks = (
            ("model", "ro.product.model", self.args.expect_model),
            ("product", "ro.product.name", self.args.expect_product),
            ("device", "ro.product.device", self.args.expect_device),
            ("API", "ro.build.version.sdk", str(self.args.expect_api)),
        )
        for label, prop, expected in checks:
            actual = self.adb.shell("getprop", prop)
            if actual != expected:
                raise QAError(f"{label} mismatch: expected {expected!r}, got {actual!r}")
        print("PASS: requested QA device identity matched")

    def verify_package(self) -> None:
        package_dump = self.adb.shell("dumpsys", "package", self.package)
        if f"Package [{self.package}]" not in package_dump:
            raise QAError(f"{self.package} is not installed")
        version_name = re.search(r"^\s*versionName=(.+)$", package_dump, re.MULTILINE)
        version_code = re.search(r"^\s*versionCode=(\d+)", package_dump, re.MULTILINE)
        if self.args.expect_version_name and (
            not version_name or version_name.group(1).strip() != self.args.expect_version_name
        ):
            raise QAError("installed versionName does not match expected QA version")
        if self.args.expect_version_code is not None and (
            not version_code or int(version_code.group(1)) != self.args.expect_version_code
        ):
            raise QAError("installed versionCode does not match expected QA version")

        if self.args.expect_apk_sha256:
            paths = [
                line.removeprefix("package:")
                for line in self.adb.shell("pm", "path", self.package).splitlines()
                if line.startswith("package:")
            ]
            if not paths:
                raise QAError("could not locate installed base APK")
            raw = self.adb.call(
                "exec-out", "cat", paths[0], timeout=45.0
            ).stdout
            digest = hashlib.sha256(raw).hexdigest()
            if digest.lower() != self.args.expect_apk_sha256.lower():
                raise QAError(f"installed APK SHA-256 mismatch: {digest}")
            print(f"PASS: installed APK SHA-256 {digest}")
        else:
            print("PASS: package/version identity verified")

    def verify_inactive(self) -> None:
        services = self.adb.shell(
            "dumpsys", "activity", "services", self.package, check=False
        )
        if "MockLocationService" in services:
            raise QAError(
                "simulation service is active; stop it through GeoJoystick before QA"
            )
        print("PASS: simulation inactive before QA")

    def verify_run_as(self) -> None:
        if not self.adb.run_as_probe("true"):
            raise QAError(
                "run-as is unavailable; refusing to mutate QA settings because "
                "app preferences could not be restored safely"
            )

    def snapshot(self) -> Snapshot:
        try:
            self.adb.shell("uiautomator", "dump", UI_DUMP_PATH, timeout=25.0)
            xml_text = self.adb.shell("cat", UI_DUMP_PATH, timeout=10.0)
        finally:
            self.adb.shell("rm", "-f", UI_DUMP_PATH, check=False)
        nodes = parse_xml(xml_text)
        current_density = parse_density(self.adb.shell("wm", "density"))
        density = current_density[1] or current_density[0]
        return Snapshot(self.width, self.height, density, nodes)

    def prepare_restore_state(self) -> None:
        stale_state = self.adb.run_as_probe(f"test -e {STATE_PATH}")
        stale_backup = self.adb.run_as_probe(f"test -e {BACKUP_PATH}")
        if stale_state or stale_backup:
            raise QAError(
                "stale Issue #10 restore state exists; rerun with --recover-only "
                "before starting a new QA matrix"
            )

        self.original_font_scale = self.adb.shell(
            "settings", "get", "system", "font_scale"
        )
        self.physical_density, self.original_density_override = parse_density(
            self.adb.shell("wm", "density")
        )
        self.width, self.height = parse_size(self.adb.shell("wm", "size"))
        prefs_exists = self.adb.run_as_probe(f"test -e {PREFS_PATH}")
        if prefs_exists:
            self.adb.run_as(f"cp {PREFS_PATH} {BACKUP_PATH}")
        state_lines = [
            f"font_scale={self.original_font_scale}",
            f"physical_density={self.physical_density}",
            (
                f"density_override={self.original_density_override}"
                if self.original_density_override is not None
                else "density_override=none"
            ),
            f"prefs_present={'yes' if prefs_exists else 'no'}",
        ]
        payload = "\n".join(state_lines) + "\n"
        escaped = payload.replace("'", "'\\''")
        self.adb.run_as(f"printf '%s' '{escaped}' > {STATE_PATH}")
        self.restore_prepared = True
        print("PASS: reversible QA state captured inside app cache")

    def recover(self, launch_after: bool = True) -> None:
        if not self.adb.run_as_probe(f"test -e {STATE_PATH}"):
            if self.restore_prepared:
                raise QAError("restore state disappeared during QA")
            return

        raw = self.adb.run_as(f"cat {STATE_PATH}")
        state: dict[str, str] = {}
        for line in raw.splitlines():
            if "=" in line:
                key, value = line.split("=", 1)
                state[key.strip()] = value.strip()

        self.adb.stop_simulation()
        self.adb.force_stop()

        if state.get("prefs_present") == "yes":
            if not self.adb.run_as_probe(f"test -e {BACKUP_PATH}"):
                raise QAError("pre-QA preferences backup is missing")
            self.adb.run_as(f"cp {BACKUP_PATH} {PREFS_PATH}")
        else:
            self.adb.run_as(f"rm -f {PREFS_PATH}")

        font_scale = state.get("font_scale")
        if not font_scale:
            raise QAError("restore state lacks font_scale")
        self.adb.shell("settings", "put", "system", "font_scale", font_scale)

        override = state.get("density_override")
        if override == "none":
            self.adb.shell("wm", "density", "reset")
        elif override and override.isdigit():
            self.adb.shell("wm", "density", override)
        else:
            raise QAError("restore state has invalid density_override")

        self.adb.run_as(f"rm -f {BACKUP_PATH} {STATE_PATH}")
        self.restore_prepared = False
        time.sleep(0.5)
        if launch_after:
            self.adb.launch()
        print("PASS: original app preferences, font scale, and density restored")

    def apply_system_scale(self, font_scale: float, density: int) -> None:
        self.adb.shell(
            "settings", "put", "system", "font_scale", f"{font_scale:.2f}"
        )
        self.adb.shell("wm", "density", str(density))
        time.sleep(0.65)

    def find_node(
        self,
        predicate: Callable[[UiNode], bool],
        *,
        scroll: str | None = None,
        attempts: int = 7,
    ) -> UiNode | None:
        for _ in range(attempts):
            snap = self.snapshot()
            matches = [
                node for node in snap.nodes
                if node.package == self.package and node.enabled and predicate(node)
            ]
            if matches:
                return min(matches, key=lambda node: (node.bounds.top, node.bounds.left))
            if scroll == "up":
                self.adb.swipe_up(snap.width, snap.height)
            elif scroll == "down":
                self.adb.swipe_down(snap.width, snap.height)
            else:
                break
        return None

    def tap_desc_any(
        self,
        values: Sequence[str],
        *,
        starts: bool = False,
        scroll: str | None = None,
    ) -> UiNode:
        node = self.find_node(
            lambda item: any(
                item.desc.startswith(value) if starts else item.desc == value
                for value in values
            ),
            scroll=scroll,
        )
        if node is None:
            raise QAError(f"could not find content description: {values}")
        self.adb.tap(node.bounds)
        return node

    def tap_text_any(
        self,
        values: Sequence[str],
        *,
        contains: bool = False,
        scroll: str | None = None,
    ) -> UiNode:
        node = self.find_node(
            lambda item: any(
                value in item.text if contains else item.text == value
                for value in values
            ),
            scroll=scroll,
        )
        if node is None:
            raise QAError(f"could not find text: {values}")
        self.adb.tap(node.bounds)
        return node

    def set_language(self, language: str) -> None:
        expected = scenario_expectations(language, "system")
        self.tap_desc_any(expected["language_title"], starts=True, scroll="up")
        self.tap_text_any(expected["language_value"])
        time.sleep(0.25)

    def set_theme(self, language: str, theme: str) -> None:
        expected = scenario_expectations(language, theme)
        self.tap_desc_any(expected["theme_title"], starts=True, scroll="up")
        self.tap_text_any(expected["theme_value"])
        time.sleep(0.25)

    def configure_app(self, language: str, theme: str) -> None:
        self.adb.force_stop()
        self.adb.launch()
        if self.find_node(lambda item: item.text in localized_text(language, "ui_062")):
            raise QAError(
                "first-run onboarding is unacknowledged; refusing to acknowledge it automatically"
            )
        self.tap_desc_any(localized_text(language, "ui_026"))
        self.set_language(language)
        self.set_theme(language, theme)
        self.adb.force_stop()
        self.adb.launch()

    def record_expected(
        self,
        snap: Snapshot,
        scenario: Scenario,
        screen: str,
        candidates: Sequence[str],
    ) -> None:
        for candidate in candidates:
            if any(
                candidate == node.text or candidate == node.desc
                or candidate in node.text or candidate in node.desc
                for node in snap.nodes
                if node.package == self.package
            ):
                return
        self.findings.append(
            Finding(scenario.key, screen, "missing", f"expected one of {candidates}")
        )

    def analyze(self, snap: Snapshot, scenario: Scenario, screen: str) -> None:
        app_nodes = [
            node for node in snap.nodes
            if node.package == self.package and node.bounds.area > 0
        ]
        density_scale = snap.density / 160.0

        for node in app_nodes:
            bounds = node.bounds
            if (
                bounds.left < 0
                or bounds.top < 0
                or bounds.right > snap.width
                or bounds.bottom > snap.height
            ):
                self.findings.append(
                    Finding(
                        scenario.key,
                        screen,
                        "bounds",
                        f"{node.class_name} {node.text or node.desc!r} outside screen: {bounds}",
                    )
                )

            if not node.clickable:
                continue

            if (
                node.class_name.endswith("ImageView") or symbol_only(node.text)
            ) and not node.desc:
                self.findings.append(
                    Finding(
                        scenario.key,
                        screen,
                        "a11y-label",
                        f"clickable {node.class_name} {node.text!r} has no content description",
                    )
                )

            explicit_control = (
                node.class_name.endswith("Button")
                or node.class_name.endswith("ImageView")
                or bool(node.desc)
            )
            if explicit_control:
                width_dp = bounds.width / density_scale
                height_dp = bounds.height / density_scale
                if width_dp < 47.0 or height_dp < 47.0:
                    self.findings.append(
                        Finding(
                            scenario.key,
                            screen,
                            "touch-target",
                            (
                                f"{node.class_name} {node.text or node.desc!r} "
                                f"is {width_dp:.1f}×{height_dp:.1f}dp"
                            ),
                        )
                    )

        controls = [
            node for node in app_nodes
            if node.clickable and node.enabled and node.bounds.area > 0
        ]
        for index, first in enumerate(controls):
            for second in controls[index + 1 :]:
                if is_ancestor(first.path, second.path) or is_ancestor(second.path, first.path):
                    continue
                ratio = overlap_ratio(first.bounds, second.bounds)
                if ratio >= 0.20:
                    self.findings.append(
                        Finding(
                            scenario.key,
                            screen,
                            "overlap",
                            (
                                f"{first.text or first.desc!r} overlaps "
                                f"{second.text or second.desc!r} ({ratio:.0%})"
                            ),
                        )
                    )

    def home_top(self, scenario: Scenario) -> None:
        self.adb.force_stop()
        self.adb.launch()
        snap = self.snapshot()
        self.record_expected(snap, scenario, "main-top", ("GeoJoystick",))
        self.record_expected(
            snap,
            scenario,
            "main-top",
            localized_text(scenario.language, "ui_008"),
        )
        self.analyze(snap, scenario, "main-top")

        self.tap_desc_any(
            localized_text(scenario.language, "ui_008")
        )
        snap = self.snapshot()
        for candidates in (
            localized_text(scenario.language, "ui_028"),
            localized_text(scenario.language, "ui_031"),
            localized_text(scenario.language, "ui_019"),
        ):
            self.record_expected(snap, scenario, "main-status", candidates)
        self.analyze(snap, scenario, "main-status")

        start = self.find_node(
            lambda item: item.desc in localized_text(scenario.language, "ui_020"),
            scroll="up",
        )
        stop = self.find_node(
            lambda item: item.desc in localized_text(scenario.language, "ui_021"),
            scroll="up",
        )
        if start is None or stop is None:
            self.findings.append(
                Finding(
                    scenario.key,
                    "main-simulation",
                    "missing",
                    "Simulation Start/Stop controls not both reachable",
                )
            )
        else:
            self.analyze(self.snapshot(), scenario, "main-simulation")

    def settings_screens(self, scenario: Scenario) -> None:
        language = scenario.language
        self.adb.force_stop()
        self.adb.launch()
        self.tap_desc_any(scenario_expectations(language, scenario.theme)["settings"])
        snap = self.snapshot()
        self.record_expected(snap, scenario, "settings-top", localized_text(scenario.language, "ui_026"))
        for candidates in (
            localized_text(scenario.language, "ui_028"),
            localized_text(scenario.language, "ui_031"),
        ):
            self.record_expected(snap, scenario, "settings-top", candidates)
        self.analyze(snap, scenario, "settings-top")

        theme = self.find_node(
            lambda item: item.desc.startswith(localized_text(scenario.language, "ui_045")),
            scroll="up",
        )
        language = self.find_node(
            lambda item: item.desc.startswith(localized_text(scenario.language, "ui_046")),
            scroll="up",
        )
        if theme is None or language is None:
            self.findings.append(
                Finding(
                    scenario.key,
                    "settings-bottom",
                    "missing",
                    "Theme/Language rows are not reachable",
                )
            )
        else:
            self.analyze(self.snapshot(), scenario, "settings-bottom")

    def about_screen(self, scenario: Scenario) -> None:
        self.adb.force_stop()
        self.adb.launch()
        self.tap_desc_any(localized_text(scenario.language, "ui_023"))
        snap = self.snapshot()
        for candidates in (
            localized_text(scenario.language, "ui_048"),
            localized_text(scenario.language, "ui_051"),
            localized_text(scenario.language, "ui_052"),
            localized_text(scenario.language, "ui_053"),
        ):
            self.record_expected(snap, scenario, "about", candidates)
        self.analyze(snap, scenario, "about")

    def map_screen(self, scenario: Scenario) -> None:
        self.adb.force_stop()
        self.adb.launch()
        self.tap_text_any(localized_text(scenario.language, "ui_014"), contains=True)
        time.sleep(0.55)
        snap = self.snapshot()
        for candidates in (
            localized_text(scenario.language, "ui_165"),
            localized_text(scenario.language, "ui_166"),
            localized_text(scenario.language, "ui_164"),
        ):
            self.record_expected(snap, scenario, "map", candidates)
        self.analyze(snap, scenario, "map")
        self.tap_desc_any(localized_text(scenario.language, "ui_164"))

    def run_scenario(self, scenario: Scenario) -> None:
        before = len(self.findings)
        self.apply_system_scale(scenario.font_scale, scenario.density)
        self.configure_app(scenario.language, scenario.theme)
        self.home_top(scenario)
        self.settings_screens(scenario)
        self.about_screen(scenario)
        self.map_screen(scenario)
        added = len(self.findings) - before
        if added:
            print(f"FAIL: {scenario.key} ({added} structural finding(s))")
        else:
            print(f"PASS: {scenario.key}")

    def fill_synthetic_coordinates(self) -> None:
        self.adb.force_stop()
        self.adb.launch()
        snap = self.snapshot()
        edits = sorted(
            [
                node for node in snap.nodes
                if node.package == self.package
                and node.class_name.endswith("EditText")
                and node.bounds.area > 0
            ],
            key=lambda node: (node.bounds.top, node.bounds.left),
        )
        if len(edits) < 3:
            raise QAError("could not locate the three coordinate fields")

        for index, value in enumerate(SYNTHETIC_COORDS):
            snap = self.snapshot()
            edits = sorted(
                [
                    node for node in snap.nodes
                    if node.package == self.package
                    and node.class_name.endswith("EditText")
                    and node.bounds.area > 0
                ],
                key=lambda node: (node.bounds.top, node.bounds.left),
            )
            if len(edits) < 3:
                raise QAError("coordinate fields disappeared during synthetic input")
            self.adb.tap(edits[index].bounds)
            self.adb.shell("input", "keyevent", "KEYCODE_MOVE_END")
            for _ in range(24):
                self.adb.shell("input", "keyevent", "KEYCODE_DEL")
            if value:
                self.adb.shell("input", "text", value)
            self.adb.press_back()
        time.sleep(0.2)

    def mock_app_selected(self) -> bool:
        output = self.adb.shell(
            "cmd", "appops", "query-op", "android:mock_location", "allow", check=False
        )
        return any(line.strip() == self.package for line in output.splitlines())

    def overlay_permission_granted(self) -> bool:
        output = self.adb.shell(
            "appops", "get", self.package, "SYSTEM_ALERT_WINDOW", check=False
        )
        return bool(re.search(r"SYSTEM_ALERT_WINDOW:\s*allow", output, re.IGNORECASE))

    def notifications_granted(self) -> bool:
        package_dump = self.adb.shell("dumpsys", "package", self.package)
        match = re.search(
            r"android\.permission\.POST_NOTIFICATIONS:\s*granted=(true|false)",
            package_dump,
        )
        return bool(match and match.group(1) == "true")

    def wait_service(self, active: bool, timeout: float = 8.0) -> bool:
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            services = self.adb.shell(
                "dumpsys", "activity", "services", self.package, check=False
            )
            present = "MockLocationService" in services
            if present == active:
                return True
            time.sleep(0.25)
        return False

    def overlay_phase(self, density: int) -> None:
        key = f"overlay/en/dark/font=1.30/density={density}"
        scenario = Scenario("en", "dark", 1.30, density)

        if not self.mock_app_selected():
            self.findings.append(
                Finding(key, "overlay", "precondition", "mock-location app is not selected")
            )
            return
        if not self.overlay_permission_granted():
            self.findings.append(
                Finding(key, "overlay", "precondition", "overlay permission is not granted")
            )
            return
        if self.args.expect_api >= 33 and not self.notifications_granted():
            self.findings.append(
                Finding(key, "overlay", "precondition", "notification permission is not granted")
            )
            return

        self.apply_system_scale(1.30, density)
        self.configure_app("en", "dark")
        self.fill_synthetic_coordinates()

        start = self.find_node(lambda item: item.desc == "Start simulation", scroll="up")
        if start is None:
            self.findings.append(
                Finding(key, "overlay", "missing", "Start simulation control not reachable")
            )
            return

        self.adb.tap(start.bounds)
        if not self.wait_service(True):
            self.findings.append(
                Finding(key, "overlay", "runtime", "simulation service did not become active")
            )
            return

        toggle = self.find_node(
            lambda item: item.desc in (
                "Switch to compact overlay",
                "Switch to expanded overlay",
            ),
            attempts=8,
        )
        if toggle is None:
            self.findings.append(
                Finding(key, "overlay", "missing", "overlay mode control not exposed")
            )
            return

        original_compact = toggle.desc == "Switch to expanded overlay"
        if original_compact:
            self.adb.tap(toggle.bounds)
            time.sleep(0.25)

        snap = self.snapshot()
        expected_expanded = (
            "Move GeoJoystick overlay",
            "Switch to compact overlay",
            "Walk speed",
            "Run speed",
            "Bike speed",
            "Stop GeoJoystick service and close overlay",
        )
        for label in expected_expanded:
            self.record_expected(snap, scenario, "overlay-expanded", (label,))
        self.analyze(snap, scenario, "overlay-expanded")

        toggle = self.find_node(lambda item: item.desc == "Switch to compact overlay")
        if toggle is None:
            self.findings.append(
                Finding(key, "overlay-compact", "missing", "compact-mode control missing")
            )
            return
        self.adb.tap(toggle.bounds)
        time.sleep(0.25)
        snap = self.snapshot()
        self.record_expected(
            snap, scenario, "overlay-compact", ("Switch to expanded overlay",)
        )
        for label in (
            "Move GeoJoystick overlay",
            "Walk speed",
            "Run speed",
            "Bike speed",
            "Stop GeoJoystick service and close overlay",
        ):
            if any(node.desc == label for node in snap.nodes if node.package == self.package):
                self.findings.append(
                    Finding(
                        key,
                        "overlay-compact",
                        "visibility",
                        f"expanded-only control remains visible: {label}",
                    )
                )
        self.analyze(snap, scenario, "overlay-compact")

        if not original_compact:
            toggle = self.find_node(lambda item: item.desc == "Switch to expanded overlay")
            if toggle:
                self.adb.tap(toggle.bounds)

        self.adb.stop_simulation()
        if not self.wait_service(False):
            self.findings.append(
                Finding(key, "overlay", "cleanup", "simulation service did not stop")
            )
        else:
            print("PASS: overlay expanded/compact structural phase")

    def print_findings(self) -> None:
        if not self.findings:
            return
        print("\n=== Structural findings ===")
        for finding in self.findings[:60]:
            print(
                f"FAIL: [{finding.scenario}] {finding.screen} "
                f"{finding.code}: {finding.detail}"
            )
        if len(self.findings) > 60:
            print(f"... {len(self.findings) - 60} additional finding(s) omitted")

    def run(self) -> int:
        self.verify_identity()
        self.verify_package()
        self.verify_run_as()

        if self.args.recover_only:
            self.recover(launch_after=True)
            print("RESULT: recovery completed")
            return 0

        self.verify_inactive()
        self.prepare_restore_state()

        baseline_density = self.original_density_override or self.physical_density
        stress_density = max(
            160,
            int(round((baseline_density * self.args.density_scale) / 10.0) * 10),
        )
        scenarios = build_scenarios(baseline_density, stress_density)

        print(
            "MATRIX: "
            f"{len(scenarios)} scenarios; baseline density={baseline_density}; "
            f"stress density={stress_density}"
        )

        primary_error: BaseException | None = None
        try:
            for scenario in scenarios:
                self.run_scenario(scenario)
            self.overlay_phase(stress_density)
        except BaseException as exc:
            primary_error = exc
        finally:
            try:
                self.recover(launch_after=True)
            except BaseException as recovery_error:
                if primary_error is None:
                    primary_error = recovery_error
                else:
                    print(f"RECOVERY FAILURE: {recovery_error}", file=sys.stderr)

        if primary_error is not None:
            raise primary_error

        self.print_findings()
        if self.findings:
            print(
                "\nRESULT: automated Issue #10 structural QA found "
                f"{len(self.findings)} issue(s); human visual acceptance not started"
            )
            return 2

        print("\n=== Result ===")
        print("PASS: automated Issue #10 structural/device-scale matrix")
        print("PASS: original app preferences/font scale/density restored")
        print("PASS: simulation inactive after QA")
        print("SCREENSHOTS: none created")
        print("ACTIONS: not queried or used")
        print(
            "NEXT: human visual checkpoints for subjective contrast, spacing, "
            "and typography"
        )
        return 0


def self_test() -> int:
    sample = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>'
        '<hierarchy rotation="0">'
        '<node index="0" text="" resource-id="" class="android.widget.FrameLayout" '
        'package="com.k2040.geojoystick" content-desc="" checkable="false" '
        'checked="false" clickable="false" enabled="true" focusable="false" '
        'focused="false" scrollable="false" long-clickable="false" password="false" '
        'selected="false" bounds="[0,0][1080,2400]">'
        '<node index="0" text="▶" class="android.widget.Button" '
        'package="com.k2040.geojoystick" content-desc="Start simulation" '
        'clickable="true" enabled="true" bounds="[100,200][244,344]" />'
        '</node></hierarchy>'
    )
    nodes = parse_xml(sample)
    assert len(nodes) == 2
    assert nodes[1].desc == "Start simulation"
    assert nodes[1].bounds.width == 144
    assert parse_size("Physical size: 1080x2400") == (1080, 2400)
    assert parse_density("Physical density: 480\nOverride density: 520") == (480, 520)
    assert parse_density("Physical density: 480") == (480, None)
    assert symbol_only("▶")
    assert not symbol_only("Map")
    assert overlap_ratio(Bounds(0, 0, 100, 100), Bounds(50, 50, 150, 150)) == 0.25
    scenarios = build_scenarios(480, 550)
    assert len(scenarios) == 90
    assert set(EXPLICIT_LANGUAGES) == {"en", "de", "fr", "es", "it", "nl", "da", "sv", "nb"}
    for language in SUPPORTED_LANGUAGES:
        expected = scenario_expectations(language, "dark")
        assert all(expected.values())
        assert localized_text(language, "ui_026")
    try:
        scenario_expectations("malformed", "dark")
    except QAError:
        pass
    else:
        raise AssertionError("unsupported scenario language was accepted")
    print("GeoJoystick Issue #10 QA harness self-test: PASS")
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Automated GeoJoystick accessibility/device-scale QA"
    )
    parser.add_argument("--adb", default="adb", help="adb executable")
    parser.add_argument("--serial", help="exact ADB serial")
    parser.add_argument("--package", default=PACKAGE_DEFAULT)
    parser.add_argument("--expect-model")
    parser.add_argument("--expect-product")
    parser.add_argument("--expect-device")
    parser.add_argument("--expect-api", type=int)
    parser.add_argument("--expect-version-name", default="0.1.3")
    parser.add_argument("--expect-version-code", type=int, default=103)
    parser.add_argument("--expect-apk-sha256")
    parser.add_argument(
        "--density-scale",
        type=float,
        default=1.15,
        help="stress display-density multiplier (default: 1.15)",
    )
    parser.add_argument(
        "--recover-only",
        action="store_true",
        help="restore a stale interrupted QA state and exit",
    )
    parser.add_argument(
        "--self-test",
        action="store_true",
        help="run dependency-free host-side helper tests and exit",
    )
    args = parser.parse_args()
    if args.self_test:
        return args
    required = {
        "--serial": args.serial,
        "--expect-model": args.expect_model,
        "--expect-product": args.expect_product,
        "--expect-device": args.expect_device,
        "--expect-api": args.expect_api,
    }
    missing = [name for name, value in required.items() if value in (None, "")]
    if missing:
        parser.error("missing required runtime identity arguments: " + ", ".join(missing))
    if not (1.0 < args.density_scale <= 1.50):
        parser.error("--density-scale must be > 1.0 and <= 1.50")
    return args


def main() -> int:
    args = parse_args()
    if args.self_test:
        return self_test()
    try:
        return Harness(args).run()
    except (QAError, ET.ParseError, subprocess.TimeoutExpired) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
