package com.liuzhihang.doc.view.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBTextField;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.YuQueSettings;

import javax.swing.*;

/**
 * @author liuzhihang
 * @date 2022/4/3 11:25
 */
public class YuQueSettingForm {
    private JPanel rootPanel;
    private JPanel yuQueProjectPanel;
    private JBTextField urlTextField;
    private JBTextField loginTextField;
    private JBTextField reposTextField;
    private JBTextField tokenTextField;
    private JBTextField apiUrlTextField;


    private final Project project;

    public YuQueSettingForm(Project project) {
        this.project = project;
        yuQueProjectPanel.setBorder(IdeBorderFactory.createTitledBorder(DocViewBundle.message("yapi.project.panel")));
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }


    public boolean isModified() {

        YuQueSettings settings = YuQueSettings.getInstance(project);

        if (!urlTextField.getText().trim().equals(settings.getUrl())) {
            return true;
        }
        if (!apiUrlTextField.getText().trim().equals(settings.getApiUrl())) {
            return true;
        }

        if (!loginTextField.getText().trim().equals(settings.getLogin())) {
            return true;
        }

        if (!reposTextField.getText().trim().equals(settings.getRepos())) {
            return true;
        }

        if (!tokenTextField.getText().trim().equals(settings.getToken())) {
            return true;
        }

        return false;
    }

    public void apply() throws ConfigurationException {
        YuQueSettings settings = YuQueSettings.getInstance(project);
        String urlTextFieldText = urlTextField.getText().trim();

        if (urlTextFieldText.endsWith("/")) {
            settings.setUrl(urlTextFieldText.substring(0, urlTextFieldText.length() - 1));
        } else {
            settings.setUrl(urlTextFieldText);
        }
        String apiUrlTextFieldText = apiUrlTextField.getText().trim();

        if (apiUrlTextFieldText.endsWith("/")) {
            settings.setApiUrl(apiUrlTextFieldText.substring(0, apiUrlTextFieldText.length() - 1));
        } else {
            settings.setApiUrl(apiUrlTextFieldText);
        }

        settings.setLogin(loginTextField.getText().trim());
        settings.setRepos(reposTextField.getText().trim());
        settings.setToken(tokenTextField.getText().trim());

    }

    public void reset() {
        YuQueSettings settings = YuQueSettings.getInstance(project);
        urlTextField.setText(settings.getUrl());
        apiUrlTextField.setText(settings.getApiUrl());
        tokenTextField.setText(settings.getToken());
        loginTextField.setText(settings.getLogin());
        reposTextField.setText(settings.getRepos());
    }
}
