package com.liuzhihang.doc.view.ui.window;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.utils.CustomFileUtils;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import com.liuzhihang.doc.view.utils.DubboPsiUtils;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;
import icons.DocViewIcons;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.util.*;

/**
 * 目录树上的一个节点
 *
 * @author liuzhihang
 * @date 2022/4/4
 */
public class MethodNode extends DocViewNode {

    private final static Map<String, Icon> ICON_MAP = new HashMap<>() {{
        put("GET", DocViewIcons.GET);
        put("POST", DocViewIcons.POST);
        put("PUT", DocViewIcons.PUT);
        put("DELETE", DocViewIcons.DELETE);
        put("DUBBO", DocViewIcons.DUBBO);

    }};

    private final PsiMethod psiMethod;
    private final PsiClass psiClass;

    protected MethodNode(SimpleNode aParent, PsiClass psiClass, PsiMethod psiMethod) {
        super(aParent);
        this.psiMethod = psiMethod;
        this.psiClass = psiClass;

        getTemplatePresentation().setIcon(null);
        getTemplatePresentation().setTooltip(DocViewUtils.getMethodDesc(psiMethod));
    }


    private Icon methodIcon() {

        if (SpringPsiUtils.isSpringClass(psiClass) && SpringPsiUtils.isSpringMethod(psiMethod)) {
            return ICON_MAP.get(SpringPsiUtils.method(psiMethod));
        }
        if (DubboPsiUtils.isDubboClass(psiClass) && DubboPsiUtils.isDubboMethod(psiMethod)) {
            return ICON_MAP.get("DUBBO");
        }

        return null;
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
        DocViewService service = DocViewService.getInstance(psiClass.getProject(), psiClass);
        if (service != null) {
            return Collections.singletonList(service.buildClassMethodDoc(psiClass.getProject(), psiClass, psiMethod));
        }

        return new LinkedList<>();
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