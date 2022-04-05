package com.liuzhihang.doc.view.ui.window;

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;

/**
 * @author liuzhihang
 * @date 2021/10/22 16:25
 */
@Slf4j
public class DocViewWindowPanel extends SimpleToolWindowPanel implements DataProvider {

    private final Project project;
    private final RootNode rootNode;
    private final SimpleTree catalogTree;
    private final StructureTreeModel<AbstractTreeStructure> treeModel;

    public DocViewWindowPanel(@NotNull Project project) {
        super(Boolean.TRUE, Boolean.TRUE);
        this.project = project;
        this.rootNode = new RootNode(project);
        treeModel = new StructureTreeModel<>(new SimpleTreeStructure() {
            @NotNull
            @Override
            public Object getRootElement() {
                return rootNode;
            }
        }, null, project);
        catalogTree = new SimpleTree(new AsyncTreeModel(treeModel, project));

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
        catalogTree.setRootVisible(true);
        catalogTree.setShowsRootHandles(true);
        catalogTree.getEmptyText().clear();
        catalogTree.setBorder(BorderFactory.createEmptyBorder());
        catalogTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        setLayout(new BorderLayout());
        add(catalogTree, BorderLayout.CENTER);
        PopupHandler.installPopupMenu(catalogTree, "liuzhihang.doc.tool.window.catalog.action", ActionPlaces.TOOLWINDOW_CONTENT);
    }

    public void updateCatalogTree() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Doc View Searching") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                AppUIUtil.invokeOnEdt(rootNode::doUpdate);
            }
        });
    }


    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {

        if (DocViewDataKeys.WINDOW_PANE.is(dataId)) {
            return this;
        }
        if (DocViewDataKeys.WINDOW_ROOT_NODE.is(dataId)) {
            return rootNode;
        }
        if (DocViewDataKeys.WINDOW_CATALOG_TREE.is(dataId)) {
            return catalogTree;
        }

        if (DocViewDataKeys.WINDOW_TOOLBAR.is(dataId)) {
            return getToolbar();
        }
        return super.getData(dataId);
    }
}
