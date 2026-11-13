package com.liuzhihang.doc.view.integration.mcp;

import com.google.gson.JsonObject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseListener;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.liuzhihang.doc.view.service.mcp.DocViewMcpExecutor;
import com.liuzhihang.doc.view.service.export.ProjectDocumentExportGuard;
import com.intellij.mcpserver.McpToolCallResult;
import kotlin.Unit;
import kotlinx.coroutines.CancellableContinuation;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;

/** Project-owned requests. Cancellation cancels work, not just the protocol response. */
@Service(Service.Level.PROJECT)
public final class DocViewMcpTaskService implements Disposable {
    private final Project project;
    private final Executor worker;
    private final ScheduledExecutorService scheduler;
    private final Operation operation;
    private final long readTimeoutMillis;
    private final long writeTimeoutMillis;
    private final Set<Run> active = new HashSet<>();
    private boolean disposed;
    @FunctionalInterface interface Operation {
        JsonObject execute(String tool, JsonObject arguments, ProgressIndicator indicator);
    }
    public DocViewMcpTaskService(Project project) {
        this(project, AppExecutorUtil.getAppExecutorService(), AppExecutorUtil.getAppScheduledExecutorService(),
                null, 5_000, 120_000);
        ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(ProjectCloseListener.TOPIC,
                new ProjectCloseListener() {
                    @Override public void projectClosing(@NotNull Project closing) {
                        if (closing == project) dispose();
                    }
                });
    }
    DocViewMcpTaskService(Project project, Executor worker, ScheduledExecutorService scheduler,
                         Operation operation, long readTimeoutMillis, long writeTimeoutMillis) {
        this.project = project; this.worker = worker; this.scheduler = scheduler;
        this.operation = operation == null ? (tool, args, indicator) ->
                new DocViewMcpExecutor(this::isGenerating).execute(project, tool, args, indicator) : operation;
        this.readTimeoutMillis = readTimeoutMillis; this.writeTimeoutMillis = writeTimeoutMillis;
    }
    synchronized boolean isGenerating() {
        return active.stream().anyMatch(run -> run.write)
                || ProjectDocumentExportGuard.getInstance(project).isGenerating();
    }
    synchronized int activeCount() { return active.size(); }
    void submit(String tool, JsonObject args, CancellableContinuation<? super McpToolCallResult> continuation) {
        Run run = new Run(tool, args, continuation);
        synchronized (this) {
            if (disposed || project.isDisposed()) {
                continuation.resumeWith(DocViewMcpToolsProvider.error(args, "PROJECT_NOT_OPEN"));
                return;
            }
            long matching = active.stream().filter(r -> r.write == run.write).count();
            if (matching >= (run.write ? 1 : 4)) {
                continuation.resumeWith(DocViewMcpToolsProvider.error(args, "BUSY"));
                return;
            }
            active.add(run);
        }
        continuation.invokeOnCancellation(cause -> { run.cancel("CANCELLED"); return Unit.INSTANCE; });
        try {
            run.deadline = scheduler.schedule(() -> run.cancel("TIMEOUT"),
                    run.write ? writeTimeoutMillis : readTimeoutMillis, TimeUnit.MILLISECONDS);
            worker.execute(run);
        } catch (RuntimeException rejected) {
            run.finish();
            if (!continuation.isCancelled()) continuation.resumeWith(DocViewMcpToolsProvider.error(args, "CANCELLED"));
        }
    }
    @Override public synchronized void dispose() {
        if (disposed) return;
        disposed = true;
        active.forEach(run -> run.cancel("PROJECT_NOT_OPEN"));
    }
    private final class Run implements Runnable {
        final String tool;
        final JsonObject arguments;
        final boolean write;
        final CancellableContinuation<? super McpToolCallResult> continuation;
        final AtomicReference<String> cancellation = new AtomicReference<>();
        final EmptyProgressIndicator indicator = new EmptyProgressIndicator() {
            @Override public void start() {
                super.start();
                // runProcess starts the indicator and resets its flag. Preserve cancellation received while queued.
                if (cancellation.get() != null) cancel();
            }
        };
        volatile ScheduledFuture<?> deadline;
        Run(String tool, JsonObject args, CancellableContinuation<? super McpToolCallResult> continuation) {
            this.tool = tool; this.arguments = args; this.continuation = continuation;
            this.write = DocViewMcpExecutor.GENERATE.equals(tool);
        }
        void cancel(String code) { cancellation.compareAndSet(null, code); indicator.cancel(); }
        @Override public void run() {
            McpToolCallResult response;
            try {
                String stopped = cancellation.get();
                if (stopped != null) throw new ProcessCanceledException();
                JsonObject value = ProgressManager.getInstance().runProcess(() ->
                        operation.execute(tool, arguments, indicator), indicator);
                // A committed or partial publication keeps its real outcome; a pre-commit timeout has a distinct code.
                if (value.has("error") && value.get("error").isJsonObject()
                        && "CANCELLED".equals(value.getAsJsonObject("error").get("code").getAsString())
                        && cancellation.get() != null) {
                    response = DocViewMcpToolsProvider.error(arguments, cancellation.get());
                } else response = DocViewMcpToolsProvider.result(value);
            } catch (ProcessCanceledException stopped) {
                response = DocViewMcpToolsProvider.error(arguments, cancellation.get() == null ? "CANCELLED" : cancellation.get());
            } catch (CancellationException stopped) {
                response = DocViewMcpToolsProvider.error(arguments, cancellation.get() == null ? "CANCELLED" : cancellation.get());
            } catch (RuntimeException failure) {
                response = DocViewMcpToolsProvider.error(arguments, "IO_ERROR");
            } finally { finish(); }
            if (!continuation.isCancelled()) continuation.resumeWith(response);
        }
        void finish() {
            ScheduledFuture<?> task = deadline;
            if (task != null) task.cancel(false);
            synchronized (DocViewMcpTaskService.this) { active.remove(this); }
        }
    }
}
