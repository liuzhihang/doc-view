package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.constant.FieldTypeConstant;
import com.liuzhihang.doc.view.dto.Body;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author liuzhihang
 * @date 2020/11/16 20:37
 */
public class DubboPsiUtils {


    /**
     * 检查方法是否满足 Dubbo 相关条件
     * <p>
     * 不是构造方法, 且 公共 非静态, 有相关注解
     *
     * @param project
     * @param psiMethod
     * @return true 不满足条件
     */
    public static boolean isDubboMethod(Project project, @NotNull PsiMethod psiMethod) {

        PsiCodeBlock body = psiMethod.getBody();

        if (body != null) {
            return false;
        }

        return !CustomPsiUtils.hasModifierProperty(psiMethod, PsiModifier.STATIC);

    }

    @NotNull
    public static List<Body> buildBody(Settings settings, @NotNull PsiMethod psiMethod) {


        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();


        List<Body> list = new ArrayList<>();

        for (PsiParameter parameter : parameters) {

            PsiType type = parameter.getType();

            // 集合
            Body body = new Body();
            body.setRequired(false);
            body.setName(parameter.getName());
            body.setType(type.getPresentableText());

            // 基本类型
            if (type instanceof PsiPrimitiveType || FieldTypeConstant.FIELD_TYPE.containsKey(type.getPresentableText())) {

            } else if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_COLLECTION)) {

                List<Body> bodyList = new ArrayList<>();
                PsiType iterableType = PsiUtil.extractIterableTypeParameter(type, false);
                PsiClass iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType);
                if (iterableClass != null) {
                    for (PsiField psiField : iterableClass.getAllFields()) {
                        if (!settings.getExcludeFieldNames().contains(psiField.getName())
                                && !CustomPsiUtils.hasModifierProperty(psiField, PsiModifier.STATIC)) {

                            Body requestParam = ParamPsiUtils.buildBodyParam(settings, psiField, null);
                            bodyList.add(requestParam);
                        }
                    }
                }

                body.setBodyList(bodyList);

            } else if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_MAP)) {
                //  map

                List<Body> bodyList = new ArrayList<>();
                PsiType matValueType = PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 1, false);
                PsiClass iterableClass = PsiUtil.resolveClassInClassTypeOnly(matValueType);
                if (iterableClass != null) {
                    for (PsiField psiField : iterableClass.getAllFields()) {
                        if (!settings.getExcludeFieldNames().contains(psiField.getName())
                                && !CustomPsiUtils.hasModifierProperty(psiField, PsiModifier.STATIC)) {
                            Body requestParam = ParamPsiUtils.buildBodyParam(settings, psiField, null);
                            bodyList.add(requestParam);
                        }
                    }
                }
                body.setBodyList(bodyList);

            } else if (type instanceof PsiClassType) {
                // 对象
                PsiClass psiClass = PsiUtil.resolveClassInClassTypeOnly(type);
                List<Body> bodyList = new ArrayList<>();
                if (psiClass != null && !psiClass.isEnum() && !psiClass.isInterface() && !psiClass.isAnnotationType()) {
                    for (PsiField psiField : psiClass.getAllFields()) {
                        if (!settings.getExcludeFieldNames().contains(psiField.getName())
                                && !CustomPsiUtils.hasModifierProperty(psiField, PsiModifier.STATIC)) {
                            Body requestParam = ParamPsiUtils.buildBodyParam(settings, psiField, null);
                            bodyList.add(requestParam);
                        }
                    }
                }
                body.setBodyList(bodyList);

            } else {
                // 未知类型

            }
            list.add(body);

        }

        return list;
    }

    @NotNull
    public static String getReqBodyJson(Settings settings, @NotNull PsiMethod psiMethod) {

        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();

        List<Map<String, Object>> fieldMapList = new ArrayList<>();

        for (PsiParameter parameter : parameters) {
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
            fieldMapList.add(fieldMap);
        }

        return GsonFormatUtil.gsonFormat(fieldMapList);
    }
}
