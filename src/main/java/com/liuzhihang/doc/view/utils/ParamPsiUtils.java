package com.liuzhihang.doc.view.utils;

import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.liuzhihang.doc.view.constant.FieldTypeConstant;
import com.liuzhihang.doc.view.dto.Body;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * 参数处理工具
 *
 * @author liuzhihang
 * @date 2020/11/17 15:15
 */
public class ParamPsiUtils {


    @NotNull
    public static Body buildBodyParam(PsiField field, PsiType[] genericArr) {

        PsiType type = field.getType();

        Body body = new Body();
        body.setRequired(DocViewUtils.isRequired(field));
        body.setName(field.getName());
        body.setPsiElement(field);
        body.setType(type.getPresentableText());
        body.setDesc(DocViewUtils.fieldDesc(field));

        if (type instanceof PsiPrimitiveType || FieldTypeConstant.FIELD_TYPE.containsKey(type.getPresentableText())) {
            return body;
        } else if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_COLLECTION)) {
            // List Set or HashSet
            List<Body> list = new ArrayList<>();
            PsiType iterableType = PsiUtil.extractIterableTypeParameter(type, false);
            PsiClass iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType);
            if (iterableClass != null) {
                for (PsiField psiField : iterableClass.getAllFields()) {
                    if (!DocViewUtils.isExcludeField(psiField)) {
                        Body requestParam = buildBodyParam(psiField, null);
                        list.add(requestParam);
                    }
                }
            }
            body.setBodyList(list);
        } else if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_MAP)) {
            // HashMap or Map
            List<Body> list = new ArrayList<>();
            PsiType matValueType = PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 1, false);
            PsiClass iterableClass = PsiUtil.resolveClassInClassTypeOnly(matValueType);
            if (iterableClass != null) {
                for (PsiField psiField : iterableClass.getAllFields()) {
                    if (!DocViewUtils.isExcludeField(psiField)) {
                        Body requestParam = buildBodyParam(psiField, null);
                        list.add(requestParam);
                    }
                }
            }
            body.setBodyList(list);
        } else {
            PsiType psiType;
            if (type.getPresentableText().equals("T") && genericArr != null && genericArr.length >= 1) {
                // T 泛型
                psiType = genericArr[0];
            } else if (type.getPresentableText().equals("K") && genericArr != null && genericArr.length >= 2) {
                // K 泛型
                psiType = genericArr[1];
            } else {
                psiType = type;
            }

            if (FieldTypeConstant.FIELD_TYPE.containsKey(psiType.getPresentableText())) {
                body.setType(psiType.getPresentableText());
                return body;
            }

            PsiClass psiClass = PsiUtil.resolveClassInClassTypeOnly(psiType);
            List<Body> list = new ArrayList<>();
            if (psiClass != null && !psiClass.isEnum() && !psiClass.isInterface() && !psiClass.isAnnotationType()) {
                for (PsiField psiField : psiClass.getAllFields()) {
                    if (!DocViewUtils.isExcludeField(psiField)) {
                        Body requestParam = buildBodyParam(psiField, null);
                        list.add(requestParam);
                    }
                }
            }
            body.setBodyList(list);
        }

        return body;
    }

    /**
     * 获取字段和字段的默认值
     *
     * @param psiClass
     * @param genericArr
     * @return
     */
    @NotNull
    public static Map<String, Object> getFieldsAndDefaultValue(PsiClass psiClass, PsiType[] genericArr) {

        Map<String, Object> fieldMap = new LinkedHashMap<>();

        if (psiClass != null && !psiClass.isEnum() && !psiClass.isInterface() && !psiClass.isAnnotationType()) {
            for (PsiField field : psiClass.getAllFields()) {

                if (DocViewUtils.isExcludeField(field)) {
                    continue;
                }

                PsiType type = field.getType();
                String name = field.getName();
                if (type instanceof PsiPrimitiveType) {
                    // 基本类型
                    fieldMap.put(name, PsiTypesUtil.getDefaultValue(type));
                } else {
                    //reference Type
                    String fieldTypeName = type.getPresentableText();
                    // 指定的类型
                    if (FieldTypeConstant.FIELD_TYPE.containsKey(fieldTypeName)) {
                        fieldMap.put(name, FieldTypeConstant.FIELD_TYPE.get(fieldTypeName));
                    } else if (type instanceof PsiArrayType) {
                        //array type
                        List<Object> list = new ArrayList<>();
                        PsiType deepType = type.getDeepComponentType();
                        String deepTypeName = deepType.getPresentableText();
                        if (deepType instanceof PsiPrimitiveType) {
                            list.add(PsiTypesUtil.getDefaultValue(deepType));
                        } else if (FieldTypeConstant.FIELD_TYPE.containsKey(deepTypeName)) {
                            list.add(FieldTypeConstant.FIELD_TYPE.get(deepTypeName));
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
                            if (FieldTypeConstant.FIELD_TYPE.containsKey(classTypeName)) {
                                list.add(FieldTypeConstant.FIELD_TYPE.get(classTypeName));
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

                        if (FieldTypeConstant.FIELD_TYPE.containsKey(type.getPresentableText())) {
                            fieldMap.put(name, FieldTypeConstant.FIELD_TYPE.get(type.getPresentableText()));
                        } else {
                            fieldMap.put(name, getFieldsAndDefaultValue(PsiUtil.resolveClassInType(type), null));
                        }


                    }
                }
            }
        }
        return fieldMap;
    }

    @NotNull
    public static List<Body> buildRespBody(PsiType returnType) {

        List<Body> list = new ArrayList<>();
        if (returnType instanceof PsiPrimitiveType || FieldTypeConstant.FIELD_TYPE.containsKey(returnType.getPresentableText())) {
            Body body = new Body();
            body.setRequired(false);
            body.setName(null);
            body.setType(returnType.getPresentableText());
            list.add(body);
        } else if (returnType instanceof PsiClassType) {

            PsiClassType psiClassType = (PsiClassType) returnType;

            // 返回值可能是 Result<T> Result<T, K> 泛型的
            PsiClass psiClass = PsiUtil.resolveClassInType(returnType);
            if (psiClass != null) {
                if (InheritanceUtil.isInheritor(psiClass, CommonClassNames.JAVA_UTIL_COLLECTION)) {
                    PsiType[] parameters = psiClassType.getParameters();
                    if (parameters.length != 0) {
                        PsiType psiType = parameters[0];

                        if (psiType instanceof PsiPrimitiveType || FieldTypeConstant.FIELD_TYPE.containsKey(psiType.getPresentableText())) {
                            return list;
                        }

                        // 泛型的类型
                        PsiClass genericsPsiClass = PsiUtil.resolveClassInClassTypeOnly(psiType);

                        if (genericsPsiClass != null) {
                            return buildBodyList(genericsPsiClass, null);
                        }
                    }
                } else {
                    return buildBodyList(psiClass, psiClassType.getParameters());
                }
            }
        } else {
            // 其他类型
        }
        return list;
    }

    @NotNull
    public static List<Body> buildBodyList(@NotNull PsiClass psiClass, PsiType[] o) {

        List<Body> list = new ArrayList<>();


        for (PsiField field : psiClass.getAllFields()) {

            if (DocViewUtils.isExcludeField(field)) {
                continue;
            }

            Body requestParam = ParamPsiUtils.buildBodyParam(field, o);
            list.add(requestParam);
        }
        return list;
    }


    @NotNull
    public static String getRespBodyJson(PsiType returnType) {


        if (returnType instanceof PsiPrimitiveType || FieldTypeConstant.FIELD_TYPE.containsKey(returnType.getPresentableText())) {
            return "";
        } else if (returnType instanceof PsiClassType) {

            PsiClassType psiClassType = (PsiClassType) returnType;

            PsiClass psiClass = PsiUtil.resolveClassInType(returnType);
            if (psiClass != null) {

                if (InheritanceUtil.isInheritor(psiClass, CommonClassNames.JAVA_UTIL_COLLECTION)) {
                    // 集合类型
                    PsiType[] parameters = psiClassType.getParameters();

                    if (parameters.length == 0) {
                        return "[]";
                    }

                    PsiType psiType = parameters[0];

                    if (psiType instanceof PsiPrimitiveType || FieldTypeConstant.FIELD_TYPE.containsKey(psiType.getPresentableText())) {
                        return "[]";
                    }

                    PsiClass iterableClass = PsiUtil.resolveClassInClassTypeOnly(psiType);
                    Map<String, Object> fieldMap = ParamPsiUtils.getFieldsAndDefaultValue(iterableClass, null);

                    Object[] objectArr = {fieldMap};

                    return GsonFormatUtil.gsonFormat(objectArr);
                } else {
                    Map<String, Object> fieldMap = ParamPsiUtils.getFieldsAndDefaultValue(psiClass, psiClassType.getParameters());

                    return GsonFormatUtil.gsonFormat(fieldMap);
                }
            }
        } else {
            // 其他类型
        }

        return "";
    }

}
