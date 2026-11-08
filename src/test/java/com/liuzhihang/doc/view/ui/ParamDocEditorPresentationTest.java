package com.liuzhihang.doc.view.ui;

import com.intellij.DynamicBundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.ActionButtonComponent;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.ex.TooltipDescriptionProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.dto.DocViewParamData;
import com.liuzhihang.doc.view.ui.treeview.ParamTreeTableView;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.AbstractCellEditor;
import java.awt.Component;
import java.awt.ContainerOrderFocusTraversalPolicy;
import java.awt.FocusTraversalPolicy;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.concurrent.atomic.AtomicReference;

public final class ParamDocEditorPresentationTest extends LightJavaCodeInsightFixtureTestCase {

    private Editor editor;

    @Override
    protected void tearDown() throws Exception {
        try {
            if (editor != null) {
                EdtTestUtil.runInEdtAndWait(() -> EditorFactory.getInstance().releaseEditor(editor));
            }
        } finally {
            super.tearDown();
        }
    }

    public void testFooterActionsDescribeCopyAndSourceWriteBackInTheToolbar() throws Exception {
        PsiClass psiClass = myFixture.addClass("package param; public class Request { private String id; }");
        PsiFile psiFile = psiClass.getContainingFile();
        AtomicReference<Editor> createdEditor = new AtomicReference<>();
        AtomicReference<ParamDocEditorForm> createdForm = new AtomicReference<>();
        EdtTestUtil.runInEdtAndWait(() -> {
            createdEditor.set(EditorFactory.getInstance().createViewer(
                    PsiDocumentManager.getInstance(getProject()).getDocument(psiFile), getProject(), EditorKind.UNTYPED));
            createdForm.set(ParamDocEditorForm.getInstance(getProject(), psiFile, createdEditor.get(), psiClass));
        });
        editor = createdEditor.get();
        ParamDocEditorForm form = createdForm.get();

        JPanel rootPanel = fieldValue(form, "rootPanel", JPanel.class);
        ActionToolbar footerToolbar = findFooterToolbar(rootPanel);
        assertNotNull("The parameter editor must expose its footer actions through the toolbar", footerToolbar);

        List<AnAction> actions = footerToolbar.getActions();
        assertEquals("Keep JSON copy, the released Markdown copy action, and source write-back", 3, actions.size());
        AnAction copyAction = actions.stream()
                .filter(action -> action.getTemplatePresentation().getText().toLowerCase(Locale.ROOT).contains("json"))
                .findFirst()
                .orElse(null);
        assertNotNull("The JSON copy action must remain available", copyAction);
        AnAction markdownAction = actions.stream()
                .filter(action -> DocViewBundle.message("action.copy.markdown")
                        .equals(action.getTemplatePresentation().getText()))
                .findFirst().orElse(null);
        assertNotNull("The released Markdown copy action must remain available", markdownAction);
        AnAction applyAction = actions.stream()
                .filter(action -> DocViewBundle.message("action.apply.source")
                        .equals(action.getTemplatePresentation().getText()))
                .findFirst().orElse(null);
        assertNotNull("The source write-back action must remain available", applyAction);

        String expectedCopyText = "zh".equals(DynamicBundle.getLocale().getLanguage()) ? "复制 JSON" : "Copy JSON";
        String expectedApplyText = "zh".equals(DynamicBundle.getLocale().getLanguage()) ? "写回源码" : "Apply to Source";
        Presentation copyPresentation = copyAction.getTemplatePresentation();
        Presentation applyPresentation = applyAction.getTemplatePresentation();

        assertEquals(expectedCopyText, DocViewBundle.message("action.copy.json"));
        assertEquals(expectedCopyText, copyPresentation.getText());
        assertEquals(expectedApplyText, DocViewBundle.message("action.apply.source"));
        assertEquals(expectedApplyText, applyPresentation.getText());
        assertSame("JSON copy must be visually distinct from Markdown copy",
                AllIcons.FileTypes.Json, copyPresentation.getIcon());
        assertSame("Source write-back must use the platform confirmation icon",
                AllIcons.Actions.Checked, applyPresentation.getIcon());
        assertEquals(DocViewBundle.message("action.copy.json.description"), copyPresentation.getDescription());
        assertEquals(DocViewBundle.message("action.apply.source.description"), applyPresentation.getDescription());
        assertSame("Markdown copy must identify its output format",
                AllIcons.FileTypes.Markdown, markdownAction.getTemplatePresentation().getIcon());
        for (AnAction action : actions) {
            Presentation presentation = action.getTemplatePresentation();
            assertTrue("Footer actions must opt into ActionButton's description tooltip",
                    action instanceof TooltipDescriptionProvider);
            assertFalse("Footer buttons must not show persistent text beside their icons",
                    Boolean.TRUE.equals(presentation.getClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR)));
            assertNotNull("Icon-only actions must keep their accessible name", presentation.getText());
            assertFalse(presentation.getText().isBlank());
            assertNotNull("Icon-only actions must keep their explanatory tooltip", presentation.getDescription());
            assertFalse(presentation.getDescription().isBlank());
        }

        String copyDescription = copyPresentation.getDescription().toLowerCase(Locale.ROOT);
        String applyDescription = applyPresentation.getDescription().toLowerCase(Locale.ROOT);
        if ("zh".equals(DynamicBundle.getLocale().getLanguage())) {
            assertTrue("Copy tooltip must identify the field example JSON", copyDescription.contains("字段")
                    && copyDescription.contains("示例") && copyDescription.contains("json"));
            assertTrue("Write-back tooltip must identify descriptions, required state, and source",
                    applyDescription.contains("描述") && applyDescription.contains("必填")
                            && applyDescription.contains("源码"));
        } else {
            assertTrue("Copy tooltip must identify the field example JSON", copyDescription.contains("field")
                    && copyDescription.contains("example") && copyDescription.contains("json"));
            assertTrue("Write-back tooltip must identify descriptions, required state, and source",
                    applyDescription.contains("description") && applyDescription.contains("required")
                            && applyDescription.contains("source"));
        }
    }

    public void testFooterActionToolbarButtonsAreKeyboardFocusable() throws Exception {
        PsiClass psiClass = myFixture.addClass("package param; public class FocusRequest { private String id; }");
        PsiFile psiFile = psiClass.getContainingFile();
        AtomicReference<Editor> createdEditor = new AtomicReference<>();
        AtomicReference<ParamDocEditorForm> createdForm = new AtomicReference<>();
        AtomicReference<JPanel> createdRootPanel = new AtomicReference<>();
        EdtTestUtil.runInEdtAndWait(() -> {
            createdEditor.set(EditorFactory.getInstance().createViewer(
                    PsiDocumentManager.getInstance(getProject()).getDocument(psiFile), getProject(), EditorKind.UNTYPED));
            createdForm.set(ParamDocEditorForm.getInstance(getProject(), psiFile, createdEditor.get(), psiClass));
            createdRootPanel.set(fieldValue(createdForm.get(), "rootPanel", JPanel.class));
        });
        editor = createdEditor.get();

        AtomicReference<ActionToolbar> createdFooterToolbar = new AtomicReference<>();
        AtomicReference<ActionToolbar> createdHeadToolbar = new AtomicReference<>();
        EdtTestUtil.runInEdtAndWait(() -> {
            JPanel rootPanel = fieldValue(createdForm.get(), "rootPanel", JPanel.class);
            createdFooterToolbar.set(findFooterToolbar(rootPanel));
            createdHeadToolbar.set(findToolbar(rootPanel, "ParamDocParamEditorHeadToolbar"));
        });
        ActionToolbar footerToolbar = createdFooterToolbar.get();
        ActionToolbar headToolbar = createdHeadToolbar.get();
        assertNotNull("The parameter editor must expose its footer actions through the toolbar", footerToolbar);
        assertNotNull("The parameter editor must expose its head actions through the toolbar", headToolbar);

        EdtTestUtil.runInEdtAndWait(() -> {
            assertTrue("Popup content must be a focus-cycle root so Tab can reach its child actions",
                    createdRootPanel.get().isFocusCycleRoot());
            JPanel outerCycleRoot = new JPanel();
            outerCycleRoot.setFocusCycleRoot(true);
            FocusTraversalPolicy inheritedPolicy = new ContainerOrderFocusTraversalPolicy();
            outerCycleRoot.setFocusTraversalPolicy(inheritedPolicy);
            outerCycleRoot.add(createdRootPanel.get());
            try {
                assertSame("The fixture must give the popup content an outer cycle-root policy to inherit",
                        outerCycleRoot, createdRootPanel.get().getFocusCycleRootAncestor());
                FocusTraversalPolicy popupPolicy = createdRootPanel.get().getFocusTraversalPolicy();
                assertNotSame("Popup content must not inherit the popup Window's focus traversal policy",
                        inheritedPolicy, popupPolicy);
                assertTrue("Popup content must own a Swing layout policy for traversing its child actions",
                        popupPolicy instanceof LayoutFocusTraversalPolicy);
            } finally {
                outerCycleRoot.remove(createdRootPanel.get());
            }
            footerToolbar.updateActionsImmediately();
            headToolbar.updateActionsImmediately();
            Component[] footerButtons = actionButtonComponents(footerToolbar);
            Component[] headButtons = actionButtonComponents(headToolbar);
            assertEquals("Each icon-only footer action must have a toolbar button", 3, footerButtons.length);
            assertEquals("Each head action must have a toolbar button", 2, headButtons.length);
            for (Component actionButton : java.util.stream.Stream.concat(
                    java.util.Arrays.stream(footerButtons), java.util.Arrays.stream(headButtons)).toList()) {
                assertTrue("Parameter editor toolbar buttons must be reachable by keyboard focus", actionButton.isFocusable());
                assertSame("Action toolbar buttons must be traversed inside the popup content",
                        createdRootPanel.get(), actionButton.getFocusCycleRootAncestor());
            }

            TestActionButtonComponent replacementButton = new TestActionButtonComponent();
            replacementButton.setFocusable(false);
            footerToolbar.getComponent().add(replacementButton);
            assertTrue("A replacement ActionButtonComponent must become focusable when the toolbar rebuilds",
                    replacementButton.isFocusable());
            footerToolbar.getComponent().remove(replacementButton);
        });
    }

    public void testTableFocusTransferShortcutsAreLocalAndDisposedWithPopup() throws Exception {
        PsiClass psiClass = myFixture.addClass("package param; public class FocusShortcutRequest { private String id; }");
        PsiFile psiFile = psiClass.getContainingFile();
        AtomicReference<Editor> createdEditor = new AtomicReference<>();
        AtomicReference<ParamDocEditorForm> createdForm = new AtomicReference<>();
        AtomicReference<JPanel> createdRootPanel = new AtomicReference<>();
        AtomicReference<FocusTrackingParamTreeTableView> createdTable = new AtomicReference<>();
        AtomicReference<Disposable> popupLifetime = new AtomicReference<>();
        AtomicReference<AnAction> forwardAction = new AtomicReference<>();
        AtomicReference<AnAction> backwardAction = new AtomicReference<>();

        EdtTestUtil.runInEdtAndWait(() -> {
            createdEditor.set(EditorFactory.getInstance().createViewer(
                    PsiDocumentManager.getInstance(getProject()).getDocument(psiFile), getProject(), EditorKind.UNTYPED));
            createdForm.set(ParamDocEditorForm.getInstance(getProject(), psiFile, createdEditor.get(), psiClass));
            createdRootPanel.set(fieldValue(createdForm.get(), "rootPanel", JPanel.class));
            createdTable.set(new FocusTrackingParamTreeTableView(createParameterModel(), new CountingCellEditor(false)));
            setFieldValue(createdForm.get(), "tableView", createdTable.get());
            popupLifetime.set(Disposer.newDisposable("parameter popup shortcut test"));
            invokePopupShortcutRegistration(createdForm.get(), popupLifetime.get());

            List<AnAction> localActions = List.copyOf(ActionUtil.getActions(createdRootPanel.get()));
            forwardAction.set(findActionWithShortcut(localActions, "ctrl TAB"));
            backwardAction.set(findActionWithShortcut(localActions, "ctrl shift TAB"));
            assertNotNull("Ctrl+Tab must be registered on the popup component", forwardAction.get());
            assertNotNull("Ctrl+Shift+Tab must be registered on the popup component", backwardAction.get());

            AnActionEvent tableEvent = focusEvent(forwardAction.get(), createdTable.get());
            forwardAction.get().update(tableEvent);
            assertTrue("Table focus must enable the local forward focus action",
                    tableEvent.getPresentation().isEnabled());

            AnActionEvent popupChromeEvent = focusEvent(forwardAction.get(), createdRootPanel.get());
            forwardAction.get().update(popupChromeEvent);
            assertFalse("Popup chrome focus must leave the IDE Switcher shortcut available",
                    popupChromeEvent.getPresentation().isEnabled());
        });
        editor = createdEditor.get();

        try {
            EdtTestUtil.runInEdtAndWait(() -> {
                assertTrue("The popup lifetime must own the local shortcut registrations",
                        ActionUtil.getActions(createdRootPanel.get()).contains(forwardAction.get()));
                Disposer.dispose(popupLifetime.get());
                assertFalse("Disposing the popup must unregister the forward local shortcut",
                        ActionUtil.getActions(createdRootPanel.get()).contains(forwardAction.get()));
                assertFalse("Disposing the popup must unregister the backward local shortcut",
                        ActionUtil.getActions(createdRootPanel.get()).contains(backwardAction.get()));
            });
        } finally {
            if (popupLifetime.get() != null && !Disposer.isDisposed(popupLifetime.get())) {
                Disposer.dispose(popupLifetime.get());
            }
        }
    }

    public void testPopupDispatchEscapeReadsTableAfterAsynchronousInitialization() {
        PsiClass owner = myFixture.addClass("package param; public class EscapeRequest { private String id; }");
        PsiFile psiFile = owner.getContainingFile();
        AtomicReference<JBPopup> popupWithoutTable = new AtomicReference<>();
        AtomicReference<JBPopup> popupCreatedBeforeTable = new AtomicReference<>();

        try {
            EdtTestUtil.runInEdtAndWait(() -> {
                editor = EditorFactory.getInstance().createViewer(
                        PsiDocumentManager.getInstance(getProject()).getDocument(psiFile), getProject(), EditorKind.UNTYPED);
                ParamDocEditorForm form = ParamDocEditorForm.getInstance(getProject(), psiFile, editor, owner);
                setFieldValue(form, "tableView", null);
                JPanel rootPanel = fieldValue(form, "rootPanel", JPanel.class);
                JScrollPane paramScrollPane = fieldValue(form, "paramScrollPane", JScrollPane.class);

                var unloadedBuilder = JBPopupFactory.getInstance()
                        .createComponentPopupBuilder(rootPanel, paramScrollPane)
                        .setCancelKeyEnabled(true);
                form.configurePopupEscapeHandler(unloadedBuilder);
                popupWithoutTable.set(unloadedBuilder.createPopup());
                KeyEvent unloadedEscape = new KeyEvent(rootPanel, KeyEvent.KEY_PRESSED,
                        System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
                assertTrue("Escape before the asynchronous table is loaded retains the popup's default handling",
                        popupWithoutTable.get().dispatchKeyEvent(unloadedEscape));
                assertTrue("An unloaded parameter table must not trap Escape in the popup",
                        popupWithoutTable.get().isDisposed());

                var loadingBuilder = JBPopupFactory.getInstance()
                        .createComponentPopupBuilder(rootPanel, paramScrollPane)
                        .setCancelKeyEnabled(true);
                form.configurePopupEscapeHandler(loadingBuilder);
                popupCreatedBeforeTable.set(loadingBuilder.createPopup());

                DocViewParamData parameter = new DocViewParamData();
                parameter.setPsiElement(owner);
                parameter.setName("id");
                parameter.setType("String");
                parameter.setDesc("initial draft");
                DefaultMutableTreeNode root = new DefaultMutableTreeNode();
                root.add(new DefaultMutableTreeNode(parameter));
                ParamTreeTableView table = new ParamTreeTableView(
                        new ListTreeTableModelOnColumns(root, ParamTreeTableView.COLUMN_INFOS));
                table.setRootVisible(false);
                table.setSize(800, table.getRowHeight());
                setFieldValue(form, "tableView", table);
                paramScrollPane.setViewportView(table);

                table.setValueAt("committed draft", 0, 3);
                assertSame(parameter, table.getModifiedMap().get(owner));
                assertTrue("The loaded description cell must start editing", table.editCellAt(0, 3));
                Component component = table.getEditorComponent();
                assertTrue("Description editing uses its real scroll pane", component instanceof JBScrollPane);
                Component editorView = ((JBScrollPane) component).getViewport().getView();
                assertTrue("The popup dispatch event must originate in the text editor", editorView instanceof JBTextArea);
                ((JBTextArea) editorView).setText("discard this\nmultiline draft");

                KeyEvent firstEscape = new KeyEvent(editorView, KeyEvent.KEY_PRESSED,
                        System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
                assertTrue("The popup created before loading must read the current table and consume Escape",
                        popupCreatedBeforeTable.get().dispatchKeyEvent(firstEscape));
                assertFalse("Escape cancels only the active cell edit", table.isEditing());
                assertFalse("The popup remains available after canceling the cell edit",
                        popupCreatedBeforeTable.get().isDisposed());
                assertEquals("An uncommitted edit must not replace the committed table draft",
                        "committed draft", parameter.getDesc());
                assertSame("The prior committed draft remains eligible for writeback",
                        parameter, table.getModifiedMap().get(owner));

                KeyEvent secondEscape = new KeyEvent(table, KeyEvent.KEY_PRESSED,
                        System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
                assertTrue("When no cell is editing, Escape retains the popup's built-in handling",
                        popupCreatedBeforeTable.get().dispatchKeyEvent(secondEscape));
                assertTrue("The built-in Escape path closes the popup after cell editing has ended",
                        popupCreatedBeforeTable.get().isDisposed());
            });
        } finally {
            EdtTestUtil.runInEdtAndWait(() -> {
                disposePopupIfNeeded(popupWithoutTable.get());
                disposePopupIfNeeded(popupCreatedBeforeTable.get());
            });
        }
    }

    private static void disposePopupIfNeeded(JBPopup popup) {
        if (popup != null && !popup.isDisposed()) {
            Disposer.dispose(popup);
        }
    }

    public void testRejectedCellEditKeepsTableFocusAndAcceptedEditTransfersIt() throws Exception {
        PsiClass psiClass = myFixture.addClass("package param; public class FocusEditRequest { private String id; }");
        PsiFile psiFile = psiClass.getContainingFile();
        AtomicReference<Editor> createdEditor = new AtomicReference<>();
        AtomicReference<ParamDocEditorForm> createdForm = new AtomicReference<>();
        AtomicReference<JPanel> createdRootPanel = new AtomicReference<>();
        AtomicReference<FocusTrackingParamTreeTableView> createdTable = new AtomicReference<>();
        AtomicReference<Disposable> popupLifetime = new AtomicReference<>();
        AtomicReference<AnAction> forwardAction = new AtomicReference<>();

        EdtTestUtil.runInEdtAndWait(() -> {
            createdEditor.set(EditorFactory.getInstance().createViewer(
                    PsiDocumentManager.getInstance(getProject()).getDocument(psiFile), getProject(), EditorKind.UNTYPED));
            createdForm.set(ParamDocEditorForm.getInstance(getProject(), psiFile, createdEditor.get(), psiClass));
            createdRootPanel.set(fieldValue(createdForm.get(), "rootPanel", JPanel.class));
            CountingCellEditor rejectingEditor = new CountingCellEditor(false);
            createdTable.set(new FocusTrackingParamTreeTableView(createParameterModel(), rejectingEditor));
            setFieldValue(createdForm.get(), "tableView", createdTable.get());
            popupLifetime.set(Disposer.newDisposable("parameter popup edit test"));
            invokePopupShortcutRegistration(createdForm.get(), popupLifetime.get());
            forwardAction.set(findActionWithShortcut(ActionUtil.getActions(createdRootPanel.get()), "ctrl TAB"));
            assertNotNull("The table focus action must be available", forwardAction.get());

            assertTrue("The description cell must enter edit mode", createdTable.get().editCellAt(0, 3));
            Component editorComponent = createdTable.get().getEditorComponent();
            AnActionEvent rejectedEvent = focusEvent(forwardAction.get(), editorComponent);
            forwardAction.get().update(rejectedEvent);
            assertTrue("A description editor descendant must enable the local action",
                    rejectedEvent.getPresentation().isEnabled());
            forwardAction.get().actionPerformed(rejectedEvent);
            assertEquals("The shortcut must ask the active cell editor to stop", 1, rejectingEditor.stopCalls);
            assertEquals("A rejected edit must not transfer focus", 0, createdTable.get().forwardTransfers);
            assertTrue("A rejected edit must stay active", createdTable.get().isEditing());

            createdTable.get().getCellEditor().cancelCellEditing();
            CountingCellEditor acceptingEditor = new CountingCellEditor(true);
            createdTable.get().setTestCellEditor(acceptingEditor);
            assertTrue("The description cell must be editable again", createdTable.get().editCellAt(0, 3));
            AnActionEvent acceptedEvent = focusEvent(forwardAction.get(), createdTable.get().getEditorComponent());
            forwardAction.get().actionPerformed(acceptedEvent);
            assertEquals("A valid edit must stop exactly once", 1, acceptingEditor.stopCalls);
            assertEquals("A valid edit must advance to the next focus-cycle component",
                    1, createdTable.get().forwardTransfers);
        });
        editor = createdEditor.get();
        EdtTestUtil.runInEdtAndWait(() -> Disposer.dispose(popupLifetime.get()));
    }

    private static Component[] actionButtonComponents(ActionToolbar toolbar) {
        return java.util.Arrays.stream(toolbar.getComponent().getComponents())
                .filter(ActionButtonComponent.class::isInstance)
                .toArray(Component[]::new);
    }

    private static ActionToolbar findFooterToolbar(Component component) {
        return findToolbar(component, "ParamDocParamEditorTailRightToolbar");
    }

    private static ActionToolbar findToolbar(Component component, String place) {
        if (component instanceof ActionToolbar toolbar && place.equals(toolbar.getPlace())) {
            toolbar.updateActionsImmediately();
            return toolbar;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                ActionToolbar toolbar = findToolbar(child, place);
                if (toolbar != null) {
                    return toolbar;
                }
            }
        }
        return null;
    }

    private static <T> T fieldValue(Object target, String name, Class<T> type) {
        try {
            Field field = ParamDocEditorForm.class.getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(target));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not read parameter editor UI component " + name, exception);
        }
    }

    private static void setFieldValue(Object target, String name, Object value) {
        try {
            Field field = ParamDocEditorForm.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not set parameter editor UI component " + name, exception);
        }
    }

    private static void invokePopupShortcutRegistration(ParamDocEditorForm form, Disposable lifetime) {
        try {
            Method method = ParamDocEditorForm.class.getDeclaredMethod(
                    "registerTableFocusTransferActions", Disposable.class);
            method.setAccessible(true);
            method.invoke(form, lifetime);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not register popup-local table focus actions", exception);
        }
    }

    private static ListTreeTableModelOnColumns createParameterModel() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DocViewParamData data = new DocViewParamData();
        data.setName("id");
        data.setType("String");
        data.setDesc("identifier");
        root.add(new DefaultMutableTreeNode(data));
        return new ListTreeTableModelOnColumns(root, ParamTreeTableView.COLUMN_INFOS);
    }

    private static AnActionEvent focusEvent(AnAction action, Component focusComponent) {
        DataContext context = SimpleDataContext.builder()
                .add(PlatformCoreDataKeys.CONTEXT_COMPONENT, focusComponent)
                .build();
        return AnActionEvent.createEvent(action, context, null, ActionPlaces.UNKNOWN, ActionUiKind.NONE, null);
    }

    private static AnAction findActionWithShortcut(List<AnAction> actions, String shortcutText) {
        KeyboardShortcut expected = new KeyboardShortcut(KeyStroke.getKeyStroke(shortcutText), null);
        return actions.stream()
                .filter(action -> java.util.Arrays.stream(action.getShortcutSet().getShortcuts())
                        .anyMatch(expected::equals))
                .findFirst().orElse(null);
    }

    private static final class TestActionButtonComponent extends JButton implements ActionButtonComponent {
        @Override
        public int getPopState() {
            return NORMAL;
        }
    }

    private static final class CountingCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTextField component = new JTextField("identifier");
        private final boolean allowStop;
        private int stopCalls;

        private CountingCellEditor(boolean allowStop) {
            this.allowStop = allowStop;
        }

        @Override
        public Object getCellEditorValue() {
            return component.getText();
        }

        @Override
        public Component getTableCellEditorComponent(javax.swing.JTable table, Object value, boolean selected,
                                                     int row, int column) {
            return component;
        }

        @Override
        public boolean stopCellEditing() {
            stopCalls++;
            return allowStop && super.stopCellEditing();
        }
    }

    private static final class FocusTrackingParamTreeTableView extends ParamTreeTableView {
        private TableCellEditor testCellEditor;
        private int forwardTransfers;
        private int backwardTransfers;

        private FocusTrackingParamTreeTableView(ListTreeTableModelOnColumns model, TableCellEditor cellEditor) {
            super(model);
            testCellEditor = cellEditor;
        }

        private void setTestCellEditor(TableCellEditor cellEditor) {
            testCellEditor = cellEditor;
        }

        @Override
        public TableCellEditor getCellEditor(int row, int column) {
            if (column == 3) {
                return testCellEditor;
            }
            return super.getCellEditor(row, column);
        }

        @Override
        public void transferFocus() {
            forwardTransfers++;
        }

        @Override
        public void transferFocusBackward() {
            backwardTransfers++;
        }
    }
}
