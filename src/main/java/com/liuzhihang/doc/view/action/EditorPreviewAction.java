package com.liuzhihang.doc.view.action;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.component.Settings;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.utils.CustomPsiUtils;
import com.liuzhihang.doc.view.utils.DubboPsiUtils;
import com.liuzhihang.doc.view.utils.NotificationUtils;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author liuzhihang
 * @date 2020/2/26 21:57
 */
public class EditorPreviewAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        // 获取当前编辑的文件, 可以进而获取 PsiClass, PsiField 对象
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);


        if (editor == null || project == null || psiFile == null) {
            return;
        }
        // 获取Java类或者接口
        PsiClass targetClass = CustomPsiUtils.getTargetClass(editor, psiFile);

        if (targetClass == null || targetClass.isAnnotationType() || targetClass.isEnum()) {
            NotificationUtils.errorNotify(DocViewBundle.message("notify.error.class"), project);
            return;
        }

        // 目标类没有方法
        PsiMethod[] methods = targetClass.getMethods();

        if (methods.length == 0) {
            NotificationUtils.errorNotify(DocViewBundle.message("notify.error.class.no.method"), project);
            return;
        }

        DocViewService instance = DocViewService.getInstance(project, psiFile, editor, targetClass);

        if (instance == null) {
            NotificationUtils.errorNotify(DocViewBundle.message("notify.error.not.support"), project);
            return;
        }

        instance.doPreview(project, psiFile, editor, targetClass);

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

        if (editor == null || project == null || psiFile == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        PsiClass targetClass = CustomPsiUtils.getTargetClass(editor, psiFile);

        if (targetClass == null || targetClass.isAnnotationType() || targetClass.isEnum()) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        Settings settings = project.getService(Settings.class);

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
                if (!SpringPsiUtils.isSpringMethod(settings, targetMethod)) {
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
                if (!DubboPsiUtils.isDubboMethod(settings, targetMethod)) {
                    presentation.setEnabledAndVisible(false);
                }

            }
        }


    }
}
