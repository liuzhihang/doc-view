package com.liuzhihang.doc.view.action.toolbar.window.catalog;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.action.toolbar.AbstractUploadAction;
import com.liuzhihang.doc.view.config.YApiSettings;
import com.liuzhihang.doc.view.config.YApiSettingsConfigurable;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.service.DocViewUploadService;
import com.liuzhihang.doc.view.service.impl.YApiServiceImpl;
import com.liuzhihang.doc.view.ui.window.DocViewWindowTreeNode;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author liuzhihang
 * @date 2021/10/23 19:55
 */
public class CatalogUploadYApiAction extends AbstractUploadAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        SimpleTree simpleTree = e.getData(DocViewDataKeys.WINDOW_CATALOG_TREE);

        if (simpleTree == null || project == null) {
            return;
        }

        YApiSettings apiSettings = YApiSettings.getInstance(project);

        if (StringUtils.isBlank(apiSettings.getUrl())
                || apiSettings.getProjectId() == null
                || StringUtils.isBlank(apiSettings.getToken())) {
            // 说明没有配置 YApi 上传地址, 跳转到配置页面
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.yapi.info.settings"));
            ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(), YApiSettingsConfigurable.class);
            return;
        }


        if (simpleTree.getLastSelectedPathComponent() instanceof DocViewWindowTreeNode) {
            DocViewWindowTreeNode node = (DocViewWindowTreeNode) simpleTree.getLastSelectedPathComponent();
            // 上传到 yapi
            DocViewUploadService uploadService = ServiceManager.getService(YApiServiceImpl.class);

            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Doc View upload", true) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {

                    ApplicationManager.getApplication().executeOnPooledThread(() -> ApplicationManager.getApplication().runReadAction(() -> {
                        // 判断是目录还是
                        DocViewService docViewService = DocViewService.getInstance(project, node.getPsiClass());

                        if (docViewService == null) {
                            return;
                        }

                        if (node.isClassPath()) {

                            List<DocView> docViews = docViewService.buildClassDoc(project, node.getPsiClass());
                            uploadService.upload(project, docViews);

                        } else {

                            DocView docView = docViewService.buildClassMethodDoc(project, node.getPsiClass(), node.getPsiMethod());
                            uploadService.upload(project, docView);
                        }

                    }));

                }
            });
        }

    }


}
