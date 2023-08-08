package com.liuzhihang.doc.view.provider;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.ui.PreviewForm;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import icons.DocViewIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * 文档地址:
 * <p>
 * https://plugins.jetbrains.com/docs/intellij/line-marker-provider.html#register-the-line-marker-provider
 *
 * @author liuzhihang
 * @date 2021/10/21 16:03
 */
public class DocViewLineMarkerProvider implements LineMarkerProvider {


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
        PsiMethod psiMethod = (PsiMethod) element.getParent();
        PsiClass psiClass = PsiTreeUtil.getParentOfType(psiMethod, PsiClass.class);

        if (psiClass == null) {
            return null;
        }

        DocViewService docViewService = checkShowLineMarker(project, psiClass);

        if (docViewService == null) {
            return null;
        }

        if (!DocViewUtils.isDocViewMethod(psiMethod)) {
            return null;
        }

        return new LineMarkerInfo<>(element, element.getTextRange(),
                DocViewIcons.DOC_VIEW,
                psiElement -> "查看文档",
                (e, elt) -> PreviewForm.getInstance(psiClass, psiMethod).popup(),
                GutterIconRenderer.Alignment.LEFT,
                () -> "Doc View");
    }


    @Nullable
    private DocViewService checkShowLineMarker(Project project, PsiClass psiClass) {

        if (DocViewUtils.isDocViewClass(psiClass)) {
            return DocViewService.getInstance(project, psiClass);
        }

        return null;

    }

    private LineMarkerInfo<PsiElement> createClassLineMarker(PsiElement element) {

        Project project = element.getProject();
        PsiClass psiClass = (PsiClass) element.getParent();

        DocViewService docViewService = checkShowLineMarker(project, psiClass);

        if (docViewService == null) {
            return null;
        }

        PsiMethod[] methods = psiClass.getAllMethods();

        if (methods.length == 0) {
            return null;
        }

        long docViewMethodCount = Arrays.stream(methods).filter(DocViewUtils::isDocViewMethod).count();
        if (docViewMethodCount == 0) {
            return null;
        }

        return new LineMarkerInfo<>(element, element.getTextRange(),
                DocViewIcons.DOC_VIEW,
                psiElement -> "查看文档",
                (e, elt) -> PreviewForm.getInstance(psiClass, null).popup(),
                GutterIconRenderer.Alignment.CENTER,
                () -> "Doc View");

    }


}
