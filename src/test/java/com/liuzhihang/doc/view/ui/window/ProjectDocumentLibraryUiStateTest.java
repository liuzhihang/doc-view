package com.liuzhihang.doc.view.ui.window;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.ApiData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.EntryData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.ExamplesData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.FileData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.IdentityData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.IndexData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.SourceData;
import com.liuzhihang.doc.view.service.document.DocumentStoreLocks;
import com.liuzhihang.doc.view.service.document.ProjectDocumentFormat;
import com.liuzhihang.doc.view.service.document.ProjectDocumentIndexService;
import com.liuzhihang.doc.view.service.export.ProjectDocumentIdentity;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Drives the project-document UI through visible controls and public index-service events. */
public final class ProjectDocumentLibraryUiStateTest extends LightJavaCodeInsightFixtureTestCase {

    private String oldAsyncProperty;
    private Path documentRoot;
    private ProjectDocumentIndexService indexService;
    private DocViewWindowPanel panel;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Exercise the production asynchronous scheduling used by Task.Backgroundable.
        oldAsyncProperty = System.getProperty("intellij.progress.task.ignoreHeadless");
        System.setProperty("intellij.progress.task.ignoreHeadless", "true");
        Path projectRoot = Path.of(getProject().getBasePath()).toAbsolutePath().normalize();
        Files.createDirectories(projectRoot);
        projectRoot = projectRoot.toRealPath();
        documentRoot = projectRoot.resolve(ProjectDocumentIndexService.DOCUMENT_ROOT);
        deleteTree(documentRoot);
        indexService = ProjectDocumentIndexService.getInstance(getProject());
        indexService.clearDerivedCache();
        ToolWindow toolWindow = (ToolWindow) Proxy.newProxyInstance(ToolWindow.class.getClassLoader(),
                new Class<?>[]{ToolWindow.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("getProject")) return getProject();
                    if (method.getName().equals("isDisposed")) return false;
                    if (method.getName().equals("toString")) return "Project document UI state test window";
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == long.class) return 0L;
                    return null;
                });
        panel = new DocViewWindowPanel(getProject(), toolWindow);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (documentRoot != null) {
                EdtTestUtil.runInEdtAndWait(() -> {
                    var editors = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(getProject());
                    for (var file : editors.getOpenFiles()) {
                        if (file.isInLocalFileSystem() && file.toNioPath().startsWith(documentRoot)) {
                            editors.closeFile(file);
                        }
                    }
                });
            }
            if (panel instanceof Disposable disposable && !Disposer.isDisposed(disposable)) {
                Disposer.dispose(disposable);
            }
            deleteTree(documentRoot);
        } finally {
            if (oldAsyncProperty == null) {
                System.clearProperty("intellij.progress.task.ignoreHeadless");
            } else {
                System.setProperty("intellij.progress.task.ignoreHeadless", oldAsyncProperty);
            }
            super.tearDown();
        }
    }

    public void testMissingIndexSearchIsReadOnlyAndSearchCanShowNoResults() throws Exception {
        awaitStatus(DocViewBundle.message("project.library.status.not.generated"));
        assertFalse("Constructing the panel and reading status must not create the document store",
                Files.exists(documentRoot));

        EdtTestUtil.runInEdtAndWait(() -> panel.showProjectSearch());
        awaitStatus(DocViewBundle.message("project.library.status.not.generated"));
        PlatformTestUtil.waitForAllBackgroundActivityToCalmDown();
        assertFalse("Searching without a committed index must not generate project documentation",
                Files.exists(documentRoot));

        IndexFixture fixture = writeStore("visible search result");
        byte[] committedIndex = Files.readAllBytes(documentRoot.resolve("index.json"));
        indexService.clearDerivedCache();
        EdtTestUtil.runInEdtAndWait(() -> panel.showProjectSearch());
        await(() -> listSize() == 1, "the real search result should appear in the panel");

        EdtTestUtil.runInEdtAndWait(() -> {
            JTextField searchField = findComponent(panel, JTextField.class,
                    field -> field.getClientProperty("JTextField.placeholderText") != null);
            assertNotNull("The visible search field must exist", searchField);
            searchField.setText("query-with-no-matching-entry");
            JButton searchButton = findButton(DocViewBundle.message("project.library.search"));
            assertNotNull("The visible Search button must exist", searchButton);
            searchButton.doClick();
        });
        awaitStatus(DocViewBundle.message("project.library.search.no.results"));
        assertEquals("A no-result search must leave the visible result list empty", 0, listSize());
        assertTrue("Read-only search must preserve the committed on-disk index",
                java.util.Arrays.equals(committedIndex, Files.readAllBytes(documentRoot.resolve("index.json"))));
        assertEquals("The read-only query must retain the installed snapshot", fixture.index().snapshotId(),
                ProjectDocumentFormat.readIndex(Files.readAllBytes(documentRoot.resolve("index.json"))).snapshotId());
    }

    public void testIndexCommitEventRefreshesUnknownCurrentAndStalePanelStates() throws Exception {
        IndexFixture fixture = writeStore("state transition fixture");
        indexService.clearDerivedCache();
        awaitStatus(DocViewBundle.message("project.library.status.unknown"));

        long capturedEpoch = indexService.captureInputEpoch();
        indexService.installCommittedIndex(fixture.index(), capturedEpoch);
        awaitStatus(DocViewBundle.message("project.library.status.current"));

        indexService.markObservedInputsChanged();
        awaitStatus(DocViewBundle.message("project.library.status.stale"));
    }

    public void testExternallyChangedMarkdownIsRejectedByTheVisibleOpenAction() throws Exception {
        IndexFixture fixture = writeStore("tamper protection fixture");
        indexService.installCommittedIndex(fixture.index(), indexService.captureInputEpoch());
        awaitStatus(DocViewBundle.message("project.library.status.current"));
        EdtTestUtil.runInEdtAndWait(() -> panel.showProjectSearch());
        await(() -> listSize() == 1, "the API must be found by the actual project search");

        byte[] originalMarkdown = Files.readAllBytes(fixture.markdownPath());
        Files.writeString(fixture.markdownPath(), "# changed outside Doc View\n", StandardCharsets.UTF_8);
        EdtTestUtil.runInEdtAndWait(() -> {
            JList<?> results = findComponent(panel, JList.class, ignored -> true);
            assertNotNull("The search result list must be visible", results);
            results.setSelectedIndex(0);
            JButton openButton = findButton(DocViewBundle.message("project.library.open.markdown"));
            assertNotNull("The visible Open Markdown action must exist", openButton);
            assertTrue("Selecting a real result must enable Open Markdown", openButton.isEnabled());
            openButton.doClick();
        });

        awaitStatus(DocViewBundle.message("project.library.status.inconsistent"));
        assertFalse("An externally changed Markdown file must not be opened as a verified result",
                java.util.Arrays.stream(com.intellij.openapi.fileEditor.FileEditorManager.getInstance(getProject())
                        .getOpenFiles()).anyMatch(file -> file.getPath().endsWith(fixture.relativeMarkdownPath())));

        Files.write(fixture.markdownPath(), originalMarkdown);
        EdtTestUtil.runInEdtAndWait(() -> {
            JButton openButton = findButton(DocViewBundle.message("project.library.open.markdown"));
            assertNotNull(openButton);
            assertTrue("The selected result must allow revalidation after external repair", openButton.isEnabled());
            openButton.doClick();
        });
        await(() -> java.util.Arrays.stream(
                        com.intellij.openapi.fileEditor.FileEditorManager.getInstance(getProject()).getOpenFiles())
                .anyMatch(file -> file.getPath().endsWith(fixture.relativeMarkdownPath())),
                "the restored and revalidated Markdown must open");
        awaitStatus(DocViewBundle.message("project.library.status.current"));
        assertTrue("Successful recovery must remain a read-only operation",
                java.util.Arrays.equals(originalMarkdown, Files.readAllBytes(fixture.markdownPath())));
    }

    public void testBusyReadSlotsShowVisibleRetryFeedback() throws Exception {
        IndexFixture fixture = writeStore("busy feedback fixture");
        indexService.installCommittedIndex(fixture.index(), indexService.captureInputEpoch());
        awaitStatus(DocViewBundle.message("project.library.status.current"));

        CountDownLatch readersWaitingOnWriter = new CountDownLatch(4);
        List<CompletableFuture<?>> readers = new ArrayList<>();
        List<Thread> readerThreads = new ArrayList<>();
        DocumentStoreLocks.Lease writer = DocumentStoreLocks.tryAcquireWrite(
                Path.of(getProject().getBasePath()), new EmptyProgressIndicator(), java.time.Duration.ofSeconds(1));
        assertNotNull("The test must hold the real document-store writer lock", writer);
        try {
            for (int i = 0; i < 4; i++) {
                CompletableFuture<Object> read = new CompletableFuture<>();
                readers.add(read);
                ProgressIndicator indicator = waitingIndicator(readersWaitingOnWriter);
                Thread readerThread = new Thread(() -> {
                    try {
                        read.complete(indexService.status(indicator));
                    } catch (Throwable failure) {
                        read.completeExceptionally(failure);
                    }
                }, "doc-view-ui-busy-reader-" + i);
                readerThread.setDaemon(true);
                readerThreads.add(readerThread);
                readerThread.start();
            }
            awaitLatch(readersWaitingOnWriter, "four real status reads should wait on the held writer lock");
            EdtTestUtil.runInEdtAndWait(() -> panel.showProjectSearch());
            awaitStatus(DocViewBundle.message("project.export.already.running"));
            assertEquals("A rejected busy search must not display stale or partial results", 0, listSize());
        } finally {
            writer.close();
        }

        for (CompletableFuture<?> reader : readers) PlatformTestUtil.waitForFuture(reader, 10_000);
        for (Thread readerThread : readerThreads) readerThread.join(2_000);
        assertEquals("Busy feedback must leave the committed snapshot unchanged", fixture.index().snapshotId(),
                ProjectDocumentFormat.readIndex(Files.readAllBytes(documentRoot.resolve("index.json"))).snapshotId());
    }

    public void testBackToCatalogCancelsPendingSearchAndLaterSearchCanRecover() throws Exception {
        IndexFixture fixture = writeStore("cancel and retry fixture");
        indexService.installCommittedIndex(fixture.index(), indexService.captureInputEpoch());
        awaitStatus(DocViewBundle.message("project.library.status.current"));
        byte[] committedIndex = Files.readAllBytes(documentRoot.resolve("index.json"));

        DocumentStoreLocks.Lease writer = DocumentStoreLocks.tryAcquireWrite(
                Path.of(getProject().getBasePath()), new EmptyProgressIndicator(), java.time.Duration.ofSeconds(1));
        assertNotNull("The test must hold the real document-store writer lock", writer);
        try {
            EdtTestUtil.runInEdtAndWait(() -> panel.showProjectSearch());
            awaitWithoutSettlingBackground(
                    () -> statusText().contains(DocViewBundle.message("project.library.search.pending")),
                    "the pending search message should appear while the writer lock is held");
            awaitWithoutSettlingBackground(this::hasThreadWaitingForDocumentLock,
                    "the actual panel search worker should reach the document-store read lock");
            EdtTestUtil.runInEdtAndWait(() -> panel.showCatalog());
        } finally {
            writer.close();
        }

        PlatformTestUtil.waitForAllBackgroundActivityToCalmDown();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertEquals("Leaving search while it is pending must discard its late result", 0, listSize());
        assertFalse("The canceled request must not replace the catalog with a search result state",
                statusText().contains(DocViewBundle.message("project.library.search.ready", 1, 1)));

        EdtTestUtil.runInEdtAndWait(() -> panel.showProjectSearch());
        await(() -> listSize() == 1, "a fresh search should recover after the writer is released");
        awaitStatus(DocViewBundle.message("project.library.search.ready", 1, 1));
        assertTrue("Canceling a read must not alter committed files",
                java.util.Arrays.equals(committedIndex, Files.readAllBytes(documentRoot.resolve("index.json"))));
    }

    private void awaitStatus(String expectedText) throws Exception {
        await(() -> statusText().contains(expectedText),
                "visible project-document status should contain: " + expectedText);
    }

    private void await(Check check, String message) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
        while (System.nanoTime() < deadline) {
            PlatformTestUtil.waitForAllBackgroundActivityToCalmDown();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            if (check.test()) return;
            Thread.sleep(40);
        }
        fail("Timed out waiting until " + message + "; visible status was: " + statusText());
    }

    private void awaitWithoutSettlingBackground(Check check, String message) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (System.nanoTime() < deadline) {
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            if (check.test()) return;
            Thread.sleep(20);
        }
        fail("Timed out waiting until " + message + "; visible status was: " + statusText());
    }

    private void awaitLatch(CountDownLatch latch, String message) throws Exception {
        CompletableFuture<Void> ready = new CompletableFuture<>();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                if (!latch.await(8, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting until " + message);
                }
                ready.complete(null);
            } catch (Throwable failure) {
                ready.completeExceptionally(failure);
            }
        });
        PlatformTestUtil.waitForFuture(ready, 10_000);
    }

    private static ProgressIndicator waitingIndicator(CountDownLatch waitingReaders) {
        EmptyProgressIndicator delegate = new EmptyProgressIndicator();
        AtomicInteger cancellationChecks = new AtomicInteger();
        AtomicBoolean reportedWaiting = new AtomicBoolean();
        return (ProgressIndicator) Proxy.newProxyInstance(ProgressIndicator.class.getClassLoader(),
                new Class<?>[]{ProgressIndicator.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("checkCanceled") && cancellationChecks.incrementAndGet() >= 8
                            && reportedWaiting.compareAndSet(false, true)) {
                        waitingReaders.countDown();
                    }
                    try {
                        return method.invoke(delegate, arguments);
                    } catch (InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                });
    }

    private boolean hasThreadWaitingForDocumentLock() {
        return Thread.getAllStackTraces().values().stream().anyMatch(stack -> {
            boolean documentRead = false;
            boolean waitingLock = false;
            for (StackTraceElement frame : stack) {
                documentRead |= frame.getClassName().equals(ProjectDocumentIndexService.class.getName())
                        && (frame.getMethodName().equals("search") || frame.getMethodName().equals("status"));
                waitingLock |= frame.getClassName().equals(DocumentStoreLocks.class.getName())
                        && frame.getMethodName().equals("tryAcquire");
            }
            return documentRead && waitingLock;
        });
    }

    private String statusText() {
        String[] visibleText = {""};
        Runnable read = () -> {
            List<String> texts = new ArrayList<>();
            for (JLabel label : findComponents(panel, JLabel.class, label -> true)) {
                String text = label.getText();
                if (text != null && !text.isBlank() && label.isVisible()) texts.add(text);
            }
            visibleText[0] = String.join("\n", texts);
        };
        if (EventQueue.isDispatchThread()) read.run();
        else EdtTestUtil.runInEdtAndWait(read::run);
        return visibleText[0];
    }

    private int listSize() {
        int[] size = {-1};
        Runnable read = () -> {
            JList<?> list = findComponent(panel, JList.class, ignored -> true);
            size[0] = list == null ? -1 : list.getModel().getSize();
        };
        if (EventQueue.isDispatchThread()) read.run();
        else EdtTestUtil.runInEdtAndWait(read::run);
        return size[0];
    }

    private JButton findButton(String text) {
        return findComponent(panel, JButton.class, button -> text.equals(button.getText()));
    }

    private static <T extends Component> T findComponent(Container root, Class<T> type,
                                                           java.util.function.Predicate<T> predicate) {
        for (T component : findComponents(root, type, predicate)) return component;
        return null;
    }

    private static <T extends Component> List<T> findComponents(Container root, Class<T> type,
                                                                  java.util.function.Predicate<T> predicate) {
        List<T> matches = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                T typed = type.cast(child);
                if (predicate.test(typed)) matches.add(typed);
            }
            if (child instanceof Container container) matches.addAll(findComponents(container, type, predicate));
        }
        return matches;
    }

    private IndexFixture writeStore(String description) throws Exception {
        ProjectDocumentIdentity identity = ProjectDocumentIdentity.of("ui-test",
                "src/main/java/example/LibraryApi.java", "example.LibraryApi", "find", List.of());
        IdentityData identityData = new IdentityData(identity.moduleName(), identity.qualifiedClassName(),
                identity.methodName(), identity.parameterTypes(), identity.displaySignature(), identity.canonicalForm());
        SourceData source = new SourceData(identity.sourcePath(), 1,
                ProjectDocumentFormat.sha256("fictional source".getBytes(StandardCharsets.UTF_8)),
                "ide-document-text");
        ApiData api = new ApiData(identity.sha256(), identityData, "Library API", description, "spring", "GET",
                "/library", List.of("ui-test"), source, List.of(), List.of(), null, null,
                new ExamplesData(null, null), "");
        byte[] apiBytes = ProjectDocumentFormat.writeApi(api);
        String relativeMarkdownPath = "apis/" + identity.pageFileName();
        String relativeDetailPath = relativeMarkdownPath.substring(0, relativeMarkdownPath.length() - 3) + ".json";
        byte[] markdownBytes = ("# Library API\n\n" + description + "\n").getBytes(StandardCharsets.UTF_8);
        Path markdownPath = documentRoot.resolve(relativeMarkdownPath);
        Files.createDirectories(markdownPath.getParent());
        Files.write(markdownPath, markdownBytes);
        Files.write(documentRoot.resolve(relativeDetailPath), apiBytes);
        EntryData entry = new EntryData(identity.sha256(), identityData, "Library API", description, "spring", "GET",
                "/library", List.of("ui-test"), source,
                new FileData(relativeMarkdownPath, ProjectDocumentFormat.sha256(markdownBytes)),
                new FileData(relativeDetailPath, ProjectDocumentFormat.sha256(apiBytes)));
        byte[] indexMarkdown = "# Project API documents\n".getBytes(StandardCharsets.UTF_8);
        IndexData unsigned = new IndexData("0".repeat(64), "2026-10-24T00:00:00Z", "test", "1".repeat(64), false,
                new FileData("index.md", ProjectDocumentFormat.sha256(indexMarkdown)), List.of(entry));
        IndexData index = new IndexData(ProjectDocumentFormat.computeSnapshotId(unsigned), unsigned.generatedAt(),
                unsigned.producerVersion(), unsigned.generatorFingerprint(), unsigned.hasUnsavedDocuments(),
                unsigned.markdownIndex(), unsigned.entries());
        Files.write(documentRoot.resolve("index.md"), indexMarkdown);
        Files.write(documentRoot.resolve("index.json"), ProjectDocumentFormat.writeIndex(index));
        return new IndexFixture(index, markdownPath, relativeMarkdownPath);
    }

    private static void deleteTree(Path root) throws Exception {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    @FunctionalInterface
    private interface Check {
        boolean test() throws Exception;
    }

    private record IndexFixture(IndexData index, Path markdownPath, String relativeMarkdownPath) {
    }
}
