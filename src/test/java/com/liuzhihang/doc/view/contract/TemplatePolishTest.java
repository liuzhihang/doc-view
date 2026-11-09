package com.liuzhihang.doc.view.contract;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.liuzhihang.doc.view.ui.TemplateSettingForm;
import javax.swing.*;
import java.awt.*;

public final class TemplatePolishTest extends LightJavaCodeInsightFixtureTestCase {
    public void testPreviewTogglesInCurrentContentAndPreservesDraftAndCaret() {
        TemplateSettingForm form = new TemplateSettingForm(getProject());
        try {
            JComponent root = form.createCenterPanel();
            var editor = form.getSpringTemplateEditor();
            WriteCommandAction.runWriteCommandAction(getProject(), () -> editor.getDocument().setText("# ${DocView.name}\n"));
            editor.getCaretModel().moveToOffset(3);
            JTabbedPane tabs = (JTabbedPane) find(root, "templateMainTabs");
            Runnable preview = () -> TemplateActionTestSupport.click(root, "template.preview.spring");
            preview.run();
            assertEquals("Preview stays in the editor workspace", 0, tabs.getSelectedIndex());
            assertFalse("Source is hidden when rendered preview is selected", editor.getComponent().isVisible());
            preview.run();
            assertTrue(editor.getComponent().isVisible());
            assertEquals("# ${DocView.name}\n", editor.getDocument().getText());
            assertEquals(3, editor.getCaretModel().getOffset());
        } finally { form.disposeIfNeeded(); }
    }
    public void testPreviewEditingActionsReturnToSource() {
        TemplateSettingForm form = new TemplateSettingForm(getProject());
        try {
            JComponent root = form.createCenterPanel();
            var editor = form.getSpringTemplateEditor();
            Runnable preview = () -> TemplateActionTestSupport.click(root, "template.preview.spring");
            TemplateActionTestSupport.click(root, "template.variables.show");
            preview.run();
            ((JButton) find(root, "insertVariableButton")).doClick();
            assertTrue("Inserting a variable reveals the edited source", editor.getComponent().isVisible());
            assertTrue(editor.getDocument().getText().contains("${DocView."));
            preview.run();
            TemplateActionTestSupport.restoreDefaults(root);
            assertTrue("Restoring defaults reveals the source draft", editor.getComponent().isVisible());
            preview.run();
            form.reset();
            assertTrue("Reset reveals persisted source", editor.getComponent().isVisible());
            assertFalse(form.isModified());
        } finally { form.disposeIfNeeded(); }
    }
    public void testVelocityReferencesAndDirectivesHaveThemeAttributes() {
        TemplateSettingForm form = new TemplateSettingForm(getProject());
        try {
            var editor = form.getSpringTemplateEditor();
            WriteCommandAction.runWriteCommandAction(getProject(), () -> editor.getDocument().setText("# Heading\n#if($DocView.name)${DocView.name}#end\n"));
            RangeHighlighter[] ranges = editor.getMarkupModel().getAllHighlighters();
            assertTrue("Velocity references and directives must be highlighted", ranges.length >= 4);
            assertTrue(java.util.Arrays.stream(ranges).anyMatch(r -> DefaultLanguageHighlighterColors.KEYWORD.equals(r.getTextAttributesKey())));
            assertTrue(java.util.Arrays.stream(ranges).anyMatch(r -> DefaultLanguageHighlighterColors.INSTANCE_FIELD.equals(r.getTextAttributesKey())));
        } finally { form.disposeIfNeeded(); }
    }
    private static Component find(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName())) return child;
            if (child instanceof Container c) { Component found = find(c, name); if (found != null) return found; }
        }
        return null;
    }
}
