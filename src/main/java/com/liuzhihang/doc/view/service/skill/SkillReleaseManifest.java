package com.liuzhihang.doc.view.service.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Strict parser and compatibility validator for the detached official Skill manifest. */
final class SkillReleaseManifest {
    static final String REPOSITORY = "liuzhihang/doc-view";
    static final String SOURCE_PATH = "skills/doc-view/";
    static final String ARCHIVE_ROOT = "doc-view/";
    static final int TOOL_CONTRACT_VERSION = 1;
    static final long MAX_ARCHIVE_BYTES = 4L * 1024 * 1024;
    static final long MAX_EXPANDED_BYTES = 8L * 1024 * 1024;
    static final long MAX_FILE_BYTES = 2L * 1024 * 1024;
    static final int MAX_FILES = 64;
    private static final Pattern VERSION = Pattern.compile("^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:-[A-Za-z0-9.-]+)?$");
    private static final Pattern STABLE_VERSION = Pattern.compile("^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$");
    private static final Pattern SHA = Pattern.compile("^[a-f0-9]{64}$");
    private static final Pattern COMMIT = Pattern.compile("^[a-f0-9]{40,64}$");

    record FileRecord(String path, long size, String sha256) {}
    record ArchiveRecord(String file, long size, String sha256) {}

    private final String skillVersion;
    private final String releaseTag;
    private final String commitSha;
    private final int toolContractVersion;
    private final String toolContractFingerprint;
    private final String minimumPluginVersion;
    private final String maximumPluginVersionExclusive;
    private final List<FileRecord> files;
    private final ArchiveRecord archive;

    private SkillReleaseManifest(String skillVersion, String releaseTag, String commitSha,
                                 int toolContractVersion, String toolContractFingerprint,
                                 String minimumPluginVersion, String maximumPluginVersionExclusive,
                                 List<FileRecord> files, ArchiveRecord archive) {
        this.skillVersion = skillVersion;
        this.releaseTag = releaseTag;
        this.commitSha = commitSha;
        this.toolContractVersion = toolContractVersion;
        this.toolContractFingerprint = toolContractFingerprint;
        this.minimumPluginVersion = minimumPluginVersion;
        this.maximumPluginVersionExclusive = maximumPluginVersionExclusive;
        this.files = List.copyOf(files);
        this.archive = archive;
    }

    static SkillReleaseManifest parse(byte[] jsonBytes) throws IOException {
        if (jsonBytes == null || jsonBytes.length == 0 || jsonBytes.length > 64 * 1024) {
            throw new IOException("Skill manifest is empty or exceeds 64 KiB");
        }
        try {
            JsonObject root = JsonParser.parseString(new String(jsonBytes, StandardCharsets.UTF_8)).getAsJsonObject();
            requireKeys(root, Set.of("formatVersion", "skillVersion", "releaseTag", "repository", "commitSha",
                    "sourcePath", "archiveRoot", "toolContractVersion", "toolContractFingerprint",
                    "pluginVersionRange", "files", "archive"));
            if (integer(root, "formatVersion") != 1 || !REPOSITORY.equals(string(root, "repository"))
                    || !SOURCE_PATH.equals(string(root, "sourcePath"))
                    || !ARCHIVE_ROOT.equals(string(root, "archiveRoot"))) {
                throw new IOException("Skill manifest source, format, or archive root is unsupported");
            }
            String skillVersion = string(root, "skillVersion");
            String releaseTag = string(root, "releaseTag");
            if (!STABLE_VERSION.matcher(skillVersion).matches() || !releaseTag.equals("v" + skillVersion)) {
                throw new IOException("Skill manifest must be tied to a stable vX.Y.Z release tag");
            }
            String commitSha = string(root, "commitSha");
            if (!COMMIT.matcher(commitSha).matches()) throw new IOException("Skill manifest commit SHA is invalid");
            int contractVersion = integer(root, "toolContractVersion");
            String fingerprint = string(root, "toolContractFingerprint");
            if (!fingerprint.matches("^sha256:[a-f0-9]{64}$")) {
                throw new IOException("Skill manifest tool contract fingerprint is invalid");
            }

            JsonObject range = object(root, "pluginVersionRange");
            requireKeys(range, Set.of("minimumInclusive", "maximumExclusive"));
            String minimum = string(range, "minimumInclusive");
            String maximum = string(range, "maximumExclusive");
            if (!STABLE_VERSION.matcher(minimum).matches() || !STABLE_VERSION.matcher(maximum).matches()
                    || compareVersion(minimum, maximum) >= 0) {
                throw new IOException("Skill manifest plugin compatibility range is invalid");
            }

            JsonArray fileArray = array(root, "files");
            if (fileArray.size() < 1 || fileArray.size() > MAX_FILES) {
                throw new IOException("Skill manifest must contain 1 to 64 files");
            }
            List<FileRecord> files = new ArrayList<>();
            Set<String> paths = new HashSet<>();
            Set<String> foldedPaths = new HashSet<>();
            long totalBytes = 0;
            boolean hasSkill = false;
            for (JsonElement value : fileArray) {
                JsonObject file = value.getAsJsonObject();
                requireKeys(file, Set.of("path", "size", "sha256"));
                String path = string(file, "path");
                validateMetadataPath(path);
                if (!paths.add(path) || !foldedPaths.add(path.toLowerCase(Locale.ROOT))) {
                    throw new IOException("Skill manifest contains duplicate or case-colliding paths");
                }
                long size = longInteger(file, "size");
                if (size < 0 || size > MAX_FILE_BYTES) throw new IOException("Skill file size exceeds 2 MiB");
                String hash = string(file, "sha256");
                if (!SHA.matcher(hash).matches()) throw new IOException("Skill file SHA-256 is invalid");
                totalBytes += size;
                if (totalBytes > MAX_EXPANDED_BYTES) throw new IOException("Expanded Skill exceeds 8 MiB");
                hasSkill |= "SKILL.md".equals(path);
                files.add(new FileRecord(path, size, hash));
            }
            if (!hasSkill) throw new IOException("Skill manifest does not include SKILL.md");

            JsonObject archiveObject = object(root, "archive");
            requireKeys(archiveObject, Set.of("file", "size", "sha256"));
            ArchiveRecord archive = new ArchiveRecord(string(archiveObject, "file"),
                    longInteger(archiveObject, "size"), string(archiveObject, "sha256"));
            if (!archive.file().equals("doc-view-skill-" + skillVersion + ".zip")
                    || archive.size() < 1 || archive.size() > MAX_ARCHIVE_BYTES
                    || !SHA.matcher(archive.sha256()).matches()) {
                throw new IOException("Skill archive metadata is invalid or exceeds 4 MiB");
            }
            files.sort((left, right) -> left.path().compareTo(right.path()));
            return new SkillReleaseManifest(skillVersion, releaseTag, commitSha, contractVersion, fingerprint,
                    minimum, maximum, files, archive);
        } catch (IOException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new IOException("Skill manifest is malformed", failure);
        }
    }

    void validateCompatibility(String pluginVersion, byte[] contractBytes) throws IOException {
        if (toolContractVersion != TOOL_CONTRACT_VERSION) {
            throw new IOException("Skill manifest requires an unsupported MCP tool contract version");
        }
        String localFingerprint = "sha256:" + sha256(contractBytes);
        if (!localFingerprint.equals(toolContractFingerprint)) {
            throw new IOException("Skill manifest MCP tool contract fingerprint does not match this plugin");
        }
        if (pluginVersion == null || !VERSION.matcher(pluginVersion).matches()) {
            throw new IOException("Current Doc View plugin version cannot be verified");
        }
        String normalized = pluginVersion.split("-", 2)[0];
        if (compareVersion(normalized, minimumPluginVersion) < 0
                || compareVersion(normalized, maximumPluginVersionExclusive) >= 0) {
            throw new IOException("Skill manifest does not support this Doc View plugin version");
        }
    }

    String skillVersion() { return skillVersion; }
    String releaseTag() { return releaseTag; }
    String commitSha() { return commitSha; }
    int toolContractVersion() { return toolContractVersion; }
    String toolContractFingerprint() { return toolContractFingerprint; }
    String minimumPluginVersion() { return minimumPluginVersion; }
    String maximumPluginVersionExclusive() { return maximumPluginVersionExclusive; }
    List<FileRecord> files() { return files; }
    ArchiveRecord archive() { return archive; }

    static byte[] localContractBytes() throws IOException {
        try (InputStream input = SkillReleaseManifest.class.getResourceAsStream("/mcp/tool-contracts.json")) {
            if (input == null) throw new IOException("Doc View MCP tool contract is unavailable");
            byte[] bytes = input.readNBytes(2 * 1024 * 1024 + 1);
            if (bytes.length > 2 * 1024 * 1024) throw new IOException("Doc View MCP tool contract exceeds its size limit");
            return bytes;
        }
    }

    static String sha256(byte[] bytes) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-256 is unavailable", impossible);
        }
    }

    static int compareVersion(String left, String right) throws IOException {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        try {
            for (int index = 0; index < 3; index++) {
                int comparison = Integer.compare(Integer.parseInt(leftParts[index]), Integer.parseInt(rightParts[index]));
                if (comparison != 0) return comparison;
            }
            return 0;
        } catch (RuntimeException invalid) {
            throw new IOException("Plugin version is invalid", invalid);
        }
    }

    static void validateMetadataPath(String path) throws IOException {
        if (path.isBlank() || path.length() > 512 || path.startsWith("/") || path.contains("\\") || path.contains(":")
                || path.indexOf('\0') >= 0 || path.endsWith("/")) {
            throw new IOException("Skill manifest contains an unsafe file path");
        }
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || segment.length() > 255 || segment.equals(".") || segment.equals("..")) {
                throw new IOException("Skill manifest contains an unsafe file path");
            }
        }
    }

    private static void requireKeys(JsonObject object, Set<String> keys) throws IOException {
        if (!object.keySet().equals(keys)) throw new IOException("Skill manifest contains missing or unexpected fields");
    }

    private static String string(JsonObject object, String key) throws IOException {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IOException("Skill manifest field is invalid: " + key);
        }
        return value.getAsString();
    }

    private static int integer(JsonObject object, String key) throws IOException {
        long value = longInteger(object, key);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) throw new IOException("Manifest integer is invalid");
        return (int) value;
    }

    private static long longInteger(JsonObject object, String key) throws IOException {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IOException("Skill manifest field is invalid: " + key);
        }
        try { return value.getAsLong(); } catch (RuntimeException invalid) { throw new IOException("Manifest integer is invalid", invalid); }
    }

    private static JsonObject object(JsonObject parent, String key) throws IOException {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonObject()) throw new IOException("Skill manifest object is invalid: " + key);
        return value.getAsJsonObject();
    }

    private static JsonArray array(JsonObject parent, String key) throws IOException {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonArray()) throw new IOException("Skill manifest array is invalid: " + key);
        return value.getAsJsonArray();
    }
}
