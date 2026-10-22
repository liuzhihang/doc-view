package com.liuzhihang.doc.view.integration.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.liuzhihang.doc.view.integration.YApiFacadeService;
import com.liuzhihang.doc.view.integration.dto.YApiCat;
import com.liuzhihang.doc.view.integration.dto.YapiSave;
import com.liuzhihang.doc.view.utils.HttpUtils;
import org.apache.http.message.BasicNameValuePair;

import java.util.List;

public class YApiFacadeServiceImpl implements YApiFacadeService {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    @Override
    public void save(YapiSave save) throws Exception {
        successfulResponse(HttpUtils.post(save.getYapiUrl() + "/api/interface/save", gson.toJson(save)));
    }

    @Override
    public List<YApiCat> getCatMenu(String yapiUrl, Long projectId, String token) throws Exception {
        String body = HttpUtils.get(yapiUrl + "/api/interface/getCatMenu", List.of(
                new BasicNameValuePair("project_id", String.valueOf(projectId)),
                new BasicNameValuePair("token", token)), null);
        JsonObject response = successfulResponse(body);
        try {
            if (response.has("data") && response.get("data").isJsonArray()) {
                List<YApiCat> categories = gson.fromJson(response.get("data"),
                        new TypeToken<List<YApiCat>>() { }.getType());
                if (categories.stream().allMatch(YApiFacadeServiceImpl::hasId)) return categories;
            }
        } catch (RuntimeException exception) {
            // Do not retain parser exceptions containing remote data.
        }
        throw invalidResponse();
    }

    @Override
    public YApiCat addCat(YApiCat cat) throws Exception {
        JsonObject response = successfulResponse(HttpUtils.post(cat.getYapiUrl() + "/api/interface/add_cat", gson.toJson(cat)));
        try {
            YApiCat category = gson.fromJson(response.get("data"), YApiCat.class);
            if (hasId(category)) return category;
        } catch (RuntimeException exception) {
            // Do not retain parser exceptions containing remote data.
        }
        throw invalidResponse();
    }

    private static boolean hasId(YApiCat category) {
        return category != null && category.getId() != null && category.getId() > 0;
    }

    private static JsonObject successfulResponse(String body) throws Exception {
        try {
            JsonObject response = gson.fromJson(body, JsonObject.class);
            if (response != null && response.has("errcode")
                    && response.get("errcode").isJsonPrimitive()
                    && "0".equals(response.get("errcode").getAsString())) return response;
        } catch (RuntimeException exception) {
            // Failure bodies and malformed JSON may echo authentication or document data.
        }
        throw invalidResponse();
    }

    private static Exception invalidResponse() { return new Exception("YApi 接口响应失败或数据无效"); }
}
