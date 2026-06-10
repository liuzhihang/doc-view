package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Doc View 后台任务入口，统一处理取消、read action 和 EDT 回投。
 */
public final class DocViewBackgroundTasks {

    private DocViewBackgroundTasks() {
    }

    public static <T> void runReadTask(@NotNull Project project,
                                       @NotNull String title,
                                       boolean cancellable,
                                       @NotNull ReadTask<T> readTask,
                                       @NotNull Consumer<T> onSuccess,
                                       @Nullable Consumer<Throwable> onError) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, title, cancellable) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    T result = ApplicationManager.getApplication().runReadAction((com.intellij.openapi.util.Computable<T>) () -> {
                        indicator.checkCanceled();
                        return readTask.compute(indicator);
                    });
                    if (!indicator.isCanceled()) {
                        invokeLater(project, () -> onSuccess.accept(result));
                    }
                } catch (ProcessCanceledException ignored) {
                    // 用户取消或项目关闭时静默丢弃 stale result。
                } catch (Throwable throwable) {
                    if (onError != null) {
                        invokeLater(project, () -> onError.accept(throwable));
                    }
                }
            }
        });
    }

    public static void runTask(@NotNull Project project,
                               @NotNull String title,
                               boolean cancellable,
                               @NotNull BackgroundTask backgroundTask,
                               @Nullable Consumer<Throwable> onError) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, title, cancellable) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    indicator.checkCanceled();
                    backgroundTask.run(indicator);
                } catch (ProcessCanceledException ignored) {
                    // 用户取消或项目关闭时静默丢弃 stale result。
                } catch (Throwable throwable) {
                    if (onError != null) {
                        invokeLater(project, () -> onError.accept(throwable));
                    }
                }
            }
        });
    }

    public static void invokeLater(@NotNull Project project, @NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) {
                runnable.run();
            }
        });
    }

    public interface ReadTask<T> {
        T compute(@NotNull ProgressIndicator indicator);
    }

    public interface BackgroundTask {
        void run(@NotNull ProgressIndicator indicator) throws Exception;
    }
}
