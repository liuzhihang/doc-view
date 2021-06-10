package com.liuzhihang.doc.view.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBTextField;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.YApiSettings;

import javax.swing.*;

public class YApiSettingForm {
    private JPanel rootPanel;

    private JPanel yapiProjectPanel;
    private JBTextField urlTextField;
    private JBTextField projectIdTextField;
    private JBTextField tokenTextField;


    private final Project project;

    public YApiSettingForm(Project project) {
        this.project = project;
        yapiProjectPanel.setBorder(IdeBorderFactory.createTitledBorder(DocViewBundle.message("yapi.project.panel")));
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }


    public boolean isModified() {

        YApiSettings settings = YApiSettings.getInstance(project);

        if (!urlTextField.getText().equals(settings.getUrl())) {
            return true;
        }

        if (Long.parseLong(projectIdTextField.getText()) != settings.getProjectId()) {
            return true;
        }

        if (!tokenTextField.getText().equals(settings.getToken())) {
            return true;
        }

        return false;
    }

    public void apply() {
        YApiSettings settings = YApiSettings.getInstance(project);
        String urlTextFieldText = urlTextField.getText();

        if (urlTextFieldText.endsWith("/")) {
            settings.setUrl(urlTextFieldText.substring(0, urlTextFieldText.length() - 1));
        } else {
            settings.setUrl(urlTextFieldText);
        }
        settings.setProjectId(Long.parseLong(projectIdTextField.getText()));
        settings.setToken(tokenTextField.getText());
    }

    public void reset() {
        YApiSettings settings = YApiSettings.getInstance(project);
        urlTextField.setText(settings.getUrl());
        projectIdTextField.setText(settings.getProjectId().toString());
        tokenTextField.setText(settings.getToken());
    }

}
