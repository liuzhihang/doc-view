package com.liuzhihang.doc.view.service.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public final class GitHubSkillReleaseSourceTest {
    private static final String COMMIT = "0123456789012345678901234567890123456789";
    private static final String TREE_SHA = "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";

    @Test
    public void verifiesEveryManifestFileAgainstTheTaggedGitTree() throws Exception {
        Map<String, byte[]> files = files("skill text", "guide text");
        SkillFixtureFactory.Fixture fixture = SkillFixtureFactory.fixture("1.7.0", files);

        GitHubSkillReleaseSource.verifyArchiveMatchesCommit(
                fixture.manifest(), fixture.archive(), commit(COMMIT, TREE_SHA), tree(TREE_SHA, files));
    }

    @Test
    public void rejectsManifestWhoseSelfDeclaredHashesDoNotMatchTheCommit() throws Exception {
        Map<String, byte[]> expectedFiles = files("release skill", "release guide");
        SkillFixtureFactory.Fixture fixture = SkillFixtureFactory.fixture("1.7.0", expectedFiles);
        Map<String, byte[]> otherCommitFiles = files("different skill", "release guide");

        assertThrows(IOException.class, () -> GitHubSkillReleaseSource.verifyArchiveMatchesCommit(
                fixture.manifest(), fixture.archive(), commit(COMMIT, TREE_SHA), tree(TREE_SHA, otherCommitFiles)));
    }

    @Test
    public void rejectsMissingAndExtraSourceFiles() throws Exception {
        Map<String, byte[]> files = files("skill", "guide");
        SkillFixtureFactory.Fixture fixture = SkillFixtureFactory.fixture("1.7.0", files);

        assertThrows(IOException.class, () -> GitHubSkillReleaseSource.verifyArchiveMatchesCommit(
                fixture.manifest(), fixture.archive(), commit(COMMIT, TREE_SHA),
                tree(TREE_SHA, Map.of("SKILL.md", files.get("SKILL.md")))));
        Map<String, byte[]> extra = new LinkedHashMap<>(files);
        extra.put("unexpected.md", "extra".getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class, () -> GitHubSkillReleaseSource.verifyArchiveMatchesCommit(
                fixture.manifest(), fixture.archive(), commit(COMMIT, TREE_SHA), tree(TREE_SHA, extra)));
    }

    @Test
    public void rejectsWrongCommitTruncatedTreesAndNonRegularSkillFiles() throws Exception {
        Map<String, byte[]> files = files("skill", "guide");
        SkillFixtureFactory.Fixture fixture = SkillFixtureFactory.fixture("1.7.0", files);

        assertThrows(IOException.class, () -> GitHubSkillReleaseSource.verifyArchiveMatchesCommit(
                fixture.manifest(), fixture.archive(), commit("f".repeat(40), TREE_SHA), tree(TREE_SHA, files)));
        JsonObject truncated = treeObject(TREE_SHA, files);
        truncated.addProperty("truncated", true);
        assertThrows(IOException.class, () -> GitHubSkillReleaseSource.verifyArchiveMatchesCommit(
                fixture.manifest(), fixture.archive(), commit(COMMIT, TREE_SHA), truncated));
        JsonObject symlink = treeObject(TREE_SHA, files);
        JsonObject skillEntry = null;
        for (var entry : symlink.getAsJsonArray("tree")) {
            JsonObject candidate = entry.getAsJsonObject();
            if ((SkillReleaseManifest.SOURCE_PATH + "SKILL.md").equals(candidate.get("path").getAsString())) {
                skillEntry = candidate;
                break;
            }
        }
        assertNotNull("Fixture must include the manifest SKILL.md source blob", skillEntry);
        skillEntry.addProperty("mode", "120000");
        assertThrows(IOException.class, () -> GitHubSkillReleaseSource.verifyArchiveMatchesCommit(
                fixture.manifest(), fixture.archive(), commit(COMMIT, TREE_SHA), symlink));
        assertThrows(IOException.class, () -> GitHubSkillReleaseSource.verifyArchiveMatchesCommit(
                fixture.manifest(), fixture.archive(), commit(COMMIT, "f".repeat(40)), tree(TREE_SHA, files)));
    }

    @Test
    public void computesTheGitBlobObjectIdentityIncludingItsTypeAndLengthHeader() throws Exception {
        assertEquals("b6fc4c620b67d95f953a5c1c1230aaab5db5a1b0",
                GitHubSkillReleaseSource.gitBlobSha1("hello".getBytes(StandardCharsets.UTF_8)));
    }

    private static Map<String, byte[]> files(String skill, String guide) {
        Map<String, byte[]> files = new LinkedHashMap<>();
        files.put("SKILL.md", skill.getBytes(StandardCharsets.UTF_8));
        files.put("references/guide.md", guide.getBytes(StandardCharsets.UTF_8));
        return files;
    }

    private static JsonObject commit(String commit, String treeSha) {
        JsonObject response = new JsonObject();
        response.addProperty("sha", commit);
        JsonObject tree = new JsonObject();
        tree.addProperty("sha", treeSha);
        response.add("tree", tree);
        return response;
    }

    private static JsonObject tree(String treeSha, Map<String, byte[]> files) throws Exception {
        return treeObject(treeSha, files);
    }

    private static JsonObject treeObject(String treeSha, Map<String, byte[]> files) throws Exception {
        JsonObject response = new JsonObject();
        response.addProperty("sha", treeSha);
        response.addProperty("truncated", false);
        JsonArray tree = new JsonArray();
        JsonObject outside = new JsonObject();
        outside.addProperty("path", "README.md");
        outside.addProperty("mode", "100644");
        outside.addProperty("type", "blob");
        outside.addProperty("sha", GitHubSkillReleaseSource.gitBlobSha1("outside".getBytes(StandardCharsets.UTF_8)));
        tree.add(outside);
        JsonObject directory = new JsonObject();
        directory.addProperty("path", "skills/doc-view/references");
        directory.addProperty("mode", "040000");
        directory.addProperty("type", "tree");
        directory.addProperty("sha", "e".repeat(40));
        tree.add(directory);
        for (var entry : files.entrySet()) {
            JsonObject blob = new JsonObject();
            blob.addProperty("path", SkillReleaseManifest.SOURCE_PATH + entry.getKey());
            blob.addProperty("mode", "100644");
            blob.addProperty("type", "blob");
            blob.addProperty("sha", GitHubSkillReleaseSource.gitBlobSha1(entry.getValue()));
            tree.add(blob);
        }
        response.add("tree", tree);
        return response;
    }
}
