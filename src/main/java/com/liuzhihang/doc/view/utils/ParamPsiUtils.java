package com.liuzhihang.doc.view.utils;

import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
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

    /**
     * 生成 body
     *
     * @param field
     * @param genericsMap key 是泛型 value 是对应的类型
     * @param parent
     */
    public static void buildBodyParam(PsiField field, Map<String, PsiType> genericsMap, Body parent) {

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

        // 提前替换字段比如 T -> UserDTO   List<T> -> List<UserDTO>
        // 如果是泛型, 且泛型字段是当前字段, 将当前字段类型替换为泛型类型, 替换完之后重新设置 body 的 type
        type = replaceFieldType(genericsMap, type);
        body.setType(type.getPresentableText());

        // 剩下都是 PsiClass 类型处理
        PsiClass fieldClass = PsiUtil.resolveClassInClassTypeOnly(type);

        if (fieldClass == null) {
            return;
        }

        // 判断 childClass 是否已经在根节点到当前节点的链表上存在, 存在的话则不继续递归
        String qualifiedName = fieldClass.getQualifiedName();

        if (StringUtils.isBlank(qualifiedName) || checkLinkedListHasTypeClass(body, qualifiedName)) {
            return;
        }

        body.setQualifiedNameForClassType(qualifiedName);
        Map<String, PsiType> fieldGenericsMap;
        PsiClass childClass;
        Body parentBody;
        // List Set or HashSet
        if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_COLLECTION)) {

            PsiType iterableType = PsiUtil.extractIterableTypeParameter(type, false);

            if (ignoreField(iterableType)) {
                return;
            }
            childClass = PsiUtil.resolveClassInClassTypeOnly(iterableType);
            if (childClass == null) {
                return;
            }
            // 集合参数构建, 集合就一个参数, 泛型 E
            fieldGenericsMap = CustomPsiUtils.getGenericsMap((PsiClassType) iterableType);
            parentBody = buildFieldGenericsBody("element", childClass, body);


        } else if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_MAP)) {
            // HashMap or Map 的泛型获取 value
            PsiType matKeyType = PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 0, false);

            // key 只能是 包装类型或者 String
            if (matKeyType == null || !FieldTypeConstant.PACKAGE_TYPE_SET.contains(matKeyType.getPresentableText())) {
                return;
            }
            PsiClass matKeyClass = PsiUtil.resolveClassInClassTypeOnly(matKeyType);
            if (matKeyClass == null) {
                return;
            }

            buildFieldGenericsBody("key", matKeyClass, body);

            // Value
            PsiType matValueType = PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 1, false);

            if (ignoreField(matValueType)) {
                return;
            }
            childClass = PsiUtil.resolveClassInClassTypeOnly(matValueType);
            if (childClass == null) {
                return;
            }

            fieldGenericsMap = CustomPsiUtils.getGenericsMap((PsiClassType) matValueType);
            parentBody = buildFieldGenericsBody("value", childClass, body);


        } else if (fieldClass.isEnum() || fieldClass.isInterface() || fieldClass.isAnnotationType()) {
            // 字段是类, 也可能带泛型
            return;

        } else {
            // 当前字段的泛型 和当前字段
            fieldGenericsMap = CustomPsiUtils.getGenericsMap((PsiClassType) type);
            parentBody = body;
            childClass = fieldClass;
        }

        if (type instanceof PsiPrimitiveType || FieldTypeConstant.FIELD_TYPE.containsKey(type.getPresentableText())) {
            return;
        }
        for (PsiField psiField : childClass.getAllFields()) {
            if (!DocViewUtils.isExcludeField(psiField)) {
                buildBodyParam(psiField, fieldGenericsMap, parentBody);
            }
        }

    }

    /**
     * 判断当前字段是否含有泛型, 从泛型映射表中替换字段类型
     * <p>
     * 替换字段比如 T -> UserDTO   List<T> -> List<UserDTO>
     *
     * @param genericsMap 含有泛型的 Map
     * @param type        当前字段的类型
     * @return
     */
    private static PsiType replaceFieldType(Map<String, PsiType> genericsMap, PsiType type) {

        if (genericsMap == null || genericsMap.isEmpty()) {
            return type;
        }

        if (!(type instanceof PsiClassType)) {
            return type;
        }

        if (!(type instanceof PsiClassReferenceType)) {
            return type;
        }

        // 当前字段的类型就是泛型 private T data;
        if (genericsMap.containsKey(type.getPresentableText())) {
            return genericsMap.get(type.getPresentableText());
        }

        // 当前字段含有泛型 List<T> -> List<UserDTO>
        PsiClass fieldClass = PsiUtil.resolveClassInClassTypeOnly(type);

        if (fieldClass == null || !fieldClass.hasTypeParameters()) {
            return type;
        }

        PsiType[] parameters = ((PsiClassType) type).getParameters();

        List<PsiType> psiTypeList = new ArrayList<>();
        // 替换泛型为实际类型
        for (PsiType psiType : parameters) {

            if (genericsMap.get(psiType.getPresentableText()) == null) {
                psiTypeList.add(psiType);
            } else {
                psiTypeList.add(genericsMap.get(psiType.getPresentableText()));
            }
        }

        if (!psiTypeList.isEmpty()) {

            PsiType[] psiTypes = new PsiType[psiTypeList.size()];

            for (int i = 0; i < psiTypeList.size(); i++) {
                psiTypes[i] = psiTypeList.get(i);
            }

            return PsiElementFactory.getInstance(fieldClass.getProject()).createType(fieldClass, psiTypes);
        }

        return type;
    }

    @NotNull
    private static Body buildFieldGenericsBody(String name, PsiClass genericsClass, Body parent) {
        Body listBody = new Body();
        listBody.setRequired(true);
        listBody.setName(name);
        listBody.setPsiElement(genericsClass);
        listBody.setType(genericsClass.getName());
        listBody.setDesc("");
        listBody.setParent(parent);

        parent.getChildList().add(listBody);
        return listBody;
    }

    private static boolean ignoreField(PsiType fieldType) {

        return fieldType instanceof PsiPrimitiveType
                || fieldType == null
                || FieldTypeConstant.FIELD_TYPE.containsKey(fieldType.getPresentableText());
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
     * @param genericMap
     * @param qualifiedNameList 根节点到当前节点的链表
     * @return
     */
    public static Map<String, Object> getFieldsAndDefaultValue(PsiClass psiClass, Map<String, PsiType> genericMap, LinkedList<String> qualifiedNameList) {

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
                continue;
            }

            // 如果是泛型, 且泛型字段是当前字段, 将当前字段类型替换为泛型类型
            type = replaceFieldType(genericMap, type);

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

                HashMap<String, Object> hashMap = new HashMap<>(4);

                PsiType matKeyType = PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 0, false);

                // key 只能是 包装类型或者 String
                if (matKeyType != null && FieldTypeConstant.PACKAGE_TYPE_SET.contains(matKeyType.getPresentableText())) {
                    // Value
                    PsiType matValueType = PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 1, false);
                    if (!ignoreField(matValueType)) {
                        PsiClass valueClass = PsiUtil.resolveClassInClassTypeOnly(matValueType);
                        if (valueClass != null) {
                            LinkedList<String> temp = new LinkedList<>(qualifiedNameList);
                            hashMap.put(matKeyType.getPresentableText(), getFieldsAndDefaultValue(valueClass, CustomPsiUtils.getGenericsMap((PsiClassType) matValueType), temp));
                        }
                    }

                }

                fieldMap.put(name, hashMap);

            } else if (psiClass.isEnum() || psiClass.isInterface() || psiClass.isAnnotationType()) {
                // enum or interface
                fieldMap.put(name, "");
            } else {

                // 参数类型为对象 校验是否递归
                PsiClass classInType = PsiUtil.resolveClassInType(type);

                LinkedList<String> temp = new LinkedList<>(qualifiedNameList);
                if (classInType != null && hasContainQualifiedName(temp, classInType.getQualifiedName())) {
                    fieldMap.put(name, "Object for " + classInType.getName());
                } else {
                    fieldMap.put(name, getFieldsAndDefaultValue(PsiUtil.resolveClassInType(type), CustomPsiUtils.getGenericsMap((PsiClassType) type), temp));
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
     * @param genericMap
     * @return
     */
    @NotNull
    public static Map<String, Object> getFieldsAndDefaultValue(PsiClass psiClass, Map<String, PsiType> genericMap) {

        return getFieldsAndDefaultValue(psiClass, genericMap, new LinkedList<>());
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


            PsiClass psiClass = PsiUtil.resolveClassInType(returnType);
            if (psiClass != null) {

                root.setQualifiedNameForClassType(psiClass.getQualifiedName());

                if (InheritanceUtil.isInheritor(psiClass, CommonClassNames.JAVA_UTIL_COLLECTION)) {

                    Body collectionBody = new Body();
                    collectionBody.setRequired(true);
                    collectionBody.setName("");
                    collectionBody.setPsiElement(psiClass);
                    collectionBody.setType(returnType.getPresentableText());
                    collectionBody.setDesc("");
                    collectionBody.setParent(root);

                    root.getChildList().add(collectionBody);


                    PsiType[] parameters = psiClassType.getParameters();
                    if (parameters.length != 0) {
                        PsiType psiType = parameters[0];

                        if (psiType instanceof PsiPrimitiveType || FieldTypeConstant.FIELD_TYPE.containsKey(psiType.getPresentableText())) {
                            return root;
                        }

                        // 泛型是类
                        PsiClass genericsPsiClass = PsiUtil.resolveClassInClassTypeOnly(psiType);

                        if (genericsPsiClass != null) {
                            buildBodyList(genericsPsiClass, null, collectionBody);
                        }
                    }
                } else {
                    // 返回值可能是带泛型的, psiClassType.getParameters() 获取到的

                    Map<String, PsiType> genericMap = CustomPsiUtils.getGenericsMap(psiClassType);

                    buildBodyList(psiClass, genericMap, root);

                }
            }
        } else {
            // 其他类型
        }
        return root;
    }

    public static void buildBodyList(@NotNull PsiClass psiClass, Map<String, PsiType> genericMap, Body parent) {

        for (PsiField field : psiClass.getAllFields()) {

            if (DocViewUtils.isExcludeField(field)) {
                continue;
            }

            ParamPsiUtils.buildBodyParam(field, genericMap, parent);
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
                // 返回类型是集合
                if (InheritanceUtil.isInheritor(psiClass, CommonClassNames.JAVA_UTIL_COLLECTION)) {

                    // 获取泛型
                    PsiType iterableType = PsiUtil.extractIterableTypeParameter(psiClassType, false);

                    if (iterableType == null) {
                        return "[]";
                    }


                    if (iterableType instanceof PsiPrimitiveType) {
                        return "[]";
                    }

                    if (CommonClassNames.JAVA_LANG_STRING_SHORT.equals(iterableType.getPresentableText())) {
                        return "[\"\"]";
                    }

                    if (FieldTypeConstant.FIELD_TYPE.containsKey(iterableType.getPresentableText())) {
                        return "[\"\"]";
                    }

                    PsiClass iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType);
                    Map<String, Object> fieldMap = ParamPsiUtils.getFieldsAndDefaultValue(iterableClass, null);

                    Object[] objectArr = {fieldMap};

                    return GsonFormatUtil.gsonFormat(objectArr);
                } else {

                    Map<String, PsiType> genericsMap = CustomPsiUtils.getGenericsMap(psiClassType);

                    Map<String, Object> fieldMap = ParamPsiUtils.getFieldsAndDefaultValue(psiClass, genericsMap);

                    return GsonFormatUtil.gsonFormat(fieldMap);
                }
            }
        } else {
            // 其他类型
        }

        return "";
    }

}
