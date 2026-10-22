package com.liuzhihang.doc.view.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.Settings;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

/**
 * The project-wide behavior settings shown on the Doc View root page.
 */
public final class SettingsForm {

    private final Project project;
    private final JPanel rootPanel = new JPanel(new BorderLayout());
    private final JCheckBox lineMarkerCheckBox = new JCheckBox();
    private final JCheckBox includeNormalInterfaceCheckBox = new JCheckBox();

    public SettingsForm(@NotNull Project project) {
        this.project = project;
        rootPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        lineMarkerCheckBox.setName("lineMarkerCheckBox");
        lineMarkerCheckBox.setText(DocViewBundle.message("settings.doc.setting.line.marker.show"));
        lineMarkerCheckBox.setToolTipText(DocViewBundle.message("settings.doc.setting.line.marker.show.tip"));

        includeNormalInterfaceCheckBox.setName("includeNormalInterfaceCheckBox");
        includeNormalInterfaceCheckBox.setText(DocViewBundle.message("settings.doc.setting.include.normal.interface"));
        includeNormalInterfaceCheckBox.setToolTipText(DocViewBundle.message("settings.doc.setting.include.normal.interface.tip"));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(JBUI.Borders.empty(8, 12));
        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel behaviorPanel = new JPanel();
        behaviorPanel.setLayout(new BoxLayout(behaviorPanel, BoxLayout.Y_AXIS));
        behaviorPanel.setBorder(JBUI.Borders.emptyBottom(8));
        behaviorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        com.intellij.ui.TitledSeparator separator = new com.intellij.ui.TitledSeparator(
                DocViewBundle.message("settings.page.behavior"));
        separator.setName("settingsBehaviorSeparator");
        ContextHelpLabel guide = SettingsGuideHelp.create(project, "settingsGuideHelpIcon",
                "settings.help.description", "settings.help.open", "/docs/user-guide/settings.md");
        heading.add(separator, BorderLayout.CENTER);
        heading.add(guide, BorderLayout.EAST);
        behaviorPanel.add(heading);
        behaviorPanel.add(lineMarkerCheckBox);
        lineMarkerCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        behaviorPanel.add(wrappingHelp(DocViewBundle.message("settings.doc.setting.line.marker.show.tip"),
                "settingsLineMarkerDescription"));
        JPanel interfaceOption = new JPanel(new BorderLayout());
        interfaceOption.setBorder(JBUI.Borders.emptyBottom(8));
        interfaceOption.setAlignmentX(Component.LEFT_ALIGNMENT);
        includeNormalInterfaceCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        interfaceOption.add(includeNormalInterfaceCheckBox, BorderLayout.WEST);
        behaviorPanel.add(interfaceOption);
        behaviorPanel.add(wrappingHelp(DocViewBundle.message("settings.doc.setting.include.normal.interface.tip"),
                "settingsNormalInterfaceDescription"));
        content.add(behaviorPanel);

        JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, JBUI.scale(8)));
        links.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton supportButton = new JButton(DocViewBundle.message("support.start"));
        supportButton.addActionListener(event -> new SupportForm(project).show());
        links.add(supportButton);
        content.add(links);

        rootPanel.add(content, BorderLayout.NORTH);
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    public boolean isModified() {
        Settings settings = Settings.getInstance(project);
        return lineMarkerCheckBox.isSelected() != settings.getLineMarker()
                || includeNormalInterfaceCheckBox.isSelected() != settings.getIncludeNormalInterface();
    }

    public void apply() {
        Settings settings = Settings.getInstance(project);
        settings.setLineMarker(lineMarkerCheckBox.isSelected());
        settings.setIncludeNormalInterface(includeNormalInterfaceCheckBox.isSelected());
    }

    public void reset() {
        Settings settings = Settings.getInstance(project);
        lineMarkerCheckBox.setSelected(settings.getLineMarker());
        includeNormalInterfaceCheckBox.setSelected(settings.getIncludeNormalInterface());
        includeNormalInterfaceCheckBox.setEnabled(true);
    }

    private static JBTextArea wrappingHelp(String text, String name) {
        JBTextArea help = new JBTextArea(text);
        help.setName(name);
        help.setEditable(false);
        help.setFocusable(false);
        help.setOpaque(false);
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setAlignmentX(Component.LEFT_ALIGNMENT);
        help.setBorder(JBUI.Borders.empty(2, 0));
        return help;
    }
}
