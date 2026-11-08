package com.liuzhihang.doc.view.ui.treeview;

import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.psi.PsiClass;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.liuzhihang.doc.view.dto.DocViewParamData;

import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.InputMap;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/** Focused Swing-level checks for parameter table edit routing and writeback. */
public final class ParamTreeTableViewTest extends LightJavaCodeInsightFixtureTestCase {

    public void testDescriptionCellStartsAndCommitsThroughEditCellAt() {
        PsiClass owner = myFixture.addClass("package test; class ParamOwner { String value; }");
        DocViewParamData parameter = parameter(owner);
        ParamTreeTableView table = table(parameter);
        int row = 0;

        assertExpectedColumnEditability(table, row);
        assertNotNull("The Description cell must resolve a Swing editor", table.getCellEditor(row, 3));
        assertTrue("JTable's editCellAt path must be able to enter Description editing", table.editCellAt(row, 3));
        JBTextArea editorComponent = descriptionEditor(table);

        editorComponent.setText("Updated description");
        TableCellEditor editor = table.getCellEditor();
        assertNotNull(editor);
        assertTrue("Stopping the editor should commit the Description value", editor.stopCellEditing());

        assertFalse(table.isEditing());
        assertEquals("Updated description", parameter.getDesc());
        assertSame(parameter, table.getModifiedMap().get(owner));
    }

    public void testDescriptionCellEditorPreservesInsertedLineFeedsAndTrailingBlankLines() {
        PsiClass owner = myFixture.addClass("package test; class MultilineOwner { String value; }");
        DocViewParamData parameter = parameter(owner);
        ParamTreeTableView table = table(parameter);

        assertTrue("JTable's editCellAt path must enter Description editing", table.editCellAt(0, 3));
        JBTextArea editor = descriptionEditor(table);
        String description = "first line\n\nquoted: \"value\"; slash: \\\\; trailing lines\n\n";
        editor.selectAll();
        editor.replaceSelection(description);
        assertEquals("Pasting into the active multi-line editor must preserve the exact draft",
                description, editor.getText());

        assertTrue("Stopping the editor should commit the complete Description value",
                table.getCellEditor().stopCellEditing());
        assertEquals("The editor must preserve consecutive/trailing lines, quotes, and backslashes",
                description, parameter.getDesc());
        assertSame(parameter, table.getModifiedMap().get(owner));
    }

    public void testDescriptionEditorEnterInsertsLineBreakAndPlatformPrimaryEnterCommits() {
        PsiClass owner = myFixture.addClass("package test; class KeyboardOwner { String value; }");
        DocViewParamData parameter = parameter(owner);
        ParamTreeTableView table = table(parameter);

        assertTrue(table.editCellAt(0, 3));
        JBTextArea editor = descriptionEditor(table);
        editor.setText("first");
        editor.setCaretPosition(editor.getDocument().getLength());
        invokeEditorAction(editor, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        assertEquals("Plain Enter inserts an LF without ending the edit", "first\n", editor.getText());
        assertTrue(table.isEditing());

        int primaryModifier = SystemInfo.isMac ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK;
        int otherPlatformModifier = SystemInfo.isMac ? InputEvent.CTRL_DOWN_MASK : InputEvent.META_DOWN_MASK;
        KeyStroke primaryCommit = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, primaryModifier);
        KeyStroke nonPrimaryCommit = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, otherPlatformModifier);
        InputMap inputMap = editor.getInputMap(JComponent.WHEN_FOCUSED);
        assertEquals("Only the platform primary modifier should map to the Doc View commit action",
                "docView.description.commitEditing", inputMap.get(primaryCommit));
        assertFalse("The non-primary modifier must not claim Doc View's commit action",
                "docView.description.commitEditing".equals(inputMap.get(nonPrimaryCommit)));

        String expected = "committed-" + primaryModifier;
        editor.setText(expected);
        invokeEditorAction(editor, primaryCommit);
        assertFalse("The platform primary modifier+Enter should stop cell editing", table.isEditing());
        assertEquals(expected, parameter.getDesc());
        assertSame(parameter, table.getModifiedMap().get(owner));
    }

    public void testDescriptionEditorEscapeCancelsDraftWithoutUpdatingModifiedMap() {
        PsiClass owner = myFixture.addClass("package test; class CancelOwner { String value; }");
        DocViewParamData parameter = parameter(owner);
        ParamTreeTableView table = table(parameter);

        assertTrue(table.editCellAt(0, 3));
        JBTextArea editor = descriptionEditor(table);
        editor.setText("uncommitted\nmultiline draft");
        invokeEditorAction(editor, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));

        assertFalse("Escape should cancel cell editing", table.isEditing());
        assertEquals("Initial description", parameter.getDesc());
        assertTrue("Canceled text must not enter the writeback map", table.getModifiedMap().isEmpty());
    }

    public void testDescriptionEditorHandsTabNavigationToTheTable() {
        PsiClass owner = myFixture.addClass("package test; class NavigationOwner { String value; }");
        ParamTreeTableView table = table(parameter(owner));

        assertTrue(table.editCellAt(0, 3));
        JBTextArea editor = descriptionEditor(table);
        KeyStroke tab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
        KeyStroke shiftTab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK);
        assertEquals("Tab should have no local text-area action", "none",
                editor.getInputMap(JComponent.WHEN_FOCUSED).get(tab));
        assertEquals("Shift+Tab should have no local text-area action", "none",
                editor.getInputMap(JComponent.WHEN_FOCUSED).get(shiftTab));
        assertNull("The sentinel binding must not consume Tab locally", editor.getActionMap().get("none"));

        assertTableHasAncestorAction(table, tab);
        assertTableHasAncestorAction(table, shiftTab);
        assertEquals("The tree table keeps its existing fixed row height", 24, table.getRowHeight());
        table.getCellEditor().cancelCellEditing();
    }

    public void testDescriptionDoubleClickEventStartsEditing() {
        PsiClass owner = myFixture.addClass("package test; class DoubleClickOwner { String value; }");
        DocViewParamData parameter = parameter(owner);
        ParamTreeTableView table = table(parameter);
        int row = 0;
        assertExpectedColumnEditability(table, row);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setSize(800, 120);
        scrollPane.doLayout();
        table.setSize(800, 120);
        table.doLayout();
        Rectangle cell = table.getCellRect(row, 3, true);
        int x = cell.x + Math.max(1, cell.width / 2);
        int y = cell.y + Math.max(1, cell.height / 2);
        long now = System.currentTimeMillis();
        table.dispatchEvent(new MouseEvent(table, MouseEvent.MOUSE_PRESSED, now, 0, x, y, 2, false,
                MouseEvent.BUTTON1));
        table.dispatchEvent(new MouseEvent(table, MouseEvent.MOUSE_RELEASED, now + 1, 0, x, y, 2, false,
                MouseEvent.BUTTON1));
        table.dispatchEvent(new MouseEvent(table, MouseEvent.MOUSE_CLICKED, now + 2, 0, x, y, 2, false,
                MouseEvent.BUTTON1));

        assertTrue("Dispatching a JTable double-click event sequence should start cell editing",
                table.isEditing());
        descriptionEditor(table);
    }

    public void testRequiredCellUsesBooleanEditorAndCommitsToModifiedMap() {
        PsiClass owner = myFixture.addClass("package test; class RequiredOwner { String value; }");
        DocViewParamData parameter = parameter(owner);
        parameter.setRequired(true);
        ParamTreeTableView table = table(parameter);
        int row = 0;

        assertExpectedColumnEditability(table, row);
        assertTrue("JTable's editCellAt path must be able to enter Required editing", table.editCellAt(row, 2));
        assertTrue(table.getCellEditor() instanceof DefaultCellEditor);
        assertTrue("The Required editor should be a Boolean combo box",
                ((DefaultCellEditor) table.getCellEditor()).getComponent() instanceof JComboBox<?>);

        @SuppressWarnings("unchecked")
        JComboBox<Boolean> editorComponent = (JComboBox<Boolean>) ((DefaultCellEditor) table.getCellEditor()).getComponent();
        TableCellEditor requiredEditor = table.getCellEditor();
        editorComponent.setSelectedItem(Boolean.FALSE);
        if (table.isEditing()) {
            assertTrue("Stopping the Required editor should commit the Boolean value", requiredEditor.stopCellEditing());
        }
        assertFalse(table.isEditing());

        assertFalse(parameter.getRequired());
        assertSame(parameter, table.getModifiedMap().get(owner));
    }

    public void testTablesHaveIndependentNameRenderers() {
        PsiClass firstOwner = myFixture.addClass("package test; class FirstOwner { String value; }");
        DocViewParamData firstParameter = parameter(firstOwner);
        firstParameter.setName("firstParameter");
        ParamTreeTableView firstTable = table(firstParameter);

        PsiClass secondOwner = myFixture.addClass("package test; class SecondOwner { String value; }");
        DocViewParamData secondParameter = parameter(secondOwner);
        secondParameter.setName("secondParameter");
        ParamTreeTableView secondTable = table(secondParameter);

        JTree firstTree = firstTable.getTree();
        JTree secondTree = secondTable.getTree();
        TreeCellRenderer firstRenderer = firstTree.getCellRenderer();
        TreeCellRenderer secondRenderer = secondTree.getCellRenderer();

        assertNotSame("Each table must own its tree renderer", firstRenderer, secondRenderer);
        Component firstRendered = render(firstTable, firstTree, firstRenderer);
        Component secondRendered = render(secondTable, secondTree, secondRenderer);
        assertNotSame("Rendered components must come from independent NodeRenderer instances",
                firstRendered, secondRendered);
        assertEquals("firstParameter", ((SimpleColoredComponent) firstRendered).getCharSequence(false).toString());
        assertEquals("secondParameter", ((SimpleColoredComponent) secondRendered).getCharSequence(false).toString());
        assertSame(firstTree, ((NodeRenderer) firstRendered).getTree());
        assertSame(secondTree, ((NodeRenderer) secondRendered).getTree());
    }

    private static Component render(ParamTreeTableView table, JTree tree, TreeCellRenderer renderer) {
        DefaultMutableTreeNode row = (DefaultMutableTreeNode)
                ((ListTreeTableModelOnColumns) table.getTableModel()).getRowValue(0);
        Component component = renderer.getTreeCellRendererComponent(tree, row, false, false, true, 0, false);
        assertTrue(component instanceof SimpleColoredComponent);
        assertTrue(component instanceof NodeRenderer);
        return component;
    }

    private static JBTextArea descriptionEditor(ParamTreeTableView table) {
        Component component = table.getEditorComponent();
        assertTrue("Description editing should use a scrollable multi-line component", component instanceof JBScrollPane);
        Component view = ((JBScrollPane) component).getViewport().getView();
        assertTrue("The scroll pane should contain a JBTextArea", view instanceof JBTextArea);
        return (JBTextArea) view;
    }

    private static void invokeEditorAction(JBTextArea editor, KeyStroke keyStroke) {
        Object actionKey = editor.getInputMap(JComponent.WHEN_FOCUSED).get(keyStroke);
        assertNotNull("The editor should define the requested key binding", actionKey);
        Action action = editor.getActionMap().get(actionKey);
        assertNotNull("The key binding should resolve to an editor action", action);
        action.actionPerformed(new ActionEvent(editor, ActionEvent.ACTION_PERFORMED, "test"));
    }

    private static void assertTableHasAncestorAction(ParamTreeTableView table, KeyStroke keyStroke) {
        Object actionKey = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(keyStroke);
        assertNotNull("The table should provide its existing ancestor navigation binding", actionKey);
        assertNotNull("The table navigation binding should resolve to an action", table.getActionMap().get(actionKey));
    }

    private static DocViewParamData parameter(PsiClass owner) {
        DocViewParamData parameter = new DocViewParamData();
        parameter.setPsiElement(owner);
        parameter.setName("value");
        parameter.setType("String");
        parameter.setRequired(false);
        parameter.setDesc("Initial description");
        return parameter;
    }

    private static ParamTreeTableView table(DocViewParamData parameter) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(parameter);
        ListTreeTableModelOnColumns model = new ListTreeTableModelOnColumns(root, ParamTreeTableView.COLUMN_INFOS);
        ParamTreeTableView table = new ParamTreeTableView(model);
        table.setRootVisible(true);
        table.setSize(800, table.getRowHeight());
        return table;
    }

    private static void assertExpectedColumnEditability(ParamTreeTableView table, int row) {
        assertFalse("Parameter name must remain read-only", table.isCellEditable(row, 0));
        assertFalse("Parameter type must remain read-only", table.isCellEditable(row, 1));
        assertTrue("Required must be editable", table.isCellEditable(row, 2));
        assertTrue("Description must be editable", table.isCellEditable(row, 3));
    }
}
