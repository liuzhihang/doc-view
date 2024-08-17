package com.liuzhihang.doc.view.ui;

import com.intellij.find.editorHeaderActions.Utils;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.SettingsConfigurable;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.service.DocViewUploadService;
import com.liuzhihang.doc.view.utils.EditorUtils;
import com.liuzhihang.doc.view.utils.ExportUtils;
import icons.DocViewIcons;
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
 * 文档预览界面 UI
 *
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

    /**
     * 分割窗口
     */
    private final JBSplitter viewSplitter = new OnePixelSplitter(0.2F);

    /**
     * 左侧目录面板
     */
    private final JBPanel<?> catalogPane = new JBPanel<>(new BorderLayout());

    /**
     * 目录面板
     */
    private final JBList<String> catalogList = new JBList<>();

    /**
     * 目录面板操作栏
     */
    private final JBPanel<?> catalogToolbarPane = new JBPanel<>(new BorderLayout());

    /**
     * 预览面板
     */
    private final JBPanel<?> previewPanel = new JBPanel<>(new BorderLayout());

    /**
     * 预览面板内容
     */
    private final JBPanel<?> previewContentPanel = new JBPanel<>(new BorderLayout());

    /**
     * 预览面板操作栏
     */
    private final JBPanel<?> previewToolbarPanel = new JBPanel<>(new BorderLayout());

    /**
     * Markdown Html 面板
     */
    private MarkdownHtmlPanel markdownHtmlPanel;

    /**
     * 根面板
     */
    private JPanel rootPanel;

    /**
     * 呈现面板
     */
    private JPanel viewPanel;

    /**
     * 工具面板
     */
    private JPanel headToolbarPanel;

    /**
     * 文档名称标签
     */
    private JLabel docNameLabel;


    /**
     * 当前 markdown 编辑器
     */
    private EditorEx markdownEditor;

    /**
     * 当前文本 text
     */
    private String currentMarkdownText;

    /**
     * 当前文档
     */
    private DocView currentDocView;

    /**
     * 弹出窗口
     */
    private JBPopup popup;

    /**
     * 当前类
     */
    private final PsiClass psiClass;

    /**
     * 当前方法
     */
    private final PsiMethod psiMethod;

    /**
     * 文档列表
     */
    private List<DocView> docViewList;

    /**
     * 文档 Map
     */
    private Map<String, DocView> docViewMap;


    public PreviewForm(@NotNull PsiClass psiClass, PsiMethod psiMethod) {

        this.psiClass = psiClass;
        this.psiMethod = psiMethod;

        // UI调整
        layout();
        // 生成文档
        buildDoc();
        // 鼠标监听事件
        addMouseListeners();
    }

    /**
     * 鼠标监听
     */
    private void addMouseListeners() {
        WindowMoveListener windowMoveListener = new WindowMoveListener(rootPanel);
        rootPanel.addMouseListener(windowMoveListener);
        rootPanel.addMouseMotionListener(windowMoveListener);
        headToolbarPanel.addMouseListener(windowMoveListener);
        headToolbarPanel.addMouseMotionListener(windowMoveListener);

    }

    @NotNull
    @Contract("_, _ -> new")
    public static PreviewForm getInstance(@NotNull PsiClass psiClass, PsiMethod psiMethod) {
        return new PreviewForm(psiClass, psiMethod);
    }

    /**
     * 弹出窗口
     */
    public void popup() {

        // dialog 改成 popup, 第一个为根面板，第二个为焦点面板
        popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(rootPanel, previewToolbarPanel)
                .setProject(psiClass.getProject())
                .setResizable(true)
                .setMovable(true)
                .setModalContext(false)
                .setRequestFocus(true)
                .setBelongsToGlobalPopupStack(true)
                .setDimensionServiceKey(null, DOC_VIEW_POPUP, true)
                .setLocateWithinScreenBounds(false)
                // 鼠标点击外部时是否取消弹窗 外部单击, 未处于 pin 状态则可关闭
                .setCancelOnMouseOutCallback(event ->
                        event.getID() == MouseEvent.MOUSE_PRESSED && !myIsPinned.get()
                )
                // 单击外部时取消弹窗
                .setCancelOnClickOutside(false)
                // 在其他窗口打开时取消
                .setCancelOnOtherWindowOpen(false)
                .setCancelOnWindowDeactivation(false)
                .createPopup();
        popup.showCenteredInCurrentWindow(psiClass.getProject());
    }


    /**
     * 根面板：界面布局
     */
    private void layout() {

        // 目录面板：左侧
        layoutCatalogPane();
        layoutPreviewPanel();
        layoutHeadToolbar();

        viewPanel.setLayout(new BorderLayout());
        viewPanel.setBorder(JBUI.Borders.empty());

        // 不为空说明指向的是当前方法
        if (psiMethod != null) {
            viewPanel.add(previewPanel, BorderLayout.CENTER);
        } else {
            // 将分割窗口插入
            viewPanel.add(viewSplitter, BorderLayout.CENTER);

            // 记录当前俩个组件的比例，存放到map中，key即为该值
            viewSplitter.setSplitterProportionKey("docViewPreviewProportionKey");
            // 填充分割面板
            viewSplitter.setFirstComponent(catalogPane);
            viewSplitter.setSecondComponent(previewPanel);
            viewSplitter.setBorder(JBUI.Borders.empty());
        }

        // 顶部文字边距
        docNameLabel.setBorder(JBUI.Borders.emptyLeft(5));
    }

    /**
     * 右边预览面板
     */
    private void layoutPreviewPanel() {
        previewPanel.setMinimumSize(new Dimension(150, 0));
        previewPanel.setPreferredSize(new Dimension(0, 0));
        // 预览面板
        previewPanel.add(previewContentPanel, BorderLayout.CENTER);
        previewPanel.add(previewToolbarPanel, BorderLayout.SOUTH);
        previewToolbarPanel.setBorder(JBUI.Borders.empty());
        previewPanel.setBorder(JBUI.Borders.empty());

        // 右侧 Markdown 源码面板
        markdownSourcePanel();
        // markdownHtmlPanel
        markdownHtmlPanel();
        // 选择在预览面板上呈现的面板：源码/HTML
        choosePreviewPanel();
        // 预览面板操作栏
        previewLeftToolbar();
        // 预览面板右侧操作栏
        previewRightToolbar();
    }

    /**
     * 目录面板渲染，左侧面板
     */
    private void layoutCatalogPane() {
        catalogPane.setMinimumSize(new Dimension(150, 0));
        catalogPane.setPreferredSize(new Dimension(150, 0));
        // 目录面板
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(catalogList);
        scrollPane.setBorder(JBUI.Borders.customLine(JBColor.LIGHT_GRAY));

        catalogPane.add(scrollPane, BorderLayout.CENTER);
        catalogPane.add(catalogToolbarPane, BorderLayout.SOUTH);

        catalogPane.setBorder(JBUI.Borders.emptyLeft(5));
        catalogList.setBorder(JBUI.Borders.empty());
        catalogList.setBackground(UIUtil.getTextFieldBackground());

        // 目录面板操作栏
        catalogPanelToolbar();
    }

    private void layoutHeadToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(new AnAction("Setting", "Doc view settings", DocViewIcons.SETTINGS) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                popup.cancel();
                ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(), SettingsConfigurable.class);
            }
        });

        group.addSeparator();

        group.add(new ToggleAction("Pin", "Pin window", DocViewIcons.PIN) {

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

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }

        });

        ActionToolbarImpl toolbar = (ActionToolbarImpl) ActionManager.getInstance().createActionToolbar("DocViewRootToolbar", group, true);
        toolbar.setTargetComponent(headToolbarPanel);

        toolbar.setForceMinimumSize(true);
        Utils.setSmallerFontForChildren(toolbar);

        headToolbarPanel.add(toolbar.getComponent(), BorderLayout.EAST);
    }


    /**
     * 初始化 Markdown 源码面板
     */
    private void markdownSourcePanel() {

        markdownEditor = (EditorEx) EditorFactory.getInstance().createViewer(markdownDocument, psiClass.getProject(), EditorKind.UNTYPED);

        FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension("md");
        markdownEditor.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(psiClass.getProject(), fileType));

        EditorUtils.renderMarkdownEditor(markdownEditor);

        markdownEditor.setBorder(JBUI.Borders.emptyLeft(5));
    }

    /**
     * markdown 预览界面
     */
    private void markdownHtmlPanel() {

        MarkdownHtmlPanelProvider provider = MarkdownHtmlPanelProvider.createFromInfo(MarkdownSettings.getDefaultProviderInfo());

        if (!JBCefApp.isSupported()) {
            // Fallback to an alternative browser-less solution
            // https://plugins.jetbrains.com/docs/intellij/jcef.html#jbcefapp
            log.info("当前不支持 JCEF");
            return;
        }

        markdownHtmlPanel = provider.createHtmlPanel();
    }

    /**
     * 预览面板，根据设置选择 HTML 面板或者 Markdown 面板
     */
    private void choosePreviewPanel() {

        if (previewIsHtml.get() && JBCefApp.isSupported()) {
            previewContentPanel.add(markdownHtmlPanel.getComponent(), BorderLayout.CENTER);
        } else {
            // 展示源码
            previewContentPanel.add(ScrollPaneFactory.createScrollPane(markdownEditor.getComponent()), BorderLayout.CENTER);
        }

    }

    /**
     * 预览面板左侧操作栏
     */
    private void previewLeftToolbar() {

        DefaultActionGroup leftGroup = new DefaultActionGroup();

        leftGroup.add(new ToggleAction("Preview", "Preview markdown", DocViewIcons.EDITOR_PREVIEW) {

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
                    DocViewNotification.notifyInfo(psiClass.getProject(), DocViewBundle.message("notify.not.support.jcef"));
                } else {
                    previewIsHtml.set(state);
                    if (state) {
                        previewContentPanel.removeAll();
                        previewContentPanel.repaint();
                        previewContentPanel.add(markdownHtmlPanel.getComponent(), BorderLayout.CENTER);
                        previewContentPanel.revalidate();
                    } else {
                        // 展示源码
                        previewContentPanel.removeAll();
                        previewContentPanel.repaint();
                        previewContentPanel.add(ScrollPaneFactory.createScrollPane(markdownEditor.getComponent()), BorderLayout.CENTER);
                        previewContentPanel.revalidate();
                    }
                }
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }

        });

        ActionToolbarImpl toolbar = (ActionToolbarImpl) ActionManager.getInstance().createActionToolbar("DocViewEditorLeftToolbar", leftGroup, true);
        toolbar.setTargetComponent(previewToolbarPanel);
        toolbar.getComponent().setBackground(markdownEditor.getBackgroundColor());

        toolbar.setForceMinimumSize(true);
        Utils.setSmallerFontForChildren(toolbar);

        previewToolbarPanel.setBackground(markdownEditor.getBackgroundColor());
        previewToolbarPanel.add(toolbar.getComponent(), BorderLayout.WEST);
    }

    /**
     * 预览面板右侧操作栏
     */
    private void previewRightToolbar() {
        DefaultActionGroup rightGroup = new DefaultActionGroup();

        rightGroup.add(new AnAction("Upload", "Upload", DocViewIcons.UPLOAD) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                myIsPinned.set(true);

                Point location = previewToolbarPanel.getLocationOnScreen();
                location.x = MouseInfo.getPointerInfo().getLocation().x;
                location.y += previewToolbarPanel.getHeight();
                JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<>(null, DocViewUploadService.UPLOAD_OPTIONS) {

                    @Override
                    public @NotNull String getTextFor(String value) {
                        return "Upload to " + value;
                    }

                    @Override
                    public @Nullable PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {

                        DocViewUploadService.getInstance(selectedValue).doUpload(psiClass.getProject(), currentDocView);

                        return FINAL_CHOICE;
                    }
                }).showInScreenCoordinates(previewToolbarPanel, location);
            }
        });


        rightGroup.addSeparator();

        rightGroup.add(new AnAction("Export", "Export markdown", DocViewIcons.DOWNLOAD) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                popup.cancel();

                ExportUtils.exportMarkdown(psiClass.getProject(), currentDocView.getName(), currentMarkdownText);
            }
        });

        rightGroup.add(new AnAction("Copy", "Copy to clipboard", DocViewIcons.COPY) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                StringSelection selection = new StringSelection(currentMarkdownText);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);

                DocViewNotification.notifyInfo(psiClass.getProject(), DocViewBundle.message("notify.copy.success", currentDocView.getName()));
            }
        });

        // init toolbar
        ActionToolbarImpl toolbar = (ActionToolbarImpl) ActionManager.getInstance().createActionToolbar("DocViewEditorRightToolbar", rightGroup, true);
        toolbar.setTargetComponent(previewToolbarPanel);
        toolbar.getComponent().setBackground(markdownEditor.getBackgroundColor());

        toolbar.setForceMinimumSize(true);
        Utils.setSmallerFontForChildren(toolbar);

        previewToolbarPanel.setBackground(markdownEditor.getBackgroundColor());
        previewToolbarPanel.add(toolbar.getComponent(), BorderLayout.EAST);

    }

    /**
     * 构建目录
     */
    private void catalogPanelToolbar() {

        DefaultActionGroup menuGroup = new DefaultActionGroup();

        menuGroup.add(new AnAction("Export All", "Export markdown", DocViewIcons.DOWNLOAD) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                popup.cancel();
                ExportUtils.batchExportMarkdown(psiClass.getProject(), currentDocView.getPsiClass().getName(), docViewList);
            }
        });
        menuGroup.addSeparator();

        menuGroup.add(new AnAction("Upload All", "Upload all To YApi", DocViewIcons.UPLOAD) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                Point location = previewToolbarPanel.getLocationOnScreen();
                location.x = MouseInfo.getPointerInfo().getLocation().x;
                location.y += previewToolbarPanel.getHeight();

                myIsPinned.set(true);

                JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<>(null, DocViewUploadService.UPLOAD_OPTIONS) {
                    @Override
                    public @NotNull String getTextFor(String value) {
                        return "Upload to " + value;
                    }

                    @Override
                    public @Nullable PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {

                        DocViewUploadService.getInstance(selectedValue).upload(psiClass.getProject(), docViewList);

                        return FINAL_CHOICE;
                    }
                }).showInScreenCoordinates(previewToolbarPanel, location);
            }
        });


        // init toolbar
        ActionToolbarImpl toolbar = (ActionToolbarImpl) ActionManager.getInstance().createActionToolbar("DocViewMenuToolbar", menuGroup, true);
        toolbar.setTargetComponent(catalogToolbarPane);
        toolbar.getComponent().setBackground(UIUtil.getTextFieldBackground());

        toolbar.setForceMinimumSize(true);
        Utils.setSmallerFontForChildren(toolbar);

        catalogToolbarPane.setBackground(UIUtil.getTextFieldBackground());
        catalogToolbarPane.add(toolbar.getComponent(), BorderLayout.WEST);

    }

    /**
     * 构造文档
     */
    private void buildDoc() {

        docViewList = DocViewService.getInstance(psiClass.getProject(), psiClass).buildDoc(psiClass, psiMethod);

        docViewMap = docViewList.stream().collect(Collectors.toMap(DocView::getName, docView -> docView));

        Vector<String> nameVector = docViewList.stream().map(DocView::getName).collect(Collectors.toCollection(Vector::new));

        catalogList.setListData(nameVector);

        catalogList.addListSelectionListener(catalog -> {

            String selectedValue = catalogList.getSelectedValue();

            currentDocView = docViewMap.get(selectedValue);

            docNameLabel.setText(currentDocView.getPsiClass().getQualifiedName());

            // 将 docView 按照模版转换
            currentMarkdownText = DocViewData.markdownText(psiClass.getProject(), currentDocView);

            if (JBCefApp.isSupported()) {

                markdownHtmlPanel.setHtml(MarkdownUtil.INSTANCE.generateMarkdownHtml(psiClass.getContainingFile().getVirtualFile(), currentMarkdownText, psiClass.getProject()), 0);
            }

            WriteCommandAction.runWriteCommandAction(psiClass.getProject(), () -> {
                // 光标放在顶部
                markdownDocument.setText(currentMarkdownText);
            });

        });

        // 默认选择第一个
        catalogList.setSelectedIndex(0);
    }
}
