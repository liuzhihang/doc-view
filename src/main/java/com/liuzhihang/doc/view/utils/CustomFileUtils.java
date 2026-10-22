package com.liuzhihang.doc.view.utils;

import com.intellij.ide.extensionResources.ExtensionsRootType;
import com.intellij.ide.scratch.ScratchFileService.Option;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
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

    private static final String METHOD_DOC_TASK_TITLE = DocViewBundle.message("task.method.documentation");
    private static final String METHOD_DOC_GENERATION_FAILED = DocViewBundle.message("notify.method.documentation.failed");

    /**
     * 生成接口文档 md 文件, 并打开
     *
     * @param project 项目
     * @param node    右侧目录树节点
     */
    public static void openMd(Project project, MethodNode node) {

        if (project == null || project.isDisposed() || node == null) {
            return;
        }

        DocViewBackgroundTasks.runReadTask(project, METHOD_DOC_TASK_TITLE, true, indicator -> {
            indicator.checkCanceled();
            if (project.isDisposed()) {
                return null;
            }

            PsiClass psiClass = node.getPsiClass();
            PsiMethod psiMethod = node.getPsiMethod();
            if (psiClass == null || psiMethod == null || !psiClass.isValid() || !psiMethod.isValid()) {
                return null;
            }

            DocViewService service = DocViewService.getInstance(project, psiClass);
            if (service == null) {
                return null;
            }

            DocView docView = service.buildClassMethodDoc(psiClass, psiMethod);
            indicator.checkCanceled();
            String markdownText = DocViewData.markdownText(project, docView);
            indicator.checkCanceled();
            String pathName = node.docPath(project);
            indicator.checkCanceled();
            return StringUtils.isBlank(pathName) ? null : new MarkdownSnapshot(pathName, markdownText);
        }, snapshot -> {
            if (snapshot == null || project.isDisposed()) {
                return;
            }

            try {
                open(project, snapshot.pathName(), snapshot.markdownText());
            } catch (ProcessCanceledException ignored) {
                // A closing project or canceled task must not publish a stale Scratch result.
            } catch (RuntimeException exception) {
                DocViewNotification.notifyError(project, METHOD_DOC_GENERATION_FAILED);
            }
        }, throwable -> DocViewNotification.notifyError(project, METHOD_DOC_GENERATION_FAILED));

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
                (Computable<DocView>) () -> service.buildClassMethodDoc(node.getPsiClass(), node.getPsiMethod()));

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

    private record MarkdownSnapshot(String pathName, String markdownText) {
    }

}
