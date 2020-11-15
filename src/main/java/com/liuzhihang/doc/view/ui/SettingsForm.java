package com.liuzhihang.doc.view.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author liuzhihang
 * @date 2020/2/26 19:17
 */
public class SettingsForm {

    private JPanel rootPanel;
    private LinkLabel<String> supportLinkLabel;
    private JPanel domainPanel;


    public SettingsForm() {

        supportLinkLabel.setBorder(JBUI.Borders.emptyTop(20));

        supportLinkLabel.setIcon(AllIcons.Actions.Find);

        supportLinkLabel.setListener((source, data) -> new SupportForm().show(), null);


        domainPanel.setBorder(IdeBorderFactory.createTitledBorder("当前版本不支持配置!"));

    }

    public JPanel getRootPanel() {
        return rootPanel;
    }
}
