package com.liuzhihang.doc.view.ui;

import com.intellij.find.editorHeaderActions.Utils;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
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
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.SettingsConfigurable;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.utils.ExportUtils;
import com.liuzhihang.doc.view.utils.NotificationUtils;
import com.liuzhihang.doc.view.utils.VelocityUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
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
    private JLabel docInfoLabel;
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
        // dialog 改成 popup
//        super(project, true, DialogWrapper.IdeModalityType.PROJECT);
        this.project = project;
        this.psiFile = psiFile;
        this.editor = editor;
        this.psiClass = psiClass;
        this.docMap = docMap;

        // dialog 改成 popup
//        init();

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
    }

    @NotNull
    @Contract("_, _, _, _, _ -> new")
    public static PreviewForm getInstance(@Nullable Project project, PsiFile psiFile, Editor editor, PsiClass psiClass, Map<String, DocView> docMap) {
        return new PreviewForm(project, psiFile, editor, psiClass, docMap);
    }

    public void popup() {
        // dialog 改成 popup, 第一个为根面板，第二个为焦点面板
        ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(rootJPanel, viewPane);
        JBPopup popup = popupBuilder
                .setProject(project)
                // 上下文要给，否则窗口相关设置无效，默认值为 true
//                .setModalContext(false)
                .setResizable(true)
                .setTitle("如果没有标题，无法移动")
                .setMovable(true)
                .setAdText("此处可以在底部设置一些广告内容")
                // 是否在其他窗口打开时，关闭窗口，默认值 false
//                .setCancelOnOtherWindowOpen(true)
                // 是否可以使用ESC关闭窗口，默认值 true
//                .setCancelKeyEnabled(false)
                // 是否在外部点击时关闭窗口，可以通过设置此值来锁定窗口不消失 默认值 true
                .setCancelOnClickOutside(false)
                .setDimensionServiceKey(project, DOC_VIEW_POPUP, true)
                .setLocateWithinScreenBounds(false)
                // 点击窗口外部位置，并且当前窗口未固定，则关闭
                .setCancelOnMouseOutCallback(event -> event.getClickCount() > 0 && !myIsPinned.get())
                .createPopup();


        // Disposer.register(popup);

        popup.showCenteredInCurrentWindow(project);
    }


    private void initUI() {
        // dialog 改成 popup
//        setTitle("Doc View");

        GuiUtils.replaceJSplitPaneWithIDEASplitter(rootJPanel, true);
        // 边框
        rootJPanel.setBorder(JBUI.Borders.empty());
        leftScrollPane.setBorder(JBUI.Borders.empty());
        viewSplitPane.setBorder(JBUI.Borders.empty());
        previewEditorPane.setBorder(JBUI.Borders.empty());
        previewPane.setBorder(JBUI.Borders.empty());
        viewPane.setBorder(JBUI.Borders.empty());

        catalogList.setBackground(UIUtil.getTextFieldBackground());

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

//        group.add(new PinActiveTabAction());
        group.add(new AnAction(() -> myIsPinned.get() ? IdeBundle.message("action.unpin.tab") : IdeBundle.message("action.pin.tab"), AllIcons.General.Pin_tab) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                myIsPinned.set(!myIsPinned.get());
            }
            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);
                e.getPresentation().setText(myIsPinned.get() ? IdeBundle.message("action.unpin.tab") : IdeBundle.message("action.pin.tab"));
                e.getPresentation().setIcon(myIsPinned.get() ? null : AllIcons.General.Pin_tab);
            }
        });

        ActionToolbarImpl toolbar = (ActionToolbarImpl) ActionManager.getInstance()
                .createActionToolbar("DocViewRootToolbar", group, true);
        toolbar.setTargetComponent(rootToolPane);

        toolbar.setForceMinimumSize(true);
        toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        Utils.setSmallerFontForChildren(toolbar);
        toolbar.setNoGapMode();

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

        JBScrollPane templateScrollPane = new JBScrollPane(markdownEditor.getComponent());
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
        toolbar.setNoGapMode();

        previewEditorPane.setBackground(markdownEditor.getBackgroundColor());
        previewEditorPane.add(toolbar.getComponent(), BorderLayout.WEST);
    }

    private void initEditorRightToolbar() {
        DefaultActionGroup rightGroup = new DefaultActionGroup();

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
        toolbar.setNoGapMode();
        previewEditorPane.setBackground(markdownEditor.getBackgroundColor());
        previewEditorPane.add(toolbar.getComponent(), BorderLayout.EAST);

    }


    private void buildDoc() {

        catalogList.setListData(new Vector<>(docMap.keySet()));

        catalogList.addListSelectionListener(catalog -> {

            String selectedValue = catalogList.getSelectedValue();

            currentDocView = docMap.get(selectedValue);

            docInfoLabel.setText(currentDocView.getMethodFullName());

            // 将 docView 按照模版转换
            DocViewData docViewData = new DocViewData(currentDocView);
            if (currentDocView.getType().equalsIgnoreCase("Dubbo")) {
                currentMarkdownText = VelocityUtils.convert(TemplateSettings.getInstance(project).getDubboTemplate(), docViewData);
            } else {
                // 按照 Spring 模版
                currentMarkdownText = VelocityUtils.convert(TemplateSettings.getInstance(project).getSpringTemplate(), docViewData);
            }

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


    /*
            // dialog 改成 popup
    @Override
    protected @Nullable
    JComponent createCenterPanel() {
        return rootJPanel;
    }

    @NotNull
    @Override
    protected Action[] createActions() {

        // 禁用默认的按钮
        return new Action[]{};
    }
     */
}
