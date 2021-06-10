package com.liuzhihang.doc.view.config;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.ui.YApiSettingForm;
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
public class YApiSettingsConfigurable implements SearchableConfigurable {

    private YApiSettingForm yApiSettingForm;

    private Project project;

    public YApiSettingsConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public String getId() {
        return "liuzhihang.api.doc.YApiSettingsConfigurable";
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "YApi Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {

        yApiSettingForm = new YApiSettingForm(project);

        return yApiSettingForm.getRootPanel();
    }


    @Override
    public boolean isModified() {

        return yApiSettingForm.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {

        yApiSettingForm.apply();
    }

    @Override
    public void reset() {

        yApiSettingForm.reset();
    }


}
