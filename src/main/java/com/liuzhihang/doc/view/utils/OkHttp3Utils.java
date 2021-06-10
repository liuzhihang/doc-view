package com.liuzhihang.doc.view.utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.collections.MapUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * OkHTTP工具
 *
 * @author liuzhihang
 * @date 2020/1/16 20:25
 */
@Slf4j
public class OkHttp3Utils {

    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static OkHttpClient client = new OkHttpClient
            .Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();


    /**
     * POST json请求
     *
     * @param url      请求URL
     * @param jsonBody 请求体
     * @return
     */
    public static String doPostJson(String url, String jsonBody) throws Exception {

        try {
            long startTime = System.currentTimeMillis();
            log.info("doPostJson 请求:{}, 参数:{}", url, jsonBody);

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.Companion.create(jsonBody, JSON_TYPE))
                    .build();

            Response response = client.newCall(request).execute();

            String res = Objects.requireNonNull(response.body()).string();

            log.info("doPostJson 请求url:{} 返回:{} 耗时:{}ms", url, res, System.currentTimeMillis() - startTime);

            return res;
        } catch (Exception e) {
            log.error("doPostJson请求异常, 当前请求地址:{}", url, e);
            throw e;
        }

    }

    /**
     * GET
     *
     * @param url     请求URL
     * @param headers 请求头
     * @return
     */
    public static String doGet(String url, Map<String, String> headers) throws IOException {

        try {

            long startTime = System.currentTimeMillis();
            log.info("doGet 请求:{}", url);

            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .get();
            buildHeader(builder, headers);

            Response response = client.newCall(builder.build()).execute();
            String res = Objects.requireNonNull(response.body()).string();

            log.info("doGet 请求url:{} 返回:{} 耗时:{}ms", url, res, System.currentTimeMillis() - startTime);
            return res;
        } catch (Exception e) {
            log.error("http请求异常, 当前请求地址:{}", url, e);
            throw e;
        }

    }


    /**
     * 增加header
     *
     * @param builder
     * @param headers
     */
    private static void buildHeader(Request.Builder builder, Map<String, String> headers) {

        if (MapUtils.isNotEmpty(headers)) {
            Set<String> headerNames = headers.keySet();
            headerNames.forEach(headerName -> builder.addHeader(headerName, headers.get(headerName)));
        }
    }

}
