package com.liuzhihang.doc.view.service;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.impl.DubboDocViewServiceImpl;
import com.liuzhihang.doc.view.service.impl.SpringDocViewServiceImpl;
import com.liuzhihang.doc.view.utils.CustomPsiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * @author liuzhihang
 * @date 2020/3/3 13:32
 */
public interface DocViewService {


    @Nullable
    static DocViewService getInstance(@NotNull Project project, @NotNull PsiClass targetClass) {
        // Dubbo
        if (targetClass.isInterface()) {
            return ServiceManager.getService(DubboDocViewServiceImpl.class);
        }

        Settings settings = Settings.getInstance(project);

        // Spring
        if (AnnotationUtil.isAnnotated(targetClass, settings.getContainClassAnnotationName(), 0)) {
            return ServiceManager.getService(SpringDocViewServiceImpl.class);
        }
        return null;
    }

    @Nullable
    default List<DocView> buildDoc(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull Editor editor, @NotNull PsiClass targetClass) {

        // 当前方法
        PsiMethod targetMethod = CustomPsiUtils.getTargetMethod(editor, psiFile);

        if (targetMethod != null && checkMethod(project, targetMethod)) {
            DocView docView = buildClassMethodDoc(project, targetClass, targetMethod);

            List<DocView> docViewList = new LinkedList<>();
            docViewList.add(docView);

            return docViewList;
        }

        // 每个方法都要生成
        if (targetMethod == null) {
            // 生成文档列表
            return buildClassDoc(project, targetClass);

        }

        return null;

    }

    boolean checkMethod(@NotNull Project project, @NotNull PsiMethod targetMethod);

    List<DocView> buildClassDoc(Project settings, @NotNull PsiClass psiClass);

    @NotNull
    DocView buildClassMethodDoc(Project settings, PsiClass psiClass, @NotNull PsiMethod psiMethod);

}
