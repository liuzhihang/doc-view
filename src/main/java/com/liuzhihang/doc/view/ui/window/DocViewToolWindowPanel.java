package com.liuzhihang.doc.view.ui.window;

import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Objects;

/**
 * @author liuzhihang
 * @date 2021/10/22 16:25
 */
@Slf4j
public class DocViewToolWindowPanel extends SimpleToolWindowPanel implements DataProvider {

    private final SimpleTree catalogTree = new SimpleTree();
    private final Project project;

    public DocViewToolWindowPanel(@NotNull Project project) {
        super(Boolean.TRUE, Boolean.TRUE);

        this.project = project;
        initToolbar();
        initCatalogTree();
        setContent(catalogTree);
    }


    private void initToolbar() {

        final ActionManager actionManager = ActionManager.getInstance();
        ActionToolbar actionToolbar = actionManager.createActionToolbar("liuzhihang.doc.tool.window.toolbar",
                (DefaultActionGroup) actionManager.getAction("liuzhihang.doc.tool.window.toolbar.action"),
                true);
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

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();
                if (userObject instanceof PsiClass) {
                    PsiClass psiClass = (PsiClass) userObject;
                    append(Objects.requireNonNull(psiClass.getName()));
                }

                if (userObject instanceof PsiMethod) {
                    PsiMethod psiMethod = (PsiMethod) userObject;
                    append(psiMethod.getName());
                }

            }
        });


    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {

        if (DocViewDataKeys.WINDOW_CATALOG_TREE.is(dataId)) {
            return catalogTree;
        }


        return super.getData(dataId);
    }
}
