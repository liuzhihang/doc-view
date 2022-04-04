package com.liuzhihang.doc.view.ui.window;

import com.intellij.psi.PsiMethod;
import com.intellij.ui.treeStructure.CachingSimpleNode;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.utils.DocViewUtils;

import java.awt.event.InputEvent;

/**
 * 目录树上的一个节点
 *
 * @author liuzhihang
 * @date 2022/4/4
 */
public class MethodNode extends CachingSimpleNode {

    private final PsiMethod psiMethod;

    protected MethodNode(SimpleNode aParent, PsiMethod psiMethod) {
        super(aParent);
        this.psiMethod = psiMethod;
        getTemplatePresentation().setIcon(null);
        getTemplatePresentation().setTooltip(DocViewUtils.getMethodDesc(psiMethod));
    }

    @Override
    protected SimpleNode[] buildChildren() {
        return new SimpleNode[0];
    }

    @Override
    public String getName() {
        return DocViewUtils.getName(psiMethod);
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