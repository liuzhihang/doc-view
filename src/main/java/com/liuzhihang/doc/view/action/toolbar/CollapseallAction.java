package com.liuzhihang.doc.view.action.toolbar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.liuzhihang.doc.view.tool.DocViewToolWindow;
import com.liuzhihang.doc.view.tool.DocViewToolWindowFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * 工具栏 - 收起
 *
 * @author lvgorice@gmail.com
 * @version 1.0
 * @date 2020-11-20
 */
public class CollapseallAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent actionEvent) {
        JTree tree = DocViewToolWindowFactory.getDataContext(actionEvent.getProject()).getData(DocViewToolWindow.DOC_VIEW_TREE);
        tree.collapseRow(0);
    }
}
