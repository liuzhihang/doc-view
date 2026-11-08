package com.liuzhihang.doc.view.utils;

import com.intellij.ide.extensionResources.ExtensionsRootType;
import com.intellij.ide.scratch.ScratchFileService.Option;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorOpenRequest;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.ui.window.MethodNode;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

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
    private static final Key<AtomicLong> MARKDOWN_OPEN_GENERATION = Key.create("doc.view.markdown.open.generation");

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
        long request = nextMarkdownOpenRequest(project);

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

            VirtualFile file;
            try {
                file = saveMarkdown(project, snapshot.pathName(), snapshot.markdownText());
            } catch (ProcessCanceledException ignored) {
                // A closing project or canceled task must not publish a stale Scratch result.
                return;
            } catch (IOException exception) {
                if (isCurrentMarkdownOpen(project, request)) {
                    DocViewNotification.notifyError(project,
                            DocViewBundle.message("notify.extensions.file.create.file", snapshot.pathName()));
                }
                return;
            } catch (RuntimeException exception) {
                if (isCurrentMarkdownOpen(project, request)) {
                    DocViewNotification.notifyError(project, METHOD_DOC_GENERATION_FAILED);
                }
                return;
            }
            if (file == null || project.isDisposed()) {
                return;
            }
            try {
                CompletableFuture<?> openResult = FileEditorManager.getInstance(project).requestOpenFile(file,
                        FileEditorOpenRequest.withFocus(true).withSelectAsCurrent(true));
                FileEditorOpenSupport.whenOpened(project, openResult,
                        () -> isCurrentMarkdownOpen(project, request),
                        opened -> { }, error -> DocViewNotification.notifyError(project,
                                DocViewBundle.message("notify.method.documentation.open.failed")));
            } catch (RuntimeException exception) {
                if (!FileEditorOpenSupport.isCancellation(exception)
                        && isCurrentMarkdownOpen(project, request)) {
                    DocViewNotification.notifyError(project,
                            DocViewBundle.message("notify.method.documentation.open.failed"));
                }
            }
        }, throwable -> {
            if (isCurrentMarkdownOpen(project, request)) {
                DocViewNotification.notifyError(project, METHOD_DOC_GENERATION_FAILED);
            }
        });

    }

    /**
     * 生成打开 .http 文件
     *
     * @param project 项目
     * @param node    节点
     */
    public static void openHttp(Project project, MethodNode node) {

        if (project != null && !project.isDisposed() && node != null) {
            com.liuzhihang.doc.view.service.http.HttpRequestService.getInstance(project)
                    .generate(node.getPsiMethod());
        }

    }

    /**
     * 打开文件
     *
     * @param project  项目
     * @param pathName 文件路径
     * @param text     文本内容
     */
    private static VirtualFile saveMarkdown(Project project, String pathName, String text) throws IOException {
        return WriteCommandAction.writeCommandAction(project)
                .withName(DocViewBundle.message("notify.extensions.file.creating"))
                .withGlobalUndo().shouldRecordActionForActiveDocument(false)
                .withUndoConfirmationPolicy(UndoConfirmationPolicy.REQUEST_CONFIRMATION).compute(() -> {
                    ExtensionsRootType fileService = ExtensionsRootType.getInstance();
                    VirtualFile scratchFile = fileService.findFile(project, pathName, Option.create_new_always);
                    if (scratchFile == null) throw new IOException("Scratch file creation returned no file");
                    VfsUtil.saveText(scratchFile, text);
                    return scratchFile;
                });
    }

    private static long nextMarkdownOpenRequest(Project project) {
        AtomicLong generation = project.getUserData(MARKDOWN_OPEN_GENERATION);
        if (generation == null) {
            synchronized (project) {
                generation = project.getUserData(MARKDOWN_OPEN_GENERATION);
                if (generation == null) {
                    generation = new AtomicLong();
                    project.putUserData(MARKDOWN_OPEN_GENERATION, generation);
                }
            }
        }
        return generation.incrementAndGet();
    }

    private static boolean isCurrentMarkdownOpen(Project project, long request) {
        if (project.isDisposed()) return false;
        AtomicLong generation = project.getUserData(MARKDOWN_OPEN_GENERATION);
        return generation != null && generation.get() == request;
    }

    private record MarkdownSnapshot(String pathName, String markdownText) {
    }

}
