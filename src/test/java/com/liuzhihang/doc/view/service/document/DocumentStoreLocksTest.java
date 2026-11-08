package com.liuzhihang.doc.view.service.document;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import junit.framework.TestCase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class DocumentStoreLocksTest extends TestCase {

    public void testReadAndWriteLocksUseCanonicalProjectRootAndRespectTimeout() throws Exception {
        Path projectRoot = Files.createTempDirectory("doc-view-lock-root-");
        Path alias = projectRoot.resolveSibling(projectRoot.getFileName() + "-alias");
        Files.createSymbolicLink(alias, projectRoot);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Path canonicalDocumentRoot = projectRoot.toRealPath().resolve("docs/doc-view");
            try (DocumentStoreLocks.Lease write = DocumentStoreLocks.tryAcquireWrite(projectRoot,
                    new EmptyProgressIndicator(), Duration.ofSeconds(1))) {
                assertNotNull(write);
                assertEquals(canonicalDocumentRoot, write.documentRoot());
                var blockedRead = executor.submit(() -> DocumentStoreLocks.tryAcquireRead(alias,
                        new EmptyProgressIndicator(), Duration.ofMillis(80)));
                assertNull("An aliased project root must share the active writer lock",
                        blockedRead.get(2, TimeUnit.SECONDS));
            }

            try (DocumentStoreLocks.Lease read = DocumentStoreLocks.tryAcquireRead(alias,
                    new EmptyProgressIndicator(), Duration.ofSeconds(1))) {
                assertNotNull(read);
                assertEquals(canonicalDocumentRoot, read.documentRoot());
            }
        } finally {
            executor.shutdownNow();
            Files.deleteIfExists(alias);
            deleteTree(projectRoot);
        }
    }

    public void testCanceledLockWaitStopsPromptly() throws Exception {
        Path projectRoot = Files.createTempDirectory("doc-view-lock-cancel-");
        try (DocumentStoreLocks.Lease write = DocumentStoreLocks.tryAcquireWrite(projectRoot,
                new EmptyProgressIndicator(), Duration.ofSeconds(1))) {
            assertNotNull(write);
            EmptyProgressIndicator canceled = new EmptyProgressIndicator();
            canceled.cancel();
            try {
                DocumentStoreLocks.tryAcquireRead(projectRoot, canceled, Duration.ofSeconds(1));
                fail("A canceled lock request must not wait for the writer");
            } catch (ProcessCanceledException expected) {
                // The caller can translate this to a canceled query or publish operation.
            }
        } finally {
            deleteTree(projectRoot);
        }
    }

    private static void deleteTree(Path root) throws Exception {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
