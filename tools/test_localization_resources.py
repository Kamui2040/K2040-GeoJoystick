#!/usr/bin/env python3
"""Validate GeoJoystick's complete, explicit application locale catalogs."""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
LOCALES = ("de", "fr", "es", "it", "nl", "da", "sv", "nb")
REQUIRED_MAP_KEYS = {"map_instruction", "map_zoom_in", "map_zoom_out"}
FORMAT = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z%]")


def catalog(path: Path) -> dict[str, str]:
    raw = path.read_text(encoding="utf-8")
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


def main() -> int:
    english = catalog(RES / "values/strings.xml")
    if not REQUIRED_MAP_KEYS <= set(english):
        raise AssertionError("English catalog is missing bundled-map localization keys")
    for locale in LOCALES:
        target = catalog(RES / f"values-{locale}/strings.xml")
        if set(target) != set(english):
            missing = sorted(set(english) - set(target))
            extra = sorted(set(target) - set(english))
            raise AssertionError(f"{locale} key mismatch: missing={missing}, extra={extra}")
        if not REQUIRED_MAP_KEYS <= set(target):
            raise AssertionError(f"{locale} is missing bundled-map localization keys")
        for name, source in english.items():
            expected = FORMAT.findall(source)
            actual = FORMAT.findall(target[name])
            if actual != expected:
                raise AssertionError(
                    f"{locale}/{name} format placeholders differ: {actual!r} != {expected!r}")
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
    print(f"Localization resource test: PASS ({len(english)} keys × {len(LOCALES)} locales)")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"Localization resource test: FAIL: {exc}", file=sys.stderr)
        raise SystemExit(1)
