package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.ui.TreeExpandCollapse;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import org.jetbrains.annotations.NotNull;

/**
 * @author liuzhihang
 * @date 2021/10/23 19:55
 */
public class WindowExpandAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        SimpleTree catalogTree = e.getData(DocViewDataKeys.WINDOW_CATALOG_TREE);

        if (catalogTree == null || project == null) {
            return;
        }
        TreeExpandCollapse.expandAll(catalogTree);
    }
}
