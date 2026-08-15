#!/usr/bin/env python3
"""Safe entrypoint for the GeoJoystick Issue #10 device-QA harness.

ADB's shell command transport reparses command text on the device. Feed run-as
shell fragments over stdin so shell syntax executes only after run-as has switched
to the app uid and package data directory.

For the automated matrix, configure GeoJoystick's own language/appearance
preferences directly while the app is force-stopped. The complete preference
file is already backed up and restored by the implementation. Preference XML is
parsed and rewritten on the host, then written through `run-as <package> tee`
with the XML bytes on stdin. This avoids Android-shell heredoc/temp-file behavior.
"""

from __future__ import annotations

import subprocess
import sys
import xml.etree.ElementTree as ET

import _qa_accessibility_device_impl as impl


PREF_LANGUAGE = "app_language"
PREF_APPEARANCE = "app_appearance"
PREF_WELCOME = "welcome_acknowledged"
PREF_LEGACY_WELCOME = "license_accepted"


class SafeAdb(impl.Adb):
    @staticmethod
    def _script_bytes(script: str) -> bytes:
        return (script.rstrip("\n") + "\n").encode("utf-8")

    @staticmethod
    def _stdout(result: subprocess.CompletedProcess) -> str:
        return (
            result.stdout.decode("utf-8", errors="replace")
            .replace("\r", "")
            .strip()
        )

    def run_as(self, script: str, check: bool = True) -> str:
        result = self.call(
            "shell",
            "run-as",
            self.package,
            "sh",
            input_data=self._script_bytes(script),
            check=check,
        )
        return self._stdout(result)

    def run_as_probe(self, script: str) -> bool:
        result = self.call(
            "shell",
            "run-as",
            self.package,
            "sh",
            input_data=self._script_bytes(script),
            check=False,
        )
        return result.returncode == 0

    def write_app_file(self, path: str, payload: bytes) -> None:
        if not path or path.startswith("/") or ".." in path.split("/"):
            raise impl.QAError(f"unsafe app-relative write path: {path!r}")
        result = self.call(
            "shell",
            "run-as",
            self.package,
            "tee",
            path,
            input_data=payload,
            check=True,
        )
        if result.stdout != payload:
            raise impl.QAError("app-sandbox preference write verification stream differed")


def _named(root: ET.Element, name: str) -> list[ET.Element]:
    return [child for child in root if child.attrib.get("name") == name]


def _boolean_value(root: ET.Element, name: str) -> bool:
    for child in _named(root, name):
        return (
            child.tag == "boolean"
            and child.attrib.get("value", "").lower() == "true"
        )
    return False


def rewrite_ui_preferences(xml_text: str, language: str, theme: str) -> str:
    if language not in {"en", "de", "system"}:
        raise impl.QAError(f"invalid QA language: {language}")
    if theme not in {"system", "light", "dark"}:
        raise impl.QAError(f"invalid QA theme: {theme}")

    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError as exc:
        raise impl.QAError(f"could not parse GeoJoystick preferences: {exc}") from exc

    if root.tag != "map":
        raise impl.QAError("unexpected GeoJoystick preferences root")

    if not (
        _boolean_value(root, PREF_WELCOME)
        or _boolean_value(root, PREF_LEGACY_WELCOME)
    ):
        raise impl.QAError(
            "first-run onboarding is unacknowledged; refusing to acknowledge it automatically"
        )

    for key in (PREF_LANGUAGE, PREF_APPEARANCE):
        for child in list(root):
            if child.attrib.get("name") == key:
                root.remove(child)

    language_node = ET.Element("string", {"name": PREF_LANGUAGE})
    language_node.text = language
    root.append(language_node)

    appearance_node = ET.Element("string", {"name": PREF_APPEARANCE})
    appearance_node.text = theme
    root.append(appearance_node)

    return ET.tostring(root, encoding="unicode", short_empty_elements=True)


def read_ui_preferences(xml_text: str) -> tuple[str | None, str | None]:
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError as exc:
        raise impl.QAError(f"could not parse GeoJoystick preferences: {exc}") from exc

    def value(name: str) -> str | None:
        matches = [
            child for child in root
            if child.tag == "string" and child.attrib.get("name") == name
        ]
        if len(matches) != 1:
            return None
        return matches[0].text or ""

    return value(PREF_LANGUAGE), value(PREF_APPEARANCE)


class SafeHarness(impl.Harness):
    def _write_ui_preferences(self, language: str, theme: str) -> None:
        self.adb.force_stop()

        if not self.adb.run_as_probe(f"test -f {impl.PREFS_PATH}"):
            raise impl.QAError("GeoJoystick preference file is unavailable")

        before = self.adb.run_as(f"cat {impl.PREFS_PATH}")
        rewritten = rewrite_ui_preferences(before, language, theme)
        payload = (rewritten + "\n").encode("utf-8")
        self.adb.write_app_file(impl.PREFS_PATH, payload)

        after = self.adb.run_as(f"cat {impl.PREFS_PATH}")
        actual_language, actual_theme = read_ui_preferences(after)
        if actual_language != language or actual_theme != theme:
            raise impl.QAError(
                "GeoJoystick UI preferences did not persist requested QA values"
            )

    def _verify_rendered_ui_configuration(self, language: str, theme: str) -> None:
        settings_desc = "Einstellungen" if language == "de" else "Settings"
        settings = self.find_node(
            lambda node: node.desc == settings_desc,
            attempts=5,
        )
        if settings is None:
            raise impl.QAError(
                f"app did not render requested language {language!r} on the home screen"
            )
        self.adb.tap(settings.bounds)

        expected_language = (
            "Sprache. Deutsch" if language == "de" else "Language. English"
        )
        language_row = self.find_node(
            lambda node: node.desc == expected_language,
            scroll="up",
            attempts=8,
        )
        if language_row is None:
            raise impl.QAError(
                f"settings did not expose requested language state: {expected_language!r}"
            )

        theme_value = {
            ("en", "system"): "System",
            ("en", "light"): "Light",
            ("en", "dark"): "Dark",
            ("de", "system"): "System",
            ("de", "light"): "Hell",
            ("de", "dark"): "Dunkel",
        }[(language, theme)]
        theme_title = "Darstellung" if language == "de" else "Theme"
        expected_theme = f"{theme_title}. {theme_value}"
        theme_row = self.find_node(
            lambda node: node.desc == expected_theme,
            scroll="up",
            attempts=8,
        )
        if theme_row is None:
            raise impl.QAError(
                f"settings did not expose requested theme state: {expected_theme!r}"
            )

    def configure_app(self, language: str, theme: str) -> None:
        self._write_ui_preferences(language, theme)
        self.adb.launch()
        self._verify_rendered_ui_configuration(language, theme)
        self.adb.force_stop()
        self.adb.launch()


def adapter_self_test() -> None:
    captured: list[tuple[tuple[str, ...], bytes | None, bool]] = []

    class ProbeAdb(SafeAdb):
        def call(
            self,
            *args: str,
            timeout: float = 20.0,
            check: bool = True,
            input_data: bytes | None = None,
        ) -> subprocess.CompletedProcess:
            captured.append((args, input_data, check))
            stdout = input_data if args[-2:-1] == ("tee",) else b"ok\r\n"
            return subprocess.CompletedProcess(args, 0, stdout or b"", b"")

    probe = ProbeAdb("adb", "synthetic-serial", "com.example.synthetic")
    output = probe.run_as("printf '%s' test > cache/state.txt")
    assert output == "ok"
    assert captured[-1] == (
        ("shell", "run-as", "com.example.synthetic", "sh"),
        b"printf '%s' test > cache/state.txt\n",
        True,
    )
    assert probe.run_as_probe("test -e cache/state.txt")
    assert captured[-1] == (
        ("shell", "run-as", "com.example.synthetic", "sh"),
        b"test -e cache/state.txt\n",
        False,
    )

    xml_payload = b"<map><boolean name=\"welcome_acknowledged\" value=\"true\" /></map>\n"
    probe.write_app_file("shared_prefs/geojoystick.xml", xml_payload)
    assert captured[-1] == (
        (
            "shell",
            "run-as",
            "com.example.synthetic",
            "tee",
            "shared_prefs/geojoystick.xml",
        ),
        xml_payload,
        True,
    )

    sample = (
        '<map>'
        '<boolean name="welcome_acknowledged" value="true" />'
        '<string name="app_language">de</string>'
        '<string name="app_appearance">light</string>'
        '<int name="overlay_size_percent" value="80" />'
        '</map>'
    )
    rewritten = rewrite_ui_preferences(sample, "en", "dark")
    language, theme = read_ui_preferences(rewritten)
    assert language == "en"
    assert theme == "dark"
    rewritten_root = ET.fromstring(rewritten)
    assert len(_named(rewritten_root, PREF_LANGUAGE)) == 1
    assert len(_named(rewritten_root, PREF_APPEARANCE)) == 1
    assert any(
        child.tag == "int"
        and child.attrib.get("name") == "overlay_size_percent"
        and child.attrib.get("value") == "80"
        for child in rewritten_root
    )

    unacknowledged = '<map><string name="app_language">en</string></map>'
    try:
        rewrite_ui_preferences(unacknowledged, "de", "dark")
    except impl.QAError:
        pass
    else:
        raise AssertionError("unacknowledged onboarding must be rejected")

    print("GeoJoystick Issue #10 safe-adapter self-test: PASS")


impl.Adb = SafeAdb
impl.Harness = SafeHarness


if __name__ == "__main__":
    if "--self-test" in sys.argv:
        adapter_self_test()
    raise SystemExit(impl.main())
