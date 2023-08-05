package com.liuzhihang.doc.view.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author liuzhihang
 * @since 2023/8/5 18:17
 */
public class JBSplitterTest extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        new JBSplitterDialogWrapper().showAndGet();
    }
}
