package com.liuzhihang.doc.view.ui;

import com.intellij.find.editorHeaderActions.Utils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.ui.WindowMoveListener;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.util.ui.JBUI;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.config.SettingsConfigurable;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.dto.DocViewParamData;
import com.liuzhihang.doc.view.exception.DocViewException;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.ui.treeview.ParamTreeTableView;
import com.liuzhihang.doc.view.utils.DocViewBackgroundTasks;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import com.liuzhihang.doc.view.utils.GsonFormatUtil;
import com.liuzhihang.doc.view.utils.ParamPsiUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author liuzhihang
 * @date 2021/3/31 10:49
 */
public class ParamDocEditorForm {

    private final Project project;
    private final PsiFile psiFile;
    private final Editor editor;
    private final PsiClass psiClass;
    private final String entityName;

    private ParamTreeTableView tableView;
    private List<DocViewParamData> paramDataList = List.of();
    private long markdownCopyRequest;

    private JPanel rootPanel;
    private JPanel headToolbarPanel;
    private JPanel tailToolbarPanel;

    private JScrollPane paramScrollPane;

    private JBPopup popup;

    @NonNls
    public static final String DOC_VIEW_POPUP = "com.intellij.docview.param.editor.popup";
    private static final AtomicBoolean myIsPinned = new AtomicBoolean(true);

    public ParamDocEditorForm(@NotNull Project project, @NotNull PsiFile psiFile,
                              @NotNull Editor editor, @NotNull PsiClass psiClass) {

        this.project = project;
        this.psiFile = psiFile;
        this.editor = editor;
        this.psiClass = psiClass;
        this.entityName = psiClass.getName();

        // UI调整
        initUI();
        initHeadToolbar();
        initTailLeftToolbar();
        initTailRightToolbar();

        initParamTable();
        addMouseListeners();
    }


    @NotNull
    @Contract("_, _, _, _ -> new")
    public static ParamDocEditorForm getInstance(@NotNull Project project, @NotNull PsiFile psiFile,
                                                 @NotNull Editor editor, @NotNull PsiClass psiClass) {
        return new ParamDocEditorForm(project, psiFile, editor, psiClass);
    }


    private void addMouseListeners() {
        WindowMoveListener windowMoveListener = new WindowMoveListener(rootPanel);
        rootPanel.addMouseListener(windowMoveListener);
        rootPanel.addMouseMotionListener(windowMoveListener);

    }

    /**
     * 显示弹窗
     */
    public void popup() {

        // dialog 改成 popup, 第一个为根面板，第二个为焦点面板
        popup = JBPopupFactory.getInstance().createComponentPopupBuilder(rootPanel, paramScrollPane)
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
                .setMinSize(new Dimension(600, 380))
                .setCancelOnWindowDeactivation(false)
                .createPopup();
        popup.showCenteredInCurrentWindow(project);

    }


    private void initUI() {
        // 边框
        paramScrollPane.setBackground(null);
        paramScrollPane.setOpaque(false);
        paramScrollPane.setBorder(JBUI.Borders.empty());
    }

    /**
     * 初始化头部工具栏
     */
    private void initHeadToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(new AnAction(DocViewBundle.message("action.settings"),
                DocViewBundle.message("action.settings.description"), AllIcons.General.Settings) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(), SettingsConfigurable.class);
            }
        });

        group.addSeparator();

        group.add(new ToggleAction(DocViewBundle.message("action.pin"),
                DocViewBundle.message("action.pin.description"), AllIcons.General.Pin) {

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

        ActionToolbar toolbar = ActionManager.getInstance()
                .createActionToolbar("ParamDocParamEditorHeadToolbar", group, true);
        toolbar.setTargetComponent(headToolbarPanel);

        keepToolbarPreferredMinimumSize(toolbar);
        Utils.setSmallerFontForChildren(toolbar.getComponent());

        headToolbarPanel.add(toolbar.getComponent(), BorderLayout.EAST);
    }

    private static void keepToolbarPreferredMinimumSize(@NotNull ActionToolbar toolbar) {
        ToolbarLayoutStrategy delegate = toolbar.getLayoutStrategy();
        toolbar.setLayoutStrategy(new ToolbarLayoutStrategy() {
            @Override
            public @NotNull List<Rectangle> calculateBounds(@NotNull ActionToolbar actionToolbar) {
                return delegate.calculateBounds(actionToolbar);
            }

            @Override
            public @NotNull Dimension calcPreferredSize(@NotNull ActionToolbar actionToolbar) {
                return delegate.calcPreferredSize(actionToolbar);
            }

            @Override
            public @NotNull Dimension calcMinimumSize(@NotNull ActionToolbar actionToolbar) {
                return delegate.calcPreferredSize(actionToolbar);
            }
        });
    }

    /**
     * 底部 右侧 主要是提交
     */
    private void initTailRightToolbar() {

        DefaultActionGroup rightGroup = new DefaultActionGroup();

        rightGroup.add(new AnAction(DocViewBundle.message("action.copy.json"),
                DocViewBundle.message("action.copy.json.description"), AllIcons.FileTypes.Json) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                DocViewBackgroundTasks.runReadTask(
                        project,
                        DocViewBundle.message("task.docview.json"),
                        true,
                        indicator -> ParamPsiUtils.getFieldsAndDefaultValue(psiClass, null),
                        fieldMap -> {
                            String format = GsonFormatUtil.gsonFormat(fieldMap);
                            StringSelection selection = new StringSelection(format);
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            clipboard.setContents(selection, selection);
                            DocViewNotification.notifyInfo(project, DocViewBundle.message("param.copy.success", psiClass.getName()));
                            popup.cancel();
                        },
                        throwable -> DocViewNotification.notifyError(project, throwable.getMessage())
                );
            }
        });

        rightGroup.add(createMarkdownCopyAction());

        rightGroup.add(new AnAction(DocViewBundle.message("action.confirm"),
                DocViewBundle.message("action.confirm.description"), AllIcons.Actions.Checked) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                if (tableView == null) {
                    DocViewNotification.notifyInfo(project, DocViewBundle.message("ui.docview.loading.retry"));
                    return;
                }

                if (tableView.isEditing()) {
                    tableView.getCellEditor().stopCellEditing();
                }

                try {
                    DocViewUtils.writeComment(project, tableView.getModifiedMap());
                } catch (DocViewException exception) {
                    DocViewNotification.notifyError(project, exception.getMessage());
                    return;
                }
                popup.cancel();
            }
        });


        // init toolbar
        ActionToolbar toolbar = ActionManager.getInstance()
                .createActionToolbar("ParamDocParamEditorTailRightToolbar", rightGroup, true);
        toolbar.setTargetComponent(tailToolbarPanel);

        keepToolbarPreferredMinimumSize(toolbar);
        Utils.setSmallerFontForChildren(toolbar.getComponent());

        tailToolbarPanel.add(toolbar.getComponent(), BorderLayout.EAST);

    }

    private void initTailLeftToolbar() {

    }

    private AnAction createMarkdownCopyAction() {
        return new AnAction(DocViewBundle.message("action.copy.markdown"),
                DocViewBundle.message("action.copy.markdown.description"), AllIcons.Actions.Copy) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (!isMarkdownCopyAvailable()) {
                    return;
                }
                if (tableView.isEditing() && !tableView.getCellEditor().stopCellEditing()) {
                    return;
                }

                // Capture editable values on EDT; rendering must not retain or dereference PSI.
                List<DocViewParamData> snapshot = snapshotParamData(paramDataList);
                boolean separate = Boolean.TRUE.equals(Settings.getInstance(project).getSeparateParam());
                long request = ++markdownCopyRequest;
                DocViewBackgroundTasks.runReadTask(project,
                        DocViewBundle.message("task.docview.markdown"), true,
                        indicator -> DocViewData.bodyMarkdown(snapshot, separate),
                        markdown -> {
                            if (request != markdownCopyRequest || !isMarkdownCopyAvailable() || markdown.isBlank()) {
                                return;
                            }
                            CopyPasteManager.getInstance().setContents(new StringSelection(markdown));
                            DocViewNotification.notifyInfo(project, DocViewBundle.message("notify.copy.success", entityName));
                            popup.cancel();
                        },
                        throwable -> {
                            if (request == markdownCopyRequest && isMarkdownCopyAvailable()) {
                                DocViewNotification.notifyError(project, throwable.getMessage());
                            }
                        });
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(isMarkdownCopyAvailable());
            }

            @Override
            public boolean isDumbAware() {
                return true;
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        };
    }

    private boolean isMarkdownCopyAvailable() {
        return !project.isDisposed() && popup != null && !popup.isDisposed()
                && tableView != null && !paramDataList.isEmpty();
    }

    private static List<DocViewParamData> snapshotParamData(List<DocViewParamData> source) {
        List<DocViewParamData> snapshot = new ArrayList<>(source.size());
        for (DocViewParamData data : source) {
            DocViewParamData copy = new DocViewParamData();
            copy.setName(data.getName());
            copy.setType(data.getType());
            copy.setRequired(data.getRequired());
            copy.setExample(data.getExample());
            copy.setDesc(data.getDesc());
            copy.setSince(data.getSince());
            copy.setVersion(data.getVersion());
            copy.setPrefixSymbol1(data.getPrefixSymbol1());
            copy.setPrefixSymbol2(data.getPrefixSymbol2());
            copy.setCollection(data.isCollection());
            copy.setMap(data.isMap());
            if (data.getChildList() != null && !data.getChildList().isEmpty()) {
                copy.setChildList(snapshotParamData(data.getChildList()));
            }
            snapshot.add(copy);
        }
        return snapshot;
    }


    private void initParamTable() {

        paramScrollPane.setViewportView(new JLabel(
                DocViewBundle.message("task.docview.parameters"), SwingConstants.CENTER));

        DocViewBackgroundTasks.runReadTask(
                project,
                DocViewBundle.message("task.docview.parameters"),
                true,
                indicator -> {
                    Body rootBody = new Body();
                    rootBody.setQualifiedNameForClassType(psiClass.getQualifiedName());
                    ParamPsiUtils.buildBodyList(psiClass, null, rootBody);
                    return DocViewData.buildBodyDataList(rootBody.getChildList());
                },
                this::applyParamTable,
                throwable -> DocViewNotification.notifyError(project, throwable.getMessage())
        );
    }

    private void applyParamTable(@NotNull List<DocViewParamData> dataList) {

        paramDataList = dataList;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();

        convertToTreeNode(root, dataList);

        ListTreeTableModelOnColumns model = new ListTreeTableModelOnColumns(root, ParamTreeTableView.COLUMN_INFOS);

        tableView = new ParamTreeTableView(model);

        paramScrollPane.setViewportView(tableView);
    }

    private void convertToTreeNode(DefaultMutableTreeNode root, List<DocViewParamData> paramDataList) {

        for (DocViewParamData data : paramDataList) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(data);
            root.add(node);

            if (data.getChildList() != null && !data.getChildList().isEmpty()) {
                convertToTreeNode(node, data.getChildList());
            }

        }
    }

}
