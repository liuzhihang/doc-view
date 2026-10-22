#!/usr/bin/env python3
"""Compile/run the isolated Java probe against one explicitly selected IDE."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile


def run(command, output):
    result = subprocess.run([str(arg) for arg in command], capture_output=True, text=True, timeout=60)
    output.write_text(result.stdout + result.stderr)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--ide-home", required=True, type=Path, help="IDE home or macOS .app/Contents")
    parser.add_argument("--output-dir", required=True, type=Path, help="New, empty directory outside this repository")
    parser.add_argument("--java-home", type=Path, help="Existing JDK with javac (defaults to existing Java 21 on macOS or PATH)")
    args = parser.parse_args()
    ide = args.ide_home.resolve()
    if (ide / "Contents").is_dir():
        ide /= "Contents"
    output = args.output_dir.resolve()
    repo = Path(__file__).resolve().parents[2]
    if output == repo or repo in output.parents or output.exists():
        parser.error("output-dir must be a new directory outside the repository")
    mcp = ide / "plugins/mcpserver/lib/mcpserver.jar"
    if not mcp.is_file():
        parser.error("selected IDE has no bundled mcpserver.jar")
    product = next((p for p in [ide / "product-info.json", ide / "Resources/product-info.json"] if p.is_file()), None)
    if product is None:
        parser.error("selected IDE has no product-info.json")
    java_home = args.java_home
    if java_home is None and sys.platform == "darwin":
        found = subprocess.run(["/usr/libexec/java_home", "-v", "21"], capture_output=True, text=True)
        if found.returncode == 0:
            java_home = Path(found.stdout.strip())
    javac = java_home / "bin/javac" if java_home else shutil.which("javac")
    java = java_home / "bin/java" if java_home else shutil.which("java")
    if not javac or not java or not Path(javac).is_file() or not Path(java).is_file():
        parser.error("an existing JDK with java and javac is required")

    output.mkdir(parents=True)
    classes = output / "classes"
    classes.mkdir()
    jars = sorted((ide / "lib").rglob("*.jar")) + sorted((ide / "plugins/mcpserver/lib").rglob("*.jar"))
    classpath = os.pathsep.join(str(p) for p in jars)
    source = Path(__file__).with_name("src") / "DocViewMcpProbe.java"
    with zipfile.ZipFile(mcp) as archive:
        descriptor = ET.fromstring(archive.read("META-INF/plugin.xml"))
    report = {"ide": json.loads(product.read_text()), "mcp_version": descriptor.findtext("version"),
              "source_sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
              "mcp_jar_sha256": hashlib.sha256(mcp.read_bytes()).hexdigest(),
              "java_version": subprocess.run([str(java), "-version"], capture_output=True, text=True).stderr.strip(),
              "classpath_jar_count": len(jars), "scope": "standalone-jvm-only", "stages": []}
    compiled = run([javac, "--release", "21", "-parameters", "-cp", classpath, "-d", classes, source], output / "compile.log")
    report["compile_exit"] = compiled.returncode
    if compiled.returncode == 0:
        executed = run([java, "-cp", str(classes) + os.pathsep + classpath, "DocViewMcpProbe"], output / "run.log")
        report["run_exit"] = executed.returncode
        report["stages"] = [json.loads(line[4:]) for line in executed.stdout.splitlines() if line.startswith("POC\t")]
    (output / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2))
    print(json.dumps({"build": report["ide"].get("buildNumber"), "compile_exit": report["compile_exit"],
                      "run_exit": report.get("run_exit"), "stages": report["stages"], "report": str(output / "report.json")}, ensure_ascii=False))
    return report["compile_exit"] or report.get("run_exit", 1)


if __name__ == "__main__":
    raise SystemExit(main())
