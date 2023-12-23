package com.liuzhihang.doc.view.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.exception.DocViewException;
import com.liuzhihang.doc.view.service.impl.DubboDocViewServiceImpl;
import com.liuzhihang.doc.view.service.impl.SpringDocViewServiceImpl;
import com.liuzhihang.doc.view.utils.DubboPsiUtils;
import com.liuzhihang.doc.view.utils.FeignPsiUtil;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DocView 服务类
 *
 * @author liuzhihang
 * @date 2020/3/3 13:32
 */
public interface DocViewService {


    /**
     * 获取 DocView 服务，有可能该处位置不允许使用
     *
     * @param project     当前 project
     * @param targetClass 当前类
     * @return DocView 服务
     */
    @NotNull
    static DocViewService getInstance(@NotNull Project project, @NotNull PsiClass targetClass) {
        Settings settings = Settings.getInstance(project);

        //spring cloud 的 feign client
        if (FeignPsiUtil.isFeignClass(targetClass)) {
            return ApplicationManager.getApplication().getService(SpringDocViewServiceImpl.class);
        }

        // Dubbo
        if (DubboPsiUtils.isDubboClass(targetClass)) {
            return ApplicationManager.getApplication().getService(DubboDocViewServiceImpl.class);
        }

        // Spring
        if (SpringPsiUtils.isSpringClass(targetClass)) {
            return ApplicationManager.getApplication().getService(SpringDocViewServiceImpl.class);
        }

        // 其他
        if (settings.getIncludeNormalInterface()) {
            return ApplicationManager.getApplication().getService(DubboDocViewServiceImpl.class);
        }

        throw new DocViewException(DocViewBundle.message("notify.error.not.support"));
    }

    /**
     * 构造文档对象
     *
     * @param targetClass  当前类
     * @param targetMethod
     * @return DocView 列表
     */
    @NotNull
    default List<DocView> buildDoc(@NotNull PsiClass targetClass, PsiMethod targetMethod) {

        if (targetMethod != null && checkMethod(targetMethod)) {
            DocView docView = buildClassMethodDoc(targetClass, targetMethod);

            List<DocView> docViewList = new LinkedList<>();
            docViewList.add(docView);

            return docViewList;
        }

        // 每个方法都要生成
        if (targetMethod == null) {
            // 生成文档列表
            List<DocView> docViews = buildClassDoc(targetClass);
            // 处理重复的名字, 如果名字重复，则在后缀加上随机数
            Set<String> nameSet = docViews.stream().map(DocView::getName).collect(Collectors.toSet());
            for (int i = 0; i < docViews.size(); i++) {
                DocView docView = docViews.get(i);
                String currentName = docView.getName();
                if (nameSet.contains(currentName)) {
                    nameSet.remove(currentName);
                } else {
                    docView.setName(docView.getName() + "_" + RandomStringUtils.randomAlphabetic(5) + i);
                }

            }
            return docViews;
        }

        throw new DocViewException(DocViewBundle.message("notify.error.not.support"));

    }

    boolean checkMethod(@NotNull PsiMethod targetMethod);

    /**
     * 构造类文档
     *
     * @param psiClass
     * @return
     */
    List<DocView> buildClassDoc(@NotNull PsiClass psiClass);

    /**
     * 构造方法文档
     *
     * @param psiClass  当前类
     * @param psiMethod 当前方法
     * @return 文档
     */
    @NotNull DocView buildClassMethodDoc(PsiClass psiClass, @NotNull PsiMethod psiMethod);

}
