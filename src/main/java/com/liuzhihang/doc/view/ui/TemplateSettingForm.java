package com.liuzhihang.doc.view.ui;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBScrollPane;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.TemplateSettings;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author liuzhihang
 * @date 2020/11/20 16:48
 */
public class TemplateSettingForm {

    private JPanel rootPanel;
    private JPanel exportPanel;
    private TextFieldWithBrowseButton exportPathButton;

    private JPanel templatePanel;
    private JPanel descriptionPanel;

    private final Editor templateEditor;

    private final Project project;

    public TemplateSettingForm(Project project) {
        this.project = project;


        exportPanel.setBorder(IdeBorderFactory.createTitledBorder("Export Path"));
        templatePanel.setBorder(IdeBorderFactory.createTitledBorder("Markdown Template"));
        descriptionPanel.setBorder(IdeBorderFactory.createTitledBorder("Description"));

        Document templateDocument = EditorFactory.getInstance().createDocument(TemplateSettings.getInstance(project).getSpringTemplate());

        // 会使用 velocity 渲染模版
        FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension("md");

        templateEditor = EditorFactory.getInstance().createEditor(templateDocument, project, fileType, false);
        EditorSettings templateEditorSettings = templateEditor.getSettings();
        templateEditorSettings.setAdditionalLinesCount(0);
        templateEditorSettings.setAdditionalColumnsCount(0);
        templateEditorSettings.setLineMarkerAreaShown(false);
        templateEditorSettings.setLineNumbersShown(false);
        templateEditorSettings.setVirtualSpace(false);
        templateEditorSettings.setLanguageSupplier(() -> Language.findLanguageByID("Markdown"));

        JBScrollPane templateScrollPane = new JBScrollPane(templateEditor.getComponent());
        templatePanel.add(templateScrollPane, BorderLayout.CENTER);


        Document descriptionDocument = EditorFactory.getInstance().createDocument(DocViewBundle.message("template.description"));

        Editor descriptionEditor = EditorFactory.getInstance().createEditor(descriptionDocument, project, fileType, true);
        EditorSettings descriptionEditorSettings = descriptionEditor.getSettings();
        descriptionEditorSettings.setAdditionalLinesCount(0);
        descriptionEditorSettings.setAdditionalColumnsCount(0);
        descriptionEditorSettings.setLineMarkerAreaShown(false);
        descriptionEditorSettings.setLineNumbersShown(false);
        descriptionEditorSettings.setVirtualSpace(false);
        descriptionEditorSettings.setLanguageSupplier(() -> Language.findLanguageByID("Markdown"));

        descriptionPanel.add(descriptionEditor.getComponent(), BorderLayout.CENTER);

    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    public boolean isModified() {

        TemplateSettings templateSettings = TemplateSettings.getInstance(project);


        return !templateSettings.getSpringTemplate().equals(templateEditor.getDocument().getText());
    }

    public void apply() {

        TemplateSettings templateSettings = TemplateSettings.getInstance(project);
        templateSettings.setSpringTemplate(templateEditor.getDocument().getText());

    }


    public void reset() {
        TemplateSettings templateSettings = TemplateSettings.getInstance(project);
        templateSettings.setSpringTemplate(DocViewBundle.message("template.spring.init"));
    }
}
