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
import com.liuzhihang.doc.view.enums.ContentTypeEnum;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.ui.window.MethodNode;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * 自定义文件操作, 文件保存在 "Scratches and Consoles" 下面
 *
 * <a href="https://www.jetbrains.com/help/idea/scratches.html">Scratch files</a>
 *
 * @author liuzhihang
 * @date 2021/12/16
 */
public class CustomFileUtils {

    /**
     * 生成接口文档 md 文件, 并打开
     *
     * @param project 项目
     * @param node    右侧目录树节点
     */
    public static void openMd(Project project, MethodNode node) {

        if (node == null) {
            return;
        }

        DocViewService service = DocViewService.getInstance(project, node.getPsiClass());

        if (service == null) {
            return;
        }

        // 生成接口对应的 markdown 文本
        String markdownText = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            DocView docView = service.buildClassMethodDoc(project, node.getPsiClass(), node.getPsiMethod());
            return DocViewData.markdownText(project, docView);
        });

        open(project, node.docPath(project), markdownText);

    }

    /**
     * 删除生成的文件
     *
     * @param project 项目
     * @param path    删除的路径
     */
    public static void delete(Project project, String path) {

        try {

            // 获取在文件系统中的虚拟文件
            VirtualFile file = ExtensionsRootType.getInstance().findFile(project, path, Option.create_if_missing);

            // 在删除之前要先关闭
            if (FileEditorManager.getInstance(project).isFileOpen(file)) {
                FileEditorManager.getInstance(project).closeFile(file);
            }

            // 删除文件
            WriteAction.runAndWait(() -> file.delete(project));

        } catch (IOException e) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.extensions.file.delete.file", path));
        }
    }

    /**
     * 生成打开 .http 文件
     *
     * @param project 项目
     * @param node    节点
     */
    public static void openHttp(Project project, MethodNode node) {

        if (node == null) {
            return;
        }

        DocViewService service = DocViewService.getInstance(project, node.getPsiClass());

        if (service == null) {
            return;
        }

        // 解析获取 DocView, 从而可以获取接口信息
        DocView docView = ApplicationManager.getApplication().runReadAction(
                (Computable<DocView>) () -> service.buildClassMethodDoc(project, node.getPsiClass(), node.getPsiMethod()));

        StringBuilder builder = new StringBuilder();
        builder.append("### Doc View: ").append(docView.getName()).append("\n");
        builder.append(docView.getMethod()).append(" {{host}}").append(docView.getPath());

        if (StringUtils.isNotBlank(docView.getReqFormExample())) {
            builder.append("?").append(docView.getReqFormExample());
        }

        builder.append("\n");

        if (docView.getContentType() == ContentTypeEnum.JSON) {
            builder.append(ContentTypeEnum.JSON.getKey()).append(": ").append(ContentTypeEnum.JSON.getValue()).append("\n");
            builder.append("\n");
            builder.append(docView.getReqBodyExample());
        } else {
            builder.append(ContentTypeEnum.FORM.getKey()).append(": ").append(ContentTypeEnum.JSON.getValue()).append("\n");
        }

        open(project, node.httpPath(project), builder.toString());

    }

    /**
     * 打开文件
     *
     * @param project  项目
     * @param pathName 文件路径
     * @param text     文本内容
     */
    private static void open(Project project, String pathName, String text) {
        try {
            // 打开虚拟文件
            VirtualFile virtualFile = WriteCommandAction.writeCommandAction(project)
                    .withName(DocViewBundle.message("notify.extensions.file.creating"))
                    .withGlobalUndo().shouldRecordActionForActiveDocument(false)
                    .withUndoConfirmationPolicy(UndoConfirmationPolicy.REQUEST_CONFIRMATION).compute(() -> {
                        // 使用 ExtensionsRootType 可以打开 Extensions 目录
                        ExtensionsRootType fileService = ExtensionsRootType.getInstance();
                        VirtualFile scratchFile = fileService.findFile(project, pathName, Option.create_new_always);
                        VfsUtil.saveText(scratchFile, text);
                        return scratchFile;
                    });

            // 打开文件, 并聚焦在该文件
            if (virtualFile != null) {
                FileEditorManager.getInstance(project).openFile(virtualFile, true);
            }
        } catch (IOException e) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.extensions.file.create.file", pathName));
        }
    }

}
