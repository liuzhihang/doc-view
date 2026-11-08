package com.liuzhihang.doc.view.action.toolbar.window.catalog;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.ui.window.MethodNode;
import com.liuzhihang.doc.view.utils.CustomFileUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author liuzhihang
 * @date 2021/10/23 19:55
 */
public class CatalogOpenAction extends AnAction implements DumbAware {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(selectedMethod(event) != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        MethodNode methodNode = selectedMethod(event);
        if (project == null || methodNode == null) {
            return;
        }

        CustomFileUtils.openMd(project, methodNode);
    }

    private static MethodNode selectedMethod(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null || project.isDisposed()) {
            return null;
        }

        SimpleTree tree = event.getData(DocViewDataKeys.WINDOW_CATALOG_TREE);
        SimpleNode selectedNode = tree == null ? null : tree.getSelectedNode();
        if (!(selectedNode instanceof MethodNode methodNode)) {
            return null;
        }

        var method = methodNode.getPsiMethod();
        return method != null && method.isValid() && method.getProject() == project ? methodNode : null;
    }

}
