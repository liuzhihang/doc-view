package com.liuzhihang.doc.view.notification;

import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

/**
 * 进行消息通知工具类
 * <p>
 * https://plugins.jetbrains.com/docs/intellij/notifications.html?from=jetbrains.org
 *
 * @author liuzhihang
 * @date 2020/2/28 18:52
 */
public class DocViewNotification {

    private static final NotificationGroup notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup("doc-view.NotificationGroup");

    public static void notifyWarn(Project project, String message) {

        notificationGroup.createNotification(message, NotificationType.WARNING).notify(project);
    }

    public static void notifyInfo(Project project, String message) {

        notificationGroup.createNotification(message, NotificationType.INFORMATION).notify(project);
    }

    public static void notifyError(Project project, String message) {

        notificationGroup.createNotification(message, NotificationType.ERROR).notify(project);
    }


}
