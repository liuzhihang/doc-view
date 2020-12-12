package com.liuzhihang.doc.view.dto;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * DocView 的模版 用来使用 Velocity 生成内容
 * <p>
 * Velocity 会根据 get 方法 获取值, 不提供 set 方法
 *
 * @author liuzhihang
 * @date 2020/11/21 16:39
 */
public class DocViewData {

    /**
     * 文档名称
     */
    private final String name;

    /**
     * 文档描述
     */
    private final String desc;

    /**
     * 环境地址
     */
    // private final   String domain;

    /**
     * 接口地址
     */
    private final String path;

    /**
     * 请求方式 GET POST PUT DELETE HEAD OPTIONS PATCH
     */
    private final String method;


    /**
     * headers
     */
    private final String requestHeader;

    /**
     * 请求参数
     */
    private final String requestParam;


    /**
     * 请求参数
     */
    private final String requestBody;

    /**
     * 请求示例
     */
    private final String requestExample;

    /**
     * 返回参数
     */
    private final String responseParam;


    /**
     * 返回示例
     */
    private final String responseExample;


    private final String methodFullName;

    public DocViewData(DocView docView) {

        this.methodFullName = docView.getMethodFullName();
        this.name = docView.getName();
        this.desc = docView.getDesc();
        this.path = docView.getPath();
        this.method = docView.getMethod();
        this.requestHeader = buildReqHeaderParam(docView.getHeaderList());
        this.requestParam = buildReqParam(docView.getReqParamList());
        this.requestBody = buildReqBodyParam(docView.getReqBodyList());
        this.requestExample = buildReqExample(docView.getReqExampleType(), docView.getReqExample());
        this.responseParam = buildRespBodyParam(docView.getRespBodyList());
        this.responseExample = buildRespExample(docView.getReqExampleType(), docView.getRespExample());
    }

    private String buildRespExample(String respExampleType, String respExample) {
        return "```json\n" +
                (respExampleType == null ? "" : respExample) + "\n" +
                "```\n\n";
    }

    private String buildReqExample(String reqExampleType, String reqExample) {

        return "```" + reqExampleType + "\n" +
                (reqExample == null ? "" : reqExample) + "\n" +
                "```";
    }

    @NotNull
    private static String buildReqHeaderParam(List<Header> paramList) {


        if (paramList == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (Header header : paramList) {
            builder.append("|").append(header.getName())
                    .append("|").append(header.getValue())
                    .append("|").append(header.getRequired() ? "Y" : "N")
                    .append("|").append(StringUtils.isNotBlank(header.getDesc()) ? header.getDesc() : "")
                    .append("|").append("\n");
        }


        return "|参数名|参数值|必填|描述|\n" +
                "|:-----|:-----|:-----|:-----|\n" +
                builder;
    }

    @NotNull
    private static String buildReqParam(List<Param> paramList) {

        if (paramList == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (Param param : paramList) {
            builder.append("|").append(param.getName())
                    .append("|").append(param.getType())
                    .append("|").append(param.getRequired() ? "Y" : "N")
                    .append("|").append(StringUtils.isNotBlank(param.getDesc()) ? param.getDesc() : "")
                    .append("|").append("\n");
        }


        return "|参数名|类型|必选|描述|\n" +
                "|:-----|:-----|:-----|:-----|\n" +
                builder;
    }


    @NotNull
    private static String buildReqBodyParam(List<Body> paramList) {

        if (paramList == null) {
            return "";
        }

        return "|参数名|类型|必选|描述|\n" +
                "|:-----|:-----|:-----|:-----|\n" +
                buildTableContext(paramList, "");
    }


    @NotNull
    private static String buildRespBodyParam(List<Body> paramList) {

        if (paramList == null) {
            return "";
        }


        return "|参数名|类型|必选|描述|\n" +
                "|:----|:----|:-----|:-----|\n" +
                buildTableContext(paramList, "");
    }

    @NotNull
    private static StringBuilder buildTableContext(List<Body> paramList, String namePrefix) {

        StringBuilder param = new StringBuilder();

        if (CollectionUtils.isEmpty(paramList)) {
            return param;
        }

        for (Body body : paramList) {
            param.append("|").append(namePrefix).append(body.getName())
                    .append("|").append(body.getType())
                    .append("|").append(body.getRequired() ? "Y" : "N")
                    .append("|").append(StringUtils.isNotBlank(body.getDesc()) ? body.getDesc() : "")
                    .append("|").append("\n");


            if (CollectionUtils.isNotEmpty(body.getObjectReqList())) {
                param.append(buildTableContext(body.getObjectReqList(), "&emsp;&emsp;"));
            }
        }
        return param;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    // public String getDomain() {
    //     return domain;
    // }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public String getRequestHeader() {
        return requestHeader;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public String getRequestExample() {
        return requestExample;
    }

    public String getResponseParam() {
        return responseParam;
    }

    public String getResponseExample() {
        return responseExample;
    }

    public String getRequestParam() {
        return requestParam;
    }


    public String getMethodFullName() {
        return methodFullName;
    }
}
