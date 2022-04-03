package com.liuzhihang.doc.view.config;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.ui.YuQueSettingForm;
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
public class YuQueSettingsConfigurable implements SearchableConfigurable {

    private YuQueSettingForm yuQueSettingForm;

    private final Project project;

    public YuQueSettingsConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public String getId() {
        return "liuzhihang.api.doc.YuQueSettingsConfigurable";
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "YuQue Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {

        yuQueSettingForm = new YuQueSettingForm(project);

        return yuQueSettingForm.getRootPanel();
    }


    @Override
    public boolean isModified() {

        return yuQueSettingForm.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {

        yuQueSettingForm.apply();
    }

    @Override
    public void reset() {

        yuQueSettingForm.reset();
    }


}
