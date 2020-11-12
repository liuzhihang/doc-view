package com.liuzhihang.doc.view.utils;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;

/**
 * 进行消息通知工具类
 *
 * @author liuzhihang
 * @date 2020/2/28 18:52
 */
public class NotificationUtils {

    private static NotificationGroup notificationGroup = new NotificationGroup("DocView.NotificationGroup", NotificationDisplayType.BALLOON, true);

    public static void warnNotify(String message, Project project) {
        Notifications.Bus.notify(notificationGroup.createNotification(message, NotificationType.WARNING), project);
    }

    public static void infoNotify(String message, Project project) {
        Notifications.Bus.notify(notificationGroup.createNotification(message, NotificationType.INFORMATION), project);
    }

    public static void errorNotify(String message, Project project) {
        Notifications.Bus.notify(notificationGroup.createNotification(message, NotificationType.ERROR), project);
    }

}
