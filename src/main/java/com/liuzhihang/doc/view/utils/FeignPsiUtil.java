package com.liuzhihang.doc.view.utils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.PsiClass;
import com.liuzhihang.doc.view.config.Settings;
import org.jetbrains.annotations.NotNull;

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

        Settings settings = Settings.getInstance(psiClass.getProject());

        if (psiClass.hasAnnotation("FeignClient")) {
            return true;
        }

        return psiClass.isInterface() && AnnotationUtil.isAnnotated(psiClass, settings.getContainClassAnnotationName(), 0);
    }

}
