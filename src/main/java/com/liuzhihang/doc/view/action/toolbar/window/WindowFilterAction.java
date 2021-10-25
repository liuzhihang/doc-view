package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.config.WindowSettings;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.ui.WindowFilterForm;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author liuzhihang
 * @date 2021/10/23 19:55
 */
public class WindowFilterAction extends ToggleAction {

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        SimpleTree catalogTree = e.getData(DocViewDataKeys.WINDOW_CATALOG_TREE);
        JComponent toolbar = e.getData(DocViewDataKeys.WINDOW_TOOLBAR);

        if (catalogTree == null || project == null || toolbar == null) {
            return false;
        }

        WindowSettings windowSettings = WindowSettings.getInstance(project);

        String scope = windowSettings.getScope();

        return StringUtils.isNotBlank(scope);
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        SimpleTree catalogTree = e.getData(DocViewDataKeys.WINDOW_CATALOG_TREE);
        JComponent toolbar = e.getData(DocViewDataKeys.WINDOW_TOOLBAR);

        if (catalogTree == null || project == null || toolbar == null) {
            return;
        }

        // 过滤
        JPanel rootPane = new WindowFilterForm(project).getRootPane();
        JBPopupFactory.getInstance()
                .createComponentPopupBuilder(rootPane, rootPane)
                .createPopup()
                .showUnderneathOf(toolbar);

    }
}
