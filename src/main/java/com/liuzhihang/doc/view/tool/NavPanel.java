package com.liuzhihang.doc.view.tool;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.dto.DocView;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;

/**
 * 侧边工具栏
 *
 * @author lvgorice@gmail.com
 * @version 1.0
 * @date 2020-11-20
 */
public class NavPanel extends SimpleToolWindowPanel {

    private final JBScrollPane contentScrollPanel;
    private final SimpleTree tree;

    public NavPanel(Project project) {
        super(Boolean.TRUE, Boolean.TRUE);
        final ActionManager actionManager = ActionManager.getInstance();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new DocView(project.getName()));
        tree = new SimpleTree(new DefaultTreeModel(root)) {

            private final JTextPane myPane = new JTextPane();

            {
                myPane.setOpaque(false);

            }

            @Override
            protected void paintComponent(Graphics g) {
                try {
                    super.paintComponent(g);
                } catch (Exception e) {
                    return;
                }


                DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
                if (!root.isLeaf()) {
                    return;
                }

                myPane.setFont(getFont());
                myPane.setBackground(getBackground());
                myPane.setForeground(getForeground());
                Rectangle bounds = getBounds();
                myPane.setBounds(0, 0, bounds.width - 10, bounds.height);

                Graphics g2 = g.create(bounds.x + 10, bounds.y + 20, bounds.width, bounds.height);
                try {
                    myPane.paint(g2);
                } finally {
                    g2.dispose();
                }
            }
        };
        tree.getEmptyText().clear();
        //tree.setRowHeight(21);
        tree.setOpaque(false);
        tree.setCellRenderer(new CustomTreeCellRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setRootVisible(false);
        tree.addMouseListener(new TreeMouseListener(tree, project));
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                System.out.println("数展开了");
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                System.out.println("合起来了");
            }
        });


        ActionToolbar actionToolbar = actionManager.createActionToolbar("docview Toolbar",
                (DefaultActionGroup) actionManager.getAction("docview.NavigatorActionsToolbar"),
                true);

        actionToolbar.setTargetComponent(tree);
        setToolbar(actionToolbar.getComponent());

        SimpleToolWindowPanel treePanel = new SimpleToolWindowPanel(Boolean.TRUE, Boolean.TRUE);

        JPanel groupPanel = new JPanel();
        groupPanel.setLayout(new BoxLayout(groupPanel, BoxLayout.Y_AXIS));
        contentScrollPanel = new JBScrollPane(tree, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        groupPanel.add(contentScrollPanel);

        treePanel.setContent(groupPanel);
        setContent(treePanel);

    }
}
