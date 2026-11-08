package com.liuzhihang.doc.view.service.export;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.liuzhihang.doc.view.service.document.ProjectDocumentIndexService;
import com.liuzhihang.doc.view.service.document.ProjectDocumentQueryException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** One cancellable parse/publish lifecycle shared by explicit UI and MCP generation. */
@Service(Service.Level.PROJECT)
public final class ProjectDocumentGenerationService {
    private final Project project;

    public ProjectDocumentGenerationService(Project project) {
        this.project = project;
    }

    public static ProjectDocumentGenerationService getInstance(Project project) {
        return project.getService(ProjectDocumentGenerationService.class);
    }

    public record Outcome(ProjectDocumentSnapshot snapshot, ProjectDocumentExportResult publication) {}

    public Outcome generate(ProgressIndicator indicator) throws IOException {
        var application = ApplicationManager.getApplication();
        if (application.isDispatchThread() || application.isReadAccessAllowed()) {
            throw new IllegalStateException("Project document generation requires a background thread without read access");
        }
        if (project.isDisposed()) {
            throw new ProcessCanceledException();
        }
        ProjectDocumentExportGuard guard = ProjectDocumentExportGuard.getInstance(project);
        if (!guard.tryStart(indicator)) {
            throw new ProjectDocumentQueryException(ProjectDocumentQueryException.Code.BUSY,
                    "Project documentation is already being generated");
        }
        AtomicBoolean timedOut = new AtomicBoolean();
        AtomicReference<Outcome> completed = new AtomicReference<>();
        var deadline = AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
            timedOut.set(true);
            indicator.cancel();
        }, 120, TimeUnit.SECONDS);
        try {
            com.intellij.openapi.util.Computable<Outcome> operation = () -> {
                indicator.checkCanceled();
                // Commit editor text to PSI, but never save documents as a side effect of generation.
                ApplicationManager.getApplication().invokeAndWait(() -> {
                    indicator.checkCanceled();
                    if (project.isDisposed()) {
                        throw new ProcessCanceledException();
                    }
                    PsiDocumentManager.getInstance(project).commitAllDocuments();
                });
                ProjectDocumentIndexService index = ProjectDocumentIndexService.getInstance(project);
                long epoch = index.captureInputEpoch();
                ProjectDocumentSnapshot snapshot = ReadAction.nonBlocking(() -> {
                    ProgressIndicator readIndicator = ProgressManager.getInstance().getProgressIndicator();
                    readIndicator.checkCanceled();
                    if (!index.isInputEpochCurrent(epoch)) {
                        throw new ProjectDocumentGenerationException(
                                ProjectDocumentGenerationException.Code.SOURCE_CHANGED);
                    }
                    if (DumbService.getInstance(project).isDumb()) {
                        throw new ProjectDocumentIndexNotReadyException();
                    }
                    return ProjectDocumentSnapshotService.getInstance(project).createSnapshot(readIndicator);
                }).withDocumentsCommitted(project).expireWith(project).wrapProgress(indicator).executeSynchronously();
                String basePath = project.getBasePath();
                if (basePath == null) {
                    throw new ProjectDocumentQueryException(ProjectDocumentQueryException.Code.PROJECT_NOT_OPEN,
                            "Project has no local directory");
                }
                try {
                    var publication = new ProjectDocumentExporter().export(Path.of(basePath), snapshot, indicator,
                            () -> !project.isDisposed(), () -> {
                                if (!index.isInputEpochCurrent(epoch)) {
                                    throw new ProjectDocumentGenerationException(
                                            ProjectDocumentGenerationException.Code.SOURCE_CHANGED);
                                }
                            }, committed -> index.installCommittedIndex(committed, epoch));
                    Outcome outcome = new Outcome(snapshot, publication);
                    completed.set(outcome);
                    return outcome;
                } catch (IOException failure) {
                    throw new UncheckedIOException(failure);
                }
            };
            ProgressManager progress = ProgressManager.getInstance();
            // Backgroundable already owns this indicator. Restarting it is a platform lifecycle error.
            return progress.getProgressIndicator() == indicator
                    ? operation.compute() : progress.runProcess(operation, indicator);
        } catch (UncheckedIOException failure) {
            throw failure.getCause();
        } catch (ProcessCanceledException cancelled) {
            if (completed.get() != null) {
                return completed.get();
            }
            if (timedOut.get()) {
                throw new ProjectDocumentGenerationException(ProjectDocumentGenerationException.Code.TIMEOUT);
            }
            throw cancelled;
        } finally {
            deadline.cancel(false);
            guard.finish();
        }
    }
}
