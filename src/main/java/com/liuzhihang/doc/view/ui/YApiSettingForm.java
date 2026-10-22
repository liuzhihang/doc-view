package com.liuzhihang.doc.view.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.YApiSettings;
import com.liuzhihang.doc.view.service.IntegrationCredentialService;
import com.liuzhihang.doc.view.utils.DocViewBackgroundTasks;

import javax.swing.*;
import java.awt.FlowLayout;
import java.util.Arrays;

public class YApiSettingForm {
    private JPanel rootPanel;
    private JPanel yapiProjectPanel;
    private JLabel urlLabel;
    private JLabel projectIdLabel;
    private JLabel tokenLabel;
    private JBTextField urlTextField;
    private JBTextField projectIdTextField;
    private JBPasswordField tokenTextField;
    private JCheckBox clearTokenCheckBox;
    private JLabel credentialStatusLabel;
    private JPanel integrationHelpContainer;

    private final Project project;
    private long statusRequest;
    private boolean disposed;

    public YApiSettingForm(Project project) {
        this.project = project;
        urlLabel.setText(DocViewBundle.message("yapi.url"));
        projectIdLabel.setText(DocViewBundle.message("yapi.project.id"));
        tokenLabel.setText(DocViewBundle.message("yapi.token"));
        clearTokenCheckBox.setText(DocViewBundle.message("integration.credentials.clear"));
        credentialStatusLabel.setText(DocViewBundle.message("integration.credentials.status.loading"));
        integrationHelpContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        integrationHelpContainer.add(SettingsGuideHelp.create(project, "integrationHelpIcon",
                "integration.help.description", "integration.help.open", "/docs/user-guide/integrations.md"));
        yapiProjectPanel.setBorder(IdeBorderFactory.createTitledBorder(DocViewBundle.message("yapi.project.panel")));
        tokenTextField.setToolTipText(DocViewBundle.message("integration.credentials.blank.keeps"));
        clearTokenCheckBox.addActionListener(event -> setCredentialEnabled());
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    public boolean isModified() {
        YApiSettings settings = YApiSettings.getInstance(project);
        String projectId = IntegrationSettingValues.trimmedOrEmpty(projectIdTextField.getText());
        String savedProjectId = settings.getProjectId() == null ? "" : settings.getProjectId().toString();
        return !IntegrationSettingValues.normalizedUrl(urlTextField.getText())
                .equals(IntegrationSettingValues.normalizedUrl(settings.getUrl()))
                || !projectId.equals(savedProjectId)
                || hasCredentialMutation();
    }

    public void apply() throws ConfigurationException {
        String url = IntegrationSettingValues.normalizedUrl(urlTextField.getText());
        Long projectId = parseProjectId(projectIdTextField.getText());
        IntegrationCredentialService.CredentialMutation token = credentialMutation();

        if (token.type() != IntegrationCredentialService.MutationType.KEEP) {
            IntegrationCredentialService credentials = IntegrationCredentialService.getInstance(project);
            credentials.runCredentialUpdate(DocViewBundle.message("task.credentials.update",
                    DocViewBundle.message("platform.name.yapi")), () -> credentials.updateToken(
                    IntegrationCredentialService.Provider.YAPI, token));
        }

        YApiSettings settings = YApiSettings.getInstance(project);
        settings.setUrl(url);
        settings.setProjectId(projectId);
        urlTextField.setText(url);
        projectIdTextField.setText(projectId == null ? "" : projectId.toString());
        resetSecretDraft();
        refreshCredentialStatus();
    }

    private static Long parseProjectId(String rawProjectId) throws ConfigurationException {
        String projectId = IntegrationSettingValues.trimmedOrEmpty(rawProjectId);
        if (projectId.isEmpty()) {
            return null;
        }
        if (!projectId.matches("[0-9]+")) {
            throw new ConfigurationException(DocViewBundle.message("notify.yapi.project.id"));
        }

        try {
            long parsedProjectId = Long.parseLong(projectId);
            if (parsedProjectId <= 0) {
                throw new ConfigurationException(DocViewBundle.message("notify.yapi.project.id"));
            }
            return parsedProjectId;
        } catch (NumberFormatException exception) {
            throw new ConfigurationException(DocViewBundle.message("notify.yapi.project.id"));
        }
    }

    public void reset() {
        YApiSettings settings = YApiSettings.getInstance(project);
        urlTextField.setText(IntegrationSettingValues.nullToEmpty(settings.getUrl()));
        projectIdTextField.setText(settings.getProjectId() == null ? "" : settings.getProjectId().toString());
        resetSecretDraft();
        refreshCredentialStatus();
    }

    public void dispose() {
        disposed = true;
        statusRequest++;
        resetSecretDraft();
    }

    private void refreshCredentialStatus() {
        if (disposed || project.isDisposed()) {
            return;
        }
        long request = ++statusRequest;
        credentialStatusLabel.setText(DocViewBundle.message("integration.credentials.status.loading"));
        DocViewBackgroundTasks.runTask(project, DocViewBundle.message("task.credentials.read"), false, indicator -> {
            IntegrationCredentialService.CredentialStatus status = IntegrationCredentialService.getInstance(project)
                    .readStatus(IntegrationCredentialService.Provider.YAPI);
            DocViewBackgroundTasks.invokeLater(project, () -> {
                if (!disposed && statusRequest == request) {
                    credentialStatusLabel.setText(DocViewBundle.message(status.apiTokenConfigured()
                            ? "integration.credentials.status.configured"
                            : "integration.credentials.status.not.configured"));
                }
            });
        }, throwable -> {
            if (!disposed && statusRequest == request) {
                credentialStatusLabel.setText(DocViewBundle.message("integration.credentials.status.unavailable"));
            }
        });
    }

    private void resetSecretDraft() {
        tokenTextField.setText("");
        clearTokenCheckBox.setSelected(false);
        tokenTextField.setEnabled(true);
    }

    private IntegrationCredentialService.CredentialMutation credentialMutation() {
        if (clearTokenCheckBox.isSelected()) {
            return IntegrationCredentialService.CredentialMutation.clear();
        }
        char[] value = tokenTextField.getPassword();
        try {
            return value.length == 0
                    ? IntegrationCredentialService.CredentialMutation.keep()
                    : IntegrationCredentialService.CredentialMutation.replace(new String(value));
        } finally {
            Arrays.fill(value, '\0');
        }
    }

    private boolean hasCredentialMutation() {
        if (clearTokenCheckBox.isSelected()) {
            return true;
        }
        char[] value = tokenTextField.getPassword();
        try {
            return value.length > 0;
        } finally {
            Arrays.fill(value, '\0');
        }
    }

    private void setCredentialEnabled() {
        boolean enabled = !clearTokenCheckBox.isSelected();
        if (!enabled) {
            tokenTextField.setText("");
        }
        tokenTextField.setEnabled(enabled);
    }
}
