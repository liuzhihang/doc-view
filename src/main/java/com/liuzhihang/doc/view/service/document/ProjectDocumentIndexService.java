package com.liuzhihang.doc.view.service.document;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.PsiTreeChangeListener;
import com.intellij.util.messages.MessageBusConnection;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.ApiData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.EntryData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.FileData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.IdentityData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.IndexData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.SourceData;
import com.liuzhihang.doc.view.service.export.ProjectDocumentIdentity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import static com.liuzhihang.doc.view.service.document.ProjectDocumentIndexStatus.Freshness;
import static com.liuzhihang.doc.view.service.document.ProjectDocumentIndexStatus.IndexState;
import static com.liuzhihang.doc.view.service.document.ProjectDocumentQueryException.Code;

/** Reads only the committed project index and its explicitly requested files. */
@Service(Service.Level.PROJECT)
public final class ProjectDocumentIndexService implements Disposable {

    public static final String DOCUMENT_ROOT = "docs/doc-view";
    private static final String INDEX_FILE = "index.json";
    private static final int MAX_INDEX_BYTES = 32 * 1024 * 1024;
    private static final int MAX_PAGE_BYTES = 1024 * 1024;
    private static final Duration QUERY_BUDGET = Duration.ofSeconds(5);
    private static final Set<String> SEARCH_FRAMEWORKS = Set.of("spring", "feign", "dubbo", "interface");

    private final Project project;
    private final AtomicLong inputEpoch = new AtomicLong();
    private final Semaphore readSlots = new Semaphore(4, true);
    private final Object observationLock = new Object();
    private final Object cacheLock = new Object();
    private final AtomicLong cacheGeneration = new AtomicLong();
    private volatile @Nullable String observedSettingsFingerprint;
    private volatile long committedInputEpoch = -1;
    private volatile @Nullable String committedSnapshotId;
    private volatile @Nullable IndexData committedIndexData;
    private volatile @Nullable String committedIndexByteHash;
    private volatile @Nullable CachedIndex loadedIndex;
    private volatile @Nullable String inconsistentSnapshotId;
    private volatile Map<String, Long> inconsistentResources = Map.of();
    private long inconsistencyTokenCounter;
    private final MessageBusConnection messageBusConnection;

    public ProjectDocumentIndexService(@NotNull Project project) {
        this.project = project;
        observedSettingsFingerprint = settingsFingerprint();

        PsiTreeChangeListener psiListener = new PsiTreeChangeAdapter() {
            @Override
            public void childAdded(@NotNull PsiTreeChangeEvent event) {
                markChangedSourceFile(event.getFile());
            }

            @Override
            public void childRemoved(@NotNull PsiTreeChangeEvent event) {
                markChangedSourceFile(event.getFile());
            }

            @Override
            public void childReplaced(@NotNull PsiTreeChangeEvent event) {
                markChangedSourceFile(event.getFile());
            }

            @Override
            public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
                markChangedSourceFile(event.getFile());
            }

            @Override
            public void childMoved(@NotNull PsiTreeChangeEvent event) {
                markChangedSourceFile(event.getFile());
            }

            @Override
            public void propertyChanged(@NotNull PsiTreeChangeEvent event) {
                markChangedSourceFile(event.getFile());
            }
        };
        PsiManager.getInstance(project).addPsiTreeChangeListener(psiListener, this);

        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                Document document = event.getDocument();
                VirtualFile file = FileDocumentManager.getInstance().getFile(document);
                markChangedSourceFile(file);
            }
        };
        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(documentListener, this);

        messageBusConnection = project.getMessageBus().connect(this);
        messageBusConnection.subscribe(ModuleRootListener.TOPIC, new ModuleRootListener() {
            @Override
            public void rootsChanged(@NotNull ModuleRootEvent event) {
                markObservedInputsChanged();
            }
        });
    }

    public static @NotNull ProjectDocumentIndexService getInstance(@NotNull Project project) {
        return project.getService(ProjectDocumentIndexService.class);
    }

    /** Captures the observed input generation before a writer begins parsing. */
    public long captureInputEpoch() {
        observeSettingsChanges();
        return inputEpoch.get();
    }

    /** Checks the writer's captured generation without scanning PSI or files. */
    public boolean isInputEpochCurrent(long capturedEpoch) {
        if (project.isDisposed()) {
            return false;
        }
        observeSettingsChanges();
        return capturedEpoch == inputEpoch.get();
    }

    /** Called by the writer for input changes it observes between its own lifecycle checkpoints. */
    public void markObservedInputsChanged() {
        if (project.isDisposed()) {
            return;
        }
        inputEpoch.incrementAndGet();
        publishStateChanged();
    }

    /** Installs the exact immutable index used by a completed final index.json move. */
    public void installCommittedIndex(@NotNull IndexData index, long capturedEpoch) {
        if (project.isDisposed()) {
            return;
        }
        observeSettingsChanges();
        long generation = cacheGeneration.get();
        String indexByteHash;
        try {
            indexByteHash = ProjectDocumentFormat.sha256(ProjectDocumentFormat.writeIndex(index));
        } catch (RuntimeException invalidCommittedIndex) {
            // The disk move already committed. Avoid turning a successful publication into a false failure;
            // the next read will validate the authoritative bytes from disk.
            indexByteHash = null;
        }
        synchronized (cacheLock) {
            committedSnapshotId = index.snapshotId();
            committedInputEpoch = capturedEpoch;
            boolean cacheGenerationMatches = cacheGeneration.get() == generation;
            committedIndexData = cacheGenerationMatches ? index : null;
            committedIndexByteHash = cacheGenerationMatches ? indexByteHash : null;
            if (cacheGenerationMatches) {
                loadedIndex = indexByteHash == null ? null : new CachedIndex(index, indexByteHash, null);
            }
            inconsistentSnapshotId = null;
            inconsistentResources = Map.of();
        }
        publishStateChanged();
    }

    /** Clears parsed/search projections while preserving the current-session generation evidence. */
    public void clearDerivedCache() {
        synchronized (cacheLock) {
            cacheGeneration.incrementAndGet();
            loadedIndex = null;
            committedIndexData = null;
            committedIndexByteHash = null;
            inconsistentSnapshotId = null;
            inconsistentResources = Map.of();
        }
        publishStateChanged();
    }

    public @NotNull ProjectDocumentIndexStatus status(@NotNull ProgressIndicator indicator) {
        observeSettingsChanges();
        try {
            return withRead(indicator, context -> {
                IndexData index = readIndex(context);
                IndexState state = hasKnownInconsistency(index.snapshotId())
                        ? IndexState.INCONSISTENT : IndexState.READY;
                return statusOf(state, index, null);
            });
        } catch (ProjectDocumentQueryException exception) {
            return switch (exception.code()) {
                case INDEX_NOT_FOUND -> new ProjectDocumentIndexStatus(IndexState.NOT_GENERATED,
                        Freshness.UNKNOWN, null, null, null, null, exception.code().name());
                case UNSUPPORTED_SCHEMA -> new ProjectDocumentIndexStatus(IndexState.UNSUPPORTED_SCHEMA,
                        Freshness.UNKNOWN, null, null, null, null, exception.code().name());
                case INDEX_INVALID, LIMIT_EXCEEDED -> new ProjectDocumentIndexStatus(IndexState.INVALID,
                        Freshness.UNKNOWN, null, null, null, null, exception.code().name());
                case INDEX_INCONSISTENT -> new ProjectDocumentIndexStatus(IndexState.INCONSISTENT,
                        Freshness.UNKNOWN, null, null, null, null, exception.code().name());
                default -> throw exception;
            };
        }
    }

    public @NotNull ProjectDocumentQueryResult search(@NotNull ProjectDocumentQuery query,
                                                       @NotNull ProgressIndicator indicator) {
        Objects.requireNonNull(query, "query");
        observeSettingsChanges();
        return withRead(indicator, context -> {
            SearchRequest request = normalizeQuery(query);
            IndexData index = readIndex(context);
            Cursor cursor = decodeCursor(request.cursor(), request, index.snapshotId());
            List<EntryData> matches = new ArrayList<>();
            if (request.terms().isEmpty()) {
                for (EntryData entry : index.entries()) {
                    context.checkActive();
                    if (matchesFilters(entry, request)) {
                        matches.add(entry);
                    }
                }
            } else {
                for (SearchProjection candidate : searchProjection(index, context)) {
                    context.checkActive();
                    if (matches(candidate, request)) {
                        matches.add(candidate.entry());
                    }
                }
            }
            int start = 0;
            if (cursor != null) {
                boolean found = false;
                for (int i = 0; i < matches.size(); i++) {
                    if (matches.get(i).identity().canonicalForm().equals(cursor.lastCanonicalForm())) {
                        start = i + 1;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw error(Code.CURSOR_STALE, "The search cursor no longer matches this index snapshot");
                }
            }
            int end = Math.min(matches.size(), start + request.limit());
            List<EntryData> page = List.copyOf(matches.subList(start, end));
            String nextCursor = end < matches.size()
                    ? encodeCursor(request, index.snapshotId(), page.get(page.size() - 1).identity().canonicalForm())
                    : null;
            context.checkActive();
            return new ProjectDocumentQueryResult(page, matches.size(), nextCursor, index.snapshotId(),
                    freshnessFor(index.snapshotId()), index.hasUnsavedDocuments(), index.generatedAt());
        });
    }

    public @NotNull ProjectDocumentDetailResult getDetail(@NotNull String apiId,
                                                           @NotNull String expectedSnapshotId,
                                                           @NotNull ProgressIndicator indicator) {
        observeSettingsChanges();
        return withRead(indicator, context -> {
            IndexData index = readIndex(context);
            requireSnapshot(index, expectedSnapshotId);
            EntryData entry = findEntry(index, apiId);
            String resourcePath = entry.detail().path();
            long inconsistencyToken = inconsistencyToken(index.snapshotId(), resourcePath);
            byte[] bytes;
            try {
                bytes = readEntryFile(entry, entry.detail(), ".json", MAX_PAGE_BYTES, context);
            } catch (ProjectDocumentQueryException exception) {
                if (exception.code() == Code.INDEX_INCONSISTENT) {
                    markInconsistent(index.snapshotId(), resourcePath);
                }
                throw exception;
            } catch (IOException unavailable) {
                markInconsistent(index.snapshotId(), resourcePath);
                throw new ProjectDocumentQueryException(Code.INDEX_INCONSISTENT,
                        "The indexed API detail is missing or unreadable", entry.detail().path(), unavailable);
            }
            if (!ProjectDocumentFormat.sha256(bytes).equals(entry.detail().sha256())) {
                markInconsistent(index.snapshotId(), resourcePath);
                throw error(Code.INDEX_INCONSISTENT, "The indexed API detail no longer matches its recorded hash",
                        entry.detail().path());
            }
            ApiData detail;
            try {
                ProjectDocumentIdentity expectedIdentity = projectIdentity(entry.identity(), entry.source());
                ProjectDocumentFormat.validateOwnedJson(bytes, "api", expectedIdentity);
                detail = ProjectDocumentFormat.readApi(bytes);
                if (!summaryMatchesDetail(entry, detail)) {
                    throw error(Code.INDEX_INCONSISTENT, "The API detail identity does not match the index",
                            entry.detail().path());
                }
            } catch (ProjectDocumentQueryException exception) {
                if (exception.code() == Code.INDEX_INCONSISTENT) {
                    markInconsistent(index.snapshotId(), resourcePath);
                }
                throw exception;
            } catch (ProjectDocumentFormat.ProjectDocumentFormatException exception) {
                ProjectDocumentQueryException failure = formatFailure(exception, resourcePath, true);
                if (failure.code() == Code.INDEX_INCONSISTENT) {
                    markInconsistent(index.snapshotId(), resourcePath);
                }
                throw failure;
            } catch (RuntimeException invalidDetail) {
                markInconsistent(index.snapshotId(), resourcePath);
                throw new ProjectDocumentQueryException(Code.INDEX_INCONSISTENT,
                        "The indexed API detail is invalid", entry.detail().path(), invalidDetail);
            }
            context.checkActive();
            clearInconsistent(index.snapshotId(), resourcePath, inconsistencyToken);
            return new ProjectDocumentDetailResult(entry, detail, index.snapshotId(),
                    freshnessFor(index.snapshotId()));
        });
    }

    public @NotNull ProjectDocumentMarkdownResult getMarkdown(@NotNull String apiId,
                                                              @NotNull String expectedSnapshotId,
                                                              @NotNull ProgressIndicator indicator) {
        observeSettingsChanges();
        return withRead(indicator, context -> {
            IndexData index = readIndex(context);
            requireSnapshot(index, expectedSnapshotId);
            EntryData entry = findEntry(index, apiId);
            String resourcePath = entry.markdown().path();
            long inconsistencyToken = inconsistencyToken(index.snapshotId(), resourcePath);
            byte[] bytes;
            try {
                bytes = readEntryFile(entry, entry.markdown(), ".md", MAX_PAGE_BYTES, context);
            } catch (ProjectDocumentQueryException exception) {
                if (exception.code() == Code.INDEX_INCONSISTENT) {
                    markInconsistent(index.snapshotId(), resourcePath);
                }
                throw exception;
            } catch (IOException unavailable) {
                markInconsistent(index.snapshotId(), resourcePath);
                throw new ProjectDocumentQueryException(Code.INDEX_INCONSISTENT,
                        "The indexed Markdown is missing or unreadable", entry.markdown().path(), unavailable);
            }
            if (!ProjectDocumentFormat.sha256(bytes).equals(entry.markdown().sha256())) {
                markInconsistent(index.snapshotId(), resourcePath);
                throw error(Code.INDEX_INCONSISTENT, "The indexed Markdown no longer matches its recorded hash",
                        entry.markdown().path());
            }
            String markdown = decodeUtf8(bytes, index.snapshotId(), resourcePath);
            context.checkActive();
            clearInconsistent(index.snapshotId(), resourcePath, inconsistencyToken);
            return new ProjectDocumentMarkdownResult(entry, markdown, entry.markdown().path(), index.snapshotId(),
                    freshnessFor(index.snapshotId()));
        });
    }

    public @NotNull ProjectDocumentIndexMarkdownResult getIndexMarkdown(
            @NotNull String expectedSnapshotId,
            @NotNull ProgressIndicator indicator) {
        observeSettingsChanges();
        return withRead(indicator, context -> {
            IndexData index = readIndex(context);
            requireSnapshot(index, expectedSnapshotId);
            FileData pointer = index.markdownIndex();
            String resourcePath = "index.md";
            long inconsistencyToken = inconsistencyToken(index.snapshotId(), resourcePath);
            if (!"index.md".equals(pointer.path())) {
                markInconsistent(index.snapshotId(), resourcePath);
                throw error(Code.INDEX_INCONSISTENT, "The index Markdown path is invalid", pointer.path());
            }
            byte[] bytes;
            try {
                bytes = readBounded(context.documentRoot.resolve(pointer.path()), MAX_INDEX_BYTES,
                        context.documentRoot, context);
            } catch (ProjectDocumentQueryException exception) {
                if (exception.code() == Code.INDEX_INVALID || exception.code() == Code.INDEX_INCONSISTENT
                        || exception.code() == Code.IO_ERROR) {
                    markInconsistent(index.snapshotId(), resourcePath);
                }
                throw exception;
            } catch (IOException unavailable) {
                markInconsistent(index.snapshotId(), resourcePath);
                throw new ProjectDocumentQueryException(Code.INDEX_INCONSISTENT,
                        "The indexed Markdown directory page is missing or unreadable", pointer.path(), unavailable);
            }
            if (!ProjectDocumentFormat.sha256(bytes).equals(pointer.sha256())) {
                markInconsistent(index.snapshotId(), resourcePath);
                throw error(Code.INDEX_INCONSISTENT, "The index Markdown no longer matches its recorded hash",
                        pointer.path());
            }
            String markdown = decodeUtf8(bytes, index.snapshotId(), resourcePath);
            context.checkActive();
            clearInconsistent(index.snapshotId(), resourcePath, inconsistencyToken);
            return new ProjectDocumentIndexMarkdownResult(markdown, pointer.path(), index.snapshotId(),
                    freshnessFor(index.snapshotId()));
        });
    }

    private <T> T withRead(ProgressIndicator indicator, ReadOperation<T> operation) {
        if (project.isDisposed()) {
            throw error(Code.PROJECT_NOT_OPEN, "The project is closed");
        }
        if (ApplicationManager.getApplication().isDispatchThread()) {
            throw error(Code.BUSY, "Project document reads must run in a background task");
        }
        ReadContext context = new ReadContext(project, indicator, deadlineAfter(QUERY_BUDGET), cacheGeneration.get());
        boolean slotAcquired = false;
        DocumentStoreLocks.Lease lock = null;
        try {
            slotAcquired = acquireReadSlot(context);
            if (!slotAcquired) {
                throw error(Code.BUSY, "The project already has the maximum number of document reads");
            }
            Path projectRoot = projectRoot();
            Duration remaining = Duration.ofNanos(Math.max(0, context.remainingNanos()));
            try {
                lock = DocumentStoreLocks.tryAcquireRead(projectRoot, indicator, remaining);
            } catch (IOException invalidRoot) {
                throw new ProjectDocumentQueryException(Code.INDEX_INVALID,
                        "The project document root contains an unsafe path", null, invalidRoot);
            }
            if (lock == null) {
                context.checkActive();
                throw error(Code.BUSY, "The project document store is being published");
            }
            context.documentRoot = lock.documentRoot();
            context.projectRoot = projectRoot;
            context.checkActive();
            return operation.run(context);
        } catch (ProjectDocumentFormat.CancellationCheckException canceled) {
            Throwable cause = canceled.getCause();
            if (cause instanceof ProjectDocumentQueryException queryFailure) {
                throw queryFailure;
            }
            throw new ProjectDocumentQueryException(Code.CANCELLED,
                    "The project document query was cancelled", null, cause);
        } catch (ProcessCanceledException canceled) {
            throw new ProjectDocumentQueryException(Code.CANCELLED, "The project document query was cancelled", null,
                    canceled);
        } catch (ProjectDocumentQueryException exception) {
            throw exception;
        } catch (IOException ioException) {
            throw new ProjectDocumentQueryException(Code.IO_ERROR, "The project document store could not be read",
                    null, ioException);
        } finally {
            if (lock != null) {
                lock.close();
            }
            if (slotAcquired) {
                readSlots.release();
            }
        }
    }

    private boolean acquireReadSlot(ReadContext context) {
        context.checkActive();
        return readSlots.tryAcquire();
    }

    private IndexData readIndex(ReadContext context) throws IOException {
        context.checkActive();
        Path projectRoot = context.projectRoot;
        Path documentRoot = context.documentRoot;
        try {
            verifyStoreRoot(projectRoot, documentRoot);
        } catch (IOException invalidRoot) {
            throw new ProjectDocumentQueryException(Code.INDEX_INVALID,
                    "The project document root contains an unsafe path", null, invalidRoot);
        }
        Path indexPath = documentRoot.resolve(INDEX_FILE);
        if (!Files.exists(indexPath, LinkOption.NOFOLLOW_LINKS)) {
            throw error(Code.INDEX_NOT_FOUND, "No project document index has been generated");
        }
        byte[] bytes;
        try {
            bytes = readBounded(indexPath, MAX_INDEX_BYTES, documentRoot, context);
        } catch (ProjectDocumentQueryException exception) {
            throw exception;
        } catch (IOException invalidIndexPath) {
            throw new ProjectDocumentQueryException(Code.INDEX_INVALID,
                    "The project document index path is invalid or unreadable", INDEX_FILE, invalidIndexPath);
        }
        String byteHash = ProjectDocumentFormat.sha256(bytes, context::checkActive);
        CachedIndex cached = loadedIndex;
        if (cached != null && cached.rawByteHash().equals(byteHash)) {
            context.checkActive();
            return cached.data();
        }
        IndexData committed = committedIndexData;
        String committedHash = committedIndexByteHash;
        if (committed != null && committedHash != null && committedHash.equals(byteHash)) {
            context.checkActive();
            cacheIndex(committed, byteHash, context.cacheGeneration);
            context.checkActive();
            return committed;
        }
        IndexData parsed;
        try {
            parsed = ProjectDocumentFormat.readIndex(bytes, context::checkActive);
        } catch (ProjectDocumentFormat.CancellationCheckException canceled) {
            throw canceled;
        } catch (ProjectDocumentFormat.ProjectDocumentFormatException exception) {
            throw formatFailure(exception, INDEX_FILE, false);
        } catch (RuntimeException invalid) {
            throw new ProjectDocumentQueryException(Code.INDEX_INVALID,
                    "The project document index is invalid", INDEX_FILE, invalid);
        }
        context.checkActive();
        cacheIndex(parsed, byteHash, context.cacheGeneration);
        context.checkActive();
        return parsed;
    }

    private byte[] readEntryFile(EntryData entry,
                                 FileData pointer,
                                 String suffix,
                                 int maximumBytes,
                                 ReadContext context) throws IOException {
        context.checkActive();
        String expectedName = projectIdentity(entry.identity(), entry.source()).pageFileName();
        if (!expectedName.endsWith(".md")) {
            throw error(Code.INDEX_INCONSISTENT, "The index entry has an invalid API identity");
        }
        String expectedPath = "apis/" + (suffix.equals(".md")
                ? expectedName : expectedName.substring(0, expectedName.length() - 3) + suffix);
        if (!expectedPath.equals(pointer.path())) {
            throw error(Code.INDEX_INCONSISTENT, "The index entry path does not match its API identity",
                    pointer.path());
        }
        Path relative;
        try {
            relative = Path.of(pointer.path());
        } catch (RuntimeException invalidPath) {
            throw new ProjectDocumentQueryException(Code.INDEX_INCONSISTENT,
                    "The indexed document path is invalid", pointer.path(), invalidPath);
        }
        if (relative.isAbsolute() || relative.getNameCount() != 2
                || !"apis".equals(relative.getName(0).toString())
                || !relative.normalize().equals(relative)) {
            throw error(Code.INDEX_INCONSISTENT, "The indexed document path escapes the document root",
                    pointer.path());
        }
        Path file = context.documentRoot.resolve(relative).normalize();
        if (!file.startsWith(context.documentRoot)) {
            throw error(Code.INDEX_INCONSISTENT, "The indexed document path escapes the document root",
                    pointer.path());
        }
        return readBounded(file, maximumBytes, context.documentRoot, context);
    }

    private byte[] readBounded(Path path,
                               int maximumBytes,
                               Path documentRoot,
                               ReadContext context) throws IOException {
        verifyStoreRoot(context.projectRoot, documentRoot);
        verifyNoSymlinkPath(documentRoot, path, true);
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException missing) {
            throw missing;
        }
        if (!attributes.isRegularFile() || attributes.isSymbolicLink()) {
            throw new IOException("Indexed document path is not a regular file");
        }
        if (attributes.size() > maximumBytes) {
            throw error(Code.LIMIT_EXCEEDED, "An indexed document exceeds its size limit",
                    documentRoot.relativize(path).toString().replace('\\', '/'));
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream((int) attributes.size());
        byte[] buffer = new byte[8192];
        int total = 0;
        try (InputStream input = Files.newInputStream(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            int count;
            while ((count = input.read(buffer)) >= 0) {
                context.checkActive();
                if (count == 0) {
                    continue;
                }
                total += count;
                if (total > maximumBytes) {
                    throw error(Code.LIMIT_EXCEEDED, "An indexed document exceeds its size limit",
                            documentRoot.relativize(path).toString().replace('\\', '/'));
                }
                output.write(buffer, 0, count);
            }
        }
        verifyStoreRoot(context.projectRoot, documentRoot);
        verifyNoSymlinkPath(documentRoot, path, true);
        return output.toByteArray();
    }

    private void verifyStoreRoot(Path projectRoot, Path documentRoot) throws IOException {
        if (!documentRoot.startsWith(projectRoot)) {
            throw error(Code.INDEX_INVALID, "The project document root escapes the project directory");
        }
        Path current = projectRoot;
        for (Path component : projectRoot.relativize(documentRoot)) {
            current = current.resolve(component);
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                return;
            }
            BasicFileAttributes attributes = Files.readAttributes(current, BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink() || !attributes.isDirectory()) {
                throw error(Code.INDEX_INVALID, "The project document root contains an invalid path component");
            }
        }
    }

    private static void verifyNoSymlinkPath(Path documentRoot, Path target, boolean requireTarget) throws IOException {
        Path normalizedRoot = documentRoot.toAbsolutePath().normalize();
        Path normalizedTarget = target.toAbsolutePath().normalize();
        if (!normalizedTarget.startsWith(normalizedRoot)) {
            throw new IOException("Indexed document path escapes the document root");
        }
        Path current = normalizedRoot;
        if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
            BasicFileAttributes rootAttributes = Files.readAttributes(current, BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (rootAttributes.isSymbolicLink() || !rootAttributes.isDirectory()) {
                throw new IOException("The project document root is not a normal directory");
            }
        }
        Path relative = normalizedRoot.relativize(normalizedTarget);
        for (int index = 0; index < relative.getNameCount(); index++) {
            current = current.resolve(relative.getName(index));
            boolean last = index == relative.getNameCount() - 1;
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                if (last && !requireTarget) {
                    return;
                }
                throw new IOException("An indexed document path does not exist");
            }
            BasicFileAttributes attributes = Files.readAttributes(current, BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink() || (!last && !attributes.isDirectory())) {
                throw new IOException("An indexed document path contains a symbolic link or non-directory");
            }
        }
    }

    private ProjectDocumentIdentity projectIdentity(IdentityData identity, SourceData source) {
        try {
            ProjectDocumentIdentity result = ProjectDocumentIdentity.of(identity.module(), source.path(),
                    identity.qualifiedClass(), identity.methodName(), identity.parameterTypes());
            if (!result.canonicalForm().equals(identity.canonicalForm())
                    || !result.displaySignature().equals(identity.fullSignature())) {
                throw new IllegalArgumentException("Identity fields are inconsistent");
            }
            return result;
        } catch (RuntimeException invalid) {
            throw new ProjectDocumentQueryException(Code.INDEX_INCONSISTENT,
                    "The indexed API identity is invalid", null, invalid);
        }
    }

    private static EntryData findEntry(IndexData index, String apiId) {
        if (apiId == null || !apiId.matches("[a-f0-9]{64}")) {
            throw error(Code.INVALID_ARGUMENT, "apiId must be a lowercase SHA-256 identity");
        }
        for (EntryData entry : index.entries()) {
            if (entry.apiId().equals(apiId)) {
                return entry;
            }
        }
        throw error(Code.API_NOT_FOUND, "The API identity is not present in the current project index");
    }

    private static void requireSnapshot(IndexData index, String expectedSnapshotId) {
        if (expectedSnapshotId == null || expectedSnapshotId.isBlank()) {
            throw error(Code.INVALID_ARGUMENT, "snapshotId is required for a detail read");
        }
        if (!index.snapshotId().equals(expectedSnapshotId)) {
            throw error(Code.SNAPSHOT_CHANGED, "The project document index has changed since the result was listed");
        }
    }

    private static boolean summaryMatchesDetail(EntryData summary, ApiData detail) {
        return summary.apiId().equals(detail.apiId())
                && summary.identity().canonicalForm().equals(detail.identity().canonicalForm())
                && summary.name().equals(detail.name())
                && summary.description().equals(detail.description())
                && summary.framework().equals(detail.framework())
                && Objects.equals(summary.httpMethod(), detail.httpMethod())
                && Objects.equals(summary.httpPath(), detail.httpPath())
                && summary.tags().equals(detail.tags())
                && summary.source().equals(detail.source());
    }

    private boolean matches(SearchProjection candidate, SearchRequest request) {
        EntryData entry = candidate.entry();
        if (!matchesFilters(entry, request)) {
            return false;
        }
        for (String term : request.terms()) {
            boolean fieldMatched = false;
            for (String field : candidate.normalizedFields()) {
                if (field.contains(term)) {
                    fieldMatched = true;
                    break;
                }
            }
            if (!fieldMatched) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesFilters(EntryData entry, SearchRequest request) {
        if (request.module() != null && !request.module().equals(entry.identity().module())) {
            return false;
        }
        if (request.framework() != null && !request.framework().equals(entry.framework())) {
            return false;
        }
        if (request.httpMethod() != null && !request.httpMethod().equals(entry.httpMethod())) {
            return false;
        }
        return true;
    }

    private static List<String> searchableFields(EntryData entry) {
        List<String> fields = new ArrayList<>(9 + entry.tags().size());
        fields.add(entry.name());
        fields.add(entry.description());
        fields.add(entry.identity().qualifiedClass());
        fields.add(entry.identity().fullSignature());
        fields.add(entry.identity().module());
        fields.add(entry.source().path());
        if (entry.httpPath() != null) {
            fields.add(entry.httpPath());
        }
        fields.addAll(entry.tags());
        return fields;
    }

    private static SearchRequest normalizeQuery(ProjectDocumentQuery query) {
        int limit = query.effectiveLimit();
        if (limit < 1 || limit > ProjectDocumentQuery.MAX_LIMIT) {
            throw error(Code.INVALID_ARGUMENT, "limit must be between 1 and 100");
        }
        String normalized = normalizeForSearch(query.query());
        int codePoints = normalized.codePointCount(0, normalized.length());
        if (codePoints > 256) {
            throw error(Code.LIMIT_EXCEEDED, "query exceeds 256 Unicode code points");
        }
        String stripped = normalized.strip();
        List<String> terms = stripped.isEmpty()
                ? List.of()
                : List.of(stripped.split("(?U)\\s+"));
        if (terms.size() > 16) {
            throw error(Code.LIMIT_EXCEEDED, "query exceeds 16 search terms");
        }
        if (query.module() != null && query.module().isBlank()) {
            throw error(Code.INVALID_ARGUMENT, "module filter must be omitted or non-blank");
        }
        if (query.framework() != null && !SEARCH_FRAMEWORKS.contains(query.framework())) {
            throw error(Code.INVALID_ARGUMENT, "framework filter is not a supported exact value");
        }
        if (query.httpMethod() != null && query.httpMethod().isBlank()) {
            throw error(Code.INVALID_ARGUMENT, "httpMethod filter must be omitted or non-blank");
        }
        return new SearchRequest(String.join(" ", terms), terms, query.module(), query.framework(), query.httpMethod(), limit,
                query.cursor());
    }

    private static String normalizeForSearch(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private static String encodeCursor(SearchRequest request, String snapshotId, String lastCanonicalForm) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (var output = new java.io.DataOutputStream(bytes)) {
                output.writeInt(1);
                writeCursorString(output, snapshotId);
                writeCursorString(output, request.normalizedQuery());
                writeCursorString(output, request.module());
                writeCursorString(output, request.framework());
                writeCursorString(output, request.httpMethod());
                writeCursorString(output, lastCanonicalForm);
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException impossible) {
            throw new IllegalStateException("Could not encode project document cursor", impossible);
        }
    }

    private static void writeCursorString(java.io.DataOutputStream output, @Nullable String value) throws IOException {
        if (value == null) {
            output.writeInt(-1);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static @Nullable Cursor decodeCursor(@Nullable String encoded,
                                                 SearchRequest request,
                                                 String currentSnapshotId) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        if (encoded.length() > 16_384) {
            throw error(Code.CURSOR_STALE, "The search cursor is invalid or too large");
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(encoded);
            try (var input = new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes))) {
                if (input.readInt() != 1) {
                    throw error(Code.CURSOR_STALE, "The search cursor version is unsupported");
                }
                String snapshotId = readCursorString(input);
                String query = readCursorString(input);
                String module = readCursorString(input);
                String framework = readCursorString(input);
                String httpMethod = readCursorString(input);
                String lastCanonicalForm = readCursorString(input);
                if (input.available() != 0 || snapshotId == null || query == null || lastCanonicalForm == null
                        || !currentSnapshotId.equals(snapshotId)
                        || !request.normalizedQuery().equals(query)
                        || !Objects.equals(request.module(), module)
                        || !Objects.equals(request.framework(), framework)
                        || !Objects.equals(request.httpMethod(), httpMethod)) {
                    throw error(Code.CURSOR_STALE, "The search cursor belongs to another query or snapshot");
                }
                return new Cursor(lastCanonicalForm);
            }
        } catch (ProjectDocumentQueryException exception) {
            throw exception;
        } catch (IOException | IllegalArgumentException invalid) {
            throw new ProjectDocumentQueryException(Code.CURSOR_STALE,
                    "The search cursor is invalid or no longer usable", null, invalid);
        }
    }

    private static @Nullable String readCursorString(java.io.DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length == -1) {
            return null;
        }
        if (length < 0 || length > 65_536 || length > input.available()) {
            throw new IOException("Invalid cursor field length");
        }
        byte[] bytes = input.readNBytes(length);
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException invalidUtf8) {
            throw new IOException("Invalid cursor UTF-8", invalidUtf8);
        }
    }

    private ProjectDocumentIndexStatus statusOf(IndexState state, IndexData index, @Nullable String errorCode) {
        return new ProjectDocumentIndexStatus(state, freshnessFor(index.snapshotId()), index.snapshotId(),
                index.entries().size(), index.generatedAt(), index.hasUnsavedDocuments(), errorCode);
    }

    private Freshness freshnessFor(String snapshotId) {
        observeSettingsChanges();
        String committed = committedSnapshotId;
        if (committed == null || !committed.equals(snapshotId) || committedInputEpoch < 0) {
            return Freshness.UNKNOWN;
        }
        return committedInputEpoch == inputEpoch.get() ? Freshness.CURRENT : Freshness.STALE;
    }

    private String decodeUtf8(byte[] bytes, String snapshotId, String relativePath) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException invalidUtf8) {
            markInconsistent(snapshotId, relativePath);
            throw new ProjectDocumentQueryException(Code.INDEX_INCONSISTENT,
                    "The indexed Markdown is not valid UTF-8", relativePath, invalidUtf8);
        }
    }

    private boolean hasKnownInconsistency(String snapshotId) {
        return snapshotId.equals(inconsistentSnapshotId) && !inconsistentResources.isEmpty();
    }

    private long inconsistencyToken(String snapshotId, String resourcePath) {
        synchronized (cacheLock) {
            if (!snapshotId.equals(inconsistentSnapshotId)) {
                return 0L;
            }
            return inconsistentResources.getOrDefault(resourcePath, 0L);
        }
    }

    private void markInconsistent(String snapshotId, String resource) {
        boolean changed = false;
        synchronized (cacheLock) {
            if (!snapshotId.equals(inconsistentSnapshotId)) {
                inconsistentSnapshotId = snapshotId;
                inconsistentResources = Map.of();
                changed = true;
            }
            changed |= !inconsistentResources.containsKey(resource);
            Map<String, Long> updated = new HashMap<>(inconsistentResources);
            updated.put(resource, ++inconsistencyTokenCounter);
            inconsistentResources = Map.copyOf(updated);
        }
        if (changed) {
            publishStateChanged();
        }
    }

    private void clearInconsistent(String snapshotId, String resourcePath, long capturedToken) {
        if (capturedToken == 0L) {
            return;
        }
        boolean changed = false;
        synchronized (cacheLock) {
            if (!snapshotId.equals(inconsistentSnapshotId)
                    || !Objects.equals(inconsistentResources.get(resourcePath), capturedToken)) {
                return;
            }
            Map<String, Long> updated = new HashMap<>(inconsistentResources);
            updated.remove(resourcePath);
            inconsistentResources = Map.copyOf(updated);
            if (inconsistentResources.isEmpty()) {
                inconsistentSnapshotId = null;
            }
            changed = true;
        }
        if (changed) {
            publishStateChanged();
        }
    }

    private void cacheIndex(IndexData data, String rawByteHash, long expectedCacheGeneration) {
        synchronized (cacheLock) {
            if (cacheGeneration.get() != expectedCacheGeneration) {
                return;
            }
            loadedIndex = new CachedIndex(data, rawByteHash, null);
            if (inconsistentSnapshotId != null && !inconsistentSnapshotId.equals(data.snapshotId())) {
                inconsistentSnapshotId = null;
                inconsistentResources = Map.of();
            }
        }
    }

    private List<SearchProjection> searchProjection(IndexData index, ReadContext context) {
        CachedIndex cached = loadedIndex;
        if (cached == null || cached.data() != index) {
            return buildSearchProjection(index, context);
        }
        if (cached.searchableEntries() != null) {
            return cached.searchableEntries();
        }
        List<SearchProjection> prepared = buildSearchProjection(index, context);
        synchronized (cacheLock) {
            CachedIndex current = loadedIndex;
            if (current == cached && cacheGeneration.get() == context.cacheGeneration) {
                loadedIndex = new CachedIndex(cached.data(), cached.rawByteHash(), prepared);
            } else if (current != null && current.data() == index && current.searchableEntries() != null) {
                return current.searchableEntries();
            }
        }
        return prepared;
    }

    private static List<SearchProjection> buildSearchProjection(IndexData index, ReadContext context) {
        List<SearchProjection> prepared = new ArrayList<>(index.entries().size());
        for (EntryData entry : index.entries()) {
            context.checkActive();
            List<String> normalizedFields = searchableFields(entry).stream()
                    .map(ProjectDocumentIndexService::normalizeForSearch)
                    .toList();
            prepared.add(new SearchProjection(entry, normalizedFields));
        }
        return List.copyOf(prepared);
    }

    private Path projectRoot() {
        String basePath = project.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            throw error(Code.PROJECT_NOT_OPEN, "The project does not have a local base path");
        }
        try {
            return Path.of(basePath).toRealPath();
        } catch (IOException | RuntimeException unavailable) {
            throw new ProjectDocumentQueryException(Code.PROJECT_NOT_OPEN,
                    "The project base path is unavailable", null, unavailable);
        }
    }

    private void observeSettingsChanges() {
        if (project.isDisposed()) {
            return;
        }
        String current = settingsFingerprint();
        boolean changed = false;
        synchronized (observationLock) {
            String previous = observedSettingsFingerprint;
            if (previous == null) {
                observedSettingsFingerprint = current;
                return;
            }
            if (!previous.equals(current)) {
                observedSettingsFingerprint = current;
                inputEpoch.incrementAndGet();
                changed = true;
            }
        }
        if (changed) {
            publishStateChanged();
        }
    }

    private void publishStateChanged() {
        if (!project.isDisposed()) {
            project.getMessageBus().syncPublisher(ProjectDocumentIndexListener.TOPIC)
                    .stateChanged(inputEpoch.get(), committedSnapshotId);
        }
    }

    private @Nullable String settingsFingerprint() {
        if (project.isDisposed()) {
            return null;
        }
        try {
            Settings settings = Settings.getInstance(project);
            TemplateSettings templates = TemplateSettings.getInstance(project);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            addFingerprint(digest, settings.getTitleTag());
            addFingerprint(digest, settings.getTitleUseCommentTag());
            addFingerprint(digest, settings.getTitleClassComment());
            addFingerprint(digest, settings.getTitleUseSimpleClassName());
            addFingerprint(digest, settings.getTitleUseFullClassName());
            addFingerprint(digest, settings.getNameTag());
            addFingerprint(digest, settings.getNameUseSwagger3());
            addFingerprint(digest, settings.getNameUseSwagger());
            addFingerprint(digest, settings.getNameUseCommentTag());
            addFingerprint(digest, settings.getNameMethodComment());
            addFingerprint(digest, settings.getDescUseSwagger3());
            addFingerprint(digest, settings.getDescUseSwagger());
            addFingerprint(digest, settings.getRequired());
            addFingerprint(digest, settings.getRequiredUseCommentTag());
            addFingerprint(digest, settings.getFieldNameJsonProperty());
            addFingerprint(digest, settings.getMergeExport());
            addFingerprint(digest, settings.getLineMarker());
            addFingerprint(digest, settings.getIncludeNormalInterface());
            addSetFingerprint(digest, settings.getContainClassAnnotationName());
            addSetFingerprint(digest, settings.getContainMethodAnnotationName());
            addSetFingerprint(digest, settings.getRequiredFieldAnnotation());
            addSetFingerprint(digest, settings.getFieldNameAnnotation());
            addSetFingerprint(digest, settings.getExcludeFieldNames());
            addSetFingerprint(digest, settings.getExcludeParameterType());
            addSetFingerprint(digest, settings.getExcludeFieldAnnotation());
            addSetFingerprint(digest, settings.getExcludeClassPackage());
            addFingerprint(digest, settings.getPrefixSymbol1());
            addFingerprint(digest, settings.getPrefixSymbol2());
            addFingerprint(digest, settings.getSeparateParam());
            addFingerprint(digest, templates.getSpringTemplate());
            addFingerprint(digest, templates.getDubboTemplate());
            return HexFormat.of().formatHex(digest.digest());
        } catch (RuntimeException | NoSuchAlgorithmException unavailable) {
            return null;
        }
    }

    private static void addSetFingerprint(MessageDigest digest, Set<String> values) {
        if (values == null) {
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(-1).array());
            return;
        }
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(values.size()).array());
        values.stream().sorted(Comparator.nullsFirst(String::compareTo)).forEach(value -> addFingerprint(digest, value));
    }

    private static void addFingerprint(MessageDigest digest, @Nullable Object value) {
        if (value == null) {
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(-1).array());
            return;
        }
        byte[] bytes = String.valueOf(value).getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private void markChangedSourceFile(@Nullable VirtualFile file) {
        if (file == null || project.isDisposed()) {
            return;
        }
        String extension = file.getExtension();
        if (!"java".equalsIgnoreCase(extension) && !"xml".equalsIgnoreCase(extension)) {
            return;
        }
        // Java sources and Dubbo XML declarations both affect the snapshot. The scanner also
        // searches configured module content roots outside basePath, so track the same scope.
        if (!ProjectFileIndex.getInstance(project).isInContent(file)) {
            return;
        }
        String basePath = project.getBasePath();
        if (basePath != null) {
            try {
                Path root = Path.of(basePath).toAbsolutePath().normalize();
                Path source = Path.of(file.getPath()).toAbsolutePath().normalize();
                if (source.startsWith(root.resolve(DOCUMENT_ROOT))) {
                    return;
                }
            } catch (RuntimeException ignored) {
                // A non-local path cannot identify the generated document root; content membership is enough.
            }
        }
        markObservedInputsChanged();
    }

    private void markChangedSourceFile(@Nullable PsiFile file) {
        if (file != null) {
            markChangedSourceFile(file.getVirtualFile());
        }
    }

    private static ProjectDocumentQueryException formatFailure(
            ProjectDocumentFormat.ProjectDocumentFormatException exception,
            String relativePath,
            boolean detail) {
        Code code = switch (exception.reason()) {
            case UNSUPPORTED_SCHEMA -> Code.UNSUPPORTED_SCHEMA;
            case LIMIT_EXCEEDED -> Code.LIMIT_EXCEEDED;
            case INCONSISTENT -> detail ? Code.INDEX_INCONSISTENT : Code.INDEX_INVALID;
            case INVALID -> detail ? Code.INDEX_INCONSISTENT : Code.INDEX_INVALID;
            case INTEGRITY_MISMATCH -> detail ? Code.INDEX_INCONSISTENT : Code.INDEX_INVALID;
        };
        return new ProjectDocumentQueryException(code,
                detail ? "The indexed API detail is invalid" : "The project document index is invalid",
                relativePath, exception);
    }

    private static long deadlineAfter(Duration duration) {
        return System.nanoTime() + duration.toNanos();
    }

    private static ProjectDocumentQueryException error(Code code, String message) {
        return new ProjectDocumentQueryException(code, message);
    }

    private static ProjectDocumentQueryException error(Code code, String message, String relativePath) {
        return new ProjectDocumentQueryException(code, message, relativePath);
    }

    @Override
    public void dispose() {
        // Project-scoped listeners are automatically detached through this service's parent disposable.
    }

    private interface ReadOperation<T> {
        T run(ReadContext context) throws IOException;
    }

    private record SearchRequest(String normalizedQuery,
                                 List<String> terms,
                                 @Nullable String module,
                                 @Nullable String framework,
                                 @Nullable String httpMethod,
                                 int limit,
                                 @Nullable String cursor) {
    }

    private record Cursor(String lastCanonicalForm) {
    }

    private record CachedIndex(IndexData data,
                               String rawByteHash,
                               @Nullable List<SearchProjection> searchableEntries) {
    }

    private record SearchProjection(EntryData entry, List<String> normalizedFields) {
        private SearchProjection {
            normalizedFields = List.copyOf(normalizedFields);
        }
    }

    private static final class ReadContext {
        private final Project project;
        private final ProgressIndicator indicator;
        private final long deadline;
        private final long cacheGeneration;
        private Path documentRoot;
        private Path projectRoot;

        private ReadContext(Project project, ProgressIndicator indicator, long deadline, long cacheGeneration) {
            this.project = project;
            this.indicator = indicator;
            this.deadline = deadline;
            this.cacheGeneration = cacheGeneration;
        }

        private void checkActive() {
            if (project.isDisposed()) {
                throw error(Code.PROJECT_NOT_OPEN, "The project is closed");
            }
            indicator.checkCanceled();
            if (remainingNanos() <= 0) {
                throw error(Code.TIMEOUT, "The project document query exceeded its five-second budget");
            }
        }

        private long remainingNanos() {
            return Math.max(0, deadline - System.nanoTime());
        }
    }
}
