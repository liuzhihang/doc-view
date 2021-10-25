package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.liuzhihang.doc.view.config.SettingsConfigurable;
import org.jetbrains.annotations.NotNull;

/**
 * @author liuzhihang
 * @date 2021/10/23 19:55
 */
public class WindowSettingsAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(), SettingsConfigurable.class);
    }
}
