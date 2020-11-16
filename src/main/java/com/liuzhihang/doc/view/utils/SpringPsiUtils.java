package com.liuzhihang.doc.view.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.liuzhihang.doc.view.component.Settings;
import com.liuzhihang.doc.view.config.AnnotationConfig;
import com.liuzhihang.doc.view.config.FieldTypeConfig;
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


    /**
     * 获取被 RequestBody 注解修饰的参数, 只需要获取第一个即可, 因为多个不会生效.
     *
     * @param psiMethod
     * @return
     */
    public static PsiParameter getRequestBodyParam(@NotNull PsiMethod psiMethod) {
        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();

        for (PsiParameter parameter : parameters) {
            if (AnnotationUtil.isAnnotated(parameter, AnnotationConfig.REQUEST_BODY, 0)) {
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

            if (!AnnotationUtil.isAnnotated(parameter, AnnotationConfig.REQUEST_HEADER, 0)) {
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
        if (type instanceof PsiPrimitiveType || FieldTypeConfig.FIELD_TYPE.containsKey(type.getPresentableText())) {
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

                    Body requestParam = buildBodyParam(settings, field, null);
                    list.add(requestParam);
                }
                return list;

            }
        }

        return null;
    }

    @NotNull
    private static Param buildPramFromParameter(@NotNull Settings settings, PsiParameter parameter) {

        Param param = new Param();
        param.setRequired(AnnotationUtil.isAnnotated(parameter, settings.getFieldRequiredAnnotationName(), 0));
        param.setName(parameter.getName());
        String fieldTypeName = parameter.getType().getPresentableText();
        param.setType(fieldTypeName);

        return param;
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

        } else if (FieldTypeConfig.FIELD_TYPE.containsKey(type.getPresentableText())) {
            fieldMap.put(name, FieldTypeConfig.FIELD_TYPE.get(type.getPresentableText()));
        } else {
            PsiClass psiClass = PsiUtil.resolveClassInType(type);
            if (psiClass != null) {
                fieldMap = getFieldsAndDefaultValue(psiClass, null);
            }
        }

        Gson gson = new GsonBuilder().serializeNulls().create();
        try {
            return GsonFormatUtil.gsonFormat(gson, fieldMap);
        } catch (IOException e) {
            return "{}";
        }

    }

    @NotNull
    public static List<Body> buildRespBody(Settings settings, PsiType returnType) {

        List<Body> list = new ArrayList<>();
        if (returnType instanceof PsiPrimitiveType || FieldTypeConfig.FIELD_TYPE.containsKey(returnType.getPresentableText())) {
            Body param = new Body();
            param.setRequired(false);
            param.setName(null);
            param.setType(returnType.getPresentableText());
            list.add(param);
        } else if (returnType instanceof PsiClassType) {

            PsiClassType psiClassType = (PsiClassType) returnType;

            // 返回值可能是 Result<T> Result<T, K> 泛型的

            PsiClass psiClass = PsiUtil.resolveClassInType(returnType);
            if (psiClass != null) {
                for (PsiField field : psiClass.getAllFields()) {

                    if (settings.getExcludeFieldNames().contains(field.getName())) {
                        continue;
                    }
                    // 排除掉被 static 修饰的字段
                    if (CustomPsiUtils.hasModifierProperty(field, PsiModifier.STATIC)) {
                        continue;
                    }

                    Body requestParam = buildBodyParam(settings, field, psiClassType.getParameters());
                    list.add(requestParam);
                }
            }

        } else {
            // 其他类型
        }
        return list;
    }

    @NotNull
    public static String getRespBodyJson(Settings settings, PsiType returnType) {


        if (returnType instanceof PsiPrimitiveType || FieldTypeConfig.FIELD_TYPE.containsKey(returnType.getPresentableText())) {
            return "";
        } else if (returnType instanceof PsiClassType) {

            PsiClassType psiClassType = (PsiClassType) returnType;

            PsiClass psiClass = PsiUtil.resolveClassInType(returnType);
            if (psiClass != null) {
                Map<String, Object> fieldMap = getFieldsAndDefaultValue(psiClass, psiClassType.getParameters());
                Gson gson = new GsonBuilder().serializeNulls().create();
                try {
                    return GsonFormatUtil.gsonFormat(gson, fieldMap);
                } catch (IOException e) {
                    return "{}";
                }
            }
        } else {
            // 其他类型
        }

        return "";
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


    @NotNull
    private static Body buildBodyParam(@NotNull Settings settings, PsiField field, PsiType[] genericArr) {


        Body param = new Body();
        param.setRequired(AnnotationUtil.isAnnotated(field, settings.getFieldRequiredAnnotationName(), 0));
        param.setName(field.getName());

        PsiType type = field.getType();

        param.setType(type.getPresentableText());

        PsiDocComment docComment = field.getDocComment();

        if (docComment != null) {
            // param.setExample();
            // 参数举例, 使用 tag 判断
            param.setDesc(CustomPsiCommentUtils.getComment(docComment));
        }

        if (type instanceof PsiPrimitiveType || FieldTypeConfig.FIELD_TYPE.containsKey(type.getPresentableText())) {
            return param;
        } else if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_COLLECTION)) {
            // List Set or HashSet
            List<Body> list = new ArrayList<>();
            PsiType iterableType = PsiUtil.extractIterableTypeParameter(type, false);
            PsiClass iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType);
            if (iterableClass != null) {
                for (PsiField psiField : iterableClass.getAllFields()) {
                    if (!settings.getExcludeFieldNames().contains(field.getName())
                            && !CustomPsiUtils.hasModifierProperty(psiField, PsiModifier.STATIC)) {

                        Body requestParam = buildBodyParam(settings, psiField, null);
                        list.add(requestParam);
                    }
                }
            }
            param.setObjectReqList(list);
        } else if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_MAP)) {
            // HashMap or Map
            List<Body> list = new ArrayList<>();
            PsiType matValueType = PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 1, false);
            PsiClass iterableClass = PsiUtil.resolveClassInClassTypeOnly(matValueType);
            if (iterableClass != null) {
                for (PsiField psiField : iterableClass.getAllFields()) {
                    if (!settings.getExcludeFieldNames().contains(field.getName())
                            && !CustomPsiUtils.hasModifierProperty(psiField, PsiModifier.STATIC)) {
                        Body requestParam = buildBodyParam(settings, psiField, null);
                        list.add(requestParam);
                    }
                }
            }
            param.setObjectReqList(list);
        } else {
            PsiClass psiClass;
            if (type.getPresentableText().equals("T") && genericArr != null && genericArr.length >= 1) {
                // T 泛型
                PsiType psiType = genericArr[0];
                psiClass = PsiUtil.resolveClassInClassTypeOnly(psiType);
            } else if (type.getPresentableText().equals("K") && genericArr != null && genericArr.length >= 2) {
                // K 泛型
                PsiType psiType = genericArr[1];
                psiClass = PsiUtil.resolveClassInClassTypeOnly(psiType);
            } else {
                psiClass = PsiUtil.resolveClassInClassTypeOnly(type);
            }

            List<Body> list = new ArrayList<>();
            if (psiClass != null && !psiClass.isEnum() && !psiClass.isInterface() && !psiClass.isAnnotationType()) {
                for (PsiField psiField : psiClass.getAllFields()) {
                    if (!settings.getExcludeFieldNames().contains(field.getName())
                            && !CustomPsiUtils.hasModifierProperty(psiField, PsiModifier.STATIC)) {
                        Body requestParam = buildBodyParam(settings, psiField, null);
                        list.add(requestParam);
                    }
                }
            }
            param.setObjectReqList(list);
        }

        return param;
    }

    public static List<Param> buildFormParam(Settings settings, PsiMethod psiMethod) {

        if (!psiMethod.hasParameters()) {
            return null;
        }

        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();

        List<Param> list = new ArrayList<>();
        for (PsiParameter parameter : parameters) {

            if (AnnotationUtil.isAnnotated(parameter, AnnotationConfig.REQUEST_BODY, 0)) {
                continue;
            }

            if (AnnotationUtil.isAnnotated(parameter, AnnotationConfig.REQUEST_HEADER, 0)) {
                continue;
            }

            PsiType type = parameter.getType();

            if (type instanceof PsiPrimitiveType || FieldTypeConfig.FIELD_TYPE.containsKey(type.getPresentableText())) {
                list.add(buildPramFromParameter(settings, parameter));
            }
            // 在 param 中不存在对象类型
        }
        return list;

    }


    @NotNull
    public static Map<String, Object> getFieldsAndDefaultValue(PsiClass psiClass, PsiType[] genericArr) {

        Map<String, Object> fieldMap = new LinkedHashMap<>();
        // Map<String, Object> commentFieldMap = new LinkedHashMap<>();

        if (psiClass != null && !psiClass.isEnum() && !psiClass.isInterface() && !psiClass.isAnnotationType()) {
            for (PsiField field : psiClass.getAllFields()) {

                if (field.getModifierList() != null && field.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                    continue;
                }


                PsiType type = field.getType();
                String name = field.getName();
                // 判断注解 javax.annotation.Resource   org.springframework.beans.factory.annotation.Autowired
                PsiAnnotation[] annotations = field.getAnnotations();
                if (annotations.length > 0 && containsAnnotation(annotations)) {
                    fieldMap.put(name, "");
                } else if (type instanceof PsiPrimitiveType) {
                    // 基本类型
                    fieldMap.put(name, PsiTypesUtil.getDefaultValue(type));
                } else {
                    //reference Type
                    String fieldTypeName = type.getPresentableText();
                    // 指定的类型
                    if (FieldTypeConfig.FIELD_TYPE.containsKey(fieldTypeName)) {
                        fieldMap.put(name, FieldTypeConfig.FIELD_TYPE.get(fieldTypeName));
                    } else if (type instanceof PsiArrayType) {
                        //array type
                        List<Object> list = new ArrayList<>();
                        PsiType deepType = type.getDeepComponentType();
                        String deepTypeName = deepType.getPresentableText();
                        if (deepType instanceof PsiPrimitiveType) {
                            list.add(PsiTypesUtil.getDefaultValue(deepType));
                        } else if (FieldTypeConfig.FIELD_TYPE.containsKey(deepTypeName)) {
                            list.add(FieldTypeConfig.FIELD_TYPE.get(deepTypeName));
                        } else {
                            list.add(getFieldsAndDefaultValue(PsiUtil.resolveClassInType(deepType), null));
                        }
                        fieldMap.put(name, list);
                    } else if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_COLLECTION)) {
                        // List Set or HashSet
                        List<Object> list = new ArrayList<>();
                        PsiType iterableType = PsiUtil.extractIterableTypeParameter(type, false);
                        PsiClass iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType);
                        if (iterableClass != null) {
                            String classTypeName = iterableClass.getName();
                            if (FieldTypeConfig.FIELD_TYPE.containsKey(classTypeName)) {
                                list.add(FieldTypeConfig.FIELD_TYPE.get(classTypeName));
                            } else {
                                list.add(getFieldsAndDefaultValue(iterableClass, null));
                            }
                        }
                        fieldMap.put(name, list);
                    } else if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_MAP)) {
                        // HashMap or Map
                        fieldMap.put(name, new HashMap<>(4));
                    } else if (psiClass.isEnum() || psiClass.isInterface() || psiClass.isAnnotationType()) {
                        // enum or interface
                        fieldMap.put(name, "");
                    } else {

                        if (type.getPresentableText().equals("T") && genericArr != null && genericArr.length >= 1) {
                            // T 泛型
                            type = genericArr[0];
                        } else if (type.getPresentableText().equals("K") && genericArr != null && genericArr.length >= 2) {
                            // K 泛型
                            type = genericArr[1];
                        }

                        fieldMap.put(name, getFieldsAndDefaultValue(PsiUtil.resolveClassInType(type), null));
                    }
                }
            }
        }
        return fieldMap;
    }

    /**
     * 是否包含指定的注解
     *
     * @param annotations
     * @return
     */
    private static boolean containsAnnotation(@NotNull PsiAnnotation[] annotations) {
        for (PsiAnnotation annotation : annotations) {
            if (FieldTypeConfig.ANNOTATION_TYPES.contains(annotation.getQualifiedName())) {
                return true;
            }
        }
        return false;
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
