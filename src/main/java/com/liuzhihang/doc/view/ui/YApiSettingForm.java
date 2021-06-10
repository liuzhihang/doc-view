package com.liuzhihang.doc.view.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.YApiSettings;

import javax.swing.*;
import java.awt.*;

public class YApiSettingForm {
    private JPanel rootPanel;

    private JPanel yapiProjectPanel;
    private final JBTextField urlTextField = new JBTextField();
    private final JBTextField projectIdTextField = new JBTextField();
    private final JBTextField tokenTextField = new JBTextField();


    private final Project project;

    public YApiSettingForm(Project project) {
        this.project = project;

        yapiProjectPanel.setBorder(IdeBorderFactory.createTitledBorder(DocViewBundle.message("yapi.project.panel")));

        JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel(DocViewBundle.message("yapi.url")), urlTextField, 1, false)
                .addLabeledComponent(new JBLabel(DocViewBundle.message("yapi.project.id")), projectIdTextField, 1, false)
                .addLabeledComponent(new JBLabel(DocViewBundle.message("yapi.token")), tokenTextField, 1, false)
                .getPanel();

        yapiProjectPanel.add(panel, BorderLayout.CENTER);


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
