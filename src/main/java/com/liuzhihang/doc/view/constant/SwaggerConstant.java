package com.liuzhihang.doc.view.constant;

/**
 * @author liuzhihang
 * @date 2021/6/10 19:01
 */
public final class SwaggerConstant {

    private SwaggerConstant() {
    }

    /**
     * Swagger 2.x 配置
     */
    public static final String API_MODEL = "io.swagger.annotations.ApiModel";
    public static final String API_MODEL_PROPERTY = "io.swagger.annotations.ApiModelProperty";
    public static final String API = "io.swagger.annotations.Api";
    public static final String API_OPERATION = "io.swagger.annotations.ApiOperation";
    public static final String API_PARAM = "io.swagger.annotations.ApiParam";


    /**
     * Swagger 3 配置
     */
    public static final String TAG = "io.swagger.v3.oas.annotations.tags.Tag";
    public static final String OPERATION = "io.swagger.v3.oas.annotations.Operation";
    public static final String PARAMETER = "io.swagger.v3.oas.annotations.Parameter";
    public static final String PARAMETERS = "io.swagger.v3.oas.annotations.Parameters";
    public static final String SCHEMA = "io.swagger.v3.oas.annotations.media.Schema";
}
