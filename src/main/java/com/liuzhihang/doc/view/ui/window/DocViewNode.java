package com.liuzhihang.doc.view.ui.window;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.CachingSimpleNode;
import com.intellij.ui.treeStructure.SimpleNode;
import com.liuzhihang.doc.view.dto.DocView;

import java.util.List;

/**
 * @author liuzhihang
 * @date 2022/4/5 16:05
 */
public abstract class DocViewNode extends CachingSimpleNode {

    protected DocViewNode(SimpleNode parentNode) {
        super(parentNode);

    }

    @Override
    protected abstract SimpleNode[] buildChildren();

    @Override
    public String getName() {
        return "Doc View";
    }

    public abstract List<DocView> docViewList();

    public abstract void updateNode(Project project);

    /**
     * 生成的文档保存路径
     *
     * @param project project
     * @return path
     */
    public abstract String docPath(Project project);

    /**
     * 生成的 .http 文件路径
     *
     * @param project project
     * @return path
     */
    public abstract String httpPath(Project project);

}
