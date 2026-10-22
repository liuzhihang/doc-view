package com.liuzhihang.doc.view.ui.window;

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.DocViewBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;

/**
 * @author liuzhihang
 * @date 2021/10/22 16:25
 */
public class DocViewWindowPanel extends SimpleToolWindowPanel implements Disposable {

    private volatile RootNode rootNode = new RootNode();

    private final Project project;
    private final SimpleTree catalogTree;
    private final ToolWindow toolWindow;
    private final StructureTreeModel<AbstractTreeStructure> treeModel;
    private final JLabel refreshStatus = new JBLabel();
    private volatile long refreshGeneration;
    private volatile boolean disposed;
    private volatile ProgressIndicator refreshIndicator;

    public DocViewWindowPanel(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        super(Boolean.TRUE, Boolean.TRUE);
        this.project = project;
        this.toolWindow = toolWindow;

        final ActionManager actionManager = ActionManager.getInstance();
        ActionToolbar actionToolbar = actionManager.createActionToolbar(ActionPlaces.TOOLWINDOW_TOOLBAR_BAR,
                (DefaultActionGroup) actionManager.getAction("liuzhihang.doc.tool.window.toolbar.action"),
                true);
        actionToolbar.setTargetComponent(this);
        setToolbar(actionToolbar.getComponent());

        treeModel = new StructureTreeModel<>(new SimpleTreeStructure() {
            @NotNull
            @Override
            public Object getRootElement() {
                return rootNode;
            }
        }, null, this);
        catalogTree = new SimpleTree(new AsyncTreeModel(treeModel, this));
        initCatalogTree();

        JPanel content = new JPanel(new BorderLayout());
        refreshStatus.setBorder(JBUI.Borders.empty(4, 8));
        content.add(refreshStatus, BorderLayout.NORTH);
        content.add(ScrollPaneFactory.createScrollPane(catalogTree), BorderLayout.CENTER);
        setContent(content);
        TreeSpeedSearch.installOn(catalogTree, false, path -> {
            DocViewNode node = TreeUtil.getLastUserObject(DocViewNode.class, path);
            return node == null ? "" : node.getSearchText();
        });
        updateCatalogTree();
    }

    /**
     * 初始化目录树
     */
    private void initCatalogTree() {
        catalogTree.setRootVisible(false);
        catalogTree.setShowsRootHandles(true);
        catalogTree.getEmptyText().clear();
        catalogTree.setBorder(BorderFactory.createEmptyBorder());
        catalogTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        PopupHandler.installPopupMenu(catalogTree, "liuzhihang.doc.tool.window.catalog.action", ActionPlaces.TOOLWINDOW_CONTENT);
    }

    public void updateCatalogTree() {
        if (disposed || project.isDisposed() || toolWindow.isDisposed()) return;
        long request = beginRefresh();
        DumbService.getInstance(project).smartInvokeLater(() -> {
            if (isCurrent(request)) doUpdateCatalogTree(request);
        });
    }

    private synchronized long beginRefresh() {
        long request = ++refreshGeneration;
        ProgressIndicator previous = refreshIndicator;
        refreshIndicator = null;
        if (previous != null) previous.cancel();
        setStatus(DocViewBundle.message(DumbService.isDumb(project)
                ? "catalog.status.indexing" : "catalog.status.searching"));
        return request;
    }

    private boolean isCurrent(long request) {
        return !disposed && !project.isDisposed() && !toolWindow.isDisposed() && request == refreshGeneration;
    }

    private synchronized boolean registerIndicator(long request, ProgressIndicator indicator) {
        if (!isCurrent(request)) {
            indicator.cancel();
            return false;
        }
        refreshIndicator = indicator;
        return true;
    }

    private void doUpdateCatalogTree(long request) {
        setStatus(DocViewBundle.message("catalog.status.searching"));
        ProgressManager.getInstance().run(new Task.Backgroundable(project,
                DocViewBundle.message("task.catalog.search"), true) {
            private RootNode result;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (!registerIndicator(request, indicator)) return;
                result = ApplicationManager.getApplication().runReadAction((Computable<RootNode>) () -> {
                    indicator.checkCanceled();
                    RootNode snapshot = new RootNode();
                    snapshot.updateNode(project);
                    indicator.checkCanceled();
                    return snapshot;
                });
            }

            @Override
            public void onSuccess() {
                completeRefresh(request, result, null, false);
            }

            @Override
            public void onCancel() {
                completeRefresh(request, null, null, true);
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                completeRefresh(request, null, error, false);
            }
        });
    }

    private void completeRefresh(long request, RootNode result, Throwable failure, boolean cancelled) {
        if (!isCurrent(request)) return;
        refreshIndicator = null;
        if (cancelled) {
            setStatus(DocViewBundle.message("catalog.status.cancelled"));
        } else if (failure != null || result == null) {
            setStatus(DocViewBundle.message("catalog.status.failed"));
        } else {
            rootNode = result;
            setStatus(result.getChildCount() == 0 ? DocViewBundle.message("catalog.status.empty") : "");
            treeModel.invalidateAsync();
        }
    }

    private void setStatus(String text) {
        refreshStatus.setText(text);
        refreshStatus.setVisible(!text.isEmpty());
        catalogTree.getEmptyText().setText(text);
    }

    static PsiElement sourceElement(SimpleNode node) {
        PsiElement element = node instanceof MethodNode method ? method.getPsiMethod()
                : node instanceof ClassNode type ? type.getPsiClass() : null;
        return element != null && element.isValid() ? element : null;
    }

    @Override
    public synchronized void dispose() {
        disposed = true;
        refreshGeneration++;
        ProgressIndicator indicator = refreshIndicator;
        refreshIndicator = null;
        if (indicator != null) indicator.cancel();
    }


    @Override
    public void uiDataSnapshot(@NotNull DataSink sink) {
        super.uiDataSnapshot(sink);
        sink.set(DocViewDataKeys.WINDOW_PANE, this);
        sink.set(DocViewDataKeys.WINDOW_ROOT_NODE, rootNode);
        sink.set(DocViewDataKeys.WINDOW_CATALOG_TREE, catalogTree);
        sink.set(DocViewDataKeys.WINDOW_TOOLBAR, getToolbar());
        SimpleNode selected = catalogTree.getSelectedNode();
        sink.lazy(CommonDataKeys.PSI_ELEMENT, () -> sourceElement(selected));
        sink.lazy(CommonDataKeys.NAVIGATABLE, () -> {
            PsiElement element = sourceElement(selected);
            return element instanceof Navigatable navigatable ? navigatable : null;
        });
    }
}
