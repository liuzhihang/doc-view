package com.liuzhihang.doc.view.ui.window;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.ui.treeStructure.SimpleNode;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.utils.DubboPsiUtils;
import com.liuzhihang.doc.view.utils.FeignPsiUtil;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liuzhihang
 * @date 2022/4/4 17:06
 */
public class ModuleNode extends DocViewNode {

    private final List<ClassNode> classNodes = new ArrayList<>();
    private final Module          module;

    protected ModuleNode(SimpleNode aParent, Module module) {
        super(aParent);
        this.module = module;

        getTemplatePresentation().setIcon(AllIcons.Nodes.Module);
        getTemplatePresentation().setPresentableText(getName());
        updateNode(module.getProject());
    }

    public void updateNode(Project project) {
        cleanUpCache();
        classNodes.clear();

        List<PsiClass> psiClasses = new LinkedList<>();

        if (Settings.getInstance(project).getIncludeNormalInterface()) {
            // 包含普通接口则扫描所有接口
            ProgressManager.checkCanceled();
            Collection<PsiClass> candidates = AllClassesSearch.search(GlobalSearchScope.moduleScope(module), project).findAll();
            for (PsiClass candidate : candidates) {
                ProgressManager.checkCanceled();
                if (candidate.isInterface()) {
                    psiClasses.add(candidate);
                }
            }
        } else {
            ProgressManager.checkCanceled();
            psiClasses.addAll(DubboPsiUtils.findDocViewFromModule(module));
            ProgressManager.checkCanceled();
            psiClasses.addAll(FeignPsiUtil.findDocViewFromModule(module));
        }

        ProgressManager.checkCanceled();
        psiClasses.addAll(SpringPsiUtils.findDocViewFromModule(module));

        List<ClassOrder> orderedClasses = new ArrayList<>(psiClasses.size());
        for (PsiClass psiClass : psiClasses) {
            ProgressManager.checkCanceled();
            String qualifiedName = psiClass.getQualifiedName();
            PsiFile containingFile = psiClass.getContainingFile();
            VirtualFile virtualFile = containingFile == null ? null : containingFile.getVirtualFile();
            String sourcePath = virtualFile != null ? virtualFile.getPath()
                    : containingFile == null ? "" : containingFile.getName();
            orderedClasses.add(new ClassOrder(psiClass, qualifiedName == null ? "" : qualifiedName,
                    sourcePath, psiClass.getTextOffset()));
        }
        orderedClasses.sort(Comparator.comparing(ClassOrder::qualifiedName)
                .thenComparing(ClassOrder::sourcePath)
                .thenComparingInt(ClassOrder::sourceOffset));

        for (ClassOrder classOrder : orderedClasses) {
            ProgressManager.checkCanceled();
            ClassNode classNode = new ClassNode(this, classOrder.psiClass());
            classNodes.add(classNode);
        }
        update();
    }

    private record ClassOrder(PsiClass psiClass, String qualifiedName, String sourcePath, int sourceOffset) {
    }

    @Override
    public String docPath(Project project) {

        RootNode rootNode = (RootNode) getParent();

        return rootNode.docPath(project) + "/" + module.getName();
    }

    @Override
    public String httpPath(Project project) {

        RootNode rootNode = (RootNode) getParent();

        return rootNode.httpPath(project) + "/" + module.getName();
    }

    @Override
    protected SimpleNode[] buildChildren() {
        return classNodes.toArray(new SimpleNode[0]);
    }

    @Override
    public @NlsSafe String getName() {
        return module.getName();
    }

    @Override
    public List<DocView> docViewList() {
        return classNodes.stream().map(ClassNode::docViewList).flatMap(Collection::stream).collect(Collectors.toList());
    }
}
