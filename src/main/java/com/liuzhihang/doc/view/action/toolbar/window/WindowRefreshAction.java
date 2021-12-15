package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.codeInsight.completion.AllClassesGetter;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.config.WindowSettings;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.ui.window.DocViewWindowTreeNode;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultTreeModel;

/**
 * @author liuzhihang
 * @date 2021/10/23 19:55
 */
public class WindowRefreshAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        SimpleTree catalogTree = e.getData(DocViewDataKeys.WINDOW_CATALOG_TREE);

        if (catalogTree == null || project == null) {
            return;
        }

        WindowSettings windowSettings = WindowSettings.getInstance(project);

        // 先移除所有的子节点
        DocViewWindowTreeNode.ROOT.removeAllChildren();
        catalogTree.setModel(new DefaultTreeModel(DocViewWindowTreeNode.ROOT));

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Doc View", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {

                progressIndicator.setText("Directory refreshing");
                refreshCatalog(project, windowSettings, catalogTree);
            }
        });
    }

    private void refreshCatalog(@NotNull Project project, @NotNull WindowSettings windowSettings, SimpleTree catalogTree) {

        String scope = windowSettings.getScope();

        GlobalSearchScope searchScope;

        if (StringUtils.isBlank(scope)) {
            searchScope = GlobalSearchScope.projectScope(project);
        } else {
            Module module = ModuleManager.getInstance(project).findModuleByName(scope);
            if (module == null) {
                searchScope = GlobalSearchScope.projectScope(project);
            } else {
                searchScope = GlobalSearchScope.moduleScope(module);
            }
        }

        AllClassesGetter.processJavaClasses(
                new PlainPrefixMatcher(""),
                project,
                searchScope,
                psiClass -> ApplicationManager.getApplication().runReadAction(new Computable<>() {
                    @Override
                    public Boolean compute() {
                        if (!DocViewUtils.isDocViewClass(psiClass)) {
                            return true;
                        }

                        if (!windowSettings.isIncludeInterface() && psiClass.isInterface()) {
                            return true;
                        }

                        // 是 Doc View 类
                        DocViewWindowTreeNode classNode = new DocViewWindowTreeNode(psiClass);

                        PsiMethod[] methods = psiClass.getMethods();

                        for (PsiMethod psiMethod : methods) {
                            if (DocViewUtils.isDocViewMethod(psiMethod)) {

                                String name = DocViewUtils.getName(psiMethod);
                                String tempFilePath = project.getBasePath() + "/.idea/doc-view/temp/" + classNode.getName() + "/" + name + ".md";

                                String method = "";

                                if (!psiClass.isInterface()) {
                                    method = SpringPsiUtils.getMethod(psiMethod);
                                }

                                classNode.add(new DocViewWindowTreeNode(psiClass, psiMethod, name, method, tempFilePath));
                            }
                        }

                        // 没有方法 则不展示
                        if (classNode.getChildCount() > 0) {
                            DocViewWindowTreeNode.ROOT.add(classNode);
                        }

                        return true;
                    }
                })
        );
        ((DefaultTreeModel) catalogTree.getModel()).reload();
    }
}
