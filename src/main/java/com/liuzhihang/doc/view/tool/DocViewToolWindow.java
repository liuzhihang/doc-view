package com.liuzhihang.doc.view.tool;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author liuzhihang
 * @date 2020/3/24 12:12
 */
public class DocViewToolWindow implements ToolWindowFactory {

    private final Logger logger = Logger.getInstance(this.getClass());

    private ToolWindow toolWindow;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        this.toolWindow = toolWindow;
    }
}
