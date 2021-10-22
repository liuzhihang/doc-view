package com.liuzhihang.doc.view.ui;

import com.intellij.codeInsight.completion.AllClassesGetter;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.find.editorHeaderActions.Utils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.SearchTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

/**
 * @author liuzhihang
 * @date 2021/10/22 16:25
 */
public class DocViewToolWindowForm extends DialogWrapper {

    private final Project project;
    private final ModuleManager moduleManager;
    private JPanel rootPanel;
    private JSplitPane viewSplitPane;
    private JComboBox<String> moduleComboBox;

    private JPanel leftToolbarPane;
    private SearchTextField searchTextField;
    private JTree catalogTree;

    public DocViewToolWindowForm(@NotNull Project project) {
        super(project, true, DialogWrapper.IdeModalityType.PROJECT);
        this.project = project;
        this.moduleManager = ModuleManager.getInstance(project);

        init();
        initUI();
        initLeftToolbarPane();
        initModule();
        initCatalogTree();

    }

    private void initUI() {
        // 边框
        rootPanel.setBorder(JBUI.Borders.empty());
        viewSplitPane.setBorder(JBUI.Borders.empty());
        leftToolbarPane.setBorder(JBUI.Borders.empty());
    }

    private void initLeftToolbarPane() {

        DefaultActionGroup group = new DefaultActionGroup();

        group.add(new AnAction("Run", "Run", AllIcons.Actions.RunAll) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

            }
        });

        group.add(new AnAction("Filter", "Filter", AllIcons.General.Filter) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

            }
        });

        group.addSeparator();
        group.add(new AnAction("Expand All", "Expand all", AllIcons.Actions.Expandall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

            }
        });

        group.add(new AnAction("Collapse All", "Collapse all", AllIcons.Actions.Collapseall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

            }
        });


        group.addSeparator();

        group.add(new AnAction("Pop View", "Pop-up window view", AllIcons.Actions.ShowHiddens) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

            }
        });

        group.add(new AnAction("Export All", "Export markdown", AllIcons.Ide.IncomingChangesOn) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

            }
        });

        group.add(new AnAction("Upload All", "Upload all To YApi", AllIcons.Ide.OutgoingChangesOn) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

            }
        });


        ActionToolbarImpl toolbar = (ActionToolbarImpl) ActionManager.getInstance()
                .createActionToolbar("DocViewToolWindowLeftToolbar", group, false);
        toolbar.setTargetComponent(leftToolbarPane);

        toolbar.setForceMinimumSize(true);
        toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        Utils.setSmallerFontForChildren(toolbar);

        leftToolbarPane.add(toolbar.getComponent(), BorderLayout.EAST);

    }


    private void initModule() {

        Module[] modules = moduleManager.getModules();

        for (Module module : modules) {
            moduleComboBox.addItem(module.getName());
        }

    }

    /**
     * 初始化目录树
     */
    private void initCatalogTree() {

        String moduleName = (String) moduleComboBox.getSelectedItem();

        if (moduleName == null) {
            return;
        }

        Module module = moduleManager.findModuleByName(moduleName);

        if (module == null) {
            return;
        }

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Doc View");
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        catalogTree.setModel(treeModel);


        AllClassesGetter.processJavaClasses(
                new PlainPrefixMatcher(""),
                project,
                GlobalSearchScope.moduleScope(module),
                psiClass -> {

                    rootNode.add(new DefaultMutableTreeNode(psiClass.getName()));

                    return true;
                }
        );


    }


    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return rootPanel;
    }
}
