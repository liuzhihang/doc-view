package com.liuzhihang.doc.view.utils;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

/**
 * 进行消息通知工具类
 *
 * @author liuzhihang
 * @date 2020/2/28 18:52
 */
public class NotificationUtils {

    public static void infoNotify(String message, @Nullable Project project) {
        NotificationGroupManager.getInstance().getNotificationGroup("doc-view.NotificationGroup")
                .createNotification(message, NotificationType.INFORMATION)
                .notify(project);
    }

    public static void errorNotify(String message, @Nullable Project project) {
        NotificationGroupManager.getInstance().getNotificationGroup("doc-view.NotificationGroup")
                .createNotification(message, NotificationType.ERROR)
                .notify(project);
    }


}
