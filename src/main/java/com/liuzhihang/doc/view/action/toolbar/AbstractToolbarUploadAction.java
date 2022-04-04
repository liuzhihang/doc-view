package com.liuzhihang.doc.view.action.toolbar;

import com.liuzhihang.doc.view.action.AbstractUploadAction;
import com.liuzhihang.doc.view.service.DocViewUploadService;

/**
 * @author liuzhihang
 * @date 2021/10/23 23:13
 */
public abstract class AbstractToolbarUploadAction extends AbstractUploadAction {

    @Override
    protected DocViewUploadService uploadService() {
        return null;
    }
}
