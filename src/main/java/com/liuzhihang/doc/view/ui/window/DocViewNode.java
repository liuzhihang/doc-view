package com.liuzhihang.doc.view.ui.window;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.CachingSimpleNode;
import com.intellij.ui.treeStructure.SimpleNode;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.utils.StorageUtils;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author liuzhihang
 * @date 2022/4/5 16:05
 */
public abstract class DocViewNode extends CachingSimpleNode {

    protected Path cachePath;

    protected DocViewNode(Project aProject, @Nullable NodeDescriptor aParentDescriptor) {
        super(aProject, aParentDescriptor);
        cachePath = Paths.get(StorageUtils.getConfigDir(aProject).toString());
    }

    @Override
    protected abstract SimpleNode[] buildChildren();

    @Override
    public String getName() {
        return "Doc View";
    }

    public abstract List<DocView> docViewList();

    public Path getCachePath() {
        return cachePath;
    }
}
