package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.utils.CustomFileUtils;
import com.liuzhihang.doc.view.utils.StorageUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * @author liuzhihang
 * @date 2021/10/23 19:55
 */
public class WindowClearAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        Path configDir = StorageUtils.getConfigDir(project);
        CustomFileUtils.delete(configDir.toFile(), project);
    }

}
