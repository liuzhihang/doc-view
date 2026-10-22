package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.ui.window.RootNode;
import com.liuzhihang.doc.view.utils.BatchMarkdownExportFile;
import com.liuzhihang.doc.view.utils.BatchMarkdownExportWriter;
import com.liuzhihang.doc.view.utils.DocViewBackgroundTasks;
import com.liuzhihang.doc.view.utils.DialogUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author liuzhihang
 * @date 2021/10/23 19:55
 */
public class WindowExportAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        JComponent toolbar = e.getData(DocViewDataKeys.WINDOW_TOOLBAR);
        RootNode rootNode = e.getData(DocViewDataKeys.WINDOW_ROOT_NODE);

        if (project == null || toolbar == null || rootNode == null || rootNode.getChildCount() == 0) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.window.export.empty"));
            return;
        }
        // 选择路径
        FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        fileChooserDescriptor.setForcedToUseIdeaFileChooser(true);
        VirtualFile chooser = FileChooser.chooseFile(fileChooserDescriptor, project, null);
        if (chooser == null) {
            return;
        }
        String path = chooser.getPath();

        // 导出到一个文件中
        Path file = Path.of(path, "DocView.md");

        // 文件已存在，选择是否覆盖导出。
        if (Files.exists(file) && !DialogUtil.confirm(
                DocViewBundle.message("notify.export.file.exists"),
                DocViewBundle.message("notify.export.file.cover"))) {
            return;
        }
        DocViewBackgroundTasks.runReadTask(project,
                DocViewBundle.message("task.docview.export.prepare"), true,
                indicator -> {
                    StringBuilder markdown = new StringBuilder();
                    for (var docView : rootNode.docViewList()) {
                        indicator.checkCanceled();
                        markdown.append(DocViewData.markdownText(project, docView));
                    }
                    return markdown.toString();
                }, markdown -> {
                    DocViewBackgroundTasks.runTask(project, DocViewBundle.message("task.docview.export"), true, indicator -> {
                        BatchMarkdownExportWriter.write(Path.of(path),
                                List.of(new BatchMarkdownExportFile("DocView.md", markdown)), indicator);
                        DocViewBackgroundTasks.invokeLater(project,
                                () -> DocViewNotification.notifyInfo(project,
                                        DocViewBundle.message("notify.export.success")));
                    }, throwable -> DocViewNotification.notifyError(project,
                            DocViewBundle.message("notify.export.fail")));
                }, throwable -> DocViewNotification.notifyError(project,
                        DocViewBundle.message("notify.export.fail")));
    }
}
