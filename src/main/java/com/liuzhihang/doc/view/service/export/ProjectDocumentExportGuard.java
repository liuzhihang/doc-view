package com.liuzhihang.doc.view.service.export;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/** Prevents overlapping project documentation writes in one IDE project. */
@Service(Service.Level.PROJECT)
public final class ProjectDocumentExportGuard {

    private final AtomicBoolean exporting = new AtomicBoolean();

    public static @NotNull ProjectDocumentExportGuard getInstance(@NotNull Project project) {
        return project.getService(ProjectDocumentExportGuard.class);
    }

    public boolean tryStart() {
        return exporting.compareAndSet(false, true);
    }

    public void finish() {
        exporting.set(false);
    }
}
