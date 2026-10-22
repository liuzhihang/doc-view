package com.liuzhihang.doc.view.ui.window;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.DocViewBundle;

import javax.swing.JLabel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
        return ((JLabel) field("refreshStatus")).getText();
    }

    private Object field(String name) throws Exception {
        Field field = DocViewWindowPanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(panel);
    }
}
