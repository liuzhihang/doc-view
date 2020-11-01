package com.liuzhihang.doc.view.service;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.*;
import com.liuzhihang.doc.view.component.Settings;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.Header;
import com.liuzhihang.doc.view.dto.Param;
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
public class DocViewService {


    public static DocViewService getInstance() {
        return ServiceManager.getService(DocViewService.class);
    }

    public Map<String, DocView> buildDocView(@NotNull Settings settings, @NotNull PsiClass psiClass, PsiMethod psiMethod) {


        if (psiClass.isInterface()) {
            // 接口按照dubbo接口生成

        } else {

            if (psiMethod != null) {
                // 单个方法
                DocView docView = buildClassMethodDoc(settings, psiClass, psiMethod);
                Map<String, DocView> docMap = new HashMap<>(2);
                if (docView != null) {
                    docMap.put(docView.getName(), docView);
                }
                return docMap;
            } else {
                // 单个类 的所有方法
                return buildClassDoc(settings, psiClass);
            }
        }
        return null;
    }

    @NotNull
    private Map<String, DocView> buildClassDoc(Settings settings, @NotNull PsiClass psiClass) {

        Map<String, DocView> docMap = new HashMap<>(32);

        for (PsiMethod method : psiClass.getMethods()) {
            DocView docView = buildClassMethodDoc(settings, psiClass, method);
            if (docView != null) {
                docMap.put(docView.getName(), docView);
            }
        }

        return docMap;
    }

    private DocView buildClassMethodDoc(Settings settings, PsiClass psiClass, @NotNull PsiMethod psiMethod) {

        if (!CustomPsiModifierUtils.hasModifierProperty(psiMethod, PsiModifier.PUBLIC)) {
            return null;
        }
        if (CustomPsiModifierUtils.hasModifierProperty(psiMethod, PsiModifier.STATIC)) {
            return null;
        }
        if (psiMethod.isConstructor()) {
            return null;
        }
        if (!AnnotationUtil.isAnnotated(psiMethod, settings.getContainMethodAnnotationName(), 0)) {
            return null;
        }
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

}
