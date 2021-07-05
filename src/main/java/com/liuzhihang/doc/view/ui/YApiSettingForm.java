package com.liuzhihang.doc.view.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBTextField;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.YApiSettings;
import com.liuzhihang.doc.view.notification.DocViewNotification;

import javax.swing.*;
import java.util.regex.Pattern;

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

        String projectId = projectIdTextField.getText().trim();


        if (!Pattern.compile("^?[0-9]+").matcher(projectId).matches()) {
            return false;
        }

        if (Long.parseLong(projectId) != settings.getProjectId()) {
            return true;
        }

        if (!tokenTextField.getText().equals(settings.getToken())) {
            return true;
        }

        return false;
    }

    public void apply() throws ConfigurationException {
        try {
            YApiSettings settings = YApiSettings.getInstance(project);
            String urlTextFieldText = urlTextField.getText();

            if (urlTextFieldText.endsWith("/")) {
                settings.setUrl(urlTextFieldText.substring(0, urlTextFieldText.length() - 1));
            } else {
                settings.setUrl(urlTextFieldText);
            }

            String projectId = projectIdTextField.getText().trim();
            if (Pattern.compile("^?[0-9]+").matcher(projectId).matches()) {
                settings.setProjectId(Long.parseLong(projectId));
            } else {
                DocViewNotification.notifyError(project, DocViewBundle.message("notify.yapi.project.id"));
            }

            settings.setToken(tokenTextField.getText());
        } catch (NumberFormatException e) {
            throw new ConfigurationException(DocViewBundle.message("notify.yapi.project.id"));
        }
    }

    public void reset() {
        YApiSettings settings = YApiSettings.getInstance(project);
        urlTextField.setText(settings.getUrl());
        if (settings.getProjectId() != null) {
            projectIdTextField.setText(settings.getProjectId().toString());
        }
        tokenTextField.setText(settings.getToken());
    }

}
