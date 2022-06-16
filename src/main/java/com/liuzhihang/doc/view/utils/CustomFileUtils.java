package com.liuzhihang.doc.view.utils;

import com.intellij.ide.extensionResources.ExtensionsRootType;
import com.intellij.ide.scratch.ScratchFileService.Option;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.ui.window.MethodNode;

import java.io.IOException;

/**
 * @author liuzhihang
 * @date 2021/12/16
 */
public class CustomFileUtils {

    public static void delete(Project project, String path) {

        try {

            VirtualFile file = ExtensionsRootType.getInstance().findFile(project, path, Option.create_if_missing);

            if (FileEditorManager.getInstance(project).isFileOpen(file)) {
                FileEditorManager.getInstance(project).closeFile(file);
            }

            WriteAction.runAndWait(() -> file.delete(project));

        } catch (IOException e) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.extensions.file.delete.file", path));
        }
    }

    public static void open(MethodNode node, Project project) {

        if (node == null) {
            return;
        }

        DocViewService service = DocViewService.getInstance(project, node.getPsiClass());

        if (service == null) {
            return;
        }

        String markdownText = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            DocView docView = service.buildClassMethodDoc(project, node.getPsiClass(), node.getPsiMethod());
            return DocViewData.markdownText(project, docView);
        });

        try {
            VirtualFile virtualFile = WriteCommandAction.writeCommandAction(project)
                    .withName(DocViewBundle.message("notify.extensions.file.creating"))
                    .withGlobalUndo().shouldRecordActionForActiveDocument(false)
                    .withUndoConfirmationPolicy(UndoConfirmationPolicy.REQUEST_CONFIRMATION).compute(() -> {
                        ExtensionsRootType fileService = ExtensionsRootType.getInstance();
                        VirtualFile scratchFile = fileService.findFile(project, node.cachePath(project), Option.create_if_missing);
                        VfsUtil.saveText(scratchFile, markdownText);
                        return scratchFile;
                    });

            if (virtualFile != null) {
                FileEditorManager.getInstance(project).openFile(virtualFile, true);
            }
        } catch (IOException e) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.extensions.file.create.file", node.getName()));
        }

    }

}
