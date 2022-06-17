package com.liuzhihang.doc.view.integration.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.liuzhihang.doc.view.integration.YApiFacadeService;
import com.liuzhihang.doc.view.integration.dto.YApiCat;
import com.liuzhihang.doc.view.integration.dto.YApiResponse;
import com.liuzhihang.doc.view.integration.dto.YapiSave;
import com.liuzhihang.doc.view.utils.HttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author liuzhihang
 * @date 2021/6/8 19:20
 */
@Slf4j
public class YApiFacadeServiceImpl implements YApiFacadeService {

    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    @Override
    public void save(YapiSave save) throws Exception {

        String resp = HttpUtils.post(save.getYapiUrl() + "/api/interface/save", gson.toJson(save));

        if (StringUtils.isBlank(resp)) {
            throw new Exception("YApi 接口返回为空");
        }

        JsonObject jsonObject = gson.fromJson(resp, JsonObject.class);

        if (jsonObject.get("errcode").getAsInt() != 0) {
            throw new Exception("YApi 接口返回失败:" + resp);
        }
    }

    @Override
    public List<YApiCat> getCatMenu(String yapiUrl, Long projectId, String token) throws Exception {

        String url = yapiUrl + "/api/interface/getCatMenu" +
                "?project_id=" + projectId +
                "&token=" + token;

        String resp = HttpUtils.get(url);

        Type jsonType = new TypeToken<YApiResponse<List<YApiCat>>>() {
        }.getType();

        YApiResponse<List<YApiCat>> response = gson.fromJson(resp, jsonType);

        if (response.getErrcode() != 0) {
            throw new Exception("YApi 接口返回失败:" + resp);
        }
        return response.getData();
    }

    @Override
    public YApiCat addCat(YApiCat cat) throws Exception {

        String resp = HttpUtils.post(cat.getYapiUrl() + "/api/interface/add_cat", gson.toJson(cat));

        if (StringUtils.isBlank(resp)) {
            throw new Exception("YApi 接口返回为空");
        }

        Type jsonType = new TypeToken<YApiResponse<YApiCat>>() {
        }.getType();

        YApiResponse<YApiCat> response = gson.fromJson(resp, jsonType);

        if (response == null || response.getErrcode() != 0) {
            throw new Exception("YApi 接口返回失败:" + resp);
        }
        return response.getData();

    }
}
