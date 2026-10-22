package com.liuzhihang.doc.view.integration.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.liuzhihang.doc.view.integration.ShowDocFacadeService;
import com.liuzhihang.doc.view.integration.dto.ShowDocUpdateRequest;
import com.liuzhihang.doc.view.integration.dto.ShowDocUpdateResponse;
import com.liuzhihang.doc.view.utils.HttpUtils;

public class ShowDocFacadeServiceImpl implements ShowDocFacadeService {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    @Override
    public ShowDocUpdateResponse updateByApi(ShowDocUpdateRequest request) throws Exception {
        String body = HttpUtils.post(request.getShowDocUrl() + "/api/item/updateByApi", gson.toJson(request));
        try {
            ShowDocUpdateResponse response = gson.fromJson(body, ShowDocUpdateResponse.class);
            if (response != null && "0".equals(response.getErrorCode()) && response.getData() != null
                    && positiveId(response.getData().getItemId()) && positiveId(response.getData().getPageId())) {
                return response;
            }
        } catch (RuntimeException exception) {
            // Do not retain parser exceptions containing remote data.
        }
        throw new Exception("ShowDoc 接口响应失败或数据无效");
    }

    private static boolean positiveId(String id) {
        return id != null && Long.parseLong(id) > 0;
    }
}
