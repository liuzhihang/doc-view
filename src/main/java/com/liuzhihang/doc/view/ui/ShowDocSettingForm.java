package com.liuzhihang.doc.view.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.ShowDocSettings;
import com.liuzhihang.doc.view.service.IntegrationCredentialService;
import com.liuzhihang.doc.view.utils.DocViewBackgroundTasks;

import javax.swing.*;
import java.awt.FlowLayout;
import java.util.Arrays;

public class ShowDocSettingForm {
    private JPanel rootPanel;
    private JPanel showDocProjectPanel;
    private JLabel urlLabel;
    private JLabel apiKeyLabel;
    private JLabel apiTokenLabel;
    private JBTextField urlTextField;
    private JBPasswordField apiKeyTextField;
    private JBPasswordField apiTokenTextField;
    private JCheckBox clearApiKeyCheckBox;
    private JCheckBox clearApiTokenCheckBox;
    private JLabel credentialStatusLabel;
    private JPanel integrationHelpContainer;

    private final Project project;
    private long statusRequest;
    private boolean disposed;

    public ShowDocSettingForm(Project project) {
        this.project = project;
        urlLabel.setText(DocViewBundle.message("showdoc.url"));
        apiKeyLabel.setText(DocViewBundle.message("showdoc.api.key"));
        apiTokenLabel.setText(DocViewBundle.message("showdoc.api.token"));
        clearApiKeyCheckBox.setText(DocViewBundle.message("integration.credentials.clear"));
        clearApiTokenCheckBox.setText(DocViewBundle.message("integration.credentials.clear"));
        credentialStatusLabel.setText(DocViewBundle.message("integration.credentials.status.loading"));
        integrationHelpContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        integrationHelpContainer.add(SettingsGuideHelp.create(project, "integrationHelpIcon",
                "integration.help.description", "integration.help.open", "/docs/user-guide/integrations.md"));
        showDocProjectPanel.setBorder(IdeBorderFactory.createTitledBorder(DocViewBundle.message("showdoc.project.panel")));
        String secretHint = DocViewBundle.message("integration.credentials.blank.keeps");
        apiKeyTextField.setToolTipText(secretHint);
        apiTokenTextField.setToolTipText(secretHint);
        clearApiKeyCheckBox.addActionListener(event -> setCredentialEnabled(apiKeyTextField, clearApiKeyCheckBox));
        clearApiTokenCheckBox.addActionListener(event -> setCredentialEnabled(apiTokenTextField, clearApiTokenCheckBox));
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    public boolean isModified() {
        ShowDocSettings settings = ShowDocSettings.getInstance(project);
        return !IntegrationSettingValues.normalizedUrl(urlTextField.getText())
                .equals(IntegrationSettingValues.normalizedUrl(settings.getUrl()))
                || hasCredentialMutation(apiKeyTextField, clearApiKeyCheckBox)
                || hasCredentialMutation(apiTokenTextField, clearApiTokenCheckBox);
    }

    public void apply() throws ConfigurationException {
        ShowDocSettings settings = ShowDocSettings.getInstance(project);
        String url = IntegrationSettingValues.normalizedUrl(urlTextField.getText());
        IntegrationCredentialService.CredentialMutation apiKey = credentialMutation(apiKeyTextField, clearApiKeyCheckBox);
        IntegrationCredentialService.CredentialMutation apiToken = credentialMutation(apiTokenTextField, clearApiTokenCheckBox);

        if (apiKey.type() != IntegrationCredentialService.MutationType.KEEP
                || apiToken.type() != IntegrationCredentialService.MutationType.KEEP) {
            IntegrationCredentialService credentials = IntegrationCredentialService.getInstance(project);
            credentials.runCredentialUpdate(DocViewBundle.message("task.credentials.update",
                    DocViewBundle.message("platform.name.showdoc")), () -> credentials.updateShowDoc(apiKey, apiToken));
        }

        settings.setUrl(url);
        urlTextField.setText(url);
        resetSecretDrafts();
        refreshCredentialStatus();
    }

    public void reset() {
        ShowDocSettings settings = ShowDocSettings.getInstance(project);
        urlTextField.setText(IntegrationSettingValues.nullToEmpty(settings.getUrl()));
        resetSecretDrafts();
        refreshCredentialStatus();
    }

    public void dispose() {
        disposed = true;
        statusRequest++;
        resetSecretDrafts();
    }

    private void refreshCredentialStatus() {
        if (disposed || project.isDisposed()) {
            return;
        }
        long request = ++statusRequest;
        credentialStatusLabel.setText(DocViewBundle.message("integration.credentials.status.loading"));
        DocViewBackgroundTasks.runTask(project, DocViewBundle.message("task.credentials.read"), false, indicator -> {
            IntegrationCredentialService.CredentialStatus status = IntegrationCredentialService.getInstance(project)
                    .readStatus(IntegrationCredentialService.Provider.SHOWDOC);
            DocViewBackgroundTasks.invokeLater(project, () -> {
                if (!disposed && statusRequest == request) {
                    credentialStatusLabel.setText(DocViewBundle.message("integration.credentials.status.showdoc",
                            credentialState(status.apiKeyConfigured()), credentialState(status.apiTokenConfigured())));
                }
            });
        }, throwable -> {
            if (!disposed && statusRequest == request) {
                credentialStatusLabel.setText(DocViewBundle.message("integration.credentials.status.unavailable"));
            }
        });
    }

    private void resetSecretDrafts() {
        apiKeyTextField.setText("");
        apiTokenTextField.setText("");
        clearApiKeyCheckBox.setSelected(false);
        clearApiTokenCheckBox.setSelected(false);
        apiKeyTextField.setEnabled(true);
        apiTokenTextField.setEnabled(true);
    }

    private static IntegrationCredentialService.CredentialMutation credentialMutation(
            JBPasswordField field, JCheckBox clearCheckBox) {
        if (clearCheckBox.isSelected()) {
            return IntegrationCredentialService.CredentialMutation.clear();
        }
        char[] value = field.getPassword();
        try {
            return value.length == 0
                    ? IntegrationCredentialService.CredentialMutation.keep()
                    : IntegrationCredentialService.CredentialMutation.replace(new String(value));
        } finally {
            Arrays.fill(value, '\0');
        }
    }

    private static void setCredentialEnabled(JBPasswordField field, JCheckBox clearCheckBox) {
        boolean enabled = !clearCheckBox.isSelected();
        if (!enabled) {
            field.setText("");
        }
        field.setEnabled(enabled);
    }

    private static boolean hasCredentialMutation(JBPasswordField field, JCheckBox clearCheckBox) {
        if (clearCheckBox.isSelected()) {
            return true;
        }
        char[] value = field.getPassword();
        try {
            return value.length > 0;
        } finally {
            Arrays.fill(value, '\0');
        }
    }

    private static String credentialState(boolean configured) {
        return DocViewBundle.message(configured
                ? "integration.credentials.status.configured"
                : "integration.credentials.status.not.configured");
    }
}
