package com.liuzhihang.doc.view.dto;

import com.intellij.psi.PsiElement;
import lombok.Data;

/**
 * @author liuzhihang
 * @date 2020/2/28 10:40
 */
@Data
public class Header {

    /**
     * 节点
     */
    private PsiElement psiElement;

    /**
     * 是否必须 1必须 0非必须
     */
    private Boolean required;
    /**
     * 参数名
     */
    private String name;

    /**
     * 值
     */
    private String value;

    /**
     * 参数备注
     */
    private String desc;


}
