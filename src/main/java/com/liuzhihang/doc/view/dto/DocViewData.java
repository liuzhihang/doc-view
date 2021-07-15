package com.liuzhihang.doc.view.dto;

import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.utils.VelocityUtils;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DocView 的模版 用来使用 Velocity 生成内容
 * <p>
 * Velocity 会根据 get 方法 获取值, 不提供 set 方法
 *
 * @author liuzhihang
 * @date 2020/11/21 16:39
 */
@Data
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
    private final List<ParamData> requestHeaderDataList;

    private final String requestHeader;

    /**
     * 请求参数
     */
    private final List<ParamData> requestParamDataList;

    private final String requestParam;


    /**
     * 请求参数
     */
    private final List<ParamData> requestBodyDataList;

    private final String requestBody;

    /**
     * 请求示例
     */
    private final String requestExample;

    /**
     * 返回参数
     */
    private final List<ParamData> responseParamDataList;
    private final String responseParam;


    /**
     * 返回示例
     */
    private final String responseExample;

    private final String type;

    public DocViewData(DocView docView) {

        this.name = docView.getName();
        this.desc = docView.getDesc();
        this.path = docView.getPath();
        this.method = docView.getMethod();
        this.type = docView.getType();

        this.requestHeaderDataList = buildReqHeaderDataList(docView.getHeaderList());
        this.requestHeader = buildReqHeaderParam(requestHeaderDataList);

        this.requestParamDataList = buildReqParamDataList(docView.getReqParamList());
        this.requestParam = buildReqParam(requestParamDataList);

        this.requestBodyDataList = buildBodyDataList(docView.getReqRootBody().getChildList());
        this.requestBody = buildBodyParam(requestBodyDataList);
        this.requestExample = buildReqExample(docView.getReqExampleType(), docView.getReqExample());

        this.responseParamDataList = buildBodyDataList(docView.getRespRootBody().getChildList());
        this.responseParam = buildBodyParam(responseParamDataList);
        this.responseExample = buildRespExample(docView.getReqExampleType(), docView.getRespExample());

    }

    @NotNull
    @Contract("_ -> new")
    public static DocViewData getInstance(@NotNull DocView docView) {
        return new DocViewData(docView);
    }

    /**
     * 递归 body 生成 List<ParamData>
     *
     * @param dataList
     * @param bodyList
     * @param prefix
     */
    private static void buildBodyDataList(@NotNull List<ParamData> dataList, @NotNull List<Body> bodyList, String prefix) {

        for (Body body : bodyList) {
            ParamData paramData = ParamData.convertFromBody(body);
            paramData.setPrefix(prefix);
            dataList.add(paramData);

            if (CollectionUtils.isNotEmpty(body.getChildList())) {
                buildBodyDataList(dataList, body.getChildList(), prefix + "-->");
            }
        }
    }

    @NotNull
    private static String buildBodyParam(List<ParamData> dataList) {

        if (CollectionUtils.isEmpty(dataList)) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (ParamData data : dataList) {
            builder.append("|").append(data.getPrefix()).append(data.getName())
                    .append("|").append(data.getType())
                    .append("|").append(data.getRequired() ? "Y" : "N")
                    .append("|").append(data.getDesc())
                    .append("|").append("\n");
        }

        return "|参数名|类型|必选|描述|\n" +
                "|:-----|:-----|:-----|:-----|\n" +
                builder.toString();
    }

    public static String buildMarkdownText(Project project, DocView docView) {

        DocViewData docViewData = new DocViewData(docView);

        if (docView.getType().equalsIgnoreCase("Dubbo")) {
            return VelocityUtils.convert(TemplateSettings.getInstance(project).getDubboTemplate(), docViewData);
        } else {
            // 按照 Spring 模版
            return VelocityUtils.convert(TemplateSettings.getInstance(project).getSpringTemplate(), docViewData);
        }
    }

    @NotNull
    private static String buildReqHeaderParam(List<ParamData> dataList) {


        if (CollectionUtils.isEmpty(dataList)) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (ParamData data : dataList) {
            builder.append("|").append(data.getName())
                    .append("|").append(data.getExample())
                    .append("|").append(data.getRequired() ? "Y" : "N")
                    .append("|").append(data.getDesc())
                    .append("|").append("\n");
        }


        return "|参数名|参数值|必填|描述|\n" +
                "|:-----|:-----|:-----|:-----|\n" +
                builder.toString();
    }

    @NotNull
    private static String buildReqParam(List<ParamData> dataList) {

        if (dataList == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (ParamData data : dataList) {
            builder.append("|").append(data.getName())
                    .append("|").append(data.getType())
                    .append("|").append(data.getRequired() ? "Y" : "N")
                    .append("|").append(data.getDesc())
                    .append("|").append("\n");
        }

        return "|参数名|类型|必选|描述|\n" +
                "|:-----|:-----|:-----|:-----|\n" +
                builder.toString();
    }

    private List<ParamData> buildReqHeaderDataList(List<Header> headerList) {

        if (CollectionUtils.isEmpty(headerList)) {
            return new ArrayList<>();
        }

        return headerList.stream().map(ParamData::convertFromHeader).collect(Collectors.toList());
    }

    private List<ParamData> buildReqParamDataList(List<Param> reqParamList) {
        if (CollectionUtils.isEmpty(reqParamList)) {
            return new ArrayList<>();
        }

        return reqParamList.stream().map(ParamData::convertFromParam).collect(Collectors.toList());
    }

    @NotNull
    public static List<ParamData> buildBodyDataList(List<Body> reqBodyList) {
        List<ParamData> dataList = new ArrayList<>();

        if (CollectionUtils.isEmpty(reqBodyList)) {
            return dataList;
        }

        buildBodyDataList(dataList, reqBodyList, "");

        return dataList;
    }

    @NotNull
    @Contract(pure = true)
    private String buildReqExample(String reqExampleType, String reqExample) {

        return "```" + reqExampleType + "\n" +
                (reqExample == null ? "" : reqExample) + "\n" +
                "```";
    }

    @NotNull
    @Contract(pure = true)
    private String buildRespExample(String respExampleType, String respExample) {
        return "```json\n" +
                (respExampleType == null ? "" : respExample) + "\n" +
                "```\n\n";
    }



}
