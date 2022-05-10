package com.liuzhihang.doc.view.action.toolbar.window.catalog;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.liuzhihang.doc.view.service.DocViewUploadService;
import com.liuzhihang.doc.view.service.impl.YuQueServiceImpl;
import org.jetbrains.annotations.NotNull;

/**
 * @author liuzhihang
 * @date 2021/10/23 19:55
 */
public class CatalogUploadYuQueAction extends AbstractCatalogUploadAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);
    }

    @Override
    protected DocViewUploadService uploadService() {
        return ApplicationManager.getApplication().getService(YuQueServiceImpl.class);
    }

}
