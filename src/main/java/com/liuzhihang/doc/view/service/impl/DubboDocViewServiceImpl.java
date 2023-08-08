package com.liuzhihang.doc.view.service.impl;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
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
 * Dubbo 处理服务
 *
 * @author liuzhihang
 * @date 2020/11/16 18:28
 */
public class DubboDocViewServiceImpl implements DocViewService {


    /**
     * 校验方法是否为符合条件
     *
     * @param targetMethod 当前方法
     * @return 是否
     */
    @Override
    public boolean checkMethod(@NotNull PsiMethod targetMethod) {
        return DubboPsiUtils.isDubboMethod(targetMethod);
    }

    /**
     * 创建类的文档
     *
     * @param psiClass 当前类
     * @return 类的所有接口文档
     */
    @Override
    public List<DocView> buildClassDoc(@NotNull PsiClass psiClass) {

        List<DocView> docViewList = new LinkedList<>();

        for (PsiMethod method : psiClass.getMethods()) {

            if (!DubboPsiUtils.isDubboMethod(method)) {
                continue;
            }

            DocView docView = buildClassMethodDoc(psiClass, method);
            docViewList.add(docView);
        }

        return docViewList;

    }

    /**
     * 构造当前类方法的文档
     *
     * @param psiClass  当前类
     * @param psiMethod 当前方法
     * @return
     */
    @NotNull
    @Override
    public DocView buildClassMethodDoc(@NotNull PsiClass psiClass, @NotNull PsiMethod psiMethod) {

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
