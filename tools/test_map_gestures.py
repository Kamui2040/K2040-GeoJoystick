#!/usr/bin/env python3
"""Regression checks for GeoJoystick's bundled map gesture behavior."""

from __future__ import annotations

import math
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MAP = ROOT / "app/src/main/assets/map.html"
TILE_SIZE = 256
MIN_ZOOM = 2
MAX_ZOOM = 19
MAX_LATITUDE = 85.05112878
PINCH_ZOOM_THRESHOLD = 1.18


def clamp(value: float, minimum: float, maximum: float) -> float:
    return max(minimum, min(maximum, value))


def scale_for(zoom: int) -> float:
    return TILE_SIZE * (2 ** zoom)


def lat_lng_to_world(latitude: float, longitude: float, zoom: int) -> tuple[float, float]:
    latitude = clamp(latitude, -MAX_LATITUDE, MAX_LATITUDE)
    scale = scale_for(zoom)
    sine = math.sin(math.radians(latitude))
    return (
        (longitude + 180.0) / 360.0 * scale,
        (0.5 - math.log((1 + sine) / (1 - sine)) / (4 * math.pi)) * scale,
    )


def world_to_lat_lng(x: float, y: float, zoom: int) -> tuple[float, float]:
    scale = scale_for(zoom)
    longitude = x / scale * 360.0 - 180.0
    n = math.pi - 2 * math.pi * y / scale
    latitude = math.degrees(math.atan(0.5 * (math.exp(n) - math.exp(-n))))
    return latitude, longitude


def screen_to_lat_lng(
    center: tuple[float, float],
    zoom: int,
    width: float,
    height: float,
    screen_x: float,
    screen_y: float,
) -> tuple[float, float]:
    return world_to_lat_lng(
        center[0] + screen_x - width / 2,
        center[1] + screen_y - height / 2,
        zoom,
    )


def zoom_around(
    center: tuple[float, float],
    zoom: int,
    requested_zoom: int,
    width: float,
    height: float,
    screen_x: float,
    screen_y: float,
) -> tuple[tuple[float, float], int]:
    target = int(clamp(round(requested_zoom), MIN_ZOOM, MAX_ZOOM))
    if target == zoom:
        return center, zoom
    latitude, longitude = screen_to_lat_lng(
        center, zoom, width, height, screen_x, screen_y
    )
    anchor_x, anchor_y = lat_lng_to_world(latitude, longitude, target)
    return (
        (
            anchor_x - screen_x + width / 2,
            anchor_y - screen_y + height / 2,
        ),
        target,
    )


def assert_close(first: float, second: float, tolerance: float = 1e-9) -> None:
    if abs(first - second) > tolerance:
        raise AssertionError(f"{first!r} != {second!r}")


def pinch_zoom_steps(scale: float) -> int:
    if not math.isfinite(scale) or scale <= 0:
        return 0
    logarithmic = math.log(scale) / math.log(PINCH_ZOOM_THRESHOLD)
    return (
        math.floor(logarithmic + 1e-9)
        if logarithmic >= 0
        else math.ceil(logarithmic - 1e-9)
    )


def center_for_anchor(
    anchor: tuple[float, float],
    zoom: int,
    width: float,
    height: float,
    midpoint: tuple[float, float],
) -> tuple[float, float]:
    world_x, world_y = lat_lng_to_world(anchor[0], anchor[1], zoom)
    return (
        world_x - midpoint[0] + width / 2,
        world_y - midpoint[1] + height / 2,
    )


def test_zoom_anchor() -> None:
    width, height = 1080.0, 2200.0
    center = lat_lng_to_world(48.137154, 11.576124, 8)
    for anchor in ((540.0, 1100.0), (175.0, 390.0), (860.0, 1700.0)):
        before = screen_to_lat_lng(center, 8, width, height, *anchor)
        zoomed_in_center, zoomed_in = zoom_around(
            center, 8, 9, width, height, *anchor
        )
        after_in = screen_to_lat_lng(
            zoomed_in_center, zoomed_in, width, height, *anchor
        )
        assert_close(before[0], after_in[0])
        assert_close(before[1], after_in[1])

        zoomed_out_center, zoomed_out = zoom_around(
            center, 8, 7, width, height, *anchor
        )
        after_out = screen_to_lat_lng(
            zoomed_out_center, zoomed_out, width, height, *anchor
        )
        assert_close(before[0], after_out[0])
        assert_close(before[1], after_out[1])


def test_pinch_anchor_invariant() -> None:
    width, height = 1080.0, 2200.0
    start_zoom = 9
    initial_center = lat_lng_to_world(48.137154, 11.576124, start_zoom)
    start_midpoint = (325.0, 640.0)
    anchor = screen_to_lat_lng(
        initial_center,
        start_zoom,
        width,
        height,
        *start_midpoint,
    )

    # Live midpoint jitter is deliberately ignored during the pinch. The
    # geographic focus remains at the fixed screen-space midpoint captured
    # when the two-finger gesture began.
    samples = (
        ((345.0, 640.0), 1.08),
        ((325.0, 640.0), 1.19),
        ((300.0, 620.0), 1.42),
        ((350.0, 665.0), 0.82),
    )
    for _live_midpoint, scale in samples:
        target_zoom = int(
            clamp(
                start_zoom + pinch_zoom_steps(scale),
                MIN_ZOOM,
                MAX_ZOOM,
            )
        )
        center = center_for_anchor(
            anchor,
            target_zoom,
            width,
            height,
            start_midpoint,
        )
        observed = screen_to_lat_lng(
            center,
            target_zoom,
            width,
            height,
            *start_midpoint,
        )
        assert_close(anchor[0], observed[0])
        assert_close(anchor[1], observed[1])


def test_pinch_thresholds() -> None:
    assert pinch_zoom_steps(1.17) == 0
    assert pinch_zoom_steps(1.18) == 1
    assert pinch_zoom_steps(1.18 ** 2) == 2
    assert pinch_zoom_steps(0.86) == 0
    assert pinch_zoom_steps(1 / 1.18) == -1
    assert pinch_zoom_steps(1 / (1.18 ** 2)) == -2


def test_zoom_bounds() -> None:
    center = (1000.0, 1000.0)
    unchanged_min, min_zoom = zoom_around(
        center, MIN_ZOOM, MIN_ZOOM - 1, 800, 1200, 400, 600
    )
    assert unchanged_min == center
    assert min_zoom == MIN_ZOOM

    unchanged_max, max_zoom = zoom_around(
        center, MAX_ZOOM, MAX_ZOOM + 1, 800, 1200, 400, 600
    )
    assert unchanged_max == center
    assert max_zoom == MAX_ZOOM


def test_source_contract() -> None:
    source = MAP.read_text(encoding="utf-8")
    required = (
        "touch-action: none;",
        "const activePointers = new Map();",
        "const pinchZoomThreshold = 1.18;",
        "function beginPinch()",
        "let pinchStartDistance = 0;",
        "let pinchStartZoom = zoom;",
        "let pinchAnchor = {lat: 0, lng: 0};",
        "let pinchStartMidpoint = {x: 0, y: 0};",
        "pinchStartMidpoint = {x: midpoint.x, y: midpoint.y};",
        "function pinchZoomSteps(scale)",
        "pinchAnchor = screenToLatLng(midpoint.x, midpoint.y);",
        "function zoomAround(screenX, screenY, requestedZoom)",
        "function updatePinch()",
        "function finishPointer(event, cancelled)",
        'map.addEventListener("pointerdown"',
        'map.addEventListener("pointermove"',
        'map.addEventListener("pointerup"',
        'map.addEventListener("pointercancel"',
        'map.addEventListener("lostpointercapture"',
        "activePointers.size >= 2",
        "const shouldPlaceMarker = !cancelled",
        "&& !gestureHadPinch",
        "&& !wasPinching",
        "zoomBy(1);",
        "zoomBy(-1);",
    )
    for token in required:
        if token not in source:
            raise AssertionError(f"map gesture source is missing {token!r}")

    forbidden = (
        "pinchCenter",
        "pinchMidpoint",
        "pinchDistance",
        "resetPinchBaseline",
        "x: anchorWorld.x - currentMidpoint.x + width / 2",
        "y: anchorWorld.y - currentMidpoint.y + height / 2",
        "let dragging = false;",
        "if (!dragging)",
        "dragging = true;",
        "dragging = false;",
    )
    for token in forbidden:
        if token in source:
            raise AssertionError(f"legacy gesture state remains: {token!r}")


def main() -> int:
    test_zoom_anchor()
    test_pinch_anchor_invariant()
    test_pinch_thresholds()
    test_zoom_bounds()
    test_source_contract()
    print("Map gesture regression test: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
