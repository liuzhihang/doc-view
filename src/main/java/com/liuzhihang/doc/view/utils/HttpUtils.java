package com.liuzhihang.doc.view.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author liuzhihang
 * @date 2021/6/11 20:07
 */
@Slf4j
public class HttpUtils {


    /**
     * get请求
     *
     * @param url
     * @return
     */
    public static String get(String url) throws IOException {
        return get(url, new HashMap<>(4));
    }


    /**
     * get请求
     *
     * @param url
     * @param header
     * @return
     */
    public static String get(String url, Map<String, String> header) throws IOException {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

            for (String key : header.keySet()) {
                connection.setRequestProperty(key, header.get(key));
            }

            connection.connect();
            // 获取输入流
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            connection.disconnect();
            return sb.toString();
        } catch (Exception e) {
            log.error("http请求异常, 当前请求地址:{}", url, e);
            throw e;
        }
    }


    /**
     * post    请求
     *
     * @param url
     * @param jsonStr
     * @return
     */
    public static String post(String url, String jsonStr) throws Exception {

        return post("POST", url, jsonStr, new HashMap<>(4));
    }


    public static String post(String requestMethod, String url, String jsonStr, HashMap<String, String> header) throws IOException {

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(requestMethod);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);//读取超时 单位毫秒
            // 发送POST请求必须设置如下两行
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Content-Type", "application/json");

            for (String key : header.keySet()) {
                connection.setRequestProperty(key, header.get(key));
            }

            connection.connect();
            OutputStream os = connection.getOutputStream();
            os.write(jsonStr.getBytes(StandardCharsets.UTF_8));
            os.flush();

            StringBuilder sb = new StringBuilder();
            int httpRspCode = connection.getResponseCode();
            if (httpRspCode == HttpURLConnection.HTTP_OK) {
                // 开始获取数据
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                return sb.toString();

            }
            return null;
        } catch (Exception e) {
            log.error("doPostJson请求异常, 当前请求地址:{}", url, e);
            throw e;
        }

    }
}
