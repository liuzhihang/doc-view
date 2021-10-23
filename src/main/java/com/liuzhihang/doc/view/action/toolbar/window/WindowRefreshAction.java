package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.codeInsight.completion.AllClassesGetter;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.treeStructure.Tree;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * @author liuzhihang
 * @date 2021/10/23 19:55
 */
public class WindowRefreshAction extends AnAction {

    private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Doc View");


    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        Tree catalogTree = e.getData(DocViewDataKeys.WINDOW_CATALOG_TREE);

        if (catalogTree == null || project == null) {
            return;
        }

        catalogTree.setModel(new DefaultTreeModel(rootNode));

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Doc View", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setText("Directory refreshing");
                refreshCatalog(project);
            }
        });

    }

    private void refreshCatalog(@NotNull Project project) {

        ApplicationManager.getApplication().runReadAction(() -> AllClassesGetter.processJavaClasses(
                new PlainPrefixMatcher(""),
                project,
                GlobalSearchScope.projectScope(project),
                psiClass -> {

                    if (!DocViewUtils.isDocViewClass(psiClass)) {
                        return true;
                    }
                    // 是 Doc View 类
                    DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(psiClass);

                    PsiMethod[] methods = psiClass.getMethods();

                    for (PsiMethod method : methods) {
                        if (DocViewUtils.isDocViewMethod(method)) {
                            classNode.add(new DefaultMutableTreeNode(method));
                        }
                    }

                    // 没有方法 则不展示
                    if (classNode.getChildCount() > 0) {
                        rootNode.add(classNode);
                    }

                    return true;
                }
        ));
    }
}
