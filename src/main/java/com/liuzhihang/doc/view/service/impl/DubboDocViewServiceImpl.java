package com.liuzhihang.doc.view.service.impl;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.enums.ContentTypeEnum;
import com.liuzhihang.doc.view.enums.FrameworkEnum;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import com.liuzhihang.doc.view.utils.DubboPsiUtils;
import com.liuzhihang.doc.view.utils.ParamPsiUtils;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * @author liuzhihang
 * @date 2020/11/16 18:28
 */
public class DubboDocViewServiceImpl implements DocViewService {


    @Override
    public boolean checkMethod(@NotNull Project project, @NotNull PsiMethod targetMethod) {
        return DubboPsiUtils.isDubboMethod(targetMethod);
    }

    @Override
    public List<DocView> buildClassDoc(@NotNull Project project, @NotNull PsiClass psiClass) {

        List<DocView> docViewList = new LinkedList<>();

        for (PsiMethod method : psiClass.getMethods()) {

            if (!DubboPsiUtils.isDubboMethod(method)) {
                continue;
            }

            DocView docView = buildClassMethodDoc(project, psiClass, method);
            docViewList.add(docView);
        }

        return docViewList;

    }

    @NotNull
    @Override
    public DocView buildClassMethodDoc(@NotNull Project project, @NotNull PsiClass psiClass, @NotNull PsiMethod psiMethod) {

        Settings settings = Settings.getInstance(project);

        DocView docView = new DocView();
        docView.setPsiClass(psiClass);
        docView.setPsiMethod(psiMethod);
        docView.setDocTitle(DocViewUtils.getTitle(psiClass));
        docView.setName(DocViewUtils.getName(psiMethod));
        docView.setDesc(DocViewUtils.getMethodDesc(psiMethod));
        docView.setPath(psiClass.getName() + "#" + psiMethod.getName());
        docView.setMethod("Dubbo");
        // docView.setDomain();
        docView.setType(FrameworkEnum.DUBBO);

        // 有参数
        if (psiMethod.hasParameters()) {
            docView.setReqBody(DubboPsiUtils.buildBody(psiMethod));
            docView.setContentType(ContentTypeEnum.JSON);
            docView.setReqBodyExample(DubboPsiUtils.getReqBodyJson(psiMethod));
        }

        PsiType returnType = psiMethod.getReturnType();
        // 返回代码相同
        if (returnType != null && returnType.isValid() && !returnType.equalsToText("void")) {
            docView.setRespBody(ParamPsiUtils.buildRespBody(returnType));
            docView.setRespExample(ParamPsiUtils.getRespBodyJson(returnType));
        }
        return docView;

    }
}
