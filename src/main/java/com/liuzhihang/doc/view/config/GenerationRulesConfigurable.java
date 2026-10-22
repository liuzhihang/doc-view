package com.liuzhihang.doc.view.config;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.ui.GenerationRulesForm;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

public final class GenerationRulesConfigurable implements SearchableConfigurable {

    private final Project project;
    private GenerationRulesForm form;

    public GenerationRulesConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public String getId() {
        return "liuzhihang.api.doc.GenerationRulesConfigurable";
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return DocViewBundle.message("settings.page.rules");
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        form = new GenerationRulesForm(project);
        return form.getRootPanel();
    }

    @Override
    public boolean isModified() {
        return form != null && form.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        if (form != null) {
            form.apply();
        }
    }

    @Override
    public void reset() {
        if (form != null) {
            form.reset();
        }
    }

    @Override
    public void disposeUIResources() {
        form = null;
    }
}
