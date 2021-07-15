package com.liuzhihang.doc.view.dto;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import lombok.Data;

import java.util.List;

/**
 * 文档参数
 *
 * @author liuzhihang
 * @date 2020/2/28 10:32
 */
@Data
public class DocView {

    /**
     * 当前接口所在的类
     */
    private PsiClass psiClass;

    /**
     * 当前接口的方法
     */
    private PsiMethod psiMethod;

    /**
     * 文档标题, 方法所属的类
     */
    private String docTitle;

    /**
     * 接口方法所属类的标签
     * <p>
     * swagger @Api(tags = {"用户接口相关", "用户11"})
     * swagger3 @Tags
     */
    private String[] classTags;

    /**
     * 接口方法的标签
     * <p>
     * swagger @ApiOperation(value = "查询用户", tags = "用户接口 xxx")
     * swagger3 @Operation(summary = "用户接口 1", tags = "测试")
     */
    private String[] tags;

    /**
     * 文档名称
     */
    private String name;

    /**
     * 文档描述
     */
    private String desc;

    /**
     * 环境地址
     */
    private List<String> domain;

    /**
     * 接口地址
     */
    private String path;

    /**
     * 请求方式 GET POST PUT DELETE HEAD OPTIONS PATCH
     */
    private String method;

    /**
     * 变动说明
     */
    private String changeLog;


    /**
     * headers
     */
    private List<Header> headerList;


    /**
     * 请求参数
     */
    private Body reqRootBody;

    /**
     * 返回参数
     */
    private Body respRootBody;


    /**
     * 请求参数
     */
    private List<Param> reqParamList;

    /**
     * 参数时Json时举例
     */
    private String reqExample;

    /**
     * reqExampleType 类型
     */
    private String reqExampleType;

    /**
     * 返回参数
     */
    private String respExample;

    /**
     * 备注
     */
    private String remark;

    private String type;

    public DocView(String name) {
        this.name = name;
    }

    public DocView() {
    }


}
