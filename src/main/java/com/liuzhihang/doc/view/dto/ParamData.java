package com.liuzhihang.doc.view.dto;

import com.intellij.psi.PsiElement;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * 参数 data 数据
 *
 * @author liuzhihang
 * @date 2020/2/27 16:39
 */
@Data
public class ParamData {

    /**
     * 参数的 psiElement
     */
    private PsiElement psiElement;

    /**
     * 前缀
     */
    private String prefix = "";

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
     * |参数名|参数值|必填|描述|
     *
     * @param header
     * @return
     */
    @NotNull
    public static ParamData convertFromHeader(@NotNull Header header) {

        ParamData paramData = new ParamData();
        paramData.setPsiElement(header.getPsiElement());
        paramData.setName(header.getName());
        paramData.setExample(header.getValue());
        paramData.setRequired(header.getRequired());
        paramData.setDesc(StringUtils.isNotBlank(header.getDesc()) ? header.getDesc() : "");

        return paramData;

    }

    @NotNull
    public static ParamData convertFromParam(@NotNull Param param) {

        ParamData paramData = new ParamData();
        paramData.setPsiElement(param.getPsiElement());
        paramData.setName(param.getName());
        paramData.setExample(param.getExample());
        paramData.setRequired(param.getRequired());
        paramData.setType(param.getType());
        paramData.setDesc(StringUtils.isNotBlank(param.getDesc()) ? param.getDesc() : "");

        return paramData;

    }

    @NotNull
    public static ParamData convertFromBody(@NotNull Body body) {

        ParamData paramData = new ParamData();
        paramData.setPsiElement(body.getPsiElement());
        paramData.setName(body.getName());
        paramData.setExample(body.getExample());
        paramData.setRequired(body.getRequired());
        paramData.setType(body.getType());
        paramData.setDesc(StringUtils.isNotBlank(body.getDesc()) ? body.getDesc() : "");

        return paramData;

    }


}
