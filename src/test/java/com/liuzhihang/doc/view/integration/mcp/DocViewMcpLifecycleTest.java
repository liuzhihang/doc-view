package com.liuzhihang.doc.view.integration.mcp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.mcpserver.McpToolCallResult;
import com.intellij.mcpserver.McpToolCallResultContent;
import com.intellij.testFramework.LightPlatformTestCase;
import com.liuzhihang.doc.view.service.mcp.DocViewMcpExecutor;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlinx.coroutines.CancellableContinuationKt;
import kotlinx.coroutines.Job;
import kotlinx.coroutines.JobKt;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Real coroutine cancellation with a deterministic queue, independent from transport acceptance. */
public final class DocViewMcpLifecycleTest extends LightPlatformTestCase {
    private ScheduledExecutorService scheduler;
    private final List<DocViewMcpTaskService> services = new ArrayList<>();
    @Override protected void setUp() throws Exception {
        super.setUp(); scheduler = Executors.newSingleThreadScheduledExecutor();
    }
    @Override protected void tearDown() throws Exception {
        try { services.forEach(DocViewMcpTaskService::dispose); scheduler.shutdownNow(); }
        finally { super.tearDown(); }
    }
    private DocViewMcpTaskService service(Executor worker, DocViewMcpTaskService.Operation operation, long timeout) {
        var service = new DocViewMcpTaskService(getProject(), worker, scheduler, operation, timeout, timeout);
        services.add(service); return service;
    }
    public void testProjectServiceCanBeInstantiated() {
        assertNotNull(getProject().getService(DocViewMcpTaskService.class));
    }
    public void testGeneratingStatusIncludesTheSharedUiGenerationGuard() {
        var service = getProject().getService(DocViewMcpTaskService.class);
        var guard = com.liuzhihang.doc.view.service.export.ProjectDocumentExportGuard.getInstance(getProject());
        assertFalse(service.isGenerating());
        assertTrue(guard.tryStart());
        try { assertTrue(service.isGenerating()); }
        finally { guard.finish(); }
        assertFalse(service.isGenerating());
    }
    public void testParentCancellationStopsInFlightOperationBeforeRecovery() throws Exception {
        var worker = Executors.newSingleThreadExecutor(); var started = new CountDownLatch(1);
        var released = new AtomicInteger();
        try {
            var service = service(worker, (tool, args, indicator) -> {
                started.countDown();
                try {
                    while (true) {
                        indicator.checkCanceled();
                        java.util.concurrent.locks.LockSupport.parkNanos(1_000_000);
                    }
                } finally { released.incrementAndGet(); }
            }, 5000);
            Request request = submit(service, DocViewMcpExecutor.STATUS);
            assertTrue(started.await(2, TimeUnit.SECONDS));
            request.job.cancel(new CancellationException("client cancelled in flight"));
            worker.submit(() -> {}).get(2, TimeUnit.SECONDS);
            assertEquals(1, released.get()); assertEquals(0, service.activeCount());
            assertTrue(request.result.isCompletedExceptionally());
        } finally { worker.shutdownNow(); }
    }
    public void testParentCancellationBeforeWorkerDoesNotRunOperationAndReleasesSlot() throws Exception {
        var queue = new LinkedBlockingQueue<Runnable>(); var executions = new AtomicInteger();
        var service = service(queue::add, (tool, args, indicator) -> {
            executions.incrementAndGet(); return DocViewMcpExecutor.error(null, "INDEX_NOT_FOUND");
        }, 5000);
        Request request = submit(service, DocViewMcpExecutor.STATUS);
        request.job.cancel(new CancellationException("client cancelled"));
        queue.take().run();
        assertEquals(0, executions.get()); assertEquals(0, service.activeCount());
        assertTrue(request.job.isCancelled());
        Request next = submit(service, DocViewMcpExecutor.STATUS); queue.take().run();
        assertEquals("INDEX_NOT_FOUND", code(next.result.get(2, TimeUnit.SECONDS)));
        assertEquals(1, executions.get()); assertEquals(0, service.activeCount());
    }
    public void testReadLimitAndWriteLimitAreSeparateAndBusyDoesNotQueue() throws Exception {
        var queue = new LinkedBlockingQueue<Runnable>();
        var service = service(queue::add, (tool, args, indicator) -> DocViewMcpExecutor.error(null, "INDEX_NOT_FOUND"), 5000);
        for (int i = 0; i < 4; i++) submit(service, DocViewMcpExecutor.STATUS);
        assertEquals("BUSY", code(submit(service, DocViewMcpExecutor.SEARCH).result.get(2, TimeUnit.SECONDS)));
        submit(service, DocViewMcpExecutor.GENERATE);
        assertEquals("BUSY", code(submit(service, DocViewMcpExecutor.GENERATE).result.get(2, TimeUnit.SECONDS)));
        assertEquals(5, queue.size()); assertEquals(5, service.activeCount());
        while (!queue.isEmpty()) queue.take().run();
        assertEquals(0, service.activeCount()); assertFalse(service.isGenerating());
    }
    public void testTimeoutAndProjectDisposalHaveDistinctTerminalCodes() throws Exception {
        var queue = new LinkedBlockingQueue<Runnable>();
        var service = service(queue::add, (tool, args, indicator) -> { fail("expired request executed"); return null; }, 1);
        Request timed = submit(service, DocViewMcpExecutor.STATUS);
        // A barrier on the same single scheduler ensures the request deadline ran first.
        scheduler.schedule(() -> {}, 10, TimeUnit.MILLISECONDS).get(2, TimeUnit.SECONDS);
        queue.take().run(); assertEquals("TIMEOUT", code(timed.result.get(2, TimeUnit.SECONDS)));
        var closingService = service(queue::add, (tool, args, indicator) -> { fail("closed request executed"); return null; }, 5000);
        Request closing = submit(closingService, DocViewMcpExecutor.STATUS); closingService.dispose(); queue.take().run();
        assertEquals("PROJECT_NOT_OPEN", code(closing.result.get(2, TimeUnit.SECONDS)));
        assertEquals(0, closingService.activeCount());
        assertEquals("PROJECT_NOT_OPEN", code(submit(closingService, DocViewMcpExecutor.STATUS).result.get(2, TimeUnit.SECONDS)));
    }
    public void testServiceDisposalCancelsRunningOperationAndFinishesExactlyOnce() throws Exception {
        var worker = Executors.newSingleThreadExecutor();
        var started = new CountDownLatch(1);
        var released = new AtomicInteger();
        var taskService = service(worker, (tool, args, indicator) -> {
            started.countDown();
            try {
                while (true) {
                    indicator.checkCanceled();
                    java.util.concurrent.locks.LockSupport.parkNanos(1_000_000);
                }
            } finally {
                released.incrementAndGet();
            }
        }, 5000);
        try {
            Request request = submit(taskService, DocViewMcpExecutor.GENERATE);
            assertTrue("The synthetic generate operation should be in flight", started.await(2, TimeUnit.SECONDS));

            taskService.dispose();
            assertEquals("PROJECT_NOT_OPEN", code(request.result.get(2, TimeUnit.SECONDS)));
            worker.submit(() -> {}).get(2, TimeUnit.SECONDS);

            assertEquals("Disposal reaches the operation cleanup exactly once", 1, released.get());
            assertEquals("The coroutine continuation receives one terminal completion", 1,
                    request.completions.get());
            assertEquals(0, taskService.activeCount());
            assertFalse("Disposed generation work no longer occupies the shared writer slot",
                    taskService.isGenerating());
            Request afterClose = submit(taskService, DocViewMcpExecutor.STATUS);
            assertEquals("PROJECT_NOT_OPEN", code(afterClose.result.get(2, TimeUnit.SECONDS)));
            assertEquals(1, afterClose.completions.get());
            assertEquals(0, taskService.activeCount());
        } finally {
            taskService.dispose();
            worker.shutdownNow();
        }
    }
    public void testRejectedSchedulingDoesNotLeakSlot() throws Exception {
        scheduler.shutdownNow();
        var service = service(Runnable::run, (tool, args, indicator) -> { fail("rejected request executed"); return null; }, 5000);
        Request rejected = submit(service, DocViewMcpExecutor.STATUS);
        assertEquals("CANCELLED", code(rejected.result.get(2, TimeUnit.SECONDS)));
        assertEquals(0, service.activeCount());
    }
    public void testCoroutineCancellationExceptionDoesNotBecomeIoError() throws Exception {
        var service = service(Runnable::run, (tool, args, indicator) -> { throw new CancellationException("fixture"); }, 5000);
        assertEquals("CANCELLED", code(submit(service, DocViewMcpExecutor.STATUS).result.get(2, TimeUnit.SECONDS)));
        assertEquals(0, service.activeCount());
    }
    private static String code(McpToolCallResult result) {
        String text = ((McpToolCallResultContent.Text) result.getContent()[0]).getText();
        return JsonParser.parseString(text).getAsJsonObject().getAsJsonObject("error").get("code").getAsString();
    }
    private record Request(Job job, CompletableFuture<McpToolCallResult> result, AtomicInteger completions) {}
    private static Request submit(DocViewMcpTaskService service, String tool) {
        Job job = JobKt.Job((Job) null); var future = new CompletableFuture<McpToolCallResult>();
        var completions = new AtomicInteger();
        JsonObject args = new JsonObject(); args.addProperty("projectPath", "/fictional");
        Continuation<McpToolCallResult> completion = new Continuation<>() {
            @Override public CoroutineContext getContext() { return job; }
            @Override public void resumeWith(Object result) {
                completions.incrementAndGet();
                try { ResultKt.throwOnFailure(result); future.complete((McpToolCallResult) result); }
                catch (Throwable failure) { future.completeExceptionally(failure); }
            }
        };
        Object value = CancellableContinuationKt.suspendCancellableCoroutine(cc -> {
            service.submit(tool, args, cc); return Unit.INSTANCE;
        }, completion);
        if (value != IntrinsicsKt.getCOROUTINE_SUSPENDED()) completion.resumeWith(value);
        return new Request(job, future, completions);
    }
}
