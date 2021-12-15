package com.liuzhihang.doc.view.ui.window;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import lombok.Getter;
import lombok.Setter;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * @author liuzhihang
 * @date 2021/10/23 17:05
 */
@Getter
@Setter
public class DocViewWindowTreeNode extends DefaultMutableTreeNode {

    public static final DocViewWindowTreeNode ROOT = new DocViewWindowTreeNode();

    private boolean classPath = false;

    /**
     * 接口类型
     * Spring Dubbo Feign
     * <p>
     * POST GET PUT DELETE
     */
    private String type;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 临时文件路径
     */
    private String tempFilePath;

    /**
     * 类信息
     */
    private PsiClass psiClass;

    /**
     * 方法名
     */
    private PsiMethod psiMethod;

    /**
     * 请求方式
     */
    private String method;

    public DocViewWindowTreeNode() {
        this.name = "Doc View";
    }

    public DocViewWindowTreeNode(PsiClass psiClass) {
        this.classPath = true;
        this.name = DocViewUtils.getTitle(psiClass);
        this.psiClass = psiClass;
    }

    public DocViewWindowTreeNode(PsiClass psiClass, PsiMethod psiMethod, String name, String method, String tempFilePath) {
        this.classPath = false;
        this.psiClass = psiClass;
        this.psiMethod = psiMethod;
        this.name = name;
        this.tempFilePath = tempFilePath;
        this.method = method;
    }

}
