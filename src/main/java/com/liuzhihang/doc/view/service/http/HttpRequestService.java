package com.liuzhihang.doc.view.service.http;

import com.intellij.ide.extensionResources.ExtensionsRootType;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.scratch.ScratchFileService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditorOpenRequest;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.testFramework.LightVirtualFile;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.export.ProjectDocumentIdentity;
import com.liuzhihang.doc.view.utils.FileEditorOpenSupport;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/** One cancelable request per selected method; only complete, current snapshots reach Scratch. */
@Service(Service.Level.PROJECT)
public final class HttpRequestService implements Disposable {
    private final Project project;
    private final HttpRequestBuilder builder;
    private final BooleanSupplier clientAvailable;
    private final Map<SmartPsiElementPointer<PsiMethod>, Request> active = new ConcurrentHashMap<>();
    private final Map<SmartPsiElementPointer<PsiMethod>, Request> latestOpen = new ConcurrentHashMap<>();
    private volatile boolean disposed;

    public HttpRequestService(Project project) {
        this(project, new HttpRequestBuilder(), HttpRequestService::isHttpClientAvailable);
    }

    HttpRequestService(Project project, HttpRequestBuilder builder, BooleanSupplier clientAvailable) {
        this.project = project;
        this.builder = builder;
        this.clientAvailable = clientAvailable;
    }

    public static HttpRequestService getInstance(Project project) {
        return project.getService(HttpRequestService.class);
    }

    public static boolean isHttpClientAvailable() {
        return PluginManagerCore.isLoaded(PluginId.getId("com.jetbrains.restClient"));
    }

    public void generate(PsiMethod method) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (disposed || project.isDisposed() || method == null || !method.isValid()
                || method.getProject() != project) {
            return;
        }
        if (DumbService.isDumb(project)) {
            notifyReason("http.request.unavailable.indexing");
            return;
        }
        if (!clientAvailable.getAsBoolean()) {
            notifyReason("http.request.unavailable.client");
            return;
        }
        // Preserve method identity across a source-editor commit; never save documents to disk.
        SmartPsiElementPointer<PsiMethod> pointer = SmartPointerManager.createPointer(method);
        var documents = PsiDocumentManager.getInstance(project);
        Document document = documents.getDocument(method.getContainingFile());
        if (document != null) documents.commitDocument(document);
        PsiMethod selected = pointer.getElement();
        if (selected == null || !selected.isValid() || !HttpRequestBuilder.isCandidate(selected)) {
            notifyReason("http.request.error.invalid_source");
            return;
        }
        Request request = new Request(pointer, document, selected.getContainingFile().getVirtualFile());
        if (active.putIfAbsent(pointer, request) != null) {
            return;
        }
        latestOpen.put(pointer, request);
        new Task.Backgroundable(project, DocViewBundle.message("http.request.task"), true) {
            private Draft draft;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                request.indicator = indicator;
                indicator.setIndeterminate(true);
                if (disposed) {
                    indicator.cancel();
                }
                indicator.checkCanceled();
                draft = ReadAction.nonBlocking(() -> {
                    indicator.checkCanceled();
                    PsiMethod current = pointer.getElement();
                    if (current == null || !current.isValid() || stale(request)) {
                        throw new ProcessCanceledException();
                    }
                    ProjectDocumentIdentity identity = identity(current);
                    HttpRequestSnapshot snapshot = builder.build(current);
                    indicator.checkCanceled();
                    // Include resolved DTO/constant changes between this read and publication.
                    request.completedModification = modificationCount();
                    String projectId = digest(project.getLocationHash());
                    String path = "doc-view-http/" + projectId + "/"
                            + identity.pageFileName().replaceFirst("\\.md$", ".http");
                    String source = "# Project: " + singleLine(project.getName())
                            + " [" + projectId.substring(0, 12) + "]\n# Source: "
                            + singleLine(identity.sourcePath()) + " :: "
                            + singleLine(identity.qualifiedClassName()) + "."
                            + singleLine(identity.displaySignature()) + "\n";
                    String text = source + snapshot.text();
                    // LargeFileEditor is read-only; publish only drafts the IDE can fully edit.
                    if (SingleRootFileViewProvider.isTooLargeForContentLoading(new LightVirtualFile(path),
                            (long) text.getBytes(StandardCharsets.UTF_8).length)) {
                        throw new HttpRequestException(HttpRequestException.TOO_LARGE);
                    }
                    indicator.checkCanceled();
                    return new Draft(path, text);
                }).withDocumentsCommitted(project).expireWith(project)
                        .expireWhen(() -> stale(request)).wrapProgress(indicator).executeSynchronously();
            }

            @Override
            public void onSuccess() {
                if (!canPublish(request)) {
                    notifyPublicationFailure(request);
                    return;
                }
                VirtualFile file;
                try {
                    file = publish(request, draft);
                } catch (ProcessCanceledException ignored) {
                    // User cancellation stays quiet; a state change at the write gate gets its reason.
                    notifyPublicationFailure(request);
                    return;
                } catch (IOException | RuntimeException exception) {
                    notifyReason("http.request.error.write_failed");
                    return;
                }
                if (file != null && !project.isDisposed()) {
                    request.openPending = true;
                    try {
                        var openResult = FileEditorManager.getInstance(project).requestOpenFile(file,
                                FileEditorOpenRequest.withFocus(true).withSelectAsCurrent(true));
                        openResult.whenComplete((opened, failure) -> {
                            if (FileEditorOpenSupport.isCancellation(failure)) {
                                latestOpen.remove(pointer, request);
                            }
                        });
                        FileEditorOpenSupport.whenOpened(project, openResult,
                                () -> !disposed && latestOpen.get(pointer) == request,
                                opened -> latestOpen.remove(pointer, request),
                                error -> {
                                    if (!disposed && latestOpen.get(pointer) == request) {
                                        notifyReason("http.request.error.open_failed");
                                    }
                                    latestOpen.remove(pointer, request);
                                });
                    } catch (RuntimeException openFailure) {
                        if (!FileEditorOpenSupport.isCancellation(openFailure)
                                && !disposed && !project.isDisposed() && latestOpen.get(pointer) == request) {
                            notifyReason("http.request.error.open_failed");
                        }
                        latestOpen.remove(pointer, request);
                    }
                }
            }

            @Override
            public void onCancel() {
                notifyPublicationFailure(request);
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                if (error instanceof ProcessCanceledException) {
                    notifyPublicationFailure(request);
                    return;
                }
                if (!disposed && !project.isDisposed()) {
                    notifyReason(error instanceof HttpRequestException failure
                            ? failure.reasonKey() : "http.request.error.build_failed");
                }
            }

            @Override
            public void onFinished() {
                active.remove(pointer, request);
                if (!request.openPending) latestOpen.remove(pointer, request);
            }
        }.queue();
    }

    private VirtualFile publish(Request request, Draft draft) throws IOException {
        if (draft == null) {
            return null;
        }
        return WriteCommandAction.writeCommandAction(project)
                .withName(DocViewBundle.message("http.request.task"))
                .shouldRecordActionForActiveDocument(false).compute(() -> {
                    if (!canPublish(request)) {
                        throw new ProcessCanceledException();
                    }
                    VirtualFile created = null;
                    try {
                        created = ExtensionsRootType.getInstance().findFile(project, draft.path(),
                                ScratchFileService.Option.create_new_always);
                        if (created == null) {
                            throw new IOException("Scratch creation returned no file");
                        }
                        VfsUtil.saveText(created, draft.text());
                        return created;
                    } catch (IOException | RuntimeException error) {
                        if (created != null && created.isValid()) {
                            try {
                                created.delete(this);
                            } catch (IOException cleanupError) {
                                error.addSuppressed(cleanupError);
                            }
                        }
                        throw error;
                    }
                });
    }

    private boolean canPublish(Request request) {
        return !stale(request) && !request.isCanceled() && active.get(request.pointer) == request
                && request.completedModification == modificationCount()
                && request.pointer.getElement() != null && !DumbService.isDumb(project)
                && clientAvailable.getAsBoolean();
    }

    private void notifyPublicationFailure(Request request) {
        if (disposed || project.isDisposed() || request.isCanceled() || active.get(request.pointer) != request) {
            return;
        }
        if (stale(request) || request.completedModification != modificationCount()
                || request.pointer.getElement() == null) {
            notifyReason("http.request.error.stale_source");
        } else if (DumbService.isDumb(project)) {
            notifyReason("http.request.unavailable.indexing");
        } else if (!clientAvailable.getAsBoolean()) {
            notifyReason("http.request.unavailable.client");
        }
    }

    private boolean stale(Request request) {
        return disposed || project.isDisposed() || request.file == null || !request.file.isValid()
                || (request.document == null ? request.file.getModificationStamp()
                : request.document.getModificationStamp()) != request.sourceModification;
    }

    private long modificationCount() {
        return PsiModificationTracker.getInstance(project).getModificationCount();
    }

    private ProjectDocumentIdentity identity(PsiMethod method) throws HttpRequestException {
        var owner = method.getContainingClass();
        var file = method.getContainingFile().getVirtualFile();
        var module = ModuleUtilCore.findModuleForPsiElement(method);
        if (owner == null || owner.getQualifiedName() == null || file == null || module == null) {
            throw new HttpRequestException("http.request.error.invalid_source");
        }
        VirtualFile root = project.getBasePath() == null ? null
                : LocalFileSystem.getInstance().findFileByPath(project.getBasePath());
        String relative = root == null ? null : VfsUtilCore.getRelativePath(file, root, '/');
        // Linked source roots remain distinct without exposing absolute paths in a draft.
        if (relative == null) {
            relative = "linked-" + digest(file.getUrl()) + "/" + file.getName();
        }
        return ProjectDocumentIdentity.of(module.getName(), relative, owner.getQualifiedName(), method.getName(),
                Arrays.stream(method.getParameterList().getParameters())
                        .map(parameter -> parameter.getType().getCanonicalText()).toList());
    }

    private void notifyReason(String key) {
        if (!disposed && !project.isDisposed()) {
            DocViewNotification.notifyError(project, DocViewBundle.message(key));
        }
    }

    private static String singleLine(String value) {
        return value.replace('\r', ' ').replace('\n', ' ');
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    int activeRequestCount() {
        return active.size();
    }

    @Override
    public void dispose() {
        disposed = true;
        active.values().forEach(request -> {
            ProgressIndicator indicator = request.indicator;
            if (indicator != null) {
                indicator.cancel();
            }
        });
        active.clear();
        latestOpen.clear();
    }

    private record Draft(String path, String text) {}

    private static final class Request {
        private final SmartPsiElementPointer<PsiMethod> pointer;
        private final Document document;
        private final VirtualFile file;
        private final long sourceModification;
        private volatile long completedModification = -1;
        private volatile ProgressIndicator indicator;
        private volatile boolean openPending;

        private Request(SmartPsiElementPointer<PsiMethod> pointer, Document document, VirtualFile file) {
            this.pointer = pointer;
            this.document = document;
            this.file = file;
            this.sourceModification = document == null ? (file == null ? -1 : file.getModificationStamp())
                    : document.getModificationStamp();
        }

        private boolean isCanceled() {
            return indicator != null && indicator.isCanceled();
        }
    }
}
