package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.notification.DocViewNotification;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author liuzhihang
 * @date 2020/11/15 14:58
 */
public class ExportUtils {

    /**
     * 批量导出接口文档
     *
     * @param project          当前项目
     * @param generatedDocView 批量生成的接口文档
     */
    public static void bathExportMarkdown(Project project, Map<String, Map<String, DocView>> generatedDocView) {
        // 选择路径
        FileChooserDescriptor fileChooserDescriptor =
                new FileChooserDescriptor(false, true, false, false, false, false);
        VirtualFile chooser = FileChooser.chooseFile(fileChooserDescriptor, project, null);
        if (chooser != null) {
            if (chooser.getChildren().length > 0) {
                DocViewNotification.notifyError(project, DocViewBundle.message("notify.export.batch.file.use.empty"));
                return;
            }
            // 文件夹路径
            StringBuilder path = new StringBuilder(chooser.getPath());
            // 逐个写入磁盘
            int success = 0;
            int fail = 0;
            for (String folder : generatedDocView.keySet()) {
                Map<String, DocView> docViewMap = generatedDocView.get(folder);
                if (new File(path + File.separator + folder).mkdir()) {
                    path.append(File.separator).append(folder);
                }
                for (Map.Entry<String, DocView> docViewEntry : docViewMap.entrySet()) {
                    // 将 docView 按照模版转换
                    DocViewData docViewData = new DocViewData(docViewEntry.getValue());
                    String markdownText = VelocityUtils.convert(TemplateSettings.getInstance(project).getSpringTemplate(), docViewData);
                    File file = new File(path + "/" + docViewEntry.getKey() + ".md");
                    try {
                        FileUtil.writeToFile(file, markdownText);
                        success++;
                    } catch (IOException ioException) {
                        fail++;
                    }
                }
            }
            DocViewNotification.notifyInfo(project, DocViewBundle.message("notify.export.batch.count", success, fail));
        }
    }


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
            } catch (IOException ioException) {
                DocViewNotification.notifyError(project, DocViewBundle.message("notify.export.fail"));
            }

        }

        DocViewNotification.notifyInfo(project, DocViewBundle.message("notify.export.success"));

    }

    public static void allExportMarkdown(Project project, String className, List<DocView> docViewList) {

        // 选择路径
        FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        fileChooserDescriptor.setForcedToUseIdeaFileChooser(true);
        VirtualFile chooser = FileChooser.chooseFile(fileChooserDescriptor, project, null);
        if (chooser != null) {
            String path = chooser.getPath();

            File file = new File(path + "/" + className + ".md");

            // 文件已存在，选择是否覆盖导出。
            if (file.exists() && !DialogUtil.confirm(
                    DocViewBundle.message("notify.export.file.exists"),
                    DocViewBundle.message("notify.export.file.cover"))) {
                return;
            }
            try {

                for (DocView docView : docViewList) {
                    FileUtil.writeToFile(file, DocViewData.buildMarkdownText(project, docView), true);
                }

            } catch (IOException ioException) {
                DocViewNotification.notifyError(project, DocViewBundle.message("notify.export.fail"));
            }

        }

        DocViewNotification.notifyInfo(project, DocViewBundle.message("notify.export.success"));
    }
}
