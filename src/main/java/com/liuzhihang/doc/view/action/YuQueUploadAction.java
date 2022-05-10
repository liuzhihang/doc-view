package com.liuzhihang.doc.view.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.liuzhihang.doc.view.service.DocViewUploadService;
import com.liuzhihang.doc.view.service.impl.YuQueServiceImpl;
import org.jetbrains.annotations.NotNull;

/**
 * 语雀 上传
 *
 * @author liuzhihang
 * @date 2021/6/8 16:40
 */
public class YuQueUploadAction extends AbstractUploadAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);
    }

    @Override
    protected DocViewUploadService uploadService() {
        return ApplicationManager.getApplication().getService(YuQueServiceImpl.class);
    }

    /**
     * 设置右键菜单是否隐藏 上传按钮
     *
     * @param e
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }

}
