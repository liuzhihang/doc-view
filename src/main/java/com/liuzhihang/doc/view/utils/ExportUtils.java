package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.notification.DocViewNotification;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author liuzhihang
 * @date 2020/11/15 14:58
 */
public class ExportUtils {

    public static void exportMarkdown(Project project, String fileName, String markdownText) {

        // 选择路径
        FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        fileChooserDescriptor.setForcedToUseIdeaFileChooser(true);
        VirtualFile chooser = FileChooser.chooseFile(fileChooserDescriptor, project, null);
        if (chooser != null) {
            String path = chooser.getPath();

            File file = new File(path + "/" + fileName + ".md");

            // 文件已存在，选择是否覆盖导出。
            if (file.exists() && !DialogUtil.confirm(
                    DocViewBundle.message("notify.export.file.exists"),
                    DocViewBundle.message("notify.export.file.cover"))) {
                return;
            }
            try {
                FileUtil.writeToFile(file, markdownText);
                DocViewNotification.notifyInfo(project, DocViewBundle.message("notify.export.success"));
            } catch (IOException ioException) {
                DocViewNotification.notifyError(project, DocViewBundle.message("notify.export.fail"));
            }

        }


    }

    public static void batchExportMarkdown(Project project, String className, List<DocView> docViewList) {

        Settings settings = Settings.getInstance(project);


        // 选择路径
        FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        fileChooserDescriptor.setForcedToUseIdeaFileChooser(true);
        VirtualFile chooser = FileChooser.chooseFile(fileChooserDescriptor, project, null);

        if (chooser == null) {
            return;
        }

        String path = chooser.getPath();

        if (settings.getMergeExport()) {
            File file = new File(path + "/" + className + ".md");
            if (file.exists() && !DialogUtil.confirm(
                    DocViewBundle.message("notify.export.file.exists"),
                    DocViewBundle.message("notify.export.file.cover"))) {
                return;
            }
        } else {
            for (DocView docView : docViewList) {
                File file = new File(path + "/" + docView.getName() + ".md");
                if (file.exists() && !DialogUtil.confirm(
                        DocViewBundle.message("notify.export.file.exists"),
                        DocViewBundle.message("notify.export.file.cover"))) {
                    return;
                }
            }
        }

        DocViewBackgroundTasks.runTask(project, "Doc View export", true, indicator -> {
            try {
                if (settings.getMergeExport()) {
                    File file = new File(path + "/" + className + ".md");
                    for (DocView docView : docViewList) {
                        indicator.checkCanceled();
                        FileUtil.writeToFile(file, DocViewData.markdownText(project, docView), true);
                    }
                } else {
                    for (DocView docView : docViewList) {
                        indicator.checkCanceled();
                        File file = new File(path + "/" + docView.getName() + ".md");
                        FileUtil.writeToFile(file, DocViewData.markdownText(project, docView), true);
                    }
                }
                DocViewBackgroundTasks.invokeLater(project,
                        () -> DocViewNotification.notifyInfo(project, DocViewBundle.message("notify.export.success")));
            } catch (IOException ioException) {
                DocViewBackgroundTasks.invokeLater(project,
                        () -> DocViewNotification.notifyError(project, DocViewBundle.message("notify.export.fail")));
            }
        }, throwable -> DocViewNotification.notifyError(project, DocViewBundle.message("notify.export.fail")));

    }


}
