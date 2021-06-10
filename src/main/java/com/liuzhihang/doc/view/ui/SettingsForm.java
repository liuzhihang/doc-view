package com.liuzhihang.doc.view.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * @author liuzhihang
 * @date 2020/2/26 19:17
 */
public class SettingsForm {

    private JPanel rootPanel;
    private JPanel docNamePanel;

    private LinkLabel<String> supportLinkLabel;
    private JPanel docDescPanel;
    private JPanel paramRequirePanel;
    private JPanel docTitlePanel;
    private JBList docTitleJBList;


    public SettingsForm() {

        supportLinkLabel.setBorder(JBUI.Borders.emptyTop(20));

        supportLinkLabel.setIcon(AllIcons.Actions.Find);

        supportLinkLabel.setListener((source, data) -> new SupportForm().show(), null);

        docNamePanel.setBorder(IdeBorderFactory.createTitledBorder("接口名称"));
        docDescPanel.setBorder(IdeBorderFactory.createTitledBorder("接口描述"));
        paramRequirePanel.setBorder(IdeBorderFactory.createTitledBorder("是否必填"));


        initDocTitle();

    }

    private void initDocTitle() {


        docTitlePanel.add(ToolbarDecorator.createDecorator(docTitleJBList)
                .setMoveUpAction(anActionButton -> {

                })
                .setMoveDownAction(anActionButton -> {

                })
                .createPanel(), BorderLayout.CENTER);
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }
}
