package com.liuzhihang.doc.view.config;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.ui.TemplateSettingForm;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 设置模版
 *
 * @author liuzhihang
 * @date 2020/3/4 13:05
 */
public class TemplateConfigurable implements SearchableConfigurable, Configurable.NoScroll {

    private TemplateSettingForm templateSettingForm;

    private final Project project;

    public TemplateConfigurable(@NotNull Project project) {
        this.project = project;
    }


    @NotNull
    @Override
    public String getId() {
        return "liuzhihang.api.doc.TemplateConfigurable";
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return DocViewBundle.message("settings.page.template");
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (templateSettingForm == null || templateSettingForm.isDisposed()) {
            templateSettingForm = new TemplateSettingForm(project);
        }

        return templateSettingForm.createCenterPanel();
    }

    @Override
    public boolean isModified() {
        return templateSettingForm != null && templateSettingForm.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        if (templateSettingForm != null) templateSettingForm.apply();
    }

    @Override
    public void reset() {
        if (templateSettingForm != null) templateSettingForm.reset();
    }

    @Override
    public void disposeUIResources() {
        TemplateSettingForm form = templateSettingForm;
        templateSettingForm = null;
        if (form != null) {
            form.disposeIfNeeded();
        }
    }
}
