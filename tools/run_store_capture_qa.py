#!/usr/bin/env python3
"""Build, install, and capture the complete GeoJoystick store screenshot QA set.

This maintainer-only host runner keeps the entire Issue #12 device workflow in one
process: source/toolchain validation, exact debug build, release-helper exclusion,
canonical-device verification, signer-safe replacement install, atomic 10-shot
capture, and final state validation. Device identity is supplied only at runtime.
"""

from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import time

import _qa_accessibility_device_impl as impl
import capture_store_bundle as bundle
import capture_store_overlay as overlay
import capture_store_screenshots as base


ROOT = Path(__file__).resolve().parents[1]
PACKAGE = base.PACKAGE


class StoreCaptureQaError(RuntimeError):
    pass


def run(
    command: list[str],
    *,
    cwd: Path | None = None,
    env: dict[str, str] | None = None,
    capture: bool = False,
    timeout: float | None = None,
) -> subprocess.CompletedProcess[str]:
    result = subprocess.run(
        command,
        cwd=cwd,
        env=env,
        text=True,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.PIPE if capture else None,
        check=False,
        timeout=timeout,
    )
    if result.returncode != 0:
        detail = ""
        if capture:
            detail = (result.stderr or result.stdout or "").strip()
        raise StoreCaptureQaError(
            f"command failed ({result.returncode}): {' '.join(command)}"
            + (f"; {detail}" if detail else "")
        )
    return result


def executable(path: Path, label: str) -> Path:
    if not path.is_file() or not os.access(path, os.X_OK):
        raise StoreCaptureQaError(f"{label} unavailable: {path}")
    return path


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def apksigner_digest(apksigner: Path, apk: Path) -> str:
    result = run(
        [str(apksigner), "verify", "--print-certs", str(apk)],
        capture=True,
    )
    match = re.search(
        r"^Signer #1 certificate SHA-256 digest: (\S+)$",
        result.stdout,
        flags=re.MULTILINE,
    )
    if not match:
        raise StoreCaptureQaError(f"signer digest unavailable for {apk}")
    return match.group(1)


def appop_mode(adb: base.SafeAdb, op: str) -> str:
    text = adb.shell("cmd", "appops", "get", PACKAGE, op, check=False)
    match = re.search(
        rf"^[ \t]*{re.escape(op)}:[ \t]*([^;\s]+)",
        text,
        flags=re.MULTILINE,
    )
    return match.group(1) if match else "unset"


def installed_base_apk(adb: base.SafeAdb) -> str:
    output = adb.shell("pm", "path", PACKAGE, check=False)
    paths = [
        line.removeprefix("package:").strip()
        for line in output.splitlines()
        if line.startswith("package:")
    ]
    if not paths:
        raise StoreCaptureQaError("GeoJoystick is not installed")
    for path in paths:
        if path.endswith("/base.apk"):
            return path
    return paths[0]


def manifest_text(aapt: Path, apk: Path) -> str:
    result = run(
        [str(aapt), "dump", "xmltree", str(apk), "AndroidManifest.xml"],
        capture=True,
    )
    return result.stdout


def verify_badging(aapt: Path, apk: Path) -> None:
    text = run([str(aapt), "dump", "badging", str(apk)], capture=True).stdout
    required = (
        f"package: name='{PACKAGE}'",
        "versionCode='103'",
        "versionName='0.1.3'",
        "targetSdkVersion:'36'",
    )
    missing = [item for item in required if item not in text]
    if missing:
        raise StoreCaptureQaError(f"APK identity mismatch: missing {missing!r}")


def build_environment(args: argparse.Namespace) -> tuple[dict[str, str], Path, Path, Path]:
    java_home = Path(args.java_home).expanduser().resolve()
    sdk = Path(args.sdk).expanduser().resolve()
    tool_cache = Path(args.tool_cache).expanduser().resolve()

    executable(java_home / "bin" / "java", "JDK java")
    if not (sdk / "platforms" / "android-36" / "android.jar").is_file():
        raise StoreCaptureQaError("Android SDK Platform 36 unavailable")

    build_tools = sdk / "build-tools" / args.build_tools
    aapt = executable(build_tools / "aapt", "aapt")
    apksigner = executable(build_tools / "apksigner", "apksigner")

    environment = os.environ.copy()
    environment["JAVA_HOME"] = str(java_home)
    environment["ANDROID_HOME"] = str(sdk)
    environment["ANDROID_SDK_ROOT"] = str(sdk)
    environment["LOCALAPPDATA"] = str(tool_cache)
    environment["PATH"] = str(java_home / "bin") + os.pathsep + environment.get("PATH", "")
    return environment, tool_cache, aapt, apksigner


def release_validation(
    args: argparse.Namespace,
    environment: dict[str, str],
    tool_cache: Path,
    aapt: Path,
) -> Path:
    gradle = executable(
        tool_cache / "K2040" / "GeoJoystick" / "gradle-8.13" / "bin" / "gradle",
        "Gradle 8.13",
    )
    run(
        [
            str(gradle),
            "--no-daemon",
            "--console=plain",
            f"-PgeoBuildToolsVersion={args.build_tools}",
            "testDebugUnitTest",
            "lintRelease",
            "assembleRelease",
        ],
        cwd=ROOT,
        env=environment,
    )

    release_dir = ROOT / "app" / "build" / "outputs" / "apk" / "release"
    release_apks = sorted(release_dir.glob("*.apk"))
    if len(release_apks) != 1:
        raise StoreCaptureQaError(
            f"expected exactly one release APK, found {len(release_apks)}"
        )
    release_apk = release_apks[0]

    if "NeutralCaptureActivity" in manifest_text(aapt, release_apk):
        raise StoreCaptureQaError("debug capture helper leaked into release APK")
    return release_apk


def ensure_preinstall_inactive(adb: base.SafeAdb) -> None:
    if not base.simulation_active(adb):
        print("PASS: simulation inactive")
        return

    # A previous failed overlay capture may have retained a synthetic service.
    # Stop only this package here; the post-install bundle recovery is responsible
    # for restoring any retained preference backup byte-for-byte.
    print("INFO: retained synthetic simulation detected; force-stopping package")
    adb.force_stop()
    deadline = time.monotonic() + 4.0
    while time.monotonic() < deadline:
        if not base.simulation_active(adb):
            print("PASS: retained synthetic simulation stopped")
            return
        time.sleep(0.2)
    raise StoreCaptureQaError("simulation remains active after package force-stop")


def command(args: argparse.Namespace) -> int:
    if base.git_tracked_status(ROOT):
        raise StoreCaptureQaError("tracked repository files are modified")

    output = Path(args.output_dir).expanduser().resolve()
    if output.exists():
        raise StoreCaptureQaError(f"output path already exists: {output}")

    expected_revision = args.expected_revision
    revision = base.git_revision(ROOT)
    if revision != expected_revision:
        raise StoreCaptureQaError(
            f"source revision mismatch: expected {expected_revision}, got {revision}"
        )

    environment, tool_cache, aapt, apksigner = build_environment(args)
    adb_path = executable(Path(args.adb).expanduser().resolve(), "adb")

    print("=== GeoJoystick Issue #12 single-command QA ===")
    print(f"PASS: exact source revision {revision}")
    print("PASS: tracked source clean")

    print("\n=== Harness self-tests ===")
    base.self_test_command(argparse.Namespace())
    overlay.self_test_command(argparse.Namespace())
    bundle.self_test_command(argparse.Namespace())
    neutral_source = (
        ROOT
        / "app"
        / "src"
        / "debug"
        / "java"
        / "com"
        / "k2040"
        / "geojoystick"
        / "NeutralCaptureActivity.java"
    ).read_text(encoding="utf-8")
    for required in (
        "geojoystick_debug_stop_simulation",
        "stopService(new Intent(this, MockLocationService.class))",
    ):
        if required not in neutral_source:
            raise StoreCaptureQaError(f"debug stop hook source missing: {required}")
    print("PASS: all harness self-tests")
    print("PASS: deterministic debug stop hook present in source")

    print("\n=== Exact debug build ===")
    run([sys.executable, str(ROOT / "tools" / "build.py")], cwd=ROOT, env=environment)
    debug_apk = ROOT / "dist" / "GeoJoystick-debug.apk"
    if not debug_apk.is_file():
        raise StoreCaptureQaError("debug APK missing after build")
    debug_sha = sha256_file(debug_apk)
    verify_badging(aapt, debug_apk)
    if "NeutralCaptureActivity" not in manifest_text(aapt, debug_apk):
        raise StoreCaptureQaError("debug capture helper missing from debug APK")
    print(f"PASS: debug APK {debug_sha}")
    print("PASS: package 0.1.3 (103), targetSdk 36")
    print("PASS: debug capture helper present")

    print("\n=== Unit / lint / release boundary ===")
    release_validation(args, environment, tool_cache, aapt)
    print("PASS: testDebugUnitTest")
    print("PASS: lintRelease")
    print("PASS: assembleRelease")
    print("PASS: capture helper absent from release APK")

    print("\n=== Canonical device ===")
    adb = base.SafeAdb(str(adb_path), args.serial, PACKAGE)
    base.verify_identity(adb, args)
    mock_before = appop_mode(adb, "MOCK_LOCATION")
    overlay_before = appop_mode(adb, "SYSTEM_ALERT_WINDOW")
    if overlay_before != "allow":
        raise StoreCaptureQaError(
            f"overlay permission is not allowed: {overlay_before}"
        )
    print("PASS: exact canonical Android 16/API 36 device")
    ensure_preinstall_inactive(adb)
    print(f"Mock-location app-op: {mock_before}")
    print(f"Overlay app-op:       {overlay_before}")

    print("\n=== Signer-safe replacement install ===")
    candidate_cert = apksigner_digest(apksigner, debug_apk)
    with tempfile.TemporaryDirectory(prefix="geojoystick-installed-apk.") as temporary:
        installed_apk = Path(temporary) / "installed.apk"
        adb.call("pull", installed_base_apk(adb), str(installed_apk), timeout=60.0)
        installed_cert = apksigner_digest(apksigner, installed_apk)
    if installed_cert != candidate_cert:
        raise StoreCaptureQaError("candidate signer differs from installed APK")
    print("PASS: candidate signer matches installed APK")

    adb.call("install", "-r", str(debug_apk), timeout=120.0)
    if appop_mode(adb, "MOCK_LOCATION") != mock_before:
        raise StoreCaptureQaError("mock-location app-op changed during install")
    if appop_mode(adb, "SYSTEM_ALERT_WINDOW") != overlay_before:
        raise StoreCaptureQaError("overlay app-op changed during install")
    print("PASS: signer-safe replacement install")
    print("PASS: app data/app-ops preserved")

    print("\n=== Atomic 10-shot capture ===")
    capture_args = argparse.Namespace(
        adb=str(adb_path),
        serial=args.serial,
        expected_model=args.expected_model,
        expected_product=args.expected_product,
        expected_device=args.expected_device,
        expected_android=args.expected_android,
        expected_api=args.expected_api,
        output_dir=str(output),
        theme=args.theme,
        locales=list(args.locales),
    )
    bundle.capture_command(capture_args)
    bundle.validate_command(argparse.Namespace(input_dir=str(output)))

    if base.simulation_active(adb):
        raise StoreCaptureQaError("simulation remains active after capture")
    if appop_mode(adb, "MOCK_LOCATION") != mock_before:
        raise StoreCaptureQaError("mock-location app-op changed during capture")
    if appop_mode(adb, "SYSTEM_ALERT_WINDOW") != overlay_before:
        raise StoreCaptureQaError("overlay app-op changed during capture")

    pngs = sorted(output.glob("*/images/phoneScreenshots/*.png"))
    if len(pngs) != 10:
        raise StoreCaptureQaError(f"expected 10 screenshots, found {len(pngs)}")

    print("\n=== Screenshot hashes ===")
    for path in pngs:
        print(f"{sha256_file(path)}  {path.relative_to(output)}")

    print("\n=== Result ===")
    print("ISSUE #12 10-SHOT CAPTURE QA: PASS")
    print(f"SOURCE REVISION: {revision}")
    print(f"DEBUG APK SHA-256: {debug_sha}")
    print("EN-US: 5 real screenshots")
    print("DE-DE: 5 real screenshots")
    print("OVERLAY: real expanded system overlay on neutral #ECEFF1")
    print("LOCATION DATA: fixed synthetic only")
    print("RELEASE HELPER LEAK: none")
    print("SIMULATION: inactive")
    print("APP-OPS: preserved")
    print("ACTIONS: not queried or used")
    print("PUBLICATION: none")
    print(f"REVIEW FOLDER: {output}")
    return 0


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(
        description="Build, install, and capture the complete GeoJoystick store QA set"
    )
    result.add_argument("--expected-revision", required=True)
    result.add_argument("--serial", required=True)
    result.add_argument("--expected-model", required=True)
    result.add_argument("--expected-product", required=True)
    result.add_argument("--expected-device", required=True)
    result.add_argument("--expected-android", required=True)
    result.add_argument("--expected-api", required=True)
    result.add_argument("--output-dir", required=True)
    result.add_argument("--java-home", required=True)
    result.add_argument("--sdk", required=True)
    result.add_argument("--tool-cache", required=True)
    result.add_argument("--build-tools", default="36.0.0")
    result.add_argument("--adb", required=True)
    result.add_argument("--theme", choices=("light", "dark"), default="light")
    result.add_argument(
        "--locales",
        nargs="+",
        choices=tuple(base.LOCALES),
        default=list(base.LOCALES),
    )
    return result


def main() -> int:
    args = parser().parse_args()
    try:
        return command(args)
    except (
        StoreCaptureQaError,
        bundle.BundleCaptureError,
        base.CaptureError,
        overlay.OverlayCaptureError,
        impl.QAError,
        OSError,
        subprocess.SubprocessError,
    ) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
