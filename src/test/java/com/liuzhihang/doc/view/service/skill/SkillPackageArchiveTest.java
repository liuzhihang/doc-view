package com.liuzhihang.doc.view.service.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class SkillPackageArchiveTest {
    @Test
    public void acceptsAsciiNamesWithoutUtf8FlagUsedByPythonPackager() throws Exception {
        byte[] skill = "name: doc-view\n".getBytes(StandardCharsets.UTF_8);
        byte[] archive = zip("doc-view/SKILL.md", skill);
        archive[7] &= (byte) ~8;
        archive[findCentralDirectory(archive) + 9] &= (byte) ~8;
        assertArrayEquals(skill, SkillPackageArchive.validate(archive,
                manifest(archive, "SKILL.md", skill)).get("SKILL.md"));
    }

    @Test
    public void rejectsNonAsciiNamesWithoutExplicitUtf8Flag() throws Exception {
        byte[] archive = zip("doc-view/说明.md", new byte[]{1});
        archive[7] &= (byte) ~8;
        archive[findCentralDirectory(archive) + 9] &= (byte) ~8;
        SkillReleaseManifest manifest = manifest(archive, "SKILL.md", new byte[]{1});
        IOException failure = assertThrows(IOException.class, () -> SkillPackageArchive.validate(archive, manifest));
        assertEquals("Non-ASCII Skill paths require explicit UTF-8 encoding", failure.getMessage());
    }

    @Test
    public void acceptsOnlyManifestBoundRegularFiles() throws Exception {
        byte[] skill = "name: doc-view\n".getBytes(StandardCharsets.UTF_8);
        byte[] archive = zip("doc-view/SKILL.md", skill);
        SkillReleaseManifest manifest = manifest(archive, "SKILL.md", skill);

        Map<String, byte[]> files = SkillPackageArchive.validate(archive, manifest);

        assertEquals(1, files.size());
        assertArrayEquals(skill, files.get("SKILL.md"));
    }

    @Test
    public void rejectsZipSlipAndUnexpectedRoot() throws Exception {
        byte[] archive = zip("doc-view/../escape.txt", new byte[]{1});
        SkillReleaseManifest manifest = manifest(archive, "SKILL.md", new byte[]{1});

        assertThrows(IOException.class, () -> SkillPackageArchive.validate(archive, manifest));
    }

    @Test
    public void rejectsManifestHashMismatch() throws Exception {
        byte[] archive = zip("doc-view/SKILL.md", new byte[]{1});
        SkillReleaseManifest manifest = manifest(archive, "SKILL.md", new byte[]{2});

        assertThrows(IOException.class, () -> SkillPackageArchive.validate(archive, manifest));
    }

    @Test
    public void rejectsUnixSymbolicLinkEntry() throws Exception {
        byte[] archive = zip("doc-view/SKILL.md", new byte[]{1});
        int central = findCentralDirectory(archive);
        archive[central + 5] = 3; // creator OS = Unix
        int symlinkMode = 0120777 << 16;
        archive[central + 38] = (byte) symlinkMode;
        archive[central + 39] = (byte) (symlinkMode >>> 8);
        archive[central + 40] = (byte) (symlinkMode >>> 16);
        archive[central + 41] = (byte) (symlinkMode >>> 24);
        SkillReleaseManifest manifest = manifest(archive, "SKILL.md", new byte[]{1});

        assertThrows(IOException.class, () -> SkillPackageArchive.validate(archive, manifest));
    }

    @Test
    public void rejectsUnixFifoAndDeviceEntries() throws Exception {
        int[] unixSpecialModes = {
                0010644, // FIFO
                0020600, // character device
                0060600, // block device
                0140600  // socket
        };

        for (int mode : unixSpecialModes) {
            byte[] content = {1};
            byte[] archive = zip("doc-view/SKILL.md", content);
            int central = findCentralDirectory(archive);
            archive[central + 5] = 3; // central directory creator OS = Unix
            long externalAttributes = ((long) mode) << 16;
            for (int byteIndex = 0; byteIndex < Integer.BYTES; byteIndex++) {
                archive[central + 38 + byteIndex] = (byte) (externalAttributes >>> (byteIndex * 8));
            }
            SkillReleaseManifest manifest = manifest(archive, "SKILL.md", content);

            assertThrows("Unix special entry mode " + Integer.toOctalString(mode), IOException.class,
                    () -> SkillPackageArchive.validate(archive, manifest));
        }
    }

    @Test
    public void rejectsDuplicateArchiveEntryNames() throws Exception {
        byte[] skill = "name: doc-view\n".getBytes(StandardCharsets.UTF_8);
        Map<String, byte[]> archiveFiles = new LinkedHashMap<>();
        archiveFiles.put("doc-view/SKILL.md", skill);
        archiveFiles.put("doc-view/skill.MD", skill);
        byte[] archive = zip(archiveFiles);
        rewriteSecondEntryNameToFirst(archive);

        assertThrows(IOException.class, () -> SkillPackageArchive.validate(archive,
                manifest(archive, "SKILL.md", skill)));
    }

    @Test
    public void rejectsArchiveCaseCollisionEvenWhenOnlyOneSpellingIsManifested() throws Exception {
        byte[] skill = "name: doc-view\n".getBytes(StandardCharsets.UTF_8);
        Map<String, byte[]> archiveFiles = new LinkedHashMap<>();
        archiveFiles.put("doc-view/SKILL.md", skill);
        archiveFiles.put("doc-view/skill.md", skill);
        byte[] archive = zip(archiveFiles);

        assertThrows(IOException.class, () -> SkillPackageArchive.validate(archive,
                manifest(archive, "SKILL.md", skill)));
    }

    @Test
    public void rejectsArchiveWithMoreThan64Files() throws Exception {
        Map<String, byte[]> archiveFiles = new LinkedHashMap<>();
        Map<String, byte[]> manifestFiles = new LinkedHashMap<>();
        byte[] skill = "name: doc-view\n".getBytes(StandardCharsets.UTF_8);
        archiveFiles.put("doc-view/SKILL.md", skill);
        manifestFiles.put("SKILL.md", skill);
        for (int index = 0; index < SkillReleaseManifest.MAX_FILES; index++) {
            String relative = "file-%02d.txt".formatted(index);
            byte[] content = {(byte) index};
            archiveFiles.put(SkillReleaseManifest.ARCHIVE_ROOT + relative, content);
            if (manifestFiles.size() < SkillReleaseManifest.MAX_FILES) manifestFiles.put(relative, content);
        }

        byte[] archive = zip(archiveFiles);

        assertEquals(SkillReleaseManifest.MAX_FILES + 1, archiveFiles.size());
        assertThrows(IOException.class, () -> SkillPackageArchive.validate(archive, manifest(archive, manifestFiles)));
    }

    @Test
    public void rejectsSingleExpandedFileAboveTwoMiB() throws Exception {
        byte[] actual = new byte[(int) SkillReleaseManifest.MAX_FILE_BYTES + 1];
        byte[] declared = Arrays.copyOf(actual, actual.length - 1);
        byte[] archive = zip("doc-view/SKILL.md", actual);

        assertThrows(IOException.class, () -> SkillPackageArchive.validate(archive,
                manifest(archive, "SKILL.md", declared)));
    }

    @Test
    public void rejectsAggregateExpansionAboveEightMiBWhenEachFileIsWithinItsLimit() throws Exception {
        Map<String, byte[]> archiveFiles = new LinkedHashMap<>();
        Map<String, byte[]> manifestFiles = new LinkedHashMap<>();
        byte[] maximumFile = new byte[(int) SkillReleaseManifest.MAX_FILE_BYTES];
        for (int index = 0; index < 4; index++) {
            String relative = index == 0 ? "SKILL.md" : "file-%d.txt".formatted(index);
            archiveFiles.put(SkillReleaseManifest.ARCHIVE_ROOT + relative, maximumFile);
            manifestFiles.put(relative, maximumFile);
        }
        String lastPath = "file-4.txt";
        archiveFiles.put(SkillReleaseManifest.ARCHIVE_ROOT + lastPath, new byte[]{1});
        manifestFiles.put(lastPath, new byte[0]);
        byte[] archive = zip(archiveFiles);

        assertEquals(SkillReleaseManifest.MAX_EXPANDED_BYTES,
                manifestFiles.values().stream().mapToLong(bytes -> bytes.length).sum());
        assertThrows(IOException.class, () -> SkillPackageArchive.validate(archive, manifest(archive, manifestFiles)));
    }

    @Test
    public void rejectsArchiveOverFourMiBBeforeInflation() throws Exception {
        byte[] oversized = new byte[(int) SkillReleaseManifest.MAX_ARCHIVE_BYTES + 1];
        SkillReleaseManifest manifest = manifest(new byte[]{1}, "SKILL.md", new byte[]{1});

        assertThrows(IOException.class, () -> SkillPackageArchive.validate(oversized, manifest));
    }

    private static SkillReleaseManifest manifest(byte[] archive, String path, byte[] content) throws Exception {
        return manifest(archive, Map.of(path, content));
    }

    private static SkillReleaseManifest manifest(byte[] archive, Map<String, byte[]> files) throws Exception {
        String contractHash = SkillReleaseManifest.sha256("contract".getBytes(StandardCharsets.UTF_8));
        JsonObject root = new JsonObject();
        root.addProperty("formatVersion", 1);
        root.addProperty("skillVersion", "1.7.0");
        root.addProperty("releaseTag", "v1.7.0");
        root.addProperty("repository", "liuzhihang/doc-view");
        root.addProperty("commitSha", "0123456789012345678901234567890123456789");
        root.addProperty("sourcePath", "skills/doc-view/");
        root.addProperty("archiveRoot", "doc-view/");
        root.addProperty("toolContractVersion", 1);
        root.addProperty("toolContractFingerprint", "sha256:" + contractHash);
        JsonObject range = new JsonObject();
        range.addProperty("minimumInclusive", "1.7.0");
        range.addProperty("maximumExclusive", "2.0.0");
        root.add("pluginVersionRange", range);
        JsonArray fileRecords = new JsonArray();
        for (var file : files.entrySet()) {
            JsonObject record = new JsonObject();
            record.addProperty("path", file.getKey());
            record.addProperty("size", file.getValue().length);
            record.addProperty("sha256", SkillReleaseManifest.sha256(file.getValue()));
            fileRecords.add(record);
        }
        root.add("files", fileRecords);
        JsonObject archiveRecord = new JsonObject();
        archiveRecord.addProperty("file", "doc-view-skill-1.7.0.zip");
        archiveRecord.addProperty("size", archive.length);
        archiveRecord.addProperty("sha256", SkillReleaseManifest.sha256(archive));
        root.add("archive", archiveRecord);
        return SkillReleaseManifest.parse(root.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] zip(String path, byte[] content) throws IOException {
        return zip(Map.of(path, content));
    }

    private static byte[] zip(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (var file : files.entrySet()) {
                ZipEntry entry = new ZipEntry(file.getKey());
                zip.putNextEntry(entry);
                zip.write(file.getValue());
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    private static void rewriteSecondEntryNameToFirst(byte[] archive) {
        int firstCentral = findCentralDirectory(archive);
        int firstNameLength = littleEndian16(archive, firstCentral + 28);
        int firstExtraLength = littleEndian16(archive, firstCentral + 30);
        int firstCommentLength = littleEndian16(archive, firstCentral + 32);
        int secondCentral = firstCentral + 46 + firstNameLength + firstExtraLength + firstCommentLength;
        int secondNameLength = littleEndian16(archive, secondCentral + 28);
        if (firstNameLength != secondNameLength) throw new AssertionError("fixture names must have equal lengths");
        byte[] firstName = Arrays.copyOfRange(archive, firstCentral + 46, firstCentral + 46 + firstNameLength);
        System.arraycopy(firstName, 0, archive, secondCentral + 46, firstNameLength);

        int secondLocal = littleEndian32(archive, secondCentral + 42);
        int secondLocalNameLength = littleEndian16(archive, secondLocal + 26);
        if (secondLocalNameLength != firstNameLength) throw new AssertionError("local fixture name length differs");
        System.arraycopy(firstName, 0, archive, secondLocal + 30, firstNameLength);
    }

    private static int littleEndian16(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8);
    }

    private static int littleEndian32(byte[] bytes, int offset) {
        return littleEndian16(bytes, offset) | (littleEndian16(bytes, offset + 2) << 16);
    }

    private static int findCentralDirectory(byte[] bytes) {
        for (int index = 0; index < bytes.length - 4; index++) {
            if (bytes[index] == 0x50 && bytes[index + 1] == 0x4b
                    && bytes[index + 2] == 0x01 && bytes[index + 3] == 0x02) return index;
        }
        throw new AssertionError("central directory not found");
    }
}
