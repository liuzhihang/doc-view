package com.liuzhihang.doc.view.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author liuzhihang
 * @date 2020/2/26 19:17
 */
public class SettingsForm extends DialogWrapper {

    private JPanel rootPanel;
    private LinkLabel<String> supportLinkLabel;
    private JPanel domainPanel;


    public SettingsForm() {
        super(null);

        supportLinkLabel.setBorder(JBUI.Borders.emptyTop(20));

        supportLinkLabel.setIcon(AllIcons.Actions.Find);

        supportLinkLabel.setListener((source, data) -> new SupportForm().show(), null);


    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return rootPanel;
    }

    private void createUIComponents() {
        // place custom component creation code here
        domainPanel.setBorder(IdeBorderFactory.createTitledBorder("Domain"));
    }
}
