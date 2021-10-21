package com.liuzhihang.doc.view.provider;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.impl.JavaLineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.ui.PreviewForm;
import com.liuzhihang.doc.view.utils.DocViewIcons;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * 文档地址:
 * <p>
 * https://plugins.jetbrains.com/docs/intellij/line-marker-provider.html#register-the-line-marker-provider
 *
 * @author liuzhihang
 * @date 2021/10/21 16:03
 */
public class DocViewLineMarkerProvider extends JavaLineMarkerProvider {


    @Override
    public LineMarkerInfo<PsiElement> getLineMarkerInfo(@NotNull PsiElement element) {

        Project project = element.getProject();

        Settings settings = Settings.getInstance(project);

        if (!settings.getLineMarker()) {
            return null;
        }

        PsiElement parent = element.getParent();

        if (element instanceof PsiIdentifier) {

            if (parent instanceof PsiClass) {
                return createClassLineMarker(element);
            }

            if (parent instanceof PsiMethod) {
                return createMethodLineMarker(element);
            }
        }

        return null;
    }

    private LineMarkerInfo<PsiElement> createMethodLineMarker(PsiElement element) {

        Project project = element.getProject();
        Settings settings = Settings.getInstance(project);
        PsiMethod psiMethod = (PsiMethod) element.getParent();
        PsiClass psiClass = PsiTreeUtil.getParentOfType(psiMethod, PsiClass.class);
        PsiFile psiFile = element.getContainingFile();

        if (psiClass == null) {
            return null;
        }

        if (!psiClass.isInterface() && !AnnotationUtil.isAnnotated(psiClass, settings.getContainClassAnnotationName(), 0)) {
            return null;
        }
        if (psiClass.isAnnotationType() || psiClass.isEnum()) {
            return null;
        }
        DocViewService docViewService = DocViewService.getInstance(project, psiClass);
        if (docViewService == null) {
            return null;
        }

        DocView docView = docViewService.buildClassMethodDoc(project, psiClass, psiMethod);

        List<DocView> docViewList = new LinkedList<>();
        docViewList.add(docView);

        return new LineMarkerInfo<>(element, element.getTextRange(),
                DocViewIcons.DOC_VIEW,
                psiElement -> "查看文档",
                (e, elt) -> PreviewForm.getInstance(project, psiFile, psiClass, docViewList).popup(), GutterIconRenderer.Alignment.CENTER,
                () -> "Doc View");
    }

    private LineMarkerInfo<PsiElement> createClassLineMarker(PsiElement element) {

        Project project = element.getProject();
        Settings settings = Settings.getInstance(project);
        PsiClass psiClass = (PsiClass) element.getParent();
        PsiFile psiFile = element.getContainingFile();

        // 是 controller 或者接口
        if (!psiClass.isInterface() && !AnnotationUtil.isAnnotated(psiClass, settings.getContainClassAnnotationName(), 0)) {
            return null;
        }
        if (psiClass.isAnnotationType() || psiClass.isEnum()) {
            return null;
        }
        PsiMethod[] methods = psiClass.getMethods();
        if (methods.length == 0) {
            return null;
        }
        DocViewService docViewService = DocViewService.getInstance(project, psiClass);
        if (docViewService == null) {
            return null;
        }

        List<DocView> docViewList = docViewService.buildClassDoc(project, psiClass);

        if (docViewList == null) {
            return null;
        }


        return new LineMarkerInfo<>(element, element.getTextRange(),
                DocViewIcons.DOC_VIEW,
                psiElement -> "查看文档",
                (e, elt) -> PreviewForm.getInstance(project, psiFile, psiClass, docViewList).popup(), GutterIconRenderer.Alignment.CENTER,
                () -> "Doc View");

    }


}
