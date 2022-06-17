package com.liuzhihang.doc.view.integration.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * 分组,菜单列表
 *
 * @author liuzhihang
 * @date 2021/6/8 23:19
 */
@Data
public class YApiCat {

    private String yapiUrl;

    private String token;

    @SerializedName("project_id")
    private Long projectId;

    @SerializedName("_id")
    private Long id;

    private String name;

    private String desc;
}
