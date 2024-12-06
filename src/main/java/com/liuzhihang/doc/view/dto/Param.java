package com.liuzhihang.doc.view.dto;

import com.intellij.psi.PsiElement;
import lombok.Data;

/**
 * @author liuzhihang
 * @date 2020/2/27 16:39
 */
@Data
public class Param {


    /**
     * 参数的 psiElement
     */
    private PsiElement psiElement;

    /**
     * 是否必须
     */
    private Boolean required;
    /**
     * 参数名
     */
    private String name;
    /**
     * 参数示例
     */
    private String example;

    /**
     * 参数描述
     */
    private String desc;

    /**
     * 类型
     */
    private String type;


    /**
     * since
     */
    private String since;

    /**
     * version
     */
    private String version;
}
