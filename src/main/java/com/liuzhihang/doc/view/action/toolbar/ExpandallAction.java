package com.liuzhihang.doc.view.action.toolbar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.liuzhihang.doc.view.tool.DocViewToolWindow;
import com.liuzhihang.doc.view.tool.DocViewToolWindowFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * 工具栏 - 展开
 *
 * @author lvgorice@gmail.com
 * @version 1.0
 * @date 2020-11-20
 */
public class ExpandallAction extends AnAction {

    /**
     * 最大可见行
     */
    int maxCount = -1;

    @Override
    public void actionPerformed(@NotNull AnActionEvent actionEvent) {
        JTree tree = DocViewToolWindowFactory.getDataContext(actionEvent.getProject()).getData(DocViewToolWindow.DOC_VIEW_TREE);
        int rowCount = tree.getRowCount();
        expandAll(tree, rowCount);
        maxCount = -1;
    }

    /**
     * 展开全部树节点
     *
     * @param tree     树
     * @param rowCount 当前可见节点
     */
    private void expandAll(JTree tree, int rowCount) {
        if (maxCount == rowCount) {
            return;
        }
        for (int i = 0; i < rowCount; i++) {
            tree.expandRow(i);
        }
        maxCount = rowCount;
        expandAll(tree, tree.getRowCount());
    }
}
