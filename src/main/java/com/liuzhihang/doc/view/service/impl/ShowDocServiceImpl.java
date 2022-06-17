package com.liuzhihang.doc.view.service.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.ShowDocSettings;
import com.liuzhihang.doc.view.config.ShowDocSettingsConfigurable;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.integration.ShowDocFacadeService;
import com.liuzhihang.doc.view.integration.dto.ShowDocUpdateRequest;
import com.liuzhihang.doc.view.integration.dto.ShowDocUpdateResponse;
import com.liuzhihang.doc.view.integration.impl.ShowDocFacadeServiceImpl;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewUploadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author liuzhihang
 * @date 2021/7/27 12:00
 */
@Slf4j
@Service
public class ShowDocServiceImpl implements DocViewUploadService {


    @Override
    public boolean checkSettings(@NotNull Project project) {
        ShowDocSettings apiSettings = ShowDocSettings.getInstance(project);

        if (StringUtils.isBlank(apiSettings.getUrl())
                || StringUtils.isBlank(apiSettings.getApiKey())
                || StringUtils.isBlank(apiSettings.getApiToken())) {
            // 说明没有配置 ShowDoc 上传地址, 跳转到配置页面
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.showdoc.info.settings"));
            ShowSettingsUtil.getInstance().showSettingsDialog(project, ShowDocSettingsConfigurable.class);
            return false;
        }
        return true;
    }

    @Override
    public void doUpload(@NotNull Project project, @NotNull DocView docView) {

        try {
            ShowDocSettings settings = ShowDocSettings.getInstance(project);

            ShowDocUpdateRequest request = new ShowDocUpdateRequest();
            request.setShowDocUrl(settings.getUrl());
            request.setApiKey(settings.getApiKey());
            request.setApiToken(settings.getApiToken());
            request.setCatName(docView.getDocTitle());
            request.setPageTitle(docView.getName());
            request.setPageContent(DocViewData.markdownText(project, docView));

            ShowDocFacadeService facadeService = ApplicationManager.getApplication().getService(ShowDocFacadeServiceImpl.class);
            ShowDocUpdateResponse response = facadeService.updateByApi(request);

            ShowDocUpdateResponse.DataInner data = response.getData();

            String showDocInterfaceUrl = settings.getUrl() + "/" + data.getItemId() + "/" + data.getPageId();

            DocViewNotification.uploadSuccess(project, "ShowDoc", showDocInterfaceUrl);
        } catch (Exception e) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.showdoc.upload.error"));
            log.error("上传单个文档失败:{}", docView, e);
        }
    }
}
