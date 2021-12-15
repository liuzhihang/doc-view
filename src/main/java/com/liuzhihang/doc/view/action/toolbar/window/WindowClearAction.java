package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author liuzhihang
 * @date 2021/10/23 19:55
 */
public class WindowClearAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        Project project = e.getProject();

        if (project == null) {
            return;
        }

        String basePath = project.getBasePath();
        File file = new File(basePath + "/.idea/doc-view/temp/");

        deleteFile(file, project);

    }

    private void deleteFile(File file, Project project) {

        if (file.isFile()) {
            closeFileIfOpen(file, project);
        }

        if (file.isDirectory()) {

            File[] files = file.listFiles();

            if (files == null) {
                return;
            }

            for (File listFile : files) {
                deleteFile(listFile, project);
            }
        }

        FileUtil.delete(file);
    }


    private void closeFileIfOpen(File file, Project project) {

        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);

        if (vf == null) {
            return;
        }

        if (FileEditorManager.getInstance(project).isFileOpen(vf)) {
            FileEditorManager.getInstance(project).closeFile(vf);
        }

    }
}
