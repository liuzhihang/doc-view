package com.liuzhihang.doc.view.ui;

import com.intellij.find.editorHeaderActions.Utils;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.editor.Editor;
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
import com.liuzhihang.doc.view.config.SettingsConfigurable;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.dto.DocViewParamData;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.ui.treeview.ParamTreeTableView;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import com.liuzhihang.doc.view.utils.GsonFormatUtil;
import com.liuzhihang.doc.view.utils.ParamPsiUtils;
import icons.DocViewIcons;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
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

    private ParamTreeTableView tableView;

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

        group.add(new AnAction("Setting", "Doc view settings", DocViewIcons.SETTINGS) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
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

        ActionToolbarImpl toolbar = (ActionToolbarImpl) ActionManager.getInstance()
                .createActionToolbar("ParamDocParamEditorHeadToolbar", group, true);
        toolbar.setTargetComponent(headToolbarPanel);

        toolbar.setForceMinimumSize(true);
        Utils.setSmallerFontForChildren(toolbar);

        headToolbarPanel.add(toolbar.getComponent(), BorderLayout.EAST);
    }

    /**
     * 底部 右侧 主要是提交
     */
    private void initTailRightToolbar() {

        DefaultActionGroup rightGroup = new DefaultActionGroup();

        rightGroup.add(new AnAction("Copy as Json", "Copy as json", DocViewIcons.JSON) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                Map<String, Object> fieldMap = ParamPsiUtils.getFieldsAndDefaultValue(psiClass, null);
                String format = GsonFormatUtil.gsonFormat(fieldMap);
                StringSelection selection = new StringSelection(format);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);
                DocViewNotification.notifyInfo(project, DocViewBundle.message("param.copy.success", psiClass.getName()));
                popup.cancel();
            }
        });

        rightGroup.add(new AnAction("Confirm", "Confirm modification", DocViewIcons.CHECKED) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                if (tableView.isEditing()) {
                    tableView.getCellEditor().stopCellEditing();
                }

                DocViewUtils.writeComment(project, tableView.getModifiedMap());
                popup.cancel();
            }
        });


        // init toolbar
        ActionToolbarImpl toolbar = (ActionToolbarImpl) ActionManager.getInstance()
                .createActionToolbar("ParamDocParamEditorTailRightToolbar", rightGroup, true);
        toolbar.setTargetComponent(tailToolbarPanel);

        toolbar.setForceMinimumSize(true);
        Utils.setSmallerFontForChildren(toolbar);

        tailToolbarPanel.add(toolbar.getComponent(), BorderLayout.EAST);

    }

    private void initTailLeftToolbar() {

    }


    private void initParamTable() {

        Body rootBody = new Body();
        rootBody.setQualifiedNameForClassType(psiClass.getQualifiedName());
        ParamPsiUtils.buildBodyList(psiClass, null, rootBody);

        List<DocViewParamData> dataList = DocViewData.buildBodyDataList(rootBody.getChildList());

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
