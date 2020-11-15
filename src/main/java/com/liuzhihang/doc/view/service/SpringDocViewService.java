package com.liuzhihang.doc.view.service;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.*;
import com.liuzhihang.doc.view.component.Settings;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.Header;
import com.liuzhihang.doc.view.dto.Param;
import com.liuzhihang.doc.view.ui.PreviewForm;
import com.liuzhihang.doc.view.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author liuzhihang
 * @date 2020/3/3 13:32
 */
public class SpringDocViewService {


    public static SpringDocViewService getInstance() {
        return ServiceManager.getService(SpringDocViewService.class);
    }


    public void doPreview(@NotNull Project project, PsiFile psiFile, Editor editor, PsiClass targetClass) {

        Settings settings = project.getService(Settings.class);

        // 当前方法
        PsiMethod targetMethod = CustomPsiUtils.getTargetMethod(editor, psiFile);

        Map<String, DocView> docMap = new HashMap<>();

        if (targetMethod != null) {

            if (checkMethod(settings, targetMethod)) {
                NotificationUtils.errorNotify("The method does not meet the conditions", project);
                return;
            }

            DocView docView = buildClassMethodDoc(settings, targetClass, targetMethod);
            docMap.put(docView.getName(), docView);

        } else {
            // 生成文档列表
            docMap = buildClassDoc(settings, targetClass);
            if (docMap.size() == 0) {
                NotificationUtils.errorNotify("There is no public modification method in the class", project);
                return;
            }
        }

        DialogWrapper dialog = new PreviewForm(project, psiFile, editor, targetClass, docMap);
        dialog.show();

    }

    @NotNull
    private Map<String, DocView> buildClassDoc(Settings settings, @NotNull PsiClass psiClass) {

        Map<String, DocView> docMap = new HashMap<>(32);

        for (PsiMethod method : psiClass.getMethods()) {

            if (checkMethod(settings, method)) {
                continue;
            }

            DocView docView = buildClassMethodDoc(settings, psiClass, method);
            docMap.put(docView.getName(), docView);
        }

        return docMap;
    }

    @NotNull
    public DocView buildClassMethodDoc(Settings settings, PsiClass psiClass, @NotNull PsiMethod psiMethod) {

        // 请求路径
        String path = CustomPsiAnnotationUtils.getPath(psiClass, psiMethod);

        // 请求方式
        String method = CustomPsiAnnotationUtils.getMethod(psiMethod);

        // 文档注释
        String desc = CustomPsiDocCommentUtils.getComment(psiMethod.getDocComment());

        String name = CustomPsiDocCommentUtils.getName(psiMethod.getDocComment());

        if (StringUtils.isBlank(name)) {
            name = psiClass.getName() + "#" + psiMethod.getName();
        }

        DocView docView = new DocView();
        docView.setName(name);
        docView.setDesc(desc);
        docView.setPath(path);
        docView.setMethod(method);
        // docView.setDomain();

        List<Header> headerList = new ArrayList<>();

        // 有参数
        if (psiMethod.hasParameters()) {

            // 获取
            PsiParameter requestBodyParam = CustomPsiParamUtils.getRequestBodyParam(psiMethod);

            if (requestBodyParam != null) {
                // 有requestBody
                Header jsonHeader = HeaderUtils.buildJsonHeader();
                headerList.add(jsonHeader);

                List<Body> reqBody = CustomPsiParamUtils.buildBody(settings, requestBodyParam);
                docView.setReqBodyList(reqBody);

                String bodyJson = CustomPsiParamUtils.getReqBodyJson(settings, requestBodyParam);
                docView.setReqExample(bodyJson);
                docView.setReqExampleType("json");

            } else {
                Header formHeader = HeaderUtils.buildFormHeader();
                headerList.add(formHeader);
                List<Param> requestParam = CustomPsiParamUtils.buildFormParam(settings, psiMethod);
                docView.setReqParamList(requestParam);

                String paramKV = CustomPsiParamUtils.getReqParamKV(requestParam);
                docView.setReqExample(paramKV);
                docView.setReqExampleType("form");

            }

            // 处理 header
            List<Header> headers = CustomPsiParamUtils.buildHeader(settings, psiMethod);
            headerList.addAll(headers);
        } else {
            docView.setReqExampleType("form");
            Header formHeader = HeaderUtils.buildFormHeader();
            headerList.add(formHeader);
        }
        docView.setHeaderList(headerList);


        PsiType returnType = psiMethod.getReturnType();
        if (returnType != null && returnType.isValid() && !returnType.equalsToText("void")) {
            List<Body> respParamList = CustomPsiParamUtils.buildRespBody(settings, returnType);
            docView.setRespBodyList(respParamList);

            String bodyJson = CustomPsiParamUtils.getRespBodyJson(settings, returnType);
            docView.setRespExample(bodyJson);
        }
        return docView;
    }

    /**
     * 检查方法是否满足条件
     *
     * @param settings
     * @param psiMethod
     * @return true 不满足条件
     */
    private boolean checkMethod(Settings settings, @NotNull PsiMethod psiMethod) {
        if (!CustomPsiModifierUtils.hasModifierProperty(psiMethod, PsiModifier.PUBLIC)) {
            return true;
        }
        if (CustomPsiModifierUtils.hasModifierProperty(psiMethod, PsiModifier.STATIC)) {
            return true;
        }
        if (psiMethod.isConstructor()) {
            return true;
        }
        if (!AnnotationUtil.isAnnotated(psiMethod, settings.getContainMethodAnnotationName(), 0)) {
            return true;
        }
        return false;
    }

}
