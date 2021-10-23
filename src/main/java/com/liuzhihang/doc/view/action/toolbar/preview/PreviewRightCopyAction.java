package com.liuzhihang.doc.view.action.toolbar.preview;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

/**
 * Preview 右侧复制
 *
 * @author liuzhihang
 * @date 2021/10/23 22:31
 */
public class PreviewRightCopyAction extends AnAction {
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

        StringSelection selection = new StringSelection(currentMarkdownText);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);

        DocViewNotification.notifyInfo(project, DocViewBundle.message("notify.copy.success", currentDocView.getName()));

    }


}
