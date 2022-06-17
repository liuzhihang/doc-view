package com.liuzhihang.doc.view.action.toolbar.window.catalog;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.ui.TreeExpandCollapse;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.ui.window.MethodNode;
import com.liuzhihang.doc.view.utils.CustomFileUtils;

/**
 * window 窗口目录树, 右键生成 HttpClient 功能
 *
 * @author liuzhihang
 * @version CatalogHttpClientAction.java, v 0.1 2022/6/16 17:53 liuzhihang
 */
public class CatalogHttpClientAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        SimpleTree simpleTree = e.getData(DocViewDataKeys.WINDOW_CATALOG_TREE);

        if (simpleTree == null || project == null) {
            return;
        }

        SimpleNode selectedNode = simpleTree.getSelectedNode();

        if (selectedNode instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) selectedNode;
            CustomFileUtils.openHttp(project, methodNode);
        } else {
            TreeExpandCollapse.expandAll(simpleTree);
        }

    }
}
