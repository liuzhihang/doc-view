package com.liuzhihang.doc.view.integration.dto;

import lombok.Data;

/**
 * @author liuzhihang
 * @date 2021/6/8 22:58
 */
@Data
public class YApiQuery {

    private String name;

    private String type;

    private String example;

    private String desc;

    /**
     * 枚举: 1,0
     */
    private String required;

}
