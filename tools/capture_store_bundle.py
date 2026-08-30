#!/usr/bin/env python3
"""Capture the complete sanitized GeoJoystick store screenshot bundle.

This is the maintainer-facing orchestration entrypoint for store screenshot QA. It runs the
normal localized app capture and the real expanded-overlay capture into a private
temporary staging directory, validates both provenance manifests and all ten PNGs,
and publishes the requested output directory only after the complete bundle passes.
Partial capture output is never promoted.
"""

from __future__ import annotations

import argparse
import os
from pathlib import Path
import shutil
import tempfile
import time

import capture_store_overlay as overlay
import capture_store_screenshots as base


DEBUG_STOP_EXTRA = "geojoystick_debug_stop_simulation"


class BundleCaptureError(RuntimeError):
    pass


def _common_namespace(args: argparse.Namespace, output_dir: Path) -> argparse.Namespace:
    return argparse.Namespace(
        adb=args.adb,
        serial=args.serial,
        expected_model=args.expected_model,
        expected_product=args.expected_product,
        expected_device=args.expected_device,
        expected_android=args.expected_android,
        expected_api=args.expected_api,
        output_dir=str(output_dir),
        theme=args.theme,
        locales=list(args.locales),
    )


def _activity_report_has_component(report: str, package: str, suffix: str) -> bool:
    """Return true when a resumed/focused activity line names the component.

    Android/OEM dumpsys output varies between short component notation
    (package/.Activity) and fully-qualified notation
    (package/package.Activity). Restrict matching to resumed/focused markers so
    a stale task-history record is not accepted as foreground evidence.
    """
    class_name = suffix.lstrip(".")
    markers = (
        "mResumedActivity",
        "topResumedActivity",
        "mCurrentFocus",
        "mFocusedApp",
    )
    for line in report.splitlines():
        if not any(marker in line for marker in markers):
            continue
        if package not in line or class_name not in line:
            continue
        return True
    return False


def _wait_activity_robust(adb: base.SafeAdb, suffix: str, timeout: float = 6.0) -> None:
    deadline = time.monotonic() + timeout
    last_reports: list[str] = []
    while time.monotonic() < deadline:
        reports = [
            adb.shell("dumpsys", "activity", "activities", check=False),
            adb.shell("dumpsys", "window", "windows", check=False),
            adb.shell("dumpsys", "window", check=False),
        ]
        last_reports = reports
        if any(
            _activity_report_has_component(report, adb.package, suffix)
            for report in reports
        ):
            return
        time.sleep(0.2)

    expected = f"{adb.package}/{suffix}"
    summaries: list[str] = []
    for report in last_reports:
        for line in report.splitlines():
            if any(
                marker in line
                for marker in (
                    "mResumedActivity",
                    "topResumedActivity",
                    "mCurrentFocus",
                    "mFocusedApp",
                )
            ):
                summaries.append(line.strip())
    detail = " | ".join(summaries[-6:]) if summaries else "no focus markers reported"
    raise BundleCaptureError(
        f"activity did not become foreground: {expected}; observed: {detail}"
    )


def _stop_via_debug_capture(
    adb: base.SafeAdb,
    locale: str,
    *,
    allow_force_stop: bool,
) -> None:
    """Stop simulation through the debug-only neutral capture activity."""
    del locale
    if not base.simulation_active(adb):
        return

    stop_error: BaseException | None = None
    try:
        adb.shell(
            "am",
            "start",
            "-W",
            "-n",
            f"{adb.package}/{overlay.NEUTRAL_ACTIVITY}",
            "--ez",
            DEBUG_STOP_EXTRA,
            "true",
            timeout=30.0,
        )
        overlay.wait_simulation(adb, False)
        return
    except BaseException as exc:
        stop_error = exc

    if allow_force_stop:
        adb.force_stop()
        try:
            overlay.wait_simulation(adb, False, timeout=3.0)
            return
        except BaseException as force_error:
            raise BundleCaptureError(
                f"debug capture stop failed: {stop_error}; "
                f"force-stop recovery also failed: {force_error}"
            ) from stop_error

    raise BundleCaptureError(f"debug capture stop failed: {stop_error}") from stop_error


def capture_command(args: argparse.Namespace) -> int:
    output = Path(args.output_dir).expanduser().resolve()
    if output.exists():
        raise BundleCaptureError(
            f"output path already exists; refusing overwrite: {output}"
        )

    common = _common_namespace(args, output)

    # Install the device/OEM-safe capture helpers before recovery so a retained
    # synthetic simulation can be stopped without accessibility discovery.
    base.wait_activity = _wait_activity_robust
    overlay.stop_via_main = _stop_via_debug_capture

    # Recovery is deliberately part of the same maintained operation so the
    # maintainer handoff contains one device-facing Python invocation only.
    overlay.recover_command(common)

    output.parent.mkdir(parents=True, exist_ok=True)
    staging_root = Path(
        tempfile.mkdtemp(prefix=".geojoystick-store-bundle.", dir=output.parent)
    )
    staged_output = staging_root / "bundle"

    try:
        common = _common_namespace(args, staged_output)
        base.capture_command(common)

        # The overlay harness imports the same capture_store_screenshots module as
        # this bundle, so the hardened foreground waiter and debug-only stop hook
        # remain active for the overlay phase.
        base.wait_activity = _wait_activity_robust
        overlay.stop_via_main = _stop_via_debug_capture
        overlay.capture_command(common)

        base_result = base.validate_capture_tree(staged_output)
        overlay_result = overlay.validate_overlay_tree(staged_output)

        pngs = sorted(staged_output.glob("*/images/phoneScreenshots/*.png"))
        if len(pngs) != 10:
            raise BundleCaptureError(
                f"complete bundle must contain exactly 10 PNGs, found {len(pngs)}"
            )

        expected_names = {
            "01-home.png",
            "02-map.png",
            "03-settings.png",
            "04-about.png",
            "05-overlay.png",
        }
        for locale in args.locales:
            locale_dir = staged_output / locale / "images" / "phoneScreenshots"
            names = {path.name for path in locale_dir.glob("*.png")}
            if names != expected_names:
                raise BundleCaptureError(
                    f"unexpected screenshot set for {locale}: {sorted(names)!r}"
                )

        if base_result["dimensions"] != overlay_result["dimensions"]:
            raise BundleCaptureError(
                "base and overlay screenshot dimensions differ: "
                f"{base_result['dimensions']} vs {overlay_result['dimensions']}"
            )

        staged_output.replace(output)

        print("PASS: complete sanitized store screenshot bundle")
        print("PASS: 10 real Android screenshots")
        print("PASS: 5 screenshots per locale")
        print("PASS: base + overlay provenance validated")
        print(f"PASS: source revision {base_result['revision']}")
        print(f"OUTPUT: {output}")
        return 0
    finally:
        shutil.rmtree(staging_root, ignore_errors=True)


def validate_command(args: argparse.Namespace) -> int:
    root = Path(args.input_dir).expanduser().resolve()
    base_result = base.validate_capture_tree(root)
    overlay_result = overlay.validate_overlay_tree(root)
    pngs = sorted(root.glob("*/images/phoneScreenshots/*.png"))
    if len(pngs) != 10:
        raise BundleCaptureError(
            f"complete bundle must contain exactly 10 PNGs, found {len(pngs)}"
        )
    if base_result["dimensions"] != overlay_result["dimensions"]:
        raise BundleCaptureError("base and overlay dimensions differ")
    print(
        "PASS: complete 10-shot bundle, "
        f"{base_result['dimensions'][0]}x{base_result['dimensions'][1]}, "
        f"revision {base_result['revision']}"
    )
    return 0


def self_test_command(_args: argparse.Namespace) -> int:
    if tuple(filename for filename, _screen in base.SCREENS) != (
        "01-home.png",
        "02-map.png",
        "03-settings.png",
        "04-about.png",
    ):
        raise BundleCaptureError("base screenshot ordering changed unexpectedly")
    if overlay.OVERLAY_FILENAME != "05-overlay.png":
        raise BundleCaptureError("overlay screenshot filename changed unexpectedly")
    if set(base.LOCALES) != set(overlay.OVERLAY_LABELS):
        raise BundleCaptureError("base/overlay locale sets differ")
    if DEBUG_STOP_EXTRA != "geojoystick_debug_stop_simulation":
        raise BundleCaptureError("debug stop extra changed unexpectedly")

    package = "com.k2040.geojoystick"
    suffix = ".NeutralCaptureActivity"
    accepted_reports = (
        "mResumedActivity: ActivityRecord{abcd u0 com.k2040.geojoystick/.NeutralCaptureActivity t42}",
        "topResumedActivity=ActivityRecord{abcd u0 com.k2040.geojoystick/com.k2040.geojoystick.NeutralCaptureActivity t42}",
        "mCurrentFocus=Window{abcd u0 com.k2040.geojoystick/com.k2040.geojoystick.NeutralCaptureActivity}",
        "mFocusedApp=ActivityRecord{abcd u0 com.k2040.geojoystick/.NeutralCaptureActivity t42}",
    )
    for report in accepted_reports:
        if not _activity_report_has_component(report, package, suffix):
            raise BundleCaptureError(
                f"foreground parser rejected supported report: {report}"
            )
    stale_report = (
        "Hist #0: ActivityRecord{abcd u0 "
        "com.k2040.geojoystick/.NeutralCaptureActivity t42}"
    )
    if _activity_report_has_component(stale_report, package, suffix):
        raise BundleCaptureError("foreground parser accepted stale task history")

    print("Store screenshot bundle self-test: PASS")
    return 0


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(
        description="Capture or validate the complete GeoJoystick store screenshot bundle"
    )
    subparsers = result.add_subparsers(dest="command", required=True)

    capture = subparsers.add_parser(
        "capture", help="recover, capture, validate, and atomically publish all 10 screenshots"
    )
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
        choices=tuple(base.LOCALES),
        default=list(base.LOCALES),
    )
    capture.set_defaults(func=capture_command)

    validate = subparsers.add_parser("validate", help="validate a complete bundle")
    validate.add_argument("--input-dir", required=True)
    validate.set_defaults(func=validate_command)

    self_test = subparsers.add_parser("self-test", help="run device-free bundle checks")
    self_test.set_defaults(func=self_test_command)

    return result


def main() -> int:
    args = parser().parse_args()
    try:
        return int(args.func(args))
    except (BundleCaptureError, base.CaptureError, overlay.OverlayCaptureError, OSError) as exc:
        print(f"FAIL: {exc}", file=os.sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
