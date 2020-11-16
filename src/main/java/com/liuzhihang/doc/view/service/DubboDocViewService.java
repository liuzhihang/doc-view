package com.liuzhihang.doc.view.service;

import com.intellij.openapi.components.ServiceManager;

/**
 * @author liuzhihang
 * @date 2020/11/16 18:28
 */
public interface DubboDocViewService {

    static DubboDocViewService getInstance() {
        return ServiceManager.getService(DubboDocViewService.class);
    }
}
