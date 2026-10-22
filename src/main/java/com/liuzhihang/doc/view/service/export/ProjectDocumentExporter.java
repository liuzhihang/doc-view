package com.liuzhihang.doc.view.service.export;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;

import java.io.IOException;
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
    private final ExportObserver observer;

    public ProjectDocumentExporter() {
        this(new ExportObserver() {
        });
    }

    ProjectDocumentExporter(ExportObserver observer) {
        this.observer = Objects.requireNonNull(observer, "observer");
    }

    public ProjectDocumentExportResult export(Path projectRoot, List<ProjectDocumentPage> pages) throws IOException {
        return export(projectRoot, pages, new EmptyProgressIndicator());
    }

    public ProjectDocumentExportResult export(Path projectRoot,
                                              List<ProjectDocumentPage> pages,
                                              ProgressIndicator indicator) throws IOException {
        return export(projectRoot, pages, indicator, () -> true);
    }

    public ProjectDocumentExportResult export(Path projectRoot,
                                              List<ProjectDocumentPage> pages,
                                              ProgressIndicator indicator,
                                              BooleanSupplier publicationAllowed) throws IOException {
        return publish(projectRoot, pages, indicator, publicationAllowed);
    }

    private ProjectDocumentExportResult publish(Path projectRoot,
                                                List<ProjectDocumentPage> pages,
                                                ProgressIndicator indicator,
                                                BooleanSupplier publicationAllowed) throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Objects.requireNonNull(pages, "pages");
        Objects.requireNonNull(indicator, "indicator");
        Objects.requireNonNull(publicationAllowed, "publicationAllowed");
        indicator.checkCanceled();
        if (!publicationAllowed.getAsBoolean()) {
            throw new ProcessCanceledException();
        }

        Path canonicalRoot = projectRoot.toRealPath();
        if (!Files.isDirectory(canonicalRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Project root is not a directory");
        }

        List<ProjectDocumentPage> sortedPages = normalizePages(pages);
        LinkedHashMap<String, byte[]> contents = new LinkedHashMap<>();
        for (ProjectDocumentPage page : sortedPages) {
            indicator.checkCanceled();
            String relativePath = API_DIRECTORY + "/" + page.identity().pageFileName();
            String body = pageBody(canonicalRoot, page, relativePath);
            contents.put(relativePath, markedContent("page", page.identity().canonicalForm(), body));
        }
        String indexBody = indexBody(canonicalRoot, sortedPages);
        contents.put(INDEX_PATH, markedIndex(indexBody));

        List<String> conflicts = preflight(canonicalRoot, sortedPages);
        if (!conflicts.isEmpty()) {
            throw new ProjectDocumentConflictException(conflicts);
        }

        Path docsPath = canonicalRoot.resolve("docs");
        Path outputRoot = canonicalRoot.resolve(OUTPUT_ROOT);
        Path apiDirectory = canonicalRoot.resolve(API_DIRECTORY);
        ensureDirectory(docsPath);
        validateExistingOutputDirectories(canonicalRoot);
        ensureDirectory(outputRoot);
        validateExistingOutputDirectories(canonicalRoot);
        ensureDirectory(apiDirectory);
        validateOutputDirectories(canonicalRoot);

        conflicts = preflight(canonicalRoot, sortedPages);
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
                indicator.checkCanceled();
                Path parent = entry.getKey().getParent();
                validateOutputDirectories(canonicalRoot);
                Path temporary = Files.createTempFile(parent, ".doc-view-", ".tmp");
                staged.add(new StagedFile(entry.getKey(), temporary, entry.getValue(), relative(canonicalRoot, entry.getKey())));
                Files.write(temporary, entry.getValue());
            }
            observer.afterStaging(canonicalRoot);
            indicator.checkCanceled();
            validateOutputDirectories(canonicalRoot);
            conflicts = changedTargets(canonicalRoot, expectedTargets);
            if (!conflicts.isEmpty()) {
                List<String> leftovers = cleanup(canonicalRoot, staged);
                if (!leftovers.isEmpty()) {
                    throw new ProjectDocumentTemporaryCleanupException(leftovers);
                }
                throw new ProjectDocumentConflictException(conflicts);
            }
            if (!publicationAllowed.getAsBoolean()) {
                throw new ProcessCanceledException();
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
            for (StagedFile file : staged) {
                if (file.target().equals(canonicalRoot.resolve(INDEX_PATH))) {
                    continue;
                }
                current = file;
                observer.beforeMove(file.relativePath());
                validateOutputDirectories(canonicalRoot);
                if (!expectedTargets.get(file.target()).matches(file.target())) {
                    throw new ProjectDocumentConflictException(List.of(file.relativePath()));
                }
                atomicReplace(file);
                published.add(file.relativePath());
                expectedTargets.put(file.target(), TargetState.of(file.content()));
            }
            for (StagedFile file : staged) {
                if (file.target().equals(canonicalRoot.resolve(INDEX_PATH))) {
                    current = file;
                    observer.beforeMove(file.relativePath());
                    validateOutputDirectories(canonicalRoot);
                    if (!expectedTargets.get(file.target()).matches(file.target())) {
                        throw new ProjectDocumentConflictException(List.of(file.relativePath()));
                    }
                    atomicReplace(file);
                    published.add(file.relativePath());
                    expectedTargets.put(file.target(), TargetState.of(file.content()));
                    break;
                }
            }
        } catch (IOException exception) {
            List<String> leftovers = cleanup(canonicalRoot, staged);
            if (published.isEmpty() && exception instanceof ProjectDocumentConflictException conflict) {
                if (!leftovers.isEmpty()) {
                    throw new ProjectDocumentTemporaryCleanupException(leftovers, conflict);
                }
                throw conflict;
            }
            throw new ProjectDocumentPublishException(exception, published,
                    current == null ? null : current.relativePath(), leftovers);
        }

        return new ProjectDocumentExportResult(sortedPages.size(), published, unchanged);
    }

    private static List<ProjectDocumentPage> normalizePages(List<ProjectDocumentPage> pages) {
        TreeMap<String, ProjectDocumentPage> byIdentity = new TreeMap<>();
        for (ProjectDocumentPage page : pages) {
            Objects.requireNonNull(page, "pages must not contain null");
            ProjectDocumentPage previous = byIdentity.putIfAbsent(page.identity().canonicalForm(), page);
            if (previous != null && !previous.markdown().equals(page.markdown())) {
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
        }
        checkIndexTarget(projectRoot.resolve(INDEX_PATH), INDEX_PATH, conflicts);
        return conflicts.stream().distinct().sorted().toList();
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
                states.put(target, TargetState.of(Files.readAllBytes(target)));
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
        String content;
        try {
            content = Files.readString(target, StandardCharsets.UTF_8);
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
        String content;
        try {
            content = Files.readString(target, StandardCharsets.UTF_8);
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
            Files.createDirectory(directory);
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
                    && MessageDigest.isEqual(content, Files.readAllBytes(target));
        }
    }
}
