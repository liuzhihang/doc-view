package com.liuzhihang.doc.view.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.constant.AnnotationConstant;
import com.liuzhihang.doc.view.constant.FieldTypeConstant;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.Header;
import com.liuzhihang.doc.view.dto.Param;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

/**
 * @author liuzhihang
 * @date 2020/3/4 19:45
 */
public class SpringPsiUtils {

    @NotNull
    public static String getMethod(PsiMethod psiMethod) {

        String method;

        if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConstant.GET_MAPPING, 0)) {
            method = "GET";
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConstant.POST_MAPPING, 0)) {
            method = "POST";
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConstant.PUT_MAPPING, 0)) {
            method = "PUT";
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConstant.DELETE_MAPPING, 0)) {
            method = "DELETE";
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConstant.PATCH_MAPPING, 0)) {
            method = "PATCH";
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConstant.REQUEST_MAPPING, 0)) {
            PsiAnnotation annotation = AnnotationUtil.findAnnotation(psiMethod, AnnotationConstant.REQUEST_MAPPING);
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

        if (!methodPath.startsWith("/")) {
            methodPath = "/" + methodPath;
        }

        String path = basePath + methodPath;

        return path.replace("//", "/");
    }

    @NotNull
    public static String getBasePath(PsiClass psiClass) {
        // controller 路径
        PsiAnnotation annotation = AnnotationUtil.findAnnotation(psiClass, AnnotationConstant.REQUEST_MAPPING);

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

        if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConstant.GET_MAPPING, 0)) {
            url = getPath(AnnotationUtil.findAnnotation(psiMethod, AnnotationConstant.GET_MAPPING));
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConstant.POST_MAPPING, 0)) {
            url = getPath(AnnotationUtil.findAnnotation(psiMethod, AnnotationConstant.POST_MAPPING));
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConstant.PUT_MAPPING, 0)) {
            url = getPath(AnnotationUtil.findAnnotation(psiMethod, AnnotationConstant.PUT_MAPPING));
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConstant.DELETE_MAPPING, 0)) {
            url = getPath(AnnotationUtil.findAnnotation(psiMethod, AnnotationConstant.DELETE_MAPPING));
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConstant.PATCH_MAPPING, 0)) {
            url = getPath(AnnotationUtil.findAnnotation(psiMethod, AnnotationConstant.PATCH_MAPPING));
        } else if (AnnotationUtil.isAnnotated(psiMethod, AnnotationConstant.REQUEST_MAPPING, 0)) {
            url = getPath(AnnotationUtil.findAnnotation(psiMethod, AnnotationConstant.REQUEST_MAPPING));
        } else {
            url = "";
        }
        return url;
    }


    /**
     * 获取被 RequestBody 注解修饰的参数, 只需要获取第一个即可, 因为多个不会生效.
     *
     * @param psiMethod
     * @return
     */
    public static PsiParameter getRequestBodyParam(@NotNull PsiMethod psiMethod) {
        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();

        for (PsiParameter parameter : parameters) {
            if (AnnotationUtil.isAnnotated(parameter, AnnotationConstant.REQUEST_BODY, 0)) {
                return parameter;
            }
        }
        return null;
    }


    /**
     * 构建 Header
     *
     * @param settings
     * @param psiMethod
     * @return
     */
    @NotNull
    public static List<Header> buildHeader(Settings settings, @NotNull PsiMethod psiMethod) {

        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();

        List<Header> list = new ArrayList<>();
        for (PsiParameter parameter : parameters) {

            if (!AnnotationUtil.isAnnotated(parameter, AnnotationConstant.REQUEST_HEADER, 0)) {
                continue;
            }

            PsiType type = parameter.getType();

            // 不是 String 就不处理了
            if (!type.getPresentableText().equals("String")) {
                continue;
            }

            Header header = new Header();
            header.setRequired(true);
            header.setName(parameter.getName());
            list.add(header);
        }
        return list;
    }

    /**
     * 构建 body
     *
     * @param settings
     * @param parameter
     * @return
     */
    @Nullable
    public static List<Body> buildBody(Settings settings, PsiParameter parameter) {

        PsiType type = parameter.getType();

        // 基本类型
        if (type instanceof PsiPrimitiveType || FieldTypeConstant.FIELD_TYPE.containsKey(type.getPresentableText())) {
            List<Body> list = new ArrayList<>();
            list.add(buildBodyFromParameter(settings, parameter));
            return list;
        } else {
            PsiClass psiClass = PsiUtil.resolveClassInType(type);
            if (psiClass != null) {

                List<Body> list = new ArrayList<>();

                for (PsiField field : psiClass.getAllFields()) {

                    if (settings.getExcludeFieldNames().contains(field.getName())) {
                        continue;
                    }
                    // 排除掉被 static 修饰的字段
                    if (CustomPsiUtils.hasModifierProperty(field, PsiModifier.STATIC)) {
                        continue;
                    }

                    Body requestParam = ParamPsiUtils.buildBodyParam(settings, field, null);
                    list.add(requestParam);
                }
                return list;

            }
        }

        return null;
    }


    @NotNull
    private static Body buildBodyFromParameter(@NotNull Settings settings, PsiParameter parameter) {

        Body body = new Body();
        body.setRequired(AnnotationUtil.isAnnotated(parameter, settings.getFieldRequiredAnnotationName(), 0));
        body.setName(parameter.getName());
        String fieldTypeName = parameter.getType().getPresentableText();
        body.setType(fieldTypeName);

        return body;
    }


    @NotNull
    public static String getReqBodyJson(Settings settings, @NotNull PsiParameter parameter) {
        Map<String, Object> fieldMap = new LinkedHashMap<>();
        String name = parameter.getName();
        PsiType type = parameter.getType();

        if (type instanceof PsiPrimitiveType) {
            fieldMap.put(name, PsiTypesUtil.getDefaultValue(type));

        } else if (FieldTypeConstant.FIELD_TYPE.containsKey(type.getPresentableText())) {
            fieldMap.put(name, FieldTypeConstant.FIELD_TYPE.get(type.getPresentableText()));
        } else {
            PsiClass psiClass = PsiUtil.resolveClassInType(type);
            if (psiClass != null) {
                fieldMap = ParamPsiUtils.getFieldsAndDefaultValue(psiClass, null);
            }
        }

        Gson gson = new GsonBuilder().serializeNulls().create();
        try {
            return GsonFormatUtil.gsonFormat(gson, fieldMap);
        } catch (IOException e) {
            return "{}";
        }

    }


    /**
     * 拼装为 kv 形式的键值对
     *
     * @param requestParam
     * @return
     */
    @NotNull
    public static String getReqParamKV(List<Param> requestParam) {

        if (requestParam == null || requestParam.isEmpty()) {
            return "";
        }

        StringBuilder paramKV = new StringBuilder();

        for (Param param : requestParam) {
            paramKV.append("&").append(param.getName()).append("=").append(param.getExample());
        }

        return paramKV.toString();
    }


    public static List<Param> buildFormParam(Settings settings, PsiMethod psiMethod) {

        if (!psiMethod.hasParameters()) {
            return null;
        }

        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();

        List<Param> list = new ArrayList<>();
        for (PsiParameter parameter : parameters) {

            if (AnnotationUtil.isAnnotated(parameter, AnnotationConstant.REQUEST_BODY, 0)) {
                continue;
            }

            if (AnnotationUtil.isAnnotated(parameter, AnnotationConstant.REQUEST_HEADER, 0)) {
                continue;
            }

            PsiType type = parameter.getType();

            if (type instanceof PsiPrimitiveType || FieldTypeConstant.FIELD_TYPE.containsKey(type.getPresentableText())) {
                list.add(buildPramFromParameter(settings, parameter));
            }
            // 在 param 中不存在对象类型
        }
        return list;

    }

    @NotNull
    private static Param buildPramFromParameter(@NotNull Settings settings, PsiParameter parameter) {

        Param param = new Param();
        param.setRequired(false);

        if (AnnotationUtil.isAnnotated(parameter, AnnotationConstant.REQUEST_PARAM, 0)) {
            PsiAnnotation annotation = parameter.getAnnotation(AnnotationConstant.REQUEST_PARAM);
            assert annotation != null;

            PsiNameValuePair[] nameValuePairs = annotation.getParameterList().getAttributes();

            // 初始为 true
            param.setRequired(true);
            for (PsiNameValuePair nameValuePair : nameValuePairs) {

                if (nameValuePair.getAttributeName().equalsIgnoreCase("required")
                        && Objects.requireNonNull(nameValuePair.getLiteralValue()).equalsIgnoreCase("false")) {
                    param.setRequired(false);
                }

            }
        }

        param.setName(parameter.getName());
        String fieldTypeName = parameter.getType().getPresentableText();
        param.setType(fieldTypeName);

        return param;
    }


    /**
     * 检查方法是否满足 Spring 相关条件
     * <p>
     * 不是构造方法, 且 公共 非静态, 有相关注解
     *
     * @param settings
     * @param psiMethod
     * @return true 不满足条件
     */
    public static boolean isSpringMethod(Settings settings, @NotNull PsiMethod psiMethod) {

        return !psiMethod.isConstructor()
                && CustomPsiUtils.hasModifierProperty(psiMethod, PsiModifier.PUBLIC)
                && !CustomPsiUtils.hasModifierProperty(psiMethod, PsiModifier.STATIC)
                && AnnotationUtil.isAnnotated(psiMethod, settings.getContainMethodAnnotationName(), 0);

    }

}
