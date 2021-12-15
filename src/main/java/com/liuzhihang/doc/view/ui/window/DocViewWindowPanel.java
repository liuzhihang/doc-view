package com.liuzhihang.doc.view.ui.window;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

        catalogTree.setCellRenderer(new ColoredTreeCellRenderer() {
            @Override
            public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

                if (value instanceof DocViewWindowTreeNode) {
                    DocViewWindowTreeNode node = (DocViewWindowTreeNode) value;
                    append(node.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                }

            }
        });

        catalogTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                if (e.getButton() == MouseEvent.BUTTON3) {
                    final ActionManager actionManager = ActionManager.getInstance();
                    final ActionGroup actionGroup = (ActionGroup) actionManager.getAction("liuzhihang.doc.tool.window.catalog.action");
                    if (actionGroup != null) {
                        actionManager.createActionPopupMenu("", actionGroup).getComponent().show(e.getComponent(), e.getX(), e.getY());
                    }
                } else if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {

                    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Doc View", false) {
                        @Override
                        public void run(@NotNull ProgressIndicator progressIndicator) {
                            SimpleTree simpleTree = (SimpleTree) e.getSource();
                            if (simpleTree.getLastSelectedPathComponent() instanceof DocViewWindowTreeNode) {
                                DocViewWindowTreeNode node = (DocViewWindowTreeNode) simpleTree.getLastSelectedPathComponent();
                                open(node);
                            }
                        }
                    });

                }


            }
        });


    }

    private void open(DocViewWindowTreeNode node) {

        if (node.isClassPath()) {
            return;
        }


        DocViewService service = DocViewService.getInstance(project, node.getPsiClass());

        if (service == null) {
            return;
        }

        File file = new File(node.getTempFilePath());

        if (!file.exists()) {

            String markdownText = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
                DocView docView = service.buildClassMethodDoc(project, node.getPsiClass(), node.getPsiMethod());
                return DocViewData.markdownText(project, docView);
            });

            try {
                FileUtil.writeToFile(file, markdownText);
            } catch (Exception ex) {
                DocViewNotification.notifyError(project, DocViewBundle.message("notify.spring.error.method"));
            }
        }

        openMarkdownDoc(file);
    }


    private void openMarkdownDoc(File file) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);

            if (vf == null) {
                return;
            }

            OpenFileDescriptor descriptor = new OpenFileDescriptor(project, vf);
            FileEditorManager.getInstance(project).openTextEditor(descriptor, false);
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
