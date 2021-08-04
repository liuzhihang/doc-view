package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author liuzhihang
 * @date 2020/3/4 16:18
 */
public class CustomPsiUtils {

    /**
     * 通过 PsiFile 获取 PsiClass
     *
     * @param file PsiFile
     * @return PsiClass
     * @author lvgorice@gmail.com
     * @since 1.0.4
     */
    public static PsiClass getTargetClass(PsiFile file) {
        PsiElement[] children = file.getChildren();
        for (PsiElement child : children) {
            if (child instanceof PsiClass) {
                return (PsiClass) child;
            }
        }
        return null;
    }

    @Nullable
    public static PsiClass getTargetClass(@NotNull Editor editor, @NotNull PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        if (element != null) {
            // 当前类
            PsiClass target = PsiTreeUtil.getParentOfType(element, PsiClass.class);

            return target instanceof SyntheticElement ? null : target;
        }

        return null;
    }

    @Nullable
    public static PsiMethod getTargetMethod(@NotNull Editor editor, @NotNull PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        if (element != null) {
            // 当前方法
            PsiMethod target = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
            return target instanceof SyntheticElement ? null : target;
        }
        return null;
    }

    /**
     * 校验方法是否含有修饰符
     *
     * @param psiMethod
     * @param modifier
     * @return
     * @see PsiModifier
     */
    public static boolean hasModifierProperty(@NotNull PsiMethod psiMethod, String modifier) {

        PsiModifierList modifierList = psiMethod.getModifierList();

        return modifierList.hasModifierProperty(modifier);
    }

    /**
     * 校验字段是否有某个修饰符
     *
     * @param psiField
     * @param modifier
     * @return
     */
    public static boolean hasModifierProperty(@NotNull PsiField psiField, String modifier) {

        PsiModifierList modifierList = psiField.getModifierList();

        if (modifierList == null) {
            return false;
        }


        return modifierList.hasModifierProperty(modifier);
    }


    public static @Nullable Map<String, PsiType> getGenericMap(@NotNull PsiClass psiClass, PsiClassType psiClassType) {

        if (!psiClass.hasTypeParameters()) {
            // 无泛型
            return null;
        }
        PsiTypeParameter[] typeParameters = psiClass.getTypeParameters();
        PsiType[] typeArr = psiClassType.getParameters();

        if (typeParameters.length != typeArr.length) {
            return null;
        }

        Map<String, PsiType> hashMap = new HashMap<>();


        for (int i = 0; i < typeParameters.length; i++) {

            hashMap.put(typeParameters[i].getName(), typeArr[i]);
        }
        return hashMap;

    }


}
