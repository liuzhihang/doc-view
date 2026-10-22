package com.liuzhihang.doc.view.service.export;

import junit.framework.TestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class ProjectDocumentExportGuardTest extends TestCase {

    public void testOnlyOneProjectWriteCanRunAtATimeAndGuardCanBeReused() {
        ProjectDocumentExportGuard guard = new ProjectDocumentExportGuard();

        assertTrue(guard.tryStart());
        assertFalse(guard.tryStart());

        guard.finish();
        assertTrue(guard.tryStart());
        guard.finish();
    }

    public void testConcurrentProjectExportIsRejectedUntilActiveWriteFinishes() throws Exception {
        ProjectDocumentExportGuard guard = new ProjectDocumentExportGuard();
        CountDownLatch activeWriteStarted = new CountDownLatch(1);
        CountDownLatch finishActiveWrite = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var activeWrite = executor.submit(() -> {
                assertTrue(guard.tryStart());
                activeWriteStarted.countDown();
                try {
                    try {
                        assertTrue(finishActiveWrite.await(5, TimeUnit.SECONDS));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(interrupted);
                    }
                } finally {
                    guard.finish();
                }
            });
            assertTrue(activeWriteStarted.await(5, TimeUnit.SECONDS));
            var overlappingWrite = executor.submit(guard::tryStart);
            assertFalse(overlappingWrite.get(5, TimeUnit.SECONDS));

            finishActiveWrite.countDown();
            activeWrite.get(5, TimeUnit.SECONDS);
            assertTrue(guard.tryStart());
            guard.finish();
        } finally {
            finishActiveWrite.countDown();
            executor.shutdownNow();
        }
    }
}
