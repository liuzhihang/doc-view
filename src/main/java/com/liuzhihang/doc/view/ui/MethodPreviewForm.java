package com.liuzhihang.doc.view.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.utils.SpringDocUtils;
import com.liuzhihang.doc.view.utils.ExportUtils;
import com.liuzhihang.doc.view.utils.NotificationUtils;
import org.intellij.plugins.markdown.ui.preview.MarkdownUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

/**
 * @author liuzhihang
 * @date 2020/11/15 14:46
 */
public class MethodPreviewForm extends DialogWrapper {
    private JPanel rootJPanel;
    private JTextPane textPane;
    private JScrollPane scrollPane;


    private Action copyAction;
    private Action uploadAction;
    private Action exportAction;

    private Project project;
    private PsiFile psiFile;
    private Editor editor;
    private PsiClass psiClass;
    private DocView docView;

    private String currentMarkdownText;

    public MethodPreviewForm(@Nullable Project project, PsiFile psiFile, Editor editor, PsiClass psiClass, DocView docView) {
        super(project, true, DialogWrapper.IdeModalityType.PROJECT);
        this.project = project;
        this.psiFile = psiFile;
        this.editor = editor;
        this.psiClass = psiClass;
        this.docView = docView;


        init();
        // UI调整
        initUI();
        // 生成文档
        buildDoc();

    }

    private void initUI() {
        setTitle("Doc View");

        scrollPane.setBorder(null);

        textPane.setBackground(UIUtil.getTextFieldBackground());
        textPane.setEditable(false);

        UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, textPane);
        textPane.setBorder(JBUI.Borders.emptyLeft(7));


    }

    private void buildDoc() {

        // 将 docView 对象转换为 markdown 文本
        currentMarkdownText = SpringDocUtils.convertMarkdownText(docView);

        String html = MarkdownUtil.INSTANCE.generateMarkdownHtml(psiFile.getVirtualFile(), currentMarkdownText, project);

        textPane.setText(html);
        textPane.setCaretPosition(0);


    }

    @NotNull
    @Override
    protected Action[] createActions() {

        myHelpAction.setEnabled(true);

        return new Action[]{getOKAction(), getCopyAction(), getExportAction(), getHelpAction()};
    }

    private Action getExportAction() {

        return exportAction;
    }


    @Override
    protected void createDefaultActions() {
        super.createDefaultActions();
        copyAction = new CopyAction();
        exportAction = new ExportAction();
    }

    @NotNull
    protected Action getCopyAction() {
        return copyAction;
    }


    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return rootJPanel;
    }

    @Override
    protected void doHelpAction() {

        BrowserUtil.browse("https://docview.liuzhihang.com");

    }

    /**
     * Copy 按钮的监听
     * 主要功能为: 复制 Markdown 到剪贴板, 并提示.
     */
    protected class CopyAction extends DialogWrapperAction {

        protected CopyAction() {
            super("Copy");
        }

        @Override
        protected void doAction(ActionEvent e) {

            StringSelection selection = new StringSelection(currentMarkdownText);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);

            NotificationUtils.infoNotify("复制 Markdown 到剪贴板成功!", project);
        }
    }

    /**
     * Export 按钮的监听
     * 主要功能为: 导出功能
     */
    protected class ExportAction extends DialogWrapperAction {

        protected ExportAction() {
            super("Export");
        }

        @Override
        protected void doAction(ActionEvent e) {

            ExportUtils.exportMarkdown(project, docView.getName(), currentMarkdownText);

        }
    }
}
