package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.action.toolbar.AbstractUploadAction;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.service.DocViewUploadService;
import com.liuzhihang.doc.view.service.impl.ShowDocServiceImpl;
import com.liuzhihang.doc.view.service.impl.YApiServiceImpl;
import com.liuzhihang.doc.view.ui.window.DocViewWindowTreeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.Enumeration;
import java.util.List;

/**
 * @author liuzhihang
 * @date 2021/10/23 19:55
 */
public class WindowUploadAction extends AbstractUploadAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        SimpleTree catalogTree = e.getData(DocViewDataKeys.WINDOW_CATALOG_TREE);

        if (catalogTree == null || project == null) {
            return;
        }

        JBPopupFactory.getInstance()
                .createListPopup(new BaseListPopupStep<>(null, "YApi", "ShowDoc") {
                    @Override
                    public @NotNull String getTextFor(String value) {
                        return "Upload to " + value;
                    }

                    @Override
                    public @Nullable PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {

                        if (selectedValue.equals("YApi")) {
                            // 上传到 yapi
                            checkYApiSettings(project);
                            upload(project, YApiServiceImpl.class);

                        } else if (selectedValue.equals("ShowDoc")) {
                            // 上传到 ShowDoc
                            checkShowDocSettings(project);
                            upload(project, ShowDocServiceImpl.class);
                        }


                        return FINAL_CHOICE;
                    }
                }).showInScreenCoordinates(catalogTree, new Point());

    }

    private void upload(@NotNull Project project, @NotNull Class<? extends DocViewUploadService> uploadServiceClass) {

        DocViewUploadService uploadService = ServiceManager.getService(uploadServiceClass);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Doc View upload", true) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {

                DocViewWindowTreeNode root = DocViewWindowTreeNode.ROOT;

                Enumeration<TreeNode> treeNodeEnumeration = root.breadthFirstEnumeration();

                while (treeNodeEnumeration.hasMoreElements()) {
                    DocViewWindowTreeNode treeNode = (DocViewWindowTreeNode) treeNodeEnumeration.nextElement();
                    // 导出全部
                    if (treeNode.isClassPath()) {
                        DocViewService service = DocViewService.getInstance(project, treeNode.getPsiClass());
                        if (service != null) {
                            List<DocView> docViews = service.buildClassDoc(project, treeNode.getPsiClass());
                            uploadService.upload(project, docViews);
                        }
                    }

                }
            }
        });
    }

}
