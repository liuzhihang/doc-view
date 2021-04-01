package com.liuzhihang.doc.view.dto;

import com.intellij.psi.PsiElement;

/**
 * @author liuzhihang
 * @date 2020/2/28 10:40
 */
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

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public PsiElement getPsiElement() {
        return psiElement;
    }

    public void setPsiElement(PsiElement psiElement) {
        this.psiElement = psiElement;
    }
}
