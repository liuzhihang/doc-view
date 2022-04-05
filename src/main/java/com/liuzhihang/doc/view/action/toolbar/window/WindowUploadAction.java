package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.action.toolbar.AbstractToolbarUploadAction;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewUploadService;
import com.liuzhihang.doc.view.ui.window.RootNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author liuzhihang
 * @date 2021/10/23 19:55
 */
public class WindowUploadAction extends AbstractToolbarUploadAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        JComponent toolbar = e.getData(DocViewDataKeys.WINDOW_TOOLBAR);
        RootNode rootNode = e.getData(DocViewDataKeys.WINDOW_ROOT_NODE);

        if (project == null || toolbar == null || rootNode == null || rootNode.getChildCount() == 0) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.window.upload.empty"));
            return;
        }

        JBPopupFactory.getInstance()
                .createListPopup(new BaseListPopupStep<>(null, DocViewUploadService.UPLOAD_OPTIONS) {
                    @Override
                    public @NotNull String getTextFor(String value) {
                        return "Upload to " + value;
                    }

                    @Override
                    public @Nullable PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {

                        DocViewUploadService.getInstance(selectedValue).upload(project, rootNode.docViewList());

                        return FINAL_CHOICE;
                    }
                }).showUnderneathOf(toolbar);


    }


}
