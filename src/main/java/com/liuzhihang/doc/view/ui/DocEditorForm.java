package com.liuzhihang.doc.view.ui;

import com.google.common.collect.Lists;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.ui.IdeBorderFactory;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.TagsSettings;
import com.liuzhihang.doc.view.constant.FieldTypeConstant;
import com.liuzhihang.doc.view.service.impl.WriterService;
import com.liuzhihang.doc.view.utils.CustomPsiCommentUtils;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 参考地址
 * https://gitee.com/starcwang/easy_javadoc
 *
 * @author liuzhihang
 * @date 2021/3/6 17:52
 */
public class DocEditorForm extends DialogWrapper {

    private JPanel rootPanel;
    private JTabbedPane baseTabbedPane;

    /**
     * 接口信息
     */
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

    /**
     * 请求参数
     */
    private JScrollPane requestParamScrollPane;
    private JTable requestParamTable;

    public static final List<String> titleList = Arrays.asList("参数名", "类型", "必选", "描述");


    private Project project;
    private PsiClass psiClass;
    private PsiMethod psiMethod;

    private WriterService writerService = ServiceManager.getService(WriterService.class);

    protected DocEditorForm(@NotNull Project project, @NotNull PsiClass psiClass, @NotNull PsiMethod psiMethod) {
        super(project, true, DialogWrapper.IdeModalityType.PROJECT);
        this.project = project;
        this.psiClass = psiClass;
        this.psiMethod = psiMethod;

        namePane.setBorder(IdeBorderFactory.createTitledBorder(DocViewBundle.message("editor.name.title")));
        descJSPane.setBorder(IdeBorderFactory.createTitledBorder(DocViewBundle.message("editor.name.desc")));
        pathPane.setBorder(IdeBorderFactory.createTitledBorder(DocViewBundle.message("editor.name.path")));
        methodPane.setBorder(IdeBorderFactory.createTitledBorder(DocViewBundle.message("editor.name.method")));

        init();
        initUI();

        initBaseTabbedPane();
        initRequestParamTable();
    }

    private void initUI() {
        pathTextArea.setEditable(false);
        methodTextArea.setEditable(false);
    }

    @NotNull
    @Contract("_, _, _ -> new")
    public static DocEditorForm getInstance(@NotNull Project project, @NotNull PsiClass psiClass, @NotNull PsiMethod psiMethod) {

        return new DocEditorForm(project, psiClass, psiMethod);
    }


    private void initBaseTabbedPane() {

        TagsSettings tagsSettings = TagsSettings.getInstance(project);

        String name = CustomPsiCommentUtils.getComment(psiMethod.getDocComment(), tagsSettings.getName());

        if (StringUtils.isBlank(name)) {
            name = psiMethod.getName();
        }
        nameText = name;
        nameTextArea.setText(nameText);

        descText = CustomPsiCommentUtils.getMethodComment(psiMethod.getDocComment());

        descTextArea.setText(descText);

        if (psiClass.isInterface()) {
            pathTextArea.setText(psiClass.getName() + "#" + psiMethod.getName());
            methodTextArea.setText("Dubbo");
        } else {
            pathTextArea.setText(SpringPsiUtils.getPath(psiClass, psiMethod));
            methodTextArea.setText(SpringPsiUtils.getMethod(psiMethod));
        }
    }

    private void initRequestParamTable() {

        Vector<String> vector = new Vector<>();

        DefaultTableModel tableModel = (DefaultTableModel) requestParamTable.getModel();

        // tableModel.setDataVector()


    }


    /**
     *
     */
    @Override
    protected void doOKAction() {

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

        System.out.println("comment = " + comment);

        PsiElementFactory factory = PsiElementFactory.getInstance(project);
        PsiDocComment psiDocComment = factory.createDocCommentFromText(comment);

        writerService.write(project, psiMethod, psiDocComment);

        super.doOKAction();
    }

    @NotNull
    private String buildComment(String docName, List<String> params, String returnTagValue, List<String> exceptions) {
        String comment;
        StringBuilder sb = new StringBuilder();

        sb.append("/**\n");
        sb.append("*\n");
        if (!descText.equals(descTextArea.getText())) {
            sb.append("* ").append(descTextArea.getText()).append("\n");
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
        System.out.println("commentItems = " + sb);

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
            if (!("PsiDocTag:" + docName).equalsIgnoreCase(element.toString())) {
                continue;
            }
            if (!nameText.equals(nameTextArea.getText())) {
                // 设置值
                return docName + " " + nameTextArea.getText();
            }

        }

        // 执行到这里, 说明没找到 tag, 之前没有设置
        if (!psiMethod.getName().equals(nameTextArea.getText())) {
            // 设置值
            return docName + " " + nameTextArea.getText();
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


    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        return rootPanel;
    }
}
