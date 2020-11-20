package com.liuzhihang.doc.view.tool;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.dto.DocView;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TreeMouseListener extends MouseAdapter {


    private final SimpleTree tree;
    private final Project project;

    public TreeMouseListener(SimpleTree tree, Project project) {
        this.tree = tree;
        this.project = project;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
        if (selPath != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
            DocView question = (DocView) node.getUserObject();
            if (e.getButton() == 3) { //鼠标右键
                final ActionManager actionManager = ActionManager.getInstance();
                final ActionGroup actionGroup = (ActionGroup) actionManager.getAction("leetcode.NavigatorActionsMenu");
                if (actionGroup != null) {
                    actionManager.createActionPopupMenu("", actionGroup).getComponent().show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }
    }
}