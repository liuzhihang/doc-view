package com.liuzhihang.doc.view.action.upload;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.liuzhihang.doc.view.action.AbstractAction;
import com.liuzhihang.doc.view.service.DocViewUploadService;
import org.jetbrains.annotations.NotNull;

/**
 * @author liuzhihang
 * @date 2021/10/23 23:13
 */
public abstract class AbstractUploadAction extends AbstractAction {

    /**
     * @see AnAction#actionPerformed(AnActionEvent)
     */
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 先执行抽象类逻辑
        super.actionPerformed(e);

        // 上传
        uploadService().upload(project, docViewList);

    }

    /**
     * 具体上传服务
     */
    protected abstract DocViewUploadService uploadService();
}
