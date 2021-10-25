package com.liuzhihang.doc.view.notification;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.DocViewBundle;
import icons.DocViewIcons;
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

    public static void notifyWarn(Project project, String message) {

        NotificationGroupManager.getInstance().getNotificationGroup("doc-view.NotificationGroup")
                .createNotification(message, NotificationType.WARNING).notify(project);
    }

    public static void notifyInfo(Project project, String message) {

        NotificationGroupManager.getInstance().getNotificationGroup("doc-view.NotificationGroup")
                .createNotification(message, NotificationType.INFORMATION).notify(project);
    }

    public static void notifyError(Project project, String message) {

        NotificationGroupManager.getInstance().getNotificationGroup("doc-view.NotificationGroup")
                .createNotification(message, NotificationType.ERROR).notify(project);
    }

    /**
     * @param project
     * @param docPlatform 接口平台名称
     * @param link        打开地址
     */
    public static void uploadSuccess(Project project, String docPlatform, String link) {

        String info = DocViewBundle.message("notify.upload.success.info", docPlatform);
        String linkText = DocViewBundle.message("notify.upload.success.link.text");
        String copy = DocViewBundle.message("notify.upload.success.link.copy");

        NotificationGroupManager.getInstance().getNotificationGroup("doc-view.NotificationGroup")
                .createNotification(DocViewBundle.message("title"), info, NotificationType.INFORMATION)
                .setIcon(DocViewIcons.DOC_VIEW)
                .addAction(new BrowseNotificationAction(linkText, link))
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


    public static void startupNotification(@NotNull Project project) {

        NotificationGroupManager.getInstance().getNotificationGroup("doc-view.NotificationGroup")
                .createNotification(DocViewBundle.message("title"), DocViewBundle.message("notify.start"), NotificationType.INFORMATION)
                .setIcon(DocViewIcons.DOC_VIEW)
                .addAction(new BrowseNotificationAction("Star", DocViewBundle.message("github")))
                .addAction(new BrowseNotificationAction("Wiki", DocViewBundle.message("wiki")))
                .addAction(new BrowseNotificationAction("Feedback", DocViewBundle.message("issues")))
                .addAction(new BrowseNotificationAction("Other", DocViewBundle.message("toolkit")))
                .notify(project);

    }

}
