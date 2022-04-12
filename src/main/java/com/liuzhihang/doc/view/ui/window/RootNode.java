package com.liuzhihang.doc.view.ui.window;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleNode;
import com.liuzhihang.doc.view.dto.DocView;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 根目录
 *
 * @author liuzhihang
 * @date 2022/4/4 16:56
 */
public class RootNode extends DocViewNode {

    private final List<ModuleNode> moduleNodes = new ArrayList<>();

    private final Project project;

    public RootNode(@NotNull Project project) {
        super(project, null);
        this.project = project;
        getTemplatePresentation().setIcon(AllIcons.Nodes.ModuleGroup);
        getTemplatePresentation().setPresentableText(getName());
    }

    @Override
    protected void doUpdate() {
        cleanUpCache();
        moduleNodes.clear();

        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            ModuleNode moduleNode = new ModuleNode(this, module);
            if (moduleNode.getChildCount() > 0) {
                moduleNodes.add(moduleNode);
            }
        }
    }

    @Override
    protected SimpleNode[] buildChildren() {
        return moduleNodes.toArray(new SimpleNode[0]);
    }

    @Override
    public String getName() {
        return "Doc View";
    }

    public List<DocView> docViewList() {

        return moduleNodes.stream().map(ModuleNode::docViewList).flatMap(Collection::stream).collect(Collectors.toList());
    }
}
