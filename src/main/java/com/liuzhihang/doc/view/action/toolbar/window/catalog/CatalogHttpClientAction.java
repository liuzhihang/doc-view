package com.liuzhihang.doc.view.action.toolbar.window.catalog;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.service.http.HttpRequestService;
import com.liuzhihang.doc.view.ui.window.MethodNode;
import com.liuzhihang.doc.view.utils.CustomFileUtils;

/**
 * window 窗口目录树, 右键生成 HttpClient 功能
 *
 * @author liuzhihang
 * @version CatalogHttpClientAction.java, v 0.1 2022/6/16 17:53 liuzhihang
 */
public class CatalogHttpClientAction extends DumbAwareAction {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(AnActionEvent event) {
        Project project = event.getProject();
        SimpleTree tree = event.getData(DocViewDataKeys.WINDOW_CATALOG_TREE);
        MethodNode method = tree != null && tree.getSelectedNode() instanceof MethodNode node ? node : null;
        boolean applicable = project != null && !project.isDisposed() && method != null
                && method.isHttpEndpoint() && method.getPsiMethod().isValid()
                && method.getPsiMethod().getProject() == project;
        event.getPresentation().setEnabledAndVisible(applicable);
        event.getPresentation().setDescription(DocViewBundle.message(
                "action.liuzhihang.doc.view.action.toolbar.window.catalog.httpclient.description"));
        if (applicable && (DumbService.isDumb(project) || !HttpRequestService.isHttpClientAvailable())) {
            event.getPresentation().setEnabled(false);
            event.getPresentation().setDescription(DocViewBundle.message(DumbService.isDumb(project)
                    ? "http.request.unavailable.indexing" : "http.request.unavailable.client"));
        }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        SimpleTree simpleTree = e.getData(DocViewDataKeys.WINDOW_CATALOG_TREE);

        if (simpleTree == null || project == null || project.isDisposed()) {
            return;
        }

        SimpleNode selectedNode = simpleTree.getSelectedNode();

        if (selectedNode instanceof MethodNode methodNode && methodNode.isHttpEndpoint()
                && methodNode.getPsiMethod().isValid() && methodNode.getPsiMethod().getProject() == project) {
            CustomFileUtils.openHttp(project, methodNode);
        }

    }
}
