package com.liuzhihang.doc.view.ui.window;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.PsiClass;
import com.intellij.ui.treeStructure.SimpleNode;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.utils.DubboPsiUtils;
import com.liuzhihang.doc.view.utils.FeignPsiUtil;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liuzhihang
 * @date 2022/4/4 17:06
 */
public class ModuleNode extends DocViewNode {

    private final List<ClassNode> classNodes = new ArrayList<>();
    private final Module module;

    protected ModuleNode(SimpleNode aParent, Module module) {
        super(module.getProject(), aParent);
        this.module = module;

        getTemplatePresentation().setIcon(AllIcons.Nodes.Module);
        getTemplatePresentation().setPresentableText(getName());
        doUpdate();
    }

    @Override
    protected void doUpdate() {
        cleanUpCache();
        classNodes.clear();

        List<PsiClass> psiClasses = new LinkedList<>();
        psiClasses.addAll(SpringPsiUtils.findDocViewFromModule(module));
        psiClasses.addAll(DubboPsiUtils.findDocViewFromModule(module));
        psiClasses.addAll(FeignPsiUtil.findDocViewFromModule(module));

        for (PsiClass psiClass : psiClasses) {
            ClassNode classNode = new ClassNode(this, psiClass);
            classNodes.add(classNode);
        }

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
