package com.liuzhihang.doc.view.ui.window;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.utils.DocViewUtils;

import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 目录树上的一个节点
 *
 * @author liuzhihang
 * @date 2022/4/4
 */
public class ClassNode extends DocViewNode {

    private final List<MethodNode> methodNodes = new ArrayList<>();
    private final PsiClass         psiClass;

    protected ClassNode(SimpleNode aParent, PsiClass psiClass) {
        super(aParent);
        this.psiClass = psiClass;

        getTemplatePresentation().setIcon(psiClass.isInterface() ? AllIcons.Nodes.Interface : AllIcons.Nodes.Class);
        getTemplatePresentation().setTooltip(DocViewUtils.getTitle(psiClass));
        updateNode(psiClass.getProject());
    }

    public void updateNode(Project project) {

        PsiMethod[] methods = psiClass.getMethods();

        for (PsiMethod psiMethod : methods) {
            if (DocViewUtils.isDocViewMethod(psiMethod)) {
                MethodNode methodNode = new MethodNode(this, psiClass, psiMethod);
                methodNodes.add(methodNode);
            }
        }
        update();
    }

    @Override
    public String docPath(Project project) {

        ModuleNode moduleNode = (ModuleNode) getParent();

        return moduleNode.docPath(project) + "/" + DocViewUtils.getTitle(psiClass);
    }

    @Override
    public String httpPath(Project project) {

        ModuleNode moduleNode = (ModuleNode) getParent();

        return moduleNode.httpPath(project) + "/" + DocViewUtils.getTitle(psiClass);
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
    public List<DocView> docViewList() {
        return methodNodes.stream().map(MethodNode::docViewList).flatMap(Collection::stream).collect(Collectors.toList());
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