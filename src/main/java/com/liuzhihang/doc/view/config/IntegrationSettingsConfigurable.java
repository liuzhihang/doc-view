package com.liuzhihang.doc.view.config;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.ui.SettingsGuideHelp;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.FlowLayout;

/** Settings landing page for the supported publishing platforms. */
public final class IntegrationSettingsConfigurable implements SearchableConfigurable {

    private static final String SHOW_DOC_ID = "liuzhihang.api.doc.ShowDocSettingsConfigurable";
    private static final String YAPI_ID = "liuzhihang.api.doc.YApiSettingsConfigurable";
    private static final String YUQUE_ID = "liuzhihang.api.doc.YuQueSettingsConfigurable";

    private final Project project;
    private JPanel rootPanel;

    public IntegrationSettingsConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public String getId() {
        return "liuzhihang.api.doc.IntegrationSettingsConfigurable";
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return DocViewBundle.message("settings.page.integrations");
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        rootPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(JBUI.Borders.empty(8, 12));
        JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        summary.add(new JBLabel(DocViewBundle.message("settings.integrations.summary")));
        ContextHelpLabel guide = SettingsGuideHelp.create(project, "integrationGuideHelpIcon",
                "integration.help.description", "integration.help.open", "/docs/user-guide/integrations.md");
        summary.add(guide);
        content.add(summary);

        JPanel platformLinks = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(8), JBUI.scale(12)));
        platformLinks.add(platformButton(DocViewBundle.message("platform.name.yapi"), YAPI_ID));
        platformLinks.add(platformButton(DocViewBundle.message("platform.name.showdoc"), SHOW_DOC_ID));
        platformLinks.add(platformButton(DocViewBundle.message("platform.name.yuque"), YUQUE_ID));
        content.add(platformLinks);

        rootPanel.add(content);
        return rootPanel;
    }

    private JButton platformButton(String title, String configurableId) {
        JButton button = new JButton(title);
        button.setName(configurableId.substring(configurableId.lastIndexOf('.') + 1) + "Button");
        button.addActionListener(event -> ShowSettingsUtil.getInstance().showSettingsDialog(
                project, configurable -> configurable instanceof SearchableConfigurable searchable
                        && configurableId.equals(searchable.getId()), configurable -> { }));
        return button;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
    }

    @Override
    public void reset() {
    }

    @Override
    public void disposeUIResources() {
        rootPanel = null;
    }
}
