package com.liuzhihang.doc.view.integration.dto;

import lombok.Data;

/**
 * 语雀更新文档
 * <p>
 * https://www.yuque.com/yuque/developer/doc#c2e9ee2a
 * <p>
 * 默认创建都是私密文档
 *
 * @author liuzhihang
 * @date 2022/4/1 22:54
 */
@Data
public class YuQueUpdate {

    private String title;

    private String slug;

    private String body;

    private Integer _force_asl = 1;

}
