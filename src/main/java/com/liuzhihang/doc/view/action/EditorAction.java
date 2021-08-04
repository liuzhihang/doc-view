package com.liuzhihang.doc.view.action;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.ui.DocEditorForm;
import com.liuzhihang.doc.view.ui.ParamDocEditorForm;
import com.liuzhihang.doc.view.utils.CustomPsiUtils;
import com.liuzhihang.doc.view.utils.DubboPsiUtils;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;
import org.jetbrains.annotations.NotNull;

/**
 * 编辑方法
 *
 * @author liuzhihang
 * @date 2020/2/26 21:57
 */
public class EditorAction extends AnAction {

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
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.error.class"));
            return;
        }

        // 当前方法
        PsiMethod targetMethod = CustomPsiUtils.getTargetMethod(editor, psiFile);

        if (targetMethod != null) {
            // 解析请求参数
            DocViewService docViewService = DocViewService.getInstance(project, targetClass);

            if (docViewService == null) {
                return;
            }

            DocView docView = docViewService.buildClassMethodDoc(project, targetClass, targetMethod);

            DocEditorForm.getInstance(project, targetClass, targetMethod, DocViewData.getInstance(docView)).popup();
        } else {
            ParamDocEditorForm.getInstance(project, psiFile, editor, targetClass).popup();
        }

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

        Settings settings = Settings.getInstance(project);
        PsiMethod targetMethod = CustomPsiUtils.getTargetMethod(editor, psiFile);


        if (targetMethod == null && AnnotationUtil.isAnnotated(targetClass, settings.getContainClassAnnotationName(), 0)) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        if (targetMethod != null) {
            // 当前方法不为空, 则必须在 Controller 或者接口中
            // 检查是否有 Controller 注解 且不是接口
            if (!targetClass.isInterface() && !AnnotationUtil.isAnnotated(targetClass, settings.getContainClassAnnotationName(), 0)) {
                presentation.setEnabledAndVisible(false);
                return;
            }

            // Spring Controller 还需要检查方法是否满足条件
            if (AnnotationUtil.isAnnotated(targetClass, settings.getContainClassAnnotationName(), 0)) {
                // 过滤掉私有和静态方法以及没有相关注解的方法
                if (!SpringPsiUtils.isSpringMethod(targetMethod)) {
                    presentation.setEnabledAndVisible(false);
                    return;
                }
            }
            // Dubbo 接口 还需要检查方法是否满足条件
            if (targetClass.isInterface()) {
                // 过滤掉私有和静态方法以及没有相关注解的方法
                if (!DubboPsiUtils.isDubboMethod(targetMethod)) {
                    presentation.setEnabledAndVisible(false);

                }
            }

        } else {
            // 否则当做普通 Bean 进行处理, 普通 JavaBean 不能是接口和 controller
            if (targetClass.isInterface() && AnnotationUtil.isAnnotated(targetClass, settings.getContainClassAnnotationName(), 0)) {
                presentation.setEnabledAndVisible(false);
            }
        }
    }
}
