package com.liuzhihang.doc.view.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.ActionLink;
import com.intellij.icons.AllIcons;
import com.intellij.ui.TitledSeparator;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.text.DateFormatUtil;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.service.skill.CodexSkillInstallService;
import com.liuzhihang.doc.view.service.skill.CodexSkillOperationResult;
import com.liuzhihang.doc.view.service.skill.CodexSkillScope;
import com.liuzhihang.doc.view.service.skill.CodexSkillStatus;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import javax.swing.BoxLayout;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/** Compact project settings for installing the supported Codex Skill. */
public final class McpAiSettingsPanel {

    private final Project project;
    private final CodexSkillInstallService service;
    private final JPanel rootPanel = new SettingsRootPanel();
    private final TextFieldWithBrowseButton executableField = new TextFieldWithBrowseButton();
    private JPanel executableBlock;
    private final JBTextArea executableDetectionLabel = new JBTextArea();
    private final JComboBox<ScopeChoice> scopeSelector = new JComboBox<>();
    private final JBTextField targetPathField = new JBTextField();
    private final JBTextArea versionSummaryLabel = new JBTextArea();
    private final JBTextArea statusLabel = new JBTextArea();
    private final JBTextArea lastCheckLabel = new JBTextArea();
    private final JBTextArea operationFeedbackLabel = new JBTextArea();
    private final ActionLink checkUpdatesLink = new ActionLink(DocViewBundle.message("skill.action.check.updates"));
    private final JButton primaryActionButton = new JButton();

    private volatile ProgressIndicator activeIndicator;
    private volatile ProgressIndicator detectionIndicator;
    private volatile boolean disposed;
    private long generation;
    private long detectionGeneration;
    private boolean running;
    private boolean detectingExecutable;
    private boolean executableDetectionFailed;
    private CodexSkillStatus status;
    private Path automaticExecutable;
    private Path validatedExecutable;
    private CodexSkillScope validatedScope;
    private boolean executableChooserVisible;

    public McpAiSettingsPanel(@NotNull Project project) {
        this.project = project;
        this.service = CodexSkillInstallService.getInstance(project);
        // Cached state is not tied to the executable currently selected by automatic discovery.
        // Require an explicit check before showing any client/version-specific result.
        status = unknownStatus(CodexSkillScope.PROJECT);
        detectingExecutable = true;
        executableChooserVisible = false;
        buildUi();
        executableField.setText("");
        renderStatus(status);
        renderExecutableDetection();
        installListeners();
        updateActionAvailability();
        startExecutableDiscovery();
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public void dispose() {
        ProgressIndicator indicator;
        ProgressIndicator executableIndicator;
        synchronized (this) {
            if (disposed) return;
            disposed = true;
            generation++;
            detectionGeneration++;
            indicator = activeIndicator;
            activeIndicator = null;
            executableIndicator = detectionIndicator;
            detectionIndicator = null;
            running = false;
            detectingExecutable = false;
        }
        if (indicator != null) indicator.cancel();
        if (executableIndicator != null) executableIndicator.cancel();
    }

    private void buildUi() {
        rootPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(JBUI.Borders.empty(4, 8));
        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel skillSection = section(DocViewBundle.message("skill.section.title"));
        skillSection.setName("skillSectionHeader");
        addContent(content, skillSection);
        addContent(content, help(DocViewBundle.message("skill.summary"), "skillSummary"));

        executableDetectionLabel.setName("skillCodexDetectionStatus");
        configureHelpArea(executableDetectionLabel);
        addContent(content, executableDetectionLabel);

        executableField.setName("codexExecutableField");
        executableField.getTextField().setColumns(36);
        executableField.getTextField().setEditable(false);
        executableField.getAccessibleContext().setAccessibleName(DocViewBundle.message("skill.codex.executable"));
        executableField.getTextField().getAccessibleContext().setAccessibleName(
                DocViewBundle.message("skill.codex.executable"));
        executableField.setToolTipText(DocViewBundle.message("skill.codex.executable.tip"));
        executableField.setAlignmentX(Component.LEFT_ALIGNMENT);
        FileChooserDescriptor executableDescriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle(DocViewBundle.message("skill.codex.choose.title"))
                .withDescription(DocViewBundle.message("skill.codex.choose.description"))
                .withShowHiddenFiles(true)
                .withLocalFileSystem();
        executableDescriptor.setForcedToUseIdeaFileChooser(true);
        executableField.addBrowseFolderListener(project, executableDescriptor);
        executableBlock = labeled(DocViewBundle.message("skill.codex.executable"), executableField);
        executableBlock.setName("skillExecutableBlock");
        executableBlock.setVisible(executableChooserVisible);
        addContent(content, executableBlock);

        scopeSelector.setName("skillScopeSelector");
        scopeSelector.getAccessibleContext().setAccessibleName(DocViewBundle.message("skill.scope.label"));
        scopeSelector.addItem(new ScopeChoice(CodexSkillScope.PROJECT,
                DocViewBundle.message("skill.scope.project")));
        scopeSelector.addItem(new ScopeChoice(CodexSkillScope.USER,
                DocViewBundle.message("skill.scope.global")));
        scopeSelector.setMaximumSize(scopeSelector.getPreferredSize());
        JPanel scopeBlock = labeled(DocViewBundle.message("skill.scope.label"), scopeSelector);
        scopeBlock.setName("skillScopeBlock");
        addContent(content, scopeBlock);

        targetPathField.setName("skillTargetPathField");
        targetPathField.setColumns(36);
        targetPathField.setEditable(false);
        targetPathField.getAccessibleContext().setAccessibleName(DocViewBundle.message("skill.target.path"));
        targetPathField.setToolTipText(DocViewBundle.message("skill.target.path.tip"));
        JPanel targetBlock = labeled(DocViewBundle.message("skill.target.path"), targetPathField);
        targetBlock.setName("skillTargetBlock");
        addContent(content, targetBlock);

        versionSummaryLabel.setName("skillVersionSummary");
        configureHelpArea(versionSummaryLabel);
        versionSummaryLabel.setColumns(60);
        versionSummaryLabel.setBorder(JBUI.Borders.emptyTop(4));
        versionSummaryLabel.setVisible(false);
        addContent(content, versionSummaryLabel);

        statusLabel.setName("skillStatusLabel");
        configureHelpArea(statusLabel);
        statusLabel.setColumns(60);
        statusLabel.setBorder(JBUI.Borders.emptyTop(4));
        addContent(content, statusLabel);

        lastCheckLabel.setName("skillLastCheckLabel");
        configureHelpArea(lastCheckLabel);
        lastCheckLabel.setColumns(60);
        lastCheckLabel.setBorder(JBUI.Borders.emptyTop(2));
        lastCheckLabel.setVisible(false);
        addContent(content, lastCheckLabel);

        checkUpdatesLink.setName("skillCheckUpdatesAction");
        checkUpdatesLink.setIcon(AllIcons.Actions.Refresh);
        checkUpdatesLink.setIconTextGap(JBUI.scale(4));
        checkUpdatesLink.setAutoHideOnDisable(false);
        checkUpdatesLink.setToolTipText(DocViewBundle.message("skill.action.check.updates.tip"));
        checkUpdatesLink.getAccessibleContext().setAccessibleName(
                DocViewBundle.message("skill.action.check.updates"));
        primaryActionButton.setName("skillPrimaryActionButton");
        primaryActionButton.setToolTipText(DocViewBundle.message("skill.action.install.tip"));
        primaryActionButton.getAccessibleContext().setAccessibleName(DocViewBundle.message("skill.action.install"));
        updatePrimaryActionPresentation();

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(4)));
        actions.setName("skillActions");
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.add(checkUpdatesLink);
        actions.add(primaryActionButton);
        addContent(content, actions);

        operationFeedbackLabel.setName("skillOperationFeedbackLabel");
        configureHelpArea(operationFeedbackLabel);
        operationFeedbackLabel.setColumns(60);
        operationFeedbackLabel.setBorder(JBUI.Borders.emptyTop(2));
        addContent(content, operationFeedbackLabel);

        JPanel mcpSection = section(DocViewBundle.message("skill.mcp.section.title"));
        mcpSection.setName("skillMcpSectionHeader");
        addContent(content, mcpSection);
        addContent(content, help(DocViewBundle.message("skill.mcp.server.info"), "skillMcpServerInfo"));
        rootPanel.add(content, BorderLayout.NORTH);
    }

    private void installListeners() {
        executableField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { executableChanged(); }
            @Override public void removeUpdate(DocumentEvent event) { executableChanged(); }
            @Override public void changedUpdate(DocumentEvent event) { executableChanged(); }
        });
        scopeSelector.addActionListener(event -> scopeChanged());
        checkUpdatesLink.addActionListener(event -> runOperation(Operation.CHECK));
        primaryActionButton.addActionListener(event -> runOperation(primaryOperation()));
    }

    private void startExecutableDiscovery() {
        long request;
        synchronized (this) {
            if (disposed || project.isDisposed()) return;
            detectingExecutable = true;
            request = ++detectionGeneration;
        }
        renderExecutableDetection();
        updateActionAvailability();
        ProgressManager.getInstance().run(new Task.Backgroundable(project,
                DocViewBundle.message("skill.operation.title.detect"), true) {
            private Path candidate;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (!registerDetection(request, indicator)) return;
                indicator.setIndeterminate(true);
                List<Path> candidates = CodexSkillInstallService.codexExecutableCandidates();
                indicator.checkCanceled();
                candidate = candidates.isEmpty() ? null : candidates.getFirst();
            }

            @Override
            public void onSuccess() {
                completeExecutableDiscovery(request, candidate, false);
            }

            @Override
            public void onCancel() {
                completeExecutableDiscovery(request, null, false);
            }

            @Override
            public void onThrowable(@NotNull Throwable throwable) {
                completeExecutableDiscovery(request, null, true);
            }
        });
    }

    private synchronized boolean registerDetection(long request, ProgressIndicator indicator) {
        if (disposed || project.isDisposed() || request != detectionGeneration) {
            indicator.cancel();
            return false;
        }
        detectionIndicator = indicator;
        return true;
    }

    private void completeExecutableDiscovery(long request, Path candidate, boolean failed) {
        synchronized (this) {
            if (disposed || project.isDisposed() || request != detectionGeneration) return;
            detectionIndicator = null;
            detectingExecutable = false;
            detectionGeneration++;
        }
        automaticExecutable = candidate;
        executableDetectionFailed = failed;
        executableChooserVisible = candidate == null;
        if (candidate != null) executableField.setText(candidate.toString());
        setExecutableChooserVisible(candidate == null);
        renderExecutableDetection();
        updateActionAvailability();
    }

    private void executableChanged() {
        Path selected = selectedExecutable();
        if (validatedExecutable == null || !validatedExecutable.equals(selected)) {
            validatedExecutable = null;
            validatedScope = null;
            renderStatus(unknownStatus(selectedScope()));
            operationFeedbackLabel.setText("");
        }
        renderExecutableDetection();
        updateActionAvailability();
    }

    private void scopeChanged() {
        if (disposed || running) return;
        validatedExecutable = null;
        validatedScope = null;
        renderStatus(unknownStatus(selectedScope()));
        renderExecutableDetection();
        operationFeedbackLabel.setText("");
        updateActionAvailability();
    }

    private void runOperation(@NotNull Operation operation) {
        Path executable = selectedExecutable();
        CodexSkillScope operationScope = selectedScope();
        if (executable == null || disposed || project.isDisposed()) return;
        long request = beginOperation();
        if (request < 0) return;

        operationFeedbackLabel.setText(DocViewBundle.message("skill.operation.running"));
        updateActionAvailability();
        String title = DocViewBundle.message(operation.titleKey);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, title, true) {
            private CodexSkillStatus nextStatus;
            private boolean committedWrite;
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (!register(request, indicator)) return;
                indicator.setIndeterminate(true);
                indicator.checkCanceled();
                switch (operation) {
                    case CHECK -> nextStatus = service.checkForUpdates(operationScope, executable, indicator);
                    case INSTALL -> {
                        CodexSkillOperationResult result = service.install(operationScope, executable, indicator);
                        nextStatus = result.status();
                        committedWrite = result.committed();
                    }
                    case UPDATE -> {
                        CodexSkillOperationResult result = service.update(operationScope, executable, indicator);
                        nextStatus = result.status();
                        committedWrite = result.committed();
                    }
                }
                // Each service call returns a terminal status or throws cancellation. A late cancellation
                // after a committed install must not replace the committed state in the UI.
            }

            @Override
            public void onSuccess() {
                if (!finishOperation(request)) return;
                if (nextStatus == null) {
                    operationFeedbackLabel.setText(DocViewBundle.message("skill.operation.failed"));
                    updateActionAvailability();
                    return;
                }
                applyOperationStatus(nextStatus, operation, executable, operationScope);
                operationFeedbackLabel.setText("");
                updateActionAvailability();
            }

            @Override
            public void onCancel() {
                finishCancelledOperation(request, nextStatus, committedWrite, operation, executable, operationScope);
            }

            @Override
            public void onThrowable(@NotNull Throwable throwable) {
                if (!finishOperation(request)) return;
                operationFeedbackLabel.setText(DocViewBundle.message("skill.operation.failed"));
                updateActionAvailability();
            }
        });
    }

    private void applyOperationStatus(CodexSkillStatus nextStatus, Operation operation,
                                     Path executable, CodexSkillScope operationScope) {
        boolean clientVerified = CodexSkillInstallService.VERIFIED_CLIENT_VERSION.equals(nextStatus.clientVersion());
        validatedExecutable = clientVerified ? executable : null;
        validatedScope = clientVerified ? operationScope : null;
        if (clientVerified) {
            automaticExecutable = executable;
            setExecutableChooserVisible(false);
        } else if (operation == Operation.CHECK
                && (nextStatus.state() == CodexSkillStatus.State.CLIENT_UNSUPPORTED
                || (nextStatus.state() == CodexSkillStatus.State.ERROR
                && nextStatus.clientVersion().isBlank()))) {
            setExecutableChooserVisible(true);
            if (executable != null && !executable.toString().equals(executableField.getText())) {
                executableField.setText(executable.toString());
            }
        }
        renderStatus(nextStatus);
        renderExecutableDetection();
    }

    private void finishCancelledOperation(long request, CodexSkillStatus completedStatus, boolean committedWrite,
                                         Operation operation, Path executable, CodexSkillScope operationScope) {
        if (!finishOperation(request)) return;
        if (completedStatus == null) {
            operationFeedbackLabel.setText(DocViewBundle.message("skill.operation.cancelled"));
        } else {
            applyOperationStatus(completedStatus, operation, executable, operationScope);
            // A completed status is authoritative. In particular, committedWrite is set only after the
            // transaction and Codex discovery both succeed, so a late cancellation cannot hide success.
            operationFeedbackLabel.setText(committedWrite && completedStatus.state() != CodexSkillStatus.State.CURRENT
                    ? DocViewBundle.message("skill.operation.failed") : "");
        }
        updateActionAvailability();
    }

    private synchronized long beginOperation() {
        if (disposed || project.isDisposed() || running) return -1;
        running = true;
        return ++generation;
    }

    private synchronized boolean register(long request, ProgressIndicator indicator) {
        if (disposed || project.isDisposed() || request != generation) {
            indicator.cancel();
            return false;
        }
        activeIndicator = indicator;
        return true;
    }

    private synchronized boolean finishOperation(long request) {
        if (disposed || project.isDisposed() || request != generation) return false;
        activeIndicator = null;
        running = false;
        return true;
    }

    private void updateActionAvailability() {
        Path executable = selectedExecutable();
        boolean hasExecutable = executable != null;
        boolean current = !running && validatedExecutable != null && validatedExecutable.equals(executable)
                && validatedScope == selectedScope();
        scopeSelector.setEnabled(!running && !disposed);
        executableField.setEnabled(executableChooserVisible && !running && !detectingExecutable && !disposed);
        checkUpdatesLink.setEnabled(!running && !detectingExecutable && !disposed && hasExecutable);
        primaryActionButton.setEnabled(current && !disposed
                && (primaryOperation() == Operation.UPDATE ? status.canUpdate() : status.canInstall()));
        updatePrimaryActionPresentation();
    }

    private void setExecutableChooserVisible(boolean visible) {
        executableChooserVisible = visible;
        if (executableBlock != null) executableBlock.setVisible(visible);
        rootPanel.revalidate();
        rootPanel.repaint();
    }

    private Operation primaryOperation() {
        return switch (status.state()) {
            case CURRENT, UPDATE_AVAILABLE, MODIFIED_CONFLICT, INCOMPATIBLE, RECOVERY_PENDING -> Operation.UPDATE;
            default -> Operation.INSTALL;
        };
    }

    private void updatePrimaryActionPresentation() {
        Operation operation = primaryOperation();
        String key = operation == Operation.UPDATE ? "skill.action.update" : "skill.action.install";
        String tipKey = operation == Operation.UPDATE ? "skill.action.update.tip" : "skill.action.install.tip";
        String label = DocViewBundle.message(key);
        primaryActionButton.setText(label);
        primaryActionButton.setToolTipText(DocViewBundle.message(tipKey));
        primaryActionButton.getAccessibleContext().setAccessibleName(label);
    }

    private Path selectedExecutable() {
        if (!executableChooserVisible) return automaticExecutable;
        String text = executableField.getText().trim();
        if (text.isEmpty()) return null;
        try {
            return Path.of(text).toAbsolutePath().normalize();
        } catch (RuntimeException invalidPath) {
            return null;
        }
    }

    private void renderStatus(CodexSkillStatus current) {
        status = current;
        targetPathField.setText(current.targetPath().toString());
        targetPathField.setCaretPosition(0);
        targetPathField.setToolTipText(current.targetPath().toString());
        java.util.List<String> knownVersions = new java.util.ArrayList<>(2);
        addKnownVersion(knownVersions, "skill.version.installed", current.installedVersion());
        addKnownVersion(knownVersions, "skill.version.available", current.availableVersion());
        versionSummaryLabel.setText(String.join(" · ", knownVersions));
        versionSummaryLabel.setVisible(!knownVersions.isEmpty());
        statusLabel.setText(localizedStatusMessage(current));
        statusLabel.setToolTipText(current.message().isBlank() ? null : current.message());
        statusLabel.setVisible(current.state() != CodexSkillStatus.State.UNKNOWN);
        if (current.lastSuccessfulCheckAt() == null) {
            lastCheckLabel.setText("");
            lastCheckLabel.setVisible(false);
        } else {
            String formattedTime = DateFormatUtil.formatDateTime(Date.from(current.lastSuccessfulCheckAt()));
            lastCheckLabel.setText(DocViewBundle.message("skill.check.last.success", formattedTime));
            lastCheckLabel.setVisible(current.state() != CodexSkillStatus.State.UNKNOWN);
        }
    }

    private static void addKnownVersion(java.util.List<String> versions, String messageKey, String value) {
        if (value != null && !value.isBlank()) versions.add(DocViewBundle.message(messageKey, value));
    }

    private void renderExecutableDetection() {
        if (detectingExecutable) {
            executableDetectionLabel.setText(DocViewBundle.message("skill.codex.detected.searching"));
            executableDetectionLabel.setToolTipText(null);
            return;
        }
        Path executable = selectedExecutable();
        if (executable == null) {
            executableDetectionLabel.setText(DocViewBundle.message(executableDetectionFailed
                    ? "skill.codex.detected.failed" : "skill.codex.detected.missing"));
            executableDetectionLabel.setToolTipText(null);
            return;
        }
        executableDetectionLabel.setToolTipText(executable.toString());
        if (validatedExecutable != null && validatedExecutable.equals(executable)
                && validatedScope == selectedScope()) {
            executableDetectionLabel.setText(DocViewBundle.message("skill.codex.detected.verified",
                    versionValue(status.clientVersion())));
        } else if (status.state() == CodexSkillStatus.State.PLATFORM_UNSUPPORTED) {
            executableDetectionLabel.setText(DocViewBundle.message("skill.codex.detected.platform.unsupported"));
        } else if (status.state() == CodexSkillStatus.State.CLIENT_UNSUPPORTED) {
            executableDetectionLabel.setText(DocViewBundle.message("skill.codex.detected.unsupported"));
        } else if (status.state() == CodexSkillStatus.State.ERROR && status.clientVersion().isBlank()) {
            executableDetectionLabel.setText(DocViewBundle.message("skill.codex.detected.failed"));
        } else {
            executableDetectionLabel.setText(DocViewBundle.message("skill.codex.detected.pending"));
        }
    }

    private CodexSkillScope selectedScope() {
        ScopeChoice selected = (ScopeChoice) scopeSelector.getSelectedItem();
        return selected == null ? CodexSkillScope.PROJECT : selected.scope();
    }

    private CodexSkillStatus unknownStatus(CodexSkillScope scope) {
        return new CodexSkillStatus(CodexSkillStatus.State.UNKNOWN, "", "",
                CodexSkillInstallService.targetPath(project, scope), "", "", false, false, scope);
    }

    private static String localizedStatusMessage(CodexSkillStatus current) {
        return switch (current.state()) {
            case UNKNOWN -> DocViewBundle.message("skill.status.unknown");
            case PLATFORM_UNSUPPORTED -> DocViewBundle.message("skill.status.platform.unsupported");
            case CLIENT_UNSUPPORTED -> DocViewBundle.message("skill.status.client.unsupported",
                    versionValue(current.clientVersion()), CodexSkillInstallService.VERIFIED_CLIENT_VERSION);
            case UNSUPPORTED_SCOPE -> DocViewBundle.message("skill.status.scope.unsupported");
            case NOT_INSTALLED -> DocViewBundle.message("skill.status.not.installed");
            case CURRENT -> DocViewBundle.message("skill.status.current", versionValue(current.installedVersion()));
            case UPDATE_AVAILABLE -> DocViewBundle.message("skill.status.update.available",
                    versionValue(current.availableVersion()));
            case UNMANAGED_CONFLICT -> DocViewBundle.message("skill.status.unmanaged.conflict");
            case MODIFIED_CONFLICT -> DocViewBundle.message("skill.status.modified.conflict");
            case INCOMPATIBLE -> DocViewBundle.message("skill.status.incompatible",
                    versionValue(current.installedVersion()));
            case NO_COMPATIBLE_RELEASE -> DocViewBundle.message("skill.status.release.unavailable");
            case OFFLINE -> DocViewBundle.message("skill.status.offline");
            case RECOVERY_PENDING -> DocViewBundle.message("skill.status.recovery.pending");
            case ERROR -> DocViewBundle.message("skill.status.error");
        };
    }

    private static String versionValue(String value) {
        return value == null || value.isBlank() ? DocViewBundle.message("skill.version.unknown") : value;
    }

    private JPanel section(String title) {
        TitledSeparator separator = new TitledSeparator(title);
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(separator, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel labeled(String label, JComponent value) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setBorder(JBUI.Borders.emptyTop(4));
        JBLabel caption = new JBLabel(label);
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        caption.setLabelFor(value);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.add(caption);
        block.add(value);
        return block;
    }

    private JBTextArea help(String text, String name) {
        JBTextArea area = new JBTextArea(text);
        area.setName(name);
        configureHelpArea(area);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setBorder(JBUI.Borders.empty(3, 0));
        return area;
    }

    private static void configureHelpArea(JBTextArea area) {
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setFont(JBUI.Fonts.smallFont());
        area.setForeground(UIUtil.getContextHelpForeground());
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private static void addContent(JPanel content, JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(component);
    }

    private enum Operation {
        CHECK("skill.operation.title.check"),
        INSTALL("skill.operation.title.install"),
        UPDATE("skill.operation.title.update");

        private final String titleKey;

        Operation(String titleKey) {
            this.titleKey = titleKey;
        }
    }

    private record ScopeChoice(CodexSkillScope scope, String label) {
        @Override public String toString() { return label; }
    }

    // SettingsConfigurable owns the viewport so explanatory text wraps when the pane is narrow.
    private static final class SettingsRootPanel extends JPanel implements Scrollable {

        private SettingsRootPanel() {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return JBUI.scale(16);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            int visibleSpan = orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
            return Math.max(JBUI.scale(16), visibleSpan - JBUI.scale(16));
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
