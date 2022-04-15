package com.liuzhihang.doc.view.utils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.NonBlockingReadAction;
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomService;
import com.liuzhihang.doc.view.constant.DubboConstant;
import com.liuzhihang.doc.view.constant.FieldTypeConstant;
import com.liuzhihang.doc.view.dom.BeansDomElement;
import com.liuzhihang.doc.view.dto.Body;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author liuzhihang
 * @date 2020/11/16 20:37
 */
public class DubboPsiUtils {

    /**
     * 检查是否是 dubbo 接口
     * <p>
     * 当前判断方式: 是接口, 并且有 xml 关联 或者实现类上有注解
     *
     * @param psiClass
     * @return
     */
    public static boolean isDubboClass(@NotNull PsiClass psiClass) {

        if (!psiClass.isInterface()) {
            return false;
        }


        // 包含相关注解, 则是 Dubbo 接口
        if (AnnotationUtil.isAnnotated(psiClass, DubboConstant.SERVICE_ANNOTATIONS, 0)) {
            return true;
        }
        // 实现类上要有注解
        if (ClassInheritorsSearch.search(psiClass).findAll()
                .stream()
                .anyMatch(psiClass1 -> AnnotationUtil.isAnnotated(psiClass1, DubboConstant.SERVICE_ANNOTATIONS, 0))) {
            return true;
        }

        Project project = psiClass.getProject();

        // xml 中的所有配置
        List<DomFileElement<BeansDomElement>> fileElements = DomService.getInstance().getFileElements(BeansDomElement.class, project, GlobalSearchScope.allScope(project));

        // 只需要判断namespace
        String qualifiedName = psiClass.getQualifiedName();

        return fileElements.stream().anyMatch(elements ->
                elements.getRootElement().getDubboServiceDomElements()
                        .stream()
                        .anyMatch(dubboServiceDomElement -> Objects.requireNonNull(dubboServiceDomElement.getInterface().getRawText()).equals(qualifiedName)));
    }

    /**
     * 检查方法是否满足 Dubbo 相关条件
     * <p>
     * 不是构造方法, 且 公共 非静态, 有相关注解
     *
     * @param psiMethod
     * @return true 是 dubbo 方法
     */
    public static boolean isDubboMethod(@NotNull PsiMethod psiMethod) {

        PsiCodeBlock body = psiMethod.getBody();

        if (body != null) {
            return false;
        }

        return !CustomPsiUtils.hasModifierProperty(psiMethod, PsiModifier.STATIC);

    }


    public static List<PsiClass> findDocViewFromModule(Module module) {

        Collection<PsiAnnotation> psiAnnotations = JavaAnnotationIndex.getInstance().get("Service", module.getProject(), GlobalSearchScope.moduleScope(module));
        psiAnnotations.addAll(JavaAnnotationIndex.getInstance().get("DubboService", module.getProject(), GlobalSearchScope.moduleScope(module)));

        List<PsiClass> psiClasses = new LinkedList<>();

        for (PsiAnnotation psiAnnotation : psiAnnotations) {
            PsiModifierList psiModifierList = (PsiModifierList) psiAnnotation.getParent();
            PsiElement psiElement = psiModifierList.getParent();

            if (psiElement instanceof PsiClass && isDubboClass((PsiClass) psiElement)) {
                psiClasses.add((PsiClass) psiElement);
            }
        }
        return psiClasses;
    }

    @NotNull
    public static Body buildBody(@NotNull PsiMethod psiMethod) {

        Body root = new Body();
        root.setQualifiedNameForClassType(Objects.requireNonNull(psiMethod.getContainingClass()).getQualifiedName());

        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();

        for (PsiParameter parameter : parameters) {

            PsiType type = parameter.getType();

            // 集合
            Body body = new Body();
            body.setRequired(false);
            body.setName(parameter.getName());
            body.setType(type.getPresentableText());
            body.setParent(root);

            root.getChildList().add(body);

            PsiClass childClass = null;

            // 基本类型
            if (type instanceof PsiPrimitiveType || FieldTypeConstant.FIELD_TYPE.containsKey(type.getPresentableText())) {

            } else if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_COLLECTION)) {
                PsiType iterableType = PsiUtil.extractIterableTypeParameter(type, false);
                childClass = PsiUtil.resolveClassInClassTypeOnly(iterableType);

                body.setPsiElement(childClass);

            } else if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_MAP)) {
                //  map
                PsiType matValueType = PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 1, false);
                childClass = PsiUtil.resolveClassInClassTypeOnly(matValueType);

                body.setPsiElement(childClass);

            } else if (type instanceof PsiClassType) {
                // 对象
                childClass = PsiUtil.resolveClassInClassTypeOnly(type);

                body.setPsiElement(childClass);

            } else {
                // 未知类型
            }

            if (childClass != null) {

                // 判断 childClass 是否已经在根节点到当前节点的链表上存在, 存在的话则不继续递归

                String qualifiedName = childClass.getQualifiedName();

                if (StringUtils.isNotBlank(qualifiedName) && !ParamPsiUtils.checkLinkedListHasTypeClass(body, qualifiedName)) {
                    body.setQualifiedNameForClassType(qualifiedName);
                    for (PsiField psiField : childClass.getAllFields()) {
                        if (!DocViewUtils.isExcludeField(psiField)) {
                            ParamPsiUtils.buildBodyParam(psiField, null, body);
                        }
                    }
                }
            }
        }

        return root;
    }

    @NotNull
    public static String getReqBodyJson(@NotNull PsiMethod psiMethod) {

        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();

        Map<String, Object> fieldMap = new LinkedHashMap<>();

        for (PsiParameter parameter : parameters) {

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

        return "{}";
    }
}
