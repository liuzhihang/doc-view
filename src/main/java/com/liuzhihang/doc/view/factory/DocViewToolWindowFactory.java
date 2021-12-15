package com.liuzhihang.doc.view.factory;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.liuzhihang.doc.view.ui.window.DocViewWindowPanel;
import org.jetbrains.annotations.NotNull;

/**
 * @author liuzhihang
 * @date 2021/10/22 16:10
 */
public class DocViewToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(new DocViewWindowPanel(project), "", false);
        toolWindow.getContentManager().addContent(content);

    }

}
