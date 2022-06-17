package com.liuzhihang.doc.view.integration.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * @author liuzhihang
 * @date 2021/7/27 11:45
 */
@Data
public class ShowDocUpdateResponse {

    @SerializedName("error_code")
    private String errorCode;
    private DataInner data;

    @Data
    public static class DataInner {

        @SerializedName("s_number")
        private String sNumber;

        @SerializedName("page_title")
        private String pageTitle;

        @SerializedName("page_id")
        private String pageId;

        @SerializedName("page_content")
        private String pageContent;

        @SerializedName("page_comments")
        private String pageComments;

        @SerializedName("item_id")
        private String itemId;

        @SerializedName("cat_id")
        private String catId;

        @SerializedName("author_username")
        private String authorUsername;

        @SerializedName("author_uid")
        private String authorUid;

        private String addtime;
    }
}
