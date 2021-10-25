package com.liuzhihang.doc.view.notification;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.util.text.VersionComparatorUtil;
import com.liuzhihang.doc.view.config.ApplicationSettings;
import org.jetbrains.annotations.NotNull;

/**
 * 安装新版本或者更新时提示
 *
 * @author liuzhihang
 * @date 2021/10/22 14:09
 */
public class DocViewStartupNotification implements StartupActivity, DumbAware {

    @Override
    public void runActivity(@NotNull Project project) {

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }
        ApplicationSettings applicationSettings = ApplicationManager.getApplication().getService(ApplicationSettings.class);

        // 上次安装的版本
        String lastVersion = applicationSettings.getPluginVersion();

        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("com.liuzhihang.doc-view"));

        // 一个版本只通知一次
        if (lastVersion != null && plugin != null) {
            final int compare = VersionComparatorUtil.compare(lastVersion, plugin.getVersion());
            if (compare < 0) {
                DocViewNotification.startupNotification(project);
                applicationSettings.setPluginVersion(plugin.getVersion());
            }
        }
    }
}
