package com.liuzhihang.doc.view.ui;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.IdeBorderFactory;

import javax.swing.*;

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
    private JTextArea templateText;
    private JTextArea descriptionText;


    public TemplateSetting() {

        exportPanel.setBorder(IdeBorderFactory.createTitledBorder("Export Path"));
        templatePanel.setBorder(IdeBorderFactory.createTitledBorder("Markdown Template"));
        descriptionPanel.setBorder(IdeBorderFactory.createTitledBorder("Description"));
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }
}
