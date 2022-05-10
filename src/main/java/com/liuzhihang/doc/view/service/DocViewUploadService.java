package com.liuzhihang.doc.view.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.impl.ShowDocServiceImpl;
import com.liuzhihang.doc.view.service.impl.YApiServiceImpl;
import com.liuzhihang.doc.view.service.impl.YuQueServiceImpl;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 上传到 showDoc
 *
 * @author liuzhihang
 * @date 2021/7/27 12:00
 */
public interface DocViewUploadService {

    public static final String[] UPLOAD_OPTIONS = {"YApi", "ShowDoc", "语雀"};

    @NotNull
    static DocViewUploadService getInstance(@NotNull String name) {
        if (name.equals("YApi")) {
            // 上传到 yapi
            return ApplicationManager.getApplication().getService(YApiServiceImpl.class);
        } else if (name.equals("ShowDoc")) {
            // 上传到 ShowDoc
            return ApplicationManager.getApplication().getService(ShowDocServiceImpl.class);
        } else if (name.equals("语雀")) {
            // 上传到语雀
            return ApplicationManager.getApplication().getService(YuQueServiceImpl.class);
        }

        // 默认返回 YApi
        return ApplicationManager.getApplication().getService(YApiServiceImpl.class);
    }

    /**
     * 批量上传, 都是循环调用的单个上传接口
     *
     * @param project
     * @param docViewList
     */
    default void upload(@NotNull Project project, @NotNull List<DocView> docViewList) {

        if (!checkSettings(project)) {
            // 没有配置相关地址 token 等
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Doc View upload", true) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> ApplicationManager.getApplication().runReadAction(() -> {

                    for (DocView docView : docViewList) {
                        doUpload(project, docView);
                    }
                }));
            }
        });

    }

    /**
     * 上传到 showDoc
     *
     * @param project
     * @param docView
     */
    default void upload(@NotNull Project project, @NotNull DocView docView) {

        if (!checkSettings(project)) {
            // 没有配置相关地址 token 等
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Doc View upload", true) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> ApplicationManager.getApplication().runReadAction(() -> {
                    doUpload(project, docView);
                }));
            }
        });
    }

    ;


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
