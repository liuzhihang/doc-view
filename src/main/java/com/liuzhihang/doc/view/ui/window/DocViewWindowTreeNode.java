package com.liuzhihang.doc.view.ui.window;

import com.intellij.psi.PsiElement;

/**
 * @author liuzhihang
 * @date 2021/10/23 17:05
 */
public class DocViewWindowTreeNode {

    /**
     * 接口类型
     * Spring Dubbo Feign
     * <p>
     * POST GET PUT DELETE
     */
    private String type;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 可能是 psiClass 或 psiMethod
     */
    private PsiElement psiElement;
}
