package com.liuzhihang.doc.view.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.ui.welcome.DocViewWelcomeService;
import org.jetbrains.annotations.NotNull;

public final class DocViewWelcomeAction extends DumbAwareAction {
    public DocViewWelcomeAction() {
        super(DocViewBundle.message("welcome.action.title"));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        if (event.getProject() != null) DocViewWelcomeService.getInstance(event.getProject()).open("welcome");
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setText(DocViewBundle.message("welcome.action.title"));
        event.getPresentation().setDescription(DocViewBundle.message("welcome.action.description"));
        event.getPresentation().setEnabledAndVisible(event.getProject() != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
