package com.liuzhihang.doc.view.integration.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * @author liuzhihang
 * @date 2021/7/27 11:45
 */
@Data
public class ShowDocUpdateRequest {

    /**
     * 请求地址
     */
    private String showDocUrl;

    /**
     * api_key，认证凭证。登录showdoc，进入具体项目后，点击右上角的”项目设置”-“开放API”便可看到
     */
    @SerializedName("api_key")
    private String apiKey;

    /**
     * 同上
     */
    @SerializedName("api_token")
    private String apiToken;

    /**
     * 可选参数。当页面文档处于目录下时，请传递目录名。当目录名不存在时，showdoc会自动创建此目录。
     * 需要创建多层目录的时候请用斜杆隔开，例如 “一层/二层/三层”
     */
    @SerializedName("cat_name")
    private String catName;

    /**
     * 页面标题。请保证其唯一。
     * （或者，当页面处于目录下时，请保证页面标题在该目录下唯一）。
     * 当页面标题不存在时，showdoc将会创建此页面。当页面标题存在时，将用page_content更新其内容
     */
    @SerializedName("page_title")
    private String pageTitle;

    /**
     * 页面内容，可传递markdown格式的文本或者html源码
     */
    @SerializedName("page_content")
    private String pageContent;

    /**
     * 可选，页面序号。默认是99。数字越小，该页面越靠前
     */
    @SerializedName("s_number")
    private Long sNumber;


}
