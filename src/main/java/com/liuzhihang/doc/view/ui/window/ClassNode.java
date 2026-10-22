package com.liuzhihang.doc.view.ui.window;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.SimpleTextAttributes;
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
    private String codeName;
    private String businessName;

    protected ClassNode(SimpleNode aParent, PsiClass psiClass) {
        super(aParent);
        this.psiClass = psiClass;

        getTemplatePresentation().setIcon(psiClass.isInterface() ? AllIcons.Nodes.Interface : AllIcons.Nodes.Class);
        getTemplatePresentation().setTooltip(DocViewUtils.getTitle(psiClass));
        updateNode(psiClass.getProject());
    }

    public void updateNode(Project project) {
        cleanUpCache();
        methodNodes.clear();
        codeName = psiClass.getName();
        businessName = DocViewUtils.getTitle(psiClass);
        PsiMethod[] methods = psiClass.getMethods();

        for (PsiMethod psiMethod : methods) {
            ProgressManager.checkCanceled();
            if (DocViewUtils.isDocViewMethod(psiMethod)) {
                MethodNode methodNode = new MethodNode(this, psiClass, psiMethod);
                methodNodes.add(methodNode);
            }
        }
        update();
    }

    @Override
    protected void doUpdate(PresentationData presentation) {
        presentation.clearText();
        presentation.addText(codeName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        if (businessName != null && !businessName.isBlank() && !businessName.equals(codeName)) {
            presentation.addText("  " + businessName, SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
    }

    public PsiClass getPsiClass() {
        return psiClass;
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
        return businessName;
    }

    @Override
    public String getSearchText() {
        return codeName + (businessName == null || businessName.isBlank() || businessName.equals(codeName)
                ? "" : " " + businessName);
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
