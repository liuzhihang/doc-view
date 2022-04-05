package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.ui.window.DocViewWindowPanel;
import org.jetbrains.annotations.NotNull;

/**
 * @author liuzhihang
 * @date 2021/10/23 19:55
 */
public class WindowRefreshAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        DocViewWindowPanel docViewWindowPanel = e.getData(DocViewDataKeys.WINDOW_PANE);

        if (docViewWindowPanel == null || project == null) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Doc View", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {

                progressIndicator.setText("Directory refreshing");
                docViewWindowPanel.updateCatalogTree();
            }
        });
    }
}
