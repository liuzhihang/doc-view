package com.liuzhihang.doc.view.action.toolbar.window.catalog;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.action.AbstractUploadAction;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.service.DocViewUploadService;
import com.liuzhihang.doc.view.ui.window.DocViewWindowTreeNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author liuzhihang
 * @date 2021/10/23 23:13
 */
public abstract class AbstractCatalogUploadAction extends AbstractUploadAction {

    protected Project project;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        // 获取当前project对象
        project = e.getData(PlatformDataKeys.PROJECT);
        SimpleTree simpleTree = e.getData(DocViewDataKeys.WINDOW_CATALOG_TREE);

        if (simpleTree == null || project == null) {
            return;
        }
        checkSettings();

        if (simpleTree.getLastSelectedPathComponent() instanceof DocViewWindowTreeNode) {
            DocViewWindowTreeNode node = (DocViewWindowTreeNode) simpleTree.getLastSelectedPathComponent();
            // 上传到 ShowDoc
            DocViewUploadService uploadService = uploadService();

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
