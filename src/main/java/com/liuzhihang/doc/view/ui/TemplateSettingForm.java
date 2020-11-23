package com.liuzhihang.doc.view.ui;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBScrollPane;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.TemplateSettings;

import javax.swing.*;
import java.awt.*;

/**
 * @author liuzhihang
 * @date 2020/11/20 16:48
 */
public class TemplateSettingForm {

    private JPanel rootPanel;

    private JTabbedPane templateTabledPane;
    private JPanel springTemplatePanel;
    private JPanel descriptionPanel;
    private JPanel dubboTemplatePanel;

    private Editor springTemplateEditor;
    private Editor dubboTemplateEditor;

    private final Project project;

    public TemplateSettingForm(Project project) {
        this.project = project;

        templateTabledPane.setBorder(IdeBorderFactory.createTitledBorder("Markdown Template"));
        descriptionPanel.setBorder(IdeBorderFactory.createTitledBorder("Description"));


        // 会使用 velocity 渲染模版
        FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension("md");

        initSpringTemplatePanel(project, fileType);
        initDubboTemplatePanel(project, fileType);
        initDescriptionPanel(project, fileType);

    }


    /**
     * 初始化 Spring 模版
     *
     * @param project
     * @param fileType
     */
    private void initSpringTemplatePanel(Project project, FileType fileType) {
        Document templateDocument = EditorFactory.getInstance().createDocument(TemplateSettings.getInstance(project).getSpringTemplate());

        springTemplateEditor = EditorFactory.getInstance().createEditor(templateDocument, project, fileType, false);
        initEditorSettingsUI(springTemplateEditor);

        JBScrollPane templateScrollPane = new JBScrollPane(springTemplateEditor.getComponent());
        springTemplatePanel.add(templateScrollPane, BorderLayout.CENTER);
    }

    /**
     * 初始化 Dubbo 模版
     *
     * @param project
     * @param fileType
     */
    private void initDubboTemplatePanel(Project project, FileType fileType) {

        Document document = EditorFactory.getInstance().createDocument(TemplateSettings.getInstance(project).getDubboTemplate());

        dubboTemplateEditor = EditorFactory.getInstance().createEditor(document, project, fileType, false);
        initEditorSettingsUI(dubboTemplateEditor);

        JBScrollPane templateScrollPane = new JBScrollPane(dubboTemplateEditor.getComponent());
        dubboTemplatePanel.add(templateScrollPane, BorderLayout.CENTER);

    }

    /**
     * 显示字段描述
     *
     * @param project
     * @param fileType
     */
    private void initDescriptionPanel(Project project, FileType fileType) {
        Document descriptionDocument = EditorFactory.getInstance().createDocument(DocViewBundle.message("template.description"));

        Editor descriptionEditor = EditorFactory.getInstance().createEditor(descriptionDocument, project, fileType, true);
        initEditorSettingsUI(descriptionEditor);

        descriptionPanel.add(descriptionEditor.getComponent(), BorderLayout.CENTER);
    }


    /**
     * 设置编辑框的 UI
     *
     * @param descriptionEditor
     */
    private void initEditorSettingsUI(Editor descriptionEditor) {
        EditorSettings descriptionEditorSettings = descriptionEditor.getSettings();
        descriptionEditorSettings.setAdditionalLinesCount(0);
        descriptionEditorSettings.setAdditionalColumnsCount(0);
        descriptionEditorSettings.setLineMarkerAreaShown(false);
        descriptionEditorSettings.setLineNumbersShown(false);
        descriptionEditorSettings.setVirtualSpace(false);
        descriptionEditorSettings.setLanguageSupplier(() -> Language.findLanguageByID("Markdown"));
    }


    public JPanel getRootPanel() {
        return rootPanel;
    }

    public boolean isModified() {

        TemplateSettings templateSettings = TemplateSettings.getInstance(project);

        if (!templateSettings.getSpringTemplate().equals(springTemplateEditor.getDocument().getText())) {
            return true;
        }
        if (!templateSettings.getDubboTemplate().equals(dubboTemplateEditor.getDocument().getText())) {
            return true;
        }


        return false;
    }

    public void apply() {

        TemplateSettings templateSettings = TemplateSettings.getInstance(project);
        templateSettings.setSpringTemplate(springTemplateEditor.getDocument().getText());
        templateSettings.setDubboTemplate(dubboTemplateEditor.getDocument().getText());

    }


    public void reset() {
        TemplateSettings templateSettings = TemplateSettings.getInstance(project);
        templateSettings.setSpringTemplate(DocViewBundle.message("template.spring.init"));
        templateSettings.setDubboTemplate(DocViewBundle.message("template.dubbo.init"));
    }
}
