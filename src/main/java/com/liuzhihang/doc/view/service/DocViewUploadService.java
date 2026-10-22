package com.liuzhihang.doc.view.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.enums.UploadPlatform;
import com.liuzhihang.doc.view.service.impl.ShowDocServiceImpl;
import com.liuzhihang.doc.view.service.impl.YApiServiceImpl;
import com.liuzhihang.doc.view.service.impl.YuQueServiceImpl;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.utils.DocViewBackgroundTasks;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 上传到 showDoc
 *
 * @author liuzhihang
 * @date 2021/7/27 12:00
 */
public interface DocViewUploadService {

    public static final List<UploadPlatform> UPLOAD_OPTIONS = List.of(UploadPlatform.values());

    @NotNull
    static DocViewUploadService getInstance(@NotNull UploadPlatform platform) {
        return switch (platform) {
            case YAPI -> ApplicationManager.getApplication().getService(YApiServiceImpl.class);
            case SHOW_DOC -> ApplicationManager.getApplication().getService(ShowDocServiceImpl.class);
            case YUQUE -> ApplicationManager.getApplication().getService(YuQueServiceImpl.class);
        };
    }

    /**
     * 批量上传, 都是循环调用的单个上传接口
     *
     * @param project
     * @param docViewList
     */
    default void upload(@NotNull Project project, @NotNull List<DocView> docViewList) {
        DocViewBackgroundTasks.runTask(project, DocViewBundle.message("task.docview.upload"), true, indicator -> {
            if (project.isDisposed()) throw new ProcessCanceledException();
            if (!checkSettings(project)) {
                return;
            }
            for (DocView docView : docViewList) {
                indicator.checkCanceled();
                if (project.isDisposed()) throw new ProcessCanceledException();
                doUpload(project, docView);
            }
        }, throwable -> DocViewNotification.notifyError(project, DocViewBundle.message("notify.integration.upload.error")));

    }

    /**
     * 上传到 showDoc
     *
     * @param project
     * @param docView
     */
    default void upload(@NotNull Project project, @NotNull DocView docView) {
        DocViewBackgroundTasks.runTask(project, DocViewBundle.message("task.docview.upload"), true,
                indicator -> {
                    if (project.isDisposed()) throw new ProcessCanceledException();
                    if (checkSettings(project)) {
                        doUpload(project, docView);
                    }
                },
                throwable -> DocViewNotification.notifyError(project, DocViewBundle.message("notify.integration.upload.error")));
    }

    /**
     * 检查配置
     *
     * @param project
     * @return
     */
    boolean checkSettings(@NotNull Project project);

    /**
     * 请求上传
     *
     * @param project
     * @param docView
     */
    void doUpload(@NotNull Project project, @NotNull DocView docView);
}
