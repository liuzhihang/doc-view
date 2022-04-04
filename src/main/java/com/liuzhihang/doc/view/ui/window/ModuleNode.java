package com.liuzhihang.doc.view.ui.window;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.PsiClass;
import com.intellij.ui.treeStructure.CachingSimpleNode;
import com.intellij.ui.treeStructure.SimpleNode;
import com.liuzhihang.doc.view.utils.DubboPsiUtils;
import com.liuzhihang.doc.view.utils.FeignPsiUtil;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author liuzhihang
 * @date 2022/4/4 17:06
 */
public class ModuleNode extends CachingSimpleNode {

    private final List<ClassNode> classNodes = new ArrayList<>();
    private final Module module;

    protected ModuleNode(SimpleNode aParent, Module module) {
        super(aParent);
        this.module = module;
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
}
