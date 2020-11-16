package com.liuzhihang.doc.view.service;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.liuzhihang.doc.view.component.Settings;
import com.liuzhihang.doc.view.dto.DocView;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author liuzhihang
 * @date 2020/3/3 13:32
 */
public interface SpringDocViewService {


    static SpringDocViewService getInstance() {
        return ServiceManager.getService(SpringDocViewService.class);
    }


    void doPreview(@NotNull Project project, PsiFile psiFile, Editor editor, PsiClass targetClass);

    Map<String, DocView> buildClassDoc(Settings settings, @NotNull PsiClass psiClass);

    @NotNull
    DocView buildClassMethodDoc(Settings settings, PsiClass psiClass, @NotNull PsiMethod psiMethod);


}
