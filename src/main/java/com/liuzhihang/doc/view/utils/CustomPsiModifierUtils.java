package com.liuzhihang.doc.view.utils;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;

/**
 * @author liuzhihang
 * @date 2020/10/29 18:36
 */
public class CustomPsiModifierUtils {

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
