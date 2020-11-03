package com.liuzhihang.doc.view.utils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.liuzhihang.doc.view.config.AnnotationConfig;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * 自定义注解工具类
 *
 * @author liuzhihang
 * @date 2020/3/4 16:18
 */
public class CustomPsiAnnotationUtils {


    @NotNull
    public static String getMethod(PsiMethod psiMethod) {

        String method;

        if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConfig.GET_MAPPING, 0)) {
            method = "GET";
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConfig.POST_MAPPING, 0)) {
            method = "POST";
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConfig.PUT_MAPPING, 0)) {
            method = "PUT";
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConfig.DELETE_MAPPING, 0)) {
            method = "DELETE";
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConfig.PATCH_MAPPING, 0)) {
            method = "PATCH";
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConfig.REQUEST_MAPPING, 0)) {
            PsiAnnotation annotation = AnnotationUtil.findAnnotation(psiMethod, AnnotationConfig.REQUEST_MAPPING);
            if (annotation == null) {
                method = "GET";
            } else {
                String value = AnnotationUtil.getStringAttributeValue(annotation, "method");
                method = value == null ? "GET" : value;
            }
        } else {
            method = "GET";
        }

        return method;
    }

    /**
     * 从类注解中解析出请求路径
     *
     * @param psiClass
     * @param psiMethod
     * @return
     */
    @NotNull
    public static String getPath(PsiClass psiClass, @NotNull PsiMethod psiMethod) {

        String basePath = getBasePath(psiClass);
        String methodPath = getMethodPath(psiMethod);

        if (StringUtils.isBlank(basePath)) {
            return methodPath;
        }

        if (StringUtils.isBlank(methodPath)) {
            return basePath;
        }

        String path = basePath + methodPath;

        return path.replace("//", "/");
    }

    @NotNull
    public static String getBasePath(PsiClass psiClass) {
        // controller 路径
        PsiAnnotation annotation = AnnotationUtil.findAnnotation(psiClass, AnnotationConfig.REQUEST_MAPPING);

        return getPath(annotation);
    }


    @NotNull
    private static String getPath(PsiAnnotation annotation) {
        if (annotation == null) {
            return "";
        }
        String value = AnnotationUtil.getStringAttributeValue(annotation, "value");
        if (value != null) {
            return value;
        }
        return "";
    }


    public static String getMethodPath(PsiMethod psiMethod) {
        String url;

        if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConfig.GET_MAPPING, 0)) {
            url = getPath(AnnotationUtil.findAnnotation(psiMethod, AnnotationConfig.GET_MAPPING));
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConfig.POST_MAPPING, 0)) {
            url = getPath(AnnotationUtil.findAnnotation(psiMethod, AnnotationConfig.POST_MAPPING));
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConfig.PUT_MAPPING, 0)) {
            url = getPath(AnnotationUtil.findAnnotation(psiMethod, AnnotationConfig.PUT_MAPPING));
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConfig.DELETE_MAPPING, 0)) {
            url = getPath(AnnotationUtil.findAnnotation(psiMethod, AnnotationConfig.DELETE_MAPPING));
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConfig.PATCH_MAPPING, 0)) {
            url = getPath(AnnotationUtil.findAnnotation(psiMethod, AnnotationConfig.PATCH_MAPPING));
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConfig.REQUEST_MAPPING, 0)) {
            url = getPath(AnnotationUtil.findAnnotation(psiMethod, AnnotationConfig.REQUEST_MAPPING));
        } else {
            url = "";
        }
        return url;
    }

}
