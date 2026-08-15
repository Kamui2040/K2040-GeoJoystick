#!/usr/bin/env python3
"""Safe entrypoint for the GeoJoystick Issue #10 device-QA harness.

ADB's shell command transport reparses command text on the device. Feed run-as
shell fragments over stdin so redirections and other shell syntax execute only
after run-as has switched to the app uid and package data directory.
"""

from __future__ import annotations

import subprocess
import sys

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


def transport_self_test() -> None:
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
    print("GeoJoystick Issue #10 run-as transport self-test: PASS")


impl.Adb = SafeAdb


if __name__ == "__main__":
    if "--self-test" in sys.argv:
        transport_self_test()
    raise SystemExit(impl.main())
