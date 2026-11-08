package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.liuzhihang.doc.view.service.export.ProjectDocumentGenerationService;
import com.liuzhihang.doc.view.service.export.ProjectDocumentGenerationException;
import com.liuzhihang.doc.view.service.document.ProjectDocumentQueryException;
import com.liuzhihang.doc.view.ui.window.DocViewWindowPanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.export.ProjectDocumentConflictException;
import com.liuzhihang.doc.view.service.export.ProjectDocumentExportResult;
import com.liuzhihang.doc.view.service.export.ProjectDocumentIndexNotReadyException;
import com.liuzhihang.doc.view.service.export.ProjectDocumentPublishException;
import com.liuzhihang.doc.view.service.export.ProjectDocumentSnapshot;
import com.liuzhihang.doc.view.service.export.ProjectDocumentTemporaryCleanupException;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;


public final class ProjectDocumentExportAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        event.getPresentation().setVisible(project != null && !project.isDisposed());
        event.getPresentation().setEnabled(project != null && !project.isDisposed()
                && !DumbService.getInstance(project).isDumb());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        if (project != null) {
            generateProject(project);
        }
    }

    public static void generateProject(Project project) {
        if (project.isDisposed()) {
            return;
        }
        if (DumbService.getInstance(project).isDumb()) {
            reportFailure(project, new ProjectDocumentIndexNotReadyException());
            return;
        }
        showStatus(project, DocViewBundle.message("task.project.export.generate"));
        ProgressManager.getInstance().run(new Task.Backgroundable(project,
                DocViewBundle.message("task.project.export.generate"), true) {
            private ProjectDocumentGenerationService.Outcome outcome;
            private Throwable failure;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    outcome = ProjectDocumentGenerationService.getInstance(project).generate(indicator);
                } catch (Exception exception) {
                    failure = exception;
                } finally {
                    boolean wrote = outcome != null && !outcome.publication().changedPaths().isEmpty()
                            || failure instanceof ProjectDocumentPublishException partial
                            && !partial.publishedPaths().isEmpty();
                    if (wrote && !project.isDisposed() && project.getBasePath() != null) {
                        try {
                            refreshProjectDocuments(Path.of(project.getBasePath()));
                        } catch (ProcessCanceledException ignored) {
                            // VFS refresh observes the cancelled task too. Preserve its disk outcome.
                        }
                    }
                }
            }

            @Override
            public void onSuccess() {
                reportCompletion();
            }

            @Override
            public void onCancel() {
                // Cancellation after a page move may be partial; cancellation after commit is success.
                reportCompletion();
            }

            @Override
            public void onThrowable(@NotNull Throwable throwable) {
                if (!project.isDisposed()) {
                    if (outcome != null || failure != null) {
                        reportCompletion();
                    } else {
                        reportFailure(project, throwable);
                    }
                }
            }

            private void reportCompletion() {
                if (project.isDisposed()) {
                    return;
                }
                if (outcome != null) {
                    ProjectDocumentExportResult result = outcome.publication();
                    ProjectDocumentSnapshot snapshot = outcome.snapshot();
                    String changed = result.changedPaths().isEmpty()
                            ? DocViewBundle.message("project.export.no.changes")
                            : DocViewBundle.message("project.export.changed", result.changedPaths().size());
                    String message = snapshot.pages().isEmpty()
                            ? DocViewBundle.message("project.export.empty", snapshot.skippedCount(), changed)
                            : DocViewBundle.message("project.export.success", result.exportedPageCount(),
                                    snapshot.skippedCount(), changed);
                    showStatus(project, message);
                    DocViewNotification.notifyInfo(project, message);
                } else if (failure == null || failure instanceof ProcessCanceledException) {
                    showStatus(project, DocViewBundle.message("project.export.cancelled"));
                } else {
                    reportFailure(project, failure);
                }
            }
        });
    }

    private static void showStatus(Project project, String message) {
        DocViewWindowPanel.setProjectDocumentStatus(project, message);
    }

    private static void refreshProjectDocuments(Path projectRoot) {
        Path outputRootPath = projectRoot.resolve("docs/doc-view");
        VirtualFile outputRoot = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(outputRootPath);
        if (outputRoot != null) {
            outputRoot.refresh(false, true, null);
        }
    }

    private static void reportFailure(Project project, Throwable throwable) {
        String message;
        boolean warning = false;
        if (throwable instanceof ProjectDocumentIndexNotReadyException) {
            message = DocViewBundle.message("project.export.index.not.ready");
            warning = true;
        } else if (throwable instanceof ProjectDocumentConflictException conflict) {
            message = DocViewBundle.message("project.export.conflict", String.join(", ", conflict.conflicts()));
            warning = true;
        } else if (throwable instanceof ProjectDocumentPublishException publish) {
            String published = publish.publishedPaths().isEmpty()
                    ? DocViewBundle.message("project.export.none") : String.join(", ", publish.publishedPaths());
            String failed = publish.failedPath() == null
                    ? DocViewBundle.message("project.export.unknown") : publish.failedPath();
            String leftovers = publish.temporaryPaths().isEmpty()
                    ? DocViewBundle.message("project.export.none") : String.join(", ", publish.temporaryPaths());
            message = DocViewBundle.message("project.export.partial.failed", published, failed, leftovers);
        } else if (throwable instanceof ProjectDocumentTemporaryCleanupException cleanup) {
            message = DocViewBundle.message("project.export.cleanup.failed", String.join(", ", cleanup.temporaryPaths()));
        } else if (throwable instanceof ProjectDocumentGenerationException generation) {
            message = DocViewBundle.message(generation.code() == ProjectDocumentGenerationException.Code.SOURCE_CHANGED
                    ? "project.export.source.changed" : "project.export.timeout");
            warning = true;
        } else if (throwable instanceof ProjectDocumentQueryException query
                && query.code() == ProjectDocumentQueryException.Code.BUSY) {
            message = DocViewBundle.message("project.export.already.running");
            warning = true;
        } else {
            com.intellij.openapi.diagnostic.Logger.getInstance(ProjectDocumentExportAction.class)
                    .warn("Project documentation generation failed", throwable);
            message = DocViewBundle.message("project.export.failed");
        }
        showStatus(project, message);
        if (warning) {
            DocViewNotification.notifyWarn(project, message);
        } else {
            DocViewNotification.notifyError(project, message);
        }
    }
}
