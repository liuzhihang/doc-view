package com.liuzhihang.doc.view.config;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.DocViewBundle;
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
        return DocViewBundle.message("settings.page.yuque");
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (yuQueSettingForm != null) {
            yuQueSettingForm.dispose();
        }
        yuQueSettingForm = new YuQueSettingForm(project);
        return yuQueSettingForm.getRootPanel();
    }


    @Override
    public boolean isModified() {
        return yuQueSettingForm != null && yuQueSettingForm.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        if (yuQueSettingForm != null) {
            yuQueSettingForm.apply();
        }
    }

    @Override
    public void reset() {
        if (yuQueSettingForm != null) {
            yuQueSettingForm.reset();
        }
    }

    @Override
    public void disposeUIResources() {
        if (yuQueSettingForm != null) {
            yuQueSettingForm.dispose();
            yuQueSettingForm = null;
        }
    }

}
