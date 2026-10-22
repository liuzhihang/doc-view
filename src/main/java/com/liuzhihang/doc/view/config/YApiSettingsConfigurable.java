package com.liuzhihang.doc.view.config;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.DocViewBundle;
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

    private final Project project;

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
        return DocViewBundle.message("settings.page.yapi");
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (yApiSettingForm != null) {
            yApiSettingForm.dispose();
        }
        yApiSettingForm = new YApiSettingForm(project);
        return yApiSettingForm.getRootPanel();
    }


    @Override
    public boolean isModified() {
        return yApiSettingForm != null && yApiSettingForm.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        if (yApiSettingForm != null) {
            yApiSettingForm.apply();
        }
    }

    @Override
    public void reset() {
        if (yApiSettingForm != null) {
            yApiSettingForm.reset();
        }
    }

    @Override
    public void disposeUIResources() {
        if (yApiSettingForm != null) {
            yApiSettingForm.dispose();
            yApiSettingForm = null;
        }
    }

}
