package com.liuzhihang.doc.view.service.export;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import junit.framework.TestCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ProjectDocumentExporterTest extends TestCase {

    private Path projectRoot;

    @Override
    protected void setUp() throws Exception {
        projectRoot = Files.createTempDirectory("doc-view-project-export-test-");
    }

    @Override
    protected void tearDown() throws Exception {
        if (projectRoot != null && Files.exists(projectRoot)) {
            try (var paths = Files.walk(projectRoot)) {
                for (Path path : paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    public void testRepeatedExportIsStableAndKeepsUnrelatedAndStalePages() throws Exception {
        ProjectDocumentPage first = page("core", "src/main/java/example/UserApi.java", "find", List.of("java.lang.String"),
                "# Find user\n\nUnique API body.\n");
        ProjectDocumentPage second = page("web", "web/src/main/java/example/OrderApi.java", "query", List.of("long"),
                "# Query order\n\nOrder body.\n");
        ProjectDocumentExporter exporter = new ProjectDocumentExporter();

        ProjectDocumentExportResult initial = export(exporter, List.of(first, second));
        Path firstFile = projectRoot.resolve("docs/doc-view/apis").resolve(first.identity().pageFileName());
        Path indexFile = projectRoot.resolve("docs/doc-view/index.md");
        byte[] firstBytes = Files.readAllBytes(firstFile);
        byte[] indexBytes = Files.readAllBytes(indexFile);
        Path unrelated = projectRoot.resolve("docs/doc-view/notes.md");
        Files.writeString(unrelated, "user-owned\n", StandardCharsets.UTF_8);

        export(exporter, List.of(first, second));

        assertTrue(initial.exportedPageCount() == 2);
        assertTrue(java.util.Arrays.equals(firstBytes, Files.readAllBytes(firstFile)));
        assertTrue(java.util.Arrays.equals(indexBytes, Files.readAllBytes(indexFile)));
        assertFalse(Files.readString(indexFile).contains("Unique API body"));
        assertEquals(1, occurrences(Files.readString(indexFile), identityMarker(first.identity())));
        assertTrue(Files.readString(indexFile).contains("## Module: `core`"));
        assertTrue(Files.readString(indexFile).contains("../../src/main/java/example/UserApi.java"));
        assertTrue(Files.readString(firstFile).contains("../../../src/main/java/example/UserApi.java"));

        ProjectDocumentPage updatedSecond = page("web", "web/src/main/java/example/OrderApi.java", "query", List.of("long"),
                "# Query order\n\nUpdated order body.\n");
        export(exporter, List.of(updatedSecond));

        assertTrue("stale managed pages are retained", Files.exists(firstFile));
        assertTrue("unrelated project files are retained", Files.exists(unrelated));
        assertEquals("user-owned\n", Files.readString(unrelated));
        assertFalse(Files.readString(indexFile).contains(first.identity().sha256()));
        Path secondFile = projectRoot.resolve("docs/doc-view/apis").resolve(second.identity().pageFileName());
        assertEquals(1, occurrences(Files.readString(secondFile), "Updated order body."));
        assertFalse(Files.readString(secondFile).contains("Order body."));
    }

    public void testIndexGroupsModulesAndClassesAndKeepsOverloadsDistinct() throws Exception {
        ProjectDocumentPage stringOverload = page("core", "src/main/java/example/UserApi.java", "example.UserApi",
                "find", List.of("java.lang.String"), "# Find by name\n");
        ProjectDocumentPage longOverload = page("core", "src/main/java/example/UserApi.java", "example.UserApi",
                "find", List.of("long"), "# Find by id\n");
        ProjectDocumentPage anotherClass = page("core", "src/main/java/example/OrderApi.java", "example.OrderApi",
                "query", List.of(), "# Query orders\n");
        ProjectDocumentPage sameClassInAnotherModule = page("web", "web/src/main/java/example/UserApi.java",
                "example.UserApi", "find", List.of("java.lang.String"), "# Web find\n");

        export(new ProjectDocumentExporter(),
                List.of(sameClassInAnotherModule, longOverload, anotherClass, stringOverload));

        String index = Files.readString(projectRoot.resolve("docs/doc-view/index.md"));
        int coreModule = index.indexOf("## Module: `core`");
        int webModule = index.indexOf("## Module: `web`");
        int orderClass = index.indexOf("### `example.OrderApi`");
        int userClass = index.indexOf("### `example.UserApi`");
        assertTrue(coreModule >= 0 && webModule > coreModule);
        assertTrue(orderClass >= 0 && userClass > orderClass);
        assertFalse(index.contains("| Identity | Module |"));
        assertTrue(index.contains("`find(java.lang.String)`"));
        assertTrue(index.contains("`find(long)`"));
        assertEquals(2, occurrences(index, "`find(java.lang.String)`"));
        assertTrue(index.contains("· `Spring` · [Document](apis/" + stringOverload.identity().pageFileName() + ")"));
        assertTrue(index.contains("[Source](../../src/main/java/example/UserApi.java)"));
        assertTrue(index.contains("[Source](../../web/src/main/java/example/UserApi.java)"));
        assertTrue(index.contains(identityMarker(stringOverload.identity())));
        assertTrue(index.contains(identityMarker(longOverload.identity())));
        assertTrue(index.contains(identityMarker(sameClassInAnotherModule.identity())));
        assertTrue(index.contains("apis/" + longOverload.identity().pageFileName() + ")"));
        assertTrue(index.contains("apis/" + sameClassInAnotherModule.identity().pageFileName() + ")"));
    }

    public void testUnmanagedTargetRejectsWholeBatchBeforePublishingAnyFile() throws Exception {
        ProjectDocumentPage newPage = page("core", "src/main/java/example/NewApi.java", "create", List.of(),
                "# Create\n");
        ProjectDocumentPage conflict = page("core", "src/main/java/example/UserApi.java", "find", List.of(),
                "# Find\n");
        Path apiDir = projectRoot.resolve("docs/doc-view/apis");
        Files.createDirectories(apiDir);
        Path conflictingFile = apiDir.resolve(conflict.identity().pageFileName());
        Files.writeString(conflictingFile, "user content\n", StandardCharsets.UTF_8);

        try {
            export(new ProjectDocumentExporter(), List.of(newPage, conflict));
            fail("expected ownership conflict");
        } catch (ProjectDocumentConflictException exception) {
            assertTrue(exception.conflicts().contains("docs/doc-view/apis/" + conflict.identity().pageFileName()));
        }

        assertFalse(Files.exists(apiDir.resolve(newPage.identity().pageFileName())));
        assertEquals("user content\n", Files.readString(conflictingFile));
        assertFalse(Files.exists(projectRoot.resolve("docs/doc-view/index.md")));
    }

    public void testEditedManagedPageBlocksTheWholeUpdate() throws Exception {
        ProjectDocumentPage original = page("core", "src/main/java/example/UserApi.java", "find", List.of(),
                "# Find\n\nOriginal.\n");
        ProjectDocumentPage newPage = page("core", "src/main/java/example/NewApi.java", "create", List.of(),
                "# Create\n");
        ProjectDocumentExporter exporter = new ProjectDocumentExporter();
        export(exporter, List.of(original));
        Path originalFile = projectRoot.resolve("docs/doc-view/apis").resolve(original.identity().pageFileName());
        byte[] indexBefore = Files.readAllBytes(projectRoot.resolve("docs/doc-view/index.md"));
        Files.writeString(originalFile, Files.readString(originalFile) + "\nmanual edit\n", StandardCharsets.UTF_8);
        byte[] editedPage = Files.readAllBytes(originalFile);

        try {
            export(exporter, List.of(original, newPage));
            fail("expected edited-page conflict");
        } catch (ProjectDocumentConflictException exception) {
            assertTrue(exception.conflicts().contains("docs/doc-view/apis/" + original.identity().pageFileName()));
        }

        assertTrue(java.util.Arrays.equals(editedPage, Files.readAllBytes(originalFile)));
        assertTrue(java.util.Arrays.equals(indexBefore, Files.readAllBytes(projectRoot.resolve("docs/doc-view/index.md"))));
        assertFalse(Files.exists(projectRoot.resolve("docs/doc-view/apis").resolve(newPage.identity().pageFileName())));
    }

    public void testEditedIndexBlocksNewPages() throws Exception {
        ProjectDocumentPage first = page("core", "src/main/java/example/UserApi.java", "find", List.of(), "# Find\n");
        ProjectDocumentPage second = page("core", "src/main/java/example/NewApi.java", "create", List.of(), "# Create\n");
        ProjectDocumentExporter exporter = new ProjectDocumentExporter();
        export(exporter, List.of(first));
        Path index = projectRoot.resolve("docs/doc-view/index.md");
        Files.writeString(index, Files.readString(index) + "\nmanual edit\n", StandardCharsets.UTF_8);
        byte[] editedIndex = Files.readAllBytes(index);

        try {
            export(exporter, List.of(first, second));
            fail("expected edited-index conflict");
        } catch (ProjectDocumentConflictException exception) {
            assertTrue(exception.conflicts().contains("docs/doc-view/index.md"));
        }

        assertTrue(java.util.Arrays.equals(editedIndex, Files.readAllBytes(index)));
        assertFalse(Files.exists(projectRoot.resolve("docs/doc-view/apis").resolve(second.identity().pageFileName())));
    }

    public void testInvalidUtf8PageIsConflictAndBlocksWholeBatch() throws Exception {
        ProjectDocumentPage invalidPage = page("core", "src/main/java/example/ExistingApi.java", "get", List.of(),
                "# Existing\n");
        ProjectDocumentPage newPage = page("core", "src/main/java/example/NewApi.java", "create", List.of(), "# New\n");
        Path apiDirectory = projectRoot.resolve("docs/doc-view/apis");
        Files.createDirectories(apiDirectory);
        Path invalidTarget = apiDirectory.resolve(invalidPage.identity().pageFileName());
        byte[] invalidBytes = {(byte) 0xC3, (byte) 0x28};
        Files.write(invalidTarget, invalidBytes);

        try {
            export(new ProjectDocumentExporter(), List.of(newPage, invalidPage));
            fail("expected undecodable page content to be reported as a conflict");
        } catch (ProjectDocumentConflictException exception) {
            assertTrue(exception.conflicts().contains("docs/doc-view/apis/" + invalidPage.identity().pageFileName()));
        }

        assertTrue(java.util.Arrays.equals(invalidBytes, Files.readAllBytes(invalidTarget)));
        assertFalse(Files.exists(apiDirectory.resolve(newPage.identity().pageFileName())));
        assertFalse(Files.exists(projectRoot.resolve("docs/doc-view/index.md")));
    }

    public void testInvalidUtf8IndexIsConflictAndBlocksWholeBatch() throws Exception {
        ProjectDocumentPage page = page("core", "src/main/java/example/NewApi.java", "create", List.of(), "# New\n");
        Path outputRoot = projectRoot.resolve("docs/doc-view");
        Files.createDirectories(outputRoot);
        Path index = outputRoot.resolve("index.md");
        byte[] invalidBytes = {(byte) 0xC3, (byte) 0x28};
        Files.write(index, invalidBytes);

        try {
            export(new ProjectDocumentExporter(), List.of(page));
            fail("expected undecodable index content to be reported as a conflict");
        } catch (ProjectDocumentConflictException exception) {
            assertTrue(exception.conflicts().contains("docs/doc-view/index.md"));
        }

        assertTrue(java.util.Arrays.equals(invalidBytes, Files.readAllBytes(index)));
        assertFalse(Files.exists(outputRoot.resolve("apis").resolve(page.identity().pageFileName())));
    }

    public void testTargetEditedAfterPreflightCancelsWholeBatch() throws Exception {
        ProjectDocumentPage existing = page("core", "src/main/java/example/ExistingApi.java", "get", List.of(),
                "# Existing\n");
        ProjectDocumentPage added = page("core", "src/main/java/example/NewApi.java", "create", List.of(), "# New\n");
        export(new ProjectDocumentExporter(), List.of(existing));
        Path existingFile = projectRoot.resolve("docs/doc-view/apis").resolve(existing.identity().pageFileName());
        Path indexFile = projectRoot.resolve("docs/doc-view/index.md");
        byte[] oldIndex = Files.readAllBytes(indexFile);

        ProjectDocumentExporter exporter = new ProjectDocumentExporter(new ProjectDocumentExporter.ExportObserver() {
            @Override
            public void afterStaging(Path root) throws IOException {
                Files.writeString(existingFile, "manual edit after preflight\n", StandardCharsets.UTF_8);
            }
        });
        try {
            export(exporter, List.of(existing, added));
            fail("expected the post-preflight edit to cancel the batch");
        } catch (ProjectDocumentConflictException exception) {
            assertTrue(exception.conflicts().contains("docs/doc-view/apis/" + existing.identity().pageFileName()));
        }

        assertEquals("manual edit after preflight\n", Files.readString(existingFile));
        assertTrue(java.util.Arrays.equals(oldIndex, Files.readAllBytes(indexFile)));
        assertFalse(Files.exists(projectRoot.resolve("docs/doc-view/apis").resolve(added.identity().pageFileName())));
        assertNoTemporaryFiles();
    }

    public void testTargetCreatedAfterPreflightCancelsWholeBatch() throws Exception {
        ProjectDocumentPage page = page("core", "src/main/java/example/NewApi.java", "create", List.of(), "# New\n");
        Path target = projectRoot.resolve("docs/doc-view/apis").resolve(page.identity().pageFileName());
        ProjectDocumentExporter exporter = new ProjectDocumentExporter(new ProjectDocumentExporter.ExportObserver() {
            @Override
            public void afterStaging(Path root) throws IOException {
                Files.writeString(target, "user-created after preflight\n", StandardCharsets.UTF_8);
            }
        });

        try {
            export(exporter, List.of(page));
            fail("expected the post-preflight target creation to cancel the batch");
        } catch (ProjectDocumentConflictException exception) {
            assertTrue(exception.conflicts().contains("docs/doc-view/apis/" + page.identity().pageFileName()));
        }

        assertEquals("user-created after preflight\n", Files.readString(target));
        assertFalse(Files.exists(projectRoot.resolve("docs/doc-view/index.md")));
        assertNoTemporaryFiles();
    }

    public void testCancellationAfterStagingKeepsExistingPagesAndIndexUnchanged() throws Exception {
        ProjectDocumentPage original = page("core", "src/main/java/example/UserApi.java", "find", List.of(),
                "# Find\n\nOriginal body.\n");
        ProjectDocumentExporter exporter = new ProjectDocumentExporter();
        export(exporter, List.of(original));
        Path pageFile = projectRoot.resolve("docs/doc-view/apis").resolve(original.identity().pageFileName());
        Path indexFile = projectRoot.resolve("docs/doc-view/index.md");
        byte[] originalPage = Files.readAllBytes(pageFile);
        byte[] originalIndex = Files.readAllBytes(indexFile);
        ProjectDocumentPage changed = page("core", "src/main/java/example/UserApi.java", "find", List.of(),
                "# Find\n\nUpdated body.\n");
        TestProgressIndicator indicator = new TestProgressIndicator();

        ProjectDocumentExporter cancelAfterStaging = new ProjectDocumentExporter(new ProjectDocumentExporter.ExportObserver() {
            @Override
            public void afterStaging(Path root) {
                indicator.cancel();
            }
        });
        try {
            cancelAfterStaging.export(projectRoot, List.of(changed), indicator);
            fail("expected cancellation after all files were staged");
        } catch (com.intellij.openapi.progress.ProcessCanceledException expected) {
            // No target is published until every new file is staged and cancellation is checked.
        }

        assertTrue(java.util.Arrays.equals(originalPage, Files.readAllBytes(pageFile)));
        assertTrue(java.util.Arrays.equals(originalIndex, Files.readAllBytes(indexFile)));
        assertNoTemporaryFiles();
    }

    public void testProjectCloseBeforePublicationCleansStagedFiles() throws Exception {
        ProjectDocumentPage page = page("core", "src/main/java/example/NewApi.java", "create", List.of(), "# New\n");
        AtomicBoolean projectOpen = new AtomicBoolean(true);
        AtomicInteger stagingCalls = new AtomicInteger();
        ProjectDocumentExporter exporter = new ProjectDocumentExporter(new ProjectDocumentExporter.ExportObserver() {
            @Override
            public void afterStaging(Path root) throws IOException {
                try (var paths = Files.walk(root)) {
                    assertTrue("project-close scenario must first create staged temp files",
                            paths.anyMatch(path -> path.getFileName() != null
                                    && path.getFileName().toString().startsWith(".doc-view-")
                                    && path.getFileName().toString().endsWith(".tmp")));
                }
                stagingCalls.incrementAndGet();
                projectOpen.set(false);
            }
        });
        try {
            exporter.export(projectRoot, List.of(page), new TestProgressIndicator(), projectOpen::get);
            fail("expected cancellation before publication");
        } catch (com.intellij.openapi.progress.ProcessCanceledException expected) {
            // Expected: no page or index can be published after the project closes.
        }

        assertEquals("staging observer must run exactly once before project close", 1, stagingCalls.get());
        assertFalse(Files.exists(projectRoot.resolve("docs/doc-view/apis").resolve(page.identity().pageFileName())));
        assertFalse(Files.exists(projectRoot.resolve("docs/doc-view/index.md")));
        assertNoTemporaryFiles();
    }

    public void testProjectAlreadyClosedBeforeWritingDoesNotCreateOutputDirectories() throws Exception {
        ProjectDocumentPage page = page("core", "src/main/java/example/NewApi.java", "create", List.of(), "# New\n");
        try {
            new ProjectDocumentExporter().export(projectRoot, List.of(page), new TestProgressIndicator(),
                    () -> false);
            fail("expected cancellation before output directories are created");
        } catch (com.intellij.openapi.progress.ProcessCanceledException expected) {
            // A project that is already closed must not leave directories or staged output behind.
        }

        assertFalse("closed project must not create docs/", Files.exists(projectRoot.resolve("docs")));
        assertFalse("closed project must not create docs/doc-view/", Files.exists(projectRoot.resolve("docs/doc-view")));
        assertFalse("closed project must not create the API directory", Files.exists(projectRoot.resolve("docs/doc-view/apis")));
        assertFalse("closed project must not create the index", Files.exists(projectRoot.resolve("docs/doc-view/index.md")));
        assertNoTemporaryFiles();
    }

    public void testPublishFailureReportsPublishedAndFailedRelativePathsThenRetryConverges() throws Exception {
        ProjectDocumentPage first = page("core", "src/main/java/example/FirstApi.java", "first", List.of(), "# First\n");
        ProjectDocumentPage second = page("core", "src/main/java/example/SecondApi.java", "second", List.of(), "# Second\n");
        AtomicInteger moveNumber = new AtomicInteger();
        ProjectDocumentExporter failingExporter = new ProjectDocumentExporter(new ProjectDocumentExporter.ExportObserver() {
            @Override
            public void beforeMove(String relativePath) throws IOException {
                if (moveNumber.incrementAndGet() == 2) {
                    throw new IOException("synthetic publish failure");
                }
            }
        });
        try {
            export(failingExporter, List.of(first, second));
            fail("expected partial publish");
        } catch (ProjectDocumentPublishException exception) {
            assertEquals(1, exception.publishedPaths().size());
            assertNotNull(exception.failedPath());
            assertTrue(exception.failedPath().startsWith("docs/doc-view/apis/"));
            assertTrue(exception.publishedPaths().getFirst().startsWith("docs/doc-view/apis/"));
        }

        assertFalse(Files.exists(projectRoot.resolve("docs/doc-view/index.md")));
        assertNoTemporaryFiles();
        export(new ProjectDocumentExporter(), List.of(first, second));
        assertTrue(Files.exists(projectRoot.resolve("docs/doc-view/index.md")));
    }

    public void testOutputDirectorySwapToSymlinkDoesNotWriteOutsideProject() throws Exception {
        Path outside = Files.createTempDirectory("doc-view-export-race-outside-");
        ProjectDocumentPage page = page("core", "src/main/java/example/NewApi.java", "create", List.of(), "# New\n");
        Path apiDirectory = projectRoot.resolve("docs/doc-view/apis");
        Path movedDirectory = projectRoot.resolve("docs/doc-view/apis-before-symlink");
        ProjectDocumentExporter exporter = new ProjectDocumentExporter(new ProjectDocumentExporter.ExportObserver() {
            @Override
            public void afterStaging(Path root) throws IOException {
                Files.move(apiDirectory, movedDirectory);
                Files.createSymbolicLink(apiDirectory, outside);
            }
        });

        try {
            export(exporter, List.of(page));
            fail("expected symlink swap detection");
        } catch (ProjectDocumentTemporaryCleanupException exception) {
            assertFalse(exception.temporaryPaths().isEmpty());
        } finally {
            try (var paths = Files.list(outside)) {
                assertEquals(0L, paths.count());
            }
            deleteTree(outside);
        }
    }

    public void testOutputSymlinkCannotEscapeProjectRoot() throws Exception {
        Path outside = Files.createTempDirectory("doc-view-export-outside-");
        Path docsLink = projectRoot.resolve("docs");
        try {
            Files.createSymbolicLink(docsLink, outside);
            ProjectDocumentPage page = page("core", "src/main/java/example/UserApi.java", "find", List.of(), "# Find\n");
            try {
                export(new ProjectDocumentExporter(), List.of(page));
                fail("expected symlink rejection");
            } catch (ProjectDocumentConflictException exception) {
                assertFalse(exception.conflicts().isEmpty());
            }
            try (var paths = Files.list(outside)) {
                assertEquals(0L, paths.count());
            }
        } catch (UnsupportedOperationException | IOException exception) {
            // The target is macOS, but keep the test portable when a filesystem disallows symlink creation.
            if (exception instanceof IOException && Files.exists(docsLink)) {
                throw exception;
            }
        } finally {
            try (var paths = Files.walk(outside)) {
                for (Path path : paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private ProjectDocumentExportResult export(ProjectDocumentExporter exporter,
                                                List<ProjectDocumentPage> pages) throws IOException {
        return exporter.export(projectRoot, pages, new TestProgressIndicator());
    }

    private static final class TestProgressIndicator implements ProgressIndicator {
        private volatile boolean canceled;

        @Override
        public void start() {}

        @Override
        public void stop() {}

        @Override
        public boolean isRunning() {
            return true;
        }

        @Override
        public void cancel() {
            canceled = true;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setText(String text) {}

        @Override
        public String getText() {
            return "";
        }

        @Override
        public void setText2(String text) {}

        @Override
        public String getText2() {
            return "";
        }

        @Override
        public double getFraction() {
            return 0;
        }

        @Override
        public void setFraction(double fraction) {}

        @Override
        public void pushState() {}

        @Override
        public void popState() {}

        @Override
        public boolean isModal() {
            return false;
        }

        @Override
        public ModalityState getModalityState() {
            return ModalityState.nonModal();
        }

        @Override
        public void setModalityProgress(ProgressIndicator modalityProgress) {}

        @Override
        public boolean isIndeterminate() {
            return false;
        }

        @Override
        public void setIndeterminate(boolean indeterminate) {}

        @Override
        public void checkCanceled() throws ProcessCanceledException {
            if (canceled) {
                throw new ProcessCanceledException();
            }
        }

        @Override
        public boolean isPopupWasShown() {
            return false;
        }

        @Override
        public boolean isShowing() {
            return false;
        }
    }

    private static ProjectDocumentPage page(String module,
                                            String sourcePath,
                                            String methodName,
                                            List<String> parameterTypes,
                                            String markdown) {
        return page(module, sourcePath, "example.Api", methodName, parameterTypes, markdown);
    }

    private static ProjectDocumentPage page(String module,
                                            String sourcePath,
                                            String qualifiedClassName,
                                            String methodName,
                                            List<String> parameterTypes,
                                            String markdown) {
        ProjectDocumentIdentity identity = ProjectDocumentIdentity.of(module, sourcePath, qualifiedClassName,
                methodName, parameterTypes);
        return new ProjectDocumentPage(identity, "Spring", markdown);
    }

    private static String identityMarker(ProjectDocumentIdentity identity) {
        return "<!-- doc-view-entry:v1 identity=" + identity.canonicalForm() + " sha256="
                + identity.sha256() + " -->";
    }

    private static int occurrences(String value, String fragment) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(fragment, index)) >= 0) {
            count++;
            index += fragment.length();
        }
        return count;
    }

    private void assertNoTemporaryFiles() throws IOException {
        if (!Files.exists(projectRoot)) {
            return;
        }
        try (var paths = Files.walk(projectRoot)) {
            assertFalse(paths.anyMatch(path -> path.getFileName() != null
                    && path.getFileName().toString().startsWith(".doc-view-")));
        }
    }

    private static void deleteTree(Path root) throws IOException {
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
