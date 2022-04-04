package com.liuzhihang.doc.view.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.List;

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
    public static String get(String url) throws Exception {
        return get(url, null, null);
    }

    /**
     * GET 请求
     *
     * @param url
     * @param parameters
     * @param headers
     * @return
     * @throws Exception
     */
    public static String get(String url, List<NameValuePair> parameters, List<Header> headers) throws Exception {
        URIBuilder uriBuilder = new URIBuilder(url);
        if (parameters != null && !parameters.isEmpty()) {
            uriBuilder.setParameters(parameters);
        }

        HttpGet httpGet = new HttpGet(uriBuilder.build());
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(httpGet::addHeader);
        }

        HttpClient client = HttpClientBuilder.create().build();
        try {
            HttpResponse response = client.execute(httpGet);
            int code = response.getStatusLine().getStatusCode();
            if (code >= 400) {
                throw new RuntimeException(EntityUtils.toString(response.getEntity()));
            }
            return EntityUtils.toString(response.getEntity());
        } catch (ClientProtocolException e) {
            throw new Exception("Client protocol exception!", e);
        } catch (IOException e) {
            throw new Exception("IO error!", e);
        } finally {
            httpGet.releaseConnection();
        }
    }


    public static String post(String url, String jsonStr) throws Exception {

        return post(url, jsonStr, null);
    }

    public static String post(String url, String jsonStr, List<Header> headers) throws Exception {

        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Charset", "UTF-8");
        httpPost.addHeader("Content-Type", "application/json");

        if (headers != null && !headers.isEmpty()) {
            headers.forEach(httpPost::addHeader);
        }

        httpPost.setEntity(new StringEntity(jsonStr, "UTF-8"));
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpResponse response = client.execute(httpPost);
            int code = response.getStatusLine().getStatusCode();
            if (code >= 400) {
                throw new Exception(EntityUtils.toString(response.getEntity()));
            }
            return EntityUtils.toString(response.getEntity());
        } catch (ClientProtocolException e) {
            throw new Exception("Client protocol exception!", e);
        } catch (IOException e) {
            throw new Exception("IO error!", e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    /**
     * put 请求
     *
     * @param url
     * @param jsonStr
     * @param headers
     * @return
     * @throws Exception
     */
    public static String put(String url, String jsonStr, List<Header> headers) throws Exception {

        HttpPut httpPut = new HttpPut(url);
        httpPut.addHeader("Charset", "UTF-8");
        httpPut.addHeader("Content-Type", "application/json");

        if (headers != null && !headers.isEmpty()) {
            headers.forEach(httpPut::addHeader);
        }

        httpPut.setEntity(new StringEntity(jsonStr, "UTF-8"));
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpResponse response = client.execute(httpPut);
            int code = response.getStatusLine().getStatusCode();
            if (code >= 400) {
                throw new Exception(EntityUtils.toString(response.getEntity()));
            }
            return EntityUtils.toString(response.getEntity());
        } catch (ClientProtocolException e) {
            throw new Exception("Client protocol exception!", e);
        } catch (IOException e) {
            throw new Exception("IO error!", e);
        } finally {
            httpPut.releaseConnection();
        }
    }


}
