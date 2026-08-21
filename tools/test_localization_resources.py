#!/usr/bin/env python3
"""Validate GeoJoystick's complete, explicit application locale catalogs."""

from __future__ import annotations

import re
import shutil
import sys
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
LOCALES = ("de", "fr", "es", "it", "nl", "da", "sv", "nb")
REQUIRED_MAP_KEYS = {"map_instruction", "map_zoom_in", "map_zoom_out"}
LANGUAGE_AUTONYMS = {
    "language_english": "English",
    "language_german": "Deutsch",
    "language_french": "Français",
    "language_spanish": "Español",
    "language_italian": "Italiano",
    "language_dutch": "Nederlands",
    "language_danish": "Dansk",
    "language_swedish": "Svenska",
    "language_norwegian_bokmal": "Norsk bokmål",
}
FORMATTED_RESOURCES = {
    "ui_116", "ui_120", "ui_125", "ui_129", "ui_131", "ui_132", "ui_133", "ui_189",
}
SPDX_LICENSE_SUMMARY_RESOURCES = {"ui_022", "ui_060", "ui_061", "ui_069", "ui_088"}
FORMAT = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z%]")
ZERO_WIDTH_SPACE = "\u200b"


def catalog(path: Path) -> dict[str, str]:
    raw = path.read_text(encoding="utf-8")
    if ZERO_WIDTH_SPACE in raw:
        raise AssertionError(f"zero width space in {path}")
    if re.search(r"(?<!\\)'", raw):
        raise AssertionError(f"unescaped apostrophe in {path}")
    try:
        root = ET.parse(path).getroot()
    except (OSError, ET.ParseError) as exc:
        raise AssertionError(f"invalid resource XML {path}: {exc}") from exc
    if root.tag != "resources":
        raise AssertionError(f"unexpected resource root in {path}")
    result: dict[str, str] = {}
    for item in root.findall("string"):
        name = item.attrib.get("name")
        value = item.text or ""
        if not name or name in result:
            raise AssertionError(f"missing or duplicate resource name in {path}")
        if not value.strip():
            raise AssertionError(f"empty translation for {name} in {path}")
        result[name] = value
    return result


def locale_directories(res_root: Path) -> set[str]:
    found: set[str] = set()
    for directory in res_root.iterdir():
        if not directory.is_dir() or not directory.name.startswith("values-"):
            continue
        qualifier = directory.name[len("values-"):].split("-")[0]
        if re.fullmatch(r"[a-z]{2}(?:-r[A-Z]{2})?", qualifier):
            found.add(qualifier[:2])
    return found


def validate(res_root: Path) -> None:
    actual_locales = locale_directories(res_root)
    expected_locales = set(LOCALES)
    if actual_locales != expected_locales:
        raise AssertionError(f"unexpected locale directories: {sorted(actual_locales ^ expected_locales)}")
    english = catalog(res_root / "values/strings.xml")
    for name in SPDX_LICENSE_SUMMARY_RESOURCES:
        if "GPL-3.0-only" not in english.get(name, ""):
            raise AssertionError(f"English {name} is missing GPL-3.0-only")
    for name, autonym in LANGUAGE_AUTONYMS.items():
        if english.get(name) != autonym:
            raise AssertionError(f"English {name} is not the required autonym")
    for name in FORMATTED_RESOURCES:
        placeholders = FORMAT.findall(english.get(name, ""))
        if not placeholders:
            raise AssertionError(f"English {name} must contain a format placeholder")
    if not REQUIRED_MAP_KEYS <= set(english):
        raise AssertionError("English catalog is missing bundled-map localization keys")
    for locale in LOCALES:
        target = catalog(res_root / f"values-{locale}/strings.xml")
        if target == english:
            raise AssertionError(f"{locale} catalog is an exact English copy")
        if set(target) != set(english):
            missing = sorted(set(english) - set(target))
            extra = sorted(set(target) - set(english))
            raise AssertionError(f"{locale} key mismatch: missing={missing}, extra={extra}")
        if not REQUIRED_MAP_KEYS <= set(target):
            raise AssertionError(f"{locale} is missing bundled-map localization keys")
        for name, autonym in LANGUAGE_AUTONYMS.items():
            if target.get(name) != autonym:
                raise AssertionError(f"{locale}/{name} is not the required autonym")
        for name in SPDX_LICENSE_SUMMARY_RESOURCES:
            if "GPL-3.0-only" not in target.get(name, ""):
                raise AssertionError(f"{locale}/{name} is missing GPL-3.0-only")
        for name in FORMATTED_RESOURCES:
            if not target.get(name, "").strip():
                raise AssertionError(f"{locale}/{name} is missing or empty")
        for name, source in english.items():
            expected = FORMAT.findall(source)
            actual = FORMAT.findall(target[name])
            if actual != expected:
                raise AssertionError(
                    f"{locale}/{name} format placeholders differ: {actual!r} != {expected!r}")


def main() -> int:
    validate(RES)
    english = catalog(RES / "values/strings.xml")
    settings_source = (ROOT / "app/src/main/java/com/k2040/geojoystick/GeoSettings.java").read_text(
        encoding="utf-8")
    setting_constants = {
        "de": "LANGUAGE_GERMAN", "fr": "LANGUAGE_FRENCH", "es": "LANGUAGE_SPANISH",
        "it": "LANGUAGE_ITALIAN", "nl": "LANGUAGE_DUTCH", "da": "LANGUAGE_DANISH",
        "sv": "LANGUAGE_SWEDISH", "nb": "LANGUAGE_NORWEGIAN_BOKMAL",
    }
    for locale, constant in setting_constants.items():
        if constant not in settings_source:
            raise AssertionError(f"GeoSettings does not declare explicit {locale} support")
    if "isSupportedLanguage(value) ? value : LANGUAGE_SYSTEM" not in settings_source:
        raise AssertionError("malformed explicit language values do not safely fall back to system mode")
    service_source = (ROOT / "app/src/main/java/com/k2040/geojoystick/MockLocationService.java").read_text(
        encoding="utf-8")
    for required in (
            "registerOnSharedPreferenceChangeListener(preferenceListener)",
            "unregisterOnSharedPreferenceChangeListener(preferenceListener)",
            "refreshLocalizedRuntimeUi",
            "GeoSettings.PREF_LANGUAGE"):
        if required not in service_source:
            raise AssertionError(f"service live-language refresh is missing {required}")
    joystick_source = (ROOT / "app/src/main/java/com/k2040/geojoystick/JoystickView.java").read_text(
        encoding="utf-8")
    if '"Movement joystick"' in joystick_source:
        raise AssertionError("JoystickView still hard-codes English accessibility text")
    print(f"Localization resource test: PASS ({len(english)} keys × {len(LOCALES)} locales)")
    return 0


def self_test() -> int:
    with tempfile.TemporaryDirectory(prefix="geojoystick-locale-test-") as temporary:
        copied = Path(temporary) / "res"
        shutil.copytree(RES, copied)
        shutil.copytree(copied / "values", copied / "values-night")
        validate(copied)
        shutil.copytree(copied / "values", copied / "values-pt")
        try:
            validate(copied)
        except AssertionError as exc:
            assert "unexpected locale directories" in str(exc)
        else:
            raise AssertionError("unexpected locale fixture falsely passed")
        shutil.rmtree(copied / "values-pt")
        shutil.copytree(copied / "values", copied / "values-fr", dirs_exist_ok=True)
        try:
            validate(copied)
        except AssertionError as exc:
            assert "exact English copy" in str(exc)
        else:
            raise AssertionError("English-copy fixture falsely passed")
        strings = copied / "values-fr/strings.xml"
        strings.write_text(
            strings.read_text(encoding="utf-8").replace(
                "GeoJoystick", "Geo\u200bJoystick", 1
            ),
            encoding="utf-8",
        )
        try:
            validate(copied)
        except AssertionError as exc:
            assert "zero width space" in str(exc)
        else:
            raise AssertionError("zero-width-space fixture falsely passed")
        shutil.rmtree(copied / "values-fr")
        shutil.copytree(RES / "values-fr", copied / "values-fr")
        strings = copied / "values-fr/strings.xml"
        strings.write_text(
            strings.read_text(encoding="utf-8").replace("GPL-3.0-only", "GPL-3.0", 1),
            encoding="utf-8",
        )
        try:
            validate(copied)
        except AssertionError as exc:
            assert "missing GPL-3.0-only" in str(exc)
        else:
            raise AssertionError("shortened-GPL fixture falsely passed")
    print("Localization resource self-test: PASS")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(self_test() if "--self-test" in sys.argv else main())
    except AssertionError as exc:
        print(f"Localization resource test: FAIL: {exc}", file=sys.stderr)
        raise SystemExit(1)
