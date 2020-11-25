package com.liuzhihang.doc.view.service;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.service.impl.ToolWindowServiceImpl;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * DocViewDataService
 *
 * @author lvgorice@gmail.com
 * @version 1.0
 * @date 2020/11/23
 */
public interface ToolWindowService {
    @Nullable
    static ToolWindowService getInstance() {
        return ServiceManager.getService(ToolWindowServiceImpl.class);
    }

    /**
     * 加载 doc-view tree
     *
     * @param project 当前项目
     */
    void loadDocViewTree(Project project, JTree tree);
}
