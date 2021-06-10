package com.liuzhihang.doc.view.ui;

import com.google.common.collect.Lists;
import com.intellij.find.editorHeaderActions.Utils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.WindowMoveListener;
import com.intellij.util.ui.JBUI;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.SettingsConfigurable;
import com.liuzhihang.doc.view.constant.FieldTypeConstant;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.dto.ParamData;
import com.liuzhihang.doc.view.service.impl.WriterService;
import com.liuzhihang.doc.view.utils.CustomPsiCommentUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 参考地址
 * https://gitee.com/starcwang/easy_javadoc
 *
 * @author liuzhihang
 * @date 2021/3/6 17:52
 */
public class DocEditorForm {

    @NonNls
    public static final String DOC_VIEW_POPUP = "com.intellij.docview.editor.popup";
    private static final AtomicBoolean myIsPinned = new AtomicBoolean(true);
    private final WriterService writerService = ServiceManager.getService(WriterService.class);

    private final DocViewData docViewData;

    private final Project project;
    private final PsiClass psiClass;
    private final PsiMethod psiMethod;

    private JPanel rootPanel;
    private JTabbedPane baseTabbedPane;
    private JPanel headToolbarPanel;
    private JPanel tailToolbarPanel;

    private JPanel namePane;
    private JTextArea nameTextArea;
    private String nameText;
    private String descText;

    private JScrollPane descJSPane;
    private JTextArea descTextArea;
    private JPanel pathPane;
    private JTextArea pathTextArea;
    private JPanel methodPane;
    private JTextArea methodTextArea;

    private JScrollPane requestParamScrollPane;
    private JTable requestParamTable;

    private JScrollPane responseParamScrollPane;
    private JTable responseParamTable;

    private JBPopup popup;

    protected DocEditorForm(@NotNull Project project, @NotNull PsiClass psiClass,
                            @NotNull PsiMethod psiMethod, @NotNull DocViewData docViewData) {
        this.project = project;
        this.psiClass = psiClass;
        this.psiMethod = psiMethod;
        this.docViewData = docViewData;

        this.nameText = this.docViewData.getName();
        this.descText = this.docViewData.getDesc();

        initUI();

        initHeadToolbar();
        initTailLeftToolbar();
        initTailRightToolbar();

        initBaseTabbedPane();
        initRequestParamTable();
        initResponseParamTable();

        // 鼠标拖动
        addMouseListeners();
    }


    @NotNull
    @Contract("_, _, _, _ -> new")
    public static DocEditorForm getInstance(@NotNull Project project, @NotNull PsiClass psiClass,
                                            @NotNull PsiMethod psiMethod, @NotNull DocViewData docViewData) {

        return new DocEditorForm(project, psiClass, psiMethod, docViewData);
    }

    private void addMouseListeners() {
        WindowMoveListener windowMoveListener = new WindowMoveListener(rootPanel);
        rootPanel.addMouseListener(windowMoveListener);
        rootPanel.addMouseMotionListener(windowMoveListener);

    }

    public void popup() {

        // dialog 改成 popup, 第一个为根¸面板，第二个为焦点面板
        popup = JBPopupFactory.getInstance().createComponentPopupBuilder(rootPanel, baseTabbedPane)
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

        namePane.setBorder(IdeBorderFactory.createTitledBorder(DocViewBundle.message("editor.name.title")));
        descJSPane.setBorder(IdeBorderFactory.createTitledBorder(DocViewBundle.message("editor.name.desc")));
        pathPane.setBorder(IdeBorderFactory.createTitledBorder(DocViewBundle.message("editor.name.path")));
        methodPane.setBorder(IdeBorderFactory.createTitledBorder(DocViewBundle.message("editor.name.method")));

        pathTextArea.setEditable(false);
        methodTextArea.setEditable(false);


    }


    private void initBaseTabbedPane() {

        nameTextArea.setText(nameText);
        descTextArea.setText(descText);
        pathTextArea.setText(docViewData.getPath());
        methodTextArea.setText(docViewData.getMethod());
    }

    private void initRequestParamTable() {

        // 边框
        responseParamScrollPane.setBorder(JBUI.Borders.empty());
        responseParamTable.setBorder(JBUI.Borders.empty());

        ParamTableModel paramTableModel = new ParamTableModel(docViewData.getResponseParamDataList());
        responseParamTable.setModel(paramTableModel);

        ParamTableUI.rowSetting(responseParamTable);
        ParamTableUI.columnSetting(responseParamTable);

    }

    private void initResponseParamTable() {

        // 边框
        requestParamScrollPane.setBorder(JBUI.Borders.empty());
        requestParamTable.setBorder(JBUI.Borders.empty());

        ParamTableModel paramTableModel = new ParamTableModel(docViewData.getRequestBodyDataList());
        requestParamTable.setModel(paramTableModel);

        ParamTableUI.rowSetting(requestParamTable);
        ParamTableUI.columnSetting(requestParamTable);

    }


    @NotNull
    private String buildComment(String docName, List<String> params, String returnTagValue, List<String> exceptions) {
        String comment;
        StringBuilder sb = new StringBuilder();

        sb.append("/**\n");
        sb.append("*\n");
        if (!descText.equals(descTextArea.getText())) {
            sb.append("* ").append(descTextArea.getText()).append("\n");
        } else {
            // 否则保持不变
            sb.append("* ").append(descText).append("\n");
        }
        if (CollectionUtils.isNotEmpty(params)) {
            for (String param : params) {
                sb.append("* ").append(param).append("\n");
            }
        }
        if (StringUtils.isNotBlank(returnTagValue)) {
            sb.append("* ").append(returnTagValue).append("\n");
        }
        if (CollectionUtils.isNotEmpty(exceptions)) {
            for (String exception : exceptions) {
                sb.append("* ").append(exception).append("\n");
            }
        }
        if (StringUtils.isNotBlank(docName)) {
            sb.append("*\n* ").append(docName).append("\n");
        }
        sb.append("*/\n");

        comment = sb.toString();
        return comment;
    }

    /**
     * 接口名称生成
     *
     * @param elements
     * @param docName
     * @return
     */
    @Nullable
    private String buildDocName(@NotNull List<PsiElement> elements, String docName) {

        for (Iterator<PsiElement> iterator = elements.iterator(); iterator.hasNext(); ) {
            PsiElement element = iterator.next();
            if (!("PsiDocTag:@" + docName).equalsIgnoreCase(element.toString())) {
                continue;
            }
            if (!nameText.equals(nameTextArea.getText())) {
                // 设置值
                return "@" + docName + " " + nameTextArea.getText();
            }

        }

        // 执行到这里, 说明没找到 tag, 之前没有设置
        if (!psiMethod.getName().equals(nameTextArea.getText())) {
            // 设置值
            return "@" + docName + " " + nameTextArea.getText();
        }

        return null;
    }


    @NotNull
    private String generateFromNoneComment(TagsSettings tagsSettings,
                                           List<String> paramNameList,
                                           String returnName,
                                           List<String> exceptionNameList) {


        StringBuilder sb = new StringBuilder();
        sb.append("/**\n");
        sb.append("*\n");
        sb.append("* ").append(descTextArea.getText()).append("\n");
        if (StringUtils.isNotBlank(nameTextArea.getText())) {
            sb.append("* @").append(tagsSettings.getName()).append(" ").append(nameTextArea.getText()).append("\n");
        }
        for (String paramName : paramNameList) {
            sb.append("* @param ").append(paramName).append(" ").append(paramName).append("\n");
        }
        if (returnName.length() > 0 && !"void".equals(returnName)) {
            if (FieldTypeConstant.BASE_TYPE_SET.contains(returnName)) {
                sb.append("* @return ").append(returnName).append("\n");
            } else {
                sb.append("* @return {@link ").append(returnName).append("}").append("\n");
            }
        }
        for (String exceptionName : exceptionNameList) {
            sb.append("* @throws ").append(exceptionName).append(" ")
                    .append(exceptionName).append("\n");
        }
        sb.append("*/\n");

        return sb.toString();
    }

    private void initHeadToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();


        group.add(new AnAction("Refresh", "Refresh", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // TODO: 2021/3/31 刷新
            }
        });

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
                .createActionToolbar("ParamDocEditorHeadToolbar", group, true);
        toolbar.setTargetComponent(headToolbarPanel);

        toolbar.setForceMinimumSize(true);
        toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        Utils.setSmallerFontForChildren(toolbar);

        headToolbarPanel.add(toolbar.getComponent(), BorderLayout.EAST);
    }

    /**
     * 底部 右侧 主要是提交
     */
    private void initTailRightToolbar() {

        DefaultActionGroup rightGroup = new DefaultActionGroup();

        rightGroup.add(new AnAction("Confirm", "Confirm modification", AllIcons.Actions.Commit) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                if (requestParamTable.isEditing()) {
                    requestParamTable.getCellEditor().stopCellEditing();
                }
                if (responseParamTable.isEditing()) {
                    responseParamTable.getCellEditor().stopCellEditing();
                }
                generateMethodComment();
                generateComment((ParamTableModel) requestParamTable.getModel());
                generateComment((ParamTableModel) responseParamTable.getModel());
                popup.cancel();
            }
        });


        // init toolbar
        ActionToolbarImpl toolbar = (ActionToolbarImpl) ActionManager.getInstance()
                .createActionToolbar("ParamDocEditorTailRightToolbar", rightGroup, true);
        toolbar.setTargetComponent(tailToolbarPanel);

        toolbar.setForceMinimumSize(true);
        toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        Utils.setSmallerFontForChildren(toolbar);

        tailToolbarPanel.add(toolbar.getComponent(), BorderLayout.EAST);

    }

    private void initTailLeftToolbar() {

    }

    private void generateMethodComment() {

        TagsSettings tagsSettings = TagsSettings.getInstance(project);


        PsiDocComment docComment = psiMethod.getDocComment();

        String comment;

        List<String> paramNameList = Arrays.stream(psiMethod.getParameterList().getParameters())
                .map(PsiParameter::getName).collect(Collectors.toList());

        String returnName = psiMethod.getReturnType() == null ? "" : psiMethod.getReturnType().getPresentableText();
        List<String> exceptionNameList = Arrays.stream(psiMethod.getThrowsList().getReferencedTypes())
                .map(PsiClassType::getName).collect(Collectors.toList());

        if (docComment == null) {
            // 注释为空, 生成
            comment = generateFromNoneComment(tagsSettings, paramNameList, returnName, exceptionNameList);

        } else {
            List<PsiElement> elements = Lists.newArrayList(Objects.requireNonNull(psiMethod.getDocComment()).getChildren());

            String docName = buildDocName(elements, tagsSettings.getName());

            List<String> params = CustomPsiCommentUtils.buildParams(elements, paramNameList);

            String returnTagValue = CustomPsiCommentUtils.buildReturn(elements, returnName);
            List<String> exceptions = CustomPsiCommentUtils.buildException(elements, exceptionNameList);

            comment = buildComment(docName, params, returnTagValue, exceptions);
        }

        PsiElementFactory factory = PsiElementFactory.getInstance(project);
        PsiDocComment psiDocComment = factory.createDocCommentFromText(comment);

        writerService.write(project, psiMethod, psiDocComment);

    }

    /**
     * 变动的字段生成注释
     */
    private void generateComment(ParamTableModel paramTableModel) {

        TagsSettings tagsSettings = TagsSettings.getInstance(project);

        Map<PsiElement, ParamData> modifyBodyMap = paramTableModel.getModifyBodyMap();

        for (PsiElement element : modifyBodyMap.keySet()) {
            ParamData data = modifyBodyMap.get(element);
            String comment;

            if (data.getRequired()) {
                comment = "/** "
                        + data.getDesc() + "\n"
                        + "* @" + tagsSettings.getRequired()
                        + " */";
            } else {
                comment = "/** "
                        + data.getDesc()
                        + " */";
            }


            PsiElementFactory factory = PsiElementFactory.getInstance(project);
            PsiDocComment psiDocComment = factory.createDocCommentFromText(comment);
            writerService.write(project, element, psiDocComment);
        }
    }

}
