package com.liuzhihang.doc.view.tool;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author liuzhihang
 * @date 2020/3/24 12:12
 */
public class DocViewToolWindowFactory implements ToolWindowFactory {

    public static String ID = "Doc View";

    /**
     * 获取上下文
     *
     * @param project 项目参数
     * @return 上下文
     */
    public static DataContext getDataContext(@NotNull Project project) {
        ToolWindow docViewToolWindow = ToolWindowManager.getInstance(project).getToolWindow(ID);
        assert docViewToolWindow != null;
        return DataManager.getInstance().getDataContext(Objects.requireNonNull(docViewToolWindow.getContentManager().getContent(0)).getComponent());
    }

    /**
     * Create the tool window content.
     *
     * @param project    current project
     * @param toolWindow current tool window
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull com.intellij.openapi.wm.ToolWindow toolWindow) {
        DocViewToolWindow docViewToolWindow = new DocViewToolWindow(project);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(docViewToolWindow, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
