package com.liuzhihang.doc.view.integration.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.liuzhihang.doc.view.integration.YuQueFacadeService;
import com.liuzhihang.doc.view.integration.dto.YuQueCreate;
import com.liuzhihang.doc.view.integration.dto.YuQueResponse;
import com.liuzhihang.doc.view.integration.dto.YuQueUpdate;
import com.liuzhihang.doc.view.utils.HttpUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.util.List;

/**
 * @author liuzhihang
 * @date 2022/3/31 23:16
 */
public class YuQueFacadeServiceImpl implements YuQueFacadeService {

    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    @Override
    public YuQueResponse getDoc(String url, String token, String namespace, String slug) throws Exception {

        Header header = new BasicHeader("X-Auth-Token", token);


        String resp = null;
        try {
            resp = HttpUtils.get(url + "/repos/" + namespace + "/docs/" + slug, null, List.of(header));
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message.contains("404") && message.contains("Not Found")) {
                return null;
            }
        }

        return gson.fromJson(resp, YuQueResponse.class);
    }

    @Override
    public YuQueResponse create(String url, String token, String namespace, YuQueCreate yuQueCreate) throws Exception {

        Header header = new BasicHeader("X-Auth-Token", token);

        String resp = HttpUtils.post(url + "/repos/" + namespace + "/docs/", gson.toJson(yuQueCreate), List.of(header));

        if (StringUtils.isBlank(resp)) {
            throw new Exception("语雀接口返回为空");
        }
        return gson.fromJson(resp, YuQueResponse.class);
    }

    @Override
    public YuQueResponse update(String url, String token, String namespace, Long id, YuQueUpdate yuQueUpdate) throws Exception {

        Header header = new BasicHeader("X-Auth-Token", token);

        String resp = HttpUtils.put(url + "/repos/" + namespace + "/docs/" + id, gson.toJson(yuQueUpdate), List.of(header));

        if (StringUtils.isBlank(resp)) {
            throw new Exception("语雀接口返回为空");
        }
        return gson.fromJson(resp, YuQueResponse.class);
    }
}
