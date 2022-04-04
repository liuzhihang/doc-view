package com.liuzhihang.doc.view.action;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.service.DocViewUploadService;
import com.liuzhihang.doc.view.utils.CustomPsiUtils;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author liuzhihang
 * @date 2021/10/23 23:13
 */
public abstract class AbstractUploadAction extends AnAction {

    protected Project project;
    protected PsiFile psiFile;
    protected Editor editor;
    protected PsiClass targetClass;

    protected List<DocView> docViewList;

    public void actionPerformed(@NotNull AnActionEvent e) {

        // 获取当前project对象
        project = e.getData(PlatformDataKeys.PROJECT);
        // 获取当前编辑的文件, 可以进而获取 PsiClass, PsiField 对象
        psiFile = e.getData(CommonDataKeys.PSI_FILE);
        editor = e.getData(CommonDataKeys.EDITOR);


        if (editor == null || project == null || psiFile == null || DumbService.isDumb(project)) {
            return;
        }
        if (DumbService.isDumb(project)) {
            return;
        }

        // 获取Java类或者接口
        targetClass = CustomPsiUtils.getTargetClass(editor, psiFile);

        if (targetClass == null || targetClass.isAnnotationType() || targetClass.isEnum()) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.error.class"));
            return;
        }

        // 目标类没有方法
        PsiMethod[] methods = targetClass.getMethods();

        if (methods.length == 0) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.error.class.no.method"));
            return;
        }

        DocViewService docViewService = DocViewService.getInstance(project, targetClass);

        if (docViewService == null) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.error.not.support"));
            return;
        }

        docViewList = docViewService.buildDoc(project, psiFile, editor, targetClass);

        if (CollectionUtils.isEmpty(docViewList)) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.error.not.support"));
            return;
        }

        // 上传
        uploadService().upload(project, docViewList);

    }

    /**
     * 具体上传服务
     *
     * @return
     */
    protected abstract DocViewUploadService uploadService();

    /**
     * 设置右键菜单是否隐藏 上传按钮
     *
     * @param e
     */
    @Override
    public void update(@NotNull AnActionEvent e) {

        Project project = e.getData(PlatformDataKeys.PROJECT);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        Presentation presentation = e.getPresentation();

        if (editor == null || project == null || psiFile == null || DumbService.isDumb(project)) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        PsiClass targetClass = CustomPsiUtils.getTargetClass(editor, psiFile);

        if (!DocViewUtils.isDocViewClass(targetClass)) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        PsiMethod targetMethod = CustomPsiUtils.getTargetMethod(editor, psiFile);

        if (targetMethod != null && !DocViewUtils.isDocViewMethod(targetMethod)) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        presentation.setEnabledAndVisible(true);
    }
}
