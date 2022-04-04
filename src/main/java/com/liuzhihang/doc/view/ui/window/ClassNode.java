package com.liuzhihang.doc.view.ui.window;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.treeStructure.CachingSimpleNode;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.utils.DocViewUtils;

import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 目录树上的一个节点
 *
 * @author liuzhihang
 * @date 2022/4/4
 */
public class ClassNode extends CachingSimpleNode {

    private final List<MethodNode> methodNodes = new ArrayList<>();
    private final PsiClass psiClass;

    protected ClassNode(SimpleNode aParent, PsiClass psiClass) {
        super(aParent);
        this.psiClass = psiClass;
        getTemplatePresentation().setIcon(null);
        getTemplatePresentation().setTooltip(DocViewUtils.getTitle(psiClass));
        doUpdate();
    }


    @Override
    protected void doUpdate() {

        PsiMethod[] methods = psiClass.getMethods();

        for (PsiMethod psiMethod : methods) {
            if (DocViewUtils.isDocViewMethod(psiMethod)) {
                MethodNode methodNode = new MethodNode(this, psiMethod);
                methodNodes.add(methodNode);
            }
        }

    }

    @Override
    protected SimpleNode[] buildChildren() {
        return methodNodes.toArray(new SimpleNode[0]);
    }

    @Override
    public String getName() {
        return DocViewUtils.getTitle(psiClass);
    }

    @Override
    public void handleSelection(SimpleTree tree) {
        super.handleSelection(tree);
    }

    /**
     * 被双击或者 Enter 时
     *
     * @param tree
     * @param inputEvent
     */
    @Override
    public void handleDoubleClickOrEnter(SimpleTree tree, InputEvent inputEvent) {
        super.handleDoubleClickOrEnter(tree, inputEvent);
    }

}