#!/usr/bin/env python3
"""Capture the complete sanitized GeoJoystick store screenshot bundle.

This is the maintainer-facing orchestration entrypoint for Issue #12. It runs the
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

import capture_store_overlay as overlay
import capture_store_screenshots as base


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


def capture_command(args: argparse.Namespace) -> int:
    output = Path(args.output_dir).expanduser().resolve()
    if output.exists():
        raise BundleCaptureError(
            f"output path already exists; refusing overwrite: {output}"
        )

    output.parent.mkdir(parents=True, exist_ok=True)
    staging_root = Path(
        tempfile.mkdtemp(prefix=".geojoystick-store-bundle.", dir=output.parent)
    )
    staged_output = staging_root / "bundle"

    try:
        common = _common_namespace(args, staged_output)
        base.capture_command(common)
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
    print("Store screenshot bundle self-test: PASS")
    return 0


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(
        description="Capture or validate the complete GeoJoystick store screenshot bundle"
    )
    subparsers = result.add_subparsers(dest="command", required=True)

    capture = subparsers.add_parser(
        "capture", help="capture and atomically publish all 10 screenshots"
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
