package com.liuzhihang.doc.view.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.Settings;
import org.jetbrains.annotations.NotNull;

import javax.accessibility.AccessibleContext;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.openapi.options.ConfigurationException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Controls the configurable sources used while creating API documentation.
 */
public final class GenerationRulesForm {

    private final Project project;
    private final JPanel rootPanel = new JPanel();
    private final List<JCheckBox> controls = new ArrayList<>();

    private final JCheckBox titleCommentTagCheckBox = checkBox("settings.doc.title.comment.tag", "titleCommentTagCheckBox");
    private final JCheckBox titleClassCommentCheckBox = checkBox("settings.doc.title.class.comment", "titleClassCommentCheckBox");
    private final JCheckBox titleSimpleClassNameCheckBox = checkBox("settings.doc.title.simple", "titleSimpleClassNameCheckBox");
    private final JCheckBox titleFullClassNameCheckBox = checkBox("settings.doc.title.full", "titleFullClassNameCheckBox");
    private final JCheckBox nameSwagger3CheckBox = checkBox("settings.doc.name.swagger3", "nameSwagger3CheckBox");
    private final JCheckBox nameSwaggerCheckBox = checkBox("settings.doc.name.swagger", "nameSwaggerCheckBox");
    private final JCheckBox nameCommentTagCheckBox = checkBox("settings.doc.name.comment.tag", "nameCommentTagCheckBox");
    private final JCheckBox nameMethodCommentCheckBox = checkBox("settings.doc.name.method.comment", "nameMethodCommentCheckBox");
    private final JCheckBox descSwagger3CheckBox = checkBox("settings.doc.desc.swagger3", "descSwagger3CheckBox");
    private final JCheckBox descSwaggerCheckBox = checkBox("settings.doc.desc.swagger", "descSwaggerCheckBox");
    private final JCheckBox requireCommentTagCheckBox = checkBox("settings.doc.required.comment.tag", "requireCommentTagCheckBox");
    private final JCheckBox fieldNameJsonPropertyCheckBox = checkBox("settings.doc.field.name.JsonProperty", "fieldNameJsonPropertyCheckBox");

    private final JCheckBox nameCommentFirstLineCheckBox = checkBox("settings.doc.name.comment.first.line", "nameCommentFirstLineCheckBox");
    private final JCheckBox descCommentBodyCheckBox = checkBox("settings.doc.desc.comment.body", "descCommentBodyCheckBox");
    private final JBTextField commentTitleMaxLengthField = new JBTextField(4);

    public GenerationRulesForm(@NotNull Project project) {
        this.project = project;
        rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.Y_AXIS));
        rootPanel.setBorder(JBUI.Borders.empty(8, 12));
        rootPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        rootPanel.add(group("settings.doc.title", "generationTitle", "settings.rules.title.summary",
                "settings.rules.title.help.name", DocViewBundle.message("settings.rules.title.order"),
                titleCommentTagCheckBox, titleClassCommentCheckBox,
                titleSimpleClassNameCheckBox, titleFullClassNameCheckBox));
        JPanel limitRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        limitRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        commentTitleMaxLengthField.setName("commentTitleMaxLengthField");
        JLabel limitLabel = new JLabel(DocViewBundle.message("settings.doc.comment.title.limit"));
        limitLabel.setLabelFor(commentTitleMaxLengthField);
        limitRow.add(limitLabel);
        limitRow.add(commentTitleMaxLengthField);
        rootPanel.add(limitRow);
        nameCommentFirstLineCheckBox.setBorder(JBUI.Borders.emptyLeft(20));
        nameMethodCommentCheckBox.addActionListener(event -> nameCommentFirstLineCheckBox.setEnabled(nameMethodCommentCheckBox.isSelected()));
        rootPanel.add(group("settings.doc.name", "generationName", "settings.rules.name.summary",
                "settings.rules.name.help.name", DocViewBundle.message("settings.rules.name.order"),
                nameSwagger3CheckBox, nameSwaggerCheckBox,
                nameCommentTagCheckBox, nameMethodCommentCheckBox, nameCommentFirstLineCheckBox));
        rootPanel.add(group("settings.doc.desc", "generationDescription", "settings.rules.desc.summary",
                "settings.rules.desc.help.name", DocViewBundle.message("settings.rules.desc.order"),
                descSwagger3CheckBox, descSwaggerCheckBox, descCommentBodyCheckBox));

        String requiredHelp = String.join("\n\n",
                DocViewBundle.message("settings.rules.required.fixed"),
                DocViewBundle.message("settings.rules.required.fields"),
                DocViewBundle.message("settings.rules.required.parameters"));
        rootPanel.add(group("settings.doc.required", "generationRequired", "settings.rules.required.summary",
                "settings.rules.required.help.name", requiredHelp, requireCommentTagCheckBox));
        rootPanel.add(group("settings.doc.field.name", "generationFieldName", "settings.rules.field.name.summary",
                "settings.rules.field.name.help.name", DocViewBundle.message("settings.rules.field.name.order"),
                fieldNameJsonPropertyCheckBox));

        JPanel guideRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        guideRow.setOpaque(false);
        guideRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        guideRow.add(SettingsGuideHelp.create(project, "settingsGuideHelpIcon",
                "settings.help.description", "settings.help.open", "/docs/user-guide/settings.md"));
        rootPanel.add(guideRow, 0);
    }

    private JPanel group(String titleKey, String groupName, String summaryKey, String helpAccessibleNameKey,
                         String detailedHelp, JCheckBox... options) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setName(groupName + "Group");
        group.setOpaque(false);
        group.setBorder(JBUI.Borders.empty(0, 0, 8, 0));
        group.setAlignmentX(Component.LEFT_ALIGNMENT);

        TitledSeparator separator = new TitledSeparator(DocViewBundle.message(titleKey));
        separator.setName(groupName + "Separator");
        if (options.length > 0) {
            separator.setLabelFor(options[0]);
        }

        ContextHelpLabel helpIcon = ContextHelpLabel.create(toTooltipHtml(detailedHelp));
        helpIcon.setName(groupName + "HelpIcon");
        AccessibleContext accessibleContext = helpIcon.getAccessibleContext();
        accessibleContext.setAccessibleName(DocViewBundle.message(helpAccessibleNameKey));
        accessibleContext.setAccessibleDescription(detailedHelp);

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(separator, BorderLayout.CENTER);
        heading.add(helpIcon, BorderLayout.EAST);
        group.add(heading);

        for (JCheckBox option : options) {
            option.setAlignmentX(Component.LEFT_ALIGNMENT);
            group.add(option);
        }
        group.add(summary(summaryKey, groupName + "Summary"));
        return group;
    }

    private static String toTooltipHtml(String detailedHelp) {
        String escaped = detailedHelp.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        return escaped.replace("\n\n", "<p>").replace("\n", "<br>");
    }

    private static JBTextArea summary(String messageKey, String name) {
        JBTextArea help = new JBTextArea(DocViewBundle.message(messageKey));
        help.setName(name);
        help.setEditable(false);
        help.setFocusable(false);
        help.setOpaque(false);
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setRows(2);
        help.setFont(JBUI.Fonts.smallFont());
        help.setForeground(UIUtil.getContextHelpForeground());
        help.setAlignmentX(Component.LEFT_ALIGNMENT);
        help.setBorder(JBUI.Borders.empty(2, 0, 0, 0));
        return help;
    }

    private JCheckBox checkBox(String messageKey, String name) {
        JCheckBox checkBox = new JCheckBox(DocViewBundle.message(messageKey));
        checkBox.setName(name);
        controls.add(checkBox);
        return checkBox;
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    public boolean isModified() {
        Settings settings = Settings.getInstance(project);
        return !Integer.toString(settings.getCommentTitleMaxLength()).equals(commentTitleMaxLengthField.getText().trim())
                || nameCommentFirstLineCheckBox.isSelected() != settings.isNameCommentFirstLine()
                || descCommentBodyCheckBox.isSelected() != settings.isDescCommentBody()
                || titleCommentTagCheckBox.isSelected() != settings.getTitleUseCommentTag()
                || titleFullClassNameCheckBox.isSelected() != settings.getTitleUseFullClassName()
                || titleSimpleClassNameCheckBox.isSelected() != settings.getTitleUseSimpleClassName()
                || titleClassCommentCheckBox.isSelected() != settings.getTitleClassComment()
                || nameSwagger3CheckBox.isSelected() != settings.getNameUseSwagger3()
                || nameSwaggerCheckBox.isSelected() != settings.getNameUseSwagger()
                || nameCommentTagCheckBox.isSelected() != settings.getNameUseCommentTag()
                || nameMethodCommentCheckBox.isSelected() != settings.getNameMethodComment()
                || descSwagger3CheckBox.isSelected() != settings.getDescUseSwagger3()
                || descSwaggerCheckBox.isSelected() != settings.getDescUseSwagger()
                || requireCommentTagCheckBox.isSelected() != settings.getRequiredUseCommentTag()
                || fieldNameJsonPropertyCheckBox.isSelected() != settings.getFieldNameJsonProperty();
    }

    public void apply() throws ConfigurationException {
        final int limit;
        try {
            limit = Integer.parseInt(commentTitleMaxLengthField.getText().trim());
            if (limit < 1 || limit > 200) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            throw new ConfigurationException(DocViewBundle.message("settings.doc.comment.title.limit.error"));
        }
        Settings settings = Settings.getInstance(project);
        settings.setCommentTitleMaxLength(limit);
        settings.setNameCommentFirstLine(nameCommentFirstLineCheckBox.isSelected());
        settings.setDescCommentBody(descCommentBodyCheckBox.isSelected());
        settings.setTitleUseCommentTag(titleCommentTagCheckBox.isSelected());
        settings.setTitleUseFullClassName(titleFullClassNameCheckBox.isSelected());
        settings.setTitleUseSimpleClassName(titleSimpleClassNameCheckBox.isSelected());
        settings.setTitleClassComment(titleClassCommentCheckBox.isSelected());
        settings.setNameUseSwagger3(nameSwagger3CheckBox.isSelected());
        settings.setNameUseSwagger(nameSwaggerCheckBox.isSelected());
        settings.setNameUseCommentTag(nameCommentTagCheckBox.isSelected());
        settings.setNameMethodComment(nameMethodCommentCheckBox.isSelected());
        settings.setDescUseSwagger3(descSwagger3CheckBox.isSelected());
        settings.setDescUseSwagger(descSwaggerCheckBox.isSelected());
        settings.setRequiredUseCommentTag(requireCommentTagCheckBox.isSelected());
        settings.setFieldNameJsonProperty(fieldNameJsonPropertyCheckBox.isSelected());
    }

    public void reset() {
        Settings settings = Settings.getInstance(project);
        commentTitleMaxLengthField.setText(Integer.toString(settings.getCommentTitleMaxLength()));
        nameCommentFirstLineCheckBox.setSelected(settings.isNameCommentFirstLine());
        nameCommentFirstLineCheckBox.setEnabled(settings.getNameMethodComment());
        descCommentBodyCheckBox.setSelected(settings.isDescCommentBody());
        titleCommentTagCheckBox.setSelected(settings.getTitleUseCommentTag());
        titleFullClassNameCheckBox.setSelected(settings.getTitleUseFullClassName());
        titleSimpleClassNameCheckBox.setSelected(settings.getTitleUseSimpleClassName());
        titleClassCommentCheckBox.setSelected(settings.getTitleClassComment());
        nameSwagger3CheckBox.setSelected(settings.getNameUseSwagger3());
        nameSwaggerCheckBox.setSelected(settings.getNameUseSwagger());
        nameCommentTagCheckBox.setSelected(settings.getNameUseCommentTag());
        nameMethodCommentCheckBox.setSelected(settings.getNameMethodComment());
        descSwagger3CheckBox.setSelected(settings.getDescUseSwagger3());
        descSwaggerCheckBox.setSelected(settings.getDescUseSwagger());
        requireCommentTagCheckBox.setSelected(settings.getRequiredUseCommentTag());
        fieldNameJsonPropertyCheckBox.setSelected(settings.getFieldNameJsonProperty());
    }
}
