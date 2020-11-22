package com.liuzhihang.doc.view.config;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
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

        return new SettingsForm().getRootPanel();
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }


}
