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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

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

    public static void batchExportMarkdown(Project project, String className, Supplier<List<DocView>> docViewSupplier) {
        boolean merge = Boolean.TRUE.equals(Settings.getInstance(project).getMergeExport());
        DocViewBackgroundTasks.runReadTask(project, DocViewBundle.message("task.docview.export.prepare"), true,
                indicator -> renderBatch(project, className, docViewSupplier.get(), merge, indicator),
                files -> {
                    if (files.isEmpty()) {
                        DocViewNotification.notifyInfo(project, DocViewBundle.message("notify.export.batch.empty"));
                        return;
                    }
                    FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
                    fileChooserDescriptor.setForcedToUseIdeaFileChooser(true);
                    VirtualFile chooser = FileChooser.chooseFile(fileChooserDescriptor, project, null);
                    if (chooser == null) {
                        return;
                    }
                    Path outputDirectory = Path.of(chooser.getPath());
                    for (BatchMarkdownExportFile file : files) {
                        Path target = outputDirectory.resolve(file.fileName());
                        if (Files.exists(target) && !DialogUtil.confirm(
                                DocViewBundle.message("notify.export.file.exists"),
                                DocViewBundle.message("notify.export.file.cover"))) {
                            return;
                        }
                    }
                    DocViewBackgroundTasks.runTask(project, DocViewBundle.message("task.docview.export"), true,
                            indicator -> {
                                BatchMarkdownExportWriter.write(outputDirectory, files, indicator);
                                DocViewBackgroundTasks.invokeLater(project,
                                        () -> DocViewNotification.notifyInfo(project,
                                                DocViewBundle.message("notify.export.success")));
                            }, throwable -> DocViewNotification.notifyError(project,
                                    DocViewBundle.message("notify.export.fail")));
                }, throwable -> DocViewNotification.notifyError(project,
                        DocViewBundle.message("notify.export.fail")));
    }

    static List<BatchMarkdownExportFile> renderBatch(Project project,
                                                     String className,
                                                     List<DocView> docViews,
                                                     boolean merge,
                                                     com.intellij.openapi.progress.ProgressIndicator indicator) {
        if (docViews.isEmpty()) {
            return List.of();
        }
        Map<String, StringBuilder> markdownByFileName = new LinkedHashMap<>();
        if (merge) {
            StringBuilder combined = new StringBuilder();
            for (DocView docView : docViews) {
                indicator.checkCanceled();
                combined.append(DocViewData.markdownText(project, docView));
            }
            markdownByFileName.put(safeFileBase(className) + ".md", combined);
        } else {
            for (DocView docView : docViews) {
                indicator.checkCanceled();
                String fileName = safeFileBase(String.valueOf(docView.getName())) + ".md";
                markdownByFileName.computeIfAbsent(fileName, ignored -> new StringBuilder())
                        .append(DocViewData.markdownText(project, docView));
            }
        }
        return markdownByFileName.entrySet().stream()
                .map(entry -> new BatchMarkdownExportFile(entry.getKey(), entry.getValue().toString()))
                .toList();
    }

    private static String safeFileBase(String value) {
        String safe = value.replace('/', '_').replace('\\', '_').replace('\0', '_');
        return safe.isBlank() ? "DocView" : safe;
    }


}
