#!/usr/bin/env python3
"""Validate GeoJoystick's complete, explicit application locale catalogs."""

from __future__ import annotations

import re
import shutil
import sys
import tempfile
import unicodedata
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
LOCALE_DIRECTORIES = {
    "de": "de", "fr": "fr", "es": "es", "it": "it", "nl": "nl",
    "da": "da", "sv": "sv", "nb": "nb", "pl": "pl", "tr": "tr",
    "uk": "uk", "ru": "ru", "ko": "ko",
    "zh-CN": "zh-rCN", "zh-TW": "zh-rTW", "ar": "ar",
}
LOCALES = tuple(LOCALE_DIRECTORIES)
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
    "language_polish": "Polski",
    "language_turkish": "Türkçe",
    "language_ukrainian": "Українська",
    "language_russian": "Русский",
    "language_korean": "한국어",
    "language_chinese_simplified": "简体中文",
    "language_chinese_traditional": "繁體中文",
    "language_arabic": "العربية",
}
FORMATTED_RESOURCES = {
    "ui_116", "ui_120", "ui_125", "ui_129", "ui_131", "ui_132", "ui_133", "ui_189",
}
EXPLICIT_NEWLINE_RESOURCES = {"ui_160"}
SPDX_LICENSE_SUMMARY_RESOURCES = {"ui_022", "ui_060", "ui_061", "ui_069", "ui_088"}
FORMAT = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z%]")
ZERO_WIDTH_SPACE = "\u200b"
LTR_BIDI_CONTROLS = frozenset(
    "\u200e\u200f\u202a\u202b\u202c\u202d\u202e\u2066\u2067\u2068\u2069"
)
PHASE2_REQUIRED_CHARACTERS = {
    "pl": frozenset("ąćęłńóśźż"),
    "tr": frozenset("çğıİöşü"),
}
PHASE3_REQUIRED_CHARACTERS = {
    "uk": frozenset("іїєґ"),
    "ru": frozenset("ыэёъ"),
}
PHASE4_REQUIRED_CHARACTERS = {
    "ko": frozenset("한국어설정저장"),
    "zh-CN": frozenset("简体设置保存导"),
    "zh-TW": frozenset("繁體設定儲存匯"),
}
PHASE5_REQUIRED_CHARACTERS = {
    "ar": frozenset("العربيةإعدادحفظموقع"),
}
PHASE2_MUST_TRANSLATE = frozenset({
    "ui_026",  # Settings
    "ui_045",  # Theme
    "ui_046",  # Language
    "ui_100",  # Cancel
    "ui_111",  # Save
    "ui_164",  # Cancel map selection
    "ui_165",  # Choose location
    "ui_178",  # Move overlay
    "joystick_accessibility",
})


def catalog(path: Path) -> dict[str, str]:
    raw = path.read_text(encoding="utf-8")
    if ZERO_WIDTH_SPACE in raw:
        raise AssertionError(f"zero width space in {path}")
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
        if name in EXPLICIT_NEWLINE_RESOURCES:
            if "\n" in value or "\r" in value:
                raise AssertionError(
                    f"literal XML line break for {name} in {path}; use explicit \\n escapes"
                )
            if "\\n" not in value:
                raise AssertionError(
                    f"missing explicit \\n escape for multiline resource {name} in {path}"
                )
        if re.search(r"(?<!\\)'", value):
            raise AssertionError(
                f"unescaped apostrophe for {name} in {path}"
            )
        if unicodedata.normalize("NFC", value) != value:
            raise AssertionError(f"non-NFC Unicode text for {name} in {path}")
        if not name or name in result:
            raise AssertionError(f"missing or duplicate resource name in {path}")
        if not value.strip():
            raise AssertionError(f"empty translation for {name} in {path}")
        result[name] = value
    return result


def locale_directories(res_root: Path) -> set[str]:
    found: set[str] = set()
    for directory in res_root.iterdir():
        if not directory.is_dir():
            continue
        match = re.fullmatch(
            r"values-([a-z]{2})(?:-r([A-Z]{2}))?",
            directory.name,
        )
        if not match:
            continue
        language, region = match.groups()
        found.add(f"{language}-{region}" if region else language)
    return found


def locale_catalog_path(res_root: Path, locale: str) -> Path:
    qualifier = LOCALE_DIRECTORIES[locale]
    return res_root / f"values-{qualifier}/strings.xml"


def validate_phase2_latin_quality(
    locale: str,
    target: dict[str, str],
    english: dict[str, str],
) -> None:
    required = PHASE2_REQUIRED_CHARACTERS.get(locale)
    if required is None:
        return

    corpus = "".join(target.values())
    missing = sorted(required - set(corpus))
    if missing:
        raise AssertionError(
            f"{locale} catalog is missing expected native characters: "
            + "".join(missing)
        )

    unexpected_controls = sorted(set(corpus) & LTR_BIDI_CONTROLS)
    if unexpected_controls:
        encoded = ", ".join(
            f"U+{ord(character):04X}"
            for character in unexpected_controls
        )
        raise AssertionError(
            f"{locale} catalog contains unexpected bidi controls: {encoded}"
        )

    for name in PHASE2_MUST_TRANSLATE:
        if target.get(name) == english.get(name):
            raise AssertionError(
                f"{locale}/{name} still matches the English source"
            )



def validate_phase3_cyrillic_quality(
    locale: str,
    target: dict[str, str],
    english: dict[str, str],
) -> None:
    required = PHASE3_REQUIRED_CHARACTERS.get(locale)
    if required is None:
        return
    corpus = "".join(target.values())
    missing = sorted(required - set(corpus))
    if missing:
        raise AssertionError(
            f"{locale} catalog is missing expected Cyrillic characters: "
            + "".join(missing)
        )
    unexpected_controls = sorted(set(corpus) & LTR_BIDI_CONTROLS)
    if unexpected_controls:
        encoded = ", ".join(f"U+{ord(c):04X}" for c in unexpected_controls)
        raise AssertionError(
            f"{locale} catalog contains unexpected bidi controls: {encoded}"
        )
    for name in PHASE2_MUST_TRANSLATE:
        if target.get(name) == english.get(name):
            raise AssertionError(
                f"{locale}/{name} still matches the English source"
            )

def validate_phase4_cjk_quality(
    locale: str,
    target: dict[str, str],
    english: dict[str, str],
) -> None:
    required = PHASE4_REQUIRED_CHARACTERS.get(locale)
    if required is None:
        return
    corpus = "".join(target.values())
    missing = sorted(required - set(corpus))
    if missing:
        raise AssertionError(
            f"{locale} catalog is missing expected CJK characters: "
            + "".join(missing)
        )
    unexpected_controls = sorted(set(corpus) & LTR_BIDI_CONTROLS)
    if unexpected_controls:
        encoded = ", ".join(f"U+{ord(c):04X}" for c in unexpected_controls)
        raise AssertionError(
            f"{locale} catalog contains unexpected bidi controls: {encoded}"
        )
    for name in PHASE2_MUST_TRANSLATE:
        if target.get(name) == english.get(name):
            raise AssertionError(
                f"{locale}/{name} still matches the English source"
            )
    if locale == "ko" and re.search(r"[가-힣]", corpus) is None:
        raise AssertionError("ko catalog contains no Hangul syllables")
    if locale.startswith("zh-") and re.search(r"[㐀-鿿]", corpus) is None:
        raise AssertionError(f"{locale} catalog contains no CJK ideographs")



def validate_phase5_arabic_quality(
    locale: str,
    target: dict[str, str],
    english: dict[str, str],
) -> None:
    required = PHASE5_REQUIRED_CHARACTERS.get(locale)
    if required is None:
        return
    corpus = "".join(target.values())
    missing = sorted(required - set(corpus))
    if missing:
        raise AssertionError(
            f"{locale} catalog is missing expected Arabic characters: "
            + "".join(missing)
        )
    unexpected_controls = sorted(set(corpus) & LTR_BIDI_CONTROLS)
    if unexpected_controls:
        encoded = ", ".join(
            f"U+{ord(character):04X}"
            for character in unexpected_controls
        )
        raise AssertionError(
            f"{locale} catalog contains unexpected bidi controls: {encoded}"
        )
    for name in PHASE2_MUST_TRANSLATE:
        if target.get(name) == english.get(name):
            raise AssertionError(
                f"{locale}/{name} still matches the English source"
            )
    if re.search(r"[؀-ۿ]", corpus) is None:
        raise AssertionError("ar catalog contains no Arabic script")


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
        target = catalog(locale_catalog_path(res_root, locale))
        if target == english:
            raise AssertionError(f"{locale} catalog is an exact English copy")
        if set(target) != set(english):
            missing = sorted(set(english) - set(target))
            extra = sorted(set(target) - set(english))
            raise AssertionError(f"{locale} key mismatch: missing={missing}, extra={extra}")
        if not REQUIRED_MAP_KEYS <= set(target):
            raise AssertionError(f"{locale} is missing bundled-map localization keys")
        validate_phase2_latin_quality(locale, target, english)
        validate_phase3_cyrillic_quality(locale, target, english)
        validate_phase4_cjk_quality(locale, target, english)
        validate_phase5_arabic_quality(locale, target, english)
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
        "pl": "LANGUAGE_POLISH", "tr": "LANGUAGE_TURKISH",
        "uk": "LANGUAGE_UKRAINIAN", "ru": "LANGUAGE_RUSSIAN",
        "ko": "LANGUAGE_KOREAN", "zh-CN": "LANGUAGE_CHINESE_SIMPLIFIED",
        "zh-TW": "LANGUAGE_CHINESE_TRADITIONAL",
        "ar": "LANGUAGE_ARABIC",
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

    manifest_source = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
    if 'android:supportsRtl="true"' not in manifest_source:
        raise AssertionError("Android manifest does not enable RTL support")

    for required in ("LANGUAGE_ARABIC", "TextUtils.getLayoutDirectionFromLocale", "boolean isRtl()"):
        if required not in settings_source:
            raise AssertionError(f"GeoSettings Arabic/RTL support is missing {required}")

    rtl_sources = {
        "MainActivity": (ROOT / "app/src/main/java/com/k2040/geojoystick/MainActivity.java").read_text(encoding="utf-8"),
        "MapActivity": (ROOT / "app/src/main/java/com/k2040/geojoystick/MapActivity.java").read_text(encoding="utf-8"),
        "JoystickOverlay": (ROOT / "app/src/main/java/com/k2040/geojoystick/JoystickOverlay.java").read_text(encoding="utf-8"),
    }
    required_contracts = {
        "MainActivity": ("setLayoutDirection(settings.layoutDirection())", "ContextThemeWrapper", "setPaddingRelative", "setMarginEnd", "View.TEXT_DIRECTION_LTR", "forwardChevron()", "backChevron()", "BidiFormatter.getInstance(true).unicodeWrap", "LTR_LICENSE_SECTION", "formatPercentLabel"),
        "MapActivity": ("document.documentElement.dir=localized.direction", "setLayoutDirection(settings.layoutDirection())", "View.TEXT_DIRECTION_LTR", "backChevron()"),
        "JoystickOverlay": ("root.setLayoutDirection(settings.layoutDirection())", "View.TEXT_DIRECTION_LTR", "setMarginStart", "setMarginEnd"),
    }
    for label, required_values in required_contracts.items():
        for required in required_values:
            if required not in rtl_sources[label]:
                raise AssertionError(f"{label} RTL support is missing {required}")

    main_source = rtl_sources["MainActivity"]
    map_source = rtl_sources["MapActivity"]
    if 'return settings.isRtl() ? "‹" : "›";' in main_source \
            or 'return settings.isRtl() ? "›" : "‹";' in main_source:
        raise AssertionError("MainActivity manually double-mirrors chevrons")
    if 'return settings.isRtl() ? "›" : "‹";' in map_source:
        raise AssertionError("MapActivity manually double-mirrors its back chevron")
    for required in ('return "›";', 'return "‹";'):
        if required not in main_source:
            raise AssertionError(
                f"MainActivity automatic chevron contract is missing {required}"
            )
    if 'return "‹";' not in map_source:
        raise AssertionError("MapActivity automatic back-chevron contract is missing")

    map_html = (ROOT / "app/src/main/assets/map.html").read_text(encoding="utf-8")
    for required in ('dir="ltr"', "inset-inline-end", "unicode-bidi: isolate"):
        if required not in map_html:
            raise AssertionError(f"bundled map RTL support is missing {required}")

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
        shutil.copytree(copied / "values-zh-rTW", copied / "values-zh-rHK")
        try:
            validate(copied)
        except AssertionError as exc:
            assert "unexpected locale directories" in str(exc)
        else:
            raise AssertionError("unexpected Chinese region fixture falsely passed")
        shutil.rmtree(copied / "values-zh-rHK")
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
            strings.read_text(encoding="utf-8").replace(
                "GeoJoystick",
                "GeoJoystick's",
                1,
            ),
            encoding="utf-8",
        )
        try:
            validate(copied)
        except AssertionError as exc:
            assert "unescaped apostrophe for" in str(exc)
        else:
            raise AssertionError(
                "unescaped-apostrophe fixture falsely passed"
            )

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

        shutil.rmtree(copied / "values-fr")
        shutil.copytree(RES / "values-fr", copied / "values-fr")
        strings = copied / "values-fr/strings.xml"
        strings.write_text(
            strings.read_text(encoding="utf-8").replace(
                "GeoJoystick",
                "Ge\u0301oJoystick",
                1,
            ),
            encoding="utf-8",
        )
        try:
            validate(copied)
        except AssertionError as exc:
            assert "non-NFC Unicode text" in str(exc)
        else:
            raise AssertionError("decomposed-Unicode fixture falsely passed")

        quality_source = {
            name: f"English {name}"
            for name in PHASE2_MUST_TRANSLATE
        }
        quality_pl = {
            name: f"Polski {name}"
            for name in PHASE2_MUST_TRANSLATE
        }
        quality_pl["script_probe"] = "ąćęłńóśźż"
        validate_phase2_latin_quality(
            "pl", quality_pl, quality_source
        )

        quality_tr = {
            name: f"Türkçe {name}"
            for name in PHASE2_MUST_TRANSLATE
        }
        quality_tr["script_probe"] = "çğıİöşü"
        validate_phase2_latin_quality(
            "tr", quality_tr, quality_source
        )

        quality_uk = {
            name: f"Український {name}"
            for name in PHASE2_MUST_TRANSLATE
        }
        quality_uk["script_probe"] = "іїєґ"
        validate_phase3_cyrillic_quality(
            "uk", quality_uk, quality_source
        )

        quality_ru = {
            name: f"Русский {name}"
            for name in PHASE2_MUST_TRANSLATE
        }
        quality_ru["script_probe"] = "ыэёъ"
        validate_phase3_cyrillic_quality(
            "ru", quality_ru, quality_source
        )

        quality_ko = {
            name: f"한국어 설정 저장 {name}"
            for name in PHASE2_MUST_TRANSLATE
        }
        quality_ko["script_probe"] = "한국어설정저장"
        validate_phase4_cjk_quality(
            "ko", quality_ko, quality_source
        )

        quality_zh_cn = {
            name: f"简体中文 设置 保存 导入 {name}"
            for name in PHASE2_MUST_TRANSLATE
        }
        quality_zh_cn["script_probe"] = "简体设置保存导"
        validate_phase4_cjk_quality(
            "zh-CN", quality_zh_cn, quality_source
        )

        quality_zh_tw = {
            name: f"繁體中文 設定 儲存 匯入 {name}"
            for name in PHASE2_MUST_TRANSLATE
        }
        quality_zh_tw["script_probe"] = "繁體設定儲存匯"
        validate_phase4_cjk_quality(
            "zh-TW", quality_zh_tw, quality_source
        )

        quality_ar = {
            name: f"العربية إعداد حفظ موقع {name}"
            for name in PHASE2_MUST_TRANSLATE
        }
        quality_ar["script_probe"] = "العربيةإعدادحفظموقع"
        validate_phase5_arabic_quality("ar", quality_ar, quality_source)

        broken_ar = dict(quality_ar)
        broken_ar["ui_026"] = quality_source["ui_026"]
        try:
            validate_phase5_arabic_quality("ar", broken_ar, quality_source)
        except AssertionError as exc:
            assert "still matches the English source" in str(exc)
        else:
            raise AssertionError("untranslated Phase 5 fixture falsely passed")

        broken_ar_control = dict(quality_ar)
        broken_ar_control["ui_026"] += "\u200f"
        try:
            validate_phase5_arabic_quality("ar", broken_ar_control, quality_source)
        except AssertionError as exc:
            assert "unexpected bidi controls" in str(exc)
        else:
            raise AssertionError("Arabic bidi-control fixture falsely passed")

        broken_zh_cn = dict(quality_zh_cn)
        broken_zh_cn["ui_026"] = quality_source["ui_026"]
        try:
            validate_phase4_cjk_quality(
                "zh-CN", broken_zh_cn, quality_source
            )
        except AssertionError as exc:
            assert "still matches the English source" in str(exc)
        else:
            raise AssertionError(
                "untranslated Phase 4 fixture falsely passed"
            )

        broken_tr = dict(quality_tr)
        broken_tr["ui_026"] = quality_source["ui_026"]
        try:
            validate_phase2_latin_quality(
                "tr", broken_tr, quality_source
            )
        except AssertionError as exc:
            assert "still matches the English source" in str(exc)
        else:
            raise AssertionError(
                "untranslated Phase 2 fixture falsely passed"
            )


        broken_uk = dict(quality_uk)
        broken_uk["ui_026"] = quality_source["ui_026"]
        try:
            validate_phase3_cyrillic_quality(
                "uk", broken_uk, quality_source
            )
        except AssertionError as exc:
            assert "still matches the English source" in str(exc)
        else:
            raise AssertionError(
                "untranslated Phase 3 fixture falsely passed"
            )

    print("Localization resource self-test: PASS")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(self_test() if "--self-test" in sys.argv else main())
    except AssertionError as exc:
        print(f"Localization resource test: FAIL: {exc}", file=sys.stderr)
        raise SystemExit(1)
