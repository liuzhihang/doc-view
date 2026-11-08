package com.liuzhihang.doc.view.ui;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManagerListener;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.psi.PsiJavaFile;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.messages.MessageBusConnection;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.ui.treeview.ParamTreeTableView;

import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

public final class ParamDocMarkdownCopyTest extends LightJavaCodeInsightFixtureTestCase {

    private Boolean originalSeparateParam;
    private Transferable originalClipboard;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        originalSeparateParam = Settings.getInstance(getProject()).getSeparateParam();
        Settings.getInstance(getProject()).setSeparateParam(false);
        EdtTestUtil.runInEdtAndWait(() -> {
            originalClipboard = CopyPasteManager.getInstance().getContents();
            CopyPasteManager.getInstance().setContents(new StringSelection("clipboard-before-copy"));
        });
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            PlatformTestUtil.waitForAllBackgroundActivityToCalmDown();
            Settings.getInstance(getProject()).setSeparateParam(originalSeparateParam);
            EdtTestUtil.runInEdtAndWait(() -> CopyPasteManager.getInstance().setContents(
                    originalClipboard == null ? new StringSelection("") : originalClipboard));
        } finally {
            super.tearDown();
        }
    }

    public void testCopiesCurrentCellEditAndRequiredFlagWithoutWritingSource() throws Exception {
        EditorFixture fixture = createEditor("""
                class CopyEntity {
                    /** Original identifier. */
                    String id;
                }
                """);
        String originalSource = myFixture.getFile().getText();
        String unsavedDescription = "Unsaved edited description\nsecond line with \"quotes\" and \\\\slash\n\n";
        AnAction action = markdownAction(fixture.form());

        EdtTestUtil.runInEdtAndWait(() -> {
            ParamTreeTableView table = field(fixture.form(), "tableView", ParamTreeTableView.class);
            int row = rowFor(table, "id");
            String originalCellDescription = String.valueOf(table.getValueAt(row, 3));
            table.setValueAt(true, row, 2);
            assertTrue("The description must be editable in the real field table", table.editCellAt(row, 3));
            assertTrue("The description editor should provide a scrollable multi-line component",
                    table.getEditorComponent() instanceof JBScrollPane);
            JBScrollPane scrollPane = (JBScrollPane) table.getEditorComponent();
            Component view = scrollPane.getViewport().getView();
            assertTrue("The description editor should contain a JBTextArea", view instanceof JBTextArea);
            assertEquals("The model should still contain the original value before the copy action commits editing",
                    originalCellDescription, table.getValueAt(row, 3));
            ((JBTextArea) view).setText(unsavedDescription);
            assertTrue("The cell must still be editing when the copy action reads its current value", table.isEditing());
            action.actionPerformed(event(action));
            assertFalse("The copy action should commit the active cell before producing Markdown", table.isEditing());
        });

        await(fixture.closed()::get, "Copying Markdown must finish and close the editor popup");
        assertTrue(clipboard(), clipboard().startsWith("|参数名|类型|必选|描述|版本|\n"));
        assertTrue("Markdown copy should render each logical newline as <br> in the original field row",
                clipboard().contains("|id|String|Y|Unsaved edited description<br>second line with \"quotes\" and \\\\slash<br><br>|"));
        assertFalse("Entity copy must not wrap the table in an API document", clipboard().contains("##"));
        assertEquals("Copy must not save the editor's pending values into source", originalSource,
                myFixture.getFile().getText());
    }

    public void testCopiesNestedFieldsUsingSeparateTableSetting() throws Exception {
        Settings.getInstance(getProject()).setSeparateParam(true);
        EditorFixture fixture = createEditor("""
                class NestedCopyEntity {
                    /** 详情信息 */
                    CopyDetail detail;
                }
                class CopyDetail {
                    /** 显示名称 */
                    String label;
                }
                """);
        AnAction action = markdownAction(fixture.form());
        EdtTestUtil.runInEdtAndWait(() -> action.actionPerformed(event(action)));

        await(fixture.closed()::get, "Nested Markdown copy must finish");
        assertTrue(clipboard(), clipboard().contains("|detail|CopyDetail|N|详情信息|"));
        assertTrue(clipboard(), clipboard().contains("\n- CopyDetail detail\n"));
        assertTrue(clipboard(), clipboard().contains("|label|String|N|显示名称|"));
    }

    public void testCopyIsDisabledForLoadingEmptyAndClosedEditor() throws Exception {
        EditorFixture fixture = createEditor("class EmptyCopyEntity {}");
        AnAction action = markdownAction(fixture.form());
        EdtTestUtil.runInEdtAndWait(() -> {
            AnActionEvent event = event(action);
            action.update(event);
            assertFalse("An entity without fields must not replace the clipboard", event.getPresentation().isEnabled());
            action.actionPerformed(event);

            setField(fixture.form(), "tableView", null);
            action.update(event);
            assertFalse("Loading content must not be copyable", event.getPresentation().isEnabled());
            action.actionPerformed(event);

            fixture.closed().set(true);
            action.update(event);
            assertFalse("A closed editor must not remain copyable", event.getPresentation().isEnabled());
            action.actionPerformed(event);
        });
        PlatformTestUtil.waitForAllBackgroundActivityToCalmDown();
        assertEquals("clipboard-before-copy", clipboard());
    }

    public void testClosingBeforeBackgroundDeliveryPreservesClipboard() throws Exception {
        EditorFixture fixture = createEditor("class LateCopyEntity { String name; }");
        AnAction action = markdownAction(fixture.form());
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch rendered = new CountDownLatch(1);
        AtomicBoolean startedOnEdt = new AtomicBoolean();
        MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
        String property = "intellij.progress.task.ignoreHeadless";
        String previousProperty = System.getProperty(property);
        // By default the platform may run Backgroundable tasks synchronously in unit tests.
        // Pin the real task before its body, so close-before-delivery never depends on timing.
        System.setProperty(property, "true");
        connection.subscribe(ProgressManagerListener.TOPIC, new ProgressManagerListener() {
            private boolean isCopyTask(Task task) {
                return task.getProject() == getProject()
                        && DocViewBundle.message("task.docview.markdown").equals(task.getTitle());
            }

            @Override
            public void beforeTaskStart(Task task, ProgressIndicator indicator) {
                if (!isCopyTask(task)) return;
                startedOnEdt.set(ApplicationManager.getApplication().isDispatchThread());
                started.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) {
                        throw new AssertionError("The test did not release the Markdown task");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
            }

            @Override
            public void afterTaskStart(Task task, ProgressIndicator indicator) {
                if (isCopyTask(task)) rendered.countDown();
            }
        });
        try {
            EdtTestUtil.runInEdtAndWait(() -> action.actionPerformed(event(action)));
            await(() -> started.getCount() == 0, "The real Markdown task must start before closing the editor");
            assertFalse("The test must hold an actual background task", startedOnEdt.get());
            assertEquals("The blocked task cannot have copied yet", "clipboard-before-copy", clipboard());
            EdtTestUtil.runInEdtAndWait(() -> {
                fixture.closed().set(true);
                CopyPasteManager.getInstance().setContents(new StringSelection("clipboard-after-close"));
            });
            release.countDown();
            await(() -> rendered.getCount() == 0, "The closed editor's task must still finish rendering");
            PlatformTestUtil.waitForAllBackgroundActivityToCalmDown();
            EdtTestUtil.runInEdtAndWait(PlatformTestUtil::dispatchAllInvocationEventsInIdeEventQueue);
            assertEquals("A closed popup's late result must not overwrite newer clipboard contents",
                    "clipboard-after-close", clipboard());
        } finally {
            release.countDown();
            connection.disconnect();
            if (previousProperty == null) System.clearProperty(property);
            else System.setProperty(property, previousProperty);
        }
    }

    private EditorFixture createEditor(String source) throws Exception {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("CopyEntity.java", source);
        AtomicReference<ParamDocEditorForm> formReference = new AtomicReference<>();
        AtomicBoolean closed = new AtomicBoolean();
        JBPopup popup = (JBPopup) Proxy.newProxyInstance(JBPopup.class.getClassLoader(),
                new Class<?>[]{JBPopup.class}, (proxy, method, arguments) -> switch (method.getName()) {
                    case "isDisposed" -> closed.get();
                    case "cancel" -> { closed.set(true); yield null; }
                    case "toString" -> "entity-markdown-test-popup";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    default -> throw new UnsupportedOperationException(method.toString());
                });
        EdtTestUtil.runInEdtAndWait(() -> {
            ParamDocEditorForm form = new ParamDocEditorForm(getProject(), file,
                    myFixture.getEditor(), file.getClasses()[0]);
            setField(form, "popup", popup);
            formReference.set(form);
        });
        ParamDocEditorForm form = formReference.get();
        await(() -> field(form, "tableView", ParamTreeTableView.class) != null,
                "The editor must load the actual PSI fields before copying");
        return new EditorFixture(form, closed);
    }

    private AnAction markdownAction(ParamDocEditorForm form) {
        AtomicReference<AnAction> found = new AtomicReference<>();
        EdtTestUtil.runInEdtAndWait(() -> {
            JPanel toolbarPanel = field(form, "tailToolbarPanel", JPanel.class);
            found.set(findMarkdownAction(toolbarPanel));
        });
        assertNotNull("The standalone entity editor must offer Copy as Markdown on its toolbar", found.get());
        return found.get();
    }

    private static AnAction findMarkdownAction(Component component) {
        if (component instanceof ActionToolbar toolbar) {
            for (AnAction action : toolbar.getActionGroup().getChildren(null)) {
                String text = action.getTemplatePresentation().getText();
                if (text != null && text.contains("Markdown")) return action;
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                AnAction result = findMarkdownAction(child);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static int rowFor(ParamTreeTableView table, String name) {
        for (int row = 0; row < table.getRowCount(); row++) {
            if (name.equals(table.getValueAt(row, 0))) return row;
        }
        throw new AssertionError("Field not visible in the editor: " + name);
    }

    private AnActionEvent event(AnAction action) {
        DataContext context = dataId -> CommonDataKeys.PROJECT.is(dataId) ? getProject() : null;
        return AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, context);
    }

    private static String clipboard() {
        AtomicReference<String> text = new AtomicReference<>();
        EdtTestUtil.runInEdtAndWait(() -> text.set(CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor)));
        return text.get();
    }

    private static void await(BooleanSupplier condition, String message) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            if (ApplicationManager.getApplication().isDispatchThread()) {
                PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            }
            Thread.sleep(10);
        }
        assertTrue(message, condition.getAsBoolean());
    }

    private static <T> T field(Object target, String name, Class<T> type) {
        try {
            Field field = ParamDocEditorForm.class.getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(target));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = ParamDocEditorForm.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private record EditorFixture(ParamDocEditorForm form, AtomicBoolean closed) {}
}
