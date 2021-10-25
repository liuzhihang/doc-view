package com.liuzhihang.doc.view.action.toolbar.preview;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.utils.ExportUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Preview 导出全部
 *
 * @author liuzhihang
 * @date 2021/10/23 22:31
 */
public class MenuExportAllAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        JBPopup popup = e.getData(DocViewDataKeys.PREVIEW_POPUP);
        DocView currentDocView = e.getData(DocViewDataKeys.PREVIEW_CURRENT_DOC_VIEW);
        List<DocView> docViewList = e.getData(DocViewDataKeys.PREVIEW_DOC_VIEW_LIST);

        if (popup == null || project == null || currentDocView == null || docViewList == null) {
            return;
        }

        popup.cancel();
        ExportUtils.batchExportMarkdown(project, currentDocView.getPsiClass().getName(), docViewList);

    }
}
