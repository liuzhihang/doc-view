package com.liuzhihang.doc.view.utils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.constant.FieldTypeConstant;
import com.liuzhihang.doc.view.constant.SpringConstant;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.Header;
import com.liuzhihang.doc.view.dto.Param;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author liuzhihang
 * @date 2020/3/4 19:45
 */
public class SpringPsiUtils extends ParamPsiUtils {

    @NotNull
    public static String getMethod(PsiMethod psiMethod) {

        String method;

        if (AnnotationUtil.isAnnotated(psiMethod, SpringConstant.GET_MAPPING, 0)) {
            method = "GET";
        } else if (AnnotationUtil.isAnnotated(psiMethod, SpringConstant.POST_MAPPING, 0)) {
            method = "POST";
        } else if (AnnotationUtil.isAnnotated(psiMethod, SpringConstant.PUT_MAPPING, 0)) {
            method = "PUT";
        } else if (AnnotationUtil.isAnnotated(psiMethod, SpringConstant.DELETE_MAPPING, 0)) {
            method = "DELETE";
        } else if (AnnotationUtil.isAnnotated(psiMethod, SpringConstant.PATCH_MAPPING, 0)) {
            method = "PATCH";
        } else if (AnnotationUtil.isAnnotated(psiMethod, SpringConstant.REQUEST_MAPPING, 0)) {
            PsiAnnotation annotation = AnnotationUtil.findAnnotation(psiMethod, SpringConstant.REQUEST_MAPPING);
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
        PsiAnnotation annotation = AnnotationUtil.findAnnotation(psiClass, SpringConstant.REQUEST_MAPPING);

        return getPath(annotation);
    }


    @NotNull
    private static String getPath(PsiAnnotation annotation) {
        if (annotation == null) {
            return "";
        }
        String value = AnnotationUtil.getStringAttributeValue(annotation, "value");
        if (value != null) {
            if (value.startsWith("/")) {
                return value;
            } else {
                return "/" + value;
            }
        }
        return "";
    }


    public static String getMethodPath(PsiMethod psiMethod) {
        String url;

        if (AnnotationUtil.isAnnotated(psiMethod, SpringConstant.GET_MAPPING, 0)) {
            url = getPath(AnnotationUtil.findAnnotation(psiMethod, SpringConstant.GET_MAPPING));
        } else if (AnnotationUtil.isAnnotated(psiMethod, SpringConstant.POST_MAPPING, 0)) {
            url = getPath(AnnotationUtil.findAnnotation(psiMethod, SpringConstant.POST_MAPPING));
        } else if (AnnotationUtil.isAnnotated(psiMethod, SpringConstant.PUT_MAPPING, 0)) {
            url = getPath(AnnotationUtil.findAnnotation(psiMethod, SpringConstant.PUT_MAPPING));
        } else if (AnnotationUtil.isAnnotated(psiMethod, SpringConstant.DELETE_MAPPING, 0)) {
            url = getPath(AnnotationUtil.findAnnotation(psiMethod, SpringConstant.DELETE_MAPPING));
        } else if (AnnotationUtil.isAnnotated(psiMethod, SpringConstant.PATCH_MAPPING, 0)) {
            url = getPath(AnnotationUtil.findAnnotation(psiMethod, SpringConstant.PATCH_MAPPING));
        } else if (AnnotationUtil.isAnnotated(psiMethod, SpringConstant.REQUEST_MAPPING, 0)) {
            url = getPath(AnnotationUtil.findAnnotation(psiMethod, SpringConstant.REQUEST_MAPPING));
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

            // 通用排除字段
            if (DocViewUtils.isExcludeParameter(parameter)) {
                continue;
            }

            if (AnnotationUtil.isAnnotated(parameter, SpringConstant.REQUEST_BODY, 0)) {
                return parameter;
            }
        }
        return null;
    }


    /**
     * 构建 Header
     *
     * @param psiMethod
     * @return
     */
    @NotNull
    public static List<Header> buildHeader(@NotNull PsiMethod psiMethod) {

        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();

        List<Header> list = new ArrayList<>();
        for (PsiParameter parameter : parameters) {

            if (!AnnotationUtil.isAnnotated(parameter, SpringConstant.REQUEST_HEADER, 0)) {
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
     * @param parameter
     * @return
     */
    @NotNull
    public static Body buildBody(@NotNull PsiParameter parameter) {

        Body root = new Body();

        PsiType type = parameter.getType();
        // 基本类型
        if (type instanceof PsiPrimitiveType || FieldTypeConstant.FIELD_TYPE.containsKey(type.getPresentableText())) {

            Body body = new Body();
            body.setRequired(DocViewUtils.isRequired(parameter));
            body.setName(parameter.getName());
            body.setType(parameter.getType().getPresentableText());
            body.setParent(root);

            // 子集合只有一个
            root.getChildList().add(body);

        } else {
            PsiClass psiClass = PsiUtil.resolveClassInType(type);
            if (psiClass != null) {

                root.setQualifiedNameForClassType(psiClass.getQualifiedName());

                for (PsiField field : psiClass.getAllFields()) {

                    // 通用排除字段
                    if (DocViewUtils.isExcludeField(field)) {
                        continue;
                    }
                    ParamPsiUtils.buildBodyParam(field, null, root);
                }

            }
        }

        return root;
    }


    @NotNull
    public static String getReqBodyJson(@NotNull PsiParameter parameter, @NotNull Settings settings) {
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

        return GsonFormatUtil.gsonFormat(fieldMap);

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


    public static List<Param> buildFormParam(PsiMethod psiMethod) {

        if (!psiMethod.hasParameters()) {
            return null;
        }

        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();

        Set<String> paramNameSet = new HashSet<>();
        List<Param> list = new ArrayList<>();
        for (PsiParameter parameter : parameters) {

            // 有 @RequestBody 注解
            if (AnnotationUtil.isAnnotated(parameter, SpringConstant.REQUEST_BODY, 0)) {
                continue;
            }

            // 有 @RequestHeader 注解
            if (AnnotationUtil.isAnnotated(parameter, SpringConstant.REQUEST_HEADER, 0)) {
                continue;
            }

            // 需要排除的字段
            if (DocViewUtils.isExcludeParameter(parameter)) {
                continue;
            }

            // 已经包含该字段
            if (!paramNameSet.add(parameter.getName())) {
                continue;
            }

            PsiType type = parameter.getType();

            if (type instanceof PsiPrimitiveType || FieldTypeConstant.FIELD_TYPE.containsKey(type.getPresentableText())) {
                list.add(buildPramFromParameter(parameter));
            } else if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_COLLECTION)) {
            } else if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_MAP)) {
            } else {
                PsiClass fieldClass = PsiUtil.resolveClassInClassTypeOnly(type);
                if (fieldClass == null) {
                    continue;
                }
                // 参数是类, get 请求只有一层
                PsiField[] psiFields = fieldClass.getAllFields();
                for (PsiField field : psiFields) {
                    // 已经包含该字段
                    if (!paramNameSet.add(field.getName())) {
                        continue;
                    }

                    if (field.getType() instanceof PsiPrimitiveType
                            || FieldTypeConstant.FIELD_TYPE.containsKey(field.getType().getPresentableText())) {
                        list.add(buildPramFromField(field));
                    }

                }
            }

        }
        return list;

    }

    @NotNull
    private static Param buildPramFromField(PsiField field) {

        Param param = new Param();
        param.setPsiElement(field);
        param.setRequired(DocViewUtils.isRequired(field));
        param.setName(field.getName());
        param.setDesc(DocViewUtils.fieldDesc(field));
        param.setType(field.getType().getPresentableText());

        return param;
    }


    @NotNull
    private static Param buildPramFromParameter(PsiParameter parameter) {

        Param param = new Param();
        param.setRequired(false);

        if (AnnotationUtil.isAnnotated(parameter, SpringConstant.REQUEST_PARAM, 0)) {
            PsiAnnotation annotation = parameter.getAnnotation(SpringConstant.REQUEST_PARAM);
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
     * @param psiMethod
     * @return true 是spring 方法
     */
    public static boolean isSpringMethod(@NotNull PsiMethod psiMethod) {

        Settings settings = Settings.getInstance(psiMethod.getProject());


        return !psiMethod.isConstructor()
                && CustomPsiUtils.hasModifierProperty(psiMethod, PsiModifier.PUBLIC)
                && !CustomPsiUtils.hasModifierProperty(psiMethod, PsiModifier.STATIC)
                && AnnotationUtil.isAnnotated(psiMethod, settings.getContainMethodAnnotationName(), 0);

    }

}
