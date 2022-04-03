package com.liuzhihang.doc.view.service.impl;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.YuQueSettings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.facade.YuQueFacadeService;
import com.liuzhihang.doc.view.facade.dto.YuQueCreate;
import com.liuzhihang.doc.view.facade.dto.YuQueResponse;
import com.liuzhihang.doc.view.facade.dto.YuQueUpdate;
import com.liuzhihang.doc.view.facade.impl.YuQueFacadeServiceImpl;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewUploadService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 上传文档到语雀
 *
 * @author liuzhihang
 * @date 2021/7/27 12:00
 */
@Slf4j
@Service
public class YuQueServiceImpl implements DocViewUploadService {

    @Override
    public void upload(@NotNull Project project, @NotNull List<DocView> docViewList) {

        for (DocView docView : docViewList) {
            upload(project, docView);
        }

    }

    @Override
    public void upload(@NotNull Project project, @NotNull DocView docView) {

        try {
            YuQueSettings settings = YuQueSettings.getInstance(project);
            YuQueFacadeService facadeService = ServiceManager.getService(YuQueFacadeServiceImpl.class);

            String slug = docView.getPath().replace("/", "_");

            // 获取文章是否存在
            YuQueResponse doc = facadeService.getDoc(settings.getApiUrl(), settings.getToken(), settings.getNamespace(), slug);

            YuQueResponse yuQueResponse;
            if (doc == null) {
                // 文档不存在
                YuQueCreate yuQueCreate = new YuQueCreate();
                yuQueCreate.setTitle(docView.getDocTitle());
                yuQueCreate.setSlug(slug);
                yuQueCreate.setBody(DocViewData.markdownText(project, docView));
                yuQueResponse = facadeService.create(settings.getApiUrl(), settings.getToken(), settings.getNamespace(), yuQueCreate);

            } else {
                // 文档存在
                YuQueUpdate yuQueCreate = new YuQueUpdate();
                yuQueCreate.setTitle(docView.getDocTitle());
                yuQueCreate.setSlug(slug);
                yuQueCreate.setBody(DocViewData.markdownText(project, docView));
                yuQueResponse = facadeService.update(settings.getApiUrl(), settings.getToken(), settings.getNamespace(), doc.getData().getId(), yuQueCreate);
            }

            String yuQueDocUrl = settings.getUrl() + "/" + settings.getNamespace() + "/" + yuQueResponse.getData().getSlug();
            DocViewNotification.uploadSuccess(project, "YuQue", yuQueDocUrl);
        } catch (Exception e) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.yuque.upload.error"));
            log.error("上传单个文档失败:{}", docView, e);
        }
    }
}
