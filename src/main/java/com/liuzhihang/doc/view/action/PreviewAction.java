package com.liuzhihang.doc.view.action;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.ui.PreviewForm;
import com.liuzhihang.doc.view.utils.CustomPsiUtils;
import com.liuzhihang.doc.view.utils.DubboPsiUtils;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 单个类或者方法中操作
 *
 * @author liuzhihang
 * @date 2020/2/26 21:57
 */
public class PreviewAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        // 获取当前编辑的文件, 可以进而获取 PsiClass, PsiField 对象
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);


        if (editor == null || project == null || psiFile == null || DumbService.isDumb(project)) {
            return;
        }


        // 获取Java类或者接口
        PsiClass targetClass = CustomPsiUtils.getTargetClass(editor, psiFile);

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

        List<DocView> docViewList = docViewService.buildDoc(project, psiFile, editor, targetClass);

        if (docViewList == null) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.error.not.support"));
            return;
        }

        PreviewForm.getInstance(project, psiFile, targetClass, docViewList).popup();
    }


    /**
     * 设置右键菜单是否隐藏 Doc View
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

        if (targetClass == null || targetClass.isAnnotationType() || targetClass.isEnum()) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        Settings settings = Settings.getInstance(project);

        // 检查是否有 Controller 注解 且不是接口
        if (!targetClass.isInterface() && !AnnotationUtil.isAnnotated(targetClass, settings.getContainClassAnnotationName(), 0)) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        // Spring Controller 还需要检查方法是否满足条件
        if (AnnotationUtil.isAnnotated(targetClass, settings.getContainClassAnnotationName(), 0)) {
            PsiMethod targetMethod = CustomPsiUtils.getTargetMethod(editor, psiFile);
            // 过滤掉私有和静态方法以及没有相关注解的方法
            if (targetMethod != null) {
                if (!SpringPsiUtils.isSpringMethod(targetMethod)) {
                    presentation.setEnabledAndVisible(false);
                    return;
                }

            }
        }

        // Dubbo 接口 还需要检查方法是否满足条件
        if (targetClass.isInterface()) {
            PsiMethod targetMethod = CustomPsiUtils.getTargetMethod(editor, psiFile);
            // 过滤掉私有和静态方法以及没有相关注解的方法
            if (targetMethod != null) {
                if (!DubboPsiUtils.isDubboMethod(targetMethod)) {
                    presentation.setEnabledAndVisible(false);
                }

            }
        }


    }
}
