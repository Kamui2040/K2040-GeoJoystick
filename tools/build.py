#!/usr/bin/env python3
"""Bootstrap Gradle and build GeoJoystick APKs locally."""

from __future__ import annotations

import argparse
import getpass
import hashlib
import os
import platform
import re
import shutil
import subprocess
import sys
import urllib.request
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Literal, NoReturn

BuildMode = Literal["debug", "release", "all", "signed-release"]

GRADLE_VERSION = "8.13"
GRADLE_URL = f"https://services.gradle.org/distributions/gradle-{GRADLE_VERSION}-bin.zip"
GRADLE_SHA_URL = GRADLE_URL + ".sha256"
COMPILE_SDK = 35
PREFERRED_BUILD_TOOLS = "35.0.0"

ROOT = Path(__file__).resolve().parents[1]
CACHE_ROOT = Path(os.environ.get("LOCALAPPDATA", ROOT / ".tools")) / "K2040" / "GeoJoystick"
GRADLE_HOME = CACHE_ROOT / f"gradle-{GRADLE_VERSION}"

MAX_SIGNING_PASSWORD_LENGTH = 1024
EXPECTED_RELEASE_KEY_ALIAS = "geojoystick-release"
EXPECTED_RELEASE_KEYSTORE_FILE = "geojoystick-release.jks"
EXPECTED_RELEASE_KEYSTORE_SHA256 = (
    "1926785c9a40e39c29a7835534630e452e9a521414551d5cbe27ac307cfd1dc3"
)
EXPECTED_RELEASE_CERT_SHA1 = "9c83829cb0477a9bc66382a5ab9e01f243d824c4"
EXPECTED_RELEASE_CERT_SHA256 = (
    "e0a833050d7c8fce7ddce85b2a86561304456d87b67bd6be1577d8f657e16778"
)
RELEASE_KEYSTORE = ROOT.parent / "secrets" / EXPECTED_RELEASE_KEYSTORE_FILE


@dataclass(frozen=True)
class ReleaseSigningSession:
    store_file: Path
    key_alias: str
    store_password: str
    key_password: str


def fail(message: str) -> NoReturn:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def parse_args() -> BuildMode:
    parser = argparse.ArgumentParser(
        description="Build GeoJoystick with the verified local Gradle bootstrap."
    )
    group = parser.add_mutually_exclusive_group()
    group.add_argument(
        "--release",
        action="store_true",
        help="Run release lint and build the unsigned release APK.",
    )
    group.add_argument(
        "--all",
        action="store_true",
        help="Build debug and unsigned release APKs and run release lint.",
    )
    group.add_argument(
        "--signed-release",
        action="store_true",
        help=(
            "Run release lint, build the unsigned release APK, prompt for the "
            "release-key passwords, and create a verified v2-only signed APK."
        ),
    )
    arguments = parser.parse_args()
    if arguments.signed_release:
        return "signed-release"
    if arguments.all:
        return "all"
    if arguments.release:
        return "release"
    return "debug"


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


REQUIRED_JAVA_MAJOR = 17


def windows_java_home(machine: bool) -> str | None:
    if os.name != "nt":
        return None

    import winreg

    hive = winreg.HKEY_LOCAL_MACHINE if machine else winreg.HKEY_CURRENT_USER
    subkey = (
        r"SYSTEM\CurrentControlSet\Control\Session Manager\Environment"
        if machine
        else r"Environment"
    )

    try:
        with winreg.OpenKey(hive, subkey) as key:
            value, _ = winreg.QueryValueEx(key, "JAVA_HOME")
    except OSError:
        return None

    return str(value).strip() or None


def java_candidates() -> list[Path]:
    candidates: list[Path] = []

    def add(value: str | Path | None) -> None:
        if value is None:
            return
        text = os.path.expandvars(str(value).strip().strip('"'))
        if text:
            candidates.append(Path(text))

    add(os.environ.get("JAVA_HOME"))

    if os.name == "nt":
        add(windows_java_home(machine=False))
        add(windows_java_home(machine=True))

        program_files = Path(os.environ.get("ProgramFiles", r"C:\Program Files"))
        adoptium = program_files / "Eclipse Adoptium"
        if adoptium.is_dir():
            for candidate in sorted(adoptium.glob("jdk-*")):
                if candidate.is_dir():
                    add(candidate)

        add(program_files / "Android" / "Android Studio" / "jbr")
        add(program_files / "Android" / "Android Studio" / "jre")

    java_on_path = shutil.which("java")
    if java_on_path:
        add(Path(java_on_path).resolve().parent.parent)

    unique: list[Path] = []
    seen: set[str] = set()
    for candidate in candidates:
        normalized = os.path.normcase(str(candidate.resolve()))
        if normalized in seen:
            continue
        seen.add(normalized)
        unique.append(candidate.resolve())

    return unique


def java_version_details(candidate: Path) -> tuple[int | None, str]:
    java_name = "java.exe" if os.name == "nt" else "java"
    javac_name = "javac.exe" if os.name == "nt" else "javac"
    java = candidate / "bin" / java_name
    javac = candidate / "bin" / javac_name

    if not java.is_file() or not javac.is_file():
        return None, ""

    result = subprocess.run(
        [str(java), "-version"],
        capture_output=True,
        check=False,
    )
    version_text = (result.stderr + result.stdout).decode(
        "utf-8",
        errors="replace",
    )
    match = re.search(r'version "(?:(?:1\.)?)(\d+)', version_text)
    major = int(match.group(1)) if match else None
    identity = next(
        (line.strip() for line in version_text.splitlines() if line.strip()),
        "",
    )
    return major, identity


def find_java_home() -> Path:
    inspected: list[str] = []

    for candidate in java_candidates():
        major, identity = java_version_details(candidate)
        inspected.append(f"{candidate} -> {major if major is not None else 'invalid'}")
        if major != REQUIRED_JAVA_MAJOR:
            continue

        executable = candidate / "bin" / (
            "java.exe" if os.name == "nt" else "java"
        )
        print(f"Using JDK {REQUIRED_JAVA_MAJOR}: {candidate}")
        print(f"Java executable: {executable}")
        print(f"Java identity: {identity}")
        return candidate

    fail(
        f"JDK {REQUIRED_JAVA_MAJOR} was not found. "
        "Set JAVA_HOME to a complete JDK 17 installation. "
        f"Inspected: {', '.join(inspected) if inspected else '(none)'}"
    )


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
    environment["PATH"] = (
        str(java_home / "bin")
        + os.pathsep
        + environment.get("PATH", "")
    )
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
    preferred_installed = any(
        name == PREFERRED_BUILD_TOOLS for _, name in versions
    )
    if not preferred_installed:
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
    selected = next(
        (name for _, name in versions if name == PREFERRED_BUILD_TOOLS),
        None,
    )
    if selected is None:
        fail(
            f"Android SDK Build-Tools {PREFERRED_BUILD_TOOLS} "
            "are still missing after installation."
        )

    print(f"Using Android SDK Build-Tools {selected}.")
    return selected


def write_local_properties(sdk: Path) -> None:
    value = sdk.resolve().as_posix().replace(":", r"\:", 1)
    (ROOT / "local.properties").write_text(f"sdk.dir={value}\n", encoding="utf-8", newline="\n")



def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def normalize_fingerprint(value: str) -> str:
    normalized = re.sub(r"[^0-9A-Fa-f]", "", value).lower()
    if not normalized:
        fail("A certificate fingerprint was empty after normalization.")
    return normalized


def require_signing_password(value: str, label: str) -> str:
    if not value:
        fail(f"{label} must not be empty.")
    if len(value) > MAX_SIGNING_PASSWORD_LENGTH:
        fail(f"{label} exceeds the supported length limit.")
    if any(character in value for character in "\x00\r\n"):
        fail(f"{label} contains an unsupported control character.")
    return value


def collect_release_signing_session() -> ReleaseSigningSession:
    store_file = RELEASE_KEYSTORE
    if store_file.is_symlink() or not store_file.is_file():
        fail(f"Release keystore must be a regular non-symlink file: {store_file}")

    actual_store_hash = sha256_file(store_file)
    if actual_store_hash != EXPECTED_RELEASE_KEYSTORE_SHA256:
        fail(
            "Release keystore SHA-256 mismatch: "
            f"expected {EXPECTED_RELEASE_KEYSTORE_SHA256}, got {actual_store_hash}"
        )

    print(f"Release keystore: {store_file}")
    print(f"Release keystore SHA-256: {actual_store_hash}")
    print("Release signing passwords are read interactively and are not stored.")

    store_password = require_signing_password(
        getpass.getpass("Release-keystore password: "),
        "Release-keystore password",
    )
    key_password = getpass.getpass(
        "Private-key password (press Enter to reuse the keystore password): "
    )
    if not key_password:
        key_password = store_password
    else:
        key_password = require_signing_password(
            key_password,
            "Private-key password",
        )

    return ReleaseSigningSession(
        store_file=store_file,
        key_alias=EXPECTED_RELEASE_KEY_ALIAS,
        store_password=store_password,
        key_password=key_password,
    )

def run_captured(
    command: list[str],
    *,
    environment: dict[str, str],
    timeout_seconds: int,
) -> str:
    try:
        result = subprocess.run(
            command,
            cwd=ROOT,
            env=environment,
            capture_output=True,
            check=False,
            timeout=timeout_seconds,
        )
    except subprocess.TimeoutExpired:
        fail(f"Command timed out after {timeout_seconds} seconds: {command[0]}")

    output = (result.stdout + result.stderr).decode("utf-8", errors="replace")
    if result.returncode != 0:
        fail(
            f"Command failed with result code {result.returncode}: {command[0]}\n"
            f"{output}"
        )
    return output


def extract_keytool_fingerprint(output: str, algorithm: str, expected_length: int) -> str:
    match = re.search(
        rf"(?im)^\s*{re.escape(algorithm)}\s*:\s*([0-9a-f:]+)\s*$",
        output,
    )
    if match is None:
        fail(f"keytool output did not contain the {algorithm} certificate fingerprint.")
    fingerprint = normalize_fingerprint(match.group(1))
    if len(fingerprint) != expected_length:
        fail(f"keytool returned an invalid {algorithm} fingerprint length.")
    return fingerprint


def verify_release_keystore(
    signing: ReleaseSigningSession,
    java_home: Path,
) -> None:
    keytool = java_home / "bin" / ("keytool.exe" if os.name == "nt" else "keytool")
    if not keytool.is_file():
        fail(f"JDK keytool was not found: {keytool}")

    environment = os.environ.copy()
    environment["GEOJOYSTICK_STORE_PASSWORD"] = signing.store_password
    command = [
        str(keytool),
        "-list",
        "-v",
        "-keystore",
        str(signing.store_file),
        "-alias",
        signing.key_alias,
        "-storepass:env",
        "GEOJOYSTICK_STORE_PASSWORD",
    ]
    output = run_captured(command, environment=environment, timeout_seconds=60)

    sha1 = extract_keytool_fingerprint(output, "SHA1", 40)
    sha256 = extract_keytool_fingerprint(output, "SHA256", 64)
    if sha1 != EXPECTED_RELEASE_CERT_SHA1:
        fail(
            "Release certificate SHA-1 mismatch: "
            f"expected {EXPECTED_RELEASE_CERT_SHA1}, got {sha1}"
        )
    if sha256 != EXPECTED_RELEASE_CERT_SHA256:
        fail(
            "Release certificate SHA-256 mismatch: "
            f"expected {EXPECTED_RELEASE_CERT_SHA256}, got {sha256}"
        )

    print(f"Release key alias: {signing.key_alias}")
    print(f"Release certificate SHA-1: {sha1}")
    print(f"Release certificate SHA-256: {sha256}")
    print("Release signing identity: PASS")


def build_tool_path(sdk: Path, build_tools: str, tool: str) -> Path:
    if os.name == "nt":
        suffix = ".bat" if tool == "apksigner" else ".exe"
    else:
        suffix = ""
    path = sdk / "build-tools" / build_tools / f"{tool}{suffix}"
    if not path.is_file():
        fail(f"Android Build Tools executable was not found: {path}")
    return path


def extract_apksigner_fingerprint(
    output: str,
    algorithm: str,
    expected_length: int,
) -> str:
    match = re.search(
        rf"(?im)^.*certificate\s+{re.escape(algorithm)}\s+digest\s*:\s*"
        r"([0-9a-f:]+)\s*$",
        output,
    )
    if match is None:
        fail(f"apksigner output did not contain the {algorithm} certificate digest.")
    fingerprint = normalize_fingerprint(match.group(1))
    if len(fingerprint) != expected_length:
        fail(f"apksigner returned an invalid {algorithm} digest length.")
    return fingerprint


def extract_scheme_result(output: str, scheme: str) -> bool | None:
    match = re.search(
        rf"(?im)^\s*Verified using {re.escape(scheme)} scheme.*:\s*(true|false)\s*$",
        output,
    )
    if match is None:
        return None
    return match.group(1).lower() == "true"


def sign_release_apk(
    artifacts: list[tuple[str, Path]],
    signing: ReleaseSigningSession,
    sdk: Path,
    build_tools: str,
    java_home: Path,
) -> tuple[str, Path]:
    unsigned_matches = [
        path for name, path in artifacts if name == "GeoJoystick-release-unsigned.apk"
    ]
    if len(unsigned_matches) != 1:
        fail("Expected exactly one unsigned release APK before signing.")
    unsigned_apk = unsigned_matches[0]

    apksigner = build_tool_path(sdk, build_tools, "apksigner")
    zipalign = build_tool_path(sdk, build_tools, "zipalign")
    signed_apk = unsigned_apk.with_name("app-release-signed.apk")
    idsig = Path(f"{signed_apk}.idsig")
    signed_apk.unlink(missing_ok=True)
    idsig.unlink(missing_ok=True)

    unsigned_hash_before = sha256_file(unsigned_apk)
    environment = os.environ.copy()
    environment["JAVA_HOME"] = str(java_home)
    environment["PATH"] = (
        str(java_home / "bin") + os.pathsep + environment.get("PATH", "")
    )
    environment["GEOJOYSTICK_STORE_PASSWORD"] = signing.store_password
    environment["GEOJOYSTICK_KEY_PASSWORD"] = signing.key_password

    print("Signing release APK with the verified external release key.")
    sign_command = [
        str(apksigner),
        "sign",
        "--ks",
        str(signing.store_file),
        "--ks-key-alias",
        signing.key_alias,
        "--ks-pass",
        "env:GEOJOYSTICK_STORE_PASSWORD",
        "--key-pass",
        "env:GEOJOYSTICK_KEY_PASSWORD",
        "--v1-signing-enabled",
        "false",
        "--v2-signing-enabled",
        "true",
        "--v3-signing-enabled",
        "false",
        "--v4-signing-enabled",
        "false",
        "--out",
        str(signed_apk),
        str(unsigned_apk),
    ]
    run_captured(sign_command, environment=environment, timeout_seconds=120)

    if not signed_apk.is_file():
        fail(f"apksigner completed but the signed APK was not found: {signed_apk}")
    if idsig.exists():
        fail(f"Unexpected APK Signature Scheme v4 sidecar was created: {idsig}")

    unsigned_hash_after = sha256_file(unsigned_apk)
    if unsigned_hash_after != unsigned_hash_before:
        fail("The unsigned release APK changed during signing.")

    alignment_output = run_captured(
        [str(zipalign), "-c", "-P", "16", "4", str(signed_apk)],
        environment=environment,
        timeout_seconds=60,
    )
    if alignment_output.strip():
        print(alignment_output.strip())

    verification = run_captured(
        [str(apksigner), "verify", "--verbose", "--print-certs", str(signed_apk)],
        environment=environment,
        timeout_seconds=60,
    )
    sha1 = extract_apksigner_fingerprint(verification, "SHA-1", 40)
    sha256 = extract_apksigner_fingerprint(verification, "SHA-256", 64)
    if sha1 != EXPECTED_RELEASE_CERT_SHA1:
        fail(
            "Signed APK certificate SHA-1 mismatch: "
            f"expected {EXPECTED_RELEASE_CERT_SHA1}, got {sha1}"
        )
    if sha256 != EXPECTED_RELEASE_CERT_SHA256:
        fail(
            "Signed APK certificate SHA-256 mismatch: "
            f"expected {EXPECTED_RELEASE_CERT_SHA256}, got {sha256}"
        )

    required_schemes = {
        "v1": False,
        "v2": True,
        "v3": False,
        "v4": False,
    }
    for scheme, expected in required_schemes.items():
        actual = extract_scheme_result(verification, scheme)
        if actual is None:
            fail(f"apksigner did not report the {scheme} signing-scheme result.")
        if actual is not expected:
            fail(
                f"Unexpected {scheme} signing-scheme result: "
                f"expected {expected}, got {actual}"
            )

    v31 = extract_scheme_result(verification, "v3.1")
    if v31 is True:
        fail("APK Signature Scheme v3.1 was unexpectedly enabled.")

    print(f"Unsigned release SHA-256: {unsigned_hash_before}")
    print(f"Signed release certificate SHA-1: {sha1}")
    print(f"Signed release certificate SHA-256: {sha256}")
    print("Signed release alignment: PASS")
    print("Signed release schemes: v2 only")
    return ("GeoJoystick-release.apk", signed_apk)


def gradle_tasks(mode: BuildMode) -> list[str]:
    if mode in {"release", "signed-release"}:
        return ["clean", "lintRelease", "lintVitalRelease", "assembleRelease"]
    if mode == "all":
        return [
            "clean",
            "assembleDebug",
            "lintRelease",
            "lintVitalRelease",
            "assembleRelease",
        ]
    return ["clean", "assembleDebug"]


def expected_artifacts(mode: BuildMode) -> list[tuple[str, Path]]:
    artifacts: list[tuple[str, Path]] = []
    if mode in {"debug", "all"}:
        artifacts.append(
            (
                "GeoJoystick-debug.apk",
                ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk",
            )
        )
    if mode in {"release", "all", "signed-release"}:
        artifacts.append(
            (
                "GeoJoystick-release-unsigned.apk",
                ROOT
                / "app"
                / "build"
                / "outputs"
                / "apk"
                / "release"
                / "app-release-unsigned.apk",
            )
        )
    return artifacts


def verify_gradle_runtime(gradle: Path, java_home: Path) -> None:
    environment = os.environ.copy()
    environment["JAVA_HOME"] = str(java_home)
    environment["PATH"] = (
        str(java_home / "bin")
        + os.pathsep
        + environment.get("PATH", "")
    )
    command = [
        str(gradle),
        "--no-daemon",
        f"-Dorg.gradle.java.home={java_home}",
        "--version",
    ]
    result = subprocess.run(
        command,
        cwd=ROOT,
        env=environment,
        capture_output=True,
        check=False,
    )
    output = (result.stdout + result.stderr).decode(
        "utf-8",
        errors="replace",
    )
    if result.returncode != 0:
        fail(f"Gradle runtime verification failed:\n{output}")

    launcher = re.search(r"^Launcher JVM:\s+(\d+)", output, re.MULTILINE)
    daemon = re.search(r"^Daemon JVM:\s+(.+)$", output, re.MULTILINE)
    if launcher is None or int(launcher.group(1)) != REQUIRED_JAVA_MAJOR:
        fail(
            f"Gradle launcher is not using Java {REQUIRED_JAVA_MAJOR}:\n"
            f"{output}"
        )

    expected_home = os.path.normcase(str(java_home.resolve()))
    daemon_text = os.path.normcase(daemon.group(1)) if daemon else ""
    if expected_home not in daemon_text:
        fail(
            "Gradle daemon is not using the selected Java home:\n"
            f"{output}"
        )

    print(f"Gradle {launcher.group(0).strip()}")
    print(f"Gradle {daemon.group(0).strip()}")


def run_build(
    gradle: Path,
    java_home: Path,
    build_tools: str,
    mode: BuildMode,
) -> list[tuple[str, Path]]:
    verify_gradle_runtime(gradle, java_home)

    environment = os.environ.copy()
    environment["JAVA_HOME"] = str(java_home)
    environment["PATH"] = (
        str(java_home / "bin")
        + os.pathsep
        + environment.get("PATH", "")
    )
    command = [
        str(gradle),
        "--no-daemon",
        f"-Dorg.gradle.java.home={java_home}",
        f"-PgeoBuildToolsVersion={build_tools}",
        *gradle_tasks(mode),
    ]
    print("Running:", " ".join(command))
    subprocess.run(command, cwd=ROOT, env=environment, check=True)

    artifacts = expected_artifacts(mode)
    for _, artifact in artifacts:
        if not artifact.exists():
            fail(f"Build completed but expected APK was not found: {artifact}")
    return artifacts


def publish(artifacts: list[tuple[str, Path]]) -> None:
    dist = ROOT / "dist"
    dist.mkdir(exist_ok=True)

    known_outputs = [
        dist / "GeoJoystick-debug.apk",
        dist / "GeoJoystick-release-unsigned.apk",
        dist / "GeoJoystick-release.apk",
        dist / "SHA256SUMS.txt",
    ]
    for output in known_outputs:
        output.unlink(missing_ok=True)

    checksum_lines: list[str] = []
    for output_name, source in artifacts:
        output = dist / output_name
        shutil.copy2(source, output)
        digest = hashlib.sha256(output.read_bytes()).hexdigest()
        checksum_lines.append(f"{digest}  {output.name}")
        print(f"APK: {output}")
        print(f"SHA-256: {digest}")

    (dist / "SHA256SUMS.txt").write_text(
        "\n".join(checksum_lines) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def run_parser_self_test(java_home: Path) -> None:
    environment = os.environ.copy()
    environment["JAVA_HOME"] = str(java_home)
    environment["PATH"] = (
        str(java_home / "bin")
        + os.pathsep
        + environment.get("PATH", "")
    )
    command = [
        sys.executable,
        str(ROOT / "tools" / "test_location_link_parser.py"),
        "--java-home",
        str(java_home),
    ]
    print("Running:", " ".join(command))
    subprocess.run(command, cwd=ROOT, env=environment, check=True)


def main() -> None:
    if platform.system() not in {"Windows", "Linux", "Darwin"}:
        fail(f"Unsupported platform: {platform.system()}")

    mode = parse_args()
    print(f"Build mode: {mode}")

    java_home = find_java_home()
    run_parser_self_test(java_home)
    sdk = find_android_sdk()
    build_tools = resolve_sdk_components(sdk, java_home)
    write_local_properties(sdk)
    gradle = ensure_gradle()
    artifacts = run_build(gradle, java_home, build_tools, mode)

    if mode == "signed-release":
        signing = collect_release_signing_session()
        verify_release_keystore(signing, java_home)
        artifacts.append(
            sign_release_apk(
                artifacts,
                signing,
                sdk,
                build_tools,
                java_home,
            )
        )
    elif mode in {"release", "all"}:
        print("Release signing: not requested; unsigned release retained.")

    publish(artifacts)


if __name__ == "__main__":
    main()
