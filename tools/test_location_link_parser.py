#!/usr/bin/env python3
"""Compile and run dependency-free tests for LocationLinkParser."""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app" / "src" / "main" / "java" / "com" / "k2040" / "geojoystick" / "LocationLinkParser.java"
FORBIDDEN_MIN_SDK_APIS = ("Set.of(", "List.of(", "Map.of(")

HARNESS = r"""
package com.k2040.geojoystick;

public final class LocationLinkParserSelfTest {
    private static void assertCoordinates(String name, double[] actual, double lat, double lng) {
        if (actual == null
                || Math.abs(actual[0] - lat) > 0.0000001
                || Math.abs(actual[1] - lng) > 0.0000001) {
            throw new AssertionError(name);
        }
    }

    private static void assertNull(String name, double[] actual) {
        if (actual != null) {
            throw new AssertionError(name);
        }
    }

    private static void assertTrue(String name, boolean value) {
        if (!value) {
            throw new AssertionError(name);
        }
    }

    private static void assertFalse(String name, boolean value) {
        if (value) {
            throw new AssertionError(name);
        }
    }

    public static void main(String[] args) {
        assertCoordinates(
                "plain coordinates",
                LocationLinkParser.parseCoordinates("12.345678, -45.678901"),
                12.345678,
                -45.678901);
        assertCoordinates(
                "google at coordinates",
                LocationLinkParser.resolveCoordinates(
                        "https://www.google.com/maps/@-23.456789,67.890123,15z"),
                -23.456789,
                67.890123);
        assertCoordinates(
                "google data coordinates",
                LocationLinkParser.resolveCoordinates(
                        "https://www.google.com/maps/place/test/data=!3d34.567891!4d-78.901234"),
                34.567891,
                -78.901234);
        assertCoordinates(
                "encoded supported query",
                LocationLinkParser.parseCoordinates(
                        "https%3A%2F%2Fmaps.apple.com%2F%3Fll%3D-16.234567%2C123.456789"),
                -16.234567,
                123.456789);
        assertCoordinates(
                "openstreetmap query",
                LocationLinkParser.resolveCoordinates(
                        "https://www.openstreetmap.org/?mlat=41.234567&mlon=-12.345678#map=14/41.234567/-12.345678"),
                41.234567,
                -12.345678);
        assertCoordinates(
                "openstreetmap fragment",
                LocationLinkParser.resolveCoordinates(
                        "https://www.openstreetmap.org/#map=12/-36.543210/98.765432"),
                -36.543210,
                98.765432);

        assertNull("embedded unrelated pair",
                LocationLinkParser.parseCoordinates("reference 12.345678, -45.678901 end"));
        assertNull("latitude range", LocationLinkParser.parseCoordinates("91.0, 13.0"));
        assertNull("longitude range", LocationLinkParser.parseCoordinates("52.0, 181.0"));
        assertNull("non-finite text", LocationLinkParser.parseCoordinates("NaN, Infinity"));
        assertNull("malformed encoding", LocationLinkParser.parseCoordinates("%ZZ"));
        assertNull(
                "unsupported host with coordinates",
                LocationLinkParser.resolveCoordinates(
                        "https://example.invalid/maps/@12.345678,-45.678901,15z"));
        assertNull(
                "near-match host",
                LocationLinkParser.resolveCoordinates(
                        "https://www.google.com.example.invalid/maps/@12.345678,-45.678901,15z"));
        assertNull(
                "encoded unsupported host",
                LocationLinkParser.resolveCoordinates(
                        "https%3A%2F%2Fexample.invalid%2Fmaps%2F%4012.345678%2C-45.678901%2C15z"));
        assertNull(
                "plain HTTP is not resolved",
                LocationLinkParser.resolveCoordinates(
                        "http://www.google.com/maps/@12.345678,-45.678901,15z"));
        assertNull(
                "oversized shared text",
                LocationLinkParser.resolveCoordinates("x".repeat(8193)));
        assertNull(
                "oversized parser text",
                LocationLinkParser.parseCoordinates("x".repeat(262145)));

        assertTrue("supported Google host",
                LocationLinkParser.isSupportedMapUrl("https://www.google.com/maps"));
        assertTrue("supported Google short host",
                LocationLinkParser.isSupportedMapUrl("https://goo.gl/maps/example"));
        assertTrue("supported Apple host",
                LocationLinkParser.isSupportedMapUrl("https://maps.apple.com/?ll=12.0,34.0"));
        assertTrue("supported OSM host",
                LocationLinkParser.isSupportedMapUrl("https://www.openstreetmap.org/#map=2/0/0"));
        assertFalse("unsupported Google short path",
                LocationLinkParser.isSupportedMapUrl("https://goo.gl/example"));
        assertFalse("unsupported port",
                LocationLinkParser.isSupportedMapUrl("https://www.google.com:444/maps"));
        assertFalse("userinfo rejected",
                LocationLinkParser.isSupportedMapUrl("https://user@www.google.com/maps"));
        assertFalse("private IPv4 literal",
                LocationLinkParser.isPublicAddressLiteral("192.168.10.20"));
        assertFalse("loopback IPv6 literal",
                LocationLinkParser.isPublicAddressLiteral("::1"));
        assertTrue("public IPv4 literal",
                LocationLinkParser.isPublicAddressLiteral("8.8.8.8"));

        System.out.println("LocationLinkParser self-test: PASS");
    }
}
"""


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--java-home", type=Path)
    return parser.parse_args()


def find_tool(name: str, java_home: Path | None) -> str:
    candidates: list[Path] = []
    executable = name + (".exe" if os.name == "nt" else "")
    if java_home is not None:
        candidates.append(java_home / "bin" / executable)
    env_java_home = os.environ.get("JAVA_HOME")
    if env_java_home:
        candidates.append(Path(env_java_home) / "bin" / executable)
    for candidate in candidates:
        if candidate.is_file():
            return str(candidate)
    found = shutil.which(name)
    if found:
        return found
    raise SystemExit(f"Required Java tool not found: {name}")


def main() -> None:
    args = parse_args()
    if not SOURCE.is_file():
        raise SystemExit(f"Parser source not found: {SOURCE}")

    source_text = SOURCE.read_text(encoding="utf-8")
    for forbidden in FORBIDDEN_MIN_SDK_APIS:
        if forbidden in source_text:
            raise SystemExit(
                f"Parser uses an Android API unavailable at minSdk 27 without "
                f"core-library desugaring: {forbidden}"
            )

    javac = find_tool("javac", args.java_home)
    java = find_tool("java", args.java_home)

    with tempfile.TemporaryDirectory(prefix="geojoystick-parser-test-") as temporary:
        temp = Path(temporary)
        package_dir = temp / "src" / "com" / "k2040" / "geojoystick"
        classes = temp / "classes"
        package_dir.mkdir(parents=True)
        classes.mkdir()

        parser_copy = package_dir / SOURCE.name
        shutil.copy2(SOURCE, parser_copy)
        harness = package_dir / "LocationLinkParserSelfTest.java"
        harness.write_text(HARNESS.strip() + "\n", encoding="utf-8", newline="\n")

        subprocess.run(
            [javac, "-encoding", "UTF-8", "-d", str(classes), str(parser_copy), str(harness)],
            check=True,
            cwd=ROOT,
        )
        subprocess.run(
            [java, "-cp", str(classes), "com.k2040.geojoystick.LocationLinkParserSelfTest"],
            check=True,
            cwd=ROOT,
        )


if __name__ == "__main__":
    main()
