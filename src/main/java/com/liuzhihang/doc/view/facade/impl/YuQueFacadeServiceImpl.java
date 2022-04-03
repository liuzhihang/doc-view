package com.liuzhihang.doc.view.facade.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.liuzhihang.doc.view.facade.YuQueFacadeService;
import com.liuzhihang.doc.view.facade.dto.YuQueCreate;
import com.liuzhihang.doc.view.facade.dto.YuQueResponse;
import com.liuzhihang.doc.view.facade.dto.YuQueUpdate;
import com.liuzhihang.doc.view.utils.HttpUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

/**
 * @author liuzhihang
 * @date 2022/3/31 23:16
 */
public class YuQueFacadeServiceImpl implements YuQueFacadeService {

    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    @Override
    public YuQueResponse getDoc(String url, String token, String namespace, String slug) throws Exception {

        HashMap<String, String> header = new HashMap<>();
        header.put("X-Auth-Token", token);

        String resp = HttpUtils.get(url + "/repos/" + namespace + "/docs/" + slug, header);

        if (StringUtils.isBlank(resp)) {
            return null;
        }

        return gson.fromJson(resp, YuQueResponse.class);
    }

    @Override
    public YuQueResponse create(String url, String token, String namespace, YuQueCreate yuQueCreate) throws Exception {

        HashMap<String, String> header = new HashMap<>();
        header.put("X-Auth-Token", token);

        String resp = HttpUtils.post("POST", url + "/repos/" + namespace + "/docs/", gson.toJson(yuQueCreate), header);

        if (StringUtils.isBlank(resp)) {
            throw new Exception("语雀接口返回为空");
        }
        return gson.fromJson(resp, YuQueResponse.class);
    }

    @Override
    public YuQueResponse update(String url, String token, String namespace, Long id, YuQueUpdate yuQueUpdate) throws Exception {

        HashMap<String, String> header = new HashMap<>();
        header.put("X-Auth-Token", token);

        String resp = HttpUtils.post("PUT", url + "/repos/" + namespace + "/docs/" + id, gson.toJson(yuQueUpdate), header);

        if (StringUtils.isBlank(resp)) {
            throw new Exception("语雀接口返回为空");
        }
        return gson.fromJson(resp, YuQueResponse.class);
    }
}
