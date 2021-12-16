package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.ui.window.DocViewWindowTreeNode;

import java.io.File;

/**
 * @author liuzhihang
 * @date 2021/12/16
 */
public class CustomFileUtils {

    public static void delete(File file, Project project) {

        if (file.isFile()) {
            closeFileIfOpen(file, project);
        }

        if (file.isDirectory()) {

            File[] files = file.listFiles();

            if (files == null) {
                return;
            }

            for (File listFile : files) {
                delete(listFile, project);
            }
        }

        FileUtil.delete(file);
    }


    private static void closeFileIfOpen(File file, Project project) {

        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);

        if (vf == null) {
            return;
        }

        if (FileEditorManager.getInstance(project).isFileOpen(vf)) {
            FileEditorManager.getInstance(project).closeFile(vf);
        }

    }


    public static void open(DocViewWindowTreeNode node, Project project) {

        if (node.isClassPath()) {
            return;
        }


        DocViewService service = DocViewService.getInstance(project, node.getPsiClass());

        if (service == null) {
            return;
        }

        File file = new File(node.getTempFilePath());

        if (!file.exists()) {

            String markdownText = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
                DocView docView = service.buildClassMethodDoc(project, node.getPsiClass(), node.getPsiMethod());
                return DocViewData.markdownText(project, docView);
            });

            try {
                FileUtil.writeToFile(file, markdownText);
            } catch (Exception ex) {
                DocViewNotification.notifyError(project, DocViewBundle.message("notify.spring.error.method"));
            }
        }

        openMarkdownDoc(file, project);
    }


    private static void openMarkdownDoc(File file, Project project) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);

            if (vf == null) {
                return;
            }

            OpenFileDescriptor descriptor = new OpenFileDescriptor(project, vf);
            FileEditorManager.getInstance(project).openTextEditor(descriptor, false);
        });
    }

}
