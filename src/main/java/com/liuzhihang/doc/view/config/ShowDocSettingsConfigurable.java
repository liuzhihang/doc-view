package com.liuzhihang.doc.view.config;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.ui.ShowDocSettingForm;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * YApi 配置
 *
 * @author liuzhihang
 * @date 2021/6/8 17:10
 */
public class ShowDocSettingsConfigurable implements SearchableConfigurable {

    private ShowDocSettingForm showDocSettingForm;

    private final Project project;

    public ShowDocSettingsConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public String getId() {
        return "liuzhihang.api.doc.ShowDocSettingsConfigurable";
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "ShowDoc Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {

        showDocSettingForm = new ShowDocSettingForm(project);

        return showDocSettingForm.getRootPanel();
    }


    @Override
    public boolean isModified() {

        return showDocSettingForm.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {

        showDocSettingForm.apply();
    }

    @Override
    public void reset() {

        showDocSettingForm.reset();
    }


}
