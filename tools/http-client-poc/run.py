#!/usr/bin/env python3
"""Build and optionally verify an isolated Java bridge probe for IDEA HTTP Client."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile


PLUGIN_ID = "org.docview.httpclient.poc"
PLUGIN_NAME = "doc-view-http-client-poc"
JAVA_PACKAGE = "org.docview.http.poc"
FIXED_ZIP_TIME = (2020, 1, 1, 0, 0, 0)
PLUGIN_DIRS = (
    "java",
    "Spring",
    "spring-mvc-impl",
    "restClient",
    "microservices-jvm",
    "microservices-plugin",
)


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def resolve_ide_home(raw: Path) -> Path:
    path = raw.expanduser().resolve()
    if path.suffix == ".app":
        path = path / "Contents"
    elif (path / "Contents" / "Resources" / "product-info.json").is_file():
        path = path / "Contents"
    if not any((path / name).is_file() for name in ("product-info.json", "Resources/product-info.json")):
        raise ValueError(f"selected IDE has no product-info.json: {path}")
    return path


def read_product_info(ide: Path) -> tuple[Path, dict]:
    for candidate in (ide / "Resources/product-info.json", ide / "product-info.json"):
        if candidate.is_file():
            return candidate, json.loads(candidate.read_text())
    raise ValueError(f"selected IDE has no product-info.json: {ide}")


def locate_java(java_home_arg: Path | None) -> tuple[Path, Path]:
    java_home = java_home_arg.expanduser().resolve() if java_home_arg else None
    if java_home is None and sys.platform == "darwin":
        result = subprocess.run(["/usr/libexec/java_home", "-v", "21"], capture_output=True, text=True)
        if result.returncode == 0:
            java_home = Path(result.stdout.strip())
    if java_home is not None:
        java = java_home / "bin/java"
        javac = java_home / "bin/javac"
    else:
        java = Path(shutil.which("java") or "")
        javac = Path(shutil.which("javac") or "")
    if not java.is_file() or not javac.is_file():
        raise ValueError("an existing JDK with java and javac is required (Java 21 preferred)")
    return java, javac


def escape_property(value: Path) -> str:
    return str(value).replace("\\", "\\\\").replace(" ", "\\ ").replace(":", "\\:")


def write_zip(path: Path, files: dict[str, bytes]) -> None:
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name in sorted(files):
            item = zipfile.ZipInfo(name, FIXED_ZIP_TIME)
            item.compress_type = zipfile.ZIP_DEFLATED
            item.create_system = 3
            item.external_attr = 0o100644 << 16
            archive.writestr(item, files[name], compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)


def build_classpath(ide: Path) -> list[Path]:
    roots = [ide / "lib"] + [ide / "plugins" / directory for directory in PLUGIN_DIRS]
    missing = [str(root) for root in roots if not root.is_dir()]
    if missing:
        raise ValueError("selected IDE is missing required platform/plugin directories: " + ", ".join(missing))
    jars = sorted({jar.resolve() for root in roots for jar in root.rglob("*.jar")})
    if not jars:
        raise ValueError("selected IDE contains no compile classpath jars")
    return jars


def discover_verifier(explicit: Path | None) -> Path:
    if explicit:
        candidate = explicit.expanduser().resolve()
        if candidate.is_file():
            return candidate
        raise ValueError(f"Plugin Verifier jar not found: {candidate}")
    cache = Path.home() / ".gradle/caches/modules-2/files-2.1/org.jetbrains.intellij.plugins/verifier-cli"
    jars = list(cache.glob("*/**/*-all.jar")) if cache.is_dir() else []
    if not jars:
        raise ValueError("Plugin Verifier CLI is not cached; pass --verifier-jar explicitly")

    def version_key(path: Path) -> tuple[int, ...]:
        match = re.fullmatch(r"(\d+(?:\.\d+)*)-all\.jar", path.name)
        return tuple(int(part) for part in match.group(1).split(".")) if match else (0,)

    return max(jars, key=version_key)


def run_logged(command: list[str | Path], log_path: Path, timeout: int) -> subprocess.CompletedProcess[str]:
    result = subprocess.run([str(arg) for arg in command], capture_output=True, text=True, timeout=timeout)
    log_path.write_text(result.stdout + result.stderr)
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--ide-home", type=Path, help="Exact IDE home or macOS .app/Contents")
    parser.add_argument("--output-dir", type=Path, help="New output directory outside this repository")
    parser.add_argument("--java-home", type=Path, help="Existing JDK; defaults to Java 21 on macOS or PATH")
    parser.add_argument("--verify", action="store_true", help="Run cached Plugin Verifier against the selected IDE")
    parser.add_argument("--verifier-jar", type=Path, help="Explicit cached Plugin Verifier all-in-one jar")
    args = parser.parse_args()
    if args.ide_home is None or args.output_dir is None:
        parser.error("--ide-home and --output-dir are required unless --help is used")

    repo = Path(__file__).resolve().parents[2]
    try:
        ide = resolve_ide_home(args.ide_home)
        output = args.output_dir.expanduser().resolve()
        if output == repo or repo in output.parents:
            raise ValueError("output-dir must be outside this repository")
        if output.exists():
            raise ValueError(f"output-dir must not exist; choose a fresh directory: {output}")
        product_path, product = read_product_info(ide)
        if product.get("buildNumber") != "261.22158.277":
            print(f"WARNING: target build is 261.22158.277; selected build is {product.get('buildNumber')}", file=sys.stderr)
        java, javac = locate_java(args.java_home)
        classpath_jars = build_classpath(ide)

        output.mkdir(parents=True)
        classes = output / "classes"
        classes.mkdir()
        profile = output / "profile"
        for name in ("config", "system", "log", "plugins"):
            (profile / name).mkdir(parents=True)
        properties_path = profile / "idea.properties"
        properties = "\n".join(
            f"idea.{key}.path={escape_property(profile / value)}"
            for key, value in (
                ("config", "config"),
                ("system", "system"),
                ("log", "log"),
                ("plugins", "plugins"),
            )
        ) + "\n"
        properties_path.write_text(properties)

        source_root = Path(__file__).resolve().parent
        source_files = sorted((source_root / "extension-src").rglob("*.java"))
        descriptor = source_root / "extension-resources/META-INF/plugin.xml"
        if not source_files or not descriptor.is_file():
            raise ValueError("probe Java source or META-INF/plugin.xml is missing")

        compile_command = [
            javac,
            "--release",
            "21",
            "-parameters",
            "-proc:none",
            "-cp",
            os.pathsep.join(str(path) for path in classpath_jars),
            "-d",
            classes,
            *source_files,
        ]
        compile_result = run_logged(compile_command, output / "compile.log", 120)
        report = {
            "plugin_id": PLUGIN_ID,
            "plugin_name": PLUGIN_NAME,
            "ide_build": product.get("buildNumber"),
            "ide_product_code": product.get("productCode"),
            "ide_home": str(ide),
            "product_info": str(product_path),
            "java": str(java),
            "javac": str(javac),
            "java_target": 21,
            "source_sha256": {str(path.relative_to(source_root)): sha256(path) for path in source_files},
            "descriptor_sha256": sha256(descriptor),
            "classpath_jar_count": len(classpath_jars),
            "compile_command": [str(arg) for arg in compile_command],
            "compile_exit": compile_result.returncode,
            "package": None,
            "package_sha256": None,
            "verifier": None,
            "profile": {
                "idea_properties": str(properties_path),
                "config": str(profile / "config"),
                "system": str(profile / "system"),
                "log": str(profile / "log"),
                "plugins": str(profile / "plugins"),
                "test_plugin_installed": False,
            },
        }

        if compile_result.returncode == 0:
            plugin_jar = output / f"{PLUGIN_NAME}.jar"
            jar_entries = {
                "META-INF/MANIFEST.MF": b"Manifest-Version: 1.0\r\n\r\n",
                "META-INF/plugin.xml": descriptor.read_bytes(),
            }
            for class_file in sorted(classes.rglob("*.class")):
                jar_entries[class_file.relative_to(classes).as_posix()] = class_file.read_bytes()
            write_zip(plugin_jar, jar_entries)
            plugin_zip = output / f"{PLUGIN_NAME}.zip"
            write_zip(plugin_zip, {f"{PLUGIN_NAME}/lib/{plugin_jar.name}": plugin_jar.read_bytes()})
            with zipfile.ZipFile(plugin_jar) as archive:
                packaged_descriptor = ET.fromstring(archive.read("META-INF/plugin.xml"))
                class_entries = [name for name in archive.namelist() if name.endswith(".class")]
            if packaged_descriptor.findtext("id") != PLUGIN_ID or not class_entries:
                raise RuntimeError("packed probe failed descriptor/class verification")
            report["package"] = str(plugin_zip)
            report["package_sha256"] = sha256(plugin_zip)
            report["package_bytes"] = plugin_zip.stat().st_size
            report["package_entries"] = [f"{PLUGIN_NAME}/lib/{plugin_jar.name}"]
            report["packaged_classes"] = class_entries

            if args.verify:
                verifier = discover_verifier(args.verifier_jar)
                verification_dir = output / "verifier-reports"
                verify_command = [
                    java,
                    "-jar",
                    verifier,
                    "check-plugin",
                    plugin_zip,
                    ide,
                    "-offline",
                    "-verification-reports-formats",
                    "plain,html",
                    "-verification-reports-dir",
                    verification_dir,
                ]
                verify_result = run_logged(verify_command, output / "verify.log", 600)
                verify_output = (verify_result.stdout or "") + (verify_result.stderr or "")
                invalid_plugin_markers = (
                    "The following files specified for the verification are not valid plugins:",
                    "Invalid plugin descriptor",
                )
                scheduled_match = re.search(r"Scheduled verifications \((\d+)\):", verify_output)
                finished_match = re.search(r"Finished (\d+) of (\d+) verifications", verify_output)
                scheduled = int(scheduled_match.group(1)) if scheduled_match else 0
                finished = (int(finished_match.group(1)), int(finished_match.group(2))) if finished_match else (0, 0)
                incompatible_api_usages = {
                    "deprecated": bool(re.search(r"Deprecated API usages\s*\(\s*[1-9]\d*\s*\)", verify_output, re.I)),
                    "internal": bool(re.search(r"Internal API usages\s*\(\s*[1-9]\d*\s*\)", verify_output, re.I)),
                    "override_only": bool(re.search(r"override.?only", verify_output, re.I)),
                }
                compatible = f"against {PLUGIN_ID}:0.1.0: Compatible." in verify_output
                verifier_accepted = (
                    verify_result.returncode == 0
                    and scheduled > 0
                    and finished == (scheduled, scheduled)
                    and compatible
                    and not any(marker in verify_output for marker in invalid_plugin_markers)
                    and not any(incompatible_api_usages.values())
                )
                report["verifier"] = {
                    "jar": str(verifier),
                    "command": [str(arg) for arg in verify_command],
                    "exit": verify_result.returncode,
                    "accepted": verifier_accepted,
                    "scheduled": scheduled,
                    "finished": {"completed": finished[0], "total": finished[1]},
                    "compatible": compatible,
                    "incompatible_api_usages": incompatible_api_usages,
                    "reports": str(verification_dir),
                }

        (output / "build-report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
        print(json.dumps({
            "ide_build": report["ide_build"],
            "compile_exit": report["compile_exit"],
            "package": report["package"],
            "package_sha256": report["package_sha256"],
            "verifier_exit": report["verifier"]["exit"] if report["verifier"] else None,
            "profile_properties": str(properties_path),
            "report": str(output / "build-report.json"),
        }, ensure_ascii=False))
        if report["compile_exit"]:
            return report["compile_exit"]
        if report["verifier"] and not report["verifier"]["accepted"]:
            return report["verifier"]["exit"] or 1
        return 0
    except (OSError, ValueError, subprocess.TimeoutExpired, zipfile.BadZipFile, ET.ParseError) as error:
        parser.error(str(error))
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
