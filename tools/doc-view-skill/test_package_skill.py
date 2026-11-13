import hashlib
import importlib.util
import json
import subprocess
import tempfile
import unittest
from argparse import Namespace
from pathlib import Path
from zipfile import ZipFile


SCRIPT = Path(__file__).with_name("package_skill.py")
SPEC = importlib.util.spec_from_file_location("package_skill", SCRIPT)
package_skill = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(package_skill)


class SkillPackageTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="doc-view-skill-package-")
        self.root = Path(self.temp.name) / "repo"
        self.output = Path(self.temp.name) / "out"
        self.root.mkdir()
        self.git("init", "--quiet")
        self.git("config", "user.name", "Doc View Test")
        self.git("config", "user.email", "doc-view-test@example.invalid")
        self.git("remote", "add", "origin", "https://github.com/liuzhihang/doc-view.git")
        (self.root / "skills/doc-view").mkdir(parents=True)
        (self.root / "skills/doc-view/SKILL.md").write_text("---\nname: doc-view\ndescription: test\n---\n")
        (self.root / "src/main/resources/mcp").mkdir(parents=True)
        self.contract = b'{"contractVersion":1,"tools":[]}\n'
        (self.root / "src/main/resources/mcp/tool-contracts.json").write_bytes(self.contract)
        (self.root / "gradle.properties").write_text("pluginVersion=1.7.0\n")
        self.git("add", "skills", "src", "gradle.properties")
        self.git("commit", "--quiet", "-m", "fixture")
        self.commit = self.git("rev-parse", "HEAD")
        self.git("tag", "v1.7.0", self.commit)

    def tearDown(self):
        self.temp.cleanup()

    def git(self, *args):
        result = subprocess.run(["git", *args], cwd=self.root, text=True, check=True, stdout=subprocess.PIPE)
        return result.stdout.strip()

    def arguments(self, **overrides):
        values = dict(
            commit=self.commit,
            tag="v1.7.0",
            tool_contract_version=1,
            expected_tool_contract_fingerprint=hashlib.sha256(self.contract).hexdigest(),
            minimum_plugin_version="1.7.0",
            maximum_plugin_version_exclusive="2.0.0",
            output=str(self.output),
        )
        values.update(overrides)
        return Namespace(**values)

    def commit_additional_files(self, files):
        for path, data in files.items():
            target = self.root / "skills/doc-view" / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
        self.git("add", "skills/doc-view")
        self.git("commit", "--quiet", "-m", "additional Skill files")
        commit = self.git("rev-parse", "HEAD")
        self.git("tag", "v1.7.1", commit)
        return self.arguments(commit=commit, tag="v1.7.1")

    def test_archive_and_detached_manifest_are_deterministic_and_bound_to_tag(self):
        archive, manifest_path = package_skill.build(self.root, self.arguments())
        first_hash = hashlib.sha256(archive.read_bytes()).hexdigest()
        first_manifest = json.loads(manifest_path.read_text())
        second_archive, second_manifest = package_skill.build(self.root, self.arguments())
        self.assertEqual(first_hash, hashlib.sha256(second_archive.read_bytes()).hexdigest())
        self.assertEqual(first_manifest, json.loads(second_manifest.read_text()))
        self.assertEqual(self.commit, first_manifest["commitSha"])
        self.assertEqual("liuzhihang/doc-view", first_manifest["repository"])
        self.assertEqual("1.7.0", first_manifest["pluginVersionRange"]["minimumInclusive"])
        self.assertEqual("sha256:" + hashlib.sha256(self.contract).hexdigest(), first_manifest["toolContractFingerprint"])
        with ZipFile(archive) as zf:
            self.assertEqual(["doc-view/SKILL.md"], zf.namelist())
            self.assertEqual("doc-view", zf.namelist()[0].split("/")[0])
        self.assertEqual(first_hash, first_manifest["archive"]["sha256"])

    def test_packaging_rejects_working_tree_changes(self):
        (self.root / "untracked.txt").write_text("dirty")
        with self.assertRaisesRegex(package_skill.PackageError, "Working tree must be clean"):
            package_skill.build(self.root, self.arguments())

    def test_packaging_rejects_non_official_origin(self):
        self.git("remote", "set-url", "origin", "https://github.com/example/unrelated.git")
        with self.assertRaisesRegex(package_skill.PackageError, "official GitHub repository"):
            package_skill.build(self.root, self.arguments())

    def test_packaging_rejects_a_tag_for_another_commit(self):
        other = self.root / "other.txt"
        other.write_text("other")
        self.git("add", "other.txt")
        self.git("commit", "--quiet", "-m", "other")
        with self.assertRaisesRegex(package_skill.PackageError, "Release tag must resolve"):
            package_skill.build(self.root, self.arguments(commit=self.git("rev-parse", "HEAD")))

    def test_packaging_rejects_contract_version_or_fingerprint_mismatch(self):
        with self.assertRaisesRegex(package_skill.PackageError, "toolContractVersion"):
            package_skill.build(self.root, self.arguments(tool_contract_version=2))
        with self.assertRaisesRegex(package_skill.PackageError, "fingerprint"):
            package_skill.build(self.root, self.arguments(expected_tool_contract_fingerprint="0" * 64))

    def test_packaging_rejects_plugin_range_mismatch(self):
        with self.assertRaisesRegex(package_skill.PackageError, "outside the requested Skill range"):
            package_skill.build(self.root, self.arguments(minimum_plugin_version="1.8.0"))

    def test_packaging_rejects_symlink_entries(self):
        source = self.root / "skills/doc-view/linked-file"
        source.symlink_to("SKILL.md")
        self.git("add", "skills/doc-view/linked-file")
        self.git("commit", "--quiet", "-m", "symlink")
        commit = self.git("rev-parse", "HEAD")
        self.git("tag", "v1.7.1", commit)
        with self.assertRaisesRegex(package_skill.PackageError, "regular files"):
            package_skill.build(self.root, self.arguments(commit=commit, tag="v1.7.1"))

    def test_packaging_rejects_single_file_over_two_mib(self):
        args = self.commit_additional_files({"oversize.bin": b"x" * (2 * 1024 * 1024 + 1)})
        with self.assertRaisesRegex(package_skill.PackageError, "exceeds 2 MiB"):
            package_skill.build(self.root, args)

    def test_packaging_rejects_expanded_total_over_eight_mib(self):
        args = self.commit_additional_files({f"large-{i}.bin": b"x" * (1700 * 1024) for i in range(5)})
        with self.assertRaisesRegex(package_skill.PackageError, "exceeds 8 MiB"):
            package_skill.build(self.root, args)

    def test_packaging_rejects_more_than_sixty_four_files(self):
        args = self.commit_additional_files({f"extra-{i:02}.md": b"x" for i in range(64)})
        with self.assertRaisesRegex(package_skill.PackageError, "64 files"):
            package_skill.build(self.root, args)

    def test_packaging_rejects_archive_over_four_mib(self):
        args = self.commit_additional_files({f"large-{i}.bin": bytes(range(256)) * 6144 for i in range(3)})
        with self.assertRaisesRegex(package_skill.PackageError, "archive exceeds 4 MiB"):
            package_skill.build(self.root, args)
        self.assertEqual([], list(self.output.iterdir()))


if __name__ == "__main__":
    unittest.main()
