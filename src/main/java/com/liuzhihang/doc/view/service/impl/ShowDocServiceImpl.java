package com.liuzhihang.doc.view.service.impl;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
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
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import com.liuzhihang.doc.view.service.IntegrationCredentialService;
import com.liuzhihang.doc.view.utils.DocViewBackgroundTasks;

/**
 * @author liuzhihang
 * @date 2021/7/27 12:00
 */
@Service
public final class ShowDocServiceImpl implements DocViewUploadService {


    @Override
    public boolean checkSettings(@NotNull Project project) {
        ShowDocSettings apiSettings = ShowDocSettings.getInstance(project);
        if (StringUtils.isBlank(apiSettings.getUrl())) {
            showConfiguration(project);
            return false;
        }
        IntegrationCredentialService.CredentialStatus credentials = IntegrationCredentialService.getInstance(project)
                .readStatus(IntegrationCredentialService.Provider.SHOWDOC);
        if (!credentials.apiKeyConfigured() || !credentials.apiTokenConfigured()) {
            // 说明没有配置 ShowDoc 上传地址, 跳转到配置页面
            showConfiguration(project);
            return false;
        }
        return true;
    }

    private static void showConfiguration(Project project) {
        DocViewBackgroundTasks.invokeLater(project, () -> {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.showdoc.info.settings"));
            ShowSettingsUtil.getInstance().showSettingsDialog(project, ShowDocSettingsConfigurable.class);
        });
    }

    @Override
    public void doUpload(@NotNull Project project, @NotNull DocView docView) {

        try {
            ProgressManager.checkCanceled();
            if (project.isDisposed()) throw new ProcessCanceledException();
            ShowDocSettings settings = ShowDocSettings.getInstance(project);
            IntegrationCredentialService.ShowDocCredentials credentials = IntegrationCredentialService.getInstance(project)
                    .readShowDocCredentials();

            ShowDocUpdateRequest request = new ShowDocUpdateRequest();
            request.setShowDocUrl(settings.getUrl());
            request.setApiKey(credentials.apiKey());
            request.setApiToken(credentials.apiToken());
            request.setCatName(docView.getDocTitle());
            request.setPageTitle(docView.getName());
            request.setPageContent(DocViewData.markdownText(project, docView));

            ShowDocFacadeService facadeService = ApplicationManager.getApplication().getService(ShowDocFacadeServiceImpl.class);
            ShowDocUpdateResponse response = facadeService.updateByApi(request);

            ShowDocUpdateResponse.DataInner data = response.getData();

            String showDocInterfaceUrl = settings.getUrl() + "/" + data.getItemId() + "/" + data.getPageId();

            ProgressManager.checkCanceled();
            if (project.isDisposed()) throw new ProcessCanceledException();
            DocViewNotification.uploadSuccess(project, DocViewBundle.message("platform.name.showdoc"), showDocInterfaceUrl);
        } catch (ProcessCanceledException exception) {
            throw exception;
        } catch (Exception e) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.showdoc.upload.error"));
        }
    }
}
