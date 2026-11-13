package com.liuzhihang.doc.view.service.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class SkillFixtureFactory {
    record Fixture(SkillReleaseManifest manifest, byte[] archive, Map<String, byte[]> files, byte[] contract) {}

    private SkillFixtureFactory() {}

    static Fixture fixture(String version, Map<String, byte[]> sourceFiles) throws Exception {
        Map<String, byte[]> files = new LinkedHashMap<>(sourceFiles);
        byte[] archive = zip(files);
        byte[] contract = SkillReleaseManifest.localContractBytes();
        JsonObject root = new JsonObject();
        root.addProperty("formatVersion", 1);
        root.addProperty("skillVersion", version);
        root.addProperty("releaseTag", "v" + version);
        root.addProperty("repository", SkillReleaseManifest.REPOSITORY);
        root.addProperty("commitSha", "0123456789012345678901234567890123456789");
        root.addProperty("sourcePath", SkillReleaseManifest.SOURCE_PATH);
        root.addProperty("archiveRoot", SkillReleaseManifest.ARCHIVE_ROOT);
        root.addProperty("toolContractVersion", SkillReleaseManifest.TOOL_CONTRACT_VERSION);
        root.addProperty("toolContractFingerprint", "sha256:" + SkillReleaseManifest.sha256(contract));
        JsonObject range = new JsonObject();
        range.addProperty("minimumInclusive", "1.7.0");
        range.addProperty("maximumExclusive", "2.0.0");
        root.add("pluginVersionRange", range);
        JsonArray fileRecords = new JsonArray();
        files.forEach((path, bytes) -> {
            JsonObject record = new JsonObject();
            record.addProperty("path", path);
            record.addProperty("size", bytes.length);
            try { record.addProperty("sha256", SkillReleaseManifest.sha256(bytes)); }
            catch (Exception failure) { throw new IllegalStateException(failure); }
            fileRecords.add(record);
        });
        root.add("files", fileRecords);
        JsonObject archiveRecord = new JsonObject();
        archiveRecord.addProperty("file", "doc-view-skill-" + version + ".zip");
        archiveRecord.addProperty("size", archive.length);
        archiveRecord.addProperty("sha256", SkillReleaseManifest.sha256(archive));
        root.add("archive", archiveRecord);
        SkillReleaseManifest manifest = SkillReleaseManifest.parse(root.toString().getBytes(StandardCharsets.UTF_8));
        manifest.validateCompatibility("1.7.0-SNAPSHOT", contract);
        return new Fixture(manifest, archive, Map.copyOf(files), contract);
    }

    static byte[] zip(Map<String, byte[]> files) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (var entry : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(SkillReleaseManifest.ARCHIVE_ROOT + entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }
}
