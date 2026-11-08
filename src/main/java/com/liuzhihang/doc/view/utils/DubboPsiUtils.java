package com.liuzhihang.doc.view.utils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.NonBlockingReadAction;
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.liuzhihang.doc.view.constant.DubboConstant;
import com.liuzhihang.doc.view.constant.FieldTypeConstant;
import com.liuzhihang.doc.view.dom.DubboXmlPsi;
import com.liuzhihang.doc.view.dto.Body;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

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

        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) {
            return false;
        }

        boolean[] declared = {false};
        DubboXmlPsi.processServiceTags(project, GlobalSearchScope.allScope(project), serviceTag -> {
            if (qualifiedName.equals(DubboXmlPsi.getInterfaceName(serviceTag))) {
                declared[0] = true;
                return false;
            }
            return true;
        });
        return declared[0];
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
        ProgressManager.checkCanceled();
        if (module.isDisposed()) {
            return List.of();
        }

        Project project = module.getProject();
        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
        Set<PsiClass> interfaces = new LinkedHashSet<>();

        // Implementations can live in a provider module while their API interfaces live in
        // another module. Query the project annotation index, then attribute each interface
        // to its source module below. Short-name hits are only candidates: exact FQNs prevent
        // Spring or user annotations named Service/DubboService from becoming Dubbo APIs.
        ProgressManager.checkCanceled();
        Collection<PsiAnnotation> serviceAnnotations = new ArrayList<>(
                JavaAnnotationIndex.getInstance().getAnnotations("Service", project, projectScope));
        serviceAnnotations.addAll(JavaAnnotationIndex.getInstance()
                .getAnnotations("DubboService", project, projectScope));
        ProgressManager.checkCanceled();
        for (PsiAnnotation annotation : serviceAnnotations) {
            ProgressManager.checkCanceled();
            if (!DubboConstant.SERVICE_ANNOTATIONS.contains(annotation.getQualifiedName())) {
                continue;
            }
            PsiElement parent = annotation.getParent();
            if (!(parent instanceof PsiModifierList modifierList)
                    || !(modifierList.getParent() instanceof PsiClass serviceClass)) {
                continue;
            }
            if (serviceClass.isInterface()) {
                addIfOwnedInterface(serviceClass, module, interfaces);
            } else {
                addImplementedInterfaces(serviceClass, module, interfaces);
            }
        }

        // XML declarations are project-discovered because the configuration file may live
        // in a provider module. Resolve each FQN using only that declaring module's
        // dependencies, then add it to the catalog belonging to the interface source module.
        DubboXmlPsi.processServiceTags(project, projectScope, serviceTag -> {
            Module declarationModule = ModuleUtilCore.findModuleForPsiElement(serviceTag);
            if (declarationModule == null || declarationModule.isDisposed()) {
                return true;
            }
            GlobalSearchScope declarationScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(declarationModule);
            String qualifiedName = DubboXmlPsi.getInterfaceName(serviceTag);
            if (qualifiedName == null || qualifiedName.isBlank()) {
                return true;
            }
            PsiClass serviceInterface = JavaPsiFacade.getInstance(project)
                    .findClass(qualifiedName, declarationScope);
            if (serviceInterface != null) {
                addIfOwnedInterface(serviceInterface, module, interfaces);
            }
            return true;
        });

        return interfaces.stream()
                .sorted(Comparator.comparing(PsiClass::getQualifiedName, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private static void addImplementedInterfaces(PsiClass serviceClass, Module module, Set<PsiClass> output) {
        Set<PsiClass> visited = new HashSet<>();
        Deque<PsiClass> pending = new ArrayDeque<>();
        pending.add(serviceClass);
        while (!pending.isEmpty()) {
            ProgressManager.checkCanceled();
            PsiClass current = pending.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            for (PsiClass superClass : current.getSupers()) {
                ProgressManager.checkCanceled();
                if (superClass.isInterface()) {
                    addIfOwnedInterface(superClass, module, output);
                }
                pending.addLast(superClass);
            }
        }
    }

    private static void addIfOwnedInterface(PsiClass psiClass, Module module, Set<PsiClass> output) {
        ProgressManager.checkCanceled();
        if (psiClass.isValid() && psiClass.isInterface()
                && ModuleUtilCore.findModuleForPsiElement(psiClass) == module) {
            output.add(psiClass);
        }
    }

    @NotNull
    public static Body buildBody(@NotNull PsiMethod psiMethod) {
        ProgressManager.checkCanceled();

        Body root = new Body();
        root.setQualifiedNameForClassType(Objects.requireNonNull(psiMethod.getContainingClass()).getQualifiedName());

        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();

        String since = CustomPsiCommentUtils.tagDocComment(psiMethod.getDocComment(), "since");
        String version = CustomPsiCommentUtils.tagDocComment(psiMethod.getDocComment(), "version");

        for (PsiParameter parameter : parameters) {
            ProgressManager.checkCanceled();

            PsiType type = parameter.getType();

            // 集合
            Body body = new Body();
            body.setRequired(false);
            body.setName(parameter.getName());
            body.setType(type.getPresentableText());
            body.setParent(root);
            // 获取@param 上的注解描述
            body.setDesc(CustomPsiCommentUtils.paramDocComment(psiMethod.getDocComment(), parameter));

            body.setSince(since);
            body.setVersion(version);

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
                        ProgressManager.checkCanceled();
                        if (!DocViewUtils.isExcludeField(psiField)) {
                            ParamPsiUtils.buildBodyParam(psiField, null, body, new HashMap<>());
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
            ProgressManager.checkCanceled();

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
