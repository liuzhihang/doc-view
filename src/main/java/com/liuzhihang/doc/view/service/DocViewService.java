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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author liuzhihang
 * @date 2020/3/3 13:32
 */
public interface DocViewService {


    @Nullable
    static DocViewService getInstance(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull Editor editor, @NotNull PsiClass targetClass) {

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


    void doPreview(@NotNull Project project, PsiFile psiFile, Editor editor, PsiClass targetClass);

    Map<String, DocView> buildClassDoc(Settings settings, @NotNull PsiClass psiClass);

    @NotNull
    DocView buildClassMethodDoc(Settings settings, PsiClass psiClass, @NotNull PsiMethod psiMethod);

}
