package com.liuzhihang.doc.view.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ContextHelpLabel;
import com.liuzhihang.doc.view.DocViewBundle;
import org.jetbrains.annotations.NotNull;

import javax.accessibility.AccessibleContext;

/** Creates the small native help icon used to open a settings guide. */
public final class SettingsGuideHelp {

    private static final String GUIDE_BASE_URL = "https://github.com/liuzhihang/doc-view/blob/master";

    private SettingsGuideHelp() {
    }

    public static @NotNull ContextHelpLabel create(@NotNull Project project,
                                                   @NotNull String componentName,
                                                   @NotNull String descriptionKey,
                                                   @NotNull String linkTextKey,
                                                   @NotNull String githubGuidePath) {
        String description = DocViewBundle.message(descriptionKey);
        String linkText = DocViewBundle.message(linkTextKey);
        String url = GUIDE_BASE_URL + githubGuidePath;
        ContextHelpLabel help = ContextHelpLabel.createWithLink(null, description, linkText, true,
                () -> BrowserUtil.browse(url, project));
        help.setName(componentName);
        AccessibleContext accessible = help.getAccessibleContext();
        accessible.setAccessibleName(linkText);
        accessible.setAccessibleDescription(description);
        return help;
    }
}
