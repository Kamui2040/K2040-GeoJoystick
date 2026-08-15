#!/usr/bin/env python3
"""Safe entrypoint for the GeoJoystick Issue #10 device-QA harness.

ADB's shell command transport reparses command text on the device. Feed run-as
shell fragments over stdin so redirections and other shell syntax execute only
after run-as has switched to the app uid and package data directory.

Android framework dialogs can expose their choice rows under a framework package
instead of the app package. Keep normal navigation package-scoped, but match
language/theme single-choice rows by exact known label and dialog-row shape.
"""

from __future__ import annotations

import subprocess
import sys
import time
from typing import Sequence

import _qa_accessibility_device_impl as impl


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


class SafeHarness(impl.Harness):
    DIALOG_CHOICE_CLASSES = (
        "android.widget.CheckedTextView",
        "android.widget.RadioButton",
    )

    @classmethod
    def _dialog_choice(
        cls,
        nodes: Sequence[impl.UiNode],
        values: Sequence[str],
        app_package: str,
    ) -> impl.UiNode | None:
        matches = [
            node for node in nodes
            if node.enabled
            and node.bounds.area > 0
            and node.text in values
            and (
                node.class_name in cls.DIALOG_CHOICE_CLASSES
                or (
                    node.package != app_package
                    and node.class_name == "android.widget.TextView"
                )
            )
        ]
        if not matches:
            return None
        return min(
            matches,
            key=lambda node: (
                0 if node.class_name in cls.DIALOG_CHOICE_CLASSES else 1,
                node.bounds.top,
                node.bounds.left,
            ),
        )

    def tap_dialog_choice(self, values: Sequence[str]) -> impl.UiNode:
        for _ in range(8):
            node = self._dialog_choice(
                self.snapshot().nodes,
                values,
                self.package,
            )
            if node is not None:
                self.adb.tap(node.bounds)
                return node
            time.sleep(0.20)
        raise impl.QAError(f"could not find dialog option: {tuple(values)}")

    def set_language(self, language: str) -> None:
        self.tap_desc_any(("Language.", "Sprache."), starts=True, scroll="up")
        options = {
            "en": ("English",),
            "de": ("Deutsch",),
            "system": ("System default", "Systemstandard"),
        }[language]
        self.tap_dialog_choice(options)
        time.sleep(0.25)

    def set_theme(self, theme: str) -> None:
        self.tap_desc_any(("Theme.", "Darstellung."), starts=True, scroll="up")
        options = {
            "system": ("System default", "Systemstandard"),
            "light": ("Light", "Hell"),
            "dark": ("Dark", "Dunkel"),
        }[theme]
        self.tap_dialog_choice(options)
        time.sleep(0.25)


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
            return subprocess.CompletedProcess(args, 0, b"ok\r\n", b"")

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

    background = impl.UiNode(
        path=(0,),
        text="English",
        desc="",
        class_name="android.widget.TextView",
        package="com.example.synthetic",
        clickable=False,
        enabled=True,
        bounds=impl.Bounds(0, 0, 120, 48),
        child_count=0,
    )
    framework_text = impl.UiNode(
        path=(1,),
        text="English",
        desc="",
        class_name="android.widget.TextView",
        package="android",
        clickable=False,
        enabled=True,
        bounds=impl.Bounds(20, 80, 260, 140),
        child_count=0,
    )
    checked = impl.UiNode(
        path=(2,),
        text="English",
        desc="",
        class_name="android.widget.CheckedTextView",
        package="android",
        clickable=False,
        enabled=True,
        bounds=impl.Bounds(20, 100, 260, 160),
        child_count=0,
    )
    assert SafeHarness._dialog_choice(
        (background, framework_text, checked),
        ("English",),
        "com.example.synthetic",
    ) == checked
    assert SafeHarness._dialog_choice(
        (background, framework_text),
        ("English",),
        "com.example.synthetic",
    ) == framework_text
    assert SafeHarness._dialog_choice(
        (background,),
        ("English",),
        "com.example.synthetic",
    ) is None

    print("GeoJoystick Issue #10 safe-adapter self-test: PASS")


impl.Adb = SafeAdb
impl.Harness = SafeHarness


if __name__ == "__main__":
    if "--self-test" in sys.argv:
        adapter_self_test()
    raise SystemExit(impl.main())
