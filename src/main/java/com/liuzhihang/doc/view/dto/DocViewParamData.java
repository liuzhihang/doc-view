package com.liuzhihang.doc.view.dto;

import com.intellij.psi.PsiElement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数 data 数据
 *
 * @author liuzhihang
 * @date 2020/2/27 16:39
 */
@Data
public class DocViewParamData {

    /**
     * 参数的 psiElement
     */
    private PsiElement psiElement;

    /**
     * 前缀 1
     */
    private String prefixSymbol1 = "";

    /**
     * 前缀 2
     */
    private String prefixSymbol2 = "";

    /**
     * 参数名
     */
    private String name;

    /**
     * 类型
     */
    private String type;

    /**
     * 是否必须
     */
    private Boolean required;

    /**
     * 参数示例
     */
    private String example;

    /**
     * 参数描述
     */
    private String desc = "";

    /**
     * 子
     */
    private List<DocViewParamData> childList = new ArrayList<>();

}
