package com.liuzhihang.doc.view.action.toolbar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.ShowDocSettings;
import com.liuzhihang.doc.view.config.ShowDocSettingsConfigurable;
import com.liuzhihang.doc.view.config.YApiSettings;
import com.liuzhihang.doc.view.config.YApiSettingsConfigurable;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author liuzhihang
 * @date 2021/10/23 23:13
 */
public abstract class AbstractUploadAction extends AnAction {

    public abstract void actionPerformed(@NotNull AnActionEvent e);

    public void checkYApiSettings(@NotNull Project project) {
        YApiSettings apiSettings = YApiSettings.getInstance(project);

        if (StringUtils.isBlank(apiSettings.getUrl())
                || apiSettings.getProjectId() == null
                || StringUtils.isBlank(apiSettings.getToken())) {
            // 说明没有配置 YApi 上传地址, 跳转到配置页面
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.yapi.info.settings"));
            ShowSettingsUtil.getInstance().showSettingsDialog(project, YApiSettingsConfigurable.class);

        }
    }

    public void checkShowDocSettings(@NotNull Project project) {
        ShowDocSettings apiSettings = ShowDocSettings.getInstance(project);

        if (StringUtils.isBlank(apiSettings.getUrl())
                || StringUtils.isBlank(apiSettings.getApiKey())
                || StringUtils.isBlank(apiSettings.getApiToken())) {
            // 说明没有配置 ShowDoc 上传地址, 跳转到配置页面
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.showdoc.info.settings"));
            ShowSettingsUtil.getInstance().showSettingsDialog(project, ShowDocSettingsConfigurable.class);

        }
    }
}
