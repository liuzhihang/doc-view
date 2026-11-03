package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Reads library documentation through the IDE's attached sources, without changing binary type identity. */
public final class DependencySourceComments {

    private DependencySourceComments() {
    }

    /** The returned field is for documentation reads only; keep the original field for types and writeback. */
    @NotNull
    public static PsiField documentationField(@NotNull PsiField field) {
        return ApplicationManager.getApplication().runReadAction((Computable<PsiField>) () -> {
            ProgressManager.checkCanceled();
            if (!field.isValid() || field.getProject().isDisposed()
                    || !(field instanceof PsiCompiledElement) || DumbService.isDumb(field.getProject())) {
                return field;
            }
            PsiElement navigation = field.getNavigationElement();
            if (!(navigation instanceof PsiField source) || !source.isValid()
                    || source instanceof PsiCompiledElement || !field.getName().equals(source.getName())) {
                return field;
            }
            PsiClass binaryOwner = field.getContainingClass();
            PsiClass sourceOwner = source.getContainingClass();
            if (binaryOwner == null || sourceOwner == null || binaryOwner.getQualifiedName() == null
                    || !Objects.equals(binaryOwner.getQualifiedName(), sourceOwner.getQualifiedName())) {
                return field;
            }
            return source;
        });
    }
}
