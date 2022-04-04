package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.action.toolbar.AbstractToolbarUploadAction;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.service.DocViewUploadService;
import com.liuzhihang.doc.view.ui.window.DocViewWindowTreeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

/**
 * @author liuzhihang
 * @date 2021/10/23 19:55
 */
public class WindowUploadAction extends AbstractToolbarUploadAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        SimpleTree catalogTree = e.getData(DocViewDataKeys.WINDOW_CATALOG_TREE);
        JComponent toolbar = e.getData(DocViewDataKeys.WINDOW_TOOLBAR);

        if (catalogTree == null || project == null || toolbar == null) {
            return;
        }

        if (DocViewWindowTreeNode.ROOT.getChildCount() == 0) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.window.upload.empty"));
            return;
        }

        JBPopupFactory.getInstance()
                .createListPopup(new BaseListPopupStep<>(null, DocViewUploadService.UPLOAD_OPTIONS) {
                    @Override
                    public @NotNull String getTextFor(String value) {
                        return "Upload to " + value;
                    }

                    @Override
                    public @Nullable PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {

                        List<DocView> docViews = new LinkedList<>();

                        Enumeration<TreeNode> treeNodeEnumeration = DocViewWindowTreeNode.ROOT.breadthFirstEnumeration();

                        while (treeNodeEnumeration.hasMoreElements()) {
                            DocViewWindowTreeNode treeNode = (DocViewWindowTreeNode) treeNodeEnumeration.nextElement();
                            if (treeNode.isClassPath()) {
                                DocViewService service = DocViewService.getInstance(project, treeNode.getPsiClass());
                                if (service != null) {
                                    docViews.addAll(service.buildClassDoc(project, treeNode.getPsiClass()));
                                }
                            }

                        }

                        DocViewUploadService.getInstance(selectedValue).upload(project, docViews);

                        return FINAL_CHOICE;
                    }
                }).showUnderneathOf(toolbar);


    }


}
