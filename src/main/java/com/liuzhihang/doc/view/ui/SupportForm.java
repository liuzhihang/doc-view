package com.liuzhihang.doc.view.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author liuzhihang
 * @date 2020/11/11 20:27
 */
public class SupportForm extends DialogWrapper {

    private JPanel rootPanel;
    private LinkLabel<String> starLinkLabel;
    private LinkLabel<String> reportLinkLabel;
    private LinkLabel<String> discussionsLinkLabel;
    private LinkLabel<String> websiteLinkLabel;

    public SupportForm() {
        super(null, true, DialogWrapper.IdeModalityType.PROJECT);

        init();

        starLinkLabel.setIcon(null);
        reportLinkLabel.setIcon(null);
        discussionsLinkLabel.setIcon(null);
        websiteLinkLabel.setIcon(null);

        rootPanel.setBorder(JBUI.Borders.empty(12, 15));
        rootPanel.setBackground(UIManager.getColor("TextArea.background"));
        starLinkLabel.setListener((source, data) -> BrowserUtil.browse(data), "https://github.com/liuzhihang/doc-view");
        reportLinkLabel.setListener((source, data) -> BrowserUtil.browse(data), "https://github.com/liuzhihang/doc-view/issues");
        discussionsLinkLabel.setListener((source, data) -> BrowserUtil.browse(data), "https://github.com/liuzhihang/doc-view/discussions");
        websiteLinkLabel.setListener((source, data) -> BrowserUtil.browse(data), "https://docview.liuzhihang.com");
    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return rootPanel;
    }
}
