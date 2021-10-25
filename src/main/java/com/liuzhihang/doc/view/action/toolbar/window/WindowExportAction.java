package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.ui.window.DocViewWindowTreeNode;
import com.liuzhihang.doc.view.utils.DialogUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreeNode;
import java.io.File;
import java.util.Enumeration;
import java.util.List;

/**
 * @author liuzhihang
 * @date 2021/10/23 19:55
 */
public class WindowExportAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        SimpleTree catalogTree = e.getData(DocViewDataKeys.WINDOW_CATALOG_TREE);

        if (catalogTree == null || project == null) {
            return;
        }
        DocViewWindowTreeNode root = DocViewWindowTreeNode.ROOT;

        if (root.getChildCount() == 0) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.window.export.empty"));
            return;
        }

        // 选择路径
        FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        fileChooserDescriptor.setForcedToUseIdeaFileChooser(true);
        VirtualFile chooser = FileChooser.chooseFile(fileChooserDescriptor, project, null);
        if (chooser == null) {
            return;
        }
        String path = chooser.getPath();

        // 导出到一个文件中
        File file = new File(path + "/DocView.md");

        // 文件已存在，选择是否覆盖导出。
        if (file.exists() && !DialogUtil.confirm(
                DocViewBundle.message("notify.export.file.exists"),
                DocViewBundle.message("notify.export.file.cover"))) {
            return;
        }
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Doc View export", true) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {

                Enumeration<TreeNode> treeNodeEnumeration = root.breadthFirstEnumeration();

                while (treeNodeEnumeration.hasMoreElements()) {
                    DocViewWindowTreeNode treeNode = (DocViewWindowTreeNode) treeNodeEnumeration.nextElement();
                    // 导出全部
                    if (treeNode.isClassPath()) {
                        DocViewService service = DocViewService.getInstance(project, treeNode.getPsiClass());
                        if (service != null) {
                            List<DocView> docViews = service.buildClassDoc(project, treeNode.getPsiClass());
                            for (DocView docView : docViews) {
                                try {
                                    FileUtil.writeToFile(file, DocViewData.markdownText(project, docView), true);
                                } catch (Exception ignored) {
                                }
                            }

                        }
                    }

                }
            }
        });
    }
}
