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
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.ui.DocEditorForm;
import com.liuzhihang.doc.view.ui.ParamDocEditorForm;
import com.liuzhihang.doc.view.utils.CustomPsiUtils;
import com.liuzhihang.doc.view.utils.DocViewUtils;
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

        if (editor == null || project == null || psiFile == null || DumbService.isDumb(project)) {
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

        if (editor == null || project == null || psiFile == null || DumbService.isDumb(project)) {
            presentation.setEnabledAndVisible(false);
            return;
        }


        PsiClass targetClass = CustomPsiUtils.getTargetClass(editor, psiFile);

        if (targetClass == null || targetClass.isAnnotationType() || targetClass.isEnum()) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        PsiMethod targetMethod = CustomPsiUtils.getTargetMethod(editor, psiFile);

        // 当前方法不为空, 则必须在 DocView 类中
        if (targetMethod != null && !DocViewUtils.isDocViewClass(targetClass)) {
            presentation.setEnabledAndVisible(false);
            return;


        }

        // 方法为空, 但是在 DocView 类中,说明不是 entity 实体类
        if (targetMethod == null && DocViewUtils.isDocViewClass(targetClass)) {
            presentation.setEnabledAndVisible(false);
            return;
        }


        presentation.setEnabledAndVisible(true);
    }
}
