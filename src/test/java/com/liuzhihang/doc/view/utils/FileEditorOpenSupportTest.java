package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class FileEditorOpenSupportTest extends LightPlatformTestCase {

    public void testSuccessWaitsForFutureAndIsDeliveredOnEdt() throws Exception {
        Application application = ApplicationManager.getApplication();
        CompletableFuture<String> result = new CompletableFuture<>();
        AtomicBoolean opened = new AtomicBoolean();
        AtomicBoolean onEdt = new AtomicBoolean();
        CompletableFuture<Void> delivered = new CompletableFuture<>();

        FileEditorOpenSupport.whenOpened(getProject(), result, () -> true, value -> {
            opened.set("editor".equals(value));
            onEdt.set(application.isDispatchThread());
            delivered.complete(null);
        }, error -> {
            delivered.completeExceptionally(error);
        });

        assertFalse("A successful open must not be reported before the future completes", opened.get());
        result.complete("editor");
        PlatformTestUtil.waitForFuture(delivered, 5_000);
        assertTrue(opened.get());
        assertTrue(onEdt.get());
    }

    public void testAsynchronousFailureIsUnwrappedAndDelivered() throws Exception {
        CompletableFuture<String> result = new CompletableFuture<>();
        IllegalStateException expected = new IllegalStateException("editor open failed");
        AtomicReference<Throwable> received = new AtomicReference<>();
        CompletableFuture<Void> delivered = new CompletableFuture<>();

        FileEditorOpenSupport.whenOpened(getProject(), result, () -> true, value -> fail("must not open"), error -> {
            received.set(error);
            delivered.complete(null);
        });

        result.completeExceptionally(new CompletionException(expected));
        PlatformTestUtil.waitForFuture(delivered, 5_000);
        assertSame(expected, received.get());
    }

    public void testCancellationAndStaleCompletionAreSilent() throws Exception {
        Application application = ApplicationManager.getApplication();
        AtomicBoolean callback = new AtomicBoolean();
        CompletableFuture<String> cancelled = new CompletableFuture<>();
        FileEditorOpenSupport.whenOpened(getProject(), cancelled, () -> true,
                value -> callback.set(true), error -> callback.set(true));
        cancelled.completeExceptionally(new CancellationException("project closed"));
        assertFalse("Cancellation must not become an open error", callback.get());
        CompletableFuture<String> processCancelled = new CompletableFuture<>();
        FileEditorOpenSupport.whenOpened(getProject(), processCancelled, () -> true,
                value -> callback.set(true), error -> callback.set(true));
        processCancelled.completeExceptionally(new CompletionException(
                new com.intellij.openapi.progress.ProcessCanceledException()));
        assertFalse("A platform cancellation must also remain silent", callback.get());

        CompletableFuture<String> stale = new CompletableFuture<>();
        AtomicBoolean current = new AtomicBoolean(true);
        EdtTestUtil.runInEdtAndWait(() -> {
            FileEditorOpenSupport.whenOpened(getProject(), stale, current::get,
                    value -> callback.set(true), error -> callback.set(true));
            current.set(false);
            stale.completeExceptionally(new IllegalStateException("late open failure"));
        });
        CompletableFuture<Void> eventQueueTail = new CompletableFuture<>();
        application.invokeLater(() -> eventQueueTail.complete(null));
        PlatformTestUtil.waitForFuture(eventQueueTail, 5_000);
        assertFalse("A stale completion must not update the UI", callback.get());
    }

    public void testProjectDisposalBeforeEdtDeliveryIsSilent() throws Exception {
        Application application = ApplicationManager.getApplication();
        AtomicBoolean disposed = new AtomicBoolean();
        AtomicBoolean callback = new AtomicBoolean();
        Project queuedProject = (Project) java.lang.reflect.Proxy.newProxyInstance(
                Project.class.getClassLoader(), new Class<?>[]{Project.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isDisposed" -> disposed.get();
                    case "toString" -> "disposed-open-test-project";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.toString());
                });
        CompletableFuture<String> result = new CompletableFuture<>();

        EdtTestUtil.runInEdtAndWait(() -> {
            FileEditorOpenSupport.whenOpened(queuedProject, result, () -> true,
                    value -> callback.set(true), error -> callback.set(true));
            result.completeExceptionally(new IllegalStateException("late open failure"));
            disposed.set(true);
        });

        CompletableFuture<Void> eventQueueTail = new CompletableFuture<>();
        application.invokeLater(() -> eventQueueTail.complete(null));
        PlatformTestUtil.waitForFuture(eventQueueTail, 5_000);
        assertFalse("Closing a project must suppress its late open failure", callback.get());
    }
}
