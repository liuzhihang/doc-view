package com.liuzhihang.doc.view.action;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.liuzhihang.doc.view.component.Settings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.ui.PreviewForm;
import com.liuzhihang.doc.view.utils.CustomPsiUtils;
import com.liuzhihang.doc.view.utils.NotificationUtils;

import java.util.Map;

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

        // 不是接口 且不包含指定的注解
        if (!targetClass.isInterface() && !AnnotationUtil.isAnnotated(targetClass, settings.getContainClassAnnotationName(), 0)) {
            NotificationUtils.errorNotify("The current class is not an interface and has no Spring annotations", project);
            return;
        }

        // 当前方法
        PsiMethod targetMethod = CustomPsiUtils.getTargetMethod(editor, psiFile);

        // 生成文档列表
        Map<String, DocView> docMap = DocViewService.getInstance().buildDocView(settings, targetClass, targetMethod);

        if (docMap == null || docMap.size() == 0) {
            NotificationUtils.errorNotify("There is no public modification method in the class", project);
            return;
        }


        DialogWrapper dialog = new PreviewForm(project, psiFile, editor, targetClass, docMap);
        dialog.show();
    }


}
