package com.liuzhihang.doc.view.ui.window;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseListener;
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
    private static final String MAINTENANCE_ACTION_GROUP_ID = "liuzhihang.doc.tool.window.maintenance.action";

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        ApplicationManager.getApplication().getMessageBus().connect(toolWindow.getDisposable())
                .subscribe(ProjectCloseListener.TOPIC, new ProjectCloseListener() {
                    @Override
                    public void projectClosing(@NotNull Project project) {
                        if (project == toolWindow.getProject()) {
                            // Native project close has passed its cancellation gates. Remove the
                            // entry before disposal can leave its factory/title in the old manager.
                            toolWindow.remove();
                        }
                    }
                });
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        AnAction maintenanceAction = ActionManager.getInstance().getAction(MAINTENANCE_ACTION_GROUP_ID);
        if (!(maintenanceAction instanceof ActionGroup maintenanceActions)) {
            throw new IllegalStateException("Missing Doc View maintenance action group: "
                    + MAINTENANCE_ACTION_GROUP_ID);
        }
        toolWindow.setAdditionalGearActions(maintenanceActions);

        ContentFactory contentFactory = ContentFactory.getInstance();
        DocViewWindowPanel panel = new DocViewWindowPanel(project, toolWindow);
        Content content = contentFactory.createContent(panel, "", false);
        content.setDisposer(panel);
        toolWindow.getContentManager().addContent(content);

    }

}
