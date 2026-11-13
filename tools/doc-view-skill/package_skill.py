#!/usr/bin/env python3
"""Create the deterministic Skill archive and detached manifest for one tagged commit."""

from __future__ import annotations

import argparse
import hashlib
import hmac
import json
import re
import subprocess
import sys
from urllib.parse import urlsplit
import zipfile
from pathlib import Path, PurePosixPath

REPOSITORY = "liuzhihang/doc-view"
SOURCE_ROOT = "skills/doc-view"
ARCHIVE_ROOT = "doc-view"
TOOL_CONTRACT = "src/main/resources/mcp/tool-contracts.json"
PLUGIN_PROPERTIES = "gradle.properties"
MAX_ARCHIVE_BYTES = 4 * 1024 * 1024
MAX_EXPANDED_BYTES = 8 * 1024 * 1024
MAX_FILE_BYTES = 2 * 1024 * 1024
MAX_FILES = 64
ZIP_TIME = (1980, 1, 1, 0, 0, 0)
VERSION = re.compile(r"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$")
SHA256 = re.compile(r"^[0-9a-f]{64}$")


class PackageError(ValueError):
    pass


def git(root: Path, *args: str, check: bool = True) -> str:
    result = subprocess.run(
        ["git", *args], cwd=root, text=True, stdout=subprocess.PIPE,
        stderr=subprocess.PIPE, check=False,
    )
    if check and result.returncode:
        raise PackageError(result.stderr.strip() or f"git {' '.join(args)} failed")
    return result.stdout.strip()


def semver(value: str, label: str) -> tuple[int, int, int]:
    match = VERSION.fullmatch(value)
    if not match:
        raise PackageError(f"{label} must be a stable x.y.z version: {value}")
    return tuple(map(int, match.groups()))


def read_commit_file(root: Path, commit: str, path: str) -> bytes:
    result = subprocess.run(
        ["git", "show", f"{commit}:{path}"], cwd=root,
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False,
    )
    if result.returncode:
        raise PackageError(f"Required file is absent from tagged commit: {path}")
    return result.stdout


def properties(raw: bytes) -> dict[str, str]:
    found = {}
    for line in raw.decode("utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith(("#", "!")):
            continue
        if "=" in line:
            key, value = line.split("=", 1)
            found[key.strip()] = value.strip()
    return found


def source_files(root: Path, commit: str) -> list[tuple[str, bytes]]:
    raw = subprocess.check_output(
        ["git", "ls-tree", "-r", "-z", "--full-tree", commit, "--", SOURCE_ROOT],
        cwd=root,
    )
    files: list[tuple[str, bytes]] = []
    seen_casefolded: set[str] = set()
    total = 0
    for record in raw.split(b"\0"):
        if not record:
            continue
        metadata, path_bytes = record.split(b"\t", 1)
        mode, object_type, object_sha = metadata.decode("ascii").split()
        path = path_bytes.decode("utf-8")
        prefix = SOURCE_ROOT + "/"
        if not path.startswith(prefix):
            raise PackageError(f"Unexpected path in Skill tree: {path}")
        relative = path[len(prefix):]
        parts = PurePosixPath(relative).parts
        if not parts or any(part in ("", ".", "..") for part in parts):
            raise PackageError(f"Unsafe Skill path: {relative}")
        if mode != "100644" or object_type != "blob":
            raise PackageError(f"Skill packages only allow regular files: {relative}")
        folded = relative.casefold()
        if folded in seen_casefolded:
            raise PackageError(f"Duplicate/case-colliding Skill path: {relative}")
        seen_casefolded.add(folded)
        content = subprocess.check_output(["git", "cat-file", "blob", object_sha], cwd=root)
        if len(content) > MAX_FILE_BYTES:
            raise PackageError(f"Skill file exceeds 2 MiB: {relative}")
        total += len(content)
        files.append((relative, content))
        if len(files) > MAX_FILES:
            raise PackageError("Skill package exceeds 64 files")
    if not any(path == "SKILL.md" for path, _ in files):
        raise PackageError("skills/doc-view/SKILL.md is required")
    if total > MAX_EXPANDED_BYTES:
        raise PackageError("Expanded Skill package exceeds 8 MiB")
    return sorted(files, key=lambda item: item[0].encode("utf-8"))


def stable_tag(root: Path, tag: str, expected_commit: str) -> None:
    if not re.fullmatch(r"v[0-9]+\.[0-9]+\.[0-9]+", tag):
        raise PackageError("Release tag must have the form vX.Y.Z")
    actual = git(root, "rev-parse", f"{tag}^{{commit}}", check=False)
    if not actual:
        raise PackageError(f"Release tag does not exist locally: {tag}")
    if actual != expected_commit:
        raise PackageError("Release tag must resolve to the selected commit")


def require_official_origin(root: Path) -> None:
    origin = git(root, "remote", "get-url", "origin", check=False)
    if origin.startswith("git@github.com:"):
        repository = origin.removeprefix("git@github.com:")
    else:
        parsed = urlsplit(origin)
        if parsed.scheme not in {"https", "ssh"} or parsed.hostname != "github.com":
            raise PackageError("Packaging requires origin to be the official GitHub repository")
        if parsed.scheme == "https" and parsed.username is not None:
            raise PackageError("Packaging origin URL must not contain credentials")
        if parsed.scheme == "ssh" and parsed.username not in {"git", None}:
            raise PackageError("Packaging origin URL must use the standard GitHub SSH account")
        repository = parsed.path.lstrip("/")
    repository = repository.removesuffix(".git").rstrip("/")
    if repository != REPOSITORY:
        raise PackageError("Packaging requires origin to be the official GitHub repository")


def build(root: Path, args: argparse.Namespace) -> tuple[Path, Path]:
    commit = git(root, "rev-parse", f"{args.commit}^{{commit}}")
    if not re.fullmatch(r"[0-9a-f]{40,64}", commit):
        raise PackageError("Unable to resolve an immutable commit SHA")
    if git(root, "status", "--porcelain"):
        raise PackageError("Working tree must be clean when packaging a tagged Skill")
    require_official_origin(root)
    tag_version = args.tag.removeprefix("v")
    semver(tag_version, "release tag")
    semver(args.minimum_plugin_version, "minimum plugin version")
    semver(args.maximum_plugin_version_exclusive, "maximum plugin version")
    if semver(args.minimum_plugin_version, "minimum plugin version") >= semver(
        args.maximum_plugin_version_exclusive, "maximum plugin version"
    ):
        raise PackageError("Plugin compatibility range must be non-empty")
    stable_tag(root, args.tag, commit)

    plugin = properties(read_commit_file(root, commit, PLUGIN_PROPERTIES))
    plugin_version = plugin.get("pluginVersion", "")
    semver(plugin_version, "tagged pluginVersion")
    if not (
        semver(args.minimum_plugin_version, "minimum plugin version")
        <= semver(plugin_version, "tagged pluginVersion")
        < semver(args.maximum_plugin_version_exclusive, "maximum plugin version")
    ):
        raise PackageError("Tagged Doc View plugin version is outside the requested Skill range")

    contract_bytes = read_commit_file(root, commit, TOOL_CONTRACT)
    try:
        contract = json.loads(contract_bytes)
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise PackageError("Tagged MCP tool contract is not valid JSON") from error
    if contract.get("contractVersion") != args.tool_contract_version:
        raise PackageError("Requested toolContractVersion does not match the tagged contract")
    fingerprint = hashlib.sha256(contract_bytes).hexdigest()
    if args.expected_tool_contract_fingerprint and not hmac.compare_digest(
        fingerprint, args.expected_tool_contract_fingerprint
    ):
        raise PackageError("Tagged tool contract fingerprint does not match the expected value")

    files = source_files(root, commit)
    out = Path(args.output).resolve()
    archive_path = out / f"doc-view-skill-{tag_version}.zip"
    manifest_path = out / f"doc-view-skill-{tag_version}.manifest.json"
    out.mkdir(parents=True, exist_ok=True)
    manifest = {
        "formatVersion": 1,
        "skillVersion": tag_version,
        "releaseTag": args.tag,
        "repository": REPOSITORY,
        "commitSha": commit,
        "sourcePath": SOURCE_ROOT + "/",
        "archiveRoot": ARCHIVE_ROOT + "/",
        "toolContractVersion": args.tool_contract_version,
        "toolContractFingerprint": "sha256:" + fingerprint,
        "pluginVersionRange": {
            "minimumInclusive": args.minimum_plugin_version,
            "maximumExclusive": args.maximum_plugin_version_exclusive,
        },
        "files": [
            {"path": name, "size": len(content), "sha256": hashlib.sha256(content).hexdigest()}
            for name, content in files
        ],
    }
    with zipfile.ZipFile(archive_path, "w", compression=zipfile.ZIP_STORED, strict_timestamps=True) as zf:
        for name, content in files:
            info = zipfile.ZipInfo(f"{ARCHIVE_ROOT}/{name}", ZIP_TIME)
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_STORED
            info.flag_bits |= 0x800
            zf.writestr(info, content)
    archive_hash = hashlib.sha256(archive_path.read_bytes()).hexdigest()
    manifest["archive"] = {
        "file": archive_path.name,
        "size": archive_path.stat().st_size,
        "sha256": archive_hash,
    }
    manifest_bytes = (json.dumps(manifest, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8")
    manifest_path.write_bytes(manifest_bytes)
    if archive_path.stat().st_size > MAX_ARCHIVE_BYTES:
        archive_path.unlink(missing_ok=True)
        manifest_path.unlink(missing_ok=True)
        raise PackageError("Skill archive exceeds 4 MiB")
    return archive_path, manifest_path


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", default=".", help="Clean Doc View git checkout")
    parser.add_argument("--commit", required=True, help="Immutable commit SHA containing source and MCP contract")
    parser.add_argument("--tag", required=True, help="Stable Git tag resolving to --commit")
    parser.add_argument("--tool-contract-version", type=int, required=True)
    parser.add_argument("--expected-tool-contract-fingerprint", help="Optional SHA-256 hex from verified package contract")
    parser.add_argument("--minimum-plugin-version", required=True)
    parser.add_argument("--maximum-plugin-version-exclusive", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    try:
        archive, manifest = build(Path(args.repo).resolve(), args)
    except (PackageError, OSError, subprocess.CalledProcessError, UnicodeDecodeError) as error:
        print(f"Skill packaging failed: {error}", file=sys.stderr)
        return 2
    print(json.dumps({"archive": str(archive), "manifest": str(manifest)}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
