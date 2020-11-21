package com.liuzhihang.doc.view.component;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.liuzhihang.doc.view.ui.TemplateSetting;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 设置末班
 *
 * @author liuzhihang
 * @date 2020/3/4 13:05
 */
public class TemplateConfigurable implements SearchableConfigurable {


    private TemplateSetting templateSetting;

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

        templateSetting = new TemplateSetting();

        return templateSetting.getRootPanel();
    }

    @Override
    public boolean isModified() {

        return templateSetting.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {

        templateSetting.apply();
    }

    @Override
    public void reset() {

        templateSetting.reset();
    }


}
