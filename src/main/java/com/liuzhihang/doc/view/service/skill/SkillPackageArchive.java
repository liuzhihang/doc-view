package com.liuzhihang.doc.view.service.skill;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Validates every ZIP entry and content hash before any target directory is made visible. */
final class SkillPackageArchive {
    private static final long LOCAL_HEADER = 0x04034b50L;
    private static final long CENTRAL_HEADER = 0x02014b50L;
    private static final long END_CENTRAL = 0x06054b50L;
    private static final int UTF8_FLAG = 1 << 11;
    private static final int ENCRYPTED_FLAG = 1;
    private static final int UNIX_OS = 3;
    private static final int UNIX_FILE_TYPE_MASK = 0170000;
    private static final int UNIX_REGULAR_FILE = 0100000;

    private SkillPackageArchive() {}

    static Map<String, byte[]> validate(byte[] archive, SkillReleaseManifest manifest) throws IOException {
        if (archive == null || archive.length == 0 || archive.length > SkillReleaseManifest.MAX_ARCHIVE_BYTES) {
            throw new IOException("Skill archive is empty or exceeds 4 MiB");
        }
        SkillReleaseManifest.ArchiveRecord archiveRecord = manifest.archive();
        if (archive.length != archiveRecord.size()
                || !SkillReleaseManifest.sha256(archive).equals(archiveRecord.sha256())) {
            throw new IOException("Skill archive size or SHA-256 does not match its manifest");
        }
        List<String> centralPaths = validateCentralDirectory(archive);
        Map<String, SkillReleaseManifest.FileRecord> expected = new HashMap<>();
        for (var file : manifest.files()) expected.put(file.path(), file);

        Map<String, byte[]> contents = new HashMap<>();
        Set<String> seenPaths = new HashSet<>();
        Set<String> seenFolded = new HashSet<>();
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (contents.size() >= SkillReleaseManifest.MAX_FILES) {
                    throw new IOException("Skill archive contains more than 64 files");
                }
                if (entry.isDirectory() || entry.getName().endsWith("/")) {
                    throw new IOException("Skill archive contains an unexpected directory entry");
                }
                if (entry.getMethod() != ZipEntry.STORED && entry.getMethod() != ZipEntry.DEFLATED) {
                    throw new IOException("Skill archive uses an unsupported compression method");
                }
                String relative = relativePath(entry.getName());
                SkillReleaseManifest.FileRecord record = expected.get(relative);
                if (record == null) throw new IOException("Skill archive contains a file not listed in its manifest");
                if (!seenPaths.add(relative) || !seenFolded.add(relative.toLowerCase(Locale.ROOT))) {
                    throw new IOException("Skill archive contains duplicate or case-colliding paths");
                }
                ByteArrayOutputStream file = new ByteArrayOutputStream((int) Math.min(record.size(), 32 * 1024));
                byte[] buffer = new byte[8192];
                int count;
                long fileSize = 0;
                while ((count = zip.read(buffer)) != -1) {
                    fileSize += count;
                    total += count;
                    if (fileSize > SkillReleaseManifest.MAX_FILE_BYTES
                            || total > SkillReleaseManifest.MAX_EXPANDED_BYTES) {
                        throw new IOException("Expanded Skill archive exceeds its size limit");
                    }
                    file.write(buffer, 0, count);
                }
                byte[] bytes = file.toByteArray();
                if (fileSize != record.size() || !SkillReleaseManifest.sha256(bytes).equals(record.sha256())) {
                    throw new IOException("Skill file size or SHA-256 does not match its manifest: " + relative);
                }
                contents.put(relative, bytes);
                zip.closeEntry();
            }
        } catch (RuntimeException invalidArchive) {
            throw new IOException("Skill archive structure is malformed", invalidArchive);
        }
        if (!contents.keySet().equals(expected.keySet())) {
            throw new IOException("Skill archive is missing one or more manifest files");
        }
        Set<String> expectedArchivePaths = new HashSet<>();
        for (String path : expected.keySet()) expectedArchivePaths.add(SkillReleaseManifest.ARCHIVE_ROOT + path);
        if (!expectedArchivePaths.equals(new HashSet<>(centralPaths))) {
            throw new IOException("Skill archive central directory does not match its manifest");
        }
        return Map.copyOf(contents);
    }

    private static String relativePath(String name) throws IOException {
        if (!name.startsWith(SkillReleaseManifest.ARCHIVE_ROOT)) {
            throw new IOException("Skill archive has an unexpected root directory");
        }
        String path = name.substring(SkillReleaseManifest.ARCHIVE_ROOT.length());
        if (path.isBlank() || path.length() > 512 || path.startsWith("/") || path.contains("\\") || path.contains(":")
                || path.indexOf('\0') >= 0 || path.endsWith("/")) {
            throw new IOException("Skill archive contains an unsafe path");
        }
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || segment.length() > 255 || segment.equals(".") || segment.equals("..")) {
                throw new IOException("Skill archive contains an unsafe path");
            }
        }
        return path;
    }

    private static List<String> validateCentralDirectory(byte[] bytes) throws IOException {
        int eocd = findEndRecord(bytes);
        int disk = uint16(bytes, eocd + 4);
        int centralDisk = uint16(bytes, eocd + 6);
        int diskEntries = uint16(bytes, eocd + 8);
        int totalEntries = uint16(bytes, eocd + 10);
        long centralSize = uint32(bytes, eocd + 12);
        long centralOffset = uint32(bytes, eocd + 16);
        int commentLength = uint16(bytes, eocd + 20);
        if (disk != 0 || centralDisk != 0 || diskEntries != totalEntries || totalEntries > SkillReleaseManifest.MAX_FILES
                || totalEntries == 0 || totalEntries == 0xffff || centralSize == 0xffffffffL
                || centralOffset == 0xffffffffL || eocd + 22 + commentLength != bytes.length
                || centralOffset + centralSize != eocd) {
            throw new IOException("Skill archive central directory is invalid or uses unsupported ZIP64 features");
        }
        int position = Math.toIntExact(centralOffset);
        int limit = Math.toIntExact(centralOffset + centralSize);
        List<String> names = new ArrayList<>(totalEntries);
        for (int index = 0; index < totalEntries; index++) {
            requireRange(bytes, position, 46);
            if (uint32(bytes, position) != CENTRAL_HEADER) throw new IOException("Skill archive central entry is malformed");
            int madeBy = uint16(bytes, position + 4);
            int flags = uint16(bytes, position + 8);
            int method = uint16(bytes, position + 10);
            int nameLength = uint16(bytes, position + 28);
            int extraLength = uint16(bytes, position + 30);
            int commentLengthEntry = uint16(bytes, position + 32);
            long localOffset = uint32(bytes, position + 42);
            long externalAttributes = uint32(bytes, position + 38);
            int end = position + 46 + nameLength + extraLength + commentLengthEntry;
            requireRange(bytes, position, end - position);
            if ((flags & ENCRYPTED_FLAG) != 0
                    || (method != ZipEntry.STORED && method != ZipEntry.DEFLATED)
                    || localOffset >= centralOffset || localOffset > bytes.length - 30
                    || uint32(bytes, (int) localOffset) != LOCAL_HEADER) {
                throw new IOException("Skill archive contains an encrypted or malformed entry");
            }
            requireUnambiguousEncoding(bytes, position + 46, nameLength, flags);
            String name = decodeUtf8(bytes, position + 46, nameLength);
            String localName = localHeaderName(bytes, Math.toIntExact(localOffset));
            if (!name.equals(localName)) throw new IOException("Skill archive local and central paths differ");
            if (((madeBy >>> 8) & 0xff) == UNIX_OS) {
                int unixMode = (int) ((externalAttributes >>> 16) & 0xffff);
                if ((unixMode & UNIX_FILE_TYPE_MASK) != UNIX_REGULAR_FILE) {
                    throw new IOException("Skill archive contains a symbolic link or special file");
                }
            } else if ((externalAttributes & 0x10) != 0) {
                throw new IOException("Skill archive contains an unexpected directory entry");
            }
            names.add(name);
            position = end;
        }
        if (position != limit) throw new IOException("Skill archive central directory has trailing or missing entries");
        return names;
    }

    private static int findEndRecord(byte[] bytes) throws IOException {
        int earliest = Math.max(0, bytes.length - 65_557);
        for (int index = bytes.length - 22; index >= earliest; index--) {
            if (uint32(bytes, index) == END_CENTRAL) return index;
        }
        throw new IOException("Skill archive is missing its ZIP end record");
    }

    private static String decodeUtf8(byte[] bytes, int offset, int length) throws IOException {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, offset, length)).toString();
        } catch (CharacterCodingException invalid) {
            throw new IOException("Skill archive path is not valid UTF-8", invalid);
        }
    }

    private static String localHeaderName(byte[] bytes, int offset) throws IOException {
        requireRange(bytes, offset, 30);
        if (uint32(bytes, offset) != LOCAL_HEADER) throw new IOException("Skill archive local entry is malformed");
        int flags = uint16(bytes, offset + 6);
        int method = uint16(bytes, offset + 8);
        int nameLength = uint16(bytes, offset + 26);
        int extraLength = uint16(bytes, offset + 28);
        if ((flags & ENCRYPTED_FLAG) != 0
                || (method != ZipEntry.STORED && method != ZipEntry.DEFLATED)) {
            throw new IOException("Skill archive local entry is encrypted or unsupported");
        }
        requireRange(bytes, offset + 30, nameLength + extraLength);
        requireUnambiguousEncoding(bytes, offset + 30, nameLength, flags);
        return decodeUtf8(bytes, offset + 30, nameLength);
    }

    private static void requireUnambiguousEncoding(byte[] bytes, int offset, int length, int flags) throws IOException {
        requireRange(bytes, offset, length);
        // ZIP writers may omit bit 11 for ASCII names; those bytes have identical UTF-8/legacy meaning.
        if ((flags & UTF8_FLAG) == 0) {
            for (int i = offset; i < offset + length; i++) {
                if ((bytes[i] & 0x80) != 0) throw new IOException("Non-ASCII Skill paths require explicit UTF-8 encoding");
            }
        }
    }

    private static int uint16(byte[] bytes, int offset) throws IOException {
        requireRange(bytes, offset, 2);
        return Short.toUnsignedInt(ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
    }

    private static long uint32(byte[] bytes, int offset) throws IOException {
        requireRange(bytes, offset, 4);
        return Integer.toUnsignedLong(ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
    }

    private static void requireRange(byte[] bytes, int offset, int length) throws IOException {
        if (offset < 0 || length < 0 || (long) offset + length > bytes.length) {
            throw new IOException("Skill archive is truncated");
        }
    }
}
