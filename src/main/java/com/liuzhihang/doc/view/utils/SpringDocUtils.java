package com.liuzhihang.doc.view.utils;

import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.Header;
import com.liuzhihang.doc.view.dto.Param;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author liuzhihang
 * @date 2020/3/6 14:54
 */
public class SpringDocUtils {

    @NotNull
    public static String convertMarkdownText(@NotNull DocView docView) {

        return "**接口名称:**\n\n" +
                docView.getName() + "\n\n" +

                "**接口描述:**\n\n" +
                docView.getDesc() + "\n\n" +

                "**请求路径:**\n\n" +
                docView.getPath() + "\n\n" +

                "**请求方式:**\n\n" +
                docView.getMethod() + "\n\n" +

                "**请求参数:**\n\n" +

                "- Header\n\n" +
                buildReqHeaderParam(docView.getHeaderList()) + "\n\n" +

                // - Param
                buildReqParam(docView.getReqParamList()) + "\n\n" +

                // - Body
                buildReqBodyParam(docView.getReqBodyList()) + "\n\n" +

                "**请求示例:**\n\n" +
                "```" + docView.getReqExampleType() + "\n" +
                (docView.getReqExample() == null ? "" : docView.getReqExample()) + "\n" +
                "```\n\n" +

                "**返回参数:**\n\n" +
                buildRespBodyParam(docView.getRespBodyList()) + "\n\n" +

                "**返回示例:**\n\n" +
                "```json\n" +
                (docView.getRespExample() == null ? "" : docView.getRespExample()) + "\n" +
                "```\n\n";
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


        return "|参数名|参数值|必填|备注|\n" +
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
                    .append("|").append(StringUtils.isNotBlank(param.getRemark()) ? param.getRemark() : "");
        }


        return "- Param\n\n" +
                "|参数名|类型|必选|说明|备注|\n" +
                "|:-----|:-----|:-----|:-----|:-----|\n" +
                builder;
    }


    @NotNull
    private static String buildReqBodyParam(List<Body> paramList) {

        if (paramList == null) {
            return "";
        }

        return "- Body\n\n" +
                "|参数名|类型|必选|说明|备注|\n" +
                "|:-----|:-----|:-----|:-----|:-----|\n" +
                buildTableContext(paramList, "");
    }


    @NotNull
    private static String buildRespBodyParam(List<Body> paramList) {

        if (paramList == null) {
            return "";
        }


        return "|参数名|类型|必选|说明|备注|\n" +
                "|:----|:----|:-----|:-----|:-----|\n" +
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
                    .append("|").append(StringUtils.isNotBlank(body.getRemark()) ? body.getRemark() : "")
                    .append("|").append("\n");


            if (CollectionUtils.isNotEmpty(body.getObjectReqList())) {
                param.append(buildTableContext(body.getObjectReqList(), "&emsp;&emsp;"));
            }
        }
        return param;
    }

}
