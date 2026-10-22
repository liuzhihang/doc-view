package com.liuzhihang.doc.view.ui;

import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.enums.FrameworkEnum;
import com.liuzhihang.doc.view.utils.EditorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Project template editor with variable help and fixed-example Markdown previews. */
public final class TemplateSettingForm extends DialogWrapper {

    private static final List<TemplateVariable> TEMPLATE_VARIABLES = List.of(
            variable("name"), variable("desc"), variable("path"), variable("method"),
            variable("requestHeader"), variable("requestParam"), variable("requestBody"),
            variable("requestExample"), variable("responseParam"), variable("responseExample"),
            variable("type"), variable("requestHeaderDataList"), variable("requestParamDataList"),
            variable("requestBodyDataList"), variable("responseParamDataList"));

    private final Project project;
    private final TemplatePreviewCoordinator previewCoordinator;
    private final Disposable documentListenerLifetime = Disposer.newDisposable("Doc View template listeners");

    private JPanel rootPanel;
    private JTabbedPane templateTabledPane;
    private JTabbedPane workspaceTabs;
    private JPanel editorContentPanel;
    private JPanel variablesPanel;
    private JBSplitter variableSplitter;
    private JPanel previewPanel;
    private JButton toggleVariablesButton;
    private EditorEx springTemplateEditor;
    private EditorEx dubboTemplateEditor;
    private EditorEx previewEditor;
    private JBTextField variableSearchField;
    private JList<TemplateVariable> variableList;
    private DefaultListModel<TemplateVariable> variableModel;
    private JTextArea variableDescriptionArea;
    private JButton insertVariableButton;
    private JLabel previewStatusLabel;
    private javax.swing.JCheckBox separateParamCheckBox;
    private javax.swing.JCheckBox mergeExportCheckBox;
    private JBTextField prefix1Field;
    private JBTextField prefix2Field;
    private com.intellij.openapi.editor.event.DocumentListener templateChangeListener;
    private boolean suppressPreviewInvalidation;
    private boolean variablesVisible;

    public TemplateSettingForm(@NotNull Project project) {
        super(project, true);
        this.project = project;
        this.previewCoordinator = new TemplatePreviewCoordinator(
                AppExecutorUtil.getAppExecutorService(),
                action -> ApplicationManager.getApplication().invokeLater(action, ModalityState.any()),
                (template, framework) -> {
                    if (project.isDisposed()) return "";
                    return TemplatePreviewRenderer.render(project, template, framework);
                });
        init();
    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        if (rootPanel == null) buildUi();
        return rootPanel;
    }

    private void buildUi() {
        rootPanel = new JPanel(new BorderLayout(JBUI.scale(0), JBUI.scale(6)));
        rootPanel.setBorder(JBUI.Borders.empty(8, 12));

        JPanel header = new JPanel(new GridBagLayout());
        header.setName("templateSettingsHeader");
        JTextArea summary = wrappedHint(DocViewBundle.message("template.settings.summary"), "templateSettingsSummary");
        GridBagConstraints summaryConstraints = new GridBagConstraints();
        summaryConstraints.gridx = 0;
        summaryConstraints.gridy = 0;
        summaryConstraints.weightx = 1;
        summaryConstraints.fill = GridBagConstraints.HORIZONTAL;
        summaryConstraints.anchor = GridBagConstraints.LINE_START;
        summaryConstraints.insets = new Insets(0, 0, 0, JBUI.scale(6));
        header.add(summary, summaryConstraints);

        ContextHelpLabel helpLabel = SettingsGuideHelp.create(project, "templateHelpLabel",
                "template.help.description", "template.help.open", "/docs/user-guide/templates.md");
        GridBagConstraints helpConstraints = new GridBagConstraints();
        helpConstraints.gridx = 1;
        helpConstraints.gridy = 0;
        helpConstraints.weightx = 0;
        helpConstraints.anchor = GridBagConstraints.CENTER;
        header.add(helpLabel, helpConstraints);
        rootPanel.add(header, BorderLayout.NORTH);

        FileType markdown = FileTypeManager.getInstance().getFileTypeByExtension("md");
        TemplateSettings settings = TemplateSettings.getInstance(project);
        springTemplateEditor = createEditor(settings.getSpringTemplate(), markdown, false);
        dubboTemplateEditor = createEditor(settings.getDubboTemplate(), markdown, false);
        previewEditor = createEditor("", markdown, true);

        templateTabledPane = new JTabbedPane();
        templateTabledPane.setName("templateFrameworkTabs");
        templateTabledPane.addTab("Spring", templatePanel(springTemplateEditor, FrameworkEnum.SPRING));
        templateTabledPane.addTab("Dubbo", templatePanel(dubboTemplateEditor, FrameworkEnum.DUBBO));
        templateTabledPane.addChangeListener(event -> updateInsertButton());
        templateTabledPane.setBorder(JBUI.Borders.empty(4, 0, 0, 0));

        variablesPanel = createVariablesPanel();
        editorContentPanel = new JPanel(new BorderLayout());
        editorContentPanel.add(templateTabledPane, BorderLayout.CENTER);
        editorContentPanel.setPreferredSize(new Dimension(JBUI.scale(900), JBUI.scale(450)));

        JPanel editorPage = new JPanel(new BorderLayout(JBUI.scale(0), JBUI.scale(4)));
        JPanel editorActions = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0));
        editorActions.setName("templateEditorActions");
        JButton restoreButton = new JButton(DocViewBundle.message("template.restore.defaults"));
        restoreButton.setName("restoreDefaultsButton");
        restoreButton.addActionListener(event -> restoreDefaultsDraft());
        editorActions.add(restoreButton);
        toggleVariablesButton = new JButton(DocViewBundle.message("template.variables.show"));
        toggleVariablesButton.setName("toggleVariablesButton");
        toggleVariablesButton.addActionListener(event -> setVariablesVisible(!variablesVisible));
        editorActions.add(toggleVariablesButton);
        editorPage.add(editorActions, BorderLayout.NORTH);
        editorPage.add(editorContentPanel, BorderLayout.CENTER);

        previewPanel = new JPanel(new BorderLayout(JBUI.scale(4), JBUI.scale(4)));
        previewPanel.setName("templatePreviewPage");
        JPanel previewHeader = new JPanel(new BorderLayout(JBUI.scale(8), 0));
        previewHeader.add(new JBLabel(DocViewBundle.message("template.preview.output")), BorderLayout.WEST);
        previewStatusLabel = new JBLabel(DocViewBundle.message("template.preview.status.initial"));
        previewStatusLabel.setName("previewStatusLabel");
        previewHeader.add(previewStatusLabel, BorderLayout.CENTER);
        previewPanel.add(previewHeader, BorderLayout.NORTH);
        previewPanel.add(previewEditor.getComponent(), BorderLayout.CENTER);
        previewPanel.setBorder(BorderFactory.createEmptyBorder(JBUI.scale(4), 0, 0, 0));

        workspaceTabs = new JTabbedPane();
        workspaceTabs.setName("templateMainTabs");
        workspaceTabs.addTab(DocViewBundle.message("template.tab.editor"), editorPage);
        workspaceTabs.addTab(DocViewBundle.message("template.tab.preview"), previewPanel);
        workspaceTabs.addTab(DocViewBundle.message("template.tab.output"), createOutputSettingsPanel());
        workspaceTabs.setSelectedIndex(0);
        rootPanel.add(workspaceTabs, BorderLayout.CENTER);
        watchTemplateChanges();
        updateInsertButton();
    }

    private JPanel createOutputSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setName("outputSettingsPanel");
        panel.setBorder(IdeBorderFactory.createTitledBorder(DocViewBundle.message("template.output.settings")));

        separateParamCheckBox = new javax.swing.JCheckBox(
                DocViewBundle.message("settings.doc.setting.param.separate"));
        separateParamCheckBox.setName("separateParamCheckBox");
        separateParamCheckBox.setToolTipText(DocViewBundle.message("settings.doc.setting.param.separate.tip"));
        addOutputRow(panel, separateParamCheckBox, 0, 2);

        mergeExportCheckBox = new javax.swing.JCheckBox(DocViewBundle.message("settings.doc.export.merge"));
        mergeExportCheckBox.setName("mergeExportCheckBox");
        mergeExportCheckBox.setToolTipText(DocViewBundle.message("settings.doc.export.merge.tip"));
        addOutputRow(panel, mergeExportCheckBox, 1, 2);

        JLabel prefix1Label = new JBLabel(DocViewBundle.message("settings.doc.prefix.symbol1"));
        prefix1Label.setName("prefixSymbol1Label");
        prefix1Field = new JBTextField(8);
        prefix1Field.setName("prefixSymbol1Field");
        prefix1Field.setToolTipText(DocViewBundle.message("settings.doc.prefix.symbol.tip1"));
        addOutputRow(panel, prefix1Label, prefix1Field, 2);

        JLabel prefix2Label = new JBLabel(DocViewBundle.message("settings.doc.prefix.symbol2"));
        prefix2Label.setName("prefixSymbol2Label");
        prefix2Field = new JBTextField(8);
        prefix2Field.setName("prefixSymbol2Field");
        prefix2Field.setToolTipText(DocViewBundle.message("settings.doc.prefix.symbol.tip2"));
        addOutputRow(panel, prefix2Label, prefix2Field, 3);
        GridBagConstraints fillerConstraints = new GridBagConstraints();
        fillerConstraints.gridx = 0;
        fillerConstraints.gridy = 4;
        fillerConstraints.gridwidth = 2;
        fillerConstraints.weightx = 1;
        fillerConstraints.weighty = 1;
        fillerConstraints.fill = GridBagConstraints.VERTICAL;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        panel.add(filler, fillerConstraints);
        resetOutputSettings(Settings.getInstance(project));
        return panel;
    }

    private static void addOutputRow(JPanel panel, JComponent component, int row, int span) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = span;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.insets = new Insets(JBUI.scale(6), JBUI.scale(8), JBUI.scale(6), JBUI.scale(8));
        panel.add(component, constraints);
    }

    private static void addOutputRow(JPanel panel, JComponent label, JComponent field, int row) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.weightx = 0;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.anchor = GridBagConstraints.LINE_START;
        labelConstraints.insets = new Insets(JBUI.scale(6), JBUI.scale(8), JBUI.scale(6), JBUI.scale(12));
        panel.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.anchor = GridBagConstraints.LINE_START;
        fieldConstraints.insets = new Insets(JBUI.scale(6), 0, JBUI.scale(6), JBUI.scale(8));
        panel.add(field, fieldConstraints);
    }

    private void setVariablesVisible(boolean visible) {
        if (variablesVisible == visible || editorContentPanel == null) return;
        variablesVisible = visible;
        editorContentPanel.removeAll();
        if (visible) {
            templateTabledPane.setMinimumSize(new Dimension(JBUI.scale(360), 0));
            variablesPanel.setMinimumSize(new Dimension(JBUI.scale(240), 0));
            variableSplitter = new OnePixelSplitter(false, 0.75f);
            variableSplitter.setName("templateVariableSplitter");
            variableSplitter.setHonorComponentsMinimumSize(true);
            variableSplitter.setFirstComponent(templateTabledPane);
            variableSplitter.setSecondComponent(variablesPanel);
            variableSplitter.setBorder(JBUI.Borders.empty());
            editorContentPanel.add(variableSplitter, BorderLayout.CENTER);
            toggleVariablesButton.setText(DocViewBundle.message("template.variables.hide"));
        } else {
            variableSplitter = null;
            templateTabledPane.setMinimumSize(new Dimension(0, 0));
            editorContentPanel.add(templateTabledPane, BorderLayout.CENTER);
            toggleVariablesButton.setText(DocViewBundle.message("template.variables.show"));
        }
        editorContentPanel.revalidate();
        editorContentPanel.repaint();
    }

    private void resetOutputSettings(Settings settings) {
        separateParamCheckBox.setSelected(Boolean.TRUE.equals(settings.getSeparateParam()));
        mergeExportCheckBox.setSelected(Boolean.TRUE.equals(settings.getMergeExport()));
        prefix1Field.setText(trimToEmpty(settings.getPrefixSymbol1()));
        prefix2Field.setText(trimToEmpty(settings.getPrefixSymbol2()));
    }

    private JPanel templatePanel(EditorEx editor, FrameworkEnum framework) {
        JPanel panel = new JPanel(new BorderLayout(JBUI.scale(4), JBUI.scale(4)));
        String frameworkName = framework == FrameworkEnum.SPRING ? "Spring" : "Dubbo";
        panel.setName("templatePanel" + frameworkName);
        JPanel controls = new JPanel(new BorderLayout(0, JBUI.scale(4)));
        controls.setName("templatePreviewControls" + frameworkName);
        JPanel previewActions = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0));
        previewActions.setName("previewActions" + frameworkName);
        JButton previewButton = new JButton(DocViewBundle.message(framework == FrameworkEnum.SPRING
                ? "template.preview.spring" : "template.preview.dubbo"));
        previewButton.setName(framework == FrameworkEnum.SPRING ? "previewSpringButton" : "previewDubboButton");
        previewButton.addActionListener(event -> requestPreview(editor, framework));
        previewActions.add(previewButton);
        controls.add(previewActions, BorderLayout.NORTH);
        controls.add(wrappedHint(DocViewBundle.message("template.preview.fixed.sample"),
                "fixedSampleHelp" + frameworkName), BorderLayout.CENTER);
        panel.add(controls, BorderLayout.NORTH);
        panel.add(editor.getComponent(), BorderLayout.CENTER);
        return panel;
    }

    private static JBTextArea wrappedHint(String text, String name) {
        JBTextArea help = new JBTextArea(text);
        help.setName(name);
        help.setEditable(false);
        help.setFocusable(false);
        help.setOpaque(false);
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setRows(2);
        help.setFont(JBUI.Fonts.smallFont());
        help.setForeground(UIUtil.getContextHelpForeground());
        help.setBorder(JBUI.Borders.empty(2, 0, 0, 0));
        return help;
    }

    private void watchTemplateChanges() {
        templateChangeListener = new com.intellij.openapi.editor.event.DocumentListener() {
            @Override
            public void documentChanged(com.intellij.openapi.editor.event.DocumentEvent event) {
                invalidatePreviewForDraftChange();
            }
        };
        springTemplateEditor.getDocument().addDocumentListener(templateChangeListener, documentListenerLifetime);
        dubboTemplateEditor.getDocument().addDocumentListener(templateChangeListener, documentListenerLifetime);
    }

    private void invalidatePreviewForDraftChange() {
        if (suppressPreviewInvalidation) return;
        long invalidation = previewCoordinator.invalidate();
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!previewCoordinator.isCurrent(invalidation) || isDisposed() || project.isDisposed()
                    || previewEditor == null || previewStatusLabel == null) {
                return;
            }
            setPreviewText("");
            previewStatusLabel.setText(DocViewBundle.message("template.preview.status.stale"));
        }, ModalityState.any());
    }

    private JPanel createVariablesPanel() {
        JPanel panel = new JPanel(new BorderLayout(JBUI.scale(4), JBUI.scale(4)));
        panel.setBorder(IdeBorderFactory.createTitledBorder(DocViewBundle.message("template.variables.title")));
        variableSearchField = new JBTextField();
        variableSearchField.setName("variableSearchField");
        variableSearchField.getEmptyText().setText(DocViewBundle.message("template.variables.search"));
        variableSearchField.getAccessibleContext().setAccessibleName(
                DocViewBundle.message("template.variables.search.accessible"));
        variableModel = new DefaultListModel<>();
        variableList = new JList<>(variableModel);
        variableList.setName("variableList");
        variableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        variableList.setCellRenderer((list, value, index, selected, focused) -> {
            JPanel row = new JPanel(new GridLayout(2, 1));
            row.setName("templateVariableRow");
            row.setOpaque(true);
            row.setBorder(JBUI.Borders.empty(3, 6));
            row.setToolTipText(value.expression() + " — " + value.displayName());

            JLabel propertyName = new JBLabel(variablePropertyName(value));
            propertyName.setName("templateVariablePropertyName");
            JLabel displayName = new JBLabel(value.displayName());
            displayName.setName("templateVariableDisplayName");
            propertyName.setFont(list.getFont());
            displayName.setFont(list.getFont());
            row.add(propertyName);
            row.add(displayName);

            Color background = selected ? list.getSelectionBackground() : list.getBackground();
            Color foreground = selected ? list.getSelectionForeground() : list.getForeground();
            row.setBackground(background);
            propertyName.setForeground(foreground);
            displayName.setForeground(foreground);
            return row;
        });
        variableList.addListSelectionListener(event -> {
            updateInsertButton();
            updateVariableDescription();
        });
        variableSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                filterVariables();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                filterVariables();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                filterVariables();
            }
        });

        insertVariableButton = new JButton(DocViewBundle.message("template.variables.insert"));
        insertVariableButton.setName("insertVariableButton");
        insertVariableButton.addActionListener(event -> insertSelectedVariable());

        JPanel searchActions = new JPanel(new BorderLayout(JBUI.scale(4), 0));
        searchActions.add(variableSearchField, BorderLayout.CENTER);
        searchActions.add(insertVariableButton, BorderLayout.EAST);
        panel.add(searchActions, BorderLayout.NORTH);
        panel.add(new JBScrollPane(variableList), BorderLayout.CENTER);

        variableDescriptionArea = new JTextArea(6, 20);
        variableDescriptionArea.setName("variableDescription");
        variableDescriptionArea.setEditable(false);
        variableDescriptionArea.setLineWrap(true);
        variableDescriptionArea.setWrapStyleWord(true);
        variableDescriptionArea.setOpaque(false);
        variableDescriptionArea.setBorder(JBUI.Borders.empty(2, 4));
        variableDescriptionArea.getAccessibleContext().setAccessibleName(
                DocViewBundle.message("template.variables.description.accessible"));
        JBScrollPane descriptionScroll = new JBScrollPane(variableDescriptionArea);
        descriptionScroll.setName("variableDescriptionScroll");
        descriptionScroll.setVerticalScrollBarPolicy(JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        descriptionScroll.setHorizontalScrollBarPolicy(JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        descriptionScroll.setPreferredSize(new Dimension(JBUI.scale(120), JBUI.scale(120)));
        descriptionScroll.setBorder(JBUI.Borders.empty());
        JPanel details = new JPanel(new BorderLayout(JBUI.scale(4), JBUI.scale(2)));
        details.setBorder(IdeBorderFactory.createTitledBorder(
                DocViewBundle.message("template.variables.description.title")));
        details.add(descriptionScroll, BorderLayout.CENTER);
        JBLabel hint = new JBLabel(DocViewBundle.message("template.variables.hint"));
        hint.setName("templateVariableHint");
        hint.setToolTipText(DocViewBundle.message("template.variables.hint.tooltip"));
        details.add(hint, BorderLayout.SOUTH);
        panel.add(details, BorderLayout.SOUTH);
        filterVariables();
        return panel;
    }

    private void updateVariableDescription() {
        if (variableDescriptionArea == null || variableList == null) return;
        TemplateVariable variable = variableList.getSelectedValue();
        variableDescriptionArea.setText(variable == null ? ""
                : variable.expression() + "\n" + variable.displayName() + "\n" + variable.description());
        variableDescriptionArea.setCaretPosition(0);
    }

    private static String variablePropertyName(TemplateVariable variable) {
        String expression = variable.expression();
        String prefix = "${DocView.";
        if (expression.startsWith(prefix) && expression.endsWith("}")) {
            return expression.substring(prefix.length(), expression.length() - 1);
        }
        return expression;
    }

    private EditorEx createEditor(String text, FileType fileType, boolean viewer) {
        Document document = EditorFactory.getInstance().createDocument(text);
        EditorEx editor = (EditorEx) EditorFactory.getInstance().createEditor(document, project, fileType, viewer);
        EditorUtils.renderMarkdownEditor(editor);
        editor.setHighlighter(HighlighterFactory.createHighlighter(
                fileType, EditorColorsManager.getInstance().getGlobalScheme(), project));
        return editor;
    }

    private void filterVariables() {
        if (variableModel == null) return;
        String query = variableSearchField.getText().trim().toLowerCase(Locale.ROOT);
        variableModel.clear();
        for (TemplateVariable variable : TEMPLATE_VARIABLES) {
            String searchable = (variable.expression() + " " + variable.displayName() + " " + variable.description())
                    .toLowerCase(Locale.ROOT);
            if (query.isEmpty() || searchable.contains(query)) variableModel.addElement(variable);
        }
        if (!variableModel.isEmpty()) variableList.setSelectedIndex(0);
        updateInsertButton();
    }

    private void updateInsertButton() {
        if (insertVariableButton != null && variableList != null) {
            insertVariableButton.setEnabled(variableList.getSelectedValue() != null);
        }
    }

    private void insertSelectedVariable() {
        TemplateVariable variable = variableList.getSelectedValue();
        if (variable == null) return;
        EditorEx editor = selectedTemplateEditor();
        boolean selected = editor.getSelectionModel().hasSelection();
        int start = selected ? editor.getSelectionModel().getSelectionStart() : editor.getCaretModel().getOffset();
        int end = selected ? editor.getSelectionModel().getSelectionEnd() : start;
        WriteCommandAction.runWriteCommandAction(project, () ->
                editor.getDocument().replaceString(start, end, variable.expression()));
        editor.getCaretModel().moveToOffset(start + variable.expression().length());
        editor.getContentComponent().requestFocusInWindow();
    }

    private EditorEx selectedTemplateEditor() {
        return templateTabledPane.getSelectedIndex() == 1 ? dubboTemplateEditor : springTemplateEditor;
    }

    private void restoreDefaultsDraft() {
        String spring = DocViewBundle.message("template.spring.init");
        String dubbo = DocViewBundle.message("template.dubbo.init");
        WriteCommandAction.runWriteCommandAction(project, () -> {
            springTemplateEditor.getDocument().setText(spring);
            dubboTemplateEditor.getDocument().setText(dubbo);
        });
    }

    private void requestPreview(EditorEx editor, FrameworkEnum framework) {
        if (isDisposed() || project.isDisposed()) return;
        workspaceTabs.setSelectedComponent(previewPanel);
        setPreviewText("");
        previewStatusLabel.setText(DocViewBundle.message("template.preview.status.loading"));
        previewCoordinator.requestPreview(documentText(editor), framework, result -> {
            if (isDisposed()) return;
            if (!result.isSuccess()) {
                setPreviewText("");
                previewStatusLabel.setText(DocViewBundle.message("template.preview.status.error", result.errorMessage()));
            } else if (result.markdown().isEmpty()) {
                setPreviewText("");
                previewStatusLabel.setText(DocViewBundle.message("template.preview.status.empty"));
            } else {
                setPreviewText(result.markdown());
                previewStatusLabel.setText(DocViewBundle.message("template.preview.status.success"));
            }
        });
    }

    private void setPreviewText(String text) {
        if (previewEditor == null || project.isDisposed()) return;
        WriteCommandAction.runWriteCommandAction(project, () -> previewEditor.getDocument().setText(text));
    }

    private static String documentText(EditorEx editor) {
        return editor.getDocument().getImmutableCharSequence().toString();
    }

    @NotNull
    public static List<TemplateVariable> getTemplateVariables() {
        return TEMPLATE_VARIABLES;
    }

    private static TemplateVariable variable(String property) {
        return new TemplateVariable("${DocView." + property + "}",
                DocViewBundle.message("template.variable." + property),
                DocViewBundle.message("template.variable." + property + ".description"));
    }

    public boolean isModified() {
        if (springTemplateEditor == null || dubboTemplateEditor == null || separateParamCheckBox == null
                || mergeExportCheckBox == null || prefix1Field == null || prefix2Field == null) return false;
        TemplateSettings settings = TemplateSettings.getInstance(project);
        Settings outputSettings = Settings.getInstance(project);
        return !settings.getSpringTemplate().equals(documentText(springTemplateEditor))
                || !settings.getDubboTemplate().equals(documentText(dubboTemplateEditor))
                || separateParamCheckBox.isSelected() != Boolean.TRUE.equals(outputSettings.getSeparateParam())
                || mergeExportCheckBox.isSelected() != Boolean.TRUE.equals(outputSettings.getMergeExport())
                || !Objects.equals(trimToEmpty(outputSettings.getPrefixSymbol1()), prefix1Field.getText().trim())
                || !Objects.equals(trimToEmpty(outputSettings.getPrefixSymbol2()), prefix2Field.getText().trim());
    }

    public void apply() {
        if (springTemplateEditor == null || dubboTemplateEditor == null || separateParamCheckBox == null
                || mergeExportCheckBox == null || prefix1Field == null || prefix2Field == null) return;
        TemplateSettings settings = TemplateSettings.getInstance(project);
        settings.setSpringTemplate(documentText(springTemplateEditor));
        settings.setDubboTemplate(documentText(dubboTemplateEditor));
        Settings outputSettings = Settings.getInstance(project);
        outputSettings.setSeparateParam(separateParamCheckBox.isSelected());
        outputSettings.setMergeExport(mergeExportCheckBox.isSelected());
        outputSettings.setPrefixSymbol1(prefix1Field.getText().trim());
        outputSettings.setPrefixSymbol2(prefix2Field.getText().trim());
        prefix1Field.setText(outputSettings.getPrefixSymbol1());
        prefix2Field.setText(outputSettings.getPrefixSymbol2());
    }

    public void reset() {
        if (springTemplateEditor == null || dubboTemplateEditor == null || separateParamCheckBox == null
                || mergeExportCheckBox == null || prefix1Field == null || prefix2Field == null) return;
        TemplateSettings settings = TemplateSettings.getInstance(project);
        Settings outputSettings = Settings.getInstance(project);
        resetOutputSettings(outputSettings);
        previewCoordinator.invalidate();
        suppressPreviewInvalidation = true;
        try {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                springTemplateEditor.getDocument().setText(settings.getSpringTemplate());
                dubboTemplateEditor.getDocument().setText(settings.getDubboTemplate());
            });
        } finally {
            suppressPreviewInvalidation = false;
        }
        setPreviewText("");
        previewStatusLabel.setText(DocViewBundle.message("template.preview.status.initial"));
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public EditorEx getSpringTemplateEditor() {
        return springTemplateEditor;
    }

    public EditorEx getDubboTemplateEditor() {
        return dubboTemplateEditor;
    }

    @Override
    protected void dispose() {
        previewCoordinator.dispose();
        Disposer.dispose(documentListenerLifetime);
        templateChangeListener = null;
        Editor[] editors = {springTemplateEditor, dubboTemplateEditor, previewEditor};
        springTemplateEditor = null;
        dubboTemplateEditor = null;
        previewEditor = null;
        rootPanel = null;
        templateTabledPane = null;
        workspaceTabs = null;
        editorContentPanel = null;
        variablesPanel = null;
        variableSplitter = null;
        previewPanel = null;
        toggleVariablesButton = null;
        try {
            for (Editor editor : editors) {
                if (editor != null && !editor.isDisposed()) {
                    EditorFactory.getInstance().releaseEditor(editor);
                }
            }
        } finally {
            super.dispose();
        }
    }

    public record TemplateVariable(String expression, String displayName, String description) {
        @Override
        public String toString() {
            return expression;
        }
    }
}
