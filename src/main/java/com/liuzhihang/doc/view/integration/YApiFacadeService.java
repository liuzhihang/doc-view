package com.liuzhihang.doc.view.integration;

import com.liuzhihang.doc.view.integration.dto.YApiCat;
import com.liuzhihang.doc.view.integration.dto.YapiSave;

import java.util.List;

/**
 * YApi 包装 service
 * <p>
 * https://hellosean1025.github.io/yapi/openapi.html
 *
 * @author liuzhihang
 * @date 2021/6/8 19:20
 */
public interface YApiFacadeService {

    /**
     * 新增接口
     *
     * @param dto
     */
    void save(YapiSave dto) throws Exception;

    /**
     * 获取菜单列表
     *
     * @param yapiUrl
     * @param projectId
     * @param token
     * @return
     * @throws Exception
     */
    List<YApiCat> getCatMenu(String yapiUrl, Long projectId, String token) throws Exception;

    /**
     * 添加菜单
     *
     * @param cat
     * @return
     * @throws Exception
     */
    YApiCat addCat(YApiCat cat) throws Exception;
}
