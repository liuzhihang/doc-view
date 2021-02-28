package com.liuzhihang.doc.view.ui;

import com.intellij.find.editorHeaderActions.Utils;
import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.WindowMoveListener;
import com.intellij.ui.components.JBScrollBar;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.SettingsConfigurable;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.utils.ExportUtils;
import com.liuzhihang.doc.view.utils.NotificationUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author liuzhihang
 * @date 2020/2/26 19:40
 */
public class PreviewForm {

    @NonNls
    public static final String DOC_VIEW_POPUP = "com.intellij.docview.popup";
    private static final AtomicBoolean myIsPinned = new AtomicBoolean(false);

    private JPanel rootJPanel;
    private JSplitPane viewSplitPane;
    private JScrollPane leftScrollPane;
    private JPanel viewPane;
    private JList<String> catalogList;
    private JPanel previewPane;
    private JPanel rootToolPane;
    private JPanel previewEditorPane;
    private JLabel docNameLabel;

    private EditorEx markdownEditor;
    private Document markdownDocument = EditorFactory.getInstance().createDocument("");
    private Project project;
    private PsiFile psiFile;
    private Editor editor;
    private PsiClass psiClass;

    private Map<String, DocView> docMap;
    private String currentMarkdownText;
    private DocView currentDocView;

    public PreviewForm(@Nullable Project project, PsiFile psiFile, Editor editor, PsiClass psiClass, Map<String, DocView> docMap) {

        this.project = project;
        this.psiFile = psiFile;
        this.editor = editor;
        this.psiClass = psiClass;
        this.docMap = docMap;

        // UI调整
        initUI();
        initRootToolbar();
        // 右侧文档
        initMarkdownEditor();
        initEditorLeftToolbar();
        initEditorRightToolbar();

        // 生成文档
        buildDoc();
        catalogList.setSelectedIndex(0);

        addMouseListeners();
    }


    private void addMouseListeners() {
        WindowMoveListener windowMoveListener = new WindowMoveListener(rootJPanel);
        rootJPanel.addMouseListener(windowMoveListener);
        rootJPanel.addMouseMotionListener(windowMoveListener);

    }

    @NotNull
    @Contract("_, _, _, _, _ -> new")
    public static PreviewForm getInstance(@Nullable Project project, PsiFile psiFile, Editor editor, PsiClass psiClass, Map<String, DocView> docMap) {
        return new PreviewForm(project, psiFile, editor, psiClass, docMap);
    }

    public void popup() {

        // dialog 改成 popup, 第一个为根面板，第二个为焦点面板
        JBPopupFactory.getInstance().createComponentPopupBuilder(rootJPanel, previewEditorPane)
                .setProject(project)
                .setResizable(true)
                .setMovable(true)

                .setModalContext(false)
                .setRequestFocus(true)
                .setBelongsToGlobalPopupStack(true)
                .setDimensionServiceKey(null, DOC_VIEW_POPUP, true)
                .setLocateWithinScreenBounds(false)
                // 鼠标点击外部时是否取消弹窗 外部单击, 未处于 pin 状态则可关闭
                .setCancelOnMouseOutCallback(event -> event.getID() == MouseEvent.MOUSE_PRESSED && !myIsPinned.get())

                // 单击外部时取消弹窗
                .setCancelOnClickOutside(false)
                // 在其他窗口打开时取消
                .setCancelOnOtherWindowOpen(false)
                .setCancelOnWindowDeactivation(false)
                .createPopup()
                .showCenteredInCurrentWindow(project);
    }


    private void initUI() {

        GuiUtils.replaceJSplitPaneWithIDEASplitter(rootJPanel, true);
        // 边框
        rootJPanel.setBorder(JBUI.Borders.empty());
        leftScrollPane.setBorder(JBUI.Borders.emptyLeft(5));
        viewSplitPane.setBorder(JBUI.Borders.empty());
        previewEditorPane.setBorder(JBUI.Borders.empty());
        previewPane.setBorder(JBUI.Borders.empty());
        viewPane.setBorder(JBUI.Borders.empty());
        docNameLabel.setBorder(JBUI.Borders.emptyLeft(5));

        catalogList.setBackground(UIUtil.getTextFieldBackground());
        leftScrollPane.setBackground(UIUtil.getTextFieldBackground());

        // 设置滚动条, 总是隐藏


        JBScrollBar jbScrollBar = new JBScrollBar();
        jbScrollBar.setBackground(UIUtil.getTextFieldBackground());

        leftScrollPane.setHorizontalScrollBar(jbScrollBar);
        //
        // leftScrollPane.setPreferredSize(new Dimension(1,1));

    }

    private void initRootToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(new AnAction("Setting", "Doc view settings", AllIcons.General.GearPlain) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(), SettingsConfigurable.class);
            }
        });

        group.addSeparator();

        group.add(new ToggleAction("Pin", "Pin window", AllIcons.General.Pin_tab) {

            @Override
            public boolean isDumbAware() {
                return true;
            }

            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return myIsPinned.get();
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                myIsPinned.set(state);
            }
        });

        ActionToolbarImpl toolbar = (ActionToolbarImpl) ActionManager.getInstance()
                .createActionToolbar("DocViewRootToolbar", group, true);
        toolbar.setTargetComponent(rootToolPane);

        toolbar.setForceMinimumSize(true);
        toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        Utils.setSmallerFontForChildren(toolbar);

        rootToolPane.add(toolbar.getComponent(), BorderLayout.EAST);
    }


    private void initMarkdownEditor() {
        // 会使用 velocity 渲染模版
        FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension("md");

        final EditorHighlighter editorHighlighter =
                HighlighterFactory.createHighlighter(fileType, EditorColorsManager.getInstance().getGlobalScheme(), project);

        markdownEditor = (EditorEx) EditorFactory.getInstance().createEditor(markdownDocument, project, fileType, true);

        EditorSettings editorSettings = markdownEditor.getSettings();
        editorSettings.setAdditionalLinesCount(0);
        editorSettings.setAdditionalColumnsCount(0);
        editorSettings.setLineMarkerAreaShown(false);
        editorSettings.setLineNumbersShown(false);
        editorSettings.setVirtualSpace(false);
        editorSettings.setFoldingOutlineShown(false);

        editorSettings.setLanguageSupplier(() -> Language.findLanguageByID("Markdown"));

        markdownEditor.setHighlighter(editorHighlighter);
        markdownEditor.setBorder(JBUI.Borders.emptyLeft(5));

        JBScrollPane templateScrollPane = new JBScrollPane(markdownEditor.getComponent());

        JBScrollBar jbScrollBar = new JBScrollBar();
        jbScrollBar.setBackground(markdownEditor.getBackgroundColor());

        templateScrollPane.setHorizontalScrollBar(jbScrollBar);

        previewPane.add(templateScrollPane, BorderLayout.CENTER);
    }


    private void initEditorLeftToolbar() {

        DefaultActionGroup leftGroup = new DefaultActionGroup();

        leftGroup.add(new AnAction("Preview", "Preview markdown", AllIcons.Actions.Preview) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                NotificationUtils.infoNotify(DocViewBundle.message("notify.preview.success"), project);

            }
        });

        leftGroup.addSeparator();

        leftGroup.add(new AnAction("Editor", "Editor doc", AllIcons.Actions.Edit) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                NotificationUtils.infoNotify(DocViewBundle.message("notify.editor.success"), project);
            }
        });

        ActionToolbarImpl toolbar = (ActionToolbarImpl) ActionManager.getInstance()
                .createActionToolbar("DocViewEditorLeftToolbar", leftGroup, true);
        toolbar.setTargetComponent(previewEditorPane);
        toolbar.getComponent().setBackground(markdownEditor.getBackgroundColor());

        toolbar.setForceMinimumSize(true);
        toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        Utils.setSmallerFontForChildren(toolbar);

        previewEditorPane.setBackground(markdownEditor.getBackgroundColor());
        previewEditorPane.add(toolbar.getComponent(), BorderLayout.WEST);
    }

    private void initEditorRightToolbar() {
        DefaultActionGroup rightGroup = new DefaultActionGroup();

        rightGroup.add(new AnAction("Export All", "Export markdown", AllIcons.Nodes.ExtractedFolder) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                ExportUtils.allExportMarkdown(project, currentDocView.getClassName(), docMap);
            }
        });

        rightGroup.add(new AnAction("Export", "Export markdown", AllIcons.ToolbarDecorator.Export) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                ExportUtils.exportMarkdown(project, currentDocView.getName(), currentMarkdownText);
            }
        });

        rightGroup.add(new AnAction("Copy", "Copy to clipboard", AllIcons.Actions.Copy) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                StringSelection selection = new StringSelection(currentMarkdownText);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);

                NotificationUtils.infoNotify(DocViewBundle.message("notify.copy.success", currentDocView.getName()), project);
            }
        });

        // init toolbar
        ActionToolbarImpl toolbar = (ActionToolbarImpl) ActionManager.getInstance()
                .createActionToolbar("DocViewEditorRightToolbar", rightGroup, true);
        toolbar.setTargetComponent(previewEditorPane);
        toolbar.getComponent().setBackground(markdownEditor.getBackgroundColor());

        toolbar.setForceMinimumSize(true);
        toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        Utils.setSmallerFontForChildren(toolbar);

        previewEditorPane.setBackground(markdownEditor.getBackgroundColor());
        previewEditorPane.add(toolbar.getComponent(), BorderLayout.EAST);

    }


    private void buildDoc() {

        catalogList.setListData(new Vector<>(docMap.keySet()));

        catalogList.addListSelectionListener(catalog -> {

            String selectedValue = catalogList.getSelectedValue();

            currentDocView = docMap.get(selectedValue);

            docNameLabel.setText(currentDocView.getFullClassName());

            // 将 docView 按照模版转换
            currentMarkdownText = DocViewData.buildMarkdownText(project, currentDocView);

            WriteCommandAction.runWriteCommandAction(project, () -> markdownDocument.setText(currentMarkdownText));

            // String html = MarkdownUtil.INSTANCE.generateMarkdownHtml(psiFile.getVirtualFile(), currentMarkdownText, project);
            //
            // html = "<html><head></head>" + html + "</html>";
            //
            // markdownHtmlPanel.setHtml(html, 0);
            // textPane.setText(html);
            // textPane.setCaretPosition(0);

        });

    }
}
