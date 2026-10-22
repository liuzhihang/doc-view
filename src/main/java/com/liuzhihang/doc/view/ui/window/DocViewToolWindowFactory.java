package com.liuzhihang.doc.view.ui.window;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * @author liuzhihang
 * @date 2021/10/22 16:10
 */
public class DocViewToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        ContentFactory contentFactory = ContentFactory.getInstance();
        DocViewWindowPanel panel = new DocViewWindowPanel(project, toolWindow);
        Content content = contentFactory.createContent(panel, "", false);
        content.setDisposer(panel);
        toolWindow.getContentManager().addContent(content);

    }

}
