#!/usr/bin/env python3
"""Bootstrap Gradle and build the private GeoJoystick APK on Windows."""

from __future__ import annotations

import hashlib
import os
import platform
import re
import shutil
import subprocess
import sys
import urllib.request
import zipfile
from pathlib import Path
from typing import NoReturn

GRADLE_VERSION = "8.13"
GRADLE_URL = f"https://services.gradle.org/distributions/gradle-{GRADLE_VERSION}-bin.zip"
GRADLE_SHA_URL = GRADLE_URL + ".sha256"
COMPILE_SDK = 35
PREFERRED_BUILD_TOOLS = "35.0.0"
MINIMUM_COMPATIBLE_BUILD_TOOLS = (35, 0, 0)

ROOT = Path(__file__).resolve().parents[1]
CACHE_ROOT = Path(os.environ.get("LOCALAPPDATA", ROOT / ".tools")) / "K2040" / "GeoJoystick"
GRADLE_HOME = CACHE_ROOT / f"gradle-{GRADLE_VERSION}"


def fail(message: str) -> NoReturn:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def download(url: str, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    print(f"Downloading {url}")
    request = urllib.request.Request(url, headers={"User-Agent": "GeoJoystick build bootstrap"})
    with urllib.request.urlopen(request, timeout=60) as response, destination.open("wb") as output:
        shutil.copyfileobj(response, output)


def ensure_gradle() -> Path:
    executable = GRADLE_HOME / "bin" / ("gradle.bat" if os.name == "nt" else "gradle")
    if executable.exists():
        return executable

    archive = CACHE_ROOT / f"gradle-{GRADLE_VERSION}-bin.zip"
    checksum_file = CACHE_ROOT / f"gradle-{GRADLE_VERSION}-bin.zip.sha256"
    if not archive.exists():
        download(GRADLE_URL, archive)
    download(GRADLE_SHA_URL, checksum_file)
    expected = checksum_file.read_text(encoding="utf-8").strip().split()[0].lower()
    actual = hashlib.sha256(archive.read_bytes()).hexdigest().lower()
    if actual != expected:
        archive.unlink(missing_ok=True)
        fail(f"Gradle archive checksum mismatch: expected {expected}, got {actual}")

    print(f"Extracting Gradle {GRADLE_VERSION}")
    with zipfile.ZipFile(archive) as package:
        package.extractall(CACHE_ROOT)
    if not executable.exists():
        fail(f"Gradle executable was not found after extraction: {executable}")
    return executable


def java_candidates() -> list[Path]:
    candidates: list[Path] = []
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        candidates.append(Path(java_home))
    if os.name == "nt":
        program_files = Path(os.environ.get("ProgramFiles", r"C:\Program Files"))
        candidates.extend([
            program_files / "Android" / "Android Studio" / "jbr",
            program_files / "Android" / "Android Studio" / "jre",
        ])
    java_on_path = shutil.which("java")
    if java_on_path:
        candidates.append(Path(java_on_path).resolve().parent.parent)
    return candidates


def find_java_home() -> Path:
    for candidate in java_candidates():
        executable = candidate / "bin" / ("java.exe" if os.name == "nt" else "java")
        if not executable.exists():
            continue
        result = subprocess.run([str(executable), "-version"], capture_output=True, text=True)
        version_text = result.stderr + result.stdout
        match = re.search(r'version "(\d+)', version_text)
        if match and int(match.group(1)) >= 17:
            return candidate
    fail("JDK 17 or newer was not found. Install Android Studio or set JAVA_HOME.")


def sdk_candidates() -> list[Path]:
    candidates: list[Path] = []
    for variable in ("ANDROID_SDK_ROOT", "ANDROID_HOME"):
        value = os.environ.get(variable)
        if value:
            candidates.append(Path(value))
    if os.name == "nt":
        local_app_data = Path(os.environ.get("LOCALAPPDATA", Path.home() / "AppData" / "Local"))
        candidates.append(local_app_data / "Android" / "Sdk")
    candidates.extend([Path.home() / "Android" / "Sdk", Path.home() / "Library" / "Android" / "sdk"])
    return candidates


def find_android_sdk() -> Path:
    for candidate in sdk_candidates():
        if (candidate / "platform-tools").exists() or (candidate / "platforms").exists():
            return candidate
    fail("Android SDK was not found. Install it through Android Studio or set ANDROID_SDK_ROOT.")


def parse_stable_version(name: str) -> tuple[int, int, int] | None:
    match = re.fullmatch(r"(\d+)\.(\d+)\.(\d+)", name)
    if not match:
        return None
    return tuple(int(part) for part in match.groups())


def installed_build_tools(sdk: Path) -> list[tuple[tuple[int, int, int], str]]:
    root = sdk / "build-tools"
    results: list[tuple[tuple[int, int, int], str]] = []
    if not root.is_dir():
        return results
    for directory in root.iterdir():
        version = parse_stable_version(directory.name)
        if version is None or not directory.is_dir():
            continue
        aapt2 = directory / ("aapt2.exe" if os.name == "nt" else "aapt2")
        d8_candidates = [directory / "d8.bat", directory / "d8"]
        if aapt2.exists() and any(candidate.exists() for candidate in d8_candidates):
            results.append((version, directory.name))
    return sorted(results, reverse=True)


def sdkmanager_candidates(sdk: Path) -> list[Path]:
    executable = "sdkmanager.bat" if os.name == "nt" else "sdkmanager"
    candidates = [sdk / "cmdline-tools" / "latest" / "bin" / executable]
    command_line_root = sdk / "cmdline-tools"
    if command_line_root.is_dir():
        versioned = sorted(
            (item for item in command_line_root.iterdir() if item.is_dir() and item.name != "latest"),
            key=lambda item: item.name,
            reverse=True,
        )
        candidates.extend(item / "bin" / executable for item in versioned)
    candidates.append(sdk / "tools" / "bin" / executable)
    return candidates


def find_sdkmanager(sdk: Path) -> Path | None:
    return next((candidate for candidate in sdkmanager_candidates(sdk) if candidate.exists()), None)


def install_sdk_packages(sdk: Path, java_home: Path, packages: list[str]) -> bool:
    manager = find_sdkmanager(sdk)
    if manager is None:
        return False

    print("Installing missing Android SDK components automatically:")
    for package in packages:
        print(f"  {package}")

    environment = os.environ.copy()
    environment["JAVA_HOME"] = str(java_home)
    environment["ANDROID_SDK_ROOT"] = str(sdk)
    command = [str(manager), f"--sdk_root={sdk}", *packages]
    result = subprocess.run(
        command,
        env=environment,
        input="y\n" * 100,
        text=True,
        check=False,
    )
    return result.returncode == 0


def resolve_sdk_components(sdk: Path, java_home: Path) -> str:
    platform_dir = sdk / "platforms" / f"android-{COMPILE_SDK}"
    packages_to_install: list[str] = []
    if not (platform_dir / "android.jar").exists():
        packages_to_install.append(f"platforms;android-{COMPILE_SDK}")

    versions = installed_build_tools(sdk)
    compatible = [item for item in versions if item[0] >= MINIMUM_COMPATIBLE_BUILD_TOOLS]
    build_tools = compatible[0][1] if compatible else None
    if build_tools is None:
        packages_to_install.append(f"build-tools;{PREFERRED_BUILD_TOOLS}")

    if packages_to_install:
        installed = install_sdk_packages(sdk, java_home, packages_to_install)
        if not installed:
            missing = ", ".join(packages_to_install)
            fail(
                "Missing Android SDK components and sdkmanager could not install them automatically: "
                f"{missing}. Install Android SDK Command-line Tools once, or install these packages in SDK Manager."
            )

    if not (platform_dir / "android.jar").exists():
        fail(f"Android SDK Platform {COMPILE_SDK} is still missing after installation.")

    versions = installed_build_tools(sdk)
    compatible = [item for item in versions if item[0] >= MINIMUM_COMPATIBLE_BUILD_TOOLS]
    if not compatible:
        fail(f"Android SDK Build-Tools {PREFERRED_BUILD_TOOLS} or newer are still missing after installation.")

    selected = compatible[0][1]
    if selected != PREFERRED_BUILD_TOOLS:
        print(f"Using installed Android SDK Build-Tools {selected} instead of {PREFERRED_BUILD_TOOLS}.")
    else:
        print(f"Using Android SDK Build-Tools {selected}.")
    return selected


def write_local_properties(sdk: Path) -> None:
    value = sdk.resolve().as_posix()
    (ROOT / "local.properties").write_text(f"sdk.dir={value}\n", encoding="utf-8")


def run_build(gradle: Path, java_home: Path, build_tools: str) -> Path:
    environment = os.environ.copy()
    environment["JAVA_HOME"] = str(java_home)
    command = [
        str(gradle),
        "--no-daemon",
        f"-PgeoBuildToolsVersion={build_tools}",
        "clean",
        "assembleDebug",
    ]
    print("Running:", " ".join(command))
    subprocess.run(command, cwd=ROOT, env=environment, check=True)
    apk = ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
    if not apk.exists():
        fail(f"Build completed but APK was not found: {apk}")
    return apk


def publish(apk: Path) -> None:
    dist = ROOT / "dist"
    dist.mkdir(exist_ok=True)
    output = dist / "GeoJoystick-debug.apk"
    shutil.copy2(apk, output)
    digest = hashlib.sha256(output.read_bytes()).hexdigest()
    (dist / "SHA256SUMS.txt").write_text(f"{digest}  {output.name}\n", encoding="utf-8")
    print(f"APK: {output}")
    print(f"SHA-256: {digest}")


def main() -> None:
    if platform.system() not in {"Windows", "Linux", "Darwin"}:
        fail(f"Unsupported platform: {platform.system()}")
    java_home = find_java_home()
    sdk = find_android_sdk()
    build_tools = resolve_sdk_components(sdk, java_home)
    write_local_properties(sdk)
    gradle = ensure_gradle()
    apk = run_build(gradle, java_home, build_tools)
    publish(apk)


if __name__ == "__main__":
    main()
