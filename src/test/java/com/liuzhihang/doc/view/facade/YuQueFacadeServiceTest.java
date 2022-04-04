package com.liuzhihang.doc.view.facade;

import com.liuzhihang.doc.view.facade.dto.YuQueCreate;
import com.liuzhihang.doc.view.facade.dto.YuQueResponse;
import com.liuzhihang.doc.view.facade.dto.YuQueUpdate;
import com.liuzhihang.doc.view.facade.impl.YuQueFacadeServiceImpl;
import org.junit.jupiter.api.Test;

/**
 * @author liuzhihang
 * @date 2022/3/31 23:18
 */
class YuQueFacadeServiceTest {

    public static final String url = "https://www.yuque.com/api/v2/";
    public static final String token = "qoN0dycmn2P6mSLCCDyGU61VMMG4h1fE27gDHQ12";
    public static final String namespace = "liuzhihangs/personal";

    @Test
    void getDoc() throws Exception {

        YuQueFacadeService service = new YuQueFacadeServiceImpl();
        YuQueResponse doc = service.getDoc(url, token, namespace, "test__1231");

        System.out.println("doc = " + doc);
    }

    @Test
    void create() throws Exception {

        YuQueFacadeService service = new YuQueFacadeServiceImpl();

        YuQueCreate yuQueCreate = new YuQueCreate();
        yuQueCreate.setTitle("测试插入数据");
        yuQueCreate.setSlug("test2");
        yuQueCreate.setBody("## 二级标题");

        YuQueResponse doc = service.create(url, token, namespace, yuQueCreate);

        System.out.println("doc = " + doc);
    }

    @Test
    void update() throws Exception {

        YuQueFacadeService service = new YuQueFacadeServiceImpl();

        YuQueUpdate yuQueUpdate = new YuQueUpdate();
        yuQueUpdate.setTitle("测试更新数据");
        yuQueUpdate.setSlug("test");
        yuQueUpdate.setBody("## 二级标题 更新后");

        YuQueResponse doc = service.update(url, token, namespace, 1L, yuQueUpdate);

        System.out.println("doc = " + doc);
    }
}