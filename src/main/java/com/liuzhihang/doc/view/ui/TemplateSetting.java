package com.liuzhihang.doc.view.ui;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;

/**
 * @author liuzhihang
 * @date 2020/11/20 16:48
 */
public class TemplateSetting {

    private JPanel rootPanel;
    private JPanel exportPanel;
    private TextFieldWithBrowseButton exportPathButton;

    private JPanel templatePanel;
    private JPanel descriptionPanel;


    public TemplateSetting() {

        exportPanel.setBorder(IdeBorderFactory.createTitledBorder("Export Path"));
        templatePanel.setBorder(IdeBorderFactory.createTitledBorder("Markdown Template"));
        descriptionPanel.setBorder(IdeBorderFactory.createTitledBorder("Description"));

        Document templateDocument = EditorFactory.getInstance().createDocument("模版内容");

        FileType mdFileType = FileTypeManager.getInstance().getFileTypeByExtension("md");

        Editor templateEditor = EditorFactory.getInstance().createEditor(templateDocument, null, mdFileType, false);
        EditorSettings templateEditorSettings = templateEditor.getSettings();
        templateEditorSettings.setAdditionalLinesCount(0);
        templateEditorSettings.setAdditionalColumnsCount(0);
        templateEditorSettings.setLineMarkerAreaShown(false);
        templateEditorSettings.setLineNumbersShown(false);
        templateEditorSettings.setVirtualSpace(false);

        JBScrollPane templateScrollPane = new JBScrollPane(templateEditor.getComponent());

        templatePanel.add(templateScrollPane, BorderLayout.CENTER);

    }

    public JPanel getRootPanel() {
        return rootPanel;
    }


    public boolean isModified() {
        return false;
    }

    public void apply() {


    }


    public void reset() {

    }
}
