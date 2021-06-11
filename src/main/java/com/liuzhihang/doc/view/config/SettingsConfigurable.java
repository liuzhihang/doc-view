package com.liuzhihang.doc.view.config;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.ui.SettingsForm;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author liuzhihang
 * @date 2020/3/4 13:05
 */
public class SettingsConfigurable implements SearchableConfigurable {


    private final Project project;

    private SettingsForm settingsForm;

    public SettingsConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public String getId() {
        return "liuzhihang.api.doc.SettingsConfigurable";
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Doc View";
    }

    @Nullable
    @Override
    public JComponent createComponent() {

        settingsForm = new SettingsForm(project);

        return settingsForm.getRootPanel();
    }

    @Override
    public boolean isModified() {

        return settingsForm.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {

        settingsForm.apply();
    }

    @Override
    public void reset() {
        settingsForm.reset();
    }

}
