package com.liuzhihang.doc.view.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author liuzhihang
 * @date 2020/2/28 18:07
 */
public class ProjectViewPreviewAction extends AnAction {


    @Override
    public void actionPerformed(AnActionEvent e) {

    }


    /**
     * 设置在哪里可以使用
     *
     * @param e
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }
}
