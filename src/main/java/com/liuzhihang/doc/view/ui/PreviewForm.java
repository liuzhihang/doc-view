package com.liuzhihang.doc.view.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.ui.GuiUtils;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import com.liuzhihang.doc.view.utils.NotificationUtils;
import org.intellij.plugins.markdown.ui.preview.MarkdownUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Vector;

/**
 * @author liuzhihang
 * @date 2020/2/26 19:40
 */
public class PreviewForm extends DialogWrapper {
    private JPanel rootJPanel;
    private JPanel viewPanel;
    private JSplitPane viewSplitPane;
    private JScrollPane leftScrollPane;
    private JScrollPane rightScrollPane;
    private JList<String> catalogList;
    private JTextPane textPane;

    private Action copyAction;
    private Action uploadAction;
    private Action exportAction;

    private Project project;
    private PsiFile psiFile;
    private Editor editor;
    private PsiClass psiClass;
    private Map<String, DocView> docMap;

    private String currentMarkdownText;

    public PreviewForm(@Nullable Project project, PsiFile psiFile, Editor editor, PsiClass psiClass, Map<String, DocView> docMap) {
        super(project, true, DialogWrapper.IdeModalityType.PROJECT);
        this.project = project;
        this.psiFile = psiFile;
        this.editor = editor;
        this.psiClass = psiClass;
        this.docMap = docMap;


        init();
        // UI调整
        initUI();
        // 生成文档
        buildDoc();
        catalogList.setSelectedIndex(0);

        // if (docMap.size() == 1) {
        //     viewSplitPane.setDividerLocation(0);
        // }

    }

    private void initUI() {
        setTitle("Doc View");

        GuiUtils.replaceJSplitPaneWithIDEASplitter(viewPanel, true);

        // 边框
        leftScrollPane.setBorder(null);
        rightScrollPane.setBorder(null);

        textPane.setBackground(UIUtil.getTextFieldBackground());
        textPane.setEditable(false);
        // textPane.addHyperlinkListener(new PluginManagerMain.MyHyperlinkListener());

        UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, textPane);

        textPane.setBorder(JBUI.Borders.emptyLeft(7));
        catalogList.setBackground(UIUtil.getTextFieldBackground());
        textPane.setBackground(UIUtil.getTextFieldBackground());

    }

    private void buildDoc() {

        catalogList.setListData(new Vector<>(docMap.keySet()));

        catalogList.addListSelectionListener(catalog -> {

            String selectedValue = catalogList.getSelectedValue();

            DocView docView = docMap.get(selectedValue);

            // 将 docView 对象转换为 markdown 文本
            currentMarkdownText = DocViewUtils.convertMarkdownText(docView);

            String html = MarkdownUtil.INSTANCE.generateMarkdownHtml(psiFile.getVirtualFile(), currentMarkdownText, project);

            textPane.setText(html);
            textPane.setCaretPosition(0);

        });

    }

    @Override
    protected void createDefaultActions() {
        super.createDefaultActions();
        copyAction = new CopyAction();
    }

    @NotNull
    @Override
    protected Action[] createActions() {

        myHelpAction.setEnabled(true);

        return new Action[]{getOKAction(), getCopyAction(), getHelpAction()};
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

        BrowserUtil.browse("https://liuzhihang.com/2020/11/01/doc-view.html");

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

}
