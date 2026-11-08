package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.ui.window.DocViewWindowPanel;
import org.jetbrains.annotations.NotNull;

/** Opens the read-only search view over the current project's committed document index. */
public final class SearchProjectDocsAction extends AnAction implements DumbAware {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        event.getPresentation().setEnabledAndVisible(project != null && !project.isDisposed());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project != null && !project.isDisposed()) {
            DocViewWindowPanel.showProjectDocumentSearch(project);
        }
    }
}
