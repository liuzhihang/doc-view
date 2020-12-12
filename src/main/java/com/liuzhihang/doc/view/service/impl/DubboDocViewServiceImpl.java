package com.liuzhihang.doc.view.service.impl;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.ui.PreviewForm;
import com.liuzhihang.doc.view.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author liuzhihang
 * @date 2020/11/16 18:28
 */
public class DubboDocViewServiceImpl implements DocViewService {

    @Override
    public void doPreview(@NotNull Project project, PsiFile psiFile, Editor editor, PsiClass targetClass) {

        Settings settings = Settings.getInstance(project);

        // 当前方法
        PsiMethod targetMethod = CustomPsiUtils.getTargetMethod(editor, psiFile);

        Map<String, DocView> docMap = new HashMap<>();

        if (targetMethod != null) {

            if (!DubboPsiUtils.isDubboMethod(settings, targetMethod)) {
                NotificationUtils.errorNotify(DocViewBundle.message("notify.dubbo.error.method"), project);
                return;
            }

            DocView docView = buildClassMethodDoc(settings, targetClass, targetMethod);
            docMap.put(docView.getName(), docView);

        } else {
            // 生成文档列表
            docMap = buildClassDoc(settings, targetClass);
            if (docMap.size() == 0) {
                NotificationUtils.errorNotify(DocViewBundle.message("notify.dubbo.error.no.method"), project);
                return;
            }
        }

        PreviewForm.getInstance(project, psiFile, editor, targetClass, docMap).popup();

    }

    @Override
    public Map<String, DocView> buildClassDoc(Settings settings, @NotNull PsiClass psiClass) {

        Map<String, DocView> docMap = new HashMap<>(32);

        for (PsiMethod method : psiClass.getMethods()) {

            if (!DubboPsiUtils.isDubboMethod(settings, method)) {
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
        String path = psiClass.getName() + "#" + psiMethod.getName();

        // 请求方式
        String method = "Dubbo";

        // 文档注释
        String desc = CustomPsiCommentUtils.getComment(psiMethod.getDocComment(), "description");

        String name = CustomPsiCommentUtils.getComment(psiMethod.getDocComment(), "name", false);

        String methodFullName = psiClass.getName() + "#" + psiMethod.getName();

        DocView docView = new DocView();
        docView.setMethodFullName(methodFullName);
        docView.setName(StringUtils.isBlank(name) ? methodFullName : name);
        docView.setDesc(desc);
        docView.setPath(path);
        docView.setMethod(method);
        // docView.setDomain();
        docView.setType("Dubbo");


        // 有参数
        if (psiMethod.hasParameters()) {

            // 获取
            List<Body> reqBody = DubboPsiUtils.buildBody(settings, psiMethod);
            docView.setReqBodyList(reqBody);
            docView.setReqExampleType("json");

            String bodyJson = DubboPsiUtils.getReqBodyJson(settings, psiMethod);
            docView.setReqExample(bodyJson);

        }

        PsiType returnType = psiMethod.getReturnType();
        // 返回代码相同
        if (returnType != null && returnType.isValid() && !returnType.equalsToText("void")) {
            List<Body> respParamList = ParamPsiUtils.buildRespBody(settings, returnType);
            docView.setRespBodyList(respParamList);

            String bodyJson = ParamPsiUtils.getRespBodyJson(settings, returnType);
            docView.setRespExample(bodyJson);
        }
        return docView;

    }
}
