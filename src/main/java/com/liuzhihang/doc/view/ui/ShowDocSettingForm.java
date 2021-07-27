package com.liuzhihang.doc.view.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBTextField;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.ShowDocSettings;

import javax.swing.*;

public class ShowDocSettingForm {
    private JPanel rootPanel;

    private JPanel showDocProjectPanel;
    private JBTextField urlTextField;
    private JBTextField apiKeyTextField;
    private JBTextField apiTokenTextField;


    private final Project project;

    public ShowDocSettingForm(Project project) {
        this.project = project;
        showDocProjectPanel.setBorder(IdeBorderFactory.createTitledBorder(DocViewBundle.message("showdoc.project.panel")));
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }


    public boolean isModified() {

        ShowDocSettings settings = ShowDocSettings.getInstance(project);

        if (!urlTextField.getText().equals(settings.getUrl())) {
            return true;
        }
        if (!apiKeyTextField.getText().equals(settings.getApiKey())) {
            return true;
        }

        if (!apiTokenTextField.getText().equals(settings.getApiToken())) {
            return true;
        }

        return false;
    }

    public void apply() throws ConfigurationException {
        ShowDocSettings settings = ShowDocSettings.getInstance(project);
        String urlTextFieldText = urlTextField.getText();

        if (urlTextFieldText.endsWith("/")) {
            settings.setUrl(urlTextFieldText.substring(0, urlTextFieldText.length() - 1));
        } else {
            settings.setUrl(urlTextFieldText);
        }
        settings.setApiKey(apiKeyTextField.getText());
        settings.setApiToken(apiTokenTextField.getText());

    }

    public void reset() {

        ShowDocSettings settings = ShowDocSettings.getInstance(project);

        urlTextField.setText(settings.getUrl());
        apiKeyTextField.setText(settings.getApiKey());
        apiTokenTextField.setText(settings.getApiToken());
    }

}
