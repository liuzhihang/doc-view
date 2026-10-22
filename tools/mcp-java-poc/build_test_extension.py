#!/usr/bin/env python3
"""Build a deterministic, test-only IntelliJ MCP extension package and empty profile."""
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


PLUGIN_ID = "org.docview.mcp.java.poc"
MCP_PLUGIN_ID = "com.intellij.mcpServer"
PLUGIN_NAME = "doc-view-mcp-java-poc"
CLASS_NAME = "org.docview.mcp.poc.DocViewMcpTestToolset"
FIXED_ZIP_TIME = (2020, 1, 1, 0, 0, 0)


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(command, log_path):
    result = subprocess.run([str(arg) for arg in command], capture_output=True, text=True, timeout=120)
    log_path.write_text(result.stdout + result.stderr)
    return result


def write_zip(path, files):
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name in sorted(files):
            item = zipfile.ZipInfo(name, FIXED_ZIP_TIME)
            item.compress_type = zipfile.ZIP_DEFLATED
            item.create_system = 3
            item.external_attr = 0o100644 << 16
            archive.writestr(item, files[name], compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)


def escape_property(value):
    return str(value).replace("\\", "\\\\").replace(" ", "\\ ").replace(":", "\\:")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--ide-home", required=True, type=Path, help="Exact existing IDE home or macOS .app/Contents")
    parser.add_argument("--output-dir", required=True, type=Path, help="New output directory outside this repository")
    parser.add_argument("--java-home", type=Path, help="Existing JDK; Java 21 is preferred, otherwise use PATH")
    args = parser.parse_args()

    ide = args.ide_home.resolve()
    if (ide / "Contents").is_dir():
        ide /= "Contents"
    output = args.output_dir.resolve()
    repo = Path(__file__).resolve().parents[2]
    if output == repo or repo in output.parents or output.exists():
        parser.error("output-dir must be a new directory outside this repository")

    mcp_jar = ide / "plugins/mcpserver/lib/mcpserver.jar"
    product_info = next((path for path in (ide / "product-info.json", ide / "Resources/product-info.json") if path.is_file()), None)
    script_dir = Path(__file__).resolve().parent
    descriptor_source = script_dir / "extension-resources/META-INF/plugin.xml"
    java_source = script_dir / "extension-src/org/docview/mcp/poc/DocViewMcpTestToolset.java"
    if not mcp_jar.is_file() or product_info is None:
        parser.error("selected IDE must contain product-info.json and plugins/mcpserver/lib/mcpserver.jar")

    with zipfile.ZipFile(mcp_jar) as archive:
        mcp_descriptor = ET.fromstring(archive.read("META-INF/plugin.xml"))
    actual_mcp_id = mcp_descriptor.findtext("id")
    mcp_toolset_ep = mcp_descriptor.find(".//extensionPoints/extensionPoint[@name='mcpToolset']")
    if actual_mcp_id != MCP_PLUGIN_ID or mcp_toolset_ep is None:
        parser.error("selected IDE MCP descriptor does not expose the expected mcpToolset extension point")

    plugin_descriptor = ET.parse(descriptor_source).getroot()
    plugin_id = plugin_descriptor.findtext("id")
    dependencies = {node.text for node in plugin_descriptor.findall("depends")}
    extension = plugin_descriptor.find(".//extensions/mcpServer.mcpToolset")
    if plugin_id != PLUGIN_ID or MCP_PLUGIN_ID not in dependencies or extension is None:
        parser.error("test plugin descriptor does not match the isolated MCP extension contract")
    if extension.attrib.get("implementation") != CLASS_NAME:
        parser.error("test plugin descriptor points to an unexpected implementation")

    java_home = args.java_home
    if java_home is None and sys.platform == "darwin":
        result = subprocess.run(["/usr/libexec/java_home", "-v", "21"], capture_output=True, text=True)
        if result.returncode == 0:
            java_home = Path(result.stdout.strip())
    javac = java_home / "bin/javac" if java_home else shutil.which("javac")
    if not javac or not Path(javac).is_file():
        parser.error("an existing javac is required")

    output.mkdir(parents=True)
    classes = output / "classes"
    classes.mkdir()
    platform_jars = sorted((ide / "lib").rglob("*.jar"))
    mcp_jars = sorted((ide / "plugins/mcpserver/lib").rglob("*.jar"))
    compile_classpath = os.pathsep.join(str(path) for path in platform_jars + mcp_jars)
    compile_result = run(
        [javac, "--release", "21", "-parameters", "-cp", compile_classpath, "-d", classes, java_source],
        output / "compile.log",
    )

    profile = output / "profile"
    for name in ("config", "system", "log", "plugins"):
        (profile / name).mkdir(parents=True)
    properties_path = profile / "idea.properties"
    properties = "\n".join(
        f"idea.{key}.path={escape_property(profile / value)}"
        for key, value in (("config", "config"), ("system", "system"), ("log", "log"), ("plugins", "plugins"))
    ) + "\n"
    properties_path.write_text(properties)

    plugin_zip = output / f"{PLUGIN_NAME}.zip"
    report_path = output / "build-report.json"
    report = {
        "plugin_id": plugin_id,
        "plugin_name": plugin_descriptor.findtext("name"),
        "version": plugin_descriptor.findtext("version"),
        "mcp_plugin_id": actual_mcp_id,
        "mcp_extension_point": mcp_toolset_ep.attrib,
        "ide_build": json.loads(product_info.read_text()).get("buildNumber"),
        "ide_home": str(ide),
        "java_compiler": str(javac),
        "java_target": 21,
        "source_sha256": sha256(java_source),
        "descriptor_sha256": sha256(descriptor_source),
        "mcp_jar_sha256": sha256(mcp_jar),
        "compile_exit": compile_result.returncode,
        "package_sha256": None,
        "package_bytes": None,
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
        jar_entries = {
            "META-INF/MANIFEST.MF": b"Manifest-Version: 1.0\r\n\r\n",
            "META-INF/plugin.xml": descriptor_source.read_bytes(),
        }
        for class_file in sorted(classes.rglob("*.class")):
            jar_entries[class_file.relative_to(classes).as_posix()] = class_file.read_bytes()
        plugin_jar = output / f"{PLUGIN_NAME}.jar"
        write_zip(plugin_jar, jar_entries)
        package_entries = {f"{PLUGIN_NAME}/lib/{plugin_jar.name}": plugin_jar.read_bytes()}
        write_zip(plugin_zip, package_entries)
        with zipfile.ZipFile(plugin_zip) as package:
            packaged_jar = package.read(f"{PLUGIN_NAME}/lib/{plugin_jar.name}")
        with zipfile.ZipFile(plugin_jar) as plugin:
            packaged_descriptor = ET.fromstring(plugin.read("META-INF/plugin.xml"))
            packaged_class = CLASS_NAME.replace(".", "/") + ".class"
            class_present = packaged_class in plugin.namelist()
        if packaged_descriptor.findtext("id") != PLUGIN_ID or not class_present:
            raise RuntimeError("packaged plugin failed descriptor/class verification")
        report["packaged_class"] = packaged_class
        report["package_sha256"] = sha256(plugin_zip)
        report["package_bytes"] = plugin_zip.stat().st_size
        report["descriptor_embedded"] = True
        report["plugin_jar_sha256"] = hashlib.sha256(packaged_jar).hexdigest()
        report["package_entries"] = [f"{PLUGIN_NAME}/lib/{plugin_jar.name}"]
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps({"ide_build": report["ide_build"], "compile_exit": report["compile_exit"],
                      "package_sha256": report["package_sha256"], "package": str(plugin_zip),
                      "profile_properties": str(properties_path), "report": str(report_path)}, ensure_ascii=False))
    return compile_result.returncode


if __name__ == "__main__":
    raise SystemExit(main())
