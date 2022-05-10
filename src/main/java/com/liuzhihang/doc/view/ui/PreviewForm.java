package com.liuzhihang.doc.view.ui;

import com.intellij.find.editorHeaderActions.Utils;
import com.intellij.icons.AllIcons;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.WindowMoveListener;
import com.intellij.ui.components.JBScrollBar;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.config.SettingsConfigurable;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewUploadService;
import com.liuzhihang.doc.view.utils.ExportUtils;
import lombok.extern.slf4j.Slf4j;
import org.intellij.plugins.markdown.settings.MarkdownSettings;
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel;
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanelProvider;
import org.intellij.plugins.markdown.ui.preview.html.MarkdownUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author liuzhihang
 * @date 2020/2/26 19:40
 */
@Slf4j
public class PreviewForm {

    @NonNls
    public static final String DOC_VIEW_POPUP = "com.intellij.docview.popup";
    public static final AtomicBoolean myIsPinned = new AtomicBoolean(false);
    private static final AtomicBoolean previewIsHtml = new AtomicBoolean(false);

    private final Document markdownDocument = EditorFactory.getInstance().createDocument("");

    private final Project project;
    private final PsiFile psiFile;
    private final PsiClass psiClass;

    private final List<DocView> docViewList;
    private Map<String, DocView> docViewMap;

    private JPanel rootPanel;
    private JSplitPane viewSplitPane;
    private JScrollPane leftScrollPane;
    private JPanel viewPanel;
    private JList<String> catalogList;
    private JPanel menuToolbarPanel;

    private JPanel previewPanel;
    private JBScrollPane markdownSourceScrollPanel;
    private MarkdownHtmlPanel markdownHtmlPanel;

    private JPanel headToolbarPanel;
    private JPanel previewToolbarPanel;
    private JLabel docNameLabel;
    private EditorEx markdownEditor;
    private String currentMarkdownText;

    private DocView currentDocView;
    private JBPopup popup;

    public PreviewForm(@NotNull Project project, @NotNull PsiFile psiFile,
                       @NotNull PsiClass psiClass, @NotNull List<DocView> docViewList) {

        this.project = project;
        this.psiFile = psiFile;
        this.psiClass = psiClass;
        this.docViewList = docViewList;

        // UI调整
        initUI();
        initHeadToolbar();
        // 右侧文档
        initMarkdownSourceScrollPanel();
        initMarkdownHtmlPanel();
        initPreviewPanel();
        initPreviewLeftToolbar();
        initPreviewRightToolbar();
        initMenuToolbarPanelToolbar();

        // 生成文档
        buildDoc();

        catalogList.setSelectedIndex(0);

        addMouseListeners();
    }

    private void addMouseListeners() {
        WindowMoveListener windowMoveListener = new WindowMoveListener(rootPanel);
        rootPanel.addMouseListener(windowMoveListener);
        rootPanel.addMouseMotionListener(windowMoveListener);
        headToolbarPanel.addMouseListener(windowMoveListener);
        headToolbarPanel.addMouseMotionListener(windowMoveListener);

    }

    @NotNull
    @Contract("_, _, _, _ -> new")
    public static PreviewForm getInstance(@NotNull Project project, @NotNull PsiFile psiFile,
                                          @NotNull PsiClass psiClass,
                                          @NotNull List<DocView> docViewList) {
        return new PreviewForm(project, psiFile, psiClass, docViewList);
    }

    public void popup() {

        // dialog 改成 popup, 第一个为根面板，第二个为焦点面板
        popup = JBPopupFactory.getInstance().createComponentPopupBuilder(rootPanel, previewToolbarPanel)
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
                .createPopup();
        popup.showCenteredInCurrentWindow(project);
    }


    private void initUI() {

        GuiUtils.replaceJSplitPaneWithIDEASplitter(rootPanel, true);
        // 边框
        rootPanel.setBorder(JBUI.Borders.empty());
        leftScrollPane.setBorder(JBUI.Borders.emptyLeft(5));
        viewSplitPane.setBorder(JBUI.Borders.empty());
        previewToolbarPanel.setBorder(JBUI.Borders.empty());
        previewPanel.setBorder(JBUI.Borders.empty());
        viewPanel.setBorder(JBUI.Borders.empty());
        docNameLabel.setBorder(JBUI.Borders.emptyLeft(5));

        catalogList.setBackground(UIUtil.getTextFieldBackground());
        leftScrollPane.setBackground(UIUtil.getTextFieldBackground());

        // 设置滚动条, 总是隐藏

        JBScrollBar jbScrollBar = new JBScrollBar();
        jbScrollBar.setBackground(UIUtil.getTextFieldBackground());
        jbScrollBar.setAutoscrolls(true);
        leftScrollPane.setHorizontalScrollBar(jbScrollBar);
        //
        // leftScrollPane.setPreferredSize(new Dimension(1,1));

    }

    private void initHeadToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(new AnAction("Setting", "Doc view settings", AllIcons.General.GearPlain) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                popup.cancel();
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
        toolbar.setTargetComponent(headToolbarPanel);

        toolbar.setForceMinimumSize(true);
        toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        Utils.setSmallerFontForChildren(toolbar);

        headToolbarPanel.add(toolbar.getComponent(), BorderLayout.EAST);
    }


    private void initMarkdownSourceScrollPanel() {

        markdownEditor = (EditorEx) EditorFactory.getInstance().createViewer(markdownDocument, project, EditorKind.UNTYPED);

        FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension("md");
        markdownEditor.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType));

        EditorSettings editorSettings = markdownEditor.getSettings();
        editorSettings.setAdditionalLinesCount(0);
        editorSettings.setAdditionalColumnsCount(0);
        editorSettings.setLineMarkerAreaShown(false);
        editorSettings.setLineNumbersShown(false);
        editorSettings.setVirtualSpace(false);
        editorSettings.setFoldingOutlineShown(false);

        editorSettings.setLanguageSupplier(() -> Language.findLanguageByID("Markdown"));

        markdownEditor.setBorder(JBUI.Borders.emptyLeft(5));

        markdownSourceScrollPanel = new JBScrollPane(markdownEditor.getComponent());
    }

    private void initMarkdownHtmlPanel() {

        MarkdownHtmlPanelProvider provider = MarkdownHtmlPanelProvider.createFromInfo(MarkdownSettings.getDefaultProviderInfo());

        if (!JBCefApp.isSupported()) {
            // Fallback to an alternative browser-less solution
            // https://plugins.jetbrains.com/docs/intellij/jcef.html#jbcefapp
            log.info("当前不支持 JCEF");
            return;
        }

        markdownHtmlPanel = provider.createHtmlPanel();
    }


    private void initPreviewPanel() {

        if (previewIsHtml.get() && JBCefApp.isSupported()) {
            previewPanel.add(markdownHtmlPanel.getComponent(), BorderLayout.CENTER);
        } else {
            // 展示源码
            previewPanel.add(markdownSourceScrollPanel, BorderLayout.CENTER);
        }

    }


    private void initPreviewLeftToolbar() {

        DefaultActionGroup leftGroup = new DefaultActionGroup();

        leftGroup.add(new ToggleAction("Preview", "Preview markdown", AllIcons.Actions.Preview) {

            @Override
            public boolean isDumbAware() {
                return true;
            }

            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return previewIsHtml.get();
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {

                if (!JBCefApp.isSupported()) {
                    // 不支持 JCEF 不允许预览
                    previewIsHtml.set(false);
                    DocViewNotification.notifyInfo(project, DocViewBundle.message("notify.not.support.jcef"));
                } else {
                    previewIsHtml.set(state);
                    if (state) {
                        previewPanel.removeAll();
                        previewPanel.repaint();
                        previewPanel.add(markdownHtmlPanel.getComponent(), BorderLayout.CENTER);
                        previewPanel.revalidate();
                    } else {
                        // 展示源码
                        previewPanel.removeAll();
                        previewPanel.repaint();
                        previewPanel.add(markdownSourceScrollPanel, BorderLayout.CENTER);
                        previewPanel.revalidate();
                    }
                }
            }
        });


        leftGroup.addSeparator();

        leftGroup.add(new AnAction("Editor", "Editor doc", AllIcons.Actions.Edit) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                popup.cancel();
                DocEditorForm.getInstance(project, psiClass, currentDocView.getPsiMethod(), DocViewData.getInstance(currentDocView))
                        .popup();

            }
        });

        ActionToolbarImpl toolbar = (ActionToolbarImpl) ActionManager.getInstance()
                .createActionToolbar("DocViewEditorLeftToolbar", leftGroup, true);
        toolbar.setTargetComponent(previewToolbarPanel);
        toolbar.getComponent().setBackground(markdownEditor.getBackgroundColor());

        toolbar.setForceMinimumSize(true);
        toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        Utils.setSmallerFontForChildren(toolbar);

        previewToolbarPanel.setBackground(markdownEditor.getBackgroundColor());
        previewToolbarPanel.add(toolbar.getComponent(), BorderLayout.WEST);
    }

    private void initPreviewRightToolbar() {
        DefaultActionGroup rightGroup = new DefaultActionGroup();

        rightGroup.add(new AnAction("Upload", "Upload", AllIcons.Actions.Upload) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                myIsPinned.set(true);

                Point location = previewToolbarPanel.getLocationOnScreen();
                location.x = MouseInfo.getPointerInfo().getLocation().x;
                location.y += previewToolbarPanel.getHeight();
                JBPopupFactory.getInstance()
                        .createListPopup(new BaseListPopupStep<>(null, DocViewUploadService.UPLOAD_OPTIONS) {

                            @Override
                            public @NotNull String getTextFor(String value) {
                                return "Upload to " + value;
                            }

                            @Override
                            public @Nullable PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {

                                DocViewUploadService.getInstance(selectedValue)
                                        .doUpload(project, currentDocView);

                                return FINAL_CHOICE;
                            }
                        }).showInScreenCoordinates(previewToolbarPanel, location);
            }
        });


        rightGroup.addSeparator();

        rightGroup.add(new AnAction("Export", "Export markdown", AllIcons.ToolbarDecorator.Export) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                popup.cancel();

                ExportUtils.exportMarkdown(project, currentDocView.getName(), currentMarkdownText);
            }
        });

        rightGroup.add(new AnAction("Copy", "Copy to clipboard", AllIcons.Actions.Copy) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                StringSelection selection = new StringSelection(currentMarkdownText);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);

                DocViewNotification.notifyInfo(project, DocViewBundle.message("notify.copy.success", currentDocView.getName()));
            }
        });

        // init toolbar
        ActionToolbarImpl toolbar = (ActionToolbarImpl) ActionManager.getInstance()
                .createActionToolbar("DocViewEditorRightToolbar", rightGroup, true);
        toolbar.setTargetComponent(previewToolbarPanel);
        toolbar.getComponent().setBackground(markdownEditor.getBackgroundColor());

        toolbar.setForceMinimumSize(true);
        toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        Utils.setSmallerFontForChildren(toolbar);

        previewToolbarPanel.setBackground(markdownEditor.getBackgroundColor());
        previewToolbarPanel.add(toolbar.getComponent(), BorderLayout.EAST);

    }

    private void initMenuToolbarPanelToolbar() {

        DefaultActionGroup menuGroup = new DefaultActionGroup();

        menuGroup.add(new AnAction("Export All", "Export markdown", AllIcons.Ide.IncomingChangesOn) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                popup.cancel();
                ExportUtils.batchExportMarkdown(project, currentDocView.getPsiClass().getName(), docViewList);
            }
        });
        menuGroup.addSeparator();

        menuGroup.add(new AnAction("Upload All", "Upload all To YApi", AllIcons.Ide.OutgoingChangesOn) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                Point location = previewToolbarPanel.getLocationOnScreen();
                location.x = MouseInfo.getPointerInfo().getLocation().x;
                location.y += previewToolbarPanel.getHeight();

                myIsPinned.set(true);

                JBPopupFactory.getInstance()
                        .createListPopup(new BaseListPopupStep<>(null, DocViewUploadService.UPLOAD_OPTIONS) {
                            @Override
                            public @NotNull String getTextFor(String value) {
                                return "Upload to " + value;
                            }

                            @Override
                            public @Nullable PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {

                                DocViewUploadService.getInstance(selectedValue)
                                        .upload(project, docViewList);

                                return FINAL_CHOICE;
                            }
                        }).showInScreenCoordinates(previewToolbarPanel, location);
            }
        });


        // init toolbar
        ActionToolbarImpl toolbar = (ActionToolbarImpl) ActionManager.getInstance()
                .createActionToolbar("DocViewMenuToolbar", menuGroup, true);
        toolbar.setTargetComponent(menuToolbarPanel);
        toolbar.getComponent().setBackground(UIUtil.getTextFieldBackground());

        toolbar.setForceMinimumSize(true);
        toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        Utils.setSmallerFontForChildren(toolbar);

        menuToolbarPanel.setBackground(UIUtil.getTextFieldBackground());
        menuToolbarPanel.add(toolbar.getComponent(), BorderLayout.WEST);

    }


    private void buildDoc() {

        docViewMap = docViewList.stream().collect(Collectors.toMap(DocView::getName, docView -> docView));

        Vector<String> nameVector = docViewList.stream().map(DocView::getName).collect(Collectors.toCollection(Vector::new));

        catalogList.setListData(nameVector);

        catalogList.addListSelectionListener(catalog -> {

            String selectedValue = catalogList.getSelectedValue();

            currentDocView = docViewMap.get(selectedValue);

            docNameLabel.setText(currentDocView.getPsiClass().getQualifiedName());

            // 将 docView 按照模版转换
            currentMarkdownText = DocViewData.markdownText(project, currentDocView);

            if (JBCefApp.isSupported()) {
                markdownHtmlPanel.setHtml(MarkdownUtil.INSTANCE.generateMarkdownHtml(psiFile.getVirtualFile(), currentMarkdownText, project), 0);
            }

            WriteCommandAction.runWriteCommandAction(project, () -> {
                // 光标放在顶部
                markdownDocument.setText(currentMarkdownText);
            });

        });
        if (docViewList.size() == 1 && Settings.getInstance(project).getHideLeft()) {
            // todo 隐藏左侧
        }
    }
}
