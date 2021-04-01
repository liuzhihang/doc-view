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

import java.util.HashMap;
import java.util.Map;

/**
 * @author liuzhihang
 * @date 2020/3/3 13:32
 */
public interface DocViewService {

    /**
     * 获取接口生成 service
     *
     * @param project 项目参数
     * @param psiFile PsiFile
     * @return DocViewService
     * @author lvgorice@gmail.com
     * @since 1.0.4
     */
    static DocViewService getInstance(Project project, PsiFile psiFile) {
        PsiClass targetClass = CustomPsiUtils.getTargetClass(psiFile);
        if (targetClass == null) {
            return null;
        }
        return getDocViewService(project, targetClass);
    }

    @Nullable
    static DocViewService getInstance(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull Editor editor, @NotNull PsiClass targetClass) {
        return getDocViewService(project, targetClass);
    }

    @Nullable
    static DocViewService getDocViewService(@NotNull Project project, @NotNull PsiClass targetClass) {
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

    /**
     * 生成接口文档
     *
     * @param project 项目参数
     * @param psiFile 当前操作文件
     * @return 生成结果
     */
    default Map<String, DocView> generatorDocView(Project project, PsiFile psiFile) {
        PsiClass targetClass = CustomPsiUtils.getTargetClass(psiFile);
        if (targetClass == null) {
            return new HashMap<>();
        }
        // 生成文档列表
        return buildClassDoc(project, targetClass);
    }

    void doPreview(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull Editor editor, @NotNull PsiClass targetClass);

    Map<String, DocView> buildClassDoc(Project settings, @NotNull PsiClass psiClass);

    @NotNull
    DocView buildClassMethodDoc(Project settings, PsiClass psiClass, @NotNull PsiMethod psiMethod);

}
