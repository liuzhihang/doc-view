package com.liuzhihang.doc.view.service.impl;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.Header;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import com.liuzhihang.doc.view.utils.ParamPsiUtils;
import com.liuzhihang.doc.view.utils.SpringHeaderUtils;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author liuzhihang
 * @date 2020/3/3 13:32
 */
public class SpringDocViewServiceImpl implements DocViewService {

    @Override
    public boolean checkMethod(@NotNull Project project, @NotNull PsiMethod targetMethod) {
        return SpringPsiUtils.isSpringMethod(targetMethod);
    }

    @NotNull
    @Override
    public List<DocView> buildClassDoc(@NotNull Project project, @NotNull PsiClass psiClass) {

        List<DocView> docViewList = new LinkedList<>();

        for (PsiMethod method : psiClass.getMethods()) {

            if (!SpringPsiUtils.isSpringMethod(method)) {
                continue;
            }

            DocView docView = buildClassMethodDoc(project, psiClass, method);
            docViewList.add(docView);
        }

        return docViewList;
    }

    @NotNull
    @Override
    public DocView buildClassMethodDoc(@NotNull Project project, PsiClass psiClass, @NotNull PsiMethod psiMethod) {

        Settings settings = Settings.getInstance(project);

        DocView docView = new DocView();
        docView.setPsiClass(psiClass);
        docView.setPsiMethod(psiMethod);
        docView.setDocTitle(DocViewUtils.getTitle(psiClass));
        docView.setName(DocViewUtils.getName(psiMethod));
        docView.setDesc(DocViewUtils.getMethodDesc(psiMethod));
        docView.setPath(SpringPsiUtils.getPath(psiClass, psiMethod));
        docView.setMethod(SpringPsiUtils.getMethod(psiMethod));
        // docView.setDomain();
        docView.setType("Spring");

        List<Header> headerList = new ArrayList<>();

        // 有参数
        if (psiMethod.hasParameters()) {

            // 获取
            PsiParameter requestBodyParam = SpringPsiUtils.getRequestBodyParam(psiMethod);

            if (requestBodyParam != null) {
                // 有requestBody
                headerList.add(SpringHeaderUtils.buildJsonHeader());
                docView.setReqBodyList(SpringPsiUtils.buildBody(requestBodyParam));
                docView.setReqExample(SpringPsiUtils.getReqBodyJson(requestBodyParam, settings));
                docView.setReqExampleType("json");

            } else {
                headerList.add(SpringHeaderUtils.buildFormHeader());
                docView.setReqParamList(SpringPsiUtils.buildFormParam(psiMethod));
                docView.setReqExample(SpringPsiUtils.getReqParamKV(docView.getReqParamList()));
                docView.setReqExampleType("form");

            }

            // 处理 header
            List<Header> headers = SpringPsiUtils.buildHeader(psiMethod);
            headerList.addAll(headers);
        } else {
            docView.setReqExampleType("form");
            headerList.add(SpringHeaderUtils.buildFormHeader());
        }
        docView.setHeaderList(headerList);

        PsiType returnType = psiMethod.getReturnType();
        if (returnType != null && returnType.isValid() && !returnType.equalsToText("void")) {
            docView.setRespBodyList(ParamPsiUtils.buildRespBody(returnType));
            docView.setRespExample(ParamPsiUtils.getRespBodyJson(returnType));
        }
        return docView;
    }


}
