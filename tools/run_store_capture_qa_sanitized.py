#!/usr/bin/env python3
"""Run store-capture QA with deterministic public-safe screenshot sanitation.

This wrapper keeps the existing maintained build/install/capture workflow intact,
but stages its output privately, waits for transient overlay UI to settle, removes
only device-reported system-bar rows from the real Android PNGs, rewrites capture
provenance hashes/dimensions, validates the sanitized bundle, and promotes it only
after the complete post-processing pass succeeds.

Device identity and private System UI contents remain runtime-only and are never
written into public provenance.
"""

from __future__ import annotations

import argparse
import binascii
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import struct
import tempfile
import time
import zlib
import sys

import capture_store_bundle as bundle
import capture_store_overlay as overlay
import capture_store_screenshots as base
import run_store_capture_qa as qa


PACKAGE = base.PACKAGE
NEUTRAL_ACTIVITY = ".NeutralCaptureActivity"
OVERLAY_SETTLE_SECONDS = 4.0
SANITIZATION_SCHEMA = 1


class SanitizedCaptureError(RuntimeError):
    pass


def _chunk(kind: bytes, payload: bytes) -> bytes:
    body = kind + payload
    return (
        struct.pack(">I", len(payload))
        + body
        + struct.pack(">I", binascii.crc32(body) & 0xFFFFFFFF)
    )


def _paeth(a: int, b: int, c: int) -> int:
    p = a + b - c
    pa = abs(p - a)
    pb = abs(p - b)
    pc = abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c


def crop_png_rows(payload: bytes, top: int, bottom: int) -> bytes:
    if payload[:8] != b"\x89PNG\r\n\x1a\n":
        raise SanitizedCaptureError("screencap output is not a PNG")

    position = 8
    ihdr: bytes | None = None
    idat_parts: list[bytes] = []
    while position + 12 <= len(payload):
        length = struct.unpack(">I", payload[position : position + 4])[0]
        kind = payload[position + 4 : position + 8]
        data_start = position + 8
        data_end = data_start + length
        if data_end + 4 > len(payload):
            raise SanitizedCaptureError("truncated PNG chunk")
        data = payload[data_start:data_end]
        if kind == b"IHDR":
            ihdr = data
        elif kind == b"IDAT":
            idat_parts.append(data)
        elif kind == b"IEND":
            break
        position = data_end + 4

    if ihdr is None or len(ihdr) != 13 or not idat_parts:
        raise SanitizedCaptureError("PNG is missing IHDR/IDAT data")

    width, height, bit_depth, color_type, compression, filtering, interlace = struct.unpack(
        ">IIBBBBB", ihdr
    )
    if bit_depth != 8 or color_type not in {0, 2, 4, 6}:
        raise SanitizedCaptureError(
            f"unsupported screencap PNG format: bitDepth={bit_depth}, colorType={color_type}"
        )
    if compression != 0 or filtering != 0 or interlace != 0:
        raise SanitizedCaptureError("unsupported compressed/interlaced screencap PNG")

    channels = {0: 1, 2: 3, 4: 2, 6: 4}[color_type]
    stride = width * channels
    raw = zlib.decompress(b"".join(idat_parts))
    expected = height * (stride + 1)
    if len(raw) != expected:
        raise SanitizedCaptureError(
            f"unexpected decompressed PNG size: {len(raw)} != {expected}"
        )
    if top < 0 or bottom < 0 or top + bottom >= height:
        raise SanitizedCaptureError(
            f"invalid crop bounds for {width}x{height}: top={top}, bottom={bottom}"
        )

    rows: list[bytes] = []
    previous = bytearray(stride)
    offset = 0
    for _ in range(height):
        filter_type = raw[offset]
        encoded = raw[offset + 1 : offset + 1 + stride]
        offset += stride + 1
        decoded = bytearray(stride)

        for index, value in enumerate(encoded):
            left = decoded[index - channels] if index >= channels else 0
            up = previous[index]
            up_left = previous[index - channels] if index >= channels else 0

            if filter_type == 0:
                result = value
            elif filter_type == 1:
                result = value + left
            elif filter_type == 2:
                result = value + up
            elif filter_type == 3:
                result = value + ((left + up) // 2)
            elif filter_type == 4:
                result = value + _paeth(left, up, up_left)
            else:
                raise SanitizedCaptureError(f"unsupported PNG filter type: {filter_type}")

            decoded[index] = result & 0xFF

        rows.append(bytes(decoded))
        previous = decoded

    cropped = rows[top : height - bottom]
    encoded_rows = b"".join(b"\x00" + row for row in cropped)
    new_ihdr = struct.pack(
        ">IIBBBBB",
        width,
        len(cropped),
        bit_depth,
        color_type,
        compression,
        filtering,
        interlace,
    )
    return (
        b"\x89PNG\r\n\x1a\n"
        + _chunk(b"IHDR", new_ihdr)
        + _chunk(b"IDAT", zlib.compress(encoded_rows, level=9))
        + _chunk(b"IEND", b"")
    )


def physical_size(adb: base.SafeAdb) -> tuple[int, int]:
    text = adb.shell("wm", "size")
    match = re.search(r"(?:Physical|Override) size:\s*(\d+)x(\d+)", text)
    if not match:
        match = re.search(r"(\d+)x(\d+)", text)
    if not match:
        raise SanitizedCaptureError(f"could not parse display size: {text!r}")
    return int(match.group(1)), int(match.group(2))


def system_bar_crop(adb: base.SafeAdb) -> tuple[int, int]:
    text = adb.shell("dumpsys", "window", "windows", check=False)
    status_values = {
        int(value)
        for value in re.findall(
            r"type=statusBars[^\n]*insetsSize=Insets\{left=0, top=(\d+), right=0, bottom=0\}",
            text,
        )
        if int(value) > 0
    }
    navigation_values = {
        int(value)
        for value in re.findall(
            r"type=navigationBars[^\n]*insetsSize=Insets\{left=0, top=0, right=0, bottom=(\d+)\}",
            text,
        )
        if int(value) > 0
    }
    if len(status_values) != 1 or len(navigation_values) != 1:
        raise SanitizedCaptureError(
            "system-bar insets are ambiguous: "
            f"status={sorted(status_values)}, navigation={sorted(navigation_values)}"
        )
    return next(iter(status_values)), next(iter(navigation_values))


def sha256_bytes(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


def update_provenance(
    root: Path,
    name: str,
    *,
    top: int,
    bottom: int,
    source_width: int,
    source_height: int,
) -> None:
    path = root / name
    data = json.loads(path.read_text(encoding="utf-8"))
    screenshots = data.get("screenshots")
    if not isinstance(screenshots, list):
        raise SanitizedCaptureError(f"{name} screenshots field is invalid")

    for item in screenshots:
        relative = item.get("path")
        if not isinstance(relative, str):
            raise SanitizedCaptureError(f"{name} screenshot path is invalid")
        screenshot = root / relative
        payload = screenshot.read_bytes()
        width, height = base.png_size(payload)
        item["width"] = width
        item["height"] = height
        item["bytes"] = len(payload)
        item["sha256"] = sha256_bytes(payload)

    data["system_bar_sanitization"] = {
        "schema": SANITIZATION_SCHEMA,
        "method": "cropped only device-reported status/navigation bar rows from real Android screencap",
        "source_dimensions": {
            "width": source_width,
            "height": source_height,
        },
        "crop_px": {
            "top_status_bar": top,
            "bottom_navigation_bar": bottom,
        },
        "output_dimensions": {
            "width": source_width,
            "height": source_height - top - bottom,
        },
        "private_system_ui_preserved_on_device": True,
    }
    path.write_text(json.dumps(data, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def sanitize_bundle(
    root: Path,
    *,
    top: int,
    bottom: int,
    source_width: int,
    source_height: int,
) -> None:
    pngs = sorted(root.glob("*/images/phoneScreenshots/*.png"))
    if len(pngs) != 10:
        raise SanitizedCaptureError(f"expected 10 captured PNGs, found {len(pngs)}")

    for path in pngs:
        payload = path.read_bytes()
        width, height = base.png_size(payload)
        if (width, height) != (source_width, source_height):
            raise SanitizedCaptureError(
                f"unexpected raw screenshot dimensions for {path}: {width}x{height}"
            )
        sanitized = crop_png_rows(payload, top, bottom)
        new_width, new_height = base.png_size(sanitized)
        expected_height = source_height - top - bottom
        if (new_width, new_height) != (source_width, expected_height):
            raise SanitizedCaptureError(
                f"sanitized screenshot dimensions differ: {new_width}x{new_height}"
            )
        path.write_bytes(sanitized)

    update_provenance(
        root,
        "screenshot-provenance.json",
        top=top,
        bottom=bottom,
        source_width=source_width,
        source_height=source_height,
    )
    update_provenance(
        root,
        overlay.OVERLAY_PROVENANCE,
        top=top,
        bottom=bottom,
        source_width=source_width,
        source_height=source_height,
    )

    base.validate_capture_tree(root)
    overlay.validate_overlay_tree(root)
    bundle.validate_command(argparse.Namespace(input_dir=str(root)))


def command(args: argparse.Namespace) -> int:
    output = Path(args.output_dir).expanduser().resolve()
    if output.exists():
        raise SanitizedCaptureError(f"output path already exists: {output}")
    output.parent.mkdir(parents=True, exist_ok=True)

    adb = base.SafeAdb(str(Path(args.adb).expanduser().resolve()), args.serial, PACKAGE)
    base.verify_identity(adb, args)
    width, height = physical_size(adb)
    top, bottom = system_bar_crop(adb)
    if top + bottom >= height:
        raise SanitizedCaptureError("system-bar crop would remove the complete frame")

    print("=== GeoJoystick sanitized store-capture QA ===")
    print(f"PASS: display {width}x{height}")
    print(f"PASS: status-bar crop {top}px")
    print(f"PASS: navigation-bar crop {bottom}px")
    print(f"PASS: sanitized output {width}x{height - top - bottom}")

    staging = Path(
        tempfile.mkdtemp(prefix=".geojoystick-store-sanitized.", dir=str(output.parent))
    )
    raw_output = staging / "raw-bundle"

    original_capture_png = base.capture_png

    def settled_capture_png(adb_instance: base.SafeAdb, destination: Path) -> dict[str, object]:
        if base.foreground_activity(adb_instance).endswith(
            f"{PACKAGE}/{NEUTRAL_ACTIVITY}"
        ):
            time.sleep(OVERLAY_SETTLE_SECONDS)
        return original_capture_png(adb_instance, destination)

    try:
        base.capture_png = settled_capture_png
        try:
            qa_args = argparse.Namespace(**vars(args))
            qa_args.output_dir = str(raw_output)
            result = qa.command(qa_args)
        finally:
            base.capture_png = original_capture_png

        if result != 0:
            raise SanitizedCaptureError(f"maintained QA returned {result}")

        sanitize_bundle(
            raw_output,
            top=top,
            bottom=bottom,
            source_width=width,
            source_height=height,
        )
        if output.exists():
            raise SanitizedCaptureError(f"output path appeared during QA: {output}")
        os.replace(raw_output, output)
    finally:
        base.capture_png = original_capture_png
        shutil.rmtree(staging, ignore_errors=True)

    pngs = sorted(output.glob("*/images/phoneScreenshots/*.png"))
    print("\n=== Sanitized screenshot hashes ===")
    for path in pngs:
        payload = path.read_bytes()
        print(f"{sha256_bytes(payload)}  {path.relative_to(output)}")

    print("\n=== Result ===")
    print("ISSUE #12 SANITIZED 10-SHOT CAPTURE QA: PASS")
    print(f"SOURCE REVISION: {base.git_revision(Path(__file__).resolve().parents[1])}")
    print(f"SYSTEM BAR CROP: top {top}px / bottom {bottom}px")
    print(f"OUTPUT DIMENSIONS: {width}x{height - top - bottom}")
    print(f"OVERLAY SETTLE: {OVERLAY_SETTLE_SECONDS:.1f}s additional")
    print("SCREENSHOTS: 10 real Android captures, system-bar rows removed only")
    print("PRIVATE NOTIFICATIONS: unchanged on device and excluded from output")
    print("SIMULATION: inactive after maintained QA")
    print("PUBLICATION: none")
    print(f"REVIEW FOLDER: {output}")
    return 0


def main() -> int:
    parser = qa.parser()
    args = parser.parse_args()
    try:
        return command(args)
    except (
        SanitizedCaptureError,
        qa.StoreCaptureQaError,
        bundle.BundleCaptureError,
        base.CaptureError,
        overlay.OverlayCaptureError,
        OSError,
    ) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
