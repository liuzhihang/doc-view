package com.liuzhihang.doc.view.enums;

import lombok.Getter;

/**
 * http 请求 content-type 枚举
 *
 * @author liuzhihang
 * @version ContentTypeEnum.java, v 0.1 2022年06月16日 8:38 PM liuzhihang
 */
@Getter
public enum ContentTypeEnum {

    JSON("Content-Type", "application/json"),
    FORM("Content-Type", "application/x-www-form-urlencoded"),

    ;

    ContentTypeEnum(String key, String value) {
        this.key = key;
        this.value = value;
    }

    private String key;

    private String value;

}