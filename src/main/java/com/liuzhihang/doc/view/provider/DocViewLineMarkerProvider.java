package com.liuzhihang.doc.view.provider;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.ui.PreviewForm;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import icons.DocViewIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        Settings settings = Settings.getInstance(project);
        PsiMethod psiMethod = (PsiMethod) element.getParent();
        PsiClass psiClass = PsiTreeUtil.getParentOfType(psiMethod, PsiClass.class);
        PsiFile psiFile = element.getContainingFile();

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
                (e, elt) -> {
                    // 在点击图标时再解析 doc
                    DocView docView = docViewService.buildClassMethodDoc(project, psiClass, psiMethod);
                    List<DocView> docViewList = new LinkedList<>();
                    docViewList.add(docView);
                    PreviewForm.getInstance(project, psiFile, psiClass, docViewList).popup();
                },
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
        PsiFile psiFile = element.getContainingFile();

        DocViewService docViewService = checkShowLineMarker(project, psiClass);

        if (docViewService == null) {
            return null;
        }

        PsiMethod[] methods = psiClass.getAllMethods();

        if (methods.length == 0) {
            return null;
        }

        int docViewMethodCount = 0;
        for (PsiMethod method : methods) {
            if (DocViewUtils.isDocViewMethod(method)) {
                docViewMethodCount++;
            }
        }
        if (docViewMethodCount == 0) {
            return null;
        }

        return new LineMarkerInfo<>(element, element.getTextRange(),
                DocViewIcons.DOC_VIEW,
                psiElement -> "查看文档",
                (e, elt) -> {
                    // 在点击图标时再解析 doc
                    List<DocView> docViewList = docViewService.buildClassDoc(project, psiClass);
                    if (docViewList == null) {
                        DocViewNotification.notifyError(project, DocViewBundle.message("notify.spring.error.no.method"));
                        return;
                    }
                    PreviewForm.getInstance(project, psiFile, psiClass, docViewList).popup();
                },
                GutterIconRenderer.Alignment.CENTER,
                () -> "Doc View");

    }


}
