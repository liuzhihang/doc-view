package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Delivers the result of an asynchronous editor open on the EDT while dropping stale requests. */
public final class FileEditorOpenSupport {
    private FileEditorOpenSupport() {}

    public static <T> void whenOpened(Project project,
                                      CompletableFuture<T> openResult,
                                      BooleanSupplier isCurrent,
                                      Consumer<? super T> onOpened,
                                      Consumer<Throwable> onFailure) {
        openResult.whenComplete((result, failure) -> {
            Throwable cause = unwrap(failure);
            if (isCancellation(cause) || project.isDisposed()) return;
            ApplicationManager.getApplication().invokeLater(() -> {
                if (project.isDisposed() || !isCurrent.getAsBoolean()) return;
                if (cause == null) onOpened.accept(result);
                else onFailure.accept(cause);
            }, ModalityState.any());
        });
    }

    public static boolean isCancellation(Throwable failure) {
        Throwable cause = unwrap(failure);
        return cause instanceof CancellationException || cause instanceof ProcessCanceledException;
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
