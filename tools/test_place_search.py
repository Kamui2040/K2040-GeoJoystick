#!/usr/bin/env python3
"""Compile and run dependency-free place-search validation tests."""

from __future__ import annotations

import os
import shutil
import subprocess
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
VALIDATOR = (
    ROOT
    / "app"
    / "src"
    / "main"
    / "java"
    / "com"
    / "k2040"
    / "geojoystick"
    / "PlaceSearchValidator.java"
)
MAP_ACTIVITY = (
    ROOT
    / "app"
    / "src"
    / "main"
    / "java"
    / "com"
    / "k2040"
    / "geojoystick"
    / "MapActivity.java"
)
GEOCODER = (
    ROOT
    / "app"
    / "src"
    / "main"
    / "java"
    / "com"
    / "k2040"
    / "geojoystick"
    / "PlaceSearchGeocoder.java"
)
MAP_HTML = ROOT / "app" / "src" / "main" / "assets" / "map.html"
PRIVACY = ROOT / "PRIVACY.md"

HARNESS = r"""
package com.k2040.geojoystick;

public final class PlaceSearchValidatorSelfTest {
    private static void assertStatus(String name, int expected, PlaceSearchValidator.Resolution actual) {
        if (actual.status != expected) {
            throw new AssertionError(name + ": " + actual.status);
        }
    }

    private static void assertSuccess(
            String name,
            PlaceSearchValidator.Resolution actual,
            double latitude,
            double longitude) {
        assertStatus(name, PlaceSearchValidator.STATUS_SUCCESS, actual);
        if (Math.abs(actual.latitude - latitude) > 0.0000001
                || Math.abs(actual.longitude - longitude) > 0.0000001) {
            throw new AssertionError(name + ": coordinates");
        }
    }

    private static void assertNull(String name, String value) {
        if (value != null) {
            throw new AssertionError(name);
        }
    }

    public static void main(String[] args) {
        if (!"Berlin".equals(PlaceSearchValidator.sanitizeQuery("  Berlin  "))) {
            throw new AssertionError("trim query");
        }
        assertNull("empty query", PlaceSearchValidator.sanitizeQuery("   "));
        assertNull("control query", PlaceSearchValidator.sanitizeQuery("Berlin\u0000test"));
        assertNull(
                "oversized query",
                PlaceSearchValidator.sanitizeQuery("x".repeat(257)));

        assertStatus(
                "no result",
                PlaceSearchValidator.STATUS_NO_RESULT,
                PlaceSearchValidator.resolve(new double[0][]));
        assertSuccess(
                "single result",
                PlaceSearchValidator.resolve(
                        new double[][]{{52.520008, 13.404954}}),
                52.520008,
                13.404954);
        assertSuccess(
                "duplicate coordinate variants",
                PlaceSearchValidator.resolve(
                        new double[][]{
                                {52.520008, 13.404954},
                                {52.5200084, 13.4049544}
                        }),
                52.520008,
                13.404954);
        assertStatus(
                "ambiguous",
                PlaceSearchValidator.STATUS_AMBIGUOUS,
                PlaceSearchValidator.resolve(
                        new double[][]{
                                {52.520008, 13.404954},
                                {48.137154, 11.576124}
                        }));
        assertStatus(
                "invalid latitude",
                PlaceSearchValidator.STATUS_INVALID,
                PlaceSearchValidator.resolve(
                        new double[][]{{90.0, 13.404954}}));
        assertStatus(
                "invalid longitude",
                PlaceSearchValidator.STATUS_INVALID,
                PlaceSearchValidator.resolve(
                        new double[][]{{52.520008, 181.0}}));
        assertStatus(
                "non-finite",
                PlaceSearchValidator.STATUS_INVALID,
                PlaceSearchValidator.resolve(
                        new double[][]{{Double.NaN, 13.404954}}));

        System.out.println("PlaceSearchValidator self-test: PASS");
    }
}
"""


def find_tool(name: str) -> str:
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        candidate = Path(java_home) / "bin" / name
        if candidate.is_file():
            return str(candidate)
    found = shutil.which(name)
    if found:
        return found
    raise SystemExit(f"Required Java tool not found: {name}")


def source_contracts() -> None:
    map_activity = MAP_ACTIVITY.read_text(encoding="utf-8")
    geocoder = GEOCODER.read_text(encoding="utf-8")
    map_html = MAP_HTML.read_text(encoding="utf-8")
    privacy = PRIVACY.read_text(encoding="utf-8")

    required_map = (
        "submitPlaceSearch()",
        "PlaceSearchValidator.sanitizeQuery",
        "PlaceSearchGeocoder.isAvailable()",
        "PlaceSearchGeocoder.search(",
        "PlaceSearchValidator.resolve(",
        "GeoMap.setLocation",
        "IME_ACTION_SEARCH",
    )
    for token in required_map:
        if token not in map_activity:
            raise SystemExit(f"MapActivity missing place-search contract: {token}")

    forbidden_map = (
        "addTextChangedListener",
        "TextWatcher",
        "afterTextChanged",
        "nominatim.openstreetmap.org",
    )
    for token in forbidden_map:
        if token in map_activity:
            raise SystemExit(f"MapActivity contains forbidden search behavior: {token}")

    required_geocoder = (
        "Geocoder.isPresent()",
        "MAX_RESULTS = 2",
        "getFromLocationName",
        "Build.VERSION_CODES.TIRAMISU",
    )
    for token in required_geocoder:
        if token not in geocoder:
            raise SystemExit(f"Geocoder adapter missing contract: {token}")

    if "connect-src 'none'" not in map_html:
        raise SystemExit("Bundled map CSP no longer blocks WebView connections")
    if "nominatim.openstreetmap.org" in geocoder:
        raise SystemExit("Direct public Nominatim endpoint must not be embedded")
    if "### Place and address search" not in privacy:
        raise SystemExit("Privacy disclosure for place search is missing")
    if "only when you submit" not in privacy:
        raise SystemExit("Privacy disclosure does not state explicit submission")


def main() -> None:
    for path in (VALIDATOR, MAP_ACTIVITY, GEOCODER, MAP_HTML, PRIVACY):
        if not path.is_file():
            raise SystemExit(f"Required file not found: {path}")

    source_contracts()
    javac = find_tool("javac")
    java = find_tool("java")

    with tempfile.TemporaryDirectory(prefix="geojoystick-place-search-test-") as temp_raw:
        temp = Path(temp_raw)
        package_dir = temp / "src" / "com" / "k2040" / "geojoystick"
        classes = temp / "classes"
        package_dir.mkdir(parents=True)
        classes.mkdir()

        validator_copy = package_dir / VALIDATOR.name
        shutil.copy2(VALIDATOR, validator_copy)
        harness = package_dir / "PlaceSearchValidatorSelfTest.java"
        harness.write_text(HARNESS.strip() + "\n", encoding="utf-8", newline="\n")

        subprocess.run(
            [javac, "-encoding", "UTF-8", "-d", str(classes), str(validator_copy), str(harness)],
            check=True,
            cwd=ROOT,
        )
        subprocess.run(
            [java, "-cp", str(classes), "com.k2040.geojoystick.PlaceSearchValidatorSelfTest"],
            check=True,
            cwd=ROOT,
        )

    print("Place search source contracts: PASS")


if __name__ == "__main__":
    main()
