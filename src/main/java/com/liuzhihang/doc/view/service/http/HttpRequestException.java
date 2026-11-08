package com.liuzhihang.doc.view.service.http;

/**
 * A safe, localized reason for why a method cannot produce an HTTP request.
 *
 * <p>This validation outcome is unchecked so it can pass through PSI helper calls and be delivered by
 * the background task's throwable callback without wrapping source-specific failures. It carries only
 * a bundle key, never source text or parameter values.
 */
public final class HttpRequestException extends RuntimeException {

    public static final String UNSUPPORTED_METHOD = "http.request.error.unsupported_method";
    public static final String UNSUPPORTED_MAPPING = "http.request.error.unsupported_mapping";
    public static final String UNSUPPORTED_PARAMETER = "http.request.error.unsupported_parameter";
    public static final String PATH_PARAMETER_MISMATCH = "http.request.error.path_parameter_mismatch";
    public static final String UNSUPPORTED_MEDIA_TYPE = "http.request.error.unsupported_media_type";
    public static final String CONTENT_TYPE_CONFLICT = "http.request.error.content_type_conflict";
    public static final String UNSAFE_HEADER = "http.request.error.unsafe_header";
    public static final String INVALID_SOURCE = "http.request.error.invalid_source";
    public static final String BUILD_FAILED = "http.request.error.build_failed";
    public static final String TOO_LARGE = "http.request.error.too_large";

    private final String reasonKey;

    public HttpRequestException(String reasonKey) {
        super();
        this.reasonKey = reasonKey;
    }

    /** Returns a bundle key without source text, parameter values, or credentials. */
    public String reasonKey() {
        return reasonKey;
    }
}
