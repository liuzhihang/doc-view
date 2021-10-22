package com.liuzhihang.doc.view.notification;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.utils.DocViewIcons;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

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

    /**
     * @param project
     * @param docPlatform 接口平台名称
     * @param link        打开地址
     */
    public static void uploadSuccess(Project project, String docPlatform, String link) {

        String title = DocViewBundle.message("notify.upload.success.title", docPlatform);
        String linkText = DocViewBundle.message("notify.upload.success.link.text");
        String copy = DocViewBundle.message("notify.upload.success.link.copy");

        notificationGroup
                .createNotification(title, NotificationType.INFORMATION)
                .setIcon(DocViewIcons.DOC_VIEW)
                .addAction(new NotificationAction(linkText) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        notification.expire();
                        BrowserUtil.browse(link);

                    }
                })
                .addAction(new NotificationAction(copy) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        notification.expire();
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        StringSelection selection = new StringSelection(link);
                        clipboard.setContents(selection, selection);
                    }
                })
                .notify(project);
    }

}
