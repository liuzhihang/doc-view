package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.ui.window.RootNode;
import com.liuzhihang.doc.view.utils.DocViewBackgroundTasks;
import com.liuzhihang.doc.view.utils.DialogUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;

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
        File file = new File(path + "/DocView.md");

        // 文件已存在，选择是否覆盖导出。
        if (file.exists() && !DialogUtil.confirm(
                DocViewBundle.message("notify.export.file.exists"),
                DocViewBundle.message("notify.export.file.cover"))) {
            return;
        }
        DocViewBackgroundTasks.runTask(project, "Doc View export", true, indicator -> {
            for (DocView docView : rootNode.docViewList()) {
                indicator.checkCanceled();
                FileUtil.writeToFile(file, DocViewData.markdownText(project, docView), true);
            }
            DocViewBackgroundTasks.invokeLater(project,
                    () -> DocViewNotification.notifyInfo(project, DocViewBundle.message("notify.export.success")));
        }, throwable -> DocViewNotification.notifyError(project, DocViewBundle.message("notify.export.fail")));
    }
}
