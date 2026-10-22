package com.liuzhihang.doc.view.notification;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.extensions.PluginAware;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.util.text.VersionComparatorUtil;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.ApplicationSettings;
import com.liuzhihang.doc.view.ui.welcome.DocViewWelcomeService;
import org.jetbrains.annotations.NotNull;

/**
 * 安装新版本或者更新时提示
 *
 * @author liuzhihang
 * @date 2021/10/22 14:09
 */
public class DocViewStartupNotification implements StartupActivity, DumbAware, PluginAware {

    private PluginDescriptor pluginDescriptor;

    @Override
    public void setPluginDescriptor(@NotNull PluginDescriptor pluginDescriptor) {
        this.pluginDescriptor = pluginDescriptor;
    }

    @Override
    public void runActivity(@NotNull Project project) {

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }
        String currentVersion = pluginDescriptor == null ? null : pluginDescriptor.getVersion();
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed() || currentVersion == null) return;
            ApplicationSettings settings = ApplicationSettings.getInstance();
            String display = startupDisplay(settings.getPluginVersion(), currentVersion, settings.isShowUpdateNotification());
            settings.setPluginVersion(currentVersion);
            if (display.equals("welcome")) {
                DocViewWelcomeService.getInstance(project).open("welcome");
            } else if (display.equals("update")) {
                NotificationGroupManager.getInstance().getNotificationGroup("doc-view.NotificationGroup")
                        .createNotification(DocViewBundle.message("welcome.update.notification.title", currentVersion),
                                DocViewBundle.message("welcome.update.notification.content"), NotificationType.INFORMATION)
                        .addAction(NotificationAction.createSimpleExpiring(
                                DocViewBundle.message("welcome.update.notification.action"), () ->
                                DocViewWelcomeService.getInstance(project).open("updates")))
                        .notify(project);
            }
        });
    }

    static String startupDisplay(String lastVersion, String currentVersion, boolean enabled) {
        if (currentVersion == null || currentVersion.isBlank()) return "none";
        if (lastVersion == null || lastVersion.isBlank()) return "welcome";
        return enabled && VersionComparatorUtil.compare(lastVersion, currentVersion) < 0 ? "update" : "none";
    }
}
