package com.liuzhihang.doc.view.ui.window;

import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;

/**
 * @author liuzhihang
 * @date 2021/10/22 16:25
 */
@Slf4j
public class DocViewWindowPanel extends SimpleToolWindowPanel implements DataProvider {

    private final SimpleTree catalogTree = new SimpleTree();
    private final Project project;

    public DocViewWindowPanel(@NotNull Project project) {
        super(Boolean.TRUE, Boolean.TRUE);

        this.project = project;
        initToolbar();
        initCatalogTree();
        setContent(catalogTree);
        new TreeSpeedSearch(catalogTree);
    }


    private void initToolbar() {

        final ActionManager actionManager = ActionManager.getInstance();
        ActionToolbar actionToolbar = actionManager.createActionToolbar("liuzhihang.doc.tool.window.toolbar",
                (DefaultActionGroup) actionManager.getAction("liuzhihang.doc.tool.window.toolbar.action"),
                true);
        actionToolbar.setTargetComponent(getToolbar());
        setToolbar(actionToolbar.getComponent());

    }

    /**
     * 初始化目录树
     */
    private void initCatalogTree() {

        catalogTree.setRootVisible(false);

        catalogTree.setCellRenderer(new NodeRenderer() {
            @Override
            public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

                if (value instanceof DocViewWindowTreeNode) {
                    DocViewWindowTreeNode node = (DocViewWindowTreeNode) value;
                    append(node.getName());
                }

            }
        });

        catalogTree.addTreeSelectionListener(e -> {
            SimpleTree simpleTree = (SimpleTree) e.getSource();
            if (simpleTree.getLastSelectedPathComponent() instanceof DocViewWindowTreeNode) {
                DocViewWindowTreeNode node = (DocViewWindowTreeNode) simpleTree.getLastSelectedPathComponent();

                if (!node.isClassPath()) {

                    DocViewService service = DocViewService.getInstance(project, node.getPsiClass());

                    if (service == null) {
                        return;
                    }

                    DocView docView = service.buildClassMethodDoc(project, node.getPsiClass(), node.getPsiMethod());

                    String markdownText = DocViewData.markdownText(project, docView);
                    String basePath = project.getBasePath();
                    File file = new File(basePath + "/.idea/doc-view/temp/" + docView.getName() + ".md");

                    try {
                        FileUtil.writeToFile(file, markdownText);
                    } catch (Exception ex) {
                        DocViewNotification.notifyError(project, DocViewBundle.message("notify.spring.error.method"));
                    }

                    ApplicationManager.getApplication().invokeAndWait(() -> {
                        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
                        OpenFileDescriptor descriptor = new OpenFileDescriptor(project, vf);
                        FileEditorManager.getInstance(project).openTextEditor(descriptor, false);
                    });
                }

            }
        });


    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {

        if (DocViewDataKeys.WINDOW_CATALOG_TREE.is(dataId)) {
            return catalogTree;
        }

        if (DocViewDataKeys.WINDOW_TOOLBAR.is(dataId)) {
            return getToolbar();
        }

        return super.getData(dataId);
    }
}
