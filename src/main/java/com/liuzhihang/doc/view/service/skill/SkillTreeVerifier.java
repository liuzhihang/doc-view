package com.liuzhihang.doc.view.service.skill;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Exact, bounded inventory/hash check for a Doc View managed Skill tree. */
final class SkillTreeVerifier {
    private SkillTreeVerifier() {}

    static boolean matches(Path target, SkillInstallMetadata metadata) throws IOException {
        if (Files.isSymbolicLink(target) || !Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)) return false;
        Map<String, SkillReleaseManifest.FileRecord> expectedFiles = new HashMap<>();
        Set<String> expectedDirectories = new HashSet<>();
        Set<String> folded = new HashSet<>();
        for (var file : metadata.files()) {
            SkillReleaseManifest.validateMetadataPath(file.path());
            if (!folded.add(file.path().toLowerCase(Locale.ROOT))) return false;
            expectedFiles.put(file.path(), file);
            int separator = file.path().indexOf('/');
            while (separator >= 0) {
                expectedDirectories.add(file.path().substring(0, separator));
                separator = file.path().indexOf('/', separator + 1);
            }
        }
        Set<String> foundFiles = new HashSet<>();
        Set<String> foundDirectories = new HashSet<>();
        boolean[] valid = {true};
        Files.walkFileTree(target, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
                if (attributes.isSymbolicLink() || Files.isSymbolicLink(directory)) {
                    valid[0] = false;
                    return FileVisitResult.TERMINATE;
                }
                if (!directory.equals(target)) {
                    String relative = relative(target, directory);
                    if (!expectedDirectories.contains(relative)) {
                        valid[0] = false;
                        return FileVisitResult.TERMINATE;
                    }
                    foundDirectories.add(relative);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                if (!attributes.isRegularFile() || attributes.isSymbolicLink() || Files.isSymbolicLink(file)) {
                    valid[0] = false;
                    return FileVisitResult.TERMINATE;
                }
                String relative = relative(target, file);
                SkillReleaseManifest.FileRecord record = expectedFiles.get(relative);
                if (record == null || !foundFiles.add(relative) || attributes.size() != record.size()
                        || attributes.size() > SkillReleaseManifest.MAX_FILE_BYTES) {
                    valid[0] = false;
                    return FileVisitResult.TERMINATE;
                }
                byte[] bytes = Files.readAllBytes(file);
                if (!SkillReleaseManifest.sha256(bytes).equals(record.sha256())) {
                    valid[0] = false;
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException failure) {
                valid[0] = false;
                return FileVisitResult.TERMINATE;
            }
        });
        return valid[0] && foundFiles.equals(expectedFiles.keySet()) && foundDirectories.equals(expectedDirectories);
    }

    static boolean matchesArchive(Path target, SkillInstallMetadata metadata) throws IOException {
        return matches(target, metadata);
    }

    private static String relative(Path target, Path path) {
        return target.relativize(path).toString().replace(path.getFileSystem().getSeparator(), "/");
    }
}
