package com.liuzhihang.doc.view.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ui.JBUI;

import javax.swing.*;

/**
 * @author liuzhihang
 * @date 2020/2/26 19:17
 */
public class SettingsForm {

    private JPanel rootPanel;
    private JPanel titlePanel;

    private LinkLabel<String> supportLinkLabel;


    public SettingsForm() {

        supportLinkLabel.setBorder(JBUI.Borders.emptyTop(20));

        supportLinkLabel.setIcon(AllIcons.Actions.Find);

        supportLinkLabel.setListener((source, data) -> new SupportForm().show(), null);


        titlePanel.setBorder(IdeBorderFactory.createTitledBorder("当前版本不支持配置!"));

    }

    public JPanel getRootPanel() {
        return rootPanel;
    }
}
