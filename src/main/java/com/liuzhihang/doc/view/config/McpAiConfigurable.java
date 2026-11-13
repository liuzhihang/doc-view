package com.liuzhihang.doc.view.config;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.ui.McpAiSettingsPanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.ScrollPaneConstants;

/** Project settings for the Doc View Codex Skill and MCP workflow. */
public final class McpAiConfigurable implements SearchableConfigurable, Configurable.NoScroll {

    private final Project project;
    private McpAiSettingsPanel panel;

    public McpAiConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public String getId() {
        return "doc.view.mcp.ai";
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return DocViewBundle.message("settings.page.mcp.ai");
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (panel == null || panel.isDisposed()) {
            panel = new McpAiSettingsPanel(project);
        }
        JBScrollPane scrollPane = new JBScrollPane(panel.getRootPanel());
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
        // This page has no persistent preference fields. File operations run only from explicit buttons.
    }

    @Override
    public void reset() {
        // The executable selection is a per-view operation input, not a saved Codex preference.
    }

    @Override
    public void disposeUIResources() {
        McpAiSettingsPanel current = panel;
        panel = null;
        if (current != null) current.dispose();
    }
}
