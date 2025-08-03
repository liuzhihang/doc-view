package com.liuzhihang.doc.view.dto;


import com.intellij.psi.PsiElement;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

/**
 * @author liuzhihang
 * @date 2020/2/27 16:39
 */
@Getter
@Setter
public class Body {

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

    /**
     * 初始化时创建集合
     */
    private List<Body> childList = new LinkedList<>();

    private Body parent;

    /**
     * 当类型是类时, 全路径
     * 如果是 HashMap 则是内部泛型的类型
     */
    private String qualifiedNameForClassType;

    /**
     * 是否是集合
     */
    private boolean isCollection = false;

    /**
     * 是否是 map
     */
    private boolean isMap = false;

}
