package com.liuzhihang.doc.view.service.export;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.action.toolbar.window.ProjectDocumentExportAction;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.EntryData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.IndexData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentFormat;
import com.liuzhihang.doc.view.service.document.ProjectDocumentIndexService;
import com.liuzhihang.doc.view.testsupport.LocalProjectJavaSources;
import com.liuzhihang.doc.view.ui.window.DocViewToolWindowFactory;
import com.liuzhihang.doc.view.ui.window.DocViewWindowPanel;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises partial publication through the visible project-library failure and retry path. */
public final class ProjectDocumentPartialLibraryUiTest extends LightJavaCodeInsightFixtureTestCase {

    private static final String TOOL_WINDOW_ID = "Doc View";

    private LocalProjectJavaSources sources;
    private PsiClass controller;
    private String originalAsyncProperty;
    private Path projectRoot;
    private Path documentRoot;
    private ToolWindowManager toolWindowManager;
    private ToolWindow toolWindow;
    private boolean registeredToolWindow;
    private Content createdContent;
    private DocViewWindowPanel panel;

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_21;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        originalAsyncProperty = System.getProperty("intellij.progress.task.ignoreHeadless");
        System.setProperty("intellij.progress.task.ignoreHeadless", "true");
        projectRoot = Path.of(getProject().getBasePath()).toAbsolutePath().normalize();
        Files.createDirectories(projectRoot);
        projectRoot = projectRoot.toRealPath();
        documentRoot = projectRoot.resolve(ProjectDocumentIndexService.DOCUMENT_ROOT);
        deleteTree(documentRoot);
        sources = new LocalProjectJavaSources(getProject(), getModule());
        sources.installApiAnnotationStubs();
        controller = sources.addClass("projectdocs/library/GeneratedApiController.java", """
                package projectdocs.library;

                @org.springframework.web.bind.annotation.RestController
                @org.springframework.web.bind.annotation.RequestMapping("/api")
                public class GeneratedApiController {
                    /** Alpha original summary. */
                    @org.springframework.web.bind.annotation.GetMapping("/alpha")
                    public String alpha(@org.springframework.web.bind.annotation.RequestParam("id") String id) {
                        return id;
                    }

                    /** Beta original summary. */
                    @org.springframework.web.bind.annotation.GetMapping("/beta")
                    public String beta() {
                        return "beta";
                    }
                }
                """);
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());
        createToolWindowPanel();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            try {
                closeDocumentEditors();
            } finally {
                try {
                    disposeToolWindowPanel();
                } finally {
                    try {
                        deleteTree(documentRoot);
                    } finally {
                        if (sources != null) sources.cleanup();
                    }
                }
            }
        } finally {
            if (originalAsyncProperty == null) {
                System.clearProperty("intellij.progress.task.ignoreHeadless");
            } else {
                System.setProperty("intellij.progress.task.ignoreHeadless", originalAsyncProperty);
            }
            super.tearDown();
        }
    }

    public void testPartialPublicationFailsClosedAndRealExportActionRepairsAndOpensMarkdown() throws Exception {
        List<Notification> notifications = new CopyOnWriteArrayList<>();
        getProject().getMessageBus().connect(getTestRootDisposable()).subscribe(Notifications.TOPIC,
                new Notifications() {
                    @Override
                    public void notify(Notification notification) {
                        notifications.add(notification);
                    }
                });

        generateWithAction();
        await(() -> statusText().contains(DocViewBundle.message("project.library.status.current")),
                "the first complete export to become current");

        Path jsonIndex = documentRoot.resolve("index.json");
        IndexData committedBefore = ProjectDocumentFormat.readIndex(Files.readAllBytes(jsonIndex));
        EntryData firstApi = committedBefore.entries().get(0);
        String changedMethodName = firstApi.identity().methodName();
        Path changedMarkdown = documentRoot.resolve(firstApi.markdown().path());
        byte[] oldMarkdown = Files.readAllBytes(changedMarkdown);
        byte[] oldJsonIndex = Files.readAllBytes(jsonIndex);

        updateSummary(changedMethodName);
        ProjectDocumentSnapshot changedSnapshot = ReadAction.computeBlocking(() ->
                ProjectDocumentSnapshotService.getInstance(getProject()).createSnapshot(new EmptyProgressIndicator()));
        ProjectDocumentExporter partialExporter = new ProjectDocumentExporter(new ProjectDocumentExporter.ExportObserver() {
            private int moveCount;

            @Override
            public void beforeMove(String relativePath) throws IOException {
                if (++moveCount == 2) {
                    throw new IOException("controlled failure after the first real page move");
                }
            }
        });
        ProjectDocumentPublishException partial = null;
        try {
            partialExporter.export(projectRoot, changedSnapshot, new EmptyProgressIndicator());
            fail("The controlled second move must leave a real partial publication");
        } catch (ProjectDocumentPublishException expected) {
            partial = expected;
        }

        assertEquals("Exactly the first Markdown page must have reached its target", 1, partial.publishedPaths().size());
        String expectedFirstPublishedPage = "docs/doc-view/" + firstApi.markdown().path();
        assertTrue("The actual published paths were " + partial.publishedPaths()
                        + "; the first changed API page should be " + expectedFirstPublishedPage,
                partial.publishedPaths().contains(expectedFirstPublishedPage));
        assertTrue(partial.failedPath().startsWith("docs/doc-view/apis/"));
        assertTrue("The failure must occur before the JSON index commit point",
                Arrays.equals(oldJsonIndex, Files.readAllBytes(jsonIndex)));
        assertFalse("The first page must now differ from the still-committed file hash",
                Arrays.equals(oldMarkdown, Files.readAllBytes(changedMarkdown)));

        // The exporter exception above is real; reflection isolates only the action's error-message/notification mapping.
        notifications.clear();
        invokePartialFailureHandler(getProject(), partial);
        String expectedPartialMessage = DocViewBundle.message("project.export.partial.failed",
                String.join(", ", partial.publishedPaths()), partial.failedPath(),
                partial.temporaryPaths().isEmpty()
                        ? DocViewBundle.message("project.export.none")
                        : String.join(", ", partial.temporaryPaths()));
        await(() -> statusText().contains(expectedPartialMessage),
                "the actual partial exception details to appear in the tool-window status");
        await(() -> notifications.stream().anyMatch(notification ->
                        notification.getType() == NotificationType.ERROR
                                && expectedPartialMessage.equals(notification.getContent())),
                "the actual partial exception to produce an error notification");

        searchAndOpen(firstApi.markdown().path());
        await(() -> statusText().contains(DocViewBundle.message("project.library.status.inconsistent")),
                "opening the published page against the old index to fail closed");
        assertFalse("The inconsistent page must not open in the editor",
                Arrays.stream(com.intellij.openapi.fileEditor.FileEditorManager.getInstance(getProject()).getOpenFiles())
                        .anyMatch(file -> file.getPath().equals(changedMarkdown.toString())));

        EdtTestUtil.runInEdtAndWait(() -> ProjectDocumentExportAction.generateProject(getProject()));
        await(() -> {
            if (!Files.isRegularFile(jsonIndex)) return false;
            IndexData index = ProjectDocumentFormat.readIndex(Files.readAllBytes(jsonIndex));
            return !committedBefore.snapshotId().equals(index.snapshotId())
                    && statusText().contains(DocViewBundle.message("project.library.status.current"));
        }, "the real action retry to install the repaired snapshot");
        IndexData repaired = ProjectDocumentFormat.readIndex(Files.readAllBytes(jsonIndex));
        assertFalse("Retry must commit the changed source snapshot", committedBefore.snapshotId().equals(repaired.snapshotId()));
        EntryData repairedApi = entry(repaired, changedMethodName);
        assertNotNull(repairedApi);
        String repairedMarkdown = Files.readString(
                documentRoot.resolve(repairedApi.markdown().path()), StandardCharsets.UTF_8);
        String expectedUpdatedSummary = changedMethodName.equals("alpha")
                ? "Alphaupdatedsummary."
                : "Betaupdatedsummary.";
        assertTrue("The repaired Markdown for " + changedMethodName + " should contain the updated summary "
                        + expectedUpdatedSummary + "; actual Markdown:\n" + repairedMarkdown,
                repairedMarkdown.contains(expectedUpdatedSummary));

        searchAndOpen(repairedApi.markdown().path());
        String openedMessage = DocViewBundle.message("project.library.opened", repairedApi.markdown().path());
        await(() -> statusText().contains(openedMessage), "the repaired Markdown page to open from the library");
        assertTrue("The retry must open the verified on-disk Markdown file",
                Arrays.stream(com.intellij.openapi.fileEditor.FileEditorManager.getInstance(getProject()).getOpenFiles())
                        .anyMatch(file -> file.getPath().equals(documentRoot.resolve(repairedApi.markdown().path()).toString())));
    }

    private void generateWithAction() throws Exception {
        EdtTestUtil.runInEdtAndWait(() -> ProjectDocumentExportAction.generateProject(getProject()));
        await(() -> Files.isRegularFile(documentRoot.resolve("index.json"))
                        && statusText().contains(DocViewBundle.message("project.library.status.current")),
                "the explicit project-export action to finish");
    }

    private void updateSummary(String methodName) {
        Document document = PsiDocumentManager.getInstance(getProject()).getDocument(controller.getContainingFile());
        assertNotNull(document);
        String oldText = document.getText();
        String originalSummary = methodName.equals("alpha") ? "Alpha original summary." : "Beta original summary.";
        String updatedSummary = methodName.equals("alpha") ? "Alpha updated summary." : "Beta updated summary.";
        assertTrue("The selected indexed method must have an editable source summary: " + methodName,
                oldText.contains(originalSummary));
        ApplicationManager.getApplication().runWriteAction(() ->
                document.setText(oldText.replace(originalSummary, updatedSummary)));
        PsiDocumentManager.getInstance(getProject()).commitDocument(document);
    }

    private void searchAndOpen(String relativeMarkdownPath) throws Exception {
        EdtTestUtil.runInEdtAndWait(() -> panel.showProjectSearch());
        await(() -> resultIndex(relativeMarkdownPath) >= 0, "the actual project search to return the selected API");
        EdtTestUtil.runInEdtAndWait(() -> {
            JList<?> results = findComponent(panel, JList.class, ignored -> true);
            assertNotNull(results);
            results.setSelectedIndex(resultIndexOnEdt(relativeMarkdownPath));
            JButton openButton = findButton(DocViewBundle.message("project.library.open.markdown"));
            assertNotNull("The real search view must expose Open Markdown", openButton);
            assertTrue("The selected API result must enable Open Markdown", openButton.isEnabled());
            openButton.doClick();
        });
    }

    private void createToolWindowPanel() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        EdtTestUtil.runInEdtAndWait(() -> {
            try {
                toolWindowManager = ToolWindowManager.getInstance(getProject());
                toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
                if (toolWindow == null) {
                    toolWindow = toolWindowManager.registerToolWindow(TOOL_WINDOW_ID, false, ToolWindowAnchor.RIGHT);
                    registeredToolWindow = true;
                }
                for (Content content : toolWindow.getContentManager().getContents()) {
                    if (content.getComponent() instanceof DocViewWindowPanel existing) {
                        panel = existing;
                        return;
                    }
                }
                int oldCount = toolWindow.getContentManager().getContentCount();
                new DocViewToolWindowFactory().createToolWindowContent(getProject(), toolWindow);
                for (Content content : toolWindow.getContentManager().getContents()) {
                    if (content.getComponent() instanceof DocViewWindowPanel created) {
                        panel = created;
                        if (toolWindow.getContentManager().getContentCount() > oldCount) createdContent = content;
                        break;
                    }
                }
                if (panel == null) throw new AssertionError("The registered Doc View tool window must contain its panel");
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        });
        if (failure.get() != null) throw new AssertionError("Could not prepare the real Doc View tool window", failure.get());
    }

    private void disposeToolWindowPanel() throws Exception {
        if (toolWindowManager == null) return;
        EdtTestUtil.runInEdtAndWait(() -> {
            if (createdContent != null && toolWindow != null) {
                ContentManager manager = toolWindow.getContentManager();
                if (manager.getIndexOfContent(createdContent) >= 0) manager.removeContent(createdContent, true);
                if (createdContent.getDisposer() instanceof Disposable disposable && !Disposer.isDisposed(disposable)) {
                    Disposer.dispose(disposable);
                }
            }
            if (registeredToolWindow) toolWindowManager.unregisterToolWindow(TOOL_WINDOW_ID);
        });
    }

    private void closeDocumentEditors() throws Exception {
        if (documentRoot == null || getProject().isDisposed()) return;
        EdtTestUtil.runInEdtAndWait(() -> {
            FileEditorManager editors = FileEditorManager.getInstance(getProject());
            for (VirtualFile file : editors.getOpenFiles()) {
                Path filePath = Path.of(file.getPath()).toAbsolutePath().normalize();
                if (filePath.startsWith(documentRoot)) editors.closeFile(file);
            }
        });
    }

    private static void invokePartialFailureHandler(Project project, ProjectDocumentPublishException failure) throws Exception {
        Method handler = ProjectDocumentExportAction.class.getDeclaredMethod("reportFailure", Project.class, Throwable.class);
        handler.setAccessible(true);
        try {
            handler.invoke(null, project, failure);
        } catch (InvocationTargetException wrapped) {
            Throwable cause = wrapped.getCause();
            if (cause instanceof Exception exception) throw exception;
            if (cause instanceof Error error) throw error;
            throw new AssertionError(cause);
        }
    }

    private int resultIndex(String relativeMarkdownPath) {
        int[] index = {-1};
        Runnable read = () -> index[0] = resultIndexOnEdt(relativeMarkdownPath);
        if (EventQueue.isDispatchThread()) read.run();
        else EdtTestUtil.runInEdtAndWait(read::run);
        return index[0];
    }

    private int resultIndexOnEdt(String relativeMarkdownPath) {
        JList<?> results = findComponent(panel, JList.class, ignored -> true);
        if (results == null) return -1;
        for (int i = 0; i < results.getModel().getSize(); i++) {
            Object item = results.getModel().getElementAt(i);
            if (item instanceof EntryData entry && entry.markdown().path().equals(relativeMarkdownPath)) return i;
        }
        return -1;
    }

    private String statusText() {
        String[] text = {""};
        Runnable read = () -> {
            List<String> labels = findComponents(panel, JLabel.class, JLabel::isVisible).stream()
                    .map(JLabel::getText).filter(value -> value != null && !value.isBlank()).toList();
            text[0] = String.join("\n", labels);
        };
        if (EventQueue.isDispatchThread()) read.run();
        else EdtTestUtil.runInEdtAndWait(read::run);
        return text[0];
    }

    private JButton findButton(String text) {
        return findComponent(panel, JButton.class, button -> text.equals(button.getText()));
    }

    private void await(Check check, String expectation) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime() < deadline) {
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            if (check.test()) return;
            Thread.sleep(25);
        }
        fail("Timed out waiting for " + expectation + "; visible status was: " + statusText());
    }

    private static EntryData entry(IndexData index, String methodName) {
        return index.entries().stream().filter(candidate -> methodName.equals(candidate.identity().methodName()))
                .findFirst().orElse(null);
    }

    private static <T extends Component> T findComponent(Container root, Class<T> type,
                                                          java.util.function.Predicate<T> predicate) {
        for (T component : findComponents(root, type, predicate)) return component;
        return null;
    }

    private static <T extends Component> List<T> findComponents(Container root, Class<T> type,
                                                                  java.util.function.Predicate<T> predicate) {
        List<T> matches = new java.util.ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                T typed = type.cast(child);
                if (predicate.test(typed)) matches.add(typed);
            }
            if (child instanceof Container container) matches.addAll(findComponents(container, type, predicate));
        }
        return matches;
    }

    private static void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    @FunctionalInterface
    private interface Check {
        boolean test() throws Exception;
    }
}
