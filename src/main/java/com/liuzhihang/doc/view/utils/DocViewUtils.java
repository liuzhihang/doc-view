package com.liuzhihang.doc.view.utils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.constant.FieldTypeConstant;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * DocView 通用处理类
 * <p>
 * 从 settings 中获取配置,以及用户自定义的配置进行判断 统一返回
 *
 * @author liuzhihang
 * @date 2021/6/10 20:10
 */
public class DocViewUtils {

    private DocViewUtils() {
    }

    /**
     * 获取方法描述
     *
     * @param psiMethod
     * @param settings
     * @return
     */
    @NotNull
    public static String methodDesc(@NotNull PsiMethod psiMethod, @NotNull Settings settings) {

        return CustomPsiCommentUtils.getComment(psiMethod.getDocComment());
    }

    /**
     * 获取方法名字
     *
     * @param psiMethod
     * @param settings
     * @return
     */
    @NotNull
    public static String methodName(@NotNull PsiMethod psiMethod, @NotNull Settings settings) {

        // 注释上的 tag
        String comment = CustomPsiCommentUtils.getComment(psiMethod.getDocComment(), settings.getName());

        if (StringUtils.isNotBlank(comment)) {
            return comment;
        }
        return psiMethod.getName();
    }

    /**
     * 匿名类和内部类可能返回 null
     *
     * @param psiClass
     * @param settings
     * @return
     */
    @NotNull
    public static String getDocTitle(@NotNull PsiClass psiClass, @NotNull Settings settings) {
        return psiClass.getQualifiedName() == null ? "" : psiClass.getQualifiedName();
    }

    /**
     * 判断是否是需要排除的字段
     *
     * @param psiField
     * @param settings
     * @return
     */
    @NotNull
    public static boolean isExcludeField(@NotNull PsiField psiField, @NotNull Settings settings) {

        if (settings.getExcludeFieldNames().contains(psiField.getName())) {
            return true;
        }
        // 排除掉被 static 修饰的字段
        if (CustomPsiUtils.hasModifierProperty(psiField, PsiModifier.STATIC)) {
            return true;
        }

        // 排除部分注解的字段
        for (PsiAnnotation annotation : psiField.getAnnotations()) {
            if (FieldTypeConstant.ANNOTATION_TYPES.contains(annotation.getQualifiedName())) {
                return true;
            }
        }

        return false;
    }


    /**
     * 判断字段是否必填
     *
     * @param psiField
     * @return
     */
    public static boolean isRequired(@NotNull PsiField psiField, @NotNull Settings settings) {

        Project project = psiField.getProject();
        // 判断是否必填
        Settings setting = Settings.getInstance(project);
        boolean annotated = AnnotationUtil.isAnnotated(psiField, setting.getFieldRequiredAnnotationName(), 0);

        if (annotated) {
            return true;
        }

        // 查看注释
        PsiDocComment docComment = psiField.getDocComment();

        if (docComment == null) {
            return true;
        }

        PsiDocTag requiredTag = docComment.findTagByName(settings.getRequired());

        // Swagger 注解

        return requiredTag != null;
    }

    public static boolean isRequired(@NotNull PsiParameter psiParameter, @NotNull Settings settings) {

        return AnnotationUtil.isAnnotated(psiParameter, settings.getFieldRequiredAnnotationName(), 0);
    }


    /**
     * 获取字段的描述
     *
     * @param psiField
     * @param settings
     * @return
     */
    @NotNull
    public static String fieldDesc(@NotNull PsiField psiField, @NotNull Settings settings) {

        PsiDocComment docComment = psiField.getDocComment();

        if (docComment != null) {
            // param.setExample();
            // 参数举例, 使用 tag 判断
            return CustomPsiCommentUtils.getComment(docComment);
        }
        return "";
    }
}
