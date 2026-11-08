package com.liuzhihang.doc.view.service.export;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;

import com.liuzhihang.doc.view.service.document.DocumentStoreLocks;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.*;
import com.liuzhihang.doc.view.service.document.ProjectDocumentFormat;
import com.liuzhihang.doc.view.service.document.ProjectDocumentQueryException;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.StandardOpenOption;
import java.nio.file.FileAlreadyExistsException;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CharacterCodingException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.BooleanSupplier;

/**
 * Publishes PSI-free API pages into a project directory.
 */
public final class ProjectDocumentExporter {

    private static final Pattern PAGE_MARKER = Pattern.compile(
            "^<!-- doc-view-page:v1 identity=([A-Za-z0-9_-]+) sha256=([a-f0-9]{64}) -->\\n");
    private static final Pattern INDEX_MARKER = Pattern.compile(
            "^<!-- doc-view-index:v1 sha256=([a-f0-9]{64}) -->\\n");
    private static final String OUTPUT_ROOT = "docs/doc-view";
    private static final String API_DIRECTORY = OUTPUT_ROOT + "/apis";
    private static final String INDEX_PATH = OUTPUT_ROOT + "/index.md";
    private static final String JSON_INDEX_PATH = OUTPUT_ROOT + "/index.json";
    private static final long MAX_PAGE_BYTES = 1024 * 1024;
    private static final long MAX_INDEX_BYTES = 32 * 1024 * 1024;
    private final ExportObserver observer;

    public ProjectDocumentExporter() {
        this(new ExportObserver() {
        });
    }

    ProjectDocumentExporter(ExportObserver observer) {
        this.observer = Objects.requireNonNull(observer, "observer");
    }

    public ProjectDocumentExportResult export(Path projectRoot, ProjectDocumentSnapshot snapshot) throws IOException {
        return export(projectRoot, snapshot, new EmptyProgressIndicator());
    }

    public ProjectDocumentExportResult export(Path projectRoot, ProjectDocumentSnapshot snapshot,
                                              ProgressIndicator indicator) throws IOException {
        return export(projectRoot, snapshot, indicator, () -> true, () -> {}, index -> {});
    }

    /** index.json is the commit point; the callback installs the already validated immutable snapshot. */
    public ProjectDocumentExportResult export(Path projectRoot, ProjectDocumentSnapshot snapshot,
                                              ProgressIndicator indicator, BooleanSupplier publicationAllowed,
                                              Runnable checkInputs, Consumer<IndexData> onCommitted) throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(indicator, "indicator");
        Objects.requireNonNull(publicationAllowed, "publicationAllowed");
        Objects.requireNonNull(checkInputs, "checkInputs");
        Objects.requireNonNull(onCommitted, "onCommitted");
        checkPublication(indicator, publicationAllowed, checkInputs);
        Path root = projectRoot.toRealPath();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Project root is not a directory");
        }
        List<ProjectDocumentPage> pages = normalizePages(snapshot.pages());
        List<String> conflicts = preflight(root, pages);
        if (!conflicts.isEmpty()) {
            throw new ProjectDocumentConflictException(conflicts);
        }
        try (DocumentStoreLocks.Lease lease = DocumentStoreLocks.tryAcquireWrite(root, indicator, Duration.ofSeconds(1))) {
            if (lease == null) {
                throw busy();
            }
            ensureDirectory(root.resolve("docs"));
            validateExistingOutputDirectories(root);
            ensureDirectory(root.resolve(OUTPUT_ROOT));
            validateExistingOutputDirectories(root);
            ensureDirectory(root.resolve(API_DIRECTORY));
            validateOutputDirectories(root);
            Path lockPath = root.resolve(OUTPUT_ROOT).resolve(".doc-view.lock");
            if (Files.exists(lockPath, LinkOption.NOFOLLOW_LINKS)
                    && (Files.isSymbolicLink(lockPath) || !Files.isRegularFile(lockPath, LinkOption.NOFOLLOW_LINKS))) {
                throw new ProjectDocumentConflictException(List.of(OUTPUT_ROOT + "/.doc-view.lock"));
            }
            // Keep the lock inode after release: deleting it lets another IDE lock a different inode.
            try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                FileLock lock;
                try {
                    lock = channel.tryLock();
                } catch (OverlappingFileLockException overlap) {
                    throw busy();
                }
                if (lock == null) {
                    throw busy();
                }
                try (lock) {
                    checkPublication(indicator, publicationAllowed, checkInputs);
                    return publish(root, snapshot, pages, indicator, publicationAllowed, checkInputs, onCommitted);
                }
            }
        }
    }

    private static ProjectDocumentQueryException busy() {
        return new ProjectDocumentQueryException(ProjectDocumentQueryException.Code.BUSY,
                "Project documentation is being generated");
    }

    private static void checkPublication(ProgressIndicator indicator, BooleanSupplier allowed, Runnable checkInputs) {
        indicator.checkCanceled();
        if (!allowed.getAsBoolean()) {
            throw new ProcessCanceledException();
        }
        checkInputs.run();
    }

    private ProjectDocumentExportResult publish(Path canonicalRoot, ProjectDocumentSnapshot snapshot,
                                                List<ProjectDocumentPage> sortedPages, ProgressIndicator indicator,
                                                BooleanSupplier publicationAllowed, Runnable checkInputs,
                                                Consumer<IndexData> onCommitted) throws IOException {
        LinkedHashMap<String, byte[]> contents = new LinkedHashMap<>();
        List<EntryData> entries = new ArrayList<>();
        for (ProjectDocumentPage page : sortedPages) {
            checkPublication(indicator, publicationAllowed, checkInputs);
            String markdownPath = "apis/" + page.identity().pageFileName();
            String detailPath = markdownPath.substring(0, markdownPath.length() - 3) + ".json";
            byte[] markdown = markedContent("page", page.identity().canonicalForm(),
                    pageBody(canonicalRoot, page, OUTPUT_ROOT + "/" + markdownPath));
            if (markdown.length > MAX_PAGE_BYTES) {
                throw new ProjectDocumentQueryException(ProjectDocumentQueryException.Code.LIMIT_EXCEEDED,
                        "Generated Markdown exceeds the page limit", markdownPath);
            }
            byte[] detail = ProjectDocumentFormat.writeApi(page.data());
            contents.put(OUTPUT_ROOT + "/" + markdownPath, markdown);
            contents.put(OUTPUT_ROOT + "/" + detailPath, detail);
            ApiData data = page.data();
            entries.add(new EntryData(data.apiId(), data.identity(), data.name(), data.description(), data.framework(),
                    data.httpMethod(), data.httpPath(), data.tags(), data.source(),
                    new FileData(markdownPath, sha256(markdown)), new FileData(detailPath, sha256(detail))));
        }
        byte[] markdownIndex = markedIndex(indexBody(canonicalRoot, sortedPages));
        if (markdownIndex.length > MAX_INDEX_BYTES) {
            throw new ProjectDocumentQueryException(ProjectDocumentQueryException.Code.LIMIT_EXCEEDED,
                    "Generated Markdown index exceeds the index limit", "index.md");
        }
        contents.put(INDEX_PATH, markdownIndex);
        IndexData candidate = new IndexData("", Instant.now().toString(), snapshot.producerVersion(),
                snapshot.generatorFingerprint(), snapshot.hasUnsavedDocuments(),
                new FileData("index.md", sha256(markdownIndex)), entries);
        String snapshotId = ProjectDocumentFormat.computeSnapshotId(candidate);
        String generatedAt = candidate.generatedAt();
        Path previousIndex = canonicalRoot.resolve(JSON_INDEX_PATH);
        if (Files.isRegularFile(previousIndex, LinkOption.NOFOLLOW_LINKS) && Files.size(previousIndex) <= MAX_INDEX_BYTES) {
            try {
                IndexData previous = ProjectDocumentFormat.readIndex(readBounded(previousIndex));
                if (snapshotId.equals(previous.snapshotId())) {
                    generatedAt = previous.generatedAt();
                }
            } catch (IllegalArgumentException ignored) {
                // Ownership is checked below. An unknown owned schema may be explicitly regenerated.
            }
        }
        IndexData index = new IndexData(snapshotId, generatedAt, candidate.producerVersion(),
                candidate.generatorFingerprint(), candidate.hasUnsavedDocuments(), candidate.markdownIndex(), entries);
        contents.put(JSON_INDEX_PATH, ProjectDocumentFormat.writeIndex(index));
        List<String> conflicts = preflight(canonicalRoot, sortedPages);
        if (!conflicts.isEmpty()) {
            throw new ProjectDocumentConflictException(conflicts);
        }
        Map<Path, TargetState> expectedTargets = snapshotTargets(canonicalRoot, contents.keySet());
        Map<Path, byte[]> changed = new LinkedHashMap<>();
        List<String> unchanged = new ArrayList<>();
        for (Map.Entry<String, byte[]> entry : contents.entrySet()) {
            Path target = canonicalRoot.resolve(entry.getKey());
            TargetState state = expectedTargets.get(target);
            if (state.exists() && MessageDigest.isEqual(state.bytes(), entry.getValue())) {
                unchanged.add(entry.getKey());
            } else {
                changed.put(target, entry.getValue());
            }
        }
        List<StagedFile> staged = new ArrayList<>();
        try {
            for (Map.Entry<Path, byte[]> entry : changed.entrySet()) {
                checkPublication(indicator, publicationAllowed, checkInputs);
                validateOutputDirectories(canonicalRoot);
                Path temporary = Files.createTempFile(entry.getKey().getParent(), ".doc-view-", ".tmp");
                staged.add(new StagedFile(entry.getKey(), temporary, entry.getValue(), relative(canonicalRoot, entry.getKey())));
                Files.write(temporary, entry.getValue());
            }
            observer.afterStaging(canonicalRoot);
            checkPublication(indicator, publicationAllowed, checkInputs);
            validateOutputDirectories(canonicalRoot);
            conflicts = changedTargets(canonicalRoot, expectedTargets);
            if (!conflicts.isEmpty()) {
                throw new ProjectDocumentConflictException(conflicts);
            }
        } catch (IOException | RuntimeException | Error throwable) {
            List<String> leftovers = cleanup(canonicalRoot, staged);
            if (!leftovers.isEmpty()) {
                throw new ProjectDocumentTemporaryCleanupException(leftovers, throwable);
            }
            throw throwable;
        }
        List<String> published = new ArrayList<>();
        StagedFile current = null;
        try {
            // LinkedHashMap insertion order guarantees that index.json is always the final move.
            for (StagedFile file : staged) {
                current = file;
                checkPublication(indicator, publicationAllowed, checkInputs);
                observer.beforeMove(file.relativePath());
                checkPublication(indicator, publicationAllowed, checkInputs);
                validateOutputDirectories(canonicalRoot);
                if (!expectedTargets.get(file.target()).matches(file.target())) {
                    throw new ProjectDocumentConflictException(List.of(file.relativePath()));
                }
                if (file.relativePath().equals(JSON_INDEX_PATH)) {
                    conflicts = changedTargets(canonicalRoot, expectedTargets);
                    if (!conflicts.isEmpty()) {
                        throw new ProjectDocumentConflictException(conflicts);
                    }
                    checkPublication(indicator, publicationAllowed, checkInputs);
                }
                atomicReplace(file);
                published.add(file.relativePath());
                expectedTargets.put(file.target(), TargetState.of(file.content()));
                if (!file.relativePath().equals(JSON_INDEX_PATH)) {
                    observer.afterMove(file.relativePath());
                    checkPublication(indicator, publicationAllowed, checkInputs);
                }
            }
        } catch (IOException | RuntimeException exception) {
            List<String> leftovers = cleanup(canonicalRoot, staged);
            if (published.isEmpty()) {
                if (!leftovers.isEmpty()) {
                    throw new ProjectDocumentTemporaryCleanupException(leftovers, exception);
                }
                throw exception;
            }
            String failedPath = current == null || published.contains(current.relativePath())
                    ? null : current.relativePath();
            throw new ProjectDocumentPublishException(exception, published, failedPath, leftovers);
        }
        // A late cancellation cannot turn a committed generation into a cancelled result.
        try {
            onCommitted.accept(index);
        } catch (ProcessCanceledException lateCancellation) {
            // The final move already committed; only the optional derived-cache installation was interrupted.
        } catch (RuntimeException cacheFailure) {
            com.intellij.openapi.diagnostic.Logger.getInstance(ProjectDocumentExporter.class)
                    .warn("Project documents were committed but their derived cache could not be installed", cacheFailure);
        }
        return new ProjectDocumentExportResult(sortedPages.size(), published, unchanged);
    }

    private static List<ProjectDocumentPage> normalizePages(List<ProjectDocumentPage> pages) {
        TreeMap<String, ProjectDocumentPage> byIdentity = new TreeMap<>();
        for (ProjectDocumentPage page : pages) {
            Objects.requireNonNull(page, "pages must not contain null");
            ProjectDocumentPage previous = byIdentity.putIfAbsent(page.identity().canonicalForm(), page);
            if (previous != null && (!previous.markdown().equals(page.markdown()) || !previous.data().equals(page.data()))) {
                throw new IllegalArgumentException("Duplicate project document identity has different content");
            }
        }
        return List.copyOf(byIdentity.values());
    }

    private static String pageBody(Path projectRoot, ProjectDocumentPage page, String relativePagePath) {
        Path pagePath = projectRoot.resolve(relativePagePath);
        String sourceHref = relativeHref(projectRoot, pagePath.getParent(), page.identity().sourcePath());
        String sourceLabel = escapeMarkdown(page.identity().sourcePath());
        String markdown = page.markdown();
        if (!markdown.isEmpty() && !markdown.endsWith("\n")) {
            markdown += "\n";
        }
        return "## Source\n\n[" + sourceLabel + "](" + sourceHref + ")\n\n" + markdown;
    }

    private static String indexBody(Path projectRoot, List<ProjectDocumentPage> pages) {
        StringBuilder body = new StringBuilder("# Doc View API Index\n\n")
                .append("Generated from the current project source by Doc View.\n\n");
        Path indexParent = projectRoot.resolve(INDEX_PATH).getParent();
        TreeMap<String, TreeMap<String, List<ProjectDocumentPage>>> byModuleAndClass = new TreeMap<>();
        for (ProjectDocumentPage page : pages) {
            ProjectDocumentIdentity identity = page.identity();
            byModuleAndClass.computeIfAbsent(identity.moduleName(), ignored -> new TreeMap<>())
                    .computeIfAbsent(identity.qualifiedClassName(), ignored -> new ArrayList<>())
                    .add(page);
        }
        Comparator<ProjectDocumentPage> byDisplaySignature = Comparator
                .comparing((ProjectDocumentPage page) -> page.identity().displaySignature())
                .thenComparing(page -> page.identity().canonicalForm());
        for (Map.Entry<String, TreeMap<String, List<ProjectDocumentPage>>> moduleEntry : byModuleAndClass.entrySet()) {
            body.append("## Module: ").append(inlineCode(moduleEntry.getKey())).append("\n\n");
            for (Map.Entry<String, List<ProjectDocumentPage>> classEntry : moduleEntry.getValue().entrySet()) {
                body.append("### ").append(inlineCode(classEntry.getKey())).append("\n\n");
                classEntry.getValue().sort(byDisplaySignature);
                for (ProjectDocumentPage page : classEntry.getValue()) {
                    ProjectDocumentIdentity identity = page.identity();
                    String sourceHref = relativeHref(projectRoot, indexParent, identity.sourcePath());
                    String pageHref = relativeHref(projectRoot, indexParent,
                            API_DIRECTORY + "/" + identity.pageFileName());
                    body.append("<!-- doc-view-entry:v1 identity=").append(identity.canonicalForm())
                            .append(" sha256=").append(identity.sha256()).append(" -->\n")
                            .append("- ").append(inlineCode(identity.displaySignature()))
                            .append(" · ").append(inlineCode(page.framework()))
                            .append(" · [Document](").append(pageHref).append(")")
                            .append(" · [Source](").append(sourceHref).append(")\n\n");
                }
            }
        }
        return body.toString();
    }

    private static String inlineCode(String value) {
        String delimiter = "`";
        while (value.contains(delimiter)) {
            delimiter += "`";
        }
        boolean needsPadding = value.startsWith(" ") || value.endsWith(" ")
                || value.startsWith("`") || value.endsWith("`");
        return delimiter + (needsPadding ? " " : "") + value + (needsPadding ? " " : "") + delimiter;
    }

    private static byte[] markedContent(String type, String canonicalIdentity, String body) {
        String marker = "<!-- doc-view-" + type + ":v1 identity=" + canonicalIdentity
                + " sha256=" + sha256(body.getBytes(StandardCharsets.UTF_8)) + " -->\n";
        return (marker + body).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] markedIndex(String body) {
        String marker = "<!-- doc-view-index:v1 sha256="
                + sha256(body.getBytes(StandardCharsets.UTF_8)) + " -->\n";
        return (marker + body).getBytes(StandardCharsets.UTF_8);
    }

    private static List<String> preflight(Path projectRoot,
                                          List<ProjectDocumentPage> pages) throws IOException {
        List<String> conflicts = new ArrayList<>();
        checkDirectory(projectRoot, "docs", conflicts);
        checkDirectory(projectRoot, OUTPUT_ROOT, conflicts);
        checkDirectory(projectRoot, API_DIRECTORY, conflicts);
        if (!conflicts.isEmpty()) {
            return conflicts.stream().distinct().sorted().toList();
        }

        for (ProjectDocumentPage page : pages) {
            String relativePath = API_DIRECTORY + "/" + page.identity().pageFileName();
            checkPageTarget(projectRoot.resolve(relativePath), page.identity(), relativePath, conflicts);
            String jsonPath = relativePath.substring(0, relativePath.length() - 3) + ".json";
            checkJsonTarget(projectRoot.resolve(jsonPath), "api", page.identity(), jsonPath, conflicts);
        }
        checkIndexTarget(projectRoot.resolve(INDEX_PATH), INDEX_PATH, conflicts);
        checkJsonTarget(projectRoot.resolve(JSON_INDEX_PATH), "index", null, JSON_INDEX_PATH, conflicts);
        return conflicts.stream().distinct().sorted().toList();
    }

    private static void checkJsonTarget(Path target, String kind, ProjectDocumentIdentity identity,
                                        String relative, List<String> conflicts) throws IOException {
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (Files.isSymbolicLink(target) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)
                || Files.size(target) > (kind.equals("index") ? MAX_INDEX_BYTES : MAX_PAGE_BYTES)) {
            conflicts.add(relative);
            return;
        }
        try {
            ProjectDocumentFormat.validateOwnedJson(readBounded(target), kind, identity);
        } catch (IllegalArgumentException invalid) {
            conflicts.add(relative);
        }
    }

    private static Map<Path, TargetState> snapshotTargets(Path projectRoot,
                                                          Iterable<String> relativePaths) throws IOException {
        Map<Path, TargetState> states = new LinkedHashMap<>();
        for (String relativePath : relativePaths) {
            Path target = projectRoot.resolve(relativePath);
            if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                states.put(target, TargetState.absent());
            } else if (Files.isSymbolicLink(target)
                    || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                throw new ProjectDocumentConflictException(List.of(relativePath));
            } else {
                states.put(target, TargetState.of(readBounded(target)));
            }
        }
        return states;
    }

    private static List<String> changedTargets(Path projectRoot, Map<Path, TargetState> expectedTargets)
            throws IOException {
        List<String> conflicts = new ArrayList<>();
        for (Map.Entry<Path, TargetState> entry : expectedTargets.entrySet()) {
            if (!entry.getValue().matches(entry.getKey())) {
                conflicts.add(relative(projectRoot, entry.getKey()));
            }
        }
        return conflicts.stream().distinct().sorted().toList();
    }

    private static void validateOutputDirectories(Path projectRoot) throws IOException {
        validateDirectories(projectRoot, true);
    }

    private static void validateExistingOutputDirectories(Path projectRoot) throws IOException {
        validateDirectories(projectRoot, false);
    }

    private static void validateDirectories(Path projectRoot, boolean requireAll) throws IOException {
        for (String relative : List.of("docs", OUTPUT_ROOT, API_DIRECTORY)) {
            Path directory = projectRoot.resolve(relative);
            if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
                if (requireAll) {
                    throw new IOException("Project documentation directory is missing: " + relative);
                }
                break;
            }
            if (Files.isSymbolicLink(directory)
                    || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Project documentation directory changed: " + relative);
            }
            Path realDirectory = directory.toRealPath();
            if (!realDirectory.startsWith(projectRoot)) {
                throw new IOException("Project documentation directory escapes the project: " + relative);
            }
        }
    }

    private static void checkDirectory(Path projectRoot, String relative, List<String> conflicts) {
        Path directory = projectRoot.resolve(relative);
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (Files.isSymbolicLink(directory) || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            conflicts.add(relative);
        }
    }

    private static void checkPageTarget(Path target,
                                        ProjectDocumentIdentity expectedIdentity,
                                        String relative,
                                        List<String> conflicts) throws IOException {
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (Files.isSymbolicLink(target) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            conflicts.add(relative);
            return;
        }
        if (Files.size(target) > (relative.equals(INDEX_PATH) ? MAX_INDEX_BYTES : MAX_PAGE_BYTES)) {
            conflicts.add(relative);
            return;
        }
        String content;
        try {
            content = StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(readBounded(target))).toString();
        } catch (CharacterCodingException invalidUtf8) {
            conflicts.add(relative);
            return;
        }
        Matcher marker = PAGE_MARKER.matcher(content);
        if (!marker.find() || !marker.group(1).equals(expectedIdentity.canonicalForm())) {
            conflicts.add(relative);
            return;
        }
        String body = content.substring(marker.end());
        if (!marker.group(2).equals(sha256(body.getBytes(StandardCharsets.UTF_8)))) {
            conflicts.add(relative);
        }
    }

    private static void checkIndexTarget(Path target, String relative, List<String> conflicts) throws IOException {
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (Files.isSymbolicLink(target) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            conflicts.add(relative);
            return;
        }
        if (Files.size(target) > (relative.equals(INDEX_PATH) ? MAX_INDEX_BYTES : MAX_PAGE_BYTES)) {
            conflicts.add(relative);
            return;
        }
        String content;
        try {
            content = StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(readBounded(target))).toString();
        } catch (CharacterCodingException invalidUtf8) {
            conflicts.add(relative);
            return;
        }
        Matcher marker = INDEX_MARKER.matcher(content);
        if (!marker.find()) {
            conflicts.add(relative);
            return;
        }
        String body = content.substring(marker.end());
        if (!marker.group(1).equals(sha256(body.getBytes(StandardCharsets.UTF_8)))) {
            conflicts.add(relative);
        }
    }

    private static void ensureDirectory(Path directory) throws IOException {
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
            try {
                Files.createDirectory(directory);
            } catch (FileAlreadyExistsException raced) {
                // Another IDE may have created the same directory; validate it below.
            }
        }
        if (Files.isSymbolicLink(directory) || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Project documentation path is not a regular directory: " + directory.getFileName());
        }
    }

    private static void atomicReplace(StagedFile file) throws IOException {
        try {
            Files.move(file.temporary(), file.target(), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            throw new IOException("Atomic replacement is unavailable for " + file.relativePath(), exception);
        }
    }

    private static List<String> cleanup(Path projectRoot, List<StagedFile> staged) {
        List<String> leftovers = new ArrayList<>();
        for (StagedFile file : staged) {
            try {
                validateOutputDirectories(projectRoot);
                Files.deleteIfExists(file.temporary());
            } catch (IOException ignored) {
                leftovers.add(relative(projectRoot, file.temporary()));
            }
        }
        return leftovers.stream().distinct().sorted().toList();
    }

    private static byte[] readBounded(Path path) throws IOException {
        long limit = path.getFileName().toString().startsWith("index.") ? MAX_INDEX_BYTES : MAX_PAGE_BYTES;
        try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = input.readNBytes((int) limit + 1);
            if (bytes.length > limit) {
                throw new IOException("Project document exceeds its size limit");
            }
            return bytes;
        }
    }

    private static String relative(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    private static String relativeHref(Path projectRoot, Path fromDirectory, String projectRelativeTarget) {
        Path target = projectRoot.resolve(projectRelativeTarget);
        String relative = fromDirectory.relativize(target).toString().replace('\\', '/');
        return percentEncodePath(relative);
    }

    private static String percentEncodePath(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder encoded = new StringBuilder(bytes.length);
        for (byte valueByte : bytes) {
            int valueUnsigned = valueByte & 0xff;
            if ((valueUnsigned >= 'a' && valueUnsigned <= 'z') || (valueUnsigned >= 'A' && valueUnsigned <= 'Z')
                    || (valueUnsigned >= '0' && valueUnsigned <= '9') || valueUnsigned == '-' || valueUnsigned == '_'
                    || valueUnsigned == '.' || valueUnsigned == '/' || valueUnsigned == '~') {
                encoded.append((char) valueUnsigned);
            } else {
                encoded.append('%').append(HexFormat.of().withUpperCase().toHexDigits((byte) valueUnsigned));
            }
        }
        return encoded.toString();
    }

    private static String escapeMarkdown(String value) {
        return value.replace("\\", "\\\\").replace("[", "\\[").replace("]", "\\]");
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private record StagedFile(Path target, Path temporary, byte[] content, String relativePath) {
    }

    interface ExportObserver {
        default void afterStaging(Path projectRoot) throws IOException {
        }

        default void beforeMove(String relativePath) throws IOException {
        }

        default void afterMove(String relativePath) throws IOException {
        }
    }

    private record TargetState(boolean exists, byte[] content) {
        private TargetState {
            content = content == null ? null : content.clone();
        }

        private static TargetState absent() {
            return new TargetState(false, null);
        }

        private static TargetState of(byte[] content) {
            return new TargetState(true, content);
        }

        private byte[] bytes() {
            return content == null ? null : content.clone();
        }

        private boolean matches(Path target) throws IOException {
            if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                return !exists;
            }
            return exists && !Files.isSymbolicLink(target)
                    && Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)
                    && MessageDigest.isEqual(content, readBounded(target));
        }
    }
}
