package com.liuzhihang.doc.view.tool;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.ToolWindowService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

/**
 * 侧边工具栏
 *
 * @author lvgorice@gmail.com
 * @version 1.0
 * @date 2020-11-20
 */
public class DocViewToolWindow extends SimpleToolWindowPanel {

    public static final DataKey<JTree> DOC_VIEW_TREE = DataKey.create("DOC_VIEW_TREE");
    public static final DataKey<JBScrollPane> DOC_VIEW_SCROLL = DataKey.create("DOC_VIEW_SCROLL");
    private final JBScrollPane contentScrollPanel;
    private final SimpleTree tree;

    public DocViewToolWindow(Project project) {
        super(Boolean.TRUE, Boolean.TRUE);
        final ActionManager actionManager = ActionManager.getInstance();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new DocView(project.getName()));
        tree = new SimpleTree(new DefaultTreeModel(root));
        tree.setRowHeight(21);
        tree.setOpaque(false);
        // 多选
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.setRootVisible(false);
        tree.addMouseListener(new TreeMouseListener(tree, project));
        ApplicationManager.getApplication().invokeAndWait(
                () -> ToolWindowService.getInstance().loadDocViewTree(project, tree));

        // 侧边栏工具条
        ActionToolbar actionToolbar = actionManager.createActionToolbar("doc-view-toolbar",
                (DefaultActionGroup) actionManager.getAction("doc-view.NavigatorActionsToolbar"),
                true);
        actionToolbar.setTargetComponent(tree);
        // 设置侧边栏工具
        setToolbar(actionToolbar.getComponent());

        // 树结构面板区域
        SimpleToolWindowPanel treePanel = new SimpleToolWindowPanel(Boolean.TRUE, Boolean.TRUE);
        JPanel groupPanel = new JPanel();
        groupPanel.setLayout(new BoxLayout(groupPanel, BoxLayout.Y_AXIS));
        // 设置滚动面板参数
        contentScrollPanel = new JBScrollPane(tree, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        groupPanel.add(contentScrollPanel);
        treePanel.setContent(groupPanel);

        // 设置侧边栏内容
        setContent(treePanel);
    }

    @Override
    public Object getData(@NotNull String dataId) {
        if (DOC_VIEW_TREE.is(dataId)) {
            return tree;
        }

        if (DOC_VIEW_SCROLL.is(dataId)) {
            return contentScrollPanel;
        }
        return super.getData(dataId);
    }
}
