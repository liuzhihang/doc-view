package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.liuzhihang.doc.view.DocViewBundle;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Bounded, single-attempt platform requests. Errors never retain request or response data. */
public final class HttpUtils {
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
            .setConnectTimeout(10_000)
            .setConnectionRequestTimeout(10_000)
            .setSocketTimeout(30_000)
            .build();

    private HttpUtils() { }

    public static String get(String url) throws Exception {
        return get(url, null, null);
    }

    public static String get(String url, List<NameValuePair> parameters, List<Header> headers) throws Exception {
        return execute(new HttpGet(uri(url, parameters)), headers);
    }

    public static String post(String url, String json) throws Exception {
        return post(url, json, null);
    }

    public static String post(String url, String json, List<Header> headers) throws Exception {
        HttpPost request = new HttpPost(uri(url, null));
        request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
        request.setHeader("Charset", "UTF-8");
        request.setHeader("Content-Type", "application/json");
        return execute(request, headers);
    }

    public static String put(String url, String json, List<Header> headers) throws Exception {
        HttpPut request = new HttpPut(uri(url, null));
        request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
        request.setHeader("Charset", "UTF-8");
        request.setHeader("Content-Type", "application/json");
        return execute(request, headers);
    }

    private static URI uri(String url, List<NameValuePair> parameters) throws Exception {
        ProgressManager.checkCanceled();
        try {
            URIBuilder builder = new URIBuilder(url);
            if (parameters != null && !parameters.isEmpty()) builder.setParameters(parameters);
            URI uri = builder.build();
            if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException();
            }
            return uri;
        } catch (URISyntaxException | IllegalArgumentException exception) {
            throw new Exception(DocViewBundle.message("notify.http.invalid.url"));
        }
    }

    private static String execute(HttpRequestBase request, List<Header> headers) throws Exception {
        ProgressManager.checkCanceled();
        try (var client = HttpClientBuilder.create()
                .setDefaultRequestConfig(REQUEST_CONFIG)
                .disableAutomaticRetries()
                .disableRedirectHandling()
                .build()) {
            if (headers != null) headers.forEach(request::addHeader);
            try (var response = client.execute(request)) {
                ProgressManager.checkCanceled();
                int code = response.getStatusLine().getStatusCode();
                if (code < 200 || code >= 300) throw new HttpStatusException(code);
                String body = response.getEntity() == null ? ""
                        : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                ProgressManager.checkCanceled();
                return body;
            }
        } catch (HttpStatusException exception) {
            throw new HttpStatusException(exception.getStatusCode());
        } catch (ProcessCanceledException exception) {
            throw exception;
        } catch (IOException | IllegalArgumentException exception) {
            // Transport exceptions can contain a URL, credentials or remote response text.
            throw new Exception(DocViewBundle.message("notify.http.network"));
        } finally {
            request.releaseConnection();
        }
    }

    public static final class HttpStatusException extends Exception {
        private final int statusCode;

        private HttpStatusException(int statusCode) {
            super(DocViewBundle.message("notify.http.status", statusCode));
            this.statusCode = statusCode;
        }

        public int getStatusCode() { return statusCode; }
    }
}
