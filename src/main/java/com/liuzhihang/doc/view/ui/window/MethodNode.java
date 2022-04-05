package com.liuzhihang.doc.view.ui.window;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.utils.DocViewUtils;

import java.awt.event.InputEvent;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 目录树上的一个节点
 *
 * @author liuzhihang
 * @date 2022/4/4
 */
public class MethodNode extends DocViewNode {

    private final PsiMethod psiMethod;
    private final PsiClass psiClass;


    protected MethodNode(SimpleNode aParent, PsiClass psiClass, PsiMethod psiMethod) {
        super(psiClass.getProject(), aParent);
        this.psiMethod = psiMethod;
        this.psiClass = psiClass;

        String tempFolderPath = Paths.get(super.cachePath.toString(), DocViewUtils.getTitle(psiClass)).toString();
        cachePath = Paths.get(tempFolderPath, DocViewUtils.getName(psiMethod) + ".md");

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
    public List<DocView> docViewList() {
        DocViewService service = DocViewService.getInstance(myProject, psiClass);
        if (service != null) {
            return Collections.singletonList(service.buildClassMethodDoc(myProject, psiClass, psiMethod));
        }

        return new LinkedList<>();
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

    public PsiMethod getPsiMethod() {
        return psiMethod;
    }

    public PsiClass getPsiClass() {
        return psiClass;
    }
}