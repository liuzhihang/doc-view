package com.liuzhihang.doc.view.tool;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.service.impl.ToolWindowServiceImpl;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * toolwindow 树模块点击事件
 *
 * @author lvgorice@gmail.com
 * @version 1.0
 * @date 2020-11-20
 */
public class TreeMouseListener extends MouseAdapter {


    private final SimpleTree tree;
    private final Project project;

    public TreeMouseListener(SimpleTree tree, Project project) {
        this.tree = tree;
        this.project = project;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        TreePath selectionPath = tree.getSelectionPath();
        if (selectionPath != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            // 双击树
            if (e.getClickCount() == 2) {
                String nodeName = node.getUserObject().toString();
                if (nodeName.endsWith(".java")) {
                    // @see com.liuzhihang.doc.view.service.impl.ToolWindowServiceImpl#fileFilter
                    String key = node.getParent().toString()
                            .replace(File.separator, ".")
                            .replace("/", ".")
                            .replace("\\", ".") + nodeName;
                    VirtualFile virtualFile = ToolWindowServiceImpl.virtualFileCache.get(key);
                    ApplicationManager.getApplication().invokeAndWait(() -> {
                        OpenFileDescriptor descriptor = new OpenFileDescriptor(project, virtualFile);
                        FileEditorManager.getInstance(project).openTextEditor(descriptor, false);
                    });
                }
            }

            if (e.getButton() == 3) { //鼠标右键
                final ActionManager actionManager = ActionManager.getInstance();
                final ActionGroup actionGroup = (ActionGroup) actionManager.getAction("doc-view.NavigatorActionsMenu");
                if (actionGroup != null) {
                    actionManager.createActionPopupMenu("", actionGroup).getComponent().show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }
    }
}