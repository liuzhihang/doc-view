package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.utils.CustomFileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;

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

        String tempPath = project.getBasePath()
                + File.separator
                + ".idea"
                + File.separator
                + "doc-view"
                + File.separator
                + "temp"
                + File.separator;

        File file = new File(tempPath);


        CustomFileUtils.delete(file, project);

    }

}
