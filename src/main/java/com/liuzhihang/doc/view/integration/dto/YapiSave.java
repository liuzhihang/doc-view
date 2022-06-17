package com.liuzhihang.doc.view.integration.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 新增或上传对象
 * <p>
 * https://hellosean1025.github.io/yapi/openapi.html
 *
 * @author liuzhihang
 * @date 2021/6/8 20:11
 */
@Data
public class YapiSave implements Serializable {

    /**
     * 项目信息
     */
    private String yapiUrl;

    /**
     * 项目 token
     */
    private String token;

    /**
     * 项目 id
     */
    private Long projectId;

    /**
     * 接口 id
     */
    private String id;

    /**
     * 品类id
     */
    @SerializedName("catid")
    private Long catId;

    /**
     * 请求路径
     */
    private String path;
    /**
     * 请求方式
     */
    private String method;

    /**
     * 请求数据类型
     * 枚举: raw,form,json
     */
    @SerializedName("req_body_type")
    private String reqBodyType;

    /**
     * 请求数据body
     */
    @SerializedName("req_body_other")
    private String reqBodyOther;
    /**
     * 请求参数body 是否为json_schema
     */
    @SerializedName("req_body_is_json_schema")
    private boolean reqBodyIsJsonSchema;


    /**
     * 请求参数 form 类型
     */
    @SerializedName("req_body_form")
    private List<YApiBodyForm> reqBodyForm;

    /**
     * 请求参数
     */
    @SerializedName("req_params")
    private List<YApiParam> reqParams;

    /**
     * 请求参数
     */
    @SerializedName("req_headers")
    private List<YApiHeader> reqHeaders;

    /**
     * 请求参数
     */
    @SerializedName("req_query")
    private List<YApiQuery> reqQuery;

    /**
     * 返回参数类型  json
     * 枚举: raw,json
     */
    @SerializedName("res_body_type")
    private String resBodyType = "json";

    /**
     * 返回参数
     */
    @SerializedName("res_body")
    private String resBody;


    /**
     * 文档描述
     */
    private String desc = "";

    /**
     * 标题
     */
    private String title;
    /**
     * 邮件开关
     */
    @SerializedName("switch_notice")
    private Boolean switchNotice = false;
    /**
     * 状态 undone,默认done
     */
    private String status = "undone";


    /**
     * 返回参数是否为json_schema
     */
    @SerializedName("res_body_is_json_schema")
    private boolean resBodyIsJsonSchema = true;

    /**
     * 备注信息
     */
    private String markdown;


}
