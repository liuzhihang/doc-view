package com.liuzhihang.doc.view.service.impl;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.*;
import com.liuzhihang.doc.view.component.Settings;
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
    public void doPreview(@NotNull Project project, PsiFile psiFile, Editor editor, PsiClass targetClass) {

        Settings settings = project.getService(Settings.class);

        // 当前方法
        PsiMethod targetMethod = CustomPsiUtils.getTargetMethod(editor, psiFile);

        Map<String, DocView> docMap = new HashMap<>();

        if (targetMethod != null) {

            if (!SpringPsiUtils.isSpringMethod(settings, targetMethod)) {
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
    @Override
    public Map<String, DocView> buildClassDoc(Settings settings, @NotNull PsiClass psiClass) {

        Map<String, DocView> docMap = new HashMap<>(32);

        for (PsiMethod method : psiClass.getMethods()) {

            if (!SpringPsiUtils.isSpringMethod(settings, method)) {
                continue;
            }

            DocView docView = buildClassMethodDoc(settings, psiClass, method);
            docMap.put(docView.getName(), docView);
        }

        return docMap;
    }

    @NotNull
    @Override
    public DocView buildClassMethodDoc(Settings settings, PsiClass psiClass, @NotNull PsiMethod psiMethod) {

        // 请求路径
        String path = SpringPsiUtils.getPath(psiClass, psiMethod);

        // 请求方式
        String method = SpringPsiUtils.getMethod(psiMethod);

        // 文档注释
        String desc = CustomPsiCommentUtils.getComment(psiMethod.getDocComment(), "description");

        String name = CustomPsiCommentUtils.getComment(psiMethod.getDocComment(), "name", false);

        if (StringUtils.isBlank(name)) {
            name = psiClass.getName() + "#" + psiMethod.getName();
        }

        DocView docView = new DocView();
        docView.setName(name);
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
