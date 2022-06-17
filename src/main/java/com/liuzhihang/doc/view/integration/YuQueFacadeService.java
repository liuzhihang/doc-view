package com.liuzhihang.doc.view.integration;

import com.liuzhihang.doc.view.integration.dto.YuQueCreate;
import com.liuzhihang.doc.view.integration.dto.YuQueResponse;
import com.liuzhihang.doc.view.integration.dto.YuQueUpdate;

/**
 * 语雀包装 service
 * <p>
 * https://www.yuque.com/yuque/developer/doc#Response-1
 *
 * @author liuzhihang
 * @date 2022-03-31 23:01:56
 */
public interface YuQueFacadeService {

    /**
     * 获取单篇文章
     *
     * @param url
     * @param token
     * @param namespace
     * @param slug
     * @return
     * @throws Exception
     */
    YuQueResponse getDoc(String url, String token, String namespace, String slug) throws Exception;

    /**
     * 创建文档
     *
     * @param url
     * @param token
     * @param namespace
     * @param yuQueCreate
     * @return
     * @throws Exception
     */
    YuQueResponse create(String url, String token, String namespace, YuQueCreate yuQueCreate) throws Exception;

    /**
     * 更新文档
     *
     * @param url
     * @param token
     * @param namespace
     * @param id          文章的 id
     * @param yuQueUpdate
     * @return
     * @throws Exception
     */
    YuQueResponse update(String url, String token, String namespace, Long id, YuQueUpdate yuQueUpdate) throws Exception;
}
