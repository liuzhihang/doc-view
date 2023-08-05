package com.liuzhihang.doc.view.action;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBSplitter;

import javax.swing.*;
import java.awt.*;

/**
 * @author liuzhihang
 * @since 2023/8/5 18:18
 */
public class JBSplitterDialogWrapper extends DialogWrapper {

    private JBSplitter splitter;

    public JBSplitterDialogWrapper() {
        super(true);
        init();
        setTitle("JBSplitter");
    }

    @Override
    protected JComponent createCenterPanel() {
        splitter = new JBSplitter(false, 0.0f);
        JPanel firstPanel = new JPanel();
        firstPanel.add(new JLabel("First Panel"));
        firstPanel.setBorder(BorderFactory.createTitledBorder("First Panel"));
        splitter.setFirstComponent(firstPanel);

        // JPanel secondPanel = new JPanel();
        // secondPanel.add(new JLabel("Second Panel"));
        // secondPanel.setBorder(BorderFactory.createTitledBorder("Second Panel"));
        // splitter.setSecondComponent(secondPanel);


        splitter.setShowDividerControls(true);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(splitter, BorderLayout.CENTER);
        return panel;
    }
}
