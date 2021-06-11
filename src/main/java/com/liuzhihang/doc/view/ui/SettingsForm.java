package com.liuzhihang.doc.view.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ui.JBUI;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.Settings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;

/**
 * @author liuzhihang
 * @date 2020/2/26 19:17
 */
public class SettingsForm {

    private static final TitledBorder titleTitleBorder = IdeBorderFactory.createTitledBorder(DocViewBundle.message("settings.doc.title"));
    private static final TitledBorder nameTitleBorder = IdeBorderFactory.createTitledBorder(DocViewBundle.message("settings.doc.name"));
    private static final TitledBorder descTitleBorder = IdeBorderFactory.createTitledBorder(DocViewBundle.message("settings.doc.desc"));
    private static final TitledBorder requiredTitleBorder = IdeBorderFactory.createTitledBorder(DocViewBundle.message("settings.doc.required"));

    private final Project project;

    private JPanel rootPanel;

    private LinkLabel<String> supportLinkLabel;

    private JPanel titlePanel;
    private JCheckBox titleCommentTagCheckBox;
    private JCheckBox titleFullClassNameCheckBox;
    private JCheckBox titleSimpleClassNameCheckBox;

    private JPanel namePanel;
    private JCheckBox nameSwagger3CheckBox;
    private JCheckBox nameSwaggerCheckBox;
    private JCheckBox nameCommentTagCheckBox;

    private JPanel docDescPanel;
    private JCheckBox descSwagger3CheckBox;
    private JCheckBox descSwaggerCheckBox;

    private JPanel requirePanel;
    private JCheckBox requireCommentTagCheckBox;

    public SettingsForm(@NotNull Project project) {

        this.project = project;

        supportLinkLabel.setBorder(JBUI.Borders.emptyTop(20));
        supportLinkLabel.setIcon(AllIcons.Actions.Find);

        supportLinkLabel.setListener((source, data) -> new SupportForm().show(), null);

        initTitle();
        initName();
        initDesc();
        initRequired();

    }


    private void initTitle() {

        titlePanel.setBorder(titleTitleBorder);
    }

    private void initName() {

        namePanel.setBorder(nameTitleBorder);
    }

    private void initDesc() {

        docDescPanel.setBorder(descTitleBorder);
    }

    private void initRequired() {

        requirePanel.setBorder(requiredTitleBorder);

    }

    public boolean isModified() {

        Settings settings = Settings.getInstance(project);

        return titleCommentTagCheckBox.isSelected() != settings.getTitleUseCommentTag()
                || titleFullClassNameCheckBox.isSelected() != settings.getTitleUseFullClassName()
                || titleSimpleClassNameCheckBox.isSelected() != settings.getTitleUseSimpleClassName()
                || nameSwagger3CheckBox.isSelected() != settings.getNameUseSwagger3()
                || nameSwaggerCheckBox.isSelected() != settings.getNameUseSwagger()
                || nameCommentTagCheckBox.isSelected() != settings.getNameUseCommentTag()
                || descSwagger3CheckBox.isSelected() != settings.getDescUseSwagger3()
                || descSwaggerCheckBox.isSelected() != settings.getDescUseSwagger()
                || requireCommentTagCheckBox.isSelected() != settings.getRequiredUseCommentTag();
    }

    public void apply() {

        Settings settings = Settings.getInstance(project);
        settings.setTitleUseCommentTag(titleCommentTagCheckBox.isSelected());
        settings.setTitleUseFullClassName(titleFullClassNameCheckBox.isSelected());
        settings.setTitleUseSimpleClassName(titleSimpleClassNameCheckBox.isSelected());
        settings.setNameUseSwagger3(nameSwagger3CheckBox.isSelected());
        settings.setNameUseSwagger(nameSwaggerCheckBox.isSelected());
        settings.setNameUseCommentTag(nameCommentTagCheckBox.isSelected());
        settings.setDescUseSwagger3(descSwagger3CheckBox.isSelected());
        settings.setDescUseSwagger(descSwaggerCheckBox.isSelected());
        settings.setRequiredUseCommentTag(requireCommentTagCheckBox.isSelected());

    }

    public void reset() {
        Settings settings = Settings.getInstance(project);
        titleCommentTagCheckBox.setSelected(settings.getTitleUseCommentTag());
        titleFullClassNameCheckBox.setSelected(settings.getTitleUseFullClassName());
        titleSimpleClassNameCheckBox.setSelected(settings.getTitleUseSimpleClassName());
        nameSwagger3CheckBox.setSelected(settings.getNameUseSwagger3());
        nameSwaggerCheckBox.setSelected(settings.getNameUseSwagger());
        nameCommentTagCheckBox.setSelected(settings.getNameUseCommentTag());
        descSwagger3CheckBox.setSelected(settings.getDescUseSwagger3());
        descSwaggerCheckBox.setSelected(settings.getDescUseSwagger());
        requireCommentTagCheckBox.setSelected(settings.getRequiredUseCommentTag());

    }


    public JPanel getRootPanel() {
        return rootPanel;
    }
}
