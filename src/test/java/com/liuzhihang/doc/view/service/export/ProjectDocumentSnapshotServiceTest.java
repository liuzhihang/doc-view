package com.liuzhihang.doc.view.service.export;

import junit.framework.TestCase;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ProjectDocumentSnapshotServiceTest extends TestCase {

    public void testSourceSymlinkOutsideProjectHasNoProjectRelativePath() throws Exception {
        Path projectRoot = Files.createTempDirectory("doc-view-source-root-").toRealPath();
        Path outside = Files.createTempDirectory("doc-view-source-outside-").toRealPath();
        try {
            Path outsideSource = Files.writeString(outside.resolve("ExternalApi.java"), "interface ExternalApi {}\n");
            Path sourceLink = projectRoot.resolve("src/ExternalApi.java");
            Files.createDirectories(sourceLink.getParent());
            Files.createSymbolicLink(sourceLink, outsideSource);

            assertNull(ProjectDocumentSnapshotService.projectRelativeSourcePath(projectRoot, sourceLink));
        } finally {
            deleteTree(projectRoot);
            deleteTree(outside);
        }
    }

    private static void deleteTree(Path root) throws Exception {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted((left, right) -> right.getNameCount() - left.getNameCount()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
