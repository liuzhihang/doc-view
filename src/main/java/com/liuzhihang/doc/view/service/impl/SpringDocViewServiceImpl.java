package com.liuzhihang.doc.view.service.impl;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.config.TagsSettings;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.Header;
import com.liuzhihang.doc.view.dto.Param;
import com.liuzhihang.doc.view.service.DocViewService;
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
public class SpringDocViewServiceImpl implements DocViewService {


    @Override
    public void doPreview(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull Editor editor, @NotNull PsiClass targetClass) {

        // 当前方法
        PsiMethod targetMethod = CustomPsiUtils.getTargetMethod(editor, psiFile);

        Map<String, DocView> docMap = new HashMap<>();

        if (targetMethod != null) {

            if (!SpringPsiUtils.isSpringMethod(project, targetMethod)) {
                NotificationUtils.errorNotify(DocViewBundle.message("notify.spring.error.method"), project);
                return;
            }

            DocView docView = buildClassMethodDoc(project, targetClass, targetMethod);
            docMap.put(docView.getName(), docView);

        } else {
            // 生成文档列表
            docMap = buildClassDoc(project, targetClass);
            if (docMap.size() == 0) {
                NotificationUtils.errorNotify(DocViewBundle.message("notify.spring.error.no.method"), project);
                return;
            }
        }
        PreviewForm.getInstance(project, psiFile, editor, targetClass, docMap).popup();
    }

    @NotNull
    @Override
    public Map<String, DocView> buildClassDoc(@NotNull Project project, @NotNull PsiClass psiClass) {

        Map<String, DocView> docMap = new HashMap<>(32);

        for (PsiMethod method : psiClass.getMethods()) {

            if (!SpringPsiUtils.isSpringMethod(project, method)) {
                continue;
            }

            DocView docView = buildClassMethodDoc(project, psiClass, method);
            docMap.put(docView.getName(), docView);
        }

        return docMap;
    }

    @NotNull
    @Override
    public DocView buildClassMethodDoc(@NotNull Project project, PsiClass psiClass, @NotNull PsiMethod psiMethod) {

        Settings settings = Settings.getInstance(project);
        TagsSettings tagsSettings = TagsSettings.getInstance(project);


        // 请求路径
        String path = SpringPsiUtils.getPath(psiClass, psiMethod);

        // 请求方式
        String method = SpringPsiUtils.getMethod(psiMethod);


        // 文档注释
        String desc = CustomPsiCommentUtils.getComment(psiMethod.getDocComment());


        String name = CustomPsiCommentUtils.getComment(psiMethod.getDocComment(), tagsSettings.getName());


        DocView docView = new DocView();
        docView.setPsiMethod(psiMethod);
        docView.setFullClassName(psiClass.getQualifiedName());
        docView.setClassName(psiClass.getName());
        docView.setName(StringUtils.isBlank(name) ? psiMethod.getName() : name);
        docView.setDesc(desc);
        docView.setPath(path);
        docView.setMethod(method);
        // docView.setDomain();
        docView.setType("Spring");

        List<Header> headerList = new ArrayList<>();

        // 有参数
        if (psiMethod.hasParameters()) {

            // 获取
            PsiParameter requestBodyParam = SpringPsiUtils.getRequestBodyParam(psiMethod);

            if (requestBodyParam != null) {
                // 有requestBody
                Header jsonHeader = SpringHeaderUtils.buildJsonHeader();
                headerList.add(jsonHeader);

                List<Body> reqBody = SpringPsiUtils.buildBody(settings, requestBodyParam);
                docView.setReqBodyList(reqBody);

                String bodyJson = SpringPsiUtils.getReqBodyJson(settings, requestBodyParam);
                docView.setReqExample(bodyJson);
                docView.setReqExampleType("json");

            } else {
                Header formHeader = SpringHeaderUtils.buildFormHeader();
                headerList.add(formHeader);
                List<Param> requestParam = SpringPsiUtils.buildFormParam(settings, psiMethod);
                docView.setReqParamList(requestParam);

                String paramKV = SpringPsiUtils.getReqParamKV(requestParam);
                docView.setReqExample(paramKV);
                docView.setReqExampleType("form");

            }

            // 处理 header
            List<Header> headers = SpringPsiUtils.buildHeader(settings, psiMethod);
            headerList.addAll(headers);
        } else {
            docView.setReqExampleType("form");
            Header formHeader = SpringHeaderUtils.buildFormHeader();
            headerList.add(formHeader);
        }
        docView.setHeaderList(headerList);


        PsiType returnType = psiMethod.getReturnType();
        if (returnType != null && returnType.isValid() && !returnType.equalsToText("void")) {
            List<Body> respParamList = ParamPsiUtils.buildRespBody(settings, returnType);
            docView.setRespBodyList(respParamList);

            String bodyJson = ParamPsiUtils.getRespBodyJson(settings, returnType);

            docView.setRespExample(bodyJson);
        }
        return docView;
    }


}
