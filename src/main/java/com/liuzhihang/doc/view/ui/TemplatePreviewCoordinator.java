package com.liuzhihang.doc.view.ui;

import com.liuzhihang.doc.view.enums.FrameworkEnum;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/** Coordinates background template preview work and rejects stale results. */
public final class TemplatePreviewCoordinator {

    @FunctionalInterface
    public interface Renderer {
        String render(String template, FrameworkEnum framework) throws Exception;
    }

    public record Result(String markdown, String html, String errorMessage) {
        public boolean isSuccess() {
            return errorMessage == null;
        }

        public static Result success(String markdown) {
            return new Result(markdown, null, null);
        }

        public static Result failure(Throwable error) {
            String message = error.getMessage();
            if (message == null || message.isBlank()) {
                message = error.getClass().getSimpleName();
            }
            return new Result(null, null, message);
        }
    }

    private final Executor backgroundExecutor;
    private final Consumer<Runnable> uiExecutor;
    private final Renderer renderer;
    private final java.util.function.Function<String, String> htmlRenderer;
    private long latestRequest;
    private boolean disposed;

    public TemplatePreviewCoordinator(Executor backgroundExecutor,
                                      Consumer<Runnable> uiExecutor,
                                      Renderer renderer) {
        this(backgroundExecutor, uiExecutor, renderer, null);
    }

    public TemplatePreviewCoordinator(Executor backgroundExecutor, Consumer<Runnable> uiExecutor,
                                      Renderer renderer, java.util.function.Function<String, String> htmlRenderer) {
        this.htmlRenderer = htmlRenderer;
        this.backgroundExecutor = Objects.requireNonNull(backgroundExecutor);
        this.uiExecutor = Objects.requireNonNull(uiExecutor);
        this.renderer = Objects.requireNonNull(renderer);
    }

    public synchronized long requestPreview(String template,
                                            FrameworkEnum framework,
                                            Consumer<Result> completion) {
        if (disposed) return latestRequest;
        long request = ++latestRequest;
        backgroundExecutor.execute(() -> {
            synchronized (TemplatePreviewCoordinator.this) {
                if (disposed || request != latestRequest) return;
            }
            Result result;
            try {
                String markdown = renderer.render(template, framework);
                result = new Result(markdown, htmlRenderer == null ? null : htmlRenderer.apply(markdown), null);
            } catch (Exception error) {
                result = Result.failure(error);
            }
            Result completedResult = result;
            uiExecutor.accept(() -> {
                synchronized (TemplatePreviewCoordinator.this) {
                    if (disposed || request != latestRequest) return;
                }
                completion.accept(completedResult);
            });
        });
        return request;
    }

    public synchronized long invalidate() {
        return ++latestRequest;
    }

    public synchronized boolean isCurrent(long request) {
        return !disposed && request == latestRequest;
    }

    public synchronized void dispose() {
        disposed = true;
        latestRequest++;
    }
}
