package com.liuzhihang.doc.view.constant;

import java.util.HashSet;
import java.util.Set;

/**
 * dubbo 相关常量
 *
 * @author liuzhihang
 * @date 2022/4/4 20:16
 */
public class DubboConstant {

    /**
     * 包含类注解名称
     */
    public static Set<String> SERVICE_ANNOTATIONS = new HashSet<>() {{
        add(DUBBO_SERVICE);
        add(SERVICE_1);
        add(SERVICE_2);
    }};

    public static final String DUBBO_SERVICE = "org.apache.dubbo.config.annotation.DubboService";
    public static final String SERVICE_1 = "org.apache.dubbo.config.annotation.Service";
    public static final String SERVICE_2 = "com.alibaba.dubbo.config.annotation.Service";
}
