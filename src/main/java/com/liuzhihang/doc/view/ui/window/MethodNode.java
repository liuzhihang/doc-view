package com.liuzhihang.doc.view.ui.window;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.SimpleTextAttributes;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.utils.CustomFileUtils;
import com.liuzhihang.doc.view.utils.DocViewUtils;

import java.awt.event.InputEvent;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 目录树上的一个节点
 *
 * @author liuzhihang
 * @date 2022/4/4
 */
public class MethodNode extends DocViewNode {

    private final PsiMethod psiMethod;
    private final PsiClass psiClass;
    private final String signature;
    private final String businessName;
    private final String methodName;

    protected MethodNode(SimpleNode aParent, PsiClass psiClass, PsiMethod psiMethod) {
        super(aParent);
        this.psiMethod = psiMethod;
        this.psiClass = psiClass;
        methodName = psiMethod.getName();
        signature = methodName + "(" + Arrays.stream(psiMethod.getParameterList().getParameters())
                .map(parameter -> parameter.getType().getCanonicalText()).collect(Collectors.joining(", ")) + ")";
        businessName = DocViewUtils.getName(psiMethod);
        getTemplatePresentation().setIcon(AllIcons.Nodes.Method);
        String methodDescription = DocViewUtils.getMethodDesc(psiMethod);
        String tooltip = signature;
        if (methodDescription != null && !methodDescription.isBlank()) {
            tooltip += " — " + methodDescription;
        }
        getTemplatePresentation().setTooltip(tooltip);
    }

    @Override
    protected void doUpdate(PresentationData presentation) {
        presentation.clearText();
        presentation.addText(methodName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        if (businessName != null && !businessName.isBlank() && !businessName.equals(methodName)) {
            presentation.addText("  " + businessName, SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
    }

    @Override
    protected SimpleNode[] buildChildren() {
        return new SimpleNode[0];
    }

    @Override
    public String getName() {
        return businessName;
    }

    @Override
    public String getSearchText() {
        return signature + (businessName == null || businessName.isBlank() || businessName.equals(methodName)
                ? "" : " " + businessName);
    }

    @Override
    public List<DocView> docViewList() {
        DocViewService service = DocViewService.getInstance(psiClass.getProject(), psiClass);
        return Collections.singletonList(service.buildClassMethodDoc(psiClass, psiMethod));

    }

    @Override
    public void updateNode(Project project) {

    }

    @Override
    public String docPath(Project project) {

        ClassNode classNode = (ClassNode) getParent();

        return classNode.docPath(project) + "/" + DocViewUtils.getName(psiMethod) + ".md";

    }

    @Override
    public String httpPath(Project project) {

        ClassNode classNode = (ClassNode) getParent();

        return classNode.httpPath(project) + "/" + DocViewUtils.getName(psiMethod) + ".http";
    }

    @Override
    public void handleSelection(SimpleTree tree) {
        super.handleSelection(tree);
    }

    /**
     * 被双击或者 Enter 时, 双击打开文档
     *
     * @param tree
     * @param inputEvent
     */
    @Override
    public void handleDoubleClickOrEnter(SimpleTree tree, InputEvent inputEvent) {
        CustomFileUtils.openMd(psiClass.getProject(), this);
    }

    public PsiMethod getPsiMethod() {
        return psiMethod;
    }

    public PsiClass getPsiClass() {
        return psiClass;
    }

}
