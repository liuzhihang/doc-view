package com.liuzhihang.doc.view.integration.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;
import com.liuzhihang.doc.view.integration.ShowDocFacadeService;
import com.liuzhihang.doc.view.integration.dto.ShowDocUpdateRequest;
import com.liuzhihang.doc.view.integration.dto.ShowDocUpdateResponse;
import com.liuzhihang.doc.view.utils.HttpUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author liuzhihang
 * @date 2021/7/27 11:53
 */
public class ShowDocFacadeServiceImpl implements ShowDocFacadeService {

    private static final Logger LOGGER = Logger.getInstance(ShowDocFacadeServiceImpl.class);

    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    @Override
    public ShowDocUpdateResponse updateByApi(ShowDocUpdateRequest request) throws Exception {

        String resp = HttpUtils.post(request.getShowDocUrl() + "/api/item/updateByApi", gson.toJson(request));

        if (StringUtils.isBlank(resp)) {
            throw new Exception("ShowDoc 接口返回为空");
        }

        JsonObject jsonObject = gson.fromJson(resp, JsonObject.class);

        if (!jsonObject.get("error_code").getAsString().equals("0")) {
            throw new Exception("ShowDoc 接口返回失败:" + resp);
        }
        return gson.fromJson(jsonObject, ShowDocUpdateResponse.class);
    }
}
