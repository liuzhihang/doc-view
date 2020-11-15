package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.liuzhihang.doc.view.dto.DocView;

import java.io.File;
import java.io.IOException;

/**
 * @author liuzhihang
 * @date 2020/11/15 14:58
 */
public class ExportUtils {

    public static void exportMarkdown(Project project, String fileName, String markdownText) {

        // 选择路径
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        VirtualFile chooser = FileChooser.chooseFile(fileChooserDescriptor, project, null);
        if (chooser != null) {
            String path = chooser.getPath();

            File file = new File(path + "/" + fileName + ".md");

            if (file.exists()) {
                // 文件已存在
                NotificationUtils.errorNotify("文件已存在, 导出 Markdown 失败!", project);
                return;
            }
            try {
                FileUtil.writeToFile(file, markdownText);
            } catch (IOException ioException) {
                NotificationUtils.errorNotify("导出 Markdown 失败!", project);
            }

        }

        NotificationUtils.infoNotify("导出 Markdown 成功!", project);

    }

}
