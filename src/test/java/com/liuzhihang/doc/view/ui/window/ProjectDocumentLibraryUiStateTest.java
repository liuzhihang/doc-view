package com.liuzhihang.doc.view.ui.window;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBTextField;
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
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.DefaultKeyboardFocusManager;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
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
            JTextField searchField = queryField();
            assertNotNull("The visible search field must exist", searchField);
            searchField.setText("query-with-no-matching-entry");
        });
        awaitStatus(DocViewBundle.message("project.library.search.no.results"));
        assertEquals("A no-result search must leave the visible result list empty", 0, listSize());
        EdtTestUtil.runInEdtAndWait(() -> queryField().setText(""));
        await(() -> listSize() == 1, "clearing the live filter should restore the first page");
        assertTrue("Read-only search must preserve the committed on-disk index",
                java.util.Arrays.equals(committedIndex, Files.readAllBytes(documentRoot.resolve("index.json"))));
        assertEquals("The read-only query must retain the installed snapshot", fixture.index().snapshotId(),
                ProjectDocumentFormat.readIndex(Files.readAllBytes(documentRoot.resolve("index.json"))).snapshotId());
    }

    public void testHighlightsLatestTypedTermsAndExplainsSourceOnlyMatches() throws Exception {
        IndexFixture fixture = writeStore("A <literal> endpoint");
        indexService.installCommittedIndex(fixture.index(), indexService.captureInputEpoch());
        awaitStatus(DocViewBundle.message("project.library.status.current"));
        EdtTestUtil.runInEdtAndWait(() -> panel.showProjectSearch());
        await(() -> listSize() == 1, "initial page must be visible");
        EdtTestUtil.runInEdtAndWait(() -> {
            JTextField field = queryField();
            JList<?> list = findComponent(panel, JList.class, ignored -> true);
            assertTrue("An empty query must not mark every result as a hit", highlightFragments(render(list, false)).isEmpty());
            field.setText("superseded");
            field.setText("lIbRaRy   <literal>  src/main audit-tag");
        });
        awaitStatus(DocViewBundle.message("project.library.search.ready", 1, 1));
        EdtTestUtil.runInEdtAndWait(() -> {
            String count = DocViewBundle.message("project.library.search.ready", 1, 1);
            JLabel status = findComponent(panel, JLabel.class, label -> label.getText() != null && label.getText().contains(count));
            assertNotNull(status);
            String total = DocViewBundle.message("project.library.status.entries", 1);
            assertTrue("A narrow search card must show the match count before snapshot details", status.getText().startsWith(count));
            assertFalse("The project total should not crowd the search feedback", status.getText().contains(total));
            assertEquals(count + " · " + DocViewBundle.message("project.library.search.freshness.current"), status.getText());
            assertTrue(status.getToolTipText().contains(total));
            assertTrue(status.getToolTipText().contains(DocViewBundle.message("project.library.status.current")));
            assertEquals(status.getToolTipText(), status.getAccessibleContext().getAccessibleDescription());
            JList<?> list = findComponent(panel, JList.class, ignored -> true);
            for (boolean selected : new boolean[]{false, true}) {
                Component rendered = render(list, selected);
                List<String> hits = highlightFragments(rendered);
                String marked = String.join(" ", hits).toLowerCase(java.util.Locale.ROOT);
                for (String term : List.of("library", "<literal>", "src/main", "audit-tag")) {
                    assertTrue("Missing literal hit " + term + " in " + hits + ", selected=" + selected,
                            marked.contains(term));
                }
                assertTrue(rendered.getAccessibleContext().getAccessibleName().contains("find("));
            }
        });
    }

    public void testLongDescriptionShowsLateLiteralHitAndPreservesItsOriginalSpelling() throws Exception {
        IndexFixture fixture = writeStore("Prefix ".repeat(900) + "Ｆｉｎａｌ <em>needle</em>");
        indexService.installCommittedIndex(fixture.index(), indexService.captureInputEpoch());
        awaitStatus(DocViewBundle.message("project.library.status.current"));
        EdtTestUtil.runInEdtAndWait(() -> panel.showProjectSearch());
        await(() -> listSize() == 1, "initial page must be visible");
        EdtTestUtil.runInEdtAndWait(() -> {
            queryField().setText("\u0085final needle");
        });
        awaitStatus(DocViewBundle.message("project.library.search.ready", 1, 1));
        EdtTestUtil.runInEdtAndWait(() -> {
            JList<?> list = findComponent(panel, JList.class, ignored -> true);
            Component rendered = render(list, false);
            String visible = findComponents((Container) rendered, SimpleColoredComponent.class, Component::isVisible)
                    .stream().map(line -> line.getCharSequence(false).toString())
                    .collect(java.util.stream.Collectors.joining(" "));
            assertTrue("The useful end of the description should be in the snippet", visible.contains("needle"));
            assertTrue("The snippet should render the markup as literal text", visible.contains("<em>needle</em>"));
            assertTrue("Long descriptions must not produce a massive horizontal scroll range", visible.length() < 1024);
            assertTrue("Rendering must preserve the original spelling, including width variants", visible.contains("Ｆｉｎａｌ"));
            assertTrue(highlightFragments(rendered).contains("needle"));
            assertTrue("A late hit belongs near the start of its own visible line", findComponents((Container) rendered,
                    SimpleColoredComponent.class, Component::isVisible).stream()
                    .map(line -> line.getCharSequence(false).toString())
                    .anyMatch(line -> line.contains("needle") && line.indexOf("needle") < 48));
        });
    }

    private List<String> highlightFragments(Component rendered) {
        List<String> hits = new ArrayList<>();
        for (var line : findComponents((Container) rendered, SimpleColoredComponent.class,
                Component::isVisible)) {
            var parts = line.iterator();
            while (parts.hasNext()) {
                String original = parts.next();
                int style = parts.getTextAttributes().getStyle();
                if ((style & (SimpleTextAttributes.STYLE_UNDERLINE
                        | SimpleTextAttributes.STYLE_SEARCH_MATCH)) != 0) hits.add(original);
            }
        }
        return hits;
    }

    private static <T> Component render(JList<T> list, boolean selected) {
        return list.getCellRenderer().getListCellRendererComponent(list, list.getModel().getElementAt(0),
                0, selected, selected);
    }

    public void testDownFromSearchSelectsResultWithoutOpeningOrRequerying() throws Exception {
        IndexFixture fixture = writeStore("keyboard target");
        indexService.installCommittedIndex(fixture.index(), indexService.captureInputEpoch());
        awaitStatus(DocViewBundle.message("project.library.status.current"));
        EdtTestUtil.runInEdtAndWait(() -> panel.showProjectSearch());
        await(() -> listSize() == 1, "initial search should complete");
        EdtTestUtil.runInEdtAndWait(() -> {
            pressDown(queryField());
            JList<?> list = findComponent(panel, JList.class, ignored -> true);
            assertEquals(0, list.getSelectedIndex());
            assertEquals(1, list.getModel().getSize());
            assertTrue(findButton(DocViewBundle.message("project.library.open.markdown")).isEnabled());
        });
    }

    public void testDownDuringFirstPageLoadSelectsTheFirstResultOnceItArrives() throws Exception {
        prepareKeyboardSearch();
        withSearchFieldFocus(query -> {
            query.setText("keyboard");
            pressDown(query);
            pressDown(query);
            assertEquals("The previous query's rows must not be presented as matches for the new query", 0, listSize());
            assertEquals(-1, searchList().getSelectedIndex());
            assertTrue(statusText().contains(DocViewBundle.message("project.library.search.pending")));
        }, () -> {
            awaitStatus(DocViewBundle.message("project.library.search.ready", 1, 1));
            assertEquals("One pending navigation enters the first result", 0, searchList().getSelectedIndex());
            assertTrue(findButton(DocViewBundle.message("project.library.open.markdown")).isEnabled());
        });
    }

    public void testDownDoesNotSurviveEditingFocusLossOrAnEmptyPage() throws Exception {
        prepareKeyboardSearch();
        withSearchFieldFocus(query -> {
            query.setText("keyboard");
            pressDown(query);
            query.setText("target");
        }, () -> {
            awaitStatus(DocViewBundle.message("project.library.search.ready", 1, 1));
            assertEquals("A newer query must discard the previous Down", -1, searchList().getSelectedIndex());
        });
        withSearchFieldFocus(query -> {
            query.setText("keyboard");
            pressDown(query);
            KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(query,
                    new FocusEvent(query, FocusEvent.FOCUS_LOST));
        }, () -> {
            awaitStatus(DocViewBundle.message("project.library.search.ready", 1, 1));
            assertEquals("Returning focus before the search ends must not revive Down", -1, searchList().getSelectedIndex());
        });
        withSearchFieldFocus(query -> {
            query.setText("no-such-endpoint");
            pressDown(query);
        }, () -> {
            awaitStatus(DocViewBundle.message("project.library.search.no.results"));
            assertEquals(-1, searchList().getSelectedIndex());
            EdtTestUtil.runInEdtAndWait(() -> queryField().setText("target"));
            awaitStatus(DocViewBundle.message("project.library.search.ready", 1, 1));
            assertEquals("A later search must not consume Down from the empty page", -1, searchList().getSelectedIndex());
        });
    }

    private void prepareKeyboardSearch() throws Exception {
        IndexFixture fixture = writeStore("keyboard target");
        indexService.installCommittedIndex(fixture.index(), indexService.captureInputEpoch());
        awaitStatus(DocViewBundle.message("project.library.status.current"));
        EdtTestUtil.runInEdtAndWait(panel::showProjectSearch);
        awaitStatus(DocViewBundle.message("project.library.search.ready", 1, 1));
    }

    private void withSearchFieldFocus(java.util.function.Consumer<JTextField> gesture, RunnableWithException check)
            throws Exception {
        KeyboardFocusManager previous = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        JTextField query = queryField();
        try {
            EdtTestUtil.runInEdtAndWait(() -> {
                KeyboardFocusManager.setCurrentKeyboardFocusManager(new DefaultKeyboardFocusManager() {
                    @Override
                    public Component getFocusOwner() {
                        return query;
                    }
                });
                gesture.accept(query);
            });
            check.run();
        } finally {
            EdtTestUtil.runInEdtAndWait(() -> KeyboardFocusManager.setCurrentKeyboardFocusManager(previous));
        }
    }

    private JList<?> searchList() {
        return findComponent(panel, JList.class, ignored -> true);
    }

    private static void pressDown(JTextField query) {
        Object key = query.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
        assertNotNull("Down should enter the results directly", key);
        Action action = query.getActionMap().get(key);
        assertNotNull(action);
        action.actionPerformed(new ActionEvent(query, ActionEvent.ACTION_PERFORMED, ""));
    }

    @FunctionalInterface
    private interface RunnableWithException {
        void run() throws Exception;
    }

    private JTextField queryField() {
        return findComponent(panel, JTextField.class, field -> DocViewBundle.message("project.library.search.query")
                .equals(field.getAccessibleContext().getAccessibleName()));
    }

    public void testNarrowSearchKeepsQueryWidthAndAccessibleCompactBackAction() throws Exception {
        EdtTestUtil.runInEdtAndWait(() -> panel.showProjectSearch());

        EdtTestUtil.runInEdtAndWait(() -> {
            JTextField query = queryField();
            JButton back = findComponent(panel, JButton.class, button -> DocViewBundle.message("project.library.back.catalog")
                    .equals(button.getAccessibleContext().getAccessibleName()));
            SearchTextField search = findComponent(panel, SearchTextField.class, component -> true);
            assertNotNull(query);
            assertNotNull(back);
            assertNotNull("Use the platform search field with its built-in search and clear icons", search);
            assertSame(query, search.getTextEditor());
            assertSame(back.getParent(), search.getParent());
            assertEquals("The query row needs only Back and the native search field", 2, back.getParent().getComponentCount());

            Container row = back.getParent();
            row.setSize(420, row.getPreferredSize().height);
            row.doLayout();
            search.doLayout();

            assertTrue("A narrow search row must leave room for a useful query", query.getWidth() >= 180);
            assertEquals("The back action should use a compact icon without a text label", "", back.getText());
            assertTrue("The icon should not reserve the width of a text button", back.getWidth() <= back.getHeight() + 2);
            assertEquals(DocViewBundle.message("project.library.search.placeholder"),
                    ((JBTextField) query).getEmptyText().getText());
            assertNotNull("The compact back action still needs a platform icon", back.getIcon());
            assertEquals(DocViewBundle.message("project.library.back.catalog"), back.getToolTipText());
            assertEquals(DocViewBundle.message("project.library.back.catalog"),
                    back.getAccessibleContext().getAccessibleName());
        });
    }

    public void testEscapeFromQueryOrResultsReturnsToCatalogAndClearsSearchResults() throws Exception {
        IndexFixture fixture = writeStore("escape navigation fixture");
        indexService.installCommittedIndex(fixture.index(), indexService.captureInputEpoch());
        awaitStatus(DocViewBundle.message("project.library.status.current"));
        EdtTestUtil.runInEdtAndWait(() -> panel.showProjectSearch());
        await(() -> listSize() == 1, "the API should appear before testing Escape navigation");

        EdtTestUtil.runInEdtAndWait(() -> {
            JTextField query = queryField();
            assertNotNull(query);
            invokeEscapeAction(query);
            assertEquals("Returning from query focus should clear the previous result set", 0, listSize());
        });

        EdtTestUtil.runInEdtAndWait(() -> panel.showProjectSearch());
        await(() -> listSize() == 1, "a new search should recover after returning to the catalog");
        EdtTestUtil.runInEdtAndWait(() -> {
            JList<?> results = findComponent(panel, JList.class, ignored -> true);
            assertNotNull(results);
            invokeEscapeAction(results);
            assertEquals("Returning from result focus should clear the previous result set", 0, listSize());
        });
    }

    public void testEscapeFromSearchActionsReturnsToCatalogWithoutBindingTheCatalogTree() throws Exception {
        IndexFixture fixture = writeStore("escape action fixture", 21);
        indexService.installCommittedIndex(fixture.index(), indexService.captureInputEpoch());
        awaitStatus(DocViewBundle.message("project.library.status.current"));
        for (String labelKey : List.of("project.library.back.catalog", "project.library.search.more",
                "project.library.open.markdown")) {
            EdtTestUtil.runInEdtAndWait(panel::showProjectSearch);
            await(() -> listSize() == 20, "the first page should appear before testing its actions");
            EdtTestUtil.runInEdtAndWait(() -> {
                JList<?> results = findComponent(panel, JList.class, ignored -> true);
                results.setSelectedIndex(0);
                String label = DocViewBundle.message(labelKey);
                JButton button = findComponent(panel, JButton.class, candidate ->
                        label.equals(candidate.getAccessibleContext().getAccessibleName()));
                assertNotNull(button);
                assertTrue(button.isVisible());
                assertTrue(button.isEnabled());
                Action searchEscape = invokeEscapeAction(button);
                assertEquals("Returning from " + label + " clears search results", 0, listSize());
                JTree catalog = findComponent(panel, JTree.class, ignored -> true);
                assertNotNull(catalog);
                assertNotSame("The catalog does not inherit the search shortcut", searchEscape, findEscapeAction(catalog));
            });
        }
    }

    private static Action findEscapeAction(JComponent source) {
        KeyStroke escape = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0);
        Object key = source.getInputMap(JComponent.WHEN_FOCUSED).get(escape);
        return key == null ? null : source.getActionMap().get(key);
    }

    private static Action invokeEscapeAction(JComponent source) {
        Action action = findEscapeAction(source);
        assertNotNull("Escape must have an action while the search control has focus", action);
        action.actionPerformed(new ActionEvent(source, ActionEvent.ACTION_PERFORMED, "Escape"));
        return action;
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
        EdtTestUtil.runInEdtAndWait(() -> panel.showProjectSearch());
        awaitStatus(DocViewBundle.message("project.library.search.freshness.stale"));
        EdtTestUtil.runInEdtAndWait(() -> panel.showCatalog());
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
            // Swing resolves empty space below the last row to its nearest item.
            results.setSize(360, 300);
            var bounds = results.getCellBounds(0, 0);
            assertTrue("Fixture must leave empty space below the row", bounds.y + bounds.height + 2 < results.getHeight());
            String before = statusText();
            results.dispatchEvent(new java.awt.event.MouseEvent(results, java.awt.event.MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, 10, bounds.y + bounds.height + 2, 2, false,
                    java.awt.event.MouseEvent.BUTTON1));
            assertEquals("A double click on empty space must not start opening the selected file", before, statusText());
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
        awaitStatus(DocViewBundle.message("project.library.search.freshness.current"));
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
            EdtTestUtil.runInEdtAndWait(() -> {
                pressDown(queryField());
                panel.showCatalog();
            });
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
        assertEquals("Returning to the search page does not reuse the old Down", -1, searchList().getSelectedIndex());
        assertTrue("Canceling a read must not alter committed files",
                java.util.Arrays.equals(committedIndex, Files.readAllBytes(documentRoot.resolve("index.json"))));
    }

    public void testEditingWhileAnOlderSearchIsBlockedOnlyShowsLatestQuery() throws Exception {
        IndexFixture fixture = writeStore("search replacement");
        indexService.installCommittedIndex(fixture.index(), indexService.captureInputEpoch());
        awaitStatus(DocViewBundle.message("project.library.status.current"));
        try (DocumentStoreLocks.Lease writer = DocumentStoreLocks.tryAcquireWrite(
                Path.of(getProject().getBasePath()), new EmptyProgressIndicator(), java.time.Duration.ofSeconds(1))) {
            assertNotNull(writer);
            EdtTestUtil.runInEdtAndWait(() -> panel.showProjectSearch());
            awaitWithoutSettlingBackground(this::hasThreadWaitingForDocumentLock, "the first search should be in flight");
            EdtTestUtil.runInEdtAndWait(() -> {
                queryField().setText("library");
                queryField().setText("no-such-endpoint");
            });
        }
        awaitStatus(DocViewBundle.message("project.library.search.no.results"));
        assertEquals(0, listSize());
        EdtTestUtil.runInEdtAndWait(() -> queryField().setText("library"));
        awaitStatus(DocViewBundle.message("project.library.search.ready", 1, 1));
        assertEquals(1, listSize());
    }

    public void testPagingAppendsRowsThenReloadsFirstPageWhenSnapshotChanges() throws Exception {
        IndexFixture fixture = writeStore("paging fixture", 41);
        indexService.installCommittedIndex(fixture.index(), indexService.captureInputEpoch());
        awaitStatus(DocViewBundle.message("project.library.status.current"));
        EdtTestUtil.runInEdtAndWait(() -> panel.showProjectSearch());
        awaitStatus(DocViewBundle.message("project.library.search.ready", 20, 41));
        EdtTestUtil.runInEdtAndWait(() -> {
            JList<?> results = findComponent(panel, JList.class, list -> true);
            results.setSelectedIndex(3);
            JButton more = findButton(DocViewBundle.message("project.library.search.more"));
            assertTrue(more.isVisible() && more.isEnabled());
            more.doClick();
        });
        awaitStatus(DocViewBundle.message("project.library.search.ready", 40, 41));
        EdtTestUtil.runInEdtAndWait(() -> {
            JList<?> results = findComponent(panel, JList.class, list -> true);
            assertEquals("Paging without moving focus keeps the user's current selection", 3, results.getSelectedIndex());
            assertEquals(40, results.getModel().getSize());
        });

        IndexFixture replacement = writeStore("replacement snapshot");
        indexService.installCommittedIndex(replacement.index(), indexService.captureInputEpoch());
        EdtTestUtil.runInEdtAndWait(() -> findButton(DocViewBundle.message("project.library.search.more")).doClick());
        awaitStatus(DocViewBundle.message("project.library.search.ready", 1, 1));
        EdtTestUtil.runInEdtAndWait(() -> {
            JList<?> results = findComponent(panel, JList.class, list -> true);
            assertEquals(1, results.getModel().getSize());
            assertTrue(results.getModel().getElementAt(0).toString().contains("replacement snapshot"));
            assertFalse(findButton(DocViewBundle.message("project.library.search.more")).isVisible());
        });
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
        return writeStore(description, 1);
    }

    private IndexFixture writeStore(String description, int entryCount) throws Exception {
        List<EntryData> entries = new ArrayList<>();
        for (int i = 0; i < entryCount; i++) {
            entries.add(writeEntry(description, i == 0 ? "find" : "find" + i));
        }
        entries.sort(Comparator.comparing(entry -> entry.identity().canonicalForm()));
        byte[] indexMarkdown = "# Project API documents\n".getBytes(StandardCharsets.UTF_8);
        IndexData unsigned = new IndexData("0".repeat(64), "2026-10-24T00:00:00Z", "test", "1".repeat(64), false,
                new FileData("index.md", ProjectDocumentFormat.sha256(indexMarkdown)), entries);
        IndexData index = new IndexData(ProjectDocumentFormat.computeSnapshotId(unsigned), unsigned.generatedAt(),
                unsigned.producerVersion(), unsigned.generatorFingerprint(), unsigned.hasUnsavedDocuments(),
                unsigned.markdownIndex(), unsigned.entries());
        Files.write(documentRoot.resolve("index.md"), indexMarkdown);
        Files.write(documentRoot.resolve("index.json"), ProjectDocumentFormat.writeIndex(index));
        String relativeMarkdownPath = entries.getFirst().markdown().path();
        return new IndexFixture(index, documentRoot.resolve(relativeMarkdownPath), relativeMarkdownPath);
    }

    private EntryData writeEntry(String description, String methodName) throws Exception {
        ProjectDocumentIdentity identity = ProjectDocumentIdentity.of("ui-test",
                "src/main/java/example/LibraryApi.java", "example.LibraryApi", methodName, List.of());
        IdentityData identityData = new IdentityData(identity.moduleName(), identity.qualifiedClassName(),
                identity.methodName(), identity.parameterTypes(), identity.displaySignature(), identity.canonicalForm());
        SourceData source = new SourceData(identity.sourcePath(), 1,
                ProjectDocumentFormat.sha256("fictional source".getBytes(StandardCharsets.UTF_8)),
                "ide-document-text");
        ApiData api = new ApiData(identity.sha256(), identityData, "Library API", description, "spring", "GET",
                "/library", List.of("ui-test", "audit-tag"), source, List.of(), List.of(), null, null,
                new ExamplesData(null, null), "");
        byte[] apiBytes = ProjectDocumentFormat.writeApi(api);
        String relativeMarkdownPath = "apis/" + identity.pageFileName();
        String relativeDetailPath = relativeMarkdownPath.substring(0, relativeMarkdownPath.length() - 3) + ".json";
        byte[] markdownBytes = ("# Library API\n\n" + description + "\n").getBytes(StandardCharsets.UTF_8);
        Path markdownPath = documentRoot.resolve(relativeMarkdownPath);
        Files.createDirectories(markdownPath.getParent());
        Files.write(markdownPath, markdownBytes);
        Files.write(documentRoot.resolve(relativeDetailPath), apiBytes);
        return new EntryData(identity.sha256(), identityData, "Library API", description, "spring", "GET",
                "/library", List.of("ui-test", "audit-tag"), source,
                new FileData(relativeMarkdownPath, ProjectDocumentFormat.sha256(markdownBytes)),
                new FileData(relativeDetailPath, ProjectDocumentFormat.sha256(apiBytes)));
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
