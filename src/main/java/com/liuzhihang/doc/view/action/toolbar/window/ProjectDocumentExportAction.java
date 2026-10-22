package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.export.ProjectDocumentConflictException;
import com.liuzhihang.doc.view.service.export.ProjectDocumentExportResult;
import com.liuzhihang.doc.view.service.export.ProjectDocumentExportGuard;
import com.liuzhihang.doc.view.service.export.ProjectDocumentExporter;
import com.liuzhihang.doc.view.service.export.ProjectDocumentIndexNotReadyException;
import com.liuzhihang.doc.view.service.export.ProjectDocumentPublishException;
import com.liuzhihang.doc.view.service.export.ProjectDocumentSnapshot;
import com.liuzhihang.doc.view.service.export.ProjectDocumentSnapshotService;
import com.liuzhihang.doc.view.service.export.ProjectDocumentTemporaryCleanupException;
import com.liuzhihang.doc.view.utils.DocViewBackgroundTasks;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.stream.Collectors;

public final class ProjectDocumentExportAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        event.getPresentation().setEnabledAndVisible(project != null && !project.isDisposed()
                && !DumbService.getInstance(project).isDumb());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        if (project == null || project.isDisposed()) {
            return;
        }
        if (DumbService.getInstance(project).isDumb()) {
            DocViewNotification.notifyWarn(project, DocViewBundle.message("project.export.index.not.ready"));
            return;
        }

        DocViewBackgroundTasks.runReadTask(project, DocViewBundle.message("task.project.export.scan"), true,
                indicator -> ProjectDocumentSnapshotService.getInstance(project).createSnapshot(indicator),
                snapshot -> exportSnapshot(project, snapshot),
                throwable -> reportFailure(project, throwable));
    }

    private static void exportSnapshot(Project project, ProjectDocumentSnapshot snapshot) {
        String basePath = project.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            DocViewNotification.notifyError(project, DocViewBundle.message("project.export.failed"));
            return;
        }
        ProjectDocumentExportGuard guard = ProjectDocumentExportGuard.getInstance(project);
        DocViewBackgroundTasks.runTask(project, DocViewBundle.message("task.project.export.write"), true, indicator -> {
            if (project.isDisposed()) {
                throw new ProcessCanceledException();
            }
            if (!guard.tryStart()) {
                DocViewBackgroundTasks.invokeLater(project,
                        () -> DocViewNotification.notifyWarn(project,
                                DocViewBundle.message("project.export.already.running")));
                return;
            }
            try {
                ProjectDocumentExportResult result;
                try {
                    result = new ProjectDocumentExporter()
                            .export(Path.of(basePath), snapshot.pages(), indicator, () -> !project.isDisposed());
                } catch (ProjectDocumentPublishException publish) {
                    if (!publish.publishedPaths().isEmpty()) {
                        refreshProjectDocuments(Path.of(basePath));
                    }
                    throw publish;
                }
                if (!result.changedPaths().isEmpty()) {
                    refreshProjectDocuments(Path.of(basePath));
                }
                String changed = result.changedPaths().isEmpty()
                        ? DocViewBundle.message("project.export.no.changes")
                        : DocViewBundle.message("project.export.changed", result.changedPaths().size());
                String message = DocViewBundle.message("project.export.success", result.exportedPageCount(),
                        snapshot.skippedCount(), changed);
                if (snapshot.pages().isEmpty()) {
                    message = DocViewBundle.message("project.export.empty", snapshot.skippedCount(), changed);
                }
                String finalMessage = message;
                DocViewBackgroundTasks.invokeLater(project,
                        () -> DocViewNotification.notifyInfo(project, finalMessage));
            } finally {
                guard.finish();
            }
        }, throwable -> reportFailure(project, throwable));
    }

    private static void refreshProjectDocuments(Path projectRoot) {
        Path outputRootPath = projectRoot.resolve("docs/doc-view");
        VirtualFile outputRoot = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(outputRootPath);
        if (outputRoot != null) {
            outputRoot.refresh(false, true, null);
        }
    }

    private static void reportFailure(Project project, Throwable throwable) {
        if (throwable instanceof ProjectDocumentIndexNotReadyException) {
            DocViewNotification.notifyWarn(project, DocViewBundle.message("project.export.index.not.ready"));
        } else if (throwable instanceof ProjectDocumentConflictException conflict) {
            String conflicts = conflict.conflicts().stream().collect(Collectors.joining(", "));
            DocViewNotification.notifyWarn(project,
                    DocViewBundle.message("project.export.conflict", conflicts));
        } else if (throwable instanceof ProjectDocumentPublishException publish) {
            String published = publish.publishedPaths().isEmpty()
                    ? DocViewBundle.message("project.export.none")
                    : String.join(", ", publish.publishedPaths());
            String failed = publish.failedPath() == null
                    ? DocViewBundle.message("project.export.unknown")
                    : publish.failedPath();
            String leftovers = publish.temporaryPaths().isEmpty()
                    ? DocViewBundle.message("project.export.none")
                    : String.join(", ", publish.temporaryPaths());
            DocViewNotification.notifyError(project,
                    DocViewBundle.message("project.export.partial.failed", published, failed, leftovers));
        } else if (throwable instanceof ProjectDocumentTemporaryCleanupException cleanup) {
            DocViewNotification.notifyError(project,
                    DocViewBundle.message("project.export.cleanup.failed", String.join(", ", cleanup.temporaryPaths())));
        } else {
            DocViewNotification.notifyError(project, DocViewBundle.message("project.export.failed"));
        }
    }
}
