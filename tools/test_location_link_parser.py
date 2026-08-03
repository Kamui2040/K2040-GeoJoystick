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

    public static void main(String[] args) {
        assertCoordinates(
                "plain coordinates",
                LocationLinkParser.parseCoordinates("52.520008, 13.404954"),
                52.520008,
                13.404954);
        assertCoordinates(
                "at coordinates",
                LocationLinkParser.parseCoordinates("https://maps.example/@-33.8688,151.2093,15z"),
                -33.8688,
                151.2093);
        assertCoordinates(
                "encoded query",
                LocationLinkParser.parseCoordinates("https%3A%2F%2Fexample.invalid%2F%3Fq%3D48.137154%2C11.576124"),
                48.137154,
                11.576124);
        assertCoordinates(
                "data coordinates",
                LocationLinkParser.parseCoordinates("!3d40.7128!4d-74.0060"),
                40.7128,
                -74.0060);
        assertNull("latitude range", LocationLinkParser.parseCoordinates("91.0, 13.0"));
        assertNull("longitude range", LocationLinkParser.parseCoordinates("52.0, 181.0"));
        assertNull("non-finite text", LocationLinkParser.parseCoordinates("NaN, Infinity"));
        assertNull(
                "plain HTTP is not resolved",
                LocationLinkParser.resolveCoordinates("http://example.invalid/no-coordinates"));
        assertNull(
                "oversized shared text",
                LocationLinkParser.resolveCoordinates("x".repeat(8193)));

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
