package com.liuzhihang.doc.view.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.liuzhihang.doc.view.ui.PreviewForm;

/**
 * 单个类或者方法中操作
 *
 * @author liuzhihang
 * @date 2020/2/26 21:57
 */
public class PreviewAction extends AbstractAction {

    /**
     * @see AnAction#actionPerformed(AnActionEvent)
     */
    @Override
    public void actionPerformed(AnActionEvent e) {
        // 先执行抽象类逻辑
        super.actionPerformed(e);

        // 预览文档
        PreviewForm.getInstance(project, psiFile, targetClass, docViewList).popup();
    }

}
