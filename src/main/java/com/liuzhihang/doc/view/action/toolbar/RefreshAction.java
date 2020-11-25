package com.liuzhihang.doc.view.action.toolbar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.service.ToolWindowService;
import com.liuzhihang.doc.view.tool.DocViewToolWindow;
import com.liuzhihang.doc.view.tool.DocViewToolWindowFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * 工具栏 - 刷新
 *
 * @author lvgorice@gmail.com
 * @version 1.0
 * @date 2020-11-20
 */
public class RefreshAction extends AnAction {

    private final ToolWindowService dataService = ToolWindowService.getInstance();

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        assert dataService != null;
        assert project != null;
        JTree tree = DocViewToolWindowFactory.getDataContext(project).getData(DocViewToolWindow.DOC_VIEW_TREE);
        dataService.loadDocViewTree(project, tree);
    }
}
