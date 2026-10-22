package com.liuzhihang.doc.view.ui;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.util.ui.JBUI;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.WindowSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author liuzhihang
 * @date 2021/10/25 12:11
 */
public class WindowFilterForm {

    private final Project project;
    private final WindowSettings windowSettings;

    private JCheckBox interfaceCheckBox;
    private JComboBox<String> scopeComboBox;
    private JPanel rootPane;
    private JPanel scopePanel;
    private JPanel otherSettingsPanel;

    public WindowFilterForm(@NotNull Project project) {
        this.project = project;
        this.windowSettings = WindowSettings.getInstance(project);

        initUI();
        initFilterPane();
        initFilterListener();
    }

    private void initUI() {

        scopePanel.setBorder(IdeBorderFactory.createTitledBorder(DocViewBundle.message("settings.window.scope")));
        otherSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder(DocViewBundle.message("settings.window.other")));
        interfaceCheckBox.setText(DocViewBundle.message("settings.window.interface"));
        interfaceCheckBox.setToolTipText(DocViewBundle.message("settings.window.interface.tip"));
        rootPane.setBorder(JBUI.Borders.empty(5));


    }

    private void initFilterPane() {

        interfaceCheckBox.setSelected(windowSettings.isIncludeInterface());

        Module[] modules = ModuleManager.getInstance(project).getModules();

        for (Module module : modules) {
            scopeComboBox.addItem(module.getName());
        }

    }

    private void initFilterListener() {
        interfaceCheckBox.addChangeListener(e -> windowSettings.setIncludeInterface(interfaceCheckBox.isSelected()));
        scopeComboBox.addItemListener(e -> windowSettings.setScope((String) scopeComboBox.getSelectedItem()));
    }

}
