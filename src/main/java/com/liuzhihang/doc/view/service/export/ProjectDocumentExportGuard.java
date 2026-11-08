package com.liuzhihang.doc.view.service.export;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/** Prevents overlapping project documentation writes in one IDE project. */
@Service(Service.Level.PROJECT)
public final class ProjectDocumentExportGuard implements Disposable {

    private final AtomicBoolean exporting = new AtomicBoolean();
    private ProgressIndicator activeIndicator;
    private boolean disposed;

    public static @NotNull ProjectDocumentExportGuard getInstance(@NotNull Project project) {
        return project.getService(ProjectDocumentExportGuard.class);
    }

    public synchronized boolean tryStart() {
        return !disposed && exporting.compareAndSet(false, true);
    }

    public synchronized boolean tryStart(ProgressIndicator indicator) {
        if (!tryStart()) {
            return false;
        }
        activeIndicator = indicator;
        return true;
    }

    public synchronized void finish() {
        activeIndicator = null;
        exporting.set(false);
    }

    @Override
    public synchronized void dispose() {
        disposed = true;
        if (activeIndicator != null) {
            activeIndicator.cancel();
        }
    }
}
