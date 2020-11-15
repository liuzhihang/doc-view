package com.liuzhihang.doc.view.action;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.liuzhihang.doc.view.component.Settings;
import com.liuzhihang.doc.view.service.SpringDocViewService;
import com.liuzhihang.doc.view.utils.CustomPsiUtils;
import com.liuzhihang.doc.view.utils.NotificationUtils;

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
            NotificationUtils.errorNotify("Please use in java class file", project);
            return;
        }

        // 目标类没有方法
        PsiMethod[] methods = targetClass.getMethods();

        if (methods.length == 0) {
            NotificationUtils.errorNotify("There is no method in the class", project);
            return;
        }

        Settings settings = project.getService(Settings.class);


        if (targetClass.isInterface()) {
            NotificationUtils.errorNotify("The current version does not support the interface", project);
            return;
        }

        // Spring
        if (AnnotationUtil.isAnnotated(targetClass, settings.getContainClassAnnotationName(), 0)) {

            SpringDocViewService.getInstance().doPreview(project, psiFile, editor, targetClass);

            return;
        }

        NotificationUtils.errorNotify("Document generation is not supported here", project);
    }


}
