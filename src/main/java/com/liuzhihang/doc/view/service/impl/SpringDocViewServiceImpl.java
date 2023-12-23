package com.liuzhihang.doc.view.service.impl;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.enums.ContentTypeEnum;
import com.liuzhihang.doc.view.enums.FrameworkEnum;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import com.liuzhihang.doc.view.utils.ParamPsiUtils;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.intellij.psi.PsiKeyword.VOID;

/**
 * @author liuzhihang
 * @date 2020/3/3 13:32
 */
public class SpringDocViewServiceImpl implements DocViewService {

    @Override
    public boolean checkMethod(@NotNull PsiMethod targetMethod) {
        return SpringPsiUtils.isSpringMethod(targetMethod);
    }

    @NotNull
    @Override
    public List<DocView> buildClassDoc(@NotNull PsiClass psiClass) {

        List<DocView> docViewList = new LinkedList<>();

        for (PsiMethod method : psiClass.getMethods()) {

            if (!SpringPsiUtils.isSpringMethod(method)) {
                continue;
            }

            DocView docView = buildClassMethodDoc(psiClass, method);
            docViewList.add(docView);
        }

        return docViewList;
    }

    @NotNull
    @Override
    public DocView buildClassMethodDoc(PsiClass psiClass, @NotNull PsiMethod psiMethod) {

        DocView docView = new DocView();
        docView.setPsiClass(psiClass);
        docView.setPsiMethod(psiMethod);
        docView.setDocTitle(DocViewUtils.getTitle(psiClass));
        docView.setName(DocViewUtils.getName(psiMethod));
        docView.setDesc(DocViewUtils.getMethodDesc(psiMethod));
        docView.setPath(SpringPsiUtils.path(psiClass, psiMethod));
        docView.setMethod(SpringPsiUtils.method(psiMethod));
        docView.setDomain(Collections.emptyList());
        docView.setType(FrameworkEnum.SPRING);

        // 有参数
        if (psiMethod.hasParameters()) {

            ContentTypeEnum contentType = SpringPsiUtils.contentType(psiMethod);
            docView.setContentType(contentType);

            // 请求中的 form 参数, url 后面拼接的 kv
            docView.setReqParamList(SpringPsiUtils.buildFormParam(psiMethod));
            docView.setReqFormExample(SpringPsiUtils.reqParamKV(docView.getReqParamList()));

            if (contentType == ContentTypeEnum.JSON) {
                // JSON 请求可能会有 body
                PsiParameter requestBodyParam = SpringPsiUtils.requestBodyParam(psiMethod);
                if (requestBodyParam != null) {
                    docView.setReqBody(SpringPsiUtils.buildBody(requestBodyParam));
                    docView.setReqBodyExample(SpringPsiUtils.reqBodyJson(requestBodyParam));
                }
            }
        } else {
            docView.setContentType(ContentTypeEnum.FORM);
        }

        docView.setHeaderList(SpringPsiUtils.buildHeader(psiMethod));

        PsiType returnType = psiMethod.getReturnType();
        if (returnType != null && returnType.isValid() && !returnType.equalsToText(VOID)) {
            docView.setRespBody(ParamPsiUtils.buildRespBody(returnType));
            docView.setRespExample(ParamPsiUtils.getRespBodyJson(returnType));
        }
        return docView;
    }

}
