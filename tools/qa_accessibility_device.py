#!/usr/bin/env python3
"""Safe entrypoint for the GeoJoystick Issue #10 device-QA harness.

The matrix keeps private device identity out of Git, preserves/restores the
complete app preference file plus font/display settings, and configures only
GeoJoystick's documented language/appearance keys while the app is stopped.

ADB shell fragments that require shell syntax are fed over stdin after run-as.
Preference XML is rewritten on the host and streamed directly through
`run-as <package> tee`, avoiding remote-shell redirection/temp-file behavior.

UIAutomator reports clipped nodes and modal/background trees in ways that are
not suitable for unrestricted geometric assertions. The adapter therefore:
- keeps 48dp checks to concrete Button/ImageView controls;
- finds disabled Simulation controls when checking reachability;
- limits overlap checks to labeled sibling controls;
- scopes About analysis to the active modal subtree;
- excludes WebView subtrees from native map-chrome geometry checks; and
- treats a non-zero `uiautomator dump` status as recoverable when a non-empty,
  parseable dump file was actually produced, with bounded retries otherwise.
"""

from __future__ import annotations

import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from collections import defaultdict
from types import SimpleNamespace

import _qa_accessibility_device_impl as impl


PREF_LANGUAGE = "app_language"
PREF_APPEARANCE = "app_appearance"
PREF_WELCOME = "welcome_acknowledged"
PREF_LEGACY_WELCOME = "license_accepted"
SUPPORTED_LANGUAGES = set(impl.SUPPORTED_LANGUAGES)


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
            raise impl.QAError(
                "app-sandbox preference write verification stream differed"
            )


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
    if language not in SUPPORTED_LANGUAGES:
        raise impl.QAError(f"invalid QA language: {language}")
    if theme not in {"system", "light", "dark"}:
        raise impl.QAError(f"invalid QA theme: {theme}")

    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError as exc:
        raise impl.QAError(
            f"could not parse GeoJoystick preferences: {exc}"
        ) from exc

    if root.tag != "map":
        raise impl.QAError("unexpected GeoJoystick preferences root")

    if not (
        _boolean_value(root, PREF_WELCOME)
        or _boolean_value(root, PREF_LEGACY_WELCOME)
    ):
        raise impl.QAError(
            "first-run onboarding is unacknowledged; "
            "refusing to acknowledge it automatically"
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
        raise impl.QAError(
            f"could not parse GeoJoystick preferences: {exc}"
        ) from exc

    def value(name: str) -> str | None:
        matches = [
            child
            for child in root
            if child.tag == "string" and child.attrib.get("name") == name
        ]
        if len(matches) != 1:
            return None
        return matches[0].text or ""

    return value(PREF_LANGUAGE), value(PREF_APPEARANCE)


class SafeHarness(impl.Harness):
    def snapshot(self) -> impl.Snapshot:
        last_detail = "uiautomator produced no usable dump"
        xml_text: str | None = None

        try:
            for attempt in range(1, 4):
                self.adb.shell("rm", "-f", impl.UI_DUMP_PATH, check=False)
                result = self.adb.call(
                    "shell",
                    "uiautomator",
                    "dump",
                    impl.UI_DUMP_PATH,
                    timeout=25.0,
                    check=False,
                )

                exists = self.adb.call(
                    "shell",
                    "test",
                    "-s",
                    impl.UI_DUMP_PATH,
                    timeout=10.0,
                    check=False,
                ).returncode == 0

                if exists:
                    candidate = self.adb.shell(
                        "cat",
                        impl.UI_DUMP_PATH,
                        timeout=10.0,
                    )
                    try:
                        impl.parse_xml(candidate)
                    except ET.ParseError as exc:
                        last_detail = (
                            f"attempt {attempt}: dump XML was not parseable: {exc}"
                        )
                    else:
                        xml_text = candidate
                        break
                else:
                    stdout = result.stdout.decode(
                        "utf-8", errors="replace"
                    ).replace("\r", "").strip()
                    stderr = result.stderr.decode(
                        "utf-8", errors="replace"
                    ).replace("\r", "").strip()
                    detail = stderr or stdout or f"exit {result.returncode}"
                    last_detail = (
                        f"attempt {attempt}: no non-empty dump file ({detail})"
                    )

                if attempt < 3:
                    time.sleep(0.25)
        finally:
            self.adb.shell("rm", "-f", impl.UI_DUMP_PATH, check=False)

        if xml_text is None:
            raise impl.QAError(
                "uiautomator dump failed after 3 attempts: " + last_detail
            )

        nodes = impl.parse_xml(xml_text)
        current_density = impl.parse_density(
            self.adb.shell("wm", "density")
        )
        density = current_density[1] or current_density[0]
        return impl.Snapshot(self.width, self.height, density, nodes)

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

    def _verify_rendered_ui_configuration(
        self, language: str, theme: str
    ) -> None:
        expected = impl.scenario_expectations(language, theme)
        settings = self.find_node(
            lambda node: node.desc in expected["settings"],
            attempts=5,
        )
        if settings is None:
            raise impl.QAError(
                f"app did not render requested language {language!r} "
                "on the home screen"
            )
        self.adb.tap(settings.bounds)

        language_row = self.find_node(
            lambda node: any(
                node.desc == f"{title}. {value}"
                for title in expected["language_title"]
                for value in expected["language_value"]),
            scroll="up",
            attempts=8,
        )
        if language_row is None:
            raise impl.QAError(
                "settings did not expose requested language state: "
                f"{expected['language_title']!r}"
            )

        theme_row = self.find_node(
            lambda node: any(
                node.desc == f"{title}. {value}"
                for title in expected["theme_title"]
                for value in expected["theme_value"]),
            scroll="up",
            attempts=8,
        )
        if theme_row is None:
            raise impl.QAError(
                "settings did not expose requested theme state: "
                f"{expected['theme_title']!r}"
            )

    def configure_app(self, language: str, theme: str) -> None:
        self._write_ui_preferences(language, theme)
        self.adb.launch()
        self._verify_rendered_ui_configuration(language, theme)
        self.adb.force_stop()
        self.adb.launch()

    def find_node_any_state(
        self,
        predicate,
        *,
        scroll: str | None = None,
        attempts: int = 7,
    ) -> impl.UiNode | None:
        for _ in range(attempts):
            snap = self.snapshot()
            matches = [
                node
                for node in snap.nodes
                if node.package == self.package and predicate(node)
            ]
            if matches:
                return min(
                    matches,
                    key=lambda node: (node.bounds.top, node.bounds.left),
                )
            if scroll == "up":
                self.adb.swipe_up(snap.width, snap.height)
            elif scroll == "down":
                self.adb.swipe_down(snap.width, snap.height)
            else:
                break
        return None

    @staticmethod
    def _subtree(
        nodes: list[impl.UiNode],
        root: impl.UiNode,
    ) -> list[impl.UiNode]:
        return [
            node
            for node in nodes
            if node.path == root.path or impl.is_ancestor(root.path, node.path)
        ]

    def _active_nodes(
        self,
        snap: impl.Snapshot,
        screen: str,
        language: str,
    ) -> list[impl.UiNode]:
        app_nodes = [
            node
            for node in snap.nodes
            if node.package == self.package and node.bounds.area > 0
        ]

        if screen == "about":
            close = next(
                (
                    node
                    for node in app_nodes
                    if node.desc in impl.modal_expectations(language)["about_close"]
                ),
                None,
            )
            if close is not None:
                ancestors = [
                    node
                    for node in app_nodes
                    if node.clickable and impl.is_ancestor(node.path, close.path)
                ]
                if ancestors:
                    modal = max(ancestors, key=lambda node: len(node.path))
                    return self._subtree(app_nodes, modal)

        if screen == "map":
            web_roots = [
                node
                for node in app_nodes
                if node.class_name == "android.webkit.WebView"
            ]
            if web_roots:
                return [
                    node
                    for node in app_nodes
                    if not any(
                        node.path == web.path
                        or impl.is_ancestor(web.path, node.path)
                        for web in web_roots
                    )
                ]

        return app_nodes

    @staticmethod
    def _touch_target_candidate(node: impl.UiNode) -> bool:
        return (
            node.class_name.endswith("Button")
            or node.class_name.endswith("ImageView")
        )

    def analyze(
        self,
        snap: impl.Snapshot,
        scenario: impl.Scenario,
        screen: str,
    ) -> None:
        app_nodes = self._active_nodes(snap, screen, scenario.language)
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
                    impl.Finding(
                        scenario.key,
                        screen,
                        "bounds",
                        (
                            f"{node.class_name} "
                            f"{node.text or node.desc!r} outside screen: {bounds}"
                        ),
                    )
                )

            if (
                node.clickable
                and (
                    node.class_name.endswith("ImageView")
                    or impl.symbol_only(node.text)
                )
                and not node.desc
            ):
                self.findings.append(
                    impl.Finding(
                        scenario.key,
                        screen,
                        "a11y-label",
                        (
                            f"clickable {node.class_name} {node.text!r} "
                            "has no content description"
                        ),
                    )
                )

            if self._touch_target_candidate(node):
                width_dp = bounds.width / density_scale
                height_dp = bounds.height / density_scale
                if width_dp < 47.0 or height_dp < 47.0:
                    self.findings.append(
                        impl.Finding(
                            scenario.key,
                            screen,
                            "touch-target",
                            (
                                f"{node.class_name} "
                                f"{node.text or node.desc!r} is "
                                f"{width_dp:.1f}×{height_dp:.1f}dp"
                            ),
                        )
                    )

        controls = [
            node
            for node in app_nodes
            if node.clickable
            and node.bounds.area > 0
            and bool(node.text or node.desc)
            and node.class_name != "android.webkit.WebView"
        ]
        for index, first in enumerate(controls):
            for second in controls[index + 1 :]:
                if first.path[:-1] != second.path[:-1]:
                    continue
                if (
                    impl.is_ancestor(first.path, second.path)
                    or impl.is_ancestor(second.path, first.path)
                ):
                    continue
                ratio = impl.overlap_ratio(first.bounds, second.bounds)
                if ratio >= 0.20:
                    self.findings.append(
                        impl.Finding(
                            scenario.key,
                            screen,
                            "overlap",
                            (
                                f"{first.text or first.desc!r} overlaps "
                                f"{second.text or second.desc!r} ({ratio:.0%})"
                            ),
                        )
                    )

    def home_top(self, scenario: impl.Scenario) -> None:
        self.adb.force_stop()
        self.adb.launch()
        snap = self.snapshot()
        self.record_expected(snap, scenario, "main-top", ("GeoJoystick",))
        self.record_expected(
            snap,
            scenario,
            "main-top",
            impl.localized_text(scenario.language, "ui_008"),
        )
        self.analyze(snap, scenario, "main-top")

        self.tap_desc_any(
            impl.localized_text(scenario.language, "ui_008")
        )
        snap = self.snapshot()
        for candidates in (
            impl.localized_text(scenario.language, "ui_028"),
            impl.localized_text(scenario.language, "ui_031"),
            impl.localized_text(scenario.language, "ui_019"),
        ):
            self.record_expected(
                snap, scenario, "main-status", candidates
            )
        self.analyze(snap, scenario, "main-status")

        start = self.find_node_any_state(
            lambda item: item.desc
            in impl.localized_text(scenario.language, "ui_020"),
            scroll="up",
        )
        stop = self.find_node_any_state(
            lambda item: item.desc
            in impl.localized_text(scenario.language, "ui_021"),
            scroll="up",
        )
        if start is None or stop is None:
            self.findings.append(
                impl.Finding(
                    scenario.key,
                    "main-simulation",
                    "missing",
                    "Simulation Start/Stop controls not both reachable",
                )
            )
        else:
            self.analyze(
                self.snapshot(),
                scenario,
                "main-simulation",
            )

    def print_findings(self) -> None:
        if not self.findings:
            return

        grouped: dict[
            tuple[str, str, str], list[str]
        ] = defaultdict(list)
        for finding in self.findings:
            grouped[
                (finding.screen, finding.code, finding.detail)
            ].append(finding.scenario)

        print("\n=== Structural findings (grouped) ===")
        ordered = sorted(
            grouped.items(),
            key=lambda item: (
                item[0][0],
                item[0][1],
                item[0][2],
            ),
        )
        for (screen, code, detail), scenarios in ordered[:120]:
            unique_scenarios = list(dict.fromkeys(scenarios))
            sample = unique_scenarios[0]
            print(
                f"FAIL: [{len(unique_scenarios)} scenario(s); sample {sample}] "
                f"{screen} {code}: {detail}"
            )
        if len(ordered) > 120:
            print(
                f"... {len(ordered) - 120} additional unique "
                "finding signature(s) omitted"
            )
        print(
            f"UNIQUE SIGNATURES: {len(ordered)}; "
            f"RAW FINDINGS: {len(self.findings)}"
        )


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

    xml_payload = (
        b'<map><boolean name="welcome_acknowledged" '
        b'value="true" /></map>\n'
    )
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
        "<map>"
        '<boolean name="welcome_acknowledged" value="true" />'
        '<string name="app_language">de</string>'
        '<string name="app_appearance">light</string>'
        '<int name="overlay_size_percent" value="80" />'
        "</map>"
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
    rewritten_french = rewrite_ui_preferences(sample, "fr", "light")
    assert read_ui_preferences(rewritten_french) == ("fr", "light")
    try:
        rewrite_ui_preferences(sample, "malformed", "light")
    except impl.QAError:
        pass
    else:
        raise AssertionError("unsupported QA language was accepted")

    unacknowledged = '<map><string name="app_language">en</string></map>'
    try:
        rewrite_ui_preferences(unacknowledged, "de", "dark")
    except impl.QAError:
        pass
    else:
        raise AssertionError("unacknowledged onboarding must be rejected")

    args = SimpleNamespace(
        adb="adb",
        serial="synthetic-serial",
        package="com.example.synthetic",
    )
    harness = SafeHarness(args)
    scenario = impl.Scenario("en", "light", 1.0, 480)
    about = impl.UiNode(
        path=(0, 0),
        text="",
        desc="About GeoJoystick",
        class_name="android.widget.ImageView",
        package=args.package,
        clickable=True,
        enabled=True,
        bounds=impl.Bounds(0, 0, 120, 120),
        child_count=0,
    )
    disabled_stop = impl.UiNode(
        path=(0, 1),
        text="■",
        desc="Stop simulation",
        class_name="android.widget.Button",
        package=args.package,
        clickable=False,
        enabled=False,
        bounds=impl.Bounds(150, 0, 294, 144),
        child_count=0,
    )
    harness.analyze(
        impl.Snapshot(1080, 2400, 480, [about, disabled_stop]),
        scenario,
        "main-top",
    )
    assert any(
        finding.code == "touch-target"
        and "About GeoJoystick" in finding.detail
        for finding in harness.findings
    )
    assert not any(
        finding.code == "touch-target"
        and "Stop simulation" in finding.detail
        for finding in harness.findings
    )

    background = impl.UiNode(
        path=(0,),
        text="Background",
        desc="",
        class_name="android.widget.Button",
        package=args.package,
        clickable=True,
        enabled=True,
        bounds=impl.Bounds(0, 0, 1080, 2400),
        child_count=0,
    )
    modal = impl.UiNode(
        path=(1,),
        text="",
        desc="",
        class_name="android.widget.LinearLayout",
        package=args.package,
        clickable=True,
        enabled=True,
        bounds=impl.Bounds(120, 300, 960, 1800),
        child_count=1,
    )
    close = impl.UiNode(
        path=(1, 0),
        text="×",
        desc=impl.modal_expectations("en")["about_close"][0],
        class_name="android.widget.TextView",
        package=args.package,
        clickable=True,
        enabled=True,
        bounds=impl.Bounds(800, 320, 944, 464),
        child_count=0,
    )
    scoped = harness._active_nodes(
        impl.Snapshot(1080, 2400, 480, [background, modal, close]),
        "about",
        "en",
    )
    assert background not in scoped
    assert modal in scoped and close in scoped

    sample_xml = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>'
        '<hierarchy rotation="0">'
        '<node index="0" text="GeoJoystick" '
        'class="android.widget.TextView" '
        'package="com.example.synthetic" enabled="true" '
        'clickable="false" bounds="[0,0][240,80]" />'
        '</hierarchy>'
    )

    class DumpProbeAdb:
        package = "com.example.synthetic"

        def __init__(self) -> None:
            self.removed = False

        def call(
            self,
            *args: str,
            timeout: float = 20.0,
            check: bool = True,
            input_data: bytes | None = None,
        ) -> subprocess.CompletedProcess:
            if args[:2] == ("shell", "uiautomator"):
                return subprocess.CompletedProcess(
                    args,
                    1,
                    b"UI hierchary dumped to: /data/local/tmp/test.xml\n",
                    b"",
                )
            if args[:3] == ("shell", "test", "-s"):
                return subprocess.CompletedProcess(args, 0, b"", b"")
            return subprocess.CompletedProcess(args, 0, b"", b"")

        def shell(
            self,
            *args: str,
            timeout: float = 20.0,
            check: bool = True,
        ) -> str:
            if args and args[0] == "cat":
                return sample_xml
            if args[:2] == ("wm", "density"):
                return "Physical density: 480"
            if args[:2] == ("rm", "-f"):
                self.removed = True
                return ""
            raise AssertionError(f"unexpected shell call: {args}")

    dump_harness = SafeHarness(args)
    dump_harness.adb = DumpProbeAdb()
    dump_harness.width = 1080
    dump_harness.height = 2400
    dump_snapshot = dump_harness.snapshot()
    assert dump_snapshot.density == 480
    assert any(node.text == "GeoJoystick" for node in dump_snapshot.nodes)
    assert dump_harness.adb.removed

    print("GeoJoystick Issue #10 safe-adapter self-test: PASS")


impl.Adb = SafeAdb
impl.Harness = SafeHarness


if __name__ == "__main__":
    if "--self-test" in sys.argv:
        adapter_self_test()
    raise SystemExit(impl.main())
