package com.liuzhihang.doc.view.utils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.liuzhihang.doc.view.constant.SpringConstant;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author liuzhihang
 * @date 2021/10/23 17:38
 */
public class FeignPsiUtil {

    /**
     * 检查类或者接口是否是 Feign 接口
     *
     * @param psiClass
     * @return
     */
    public static boolean isFeignClass(@NotNull PsiClass psiClass) {

        // 必须匹配是 Feign 注解才可以
        return psiClass.isInterface() && AnnotationUtil.isAnnotated(psiClass, SpringConstant.FEIGN_CLIENT, 0);
    }

    public static List<PsiClass> findDocViewFromModule(Module module) {

        Collection<PsiAnnotation> psiAnnotations = JavaAnnotationIndex.getInstance().get("FeignClient", module.getProject(), GlobalSearchScope.moduleScope(module));

        List<PsiClass> psiClasses = new LinkedList<>();

        for (PsiAnnotation psiAnnotation : psiAnnotations) {
            PsiModifierList psiModifierList = (PsiModifierList) psiAnnotation.getParent();
            PsiElement psiElement = psiModifierList.getParent();

            if (psiElement instanceof PsiClass && isFeignClass((PsiClass) psiElement)) {
                psiClasses.add((PsiClass) psiElement);
            }
        }
        return psiClasses;
    }

}
