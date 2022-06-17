package com.liuzhihang.doc.view.constant;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liuzhihang
 * @date 2020/3/4 13:44
 */
public final class SpringConstant {

    private SpringConstant() {
    }

    /**
     * controller
     */
    public static final String CONTROLLER = "org.springframework.stereotype.Controller";
    public static final String REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController";
    public static final String FEIGN_CLIENT="org.springframework.cloud.openfeign.FeignClient";

    /**
     * mapping
     */
    public static final String GET_MAPPING = "org.springframework.web.bind.annotation.GetMapping";
    public static final String POST_MAPPING = "org.springframework.web.bind.annotation.PostMapping";
    public static final String PUT_MAPPING = "org.springframework.web.bind.annotation.PutMapping";
    public static final String DELETE_MAPPING = "org.springframework.web.bind.annotation.DeleteMapping";
    public static final String PATCH_MAPPING = "org.springframework.web.bind.annotation.PatchMapping";

    public static final String REQUEST_MAPPING = "org.springframework.web.bind.annotation.RequestMapping";
    public static final String MAPPING = "org.springframework.web.bind.annotation.Mapping";

    /**
     * param
     */
    public static final String PATH_VARIABLE = "org.springframework.web.bind.annotation.PathVariable";
    public static final String REQUEST_ATTRIBUTE = "org.springframework.web.bind.annotation.RequestAttribute";
    public static final String SESSION_ATTRIBUTE = "org.springframework.web.bind.annotation.SessionAttribute";
    public static final String SESSION_ATTRIBUTES = "org.springframework.web.bind.annotation.SessionAttributes";
    public static final String REQUEST_BODY = "org.springframework.web.bind.annotation.RequestBody";

    public static final String REQUEST_PARAM = "org.springframework.web.bind.annotation.RequestParam";
    public static final String REQUEST_PART = "org.springframework.web.bind.annotation.RequestPart";
    /**
     * header
     */
    public static final String REQUEST_HEADER = "org.springframework.web.bind.annotation.RequestHeader";

    public static final String RESPONSE_BODY = "org.springframework.web.bind.annotation.ResponseBody";
    public static final String RESPONSE_STATUS = "org.springframework.web.bind.annotation.ResponseStatus";

    /**
     * 校验
     */
    public static final String VALIDATED = "org.springframework.validation.annotation.Validated";

    /**
     * other
     */
    public static final String CONTROLLER_ADVICE      = "org.springframework.web.bind.annotation.ControllerAdvice";
    public static final String REST_CONTROLLER_ADVICE = "org.springframework.web.bind.annotation.RestControllerAdvice";
    public static final String COOKIE_VALUE           = "org.springframework.web.bind.annotation.CookieValue";
    public static final String CROSS_ORIGIN           = "org.springframework.web.bind.annotation.CrossOrigin";

    public static final String EXCEPTION_HANDLER = "org.springframework.web.bind.annotation.ExceptionHandler";
    public static final String INIT_BINDER       = "org.springframework.web.bind.annotation.InitBinder";
    public static final String MATRIX_VARIABLE   = "org.springframework.web.bind.annotation.MatrixVariable";
    public static final String MODEL_ATTRIBUTE   = "org.springframework.web.bind.annotation.ModelAttribute";

    /**
     * Spring Mapping 注解列表
     */
    public static final List<String> MAPPING_ANNOTATIONS = new ArrayList<>() {{

        add(GET_MAPPING);
        add(POST_MAPPING);
        add(PUT_MAPPING);
        add(DELETE_MAPPING);
        add(PATCH_MAPPING);
        add(REQUEST_MAPPING);
    }};

}
