package com.liuzhihang.doc.view.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.liuzhihang.doc.view.service.DocViewUploadService;
import com.liuzhihang.doc.view.service.impl.YApiServiceImpl;
import org.jetbrains.annotations.NotNull;

/**
 * YApi 上传
 *
 * @author liuzhihang
 * @date 2021/6/8 16:40
 */
public class YApiUploadAction extends AbstractUploadAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);
    }

    @Override
    protected DocViewUploadService uploadService() {
        return ApplicationManager.getApplication().getService(YApiServiceImpl.class);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }
}
