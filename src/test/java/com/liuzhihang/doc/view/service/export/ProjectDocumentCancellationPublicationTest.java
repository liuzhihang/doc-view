package com.liuzhihang.doc.view.service.export;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.liuzhihang.doc.view.service.document.ProjectDocumentFormat;
import junit.framework.TestCase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.liuzhihang.doc.view.service.export.ProjectDocumentFixtures.*;

public final class ProjectDocumentCancellationPublicationTest extends TestCase {
    private Path root;
    private ProjectDocumentSnapshot snapshot;

    @Override protected void setUp() throws Exception {
        root = Files.createTempDirectory("doc-view-publication-");
        snapshot = snapshot(List.of(page(ProjectDocumentIdentity.of("fixture", "src/Api.java", "example.Api",
                "find", List.of("long")), "SPRING", "# Find\n")));
    }

    @Override protected void tearDown() throws Exception {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    public void testCancelBeforeEachMovePreservesCommitAndReportsPublishedPrefix() throws Exception {
        for (int stopBefore = 1; stopBefore <= 4; stopBefore++) {
            Path run = Files.createDirectory(root.resolve("run-" + stopBefore));
            var indicator = new ProjectDocumentExporterTest.TestProgressIndicator();
            AtomicInteger moves = new AtomicInteger();
            int boundary = stopBefore;
            var exporter = new ProjectDocumentExporter(new ProjectDocumentExporter.ExportObserver() {
                @Override public void beforeMove(String path) {
                    if (moves.incrementAndGet() == boundary) indicator.cancel();
                }
            });
            try {
                exporter.export(run, snapshot, indicator);
                fail("must cancel at boundary " + boundary);
            } catch (ProcessCanceledException cancelled) {
                assertEquals(1, boundary);
            } catch (ProjectDocumentPublishException partial) {
                assertEquals(boundary - 1, partial.publishedPaths().size());
                assertNotNull(partial.failedPath());
                assertFalse(partial.publishedPaths().contains(partial.failedPath()));
                assertTrue(partial.getCause() instanceof ProcessCanceledException);
                assertTrue(partial.temporaryPaths().isEmpty());
            }
            assertFalse(Files.exists(run.resolve("docs/doc-view/index.json")));
            assertNoTemps(run);
            new ProjectDocumentExporter().export(run, snapshot, new ProjectDocumentExporterTest.TestProgressIndicator());
            assertEquals(1, ProjectDocumentFormat.readIndex(Files.readAllBytes(
                    run.resolve("docs/doc-view/index.json"))).entries().size());
        }
    }

    public void testCancelImmediatelyAfterEachNonFinalMoveReportsPartial() throws Exception {
        for (int stopAfter = 1; stopAfter <= 3; stopAfter++) {
            Path run = Files.createDirectory(root.resolve("after-" + stopAfter));
            var indicator = new ProjectDocumentExporterTest.TestProgressIndicator();
            AtomicInteger moves = new AtomicInteger();
            int boundary = stopAfter;
            var exporter = new ProjectDocumentExporter(new ProjectDocumentExporter.ExportObserver() {
                @Override public void afterMove(String path) {
                    if (moves.incrementAndGet() == boundary) indicator.cancel();
                }
            });
            try {
                exporter.export(run, snapshot, indicator);
                fail("must report cancellation after move");
            } catch (ProjectDocumentPublishException partial) {
                assertEquals(boundary, partial.publishedPaths().size());
                assertNull("A file already published is not a failed target", partial.failedPath());
                assertTrue(partial.getCause() instanceof ProcessCanceledException);
                assertTrue(partial.temporaryPaths().isEmpty());
            }
            assertFalse(Files.exists(run.resolve("docs/doc-view/index.json")));
            assertNoTemps(run);
        }
    }

    public void testFinalIndexCommitWinsLateCancellation() throws Exception {
        var indicator = new ProjectDocumentExporterTest.TestProgressIndicator();
        AtomicBoolean installed = new AtomicBoolean();
        var result = new ProjectDocumentExporter().export(root, snapshot, indicator, () -> true, () -> {}, index -> {
            assertTrue(Files.isRegularFile(root.resolve("docs/doc-view/index.json")));
            installed.set(true);
            indicator.cancel();
        });
        assertEquals(4, result.changedPaths().size());
        assertEquals("docs/doc-view/index.json", result.changedPaths().getLast());
        assertTrue(installed.get());
        assertTrue(indicator.isCanceled());
        assertNoTemps(root);
    }

    public void testDisposalAfterFirstMoveStopsBeforeCommit() throws Exception {
        AtomicBoolean live = new AtomicBoolean(true);
        var exporter = new ProjectDocumentExporter(new ProjectDocumentExporter.ExportObserver() {
            @Override public void afterMove(String path) { live.set(false); }
        });
        try {
            exporter.export(root, snapshot, new ProjectDocumentExporterTest.TestProgressIndicator(),
                    live::get, () -> {}, index -> fail("must not install"));
            fail("must stop disposed generation");
        } catch (ProjectDocumentPublishException partial) {
            assertEquals(1, partial.publishedPaths().size());
            assertNull(partial.failedPath());
            assertTrue(partial.getCause() instanceof ProcessCanceledException);
        }
        assertFalse(Files.exists(root.resolve("docs/doc-view/index.json")));
        assertNoTemps(root);
    }

    public void testObservedSourceChangeAfterFirstMoveStopsAndRetryConverges() throws Exception {
        AtomicBoolean current = new AtomicBoolean(true);
        var exporter = new ProjectDocumentExporter(new ProjectDocumentExporter.ExportObserver() {
            @Override public void afterMove(String path) { current.set(false); }
        });
        try {
            exporter.export(root, snapshot, new ProjectDocumentExporterTest.TestProgressIndicator(), () -> true, () -> {
                if (!current.get()) throw new ProjectDocumentGenerationException(
                        ProjectDocumentGenerationException.Code.SOURCE_CHANGED);
            }, index -> fail("must not install"));
            fail("must stop stale generation");
        } catch (ProjectDocumentPublishException partial) {
            assertEquals(1, partial.publishedPaths().size());
            assertNull(partial.failedPath());
            assertEquals(ProjectDocumentGenerationException.Code.SOURCE_CHANGED,
                    ((ProjectDocumentGenerationException) partial.getCause()).code());
        }
        assertNoTemps(root);
        new ProjectDocumentExporter().export(root, snapshot, new ProjectDocumentExporterTest.TestProgressIndicator());
        assertTrue(Files.isRegularFile(root.resolve("docs/doc-view/index.json")));
    }

    private static void assertNoTemps(Path root) throws Exception {
        try (var paths = Files.walk(root)) {
            assertFalse(paths.anyMatch(path -> path.toString().endsWith(".tmp")));
        }
    }
}
