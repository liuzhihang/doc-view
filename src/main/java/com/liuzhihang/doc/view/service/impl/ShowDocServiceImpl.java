package com.liuzhihang.doc.view.service.impl;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.config.ShowDocSettings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.facade.ShowDocFacadeService;
import com.liuzhihang.doc.view.facade.dto.ShowDocUpdateRequest;
import com.liuzhihang.doc.view.facade.dto.ShowDocUpdateResponse;
import com.liuzhihang.doc.view.facade.impl.ShowDocFacadeServiceImpl;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.ShowDocService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author liuzhihang
 * @date 2021/7/27 12:00
 */
@Slf4j
@Service
public class ShowDocServiceImpl implements ShowDocService {

    @Override
    public void upload(@NotNull Project project, @NotNull List<DocView> docViewList) {

        for (DocView docView : docViewList) {
            upload(project, docView);
        }

    }

    @Override
    public void upload(@NotNull Project project, @NotNull DocView docView) {

        try {
            ShowDocSettings settings = ShowDocSettings.getInstance(project);

            String catName = docView.getDocTitle();

            // 全类名过长
            if (Settings.getInstance(project).getTitleUseFullClassName() && catName.contains(".")) {
                catName = catName.substring(catName.lastIndexOf("."));
            }

            ShowDocUpdateRequest request = new ShowDocUpdateRequest();
            request.setShowDocUrl(settings.getUrl());
            request.setApiKey(settings.getApiKey());
            request.setApiToken(settings.getApiToken());
            request.setCatName(catName);
            request.setPageTitle(docView.getName());
            request.setPageContent(DocViewData.markdownText(project, docView));

            ShowDocFacadeService facadeService = ServiceManager.getService(ShowDocFacadeServiceImpl.class);
            ShowDocUpdateResponse response = facadeService.updateByApi(request);

            ShowDocUpdateResponse.DataInner data = response.getData();

            String showDocInterfaceUrl = settings.getUrl() + "/" + data.getItemId() + "/" + data.getPageId();

            DocViewNotification.notifyInfo(project, DocViewBundle.message("notify.showdoc.upload.success", showDocInterfaceUrl));
        } catch (Exception e) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.showdoc.upload.error"));
            log.error("上传单个文档失败:{}", docView, e);
        }
    }
}
