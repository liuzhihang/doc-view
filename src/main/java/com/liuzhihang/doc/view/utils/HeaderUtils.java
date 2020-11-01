package com.liuzhihang.doc.view.utils;

import com.liuzhihang.doc.view.config.HeaderConfig;
import com.liuzhihang.doc.view.dto.Header;
import org.jetbrains.annotations.NotNull;

/**
 * @author liuzhihang
 * @date 2020/3/6 13:24
 */
public class HeaderUtils {

    @NotNull
    public static Header buildJsonHeader() {

        Header header = new Header();
        header.setRequired(true);
        header.setName("Content-Type");
        header.setValue(HeaderConfig.APPLICATION_JSON);
        header.setDesc(HeaderConfig.APPLICATION_JSON);


        return header;
    }

    @NotNull
    public static Header buildFormHeader() {

        Header header = new Header();
        header.setRequired(true);
        header.setName("Content-Type");
        header.setValue(HeaderConfig.APPLICATION_FORM);
        header.setDesc(HeaderConfig.APPLICATION_FORM);


        return header;
    }

}
