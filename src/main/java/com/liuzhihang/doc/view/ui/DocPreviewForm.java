package com.liuzhihang.doc.view.ui;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.ui.WindowMoveListener;
import org.intellij.plugins.markdown.settings.MarkdownApplicationSettings;
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel;
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanelProvider;
import org.intellij.plugins.markdown.ui.preview.html.MarkdownUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author liuzhihang
 * @date 2021/6/9 20:41
 */
public class DocPreviewForm {

    @NonNls
    public static final String DOC_VIEW_PREVIEW_POPUP = "com.intellij.docview.preview.popup";
    private static final AtomicBoolean myIsPinned = new AtomicBoolean(false);


    private JPanel rootPanel;
    private JPanel docPreviewPanel;

    private Project project;
    private PsiFile psiFile;
    private Editor editor;
    private PsiClass psiClass;

    private String markdownStr;

    private JBPopup popup;

    public DocPreviewForm(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull Editor editor,
                          @NotNull PsiClass psiClass, @NotNull String markdownStr) {
        this.project = project;
        this.psiFile = psiFile;
        this.editor = editor;
        this.psiClass = psiClass;
        this.markdownStr = markdownStr;

        initMarkdown();


        addMouseListeners();
    }


    @NotNull
    @Contract("_, _, _, _, _ -> new")
    public static DocPreviewForm getInstance(@NotNull Project project, @NotNull PsiFile psiFile,
                                             @NotNull Editor editor, @NotNull PsiClass psiClass,
                                             @NotNull String markdownStr) {
        return new DocPreviewForm(project, psiFile, editor, psiClass, markdownStr);
    }

    public void popup() {

        // dialog 改成 popup, 第一个为根面板，第二个为焦点面板
        popup = JBPopupFactory.getInstance().createComponentPopupBuilder(rootPanel, rootPanel)
                .setProject(project)
                .setResizable(true)
                .setMovable(true)

                .setModalContext(false)
                .setRequestFocus(true)
                .setBelongsToGlobalPopupStack(true)
                .setDimensionServiceKey(null, DOC_VIEW_PREVIEW_POPUP, true)
                .setLocateWithinScreenBounds(false)
                // 鼠标点击外部时是否取消弹窗 外部单击, 未处于 pin 状态则可关闭
                .setCancelOnMouseOutCallback(event -> event.getID() == MouseEvent.MOUSE_PRESSED && !myIsPinned.get())

                // 单击外部时取消弹窗
                .setCancelOnClickOutside(false)
                // 在其他窗口打开时取消
                .setCancelOnOtherWindowOpen(false)
                .setMinSize(new Dimension(600, 380))
                .setCancelOnWindowDeactivation(false)
                .createPopup();
        popup.showCenteredInCurrentWindow(project);
    }

    private void addMouseListeners() {
        WindowMoveListener windowMoveListener = new WindowMoveListener(rootPanel);
        rootPanel.addMouseListener(windowMoveListener);
        rootPanel.addMouseMotionListener(windowMoveListener);

    }


    private void initMarkdown() {

        String html = MarkdownUtil.INSTANCE.generateMarkdownHtml(psiFile.getVirtualFile(), markdownStr, project);

        MarkdownApplicationSettings settings = MarkdownApplicationSettings.getInstance();

        final MarkdownHtmlPanelProvider.ProviderInfo providerInfo = settings.getMarkdownPreviewSettings().getHtmlPanelProviderInfo();

        MarkdownHtmlPanelProvider provider = MarkdownHtmlPanelProvider.createFromInfo(providerInfo);

        @NotNull MarkdownHtmlPanel htmlPanel = provider.createHtmlPanel();
        htmlPanel.setHtml(html, 0);

        // myPanel = retrievePanelProvider(settings).createHtmlPanel();
        docPreviewPanel.add(htmlPanel.getComponent(), BorderLayout.CENTER);
    }
}
