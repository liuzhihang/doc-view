package com.liuzhihang.doc.view.ui;

import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBScrollPane;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.utils.EditorUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author liuzhihang
 * @date 2020/11/20 16:48
 */
public class TemplateSettingForm extends DialogWrapper {

    private JPanel rootPanel;

    private JTabbedPane templateTabledPane;
    private JPanel springTemplatePanel;
    private JPanel descriptionPanel;
    private JPanel dubboTemplatePanel;

    private EditorEx springTemplateEditor;
    private EditorEx dubboTemplateEditor;

    private final Project project;

    public TemplateSettingForm(Project project) {
        super(project, true);
        this.project = project;

        init();

        templateTabledPane.setBorder(IdeBorderFactory.createTitledBorder("Markdown Template"));
        descriptionPanel.setBorder(IdeBorderFactory.createTitledBorder("Description"));


        // 会使用 velocity 渲染模版
        FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension("md");

        initSpringTemplatePanel(project, fileType);
        initDubboTemplatePanel(project, fileType);
        initDescriptionPanel(project, fileType);

    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return rootPanel;
    }


    /**
     * 初始化 Spring 模版
     *
     * @param project
     * @param fileType
     */
    private void initSpringTemplatePanel(Project project, FileType fileType) {
        Document templateDocument = EditorFactory.getInstance().createDocument(TemplateSettings.getInstance(project).getSpringTemplate());

        final EditorHighlighter editorHighlighter =
                HighlighterFactory.createHighlighter(fileType, EditorColorsManager.getInstance().getGlobalScheme(), project);


        springTemplateEditor = (EditorEx) EditorFactory.getInstance().createEditor(templateDocument, project, fileType, false);
        EditorUtils.renderMarkdownEditor(springTemplateEditor);

        springTemplateEditor.setHighlighter(editorHighlighter);

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

        dubboTemplateEditor = (EditorEx) EditorFactory.getInstance().createEditor(document, project, fileType, false);
        EditorUtils.renderMarkdownEditor(dubboTemplateEditor);


        final EditorHighlighter editorHighlighter =
                HighlighterFactory.createHighlighter(fileType, EditorColorsManager.getInstance().getGlobalScheme(), project);

        dubboTemplateEditor.setHighlighter(editorHighlighter);

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
        EditorUtils.renderMarkdownEditor((EditorEx) descriptionEditor);

        descriptionPanel.add(descriptionEditor.getComponent(), BorderLayout.CENTER);
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
