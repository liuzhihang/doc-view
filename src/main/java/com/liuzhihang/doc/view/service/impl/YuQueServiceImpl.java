package com.liuzhihang.doc.view.service.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.ShowDocSettingsConfigurable;
import com.liuzhihang.doc.view.config.YuQueSettings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.integration.YuQueFacadeService;
import com.liuzhihang.doc.view.integration.dto.YuQueCreate;
import com.liuzhihang.doc.view.integration.dto.YuQueResponse;
import com.liuzhihang.doc.view.integration.dto.YuQueUpdate;
import com.liuzhihang.doc.view.integration.impl.YuQueFacadeServiceImpl;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewUploadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

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
    public boolean checkSettings(@NotNull Project project) {

        YuQueSettings apiSettings = YuQueSettings.getInstance(project);

        if (StringUtils.isBlank(apiSettings.getUrl())
                || StringUtils.isBlank(apiSettings.getApiUrl())
                || StringUtils.isBlank(apiSettings.getRepos())
                || StringUtils.isBlank(apiSettings.getLogin())
                || StringUtils.isBlank(apiSettings.getToken())) {
            // 说明没有配置语雀上传地址, 跳转到配置页面
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.yuque.info.settings"));
            ShowSettingsUtil.getInstance().showSettingsDialog(project, ShowDocSettingsConfigurable.class);
            return false;
        }
        return true;
    }

    @Override
    public void doUpload(@NotNull Project project, @NotNull DocView docView) {

        try {
            YuQueSettings settings = YuQueSettings.getInstance(project);
            YuQueFacadeService facadeService = ApplicationManager.getApplication().getService(YuQueFacadeServiceImpl.class);
            String slug;
            if (docView.getPath().startsWith("/")) {
                slug = docView.getPath().substring(1).replace("/", "_");
            } else {
                slug = docView.getPath().replace("/", "_");
            }


            // 获取文章是否存在
            YuQueResponse doc = facadeService.getDoc(settings.getApiUrl(), settings.getToken(), settings.getNamespace(), slug);

            YuQueResponse yuQueResponse;
            if (doc == null) {
                // 文档不存在
                YuQueCreate yuQueCreate = new YuQueCreate();
                yuQueCreate.setTitle(docView.getName());
                yuQueCreate.setSlug(slug);
                yuQueCreate.setBody(DocViewData.markdownText(project, docView));
                yuQueResponse = facadeService.create(settings.getApiUrl(), settings.getToken(), settings.getNamespace(), yuQueCreate);

            } else {
                // 文档存在
                YuQueUpdate yuQueCreate = new YuQueUpdate();
                yuQueCreate.setTitle(docView.getName());
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
