package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialogManager;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.components.ActionLink;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.ui.*;
import javax.swing.*;
import java.awt.*;
import static com.liuzhihang.doc.view.contract.TemplateActionTestSupport.*;

public final class CompactSettingsActionsTest extends LightJavaCodeInsightFixtureTestCase {
    public void testTemplateActionsAreCompactAndRestoreIsSecondary() {
        TemplateSettingForm form = new TemplateSettingForm(getProject());
        try {
            JComponent root = form.createCenterPanel();
            for (String framework : new String[]{"Spring", "Dubbo"}) {
                ActionToolbar toolbar = toolbar(root, framework);
                AnAction[] actions = toolbar.getActionGroup().getChildren(null);
                assertEquals(3, actions.length);
                assertTrue(actions[0] instanceof ToggleAction);
                assertTrue(actions[1] instanceof ToggleAction);
                assertTrue(actions[2] instanceof ActionGroup);
                assertEquals(1, ((ActionGroup) actions[2]).getChildren(null).length);
                assertEquals(DocViewBundle.message("template.restore.defaults"),
                        ((ActionGroup) actions[2]).getChildren(null)[0].getTemplatePresentation().getText());
                for (AnAction action : actions) {
                    assertNotNull(action.getTemplatePresentation().getIcon());
                    assertFalse(action.getTemplatePresentation().getText().isBlank());
                    assertFalse(action.getTemplatePresentation().getDescription().isBlank());
                }
            }
            assertNull(find(root, "restoreDefaultsButton"));
            assertNull(find(root, "toggleVariablesButton"));
            assertNull(find(root, "previewSpringButton"));
        } finally { form.disposeIfNeeded(); }
    }

    public void testRestoreCancelPreservesPreviewAndConfirmOnlyChangesDraft() {
        TemplateSettings settings = TemplateSettings.getInstance(getProject());
        String savedSpring = settings.getSpringTemplate(), savedDubbo = settings.getDubboTemplate();
        TemplateSettingForm form = new TemplateSettingForm(getProject());
        var previous = TestDialogManager.setTestDialog(message -> Messages.CANCEL);
        try {
            JComponent root = form.createCenterPanel();
            var editor = form.getSpringTemplateEditor();
            WriteCommandAction.runWriteCommandAction(getProject(), () -> editor.getDocument().setText("# CUSTOM DRAFT"));
            editor.getCaretModel().moveToOffset(3);
            click(root, "template.preview.spring");
            click(root, "template.restore.defaults");
            assertFalse(editor.getComponent().isVisible());
            assertEquals("# CUSTOM DRAFT", editor.getDocument().getText());
            assertEquals(3, editor.getCaretModel().getOffset());
            assertEquals(savedSpring, settings.getSpringTemplate());
            TestDialogManager.setTestDialog(message -> Messages.OK);
            click(root, "template.restore.defaults");
            assertTrue(editor.getComponent().isVisible());
            assertEquals(DocViewBundle.message("template.spring.init"), editor.getDocument().getText());
            assertEquals(DocViewBundle.message("template.dubbo.init"), form.getDubboTemplateEditor().getDocument().getText());
            assertEquals(savedSpring, settings.getSpringTemplate());
            assertEquals(savedDubbo, settings.getDubboTemplate());
            form.reset();
            assertFalse(form.isModified());
        } finally { TestDialogManager.setTestDialog(previous); form.disposeIfNeeded(); }
    }

    public void testSharedLimitImmediatelyFollowsClassCommentAndGithubUsesLink() {
        GenerationRulesForm rules = new GenerationRulesForm(getProject());
        Component field = find(rules.getRootPanel(), "commentTitleMaxLengthField");
        Component option = find(rules.getRootPanel(), "titleClassCommentCheckBox");
        Container row = field.getParent();
        assertSame("Limit belongs to the class comment group", option.getParent(), row.getParent());
        assertEquals(option.getParent().getComponentZOrder(option) + 1, row.getParent().getComponentZOrder(row));
        assertNotNull("Method comment references the same setting", find(rules.getRootPanel(), "methodCommentLimitHint"));
        SettingsForm general = new SettingsForm(getProject());
        Component star = find(general.getRootPanel(), "githubStarButton");
        assertTrue("Github entry uses a lightweight native link", star instanceof ActionLink);
        assertEquals("Star on GitHub", ((ActionLink) star).getText());
        assertNotNull(((ActionLink) star).getIcon());
    }
}
