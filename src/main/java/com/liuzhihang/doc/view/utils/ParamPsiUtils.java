package com.liuzhihang.doc.view.utils;

import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.liuzhihang.doc.view.constant.FieldTypeConstant;
import com.liuzhihang.doc.view.dto.Body;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * 参数处理工具
 *
 * @author liuzhihang
 * @date 2020/11/17 15:15
 */
public class ParamPsiUtils {


    public static void buildBodyParam(PsiField field, PsiType[] genericArr, Body parent) {

        PsiType type = field.getType();

        Body body = new Body();
        body.setRequired(DocViewUtils.isRequired(field));
        body.setName(field.getName());
        body.setPsiElement(field);
        body.setType(type.getPresentableText());
        body.setDesc(DocViewUtils.fieldDesc(field));
        body.setParent(parent);

        parent.getChildList().add(body);
        if (type instanceof PsiPrimitiveType || FieldTypeConstant.FIELD_TYPE.containsKey(type.getPresentableText())) {
            return;
        }

        // 字段如果是类, 则会继续对当前 body 进行设置 child

        PsiClass childClass;

        if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_COLLECTION)) {
            // List Set or HashSet
            PsiType iterableType = PsiUtil.extractIterableTypeParameter(type, false);
            childClass = PsiUtil.resolveClassInClassTypeOnly(iterableType);
        } else if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_MAP)) {
            // HashMap or Map
            PsiType matValueType = PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 1, false);
            childClass = PsiUtil.resolveClassInClassTypeOnly(matValueType);
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
                return;
            }
            childClass = PsiUtil.resolveClassInClassTypeOnly(psiType);
        }

        if (childClass == null || childClass.isEnum() || childClass.isInterface() || childClass.isAnnotationType()) {
            return;
        }

        // 判断 childClass 是否已经在根节点到当前节点的链表上存在, 存在的话则不继续递归

        String qualifiedName = childClass.getQualifiedName();

        if (StringUtils.isBlank(qualifiedName) || checkLinkedListHasTypeClass(body, qualifiedName)) {
            return;
        } else {
            body.setQualifiedNameForClassType(qualifiedName);
        }

        for (PsiField psiField : childClass.getAllFields()) {
            if (!DocViewUtils.isExcludeField(psiField)) {
                buildBodyParam(psiField, null, body);
            }
        }
    }

    /**
     * 检查从当前节点到根节点的链表上是否存在当前类型的节点, 存在则说明递归了
     *
     * @param body
     * @param qualifiedName
     * @return
     */
    public static boolean checkLinkedListHasTypeClass(@NotNull Body body, @NotNull String qualifiedName) {

        Body temp = body.getParent();

        while (temp != null) {
            if (qualifiedName.equals(temp.getQualifiedNameForClassType())) {
                return true;
            }
            temp = temp.getParent();
        }

        return false;

    }

    /**
     * @param psiClass
     * @param genericArr
     * @param qualifiedNameList 根节点到当前节点的链表
     * @return
     */
    public static Map<String, Object> getFieldsAndDefaultValue(PsiClass psiClass, PsiType[] genericArr, LinkedList<String> qualifiedNameList) {

        Map<String, Object> fieldMap = new LinkedHashMap<>();

        if (psiClass == null || psiClass.isEnum() || psiClass.isInterface() || psiClass.isAnnotationType()) {
            return fieldMap;
        }

        // 设置当前类的类型
        qualifiedNameList.add(psiClass.getQualifiedName());


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
                // 引用类型
                String fieldTypeName = type.getPresentableText();
                // 指定的类型
                if (FieldTypeConstant.FIELD_TYPE.containsKey(fieldTypeName)) {
                    fieldMap.put(name, FieldTypeConstant.FIELD_TYPE.get(fieldTypeName));
                } else if (type instanceof PsiArrayType) {
                    // 数组类型
                    List<Object> list = new ArrayList<>();
                    PsiType deepType = type.getDeepComponentType();
                    if (deepType instanceof PsiPrimitiveType) {
                        list.add(PsiTypesUtil.getDefaultValue(deepType));
                    } else if (FieldTypeConstant.FIELD_TYPE.containsKey(deepType.getPresentableText())) {
                        list.add(FieldTypeConstant.FIELD_TYPE.get(deepType.getPresentableText()));
                    } else {
                        // 参数类型为对象 校验是否递归
                        PsiClass classInType = PsiUtil.resolveClassInType(deepType);

                        LinkedList<String> temp = new LinkedList<>(qualifiedNameList);
                        if (classInType != null && hasContainQualifiedName(temp, classInType.getQualifiedName())) {
                            list.add("Object for " + classInType.getName());
                        } else {
                            list.add(getFieldsAndDefaultValue(classInType, null, temp));
                        }
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

                            // 参数类型为对象 校验是否递归
                            LinkedList<String> temp = new LinkedList<>(qualifiedNameList);
                            if (hasContainQualifiedName(temp, iterableClass.getQualifiedName())) {
                                list.add("Object for " + iterableClass.getName());
                            } else {
                                list.add(getFieldsAndDefaultValue(iterableClass, null, temp));
                            }

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

                        // 参数类型为对象 校验是否递归
                        PsiClass classInType = PsiUtil.resolveClassInType(type);

                        LinkedList<String> temp = new LinkedList<>(qualifiedNameList);
                        if (classInType != null && hasContainQualifiedName(temp, classInType.getQualifiedName())) {
                            fieldMap.put(name, "Object for " + classInType.getName());
                        } else {
                            fieldMap.put(name, getFieldsAndDefaultValue(PsiUtil.resolveClassInType(type), null, temp));
                        }


                    }


                }
            }

        }
        return fieldMap;
    }

    private static boolean hasContainQualifiedName(LinkedList<String> qualifiedNameList, String qualifiedName) {

        if (qualifiedNameList.isEmpty()) {
            return false;
        }

        for (String s : qualifiedNameList) {
            if (s.equals(qualifiedName)) {
                return true;
            }
        }

        return false;

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

        return getFieldsAndDefaultValue(psiClass, genericArr, new LinkedList<String>());
    }

    @NotNull
    public static Body buildRespBody(PsiType returnType) {

        Body root = new Body();

        if (returnType instanceof PsiPrimitiveType || FieldTypeConstant.FIELD_TYPE.containsKey(returnType.getPresentableText())) {

            Body body = new Body();
            body.setRequired(false);
            body.setName(null);
            body.setType(returnType.getPresentableText());
            body.setParent(root);

            root.getChildList().add(body);
            return root;
        }

        if (returnType instanceof PsiClassType) {

            PsiClassType psiClassType = (PsiClassType) returnType;

            // 返回值可能是 Result<T> Result<T, K> 泛型的
            PsiClass psiClass = PsiUtil.resolveClassInType(returnType);
            if (psiClass != null) {

                root.setQualifiedNameForClassType(psiClass.getQualifiedName());

                if (InheritanceUtil.isInheritor(psiClass, CommonClassNames.JAVA_UTIL_COLLECTION)) {
                    PsiType[] parameters = psiClassType.getParameters();
                    if (parameters.length != 0) {
                        PsiType psiType = parameters[0];

                        if (psiType instanceof PsiPrimitiveType || FieldTypeConstant.FIELD_TYPE.containsKey(psiType.getPresentableText())) {
                            return root;
                        }

                        // 泛型是类
                        PsiClass genericsPsiClass = PsiUtil.resolveClassInClassTypeOnly(psiType);

                        if (genericsPsiClass != null) {
                            buildBodyList(genericsPsiClass, null, root);
                        }
                    }
                } else {
                    buildBodyList(psiClass, psiClassType.getParameters(), root);
                }
            }
        } else {
            // 其他类型
        }
        return root;
    }

    public static void buildBodyList(@NotNull PsiClass psiClass, PsiType[] o, Body parent) {

        for (PsiField field : psiClass.getAllFields()) {

            if (DocViewUtils.isExcludeField(field)) {
                continue;
            }

            ParamPsiUtils.buildBodyParam(field, o, parent);
        }

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
