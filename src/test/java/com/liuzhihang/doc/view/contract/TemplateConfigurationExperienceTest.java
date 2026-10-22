package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.enums.FrameworkEnum;
import com.liuzhihang.doc.view.ui.TemplatePreviewCoordinator;
import com.liuzhihang.doc.view.ui.TemplatePreviewRenderer;
import com.liuzhihang.doc.view.ui.TemplateSettingForm;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class TemplateConfigurationExperienceTest extends LightJavaCodeInsightFixtureTestCase {

    private Project project;
    private TemplateSettings settings;
    private String originalSpring;
    private String originalDubbo;
    private final List<TemplateSettingForm> forms = new ArrayList<>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        project = getProject();
        settings = TemplateSettings.getInstance(project);
        originalSpring = settings.getSpringTemplate();
        originalDubbo = settings.getDubboTemplate();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            EdtTestUtil.runInEdtAndWait(() -> forms.forEach(form -> {
                if (!form.isDisposed()) form.disposeIfNeeded();
            }));
            settings.setSpringTemplate(originalSpring);
            settings.setDubboTemplate(originalDubbo);
        } finally {
            super.tearDown();
        }
    }

    public void testVariableSearchAndInsertAtCurrentCaret() {
        ResourceBundle simplifiedChinese = ResourceBundle.getBundle(
                "messages.DocViewBundle", Locale.SIMPLIFIED_CHINESE, getClass().getClassLoader());
        assertEquals("The Chinese locale should retain the translated variable label",
                "请求示例", simplifiedChinese.getString("template.variable.requestExample"));
        assertTrue("The Chinese locale should retain the translated searchable description",
                simplifiedChinese.getString("template.variable.requestExample.description").contains("请求示例"));

        EdtTestUtil.runInEdtAndWait(() -> {
            TemplateSettingForm form = createForm();
            JComponent root = form.createCenterPanel();

            JTabbedPane workspaces = component(root, "templateMainTabs", JTabbedPane.class);
            JButton toggleVariables = component(root, "toggleVariablesButton", JButton.class);

            assertNotNull(workspaces);
            assertEquals(3, workspaces.getTabCount());
            assertEquals(DocViewBundle.message("template.tab.editor"), workspaces.getTitleAt(0));
            assertEquals(DocViewBundle.message("template.tab.preview"), workspaces.getTitleAt(1));
            assertEquals(DocViewBundle.message("template.tab.output"), workspaces.getTitleAt(2));
            assertEquals("The editor workspace should be selected on first open", 0, workspaces.getSelectedIndex());
            assertEquals(2, component(root, "templateFrameworkTabs", JTabbedPane.class).getTabCount());
            assertNull("The variable sidebar should be collapsed on first open",
                    component(root, "templateVariableSplitter", JBSplitter.class));
            assertEquals(DocViewBundle.message("template.variables.show"), toggleVariables.getText());
            toggleVariables.doClick();
            JBSplitter variableSplitter = component(root, "templateVariableSplitter", JBSplitter.class);
            assertTrue(variableSplitter instanceof OnePixelSplitter);
            assertTrue(variableSplitter.getFirstComponent().getMinimumSize().width >= 360);
            assertTrue(variableSplitter.getSecondComponent().getMinimumSize().width >= 240);
            assertEquals(DocViewBundle.message("template.variables.hide"), toggleVariables.getText());
            JBTextField search = component(root, "variableSearchField", JBTextField.class);
            JList<?> variables = component(root, "variableList", JList.class);
            JTextArea description = component(root, "variableDescription", JTextArea.class);
            JButton insert = component(root, "insertVariableButton", JButton.class);
            assertEquals(DocViewBundle.message("template.variables.search"), search.getEmptyText().getText());
            assertEquals(DocViewBundle.message("template.variables.search.accessible"),
                    search.getAccessibleContext().getAccessibleName());
            search.setText("responseParamDataList");
            assertEquals("The variable search should filter the list", 1, variables.getModel().getSize());
            TemplateSettingForm.TemplateVariable selectedVariable =
                    (TemplateSettingForm.TemplateVariable) variables.getSelectedValue();
            Object item = variables.getModel().getElementAt(0);
            assertEquals("${DocView.responseParamDataList}", item.toString());
            JPanel variableRow = (JPanel) renderSelectedVariable(variables);
            assertEquals(2, variableRow.getComponentCount());
            JLabel propertyName = (JLabel) variableRow.getComponent(0);
            JLabel displayName = (JLabel) variableRow.getComponent(1);
            assertEquals("responseParamDataList", propertyName.getText());
            assertEquals(DocViewBundle.message("template.variable.responseParamDataList"), displayName.getText());
            assertEquals("The compact property row should retain the list font size",
                    variables.getFont().getSize(), propertyName.getFont().getSize());
            assertEquals(variables.getFont().getSize(), displayName.getFont().getSize());
            assertTrue(description.getText().contains(selectedVariable.expression()));
            assertTrue(description.getText().contains(selectedVariable.displayName()));
            assertTrue(description.getText().contains(selectedVariable.description()));
            JBScrollPane descriptionScroll = component(root, "variableDescriptionScroll", JBScrollPane.class);
            assertNotNull("The variable details should have their own scrollable viewport", descriptionScroll);
            assertEquals("The details panel should reserve six readable text rows",
                    6, description.getRows());
            assertEquals("The details panel should reserve the agreed scaled height",
                    JBUI.scale(120), descriptionScroll.getPreferredSize().height);
            assertEquals("Short windows should scroll the details instead of clipping them",
                    JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, descriptionScroll.getVerticalScrollBarPolicy());
            JLabel hint = component(root, "templateVariableHint", JLabel.class);
            assertEquals(DocViewBundle.message("template.variables.hint"), hint.getText());
            assertEquals(DocViewBundle.message("template.variables.hint.tooltip"), hint.getToolTipText());
            form.getSpringTemplateEditor().getCaretModel().moveToOffset(0);
            insert.doClick();
            assertTrue("Inserting a compactly displayed variable should keep its full Velocity expression",
                    form.getSpringTemplateEditor().getDocument().getText()
                            .startsWith("${DocView.responseParamDataList}"));

            search.setText(DocViewBundle.message("template.variable.requestExample.description"));
            assertEquals("The localized variable description should remain searchable", 1,
                    variables.getModel().getSize());
            item = variables.getModel().getElementAt(0);
            assertEquals("${DocView.requestExample}", item.toString());
            assertTrue(description.getText().contains("${DocView.requestExample}"));
            assertTrue(description.getText().contains(DocViewBundle.message(
                    "template.variable.requestExample.description")));
            assertEquals(DocViewBundle.message("template.variables.description.accessible"),
                    description.getAccessibleContext().getAccessibleName());
            assertTrue("The selected variable explanation should support wrapped multi-line reading",
                    description.getLineWrap() && description.getRows() >= 6);

            form.getSpringTemplateEditor().getCaretModel().moveToOffset(0);
            variables.setSelectedIndex(0);
            insert.doClick();

            assertTrue(form.getSpringTemplateEditor().getDocument().getText()
                    .startsWith("${DocView.requestExample}"));
            toggleVariables.doClick();
            assertNull("The variable sidebar should collapse on demand",
                    component(root, "templateVariableSplitter", JBSplitter.class));
            assertEquals(DocViewBundle.message("template.variables.show"), toggleVariables.getText());
            toggleVariables.doClick();
            JTabbedPane frameworks = component(root, "templateFrameworkTabs", JTabbedPane.class);
            frameworks.setSelectedIndex(1);
            form.getDubboTemplateEditor().getCaretModel().moveToOffset(0);
            insert.doClick();
            assertTrue("The same variable action should target the selected Dubbo editor",
                    form.getDubboTemplateEditor().getDocument().getText().startsWith("${DocView.requestExample}"));
        });
    }

    public void testTemplateGuidanceRemainsVisibleInNarrowViewport() {
        ResourceBundle english = ResourceBundle.getBundle(
                "messages.DocViewBundle", Locale.ROOT, getClass().getClassLoader());
        ResourceBundle simplifiedChinese = ResourceBundle.getBundle(
                "messages.DocViewBundle", Locale.SIMPLIFIED_CHINESE, getClass().getClassLoader());
        EdtTestUtil.runInEdtAndWait(() -> {
            TemplateSettingForm form = createForm();
            JComponent root = form.createCenterPanel();
            JTabbedPane frameworks = component(root, "templateFrameworkTabs", JTabbedPane.class);
            JTextArea summary = component(root, "templateSettingsSummary", JTextArea.class);
            JPanel header = component(root, "templateSettingsHeader", JPanel.class);
            ContextHelpLabel help = component(root, "templateHelpLabel", ContextHelpLabel.class);
            JPanel editorActions = component(root, "templateEditorActions", JPanel.class);
            JButton restore = component(root, "restoreDefaultsButton", JButton.class);
            JButton variables = component(root, "toggleVariablesButton", JButton.class);
            assertNotNull(summary);
            assertNotNull(header);
            assertNotNull(help);
            assertNotNull(editorActions);
            assertNotNull(restore);
            assertNotNull(variables);

            for (int width : new int[]{900, 640}) {
                for (ResourceBundle locale : new ResourceBundle[]{english, simplifiedChinese}) {
                    summary.setText(locale.getString("template.settings.summary"));
                    root.setSize(width, 700);
                    root.doLayout();
                    for (int frameworkIndex = 0; frameworkIndex < frameworks.getTabCount(); frameworkIndex++) {
                        frameworks.setSelectedIndex(frameworkIndex);
                        layoutRecursively(root);

                        String frameworkName = frameworkIndex == 0 ? "Spring" : "Dubbo";
                        JPanel templatePanel = component(root, "templatePanel" + frameworkName, JPanel.class);
                        JPanel previewControls = component(root, "templatePreviewControls" + frameworkName, JPanel.class);
                        JPanel previewActions = component(root, "previewActions" + frameworkName, JPanel.class);
                        JTextArea fixedSampleHelp = component(root, "fixedSampleHelp" + frameworkName, JTextArea.class);

                        assertNotNull(templatePanel);
                        assertNotNull(previewControls);
                        assertNotNull(previewActions);
                        assertNotNull(fixedSampleHelp);
                        assertEquals(locale.getString("template.settings.summary"), summary.getText());
                        assertTrue("The header summary should wrap and remain read-only", summary.getLineWrap()
                                && !summary.isEditable());
                        assertTrue("The summary must have visible width in the narrow viewport", summary.getWidth() > 0
                                && summary.getWidth() < header.getWidth());
                        assertTrue("The summary must have enough height for its wrapped rows",
                                summary.getHeight() >= summary.getPreferredSize().height
                                        && summary.getY() >= 0
                                        && summary.getY() + summary.getHeight() <= header.getHeight());
                        assertTrue("The context icon should sit beside the summary and remain inside the header",
                                help.getX() >= summary.getX() + summary.getWidth()
                                        && help.getY() >= 0
                                        && help.getY() + help.getHeight() <= header.getHeight()
                                        && help.getX() + help.getWidth() <= header.getWidth());
                        assertTrue("The editor operation row should fit the selected viewport",
                                editorActions.getWidth() > 0 && editorActions.getX() >= 0
                                        && editorActions.getX() + editorActions.getWidth() <= root.getWidth());
                        assertSame(editorActions, restore.getParent());
                        assertSame(editorActions, variables.getParent());
                        assertTrue("The header should no longer reserve a second button row",
                                header.getHeight() <= summary.getHeight() + JBUI.scale(8));
                        assertEquals(DocViewBundle.message("template.preview.fixed.sample"), fixedSampleHelp.getText());
                        assertTrue("Fixed-sample guidance should wrap and remain read-only", fixedSampleHelp.getLineWrap()
                                && !fixedSampleHelp.isEditable());
                        assertTrue("The fixed-sample guidance must fit the available width",
                                fixedSampleHelp.getWidth() > 0 && fixedSampleHelp.getWidth() <= previewControls.getWidth());
                        assertTrue("The fixed-sample guidance must have enough height for its wrapped rows",
                                fixedSampleHelp.getHeight() >= fixedSampleHelp.getPreferredSize().height);
                        assertTrue("Preview guidance must have its own row below the preview action",
                                fixedSampleHelp.getY() >= previewActions.getY() + previewActions.getHeight());
                        assertTrue("The preview guidance must remain inside its control area",
                                fixedSampleHelp.getY() + fixedSampleHelp.getHeight() <= previewControls.getHeight()
                                        && previewControls.getY() + previewControls.getHeight() <= templatePanel.getHeight());
                    }
                }
            }
        });
    }

    public void testTemplateHelpUsesContextIconAndRestoreBelongsToEditorActions() {
        EdtTestUtil.runInEdtAndWait(() -> {
            TemplateSettingForm form = createForm();
            JComponent root = form.createCenterPanel();
            ContextHelpLabel help = component(root, "templateHelpLabel", ContextHelpLabel.class);
            JPanel editorActions = component(root, "templateEditorActions", JPanel.class);
            JButton restore = component(root, "restoreDefaultsButton", JButton.class);
            JButton variables = component(root, "toggleVariablesButton", JButton.class);

            assertNotNull("Template guidance should be a native context-help icon", help);
            assertEquals(DocViewBundle.message("template.help.open"),
                    help.getAccessibleContext().getAccessibleName());
            assertEquals(DocViewBundle.message("template.help.description"),
                    help.getAccessibleContext().getAccessibleDescription());
            assertNotNull(editorActions);
            assertSame("Restore and variable actions should share the template editor operation row",
                    editorActions, restore.getParent());
            assertSame(editorActions, variables.getParent());
            assertNull("The page header should not contain a large help button",
                    component(root, "templateHelpButton", JButton.class));
            assertNull("The page header should not contain a separate actions row",
                    component(root, "templateHeaderActions", JPanel.class));

            JTabbedPane workspaces = component(root, "templateMainTabs", JTabbedPane.class);
            assertTrue("The restore action should be contained by the editor workspace",
                    isDescendant(workspaces.getComponentAt(0), restore));
            assertFalse("The restore action should not be part of preview",
                    isDescendant(workspaces.getComponentAt(1), restore));
            assertFalse("The restore action should not be part of output settings",
                    isDescendant(workspaces.getComponentAt(2), restore));
        });
    }

    public void testPreviewStatusDistinguishesInitialSuccessStaleAndReset() throws Exception {
        CountDownLatch previewCompleted = new CountDownLatch(1);
        CountDownLatch draftMarkedStale = new CountDownLatch(1);
        TemplateSettingForm[] formRef = new TemplateSettingForm[1];
        JLabel[] statusRef = new JLabel[1];

        EdtTestUtil.runInEdtAndWait(() -> {
            TemplateSettingForm form = createForm();
            JComponent root = form.createCenterPanel();
            JTabbedPane workspaces = component(root, "templateMainTabs", JTabbedPane.class);
            JLabel status = component(root, "previewStatusLabel", JLabel.class);
            formRef[0] = form;
            statusRef[0] = status;
            assertEquals(0, workspaces.getSelectedIndex());
            assertEquals(DocViewBundle.message("template.preview.status.initial"), status.getText());
            status.addPropertyChangeListener("text", event -> {
                if (DocViewBundle.message("template.preview.status.success").equals(event.getNewValue())) {
                    previewCompleted.countDown();
                }
                if (DocViewBundle.message("template.preview.status.stale").equals(event.getNewValue())) {
                    draftMarkedStale.countDown();
                }
            });

            component(root, "previewSpringButton", JButton.class).doClick();
            assertEquals("Requesting a preview should open its workspace immediately", 1,
                    workspaces.getSelectedIndex());
            assertEquals(DocViewBundle.message("template.preview.status.loading"), status.getText());
            workspaces.setSelectedIndex(2);
        });

        assertTrue("A successful fixed-sample preview should report completion",
                awaitUiUpdate(previewCompleted));
        EdtTestUtil.runInEdtAndWait(() -> {
            assertEquals(DocViewBundle.message("template.preview.status.success"), statusRef[0].getText());
            JTabbedPane workspaces = component(formRef[0].createCenterPanel(), "templateMainTabs", JTabbedPane.class);
            assertEquals("An async result must not take over a workspace selected by the user", 2,
                    workspaces.getSelectedIndex());
            workspaces.setSelectedIndex(1);
            WriteCommandAction.runWriteCommandAction(project, () ->
                    formRef[0].getSpringTemplateEditor().getDocument().insertString(0, "# draft\n"));
        });
        assertTrue("Editing the template should mark the displayed preview stale",
                awaitUiUpdate(draftMarkedStale));
        EdtTestUtil.runInEdtAndWait(() -> {
            assertEquals(DocViewBundle.message("template.preview.status.stale"), statusRef[0].getText());
            formRef[0].reset();
            assertEquals("Reset should return to the unpreviewed state",
                    DocViewBundle.message("template.preview.status.initial"), statusRef[0].getText());
            assertFalse(formRef[0].isModified());
            assertEquals("Reset should leave the selected workspace unchanged", 1,
                    component(formRef[0].createCenterPanel(), "templateMainTabs", JTabbedPane.class).getSelectedIndex());
        });
    }

    public void testEmptyAndFailedPreviewsShowFormStateAndClearPreviousMarkdown() throws Exception {
        CountDownLatch previewCompleted = new CountDownLatch(1);
        CountDownLatch emptyPreviewShown = new CountDownLatch(1);
        CountDownLatch failedPreviewShown = new CountDownLatch(1);
        String errorPrefix = DocViewBundle.message("template.preview.status.error", "");
        TemplateSettingForm[] formRef = new TemplateSettingForm[1];
        JComponent[] rootRef = new JComponent[1];
        JLabel[] statusRef = new JLabel[1];
        EditorEx[] previewEditorRef = new EditorEx[1];
        JTabbedPane[] workspacesRef = new JTabbedPane[1];

        EdtTestUtil.runInEdtAndWait(() -> {
            TemplateSettingForm form = createForm();
            JComponent root = form.createCenterPanel();
            JPanel previewPanel = component(root, "templatePreviewPage", JPanel.class);
            Component previewComponent = ((BorderLayout) previewPanel.getLayout())
                    .getLayoutComponent(BorderLayout.CENTER);
            EditorEx previewEditor = editorForComponent((JComponent) previewComponent);
            JLabel status = component(root, "previewStatusLabel", JLabel.class);
            JTabbedPane workspaces = component(root, "templateMainTabs", JTabbedPane.class);
            formRef[0] = form;
            rootRef[0] = root;
            statusRef[0] = status;
            previewEditorRef[0] = previewEditor;
            workspacesRef[0] = workspaces;
            status.addPropertyChangeListener("text", event -> {
                if (!(event.getNewValue() instanceof String text)) return;
                if (DocViewBundle.message("template.preview.status.success").equals(text)) {
                    previewCompleted.countDown();
                } else if (DocViewBundle.message("template.preview.status.empty").equals(text)) {
                    emptyPreviewShown.countDown();
                } else if (text.startsWith(errorPrefix) && text.length() > errorPrefix.length()) {
                    failedPreviewShown.countDown();
                }
            });

            component(root, "previewSpringButton", JButton.class).doClick();
            assertEquals(1, workspaces.getSelectedIndex());
            assertEquals(DocViewBundle.message("template.preview.status.loading"), status.getText());
        });

        assertTrue("The real form should show a successful preview before the empty/error cases",
                awaitUiUpdate(previewCompleted));
        EdtTestUtil.runInEdtAndWait(() -> {
            assertEquals(DocViewBundle.message("template.preview.status.success"), statusRef[0].getText());
            assertTrue(previewEditorRef[0].getDocument().getImmutableCharSequence().toString()
                    .contains("/api/demo/users/{userId}"));

            WriteCommandAction.runWriteCommandAction(project,
                    () -> formRef[0].getSpringTemplateEditor().getDocument().setText(""));
            component(rootRef[0], "previewSpringButton", JButton.class).doClick();
            assertEquals(1, workspacesRef[0].getSelectedIndex());
        });

        assertTrue("The real form should show its empty-result status",
                awaitUiUpdate(emptyPreviewShown));
        EdtTestUtil.runInEdtAndWait(() -> {
            assertEquals(DocViewBundle.message("template.preview.status.empty"), statusRef[0].getText());
            assertEquals("An empty result must clear the prior successful Markdown", "",
                    previewEditorRef[0].getDocument().getImmutableCharSequence().toString());

            WriteCommandAction.runWriteCommandAction(project,
                    () -> formRef[0].getSpringTemplateEditor().getDocument()
                            .setText("#if($DocView.name)\nunfinished"));
            component(rootRef[0], "previewSpringButton", JButton.class).doClick();
            assertEquals(1, workspacesRef[0].getSelectedIndex());
        });

        assertTrue("The real form should expose a rendering failure in its status label",
                awaitUiUpdate(failedPreviewShown));
        EdtTestUtil.runInEdtAndWait(() -> {
            assertTrue("The status should include the localized render-error prefix",
                    statusRef[0].getText().startsWith(errorPrefix));
            assertTrue("The status should include a reason after the error prefix",
                    statusRef[0].getText().length() > errorPrefix.length());
            assertEquals("A failed render must not leave old Markdown visible", "",
                    previewEditorRef[0].getDocument().getImmutableCharSequence().toString());
        });
    }

    private static boolean awaitUiUpdate(CountDownLatch update) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (update.getCount() != 0 && System.nanoTime() < deadline) {
            EdtTestUtil.runInEdtAndWait(PlatformTestUtil::dispatchAllEventsInIdeEventQueue);
            update.await(10, TimeUnit.MILLISECONDS);
        }
        return update.getCount() == 0;
    }

    public void testRestoreDefaultsChangesOnlyDraftUntilApply() {
        String savedSpring = "# SAVED_SPRING\n";
        String savedDubbo = "# SAVED_DUBBO\n";
        settings.setSpringTemplate(savedSpring);
        settings.setDubboTemplate(savedDubbo);

        EdtTestUtil.runInEdtAndWait(() -> {
            TemplateSettingForm form = createForm();
            JComponent root = form.createCenterPanel();
            component(root, "restoreDefaultsButton", JButton.class).doClick();

            assertEquals(DocViewBundle.message("template.spring.init"),
                    form.getSpringTemplateEditor().getDocument().getText());
            assertEquals(DocViewBundle.message("template.dubbo.init"),
                    form.getDubboTemplateEditor().getDocument().getText());
            assertEquals(savedSpring, settings.getSpringTemplate());
            assertEquals(savedDubbo, settings.getDubboTemplate());
            assertTrue(form.isModified());

            form.disposeIfNeeded();
            TemplateSettingForm reopened = createForm();
            reopened.createCenterPanel();
            assertEquals(savedSpring, reopened.getSpringTemplateEditor().getDocument().getText());
            assertEquals(savedDubbo, reopened.getDubboTemplateEditor().getDocument().getText());

            component(reopened.createCenterPanel(), "restoreDefaultsButton", JButton.class).doClick();
            reopened.apply();
            assertEquals(DocViewBundle.message("template.spring.init"), settings.getSpringTemplate());
            assertEquals(DocViewBundle.message("template.dubbo.init"), settings.getDubboTemplate());
            assertFalse(reopened.isModified());
        });
    }

    public void testFixedSpringAndDubboExamplesRenderMarkdownWithoutProjectPsi() {
        String spring = TemplatePreviewRenderer.render(project,
                DocViewBundle.message("template.spring.init"), FrameworkEnum.SPRING);
        String dubbo = TemplatePreviewRenderer.render(project,
                DocViewBundle.message("template.dubbo.init"), FrameworkEnum.DUBBO);

        assertTrue(spring.contains("查询示例用户"));
        assertTrue(spring.contains("/api/demo/users/{userId}"));
        assertTrue(spring.contains("42"));
        assertTrue(dubbo.contains("查询示例用户"));
        assertTrue(dubbo.contains("com.example.demo.DemoUserService.findUser"));
        assertTrue(dubbo.contains("示例用户"));
        assertFalse(spring.contains("ERROR"));
        assertFalse(dubbo.contains("ERROR"));
    }

    public void testOnlyLatestPreviewResultIsDeliveredAndFailuresAreVisible() {
        List<Runnable> backgroundTasks = new ArrayList<>();
        List<Runnable> uiTasks = new ArrayList<>();
        List<TemplatePreviewCoordinator.Result> results = new ArrayList<>();
        TemplatePreviewCoordinator coordinator = new TemplatePreviewCoordinator(
                backgroundTasks::add,
                uiTasks::add,
                (template, type) -> {
                    if ("broken".equals(template)) throw new IllegalArgumentException("line 3, column 2: invalid directive");
                    return template + ":" + type;
                });

        coordinator.requestPreview("older", FrameworkEnum.SPRING, results::add);
        backgroundTasks.get(0).run();
        coordinator.requestPreview("latest", FrameworkEnum.DUBBO, results::add);
        backgroundTasks.get(1).run();
        uiTasks.forEach(Runnable::run);

        assertEquals(1, results.size());
        assertEquals("latest:DUBBO", results.get(0).markdown());
        assertTrue(results.get(0).isSuccess());

        coordinator.requestPreview("broken", FrameworkEnum.SPRING, results::add);
        backgroundTasks.get(backgroundTasks.size() - 1).run();
        uiTasks.get(uiTasks.size() - 1).run();
        assertEquals(2, results.size());
        assertFalse(results.get(1).isSuccess());
        assertTrue(results.get(1).errorMessage().contains("line 3, column 2"));

        coordinator.dispose();
    }

    public void testInvalidatedQueuedPreviewDoesNotRenderOrDeliver() {
        List<Runnable> backgroundTasks = new ArrayList<>();
        List<Runnable> uiTasks = new ArrayList<>();
        List<TemplatePreviewCoordinator.Result> results = new ArrayList<>();
        int[] renderCalls = {0};
        TemplatePreviewCoordinator coordinator = new TemplatePreviewCoordinator(
                backgroundTasks::add,
                uiTasks::add,
                (template, framework) -> {
                    renderCalls[0]++;
                    return template;
                });

        coordinator.requestPreview("old draft", FrameworkEnum.SPRING, results::add);
        coordinator.invalidate();
        backgroundTasks.get(0).run();
        uiTasks.forEach(Runnable::run);

        assertEquals("An edited draft must invalidate work before its renderer starts", 0, renderCalls[0]);
        assertTrue("An invalidated preview must not be delivered", results.isEmpty());
        coordinator.dispose();
    }

    public void testTemplatePageAppliesAndResetsOnlyItsOutputSettings() {
        Settings output = Settings.getInstance(project);
        Boolean oldSeparate = output.getSeparateParam();
        Boolean oldMerge = output.getMergeExport();
        String oldPrefix1 = output.getPrefixSymbol1();
        String oldPrefix2 = output.getPrefixSymbol2();
        Boolean oldLineMarker = output.getLineMarker();
        Boolean oldTitleComment = output.getTitleClassComment();
        String oldSpring = settings.getSpringTemplate();
        String oldDubbo = settings.getDubboTemplate();
        try {
            output.setSeparateParam(false);
            output.setMergeExport(true);
            output.setPrefixSymbol1("");
            output.setPrefixSymbol2("-->");
            output.setLineMarker(false);
            output.setTitleClassComment(false);

            EdtTestUtil.runInEdtAndWait(() -> {
                TemplateSettingForm form = createForm();
                JComponent root = form.createCenterPanel();
                JTabbedPane workspaces = component(root, "templateMainTabs", JTabbedPane.class);
                JPanel outputPanel = component(root, "outputSettingsPanel", JPanel.class);
                JCheckBox separate = component(root, "separateParamCheckBox", JCheckBox.class);
                JCheckBox merge = component(root, "mergeExportCheckBox", JCheckBox.class);
                JTextComponent prefix1 = component(root, "prefixSymbol1Field", JTextComponent.class);
                JTextComponent prefix2 = component(root, "prefixSymbol2Field", JTextComponent.class);
                assertNotNull(separate);
                assertNotNull(merge);
                assertNotNull(prefix1);
                assertNotNull(prefix2);
                assertTrue(outputPanel.getLayout() instanceof GridBagLayout);
                workspaces.setSelectedIndex(2);
                assertEquals(DocViewBundle.message("template.tab.output"), workspaces.getTitleAt(workspaces.getSelectedIndex()));
                GridBagLayout outputLayout = (GridBagLayout) outputPanel.getLayout();
                GridBagConstraints prefix1Label = outputLayout.getConstraints(
                        component(root, "prefixSymbol1Label", JLabel.class));
                GridBagConstraints prefix1Input = outputLayout.getConstraints(component(root, "prefixSymbol1Field", Component.class));
                GridBagConstraints prefix2Label = outputLayout.getConstraints(
                        component(root, "prefixSymbol2Label", JLabel.class));
                GridBagConstraints prefix2Input = outputLayout.getConstraints(component(root, "prefixSymbol2Field", Component.class));
                assertEquals(prefix1Label.gridy, prefix1Input.gridy);
                assertEquals(prefix2Label.gridy, prefix2Input.gridy);
                assertTrue(prefix1Input.gridx > prefix1Label.gridx);
                assertTrue(prefix2Input.gridx > prefix2Label.gridx);
                assertFalse(separate.isSelected());
                assertTrue(merge.isSelected());
                assertEquals("", prefix1.getText());
                assertEquals("-->", prefix2.getText());

                separate.setSelected(true);
                merge.setSelected(false);
                prefix1.setText("  :  ");
                prefix2.setText("  >>  ");
                assertTrue("Output edits should be visible to Settings Apply", form.isModified());
                form.apply();

                assertEquals(Boolean.TRUE, output.getSeparateParam());
                assertEquals(Boolean.FALSE, output.getMergeExport());
                assertEquals(":", output.getPrefixSymbol1());
                assertEquals(">>", output.getPrefixSymbol2());
                assertFalse("Applying output settings must not change the root page option", output.getLineMarker());
                assertFalse("Applying output settings must not change a generation rule", output.getTitleClassComment());
                assertFalse(form.isModified());

                separate.setSelected(false);
                merge.setSelected(true);
                prefix1.setText("draft-1");
                prefix2.setText("draft-2");
                form.reset();
                assertTrue(separate.isSelected());
                assertFalse(merge.isSelected());
                assertEquals(":", prefix1.getText());
                assertEquals(">>", prefix2.getText());
                assertEquals(Boolean.TRUE, output.getSeparateParam());
                assertEquals(Boolean.FALSE, output.getMergeExport());
                assertEquals(":", output.getPrefixSymbol1());
                assertEquals(">>", output.getPrefixSymbol2());
                assertFalse(form.isModified());

                separate.setSelected(false);
                prefix1.setText("cancelled draft");
                form.disposeIfNeeded();
                TemplateSettingForm reopened = createForm();
                JComponent reopenedRoot = reopened.createCenterPanel();
                assertTrue(component(reopenedRoot, "separateParamCheckBox", JCheckBox.class).isSelected());
                assertEquals(":", component(reopenedRoot, "prefixSymbol1Field", JTextComponent.class).getText());
                assertEquals(">>", component(reopenedRoot, "prefixSymbol2Field", JTextComponent.class).getText());
                assertEquals(oldSpring, settings.getSpringTemplate());
                assertEquals(oldDubbo, settings.getDubboTemplate());
            });
        } finally {
            output.setSeparateParam(oldSeparate);
            output.setMergeExport(oldMerge);
            output.setPrefixSymbol1(oldPrefix1);
            output.setPrefixSymbol2(oldPrefix2);
            output.setLineMarker(oldLineMarker);
            output.setTitleClassComment(oldTitleComment);
        }
    }

    private TemplateSettingForm createForm() {
        TemplateSettingForm form = new TemplateSettingForm(project);
        forms.add(form);
        return form;
    }

    private EditorEx editorForComponent(JComponent component) {
        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            if (editor.getComponent() == component) {
                assertTrue("The preview editor should be read-only", editor.isViewer());
                return (EditorEx) editor;
            }
        }
        throw new AssertionError("No editor is registered for the preview component");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Component renderSelectedVariable(JList<?> list) {
        ListCellRenderer renderer = list.getCellRenderer();
        return renderer.getListCellRendererComponent((JList) list, list.getSelectedValue(),
                list.getSelectedIndex(), true, false);
    }

    private static <T extends Component> T component(Container root, String name, Class<T> type) {
        if (name.equals(root.getName()) && type.isInstance(root)) return type.cast(root);
        for (Component child : root.getComponents()) {
            if (child.getName() != null && name.equals(child.getName()) && type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T found = component(container, name, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean isDescendant(Component root, Component target) {
        if (root == target) return true;
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                if (child == target) return true;
                if (child instanceof Container nested && isDescendant(nested, target)) return true;
            }
        }
        return false;
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layoutRecursively(nested);
            }
        }
    }
}
