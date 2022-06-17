package com.liuzhihang.doc.view.integration.dto;

import lombok.Data;

/**
 * 语雀创建文档
 * <p>
 * https://www.yuque.com/yuque/developer/doc#63851c78
 * <p>
 * 默认创建都是私密文档
 *
 * @author liuzhihang
 * @date 2022/4/1 22:54
 */
@Data
public class YuQueCreate {

    private String title;

    private String slug;

    private String format = "markdown";

    private String body;
}
