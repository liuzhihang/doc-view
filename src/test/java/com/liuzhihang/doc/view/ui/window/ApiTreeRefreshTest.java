package com.liuzhihang.doc.view.ui.window;

import com.intellij.openapi.Disposable;
import com.intellij.util.Alarm;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.EntryData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.FileData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.IdentityData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.SourceData;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeModel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Exercises terminal callbacks in a controlled order against the real Swing panel. */
public final class ApiTreeRefreshTest extends LightJavaCodeInsightFixtureTestCase {
    private DocViewWindowPanel panel;
    private final AtomicInteger showCalls = new AtomicInteger();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ToolWindow window = (ToolWindow) Proxy.newProxyInstance(ToolWindow.class.getClassLoader(),
                new Class<?>[]{ToolWindow.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("show")) showCalls.incrementAndGet();
                    if (method.getName().equals("getProject")) return getProject();
                    if (method.getName().equals("toString")) return "Hidden test tool window";
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
                });
        panel = new DocViewWindowPanel(getProject(), window);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (panel instanceof Disposable disposable && !Disposer.isDisposed(disposable)) {
                Disposer.dispose(disposable);
            }
        } finally {
            super.tearDown();
        }
    }

    public void testLatestRefreshWinsAndOldErrorsCannotReplaceItsState() throws Exception {
        long first = begin();
        long second = begin();
        RootNode current = new RootNode();
        finish(second, current, null, false);
        String currentStatus = status();
        finish(first, new RootNode(), null, false);
        finish(first, null, new IllegalStateException("old failure"), false);
        finish(first, null, null, true);
        assertSame(current, field("rootNode"));
        assertEquals(currentStatus, status());
    }

    public void testFailureAndCancelRetainPublishedTreeAndShowAStatus() throws Exception {
        RootNode current = new RootNode();
        finish(begin(), current, null, false);
        finish(begin(), null, new IllegalStateException("test failure"), false);
        assertSame(current, field("rootNode"));
        assertEquals(DocViewBundle.message("catalog.status.failed"), status());
        finish(begin(), null, null, true);
        assertSame(current, field("rootNode"));
        assertEquals(DocViewBundle.message("catalog.status.cancelled"), status());
    }

    public void testEmptyResultHasActionableStatusAndNoDuplicateRoot() throws Exception {
        finish(begin(), new RootNode(), null, false);
        assertEquals(DocViewBundle.message("catalog.status.empty"), status());
        assertFalse(((SimpleTree) field("catalogTree")).isRootVisible());
        assertEquals(0, showCalls.get());
    }

    public void testDisposedPanelIgnoresLateCompletion() throws Exception {
        long request = begin();
        RootNode original = (RootNode) field("rootNode");
        assertTrue("Tool window content must own a disposable panel", panel instanceof Disposable);
        Disposer.dispose((Disposable) panel);
        String before = status();
        finish(request, new RootNode(), null, false);
        finish(request, null, new IllegalStateException("late failure"), false);
        assertSame(original, field("rootNode"));
        assertEquals(before, status());
        assertEquals(0, showCalls.get());
    }

    public void testDisposeDetachesCatalogTreeAndReleasesItsModel() throws Exception {
        SimpleTree tree = (SimpleTree) field("catalogTree");
        Object originalModel = tree.getModel();
        TreeExpansionListener[] expansionListenersBeforeDispose = tree.getTreeExpansionListeners();
        assertNotNull("The catalog tree must start attached to its scroll pane", tree.getParent());
        assertNotNull("The catalog tree must start with its platform UI delegate", tree.getUI());
        assertTrue("The platform UI must install an expansion listener for this regression",
                expansionListenersBeforeDispose.length > 0);

        Disposer.dispose((Disposable) panel);

        assertTrue("Disposal must replace the Doc View model", tree.getModel() instanceof DefaultTreeModel);
        assertNotSame("The disposed plugin model must no longer be held by JTree", originalModel, tree.getModel());
        assertNull("The disposed tree must no longer retain its panel through the component parent", tree.getParent());
        assertEquals("A disposed catalog tree must not retain listeners that cache node paths",
                0, tree.getMouseListeners().length);
        int retainedExpansionListeners = 0;
        for (TreeExpansionListener before : expansionListenersBeforeDispose) {
            for (TreeExpansionListener after : tree.getTreeExpansionListeners()) {
                if (before == after) retainedExpansionListeners++;
            }
        }
        assertEquals("Disposal must detach the old UI expansion listener that retains its pressed TreePath",
                0, retainedExpansionListeners);
        assertNull("A permanently disposed catalog tree must uninstall its UI delegate", tree.getUI());
        assertNull(tree.getSelectionPath());
    }

    @SuppressWarnings("unchecked")
    public void testSingleSelectionEnablesAndClearingSelectionDisablesOpenMarkdown() throws Exception {
        DefaultListModel<EntryData> model = (DefaultListModel<EntryData>) field("searchResultsModel");
        JList<EntryData> results = (JList<EntryData>) field("searchResults");
        JButton openButton = (JButton) field("openSelectedButton");
        setField("searchSnapshotId", "snapshot-test");
        model.addElement(sampleEntry());

        assertFalse(openButton.isEnabled());
        results.setSelectedIndex(0);
        assertTrue("A single-click selection must enable the Markdown action", openButton.isEnabled());
        results.clearSelection();
        assertFalse("Clearing the selection must disable the Markdown action", openButton.isEnabled());
    }

    public void testProjectDocumentStatusEventsAreDebouncedIntoOneRead() throws Exception {
        Method schedule = DocViewWindowPanel.class.getDeclaredMethod("scheduleProjectDocumentStatusRefresh");
        schedule.setAccessible(true);
        schedule.invoke(panel);
        schedule.invoke(panel);

        Alarm alarm = (Alarm) field("projectStatusRefreshAlarm");
        assertEquals("Repeated PSI/document changes should leave one pending status read", 1,
                alarm.getActiveRequestCount());
    }

    private long begin() throws Exception {
        Method method = DocViewWindowPanel.class.getDeclaredMethod("beginRefresh");
        method.setAccessible(true);
        return (long) method.invoke(panel);
    }

    private void finish(long request, RootNode result, Throwable failure, boolean cancelled) throws Exception {
        Method method = DocViewWindowPanel.class.getDeclaredMethod("completeRefresh", long.class,
                RootNode.class, Throwable.class, boolean.class);
        method.setAccessible(true);
        method.invoke(panel, request, result, failure, cancelled);
    }

    private String status() throws Exception {
        return (String) field("catalogStatusText");
    }

    private Object field(String name) throws Exception {
        Field field = DocViewWindowPanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(panel);
    }

    private void setField(String name, Object value) throws Exception {
        Field field = DocViewWindowPanel.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(panel, value);
    }

    private static EntryData sampleEntry() {
        IdentityData identity = new IdentityData("main", "sample.Api", "find", List.of(), "find()", "canonical");
        SourceData source = new SourceData("src/main/java/sample/Api.java", 1, "a".repeat(64), "ide-document-text");
        return new EntryData("a".repeat(64), identity, "Find API", "Sample", "spring", "GET", "/find",
                List.of(), source, new FileData("apis/find.md", "b".repeat(64)),
                new FileData("apis/find.json", "c".repeat(64)));
    }
}
