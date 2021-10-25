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

/**
 * Preview 右侧单个导出
 *
 * @author liuzhihang
 * @date 2021/10/23 22:31
 */
public class PreviewRightExportAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        JBPopup popup = e.getData(DocViewDataKeys.PREVIEW_POPUP);
        DocView currentDocView = e.getData(DocViewDataKeys.PREVIEW_CURRENT_DOC_VIEW);
        String currentMarkdownText = e.getData(DocViewDataKeys.PREVIEW_MARKDOWN_TEXT);

        if (popup == null || project == null || currentDocView == null || currentMarkdownText == null) {
            return;
        }
        popup.cancel();
        ExportUtils.exportMarkdown(project, currentDocView.getName(), currentMarkdownText);
    }

}
