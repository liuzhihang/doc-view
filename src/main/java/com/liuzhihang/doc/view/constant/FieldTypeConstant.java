package com.liuzhihang.doc.view.constant;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NonNls;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author liuzhihang
 * @date 2020/3/4 20:35
 */
public class FieldTypeConstant {


    @NonNls
    public static final Map<String, Object> FIELD_TYPE = new HashMap<>(16);
    @NonNls
    public static final Set<String> ANNOTATION_TYPES = new HashSet<>();

    public static final Set<String> BASE_TYPE_SET = Sets.newHashSet("byte", "short", "int", "long", "char", "float",
            "double", "boolean");

    static {
        // 包装数据类型
        FIELD_TYPE.put("Byte", 0);
        FIELD_TYPE.put("Short", 0);
        FIELD_TYPE.put("Integer", 0);
        FIELD_TYPE.put("Long", 0L);
        FIELD_TYPE.put("Float", 0.0F);
        FIELD_TYPE.put("Double", 0.0D);
        FIELD_TYPE.put("Boolean", false);
        // 其他
        FIELD_TYPE.put("String", "");
        FIELD_TYPE.put("BigDecimal", null);
        FIELD_TYPE.put("Date", null);
        FIELD_TYPE.put("LocalDate", null);
        FIELD_TYPE.put("LocalTime", null);
        FIELD_TYPE.put("LocalDateTime", null);


        // 注解过滤
        ANNOTATION_TYPES.add("javax.annotation.Resource");
        ANNOTATION_TYPES.add("org.springframework.beans.factory.annotation.Autowired");
    }
}
