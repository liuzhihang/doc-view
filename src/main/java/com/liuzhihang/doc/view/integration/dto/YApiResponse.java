package com.liuzhihang.doc.view.integration.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author liuzhihang
 * @date 2021/6/8 20:03
 */
@Data
public class YApiResponse<T> implements Serializable {

    private String errmsg;
    private Long errcode;
    private T data;

}
