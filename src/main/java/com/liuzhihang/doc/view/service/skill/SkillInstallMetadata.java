package com.liuzhihang.doc.view.service.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Plugin-owned installation record stored outside the user-visible Skill directory. */
record SkillInstallMetadata(
        CodexSkillScope scope,
        String skillVersion,
        String releaseTag,
        String commitSha,
        int toolContractVersion,
        String toolContractFingerprint,
        String minimumPluginVersion,
        String maximumPluginVersionExclusive,
        String clientVersion,
        String targetPath,
        String installedAt,
        List<SkillReleaseManifest.FileRecord> files) {

    static SkillInstallMetadata from(SkillReleaseManifest manifest, String clientVersion,
                                     String targetPath, Instant installedAt) {
        return from(CodexSkillScope.PROJECT, manifest, clientVersion, targetPath, installedAt);
    }

    static SkillInstallMetadata from(CodexSkillScope scope, SkillReleaseManifest manifest, String clientVersion,
                                     String targetPath, Instant installedAt) {
        return new SkillInstallMetadata(scope, manifest.skillVersion(), manifest.releaseTag(), manifest.commitSha(),
                manifest.toolContractVersion(), manifest.toolContractFingerprint(), manifest.minimumPluginVersion(),
                manifest.maximumPluginVersionExclusive(), clientVersion, targetPath, installedAt.toString(),
                manifest.files());
    }

    byte[] encode() {
        JsonObject root = new JsonObject();
        root.addProperty("formatVersion", 2);
        root.addProperty("scope", scope.name());
        root.addProperty("skillVersion", skillVersion);
        root.addProperty("releaseTag", releaseTag);
        root.addProperty("commitSha", commitSha);
        root.addProperty("toolContractVersion", toolContractVersion);
        root.addProperty("toolContractFingerprint", toolContractFingerprint);
        root.addProperty("minimumPluginVersion", minimumPluginVersion);
        root.addProperty("maximumPluginVersionExclusive", maximumPluginVersionExclusive);
        root.addProperty("clientVersion", clientVersion);
        root.addProperty("targetPath", targetPath);
        root.addProperty("installedAt", installedAt);
        JsonArray fileArray = new JsonArray();
        for (var file : files) {
            JsonObject item = new JsonObject();
            item.addProperty("path", file.path());
            item.addProperty("size", file.size());
            item.addProperty("sha256", file.sha256());
            fileArray.add(item);
        }
        root.add("files", fileArray);
        return (root + "\n").getBytes(StandardCharsets.UTF_8);
    }

    static SkillInstallMetadata parse(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0 || bytes.length > 64 * 1024) {
            throw new IOException("Skill installation metadata is empty or exceeds 64 KiB");
        }
        try {
            JsonObject root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            Set<String> fields = Set.of("formatVersion", "scope", "skillVersion", "releaseTag", "commitSha",
                    "toolContractVersion", "toolContractFingerprint", "minimumPluginVersion",
                    "maximumPluginVersionExclusive", "clientVersion", "targetPath", "installedAt", "files");
            if (!root.keySet().equals(fields) || root.get("formatVersion").getAsInt() != 2) {
                throw new IOException("Skill installation metadata format is unsupported");
            }
            JsonArray fileArray = root.getAsJsonArray("files");
            if (fileArray.size() < 1 || fileArray.size() > SkillReleaseManifest.MAX_FILES) {
                throw new IOException("Skill installation metadata file list is invalid");
            }
            List<SkillReleaseManifest.FileRecord> files = new ArrayList<>();
            for (JsonElement element : fileArray) {
                JsonObject file = element.getAsJsonObject();
                if (!file.keySet().equals(Set.of("path", "size", "sha256"))) {
                    throw new IOException("Skill installation metadata file record is invalid");
                }
                files.add(new SkillReleaseManifest.FileRecord(file.get("path").getAsString(),
                        file.get("size").getAsLong(), file.get("sha256").getAsString()));
            }
            var metadata = new SkillInstallMetadata(CodexSkillScope.valueOf(root.get("scope").getAsString()),
                    root.get("skillVersion").getAsString(),
                    root.get("releaseTag").getAsString(), root.get("commitSha").getAsString(),
                    root.get("toolContractVersion").getAsInt(), root.get("toolContractFingerprint").getAsString(),
                    root.get("minimumPluginVersion").getAsString(), root.get("maximumPluginVersionExclusive").getAsString(),
                    root.get("clientVersion").getAsString(), root.get("targetPath").getAsString(),
                    root.get("installedAt").getAsString(), List.copyOf(files));
            validate(metadata);
            return metadata;
        } catch (IOException failure) {
            throw failure;
        } catch (RuntimeException invalid) {
            throw new IOException("Skill installation metadata is malformed", invalid);
        }
    }

    private static void validate(SkillInstallMetadata metadata) throws IOException {
        if (metadata.scope() == null || !metadata.releaseTag().equals("v" + metadata.skillVersion())
                || !metadata.commitSha().matches("[a-f0-9]{40,64}")
                || !metadata.toolContractFingerprint().matches("sha256:[a-f0-9]{64}")
                || !CodexClientProbe.SUPPORTED_VERSION.equals(metadata.clientVersion())
                || metadata.targetPath().isBlank()) {
            throw new IOException("Skill installation metadata does not match the supported install contract");
        }
        try {
            Instant.parse(metadata.installedAt());
        } catch (RuntimeException invalid) {
            throw new IOException("Skill installation timestamp is invalid", invalid);
        }
        long total = 0;
        var names = new java.util.HashSet<String>();
        var folded = new java.util.HashSet<String>();
        for (var file : metadata.files()) {
            SkillReleaseManifest.validateMetadataPath(file.path());
            if (file.size() < 0 || file.size() > SkillReleaseManifest.MAX_FILE_BYTES
                    || !file.sha256().matches("[a-f0-9]{64}")
                    || !names.add(file.path()) || !folded.add(file.path().toLowerCase(java.util.Locale.ROOT))) {
                throw new IOException("Skill installation file metadata is invalid");
            }
            total += file.size();
            if (total > SkillReleaseManifest.MAX_EXPANDED_BYTES) throw new IOException("Skill metadata exceeds 8 MiB");
        }
        if (!names.contains("SKILL.md")) throw new IOException("Skill installation metadata does not include SKILL.md");
    }
}
