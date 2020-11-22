package com.liuzhihang.doc.view.config;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
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
public class TemplateConfigurable implements SearchableConfigurable {

    private TemplateSettingForm templateSettingForm;

    private Project project;

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
        return "Markdown Template";
    }

    @Nullable
    @Override
    public JComponent createComponent() {

        templateSettingForm = new TemplateSettingForm(project);

        return templateSettingForm.getRootPanel();
    }

    @Override
    public boolean isModified() {

        return templateSettingForm.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {

        templateSettingForm.apply();
    }

    @Override
    public void reset() {

        templateSettingForm.reset();
    }
}
