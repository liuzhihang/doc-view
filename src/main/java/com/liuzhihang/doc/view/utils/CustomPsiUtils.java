package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author liuzhihang
 * @date 2020/3/4 16:18
 */
public class CustomPsiUtils {


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
    public static boolean hasModifierProperty(PsiMethod psiMethod, String modifier) {

        PsiModifierList modifierList = psiMethod.getModifierList();

        if (modifierList.hasModifierProperty(modifier)) {
            return true;
        }

        return false;
    }

    /**
     * 校验字段是否有某个修饰符
     *
     * @param psiField
     * @param modifier
     * @return
     */
    public static boolean hasModifierProperty(PsiField psiField, String modifier) {

        PsiModifierList modifierList = psiField.getModifierList();

        if (modifierList == null) {
            return false;
        }


        if (modifierList.hasModifierProperty(modifier)) {
            return true;
        }

        return false;
    }


}
