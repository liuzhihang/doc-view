package com.liuzhihang.doc.view.contract;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.McpAiConfigurable;
import com.liuzhihang.doc.view.service.skill.CodexSkillScope;
import com.liuzhihang.doc.view.ui.McpAiSettingsPanel;
import com.liuzhihang.doc.view.service.skill.CodexSkillInstallService;
import com.liuzhihang.doc.view.service.skill.CodexSkillStatus;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

public final class CodexSkillSettingsTest extends LightJavaCodeInsightFixtureTestCase {

    private String oldIgnoreHeadlessProperty;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        oldIgnoreHeadlessProperty = System.getProperty("intellij.progress.task.ignoreHeadless");
        System.setProperty("intellij.progress.task.ignoreHeadless", "true");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (oldIgnoreHeadlessProperty == null) {
                System.clearProperty("intellij.progress.task.ignoreHeadless");
            } else {
                System.setProperty("intellij.progress.task.ignoreHeadless", oldIgnoreHeadlessProperty);
            }
        } finally {
            super.tearDown();
        }
    }

    public void testMcpAiPageIsProjectScopedAndShowsItsExactInstallTarget() throws Exception {
        McpAiConfigurable configurable = new McpAiConfigurable(getProject());
        CodexSkillStatus status = CodexSkillInstallService.getStatus(getProject(), CodexSkillScope.PROJECT);
        try {
            assertEquals("doc.view.mcp.ai", configurable.getId());
            assertEquals(DocViewBundle.message("settings.page.mcp.ai"), configurable.getDisplayName());

            JComponent root = configurable.createComponent();
            JComboBox<?> scope = component(root, "skillScopeSelector", JComboBox.class);
            JBTextField target = component(root, "skillTargetPathField", JBTextField.class);
            JBTextArea statusLabel = component(root, "skillStatusLabel", JBTextArea.class);
            JBTextArea versionSummary = component(root, "skillVersionSummary", JBTextArea.class);

            assertEquals(DocViewBundle.message("skill.scope.project"), scope.getSelectedItem().toString());
            assertEquals(DocViewBundle.message("skill.scope.label"),
                    scope.getAccessibleContext().getAccessibleName());
            assertEquals(DocViewBundle.message("skill.target.path"),
                    target.getAccessibleContext().getAccessibleName());
            assertEquals(status.targetPath().toString(), target.getText());
            assertFalse("The computed install target is read-only", target.isEditable());
            assertEquals("The readonly path field should initially reveal the beginning of a long path",
                    0, target.getCaretPosition());
            assertFalse("Unknown status would duplicate the automatic-detection hint", statusLabel.isVisible());
            assertFalse("Unknown versions are hidden until a client check completes", versionSummary.isVisible());
            assertTrue("The candidate target must remain under this IDEA project's repo skill root",
                    status.targetPath().startsWith(Path.of(getProject().getBasePath()).resolve(".agents/skills")));
            assertFalse("Opening Settings must not create the Skill target", Files.exists(status.targetPath()));

            scope.setSelectedIndex(1);
            Path globalTarget = CodexSkillInstallService.targetPath(getProject(), CodexSkillScope.USER);
            assertEquals(DocViewBundle.message("skill.scope.global"), scope.getSelectedItem().toString());
            assertEquals(globalTarget.toString(), target.getText());
            assertEquals(Path.of(System.getProperty("user.home")).resolve(".agents/skills/doc-view"), globalTarget);
            assertFalse("Changing scope invalidates any unverified cached state", statusLabel.isVisible());
            assertFalse(versionSummary.isVisible());
            assertFalse("This page has no persisted settings draft", configurable.isModified());
        } finally {
            configurable.disposeUIResources();
        }
    }

    public void testCodexExecutableDetectionRunsInBackgroundAndKeepsPathOutOfSummary() throws Exception {
        McpAiConfigurable configurable = new McpAiConfigurable(getProject());
        CodexSkillStatus status = CodexSkillInstallService.getStatus(getProject(), CodexSkillScope.PROJECT);
        try {
            JComponent root = configurable.createComponent();
            TextFieldWithBrowseButton executable = component(root, "codexExecutableField",
                    TextFieldWithBrowseButton.class);
            ActionLink check = component(root, "skillCheckUpdatesAction", ActionLink.class);
            JButton primary = component(root, "skillPrimaryActionButton", JButton.class);
            JPanel executableBlock = component(root, "skillExecutableBlock", JPanel.class);
            JBTextArea detection = component(root, "skillCodexDetectionStatus", JBTextArea.class);
            JBTextArea statusLabel = component(root, "skillStatusLabel", JBTextArea.class);
            JBTextArea versionSummary = component(root, "skillVersionSummary", JBTextArea.class);

            assertEquals("Before the background resolver completes, the UI must not claim that a CLI was found",
                    DocViewBundle.message("skill.codex.detected.searching"), detection.getText());
            assertTrue(executable.getText().isBlank());
            assertFalse(executableBlock.isVisible());
            assertFalse(check.isEnabled());
            assertFalse("Do not show an unknown status next to the pending-detection hint", statusLabel.isVisible());
            assertFalse("Unknown versions should not be repeated beside the pending-check hint",
                    versionSummary.isVisible());
            assertEquals(DocViewBundle.message("skill.action.install"), primary.getText());
            assertFalse("Unknown state must keep the Install action disabled", primary.isEnabled());
            assertEquals(DocViewBundle.message("skill.action.install"),
                    primary.getAccessibleContext().getAccessibleName());
            assertNotNull(primary.getToolTipText());
            awaitDetection(detection);
            if (DocViewBundle.message("skill.codex.detected.pending").equals(detection.getText())) {
                assertFalse("Hide manual selection when automatic discovery found the CLI", executableBlock.isVisible());
                assertFalse(executable.getText().isBlank());
                assertEquals(DocViewBundle.message("skill.codex.detected.pending"), detection.getText());
                assertEquals(executable.getText(), detection.getToolTipText());
                assertFalse("The discovered executable path belongs in the tooltip, not the visible summary",
                        detection.getText().contains(executable.getText()));
                assertTrue(check.isEnabled());
            } else {
                assertTrue("Show the chooser only when automatic discovery did not find a usable CLI",
                        executableBlock.isVisible());
                assertTrue(executable.getText().isBlank());
                assertTrue(DocViewBundle.message("skill.codex.detected.missing").equals(detection.getText())
                        || DocViewBundle.message("skill.codex.detected.failed").equals(detection.getText()));
                assertFalse(check.isEnabled());
            }
            assertEquals(DocViewBundle.message("skill.action.check.updates"),
                    check.getAccessibleContext().getAccessibleName());
            assertNotNull(check.getToolTipText());
            assertFalse("Rendering the page must not create the target directory", Files.exists(status.targetPath()));
            assertFalse("Opening the page must not start a background operation", configurable.isModified());
        } finally {
            configurable.disposeUIResources();
        }
    }

    private void awaitDetection(JBTextArea label) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            PlatformTestUtil.waitForAllBackgroundActivityToCalmDown();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            if (!DocViewBundle.message("skill.codex.detected.searching").equals(label.getText())) return;
            Thread.sleep(40);
        }
        fail("Codex CLI detection did not finish in background");
    }

    public void testDisposedSettingsPanelDropsLateCliDiscoveryResult() throws Exception {
        McpAiSettingsPanel panel = new McpAiSettingsPanel(getProject());
        JBTextArea detection = component(panel.getRootPanel(), "skillCodexDetectionStatus", JBTextArea.class);
        try {
            awaitDetection(detection);
            Field executable = McpAiSettingsPanel.class.getDeclaredField("automaticExecutable");
            executable.setAccessible(true);
            Path before = (Path) executable.get(panel);
            String detectionText = detection.getText();
            Field requestField = McpAiSettingsPanel.class.getDeclaredField("detectionGeneration");
            requestField.setAccessible(true);
            long staleRequest = requestField.getLong(panel) - 1;

            panel.dispose();
            Method complete = McpAiSettingsPanel.class.getDeclaredMethod("completeExecutableDiscovery",
                    long.class, Path.class, boolean.class);
            complete.setAccessible(true);
            complete.invoke(panel, staleRequest, Path.of("/isolated-test/late-codex"), false);

            assertEquals("A disposed panel must ignore a late detected path", before, executable.get(panel));
            assertEquals("A disposed panel must not update visible state", detectionText, detection.getText());
        } finally {
            panel.dispose();
        }
    }

    public void testOpeningSettingsDoesNotReuseCachedStatusWithoutExecutableIdentity() throws Exception {
        Path target = CodexSkillInstallService.targetPath(getProject(), CodexSkillScope.PROJECT);
        CodexSkillStatus previousCheck = new CodexSkillStatus(CodexSkillStatus.State.UPDATE_AVAILABLE,
                "cached status from a previous executable", CodexSkillInstallService.VERIFIED_CLIENT_VERSION,
                target, "1.0.0", "1.1.0", false, true, CodexSkillScope.PROJECT);
        Method publish = CodexSkillInstallService.class.getDeclaredMethod("publish", CodexSkillStatus.class);
        publish.setAccessible(true);
        publish.invoke(CodexSkillInstallService.getInstance(getProject()), previousCheck);

        McpAiConfigurable configurable = new McpAiConfigurable(getProject());
        try {
            JComponent root = configurable.createComponent();
            JBTextArea status = component(root, "skillStatusLabel", JBTextArea.class);
            JBTextArea versions = component(root, "skillVersionSummary", JBTextArea.class);
            JButton primary = component(root, "skillPrimaryActionButton", JButton.class);

            assertEquals("The test starts with a cached successful status", previousCheck,
                    CodexSkillInstallService.getStatus(getProject(), CodexSkillScope.PROJECT));
            assertFalse("A cached status is not associated with the executable auto-detected for this view",
                    status.isVisible());
            assertFalse("Do not show versions from a previous executable before the user checks this view",
                    versions.isVisible());
            assertFalse("The cached UPDATE_AVAILABLE status must not enable a write action",
                    primary.isEnabled());
            assertEquals(DocViewBundle.message("skill.action.install"), primary.getText());
            assertFalse("Opening settings must remain read-only", Files.exists(target));
        } finally {
            configurable.disposeUIResources();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testUnknownStatusRetainsGlobalScope() throws Exception {
        McpAiSettingsPanel panel = new McpAiSettingsPanel(getProject());
        try {
            Method unknownStatus = McpAiSettingsPanel.class.getDeclaredMethod("unknownStatus", CodexSkillScope.class);
            unknownStatus.setAccessible(true);
            CodexSkillStatus status = (CodexSkillStatus) unknownStatus.invoke(panel, CodexSkillScope.USER);
            assertEquals(CodexSkillScope.USER, status.scope());
            assertEquals(CodexSkillInstallService.targetPath(getProject(), CodexSkillScope.USER), status.targetPath());
        } finally {
            panel.dispose();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testUnsupportedPlatformDoesNotOfferAnotherCliExecutable() throws Exception {
        McpAiSettingsPanel panel = new McpAiSettingsPanel(getProject());
        Path executable = Path.of("/isolated-test/codex");
        Path target = CodexSkillInstallService.targetPath(getProject(), CodexSkillScope.PROJECT);
        CodexSkillStatus unsupported = new CodexSkillStatus(CodexSkillStatus.State.PLATFORM_UNSUPPORTED,
                "platform is not verified", "", target, "", "", false, false, CodexSkillScope.PROJECT);
        try {
            awaitDetection(component(panel.getRootPanel(), "skillCodexDetectionStatus", JBTextArea.class));
            Method chooserVisibility = McpAiSettingsPanel.class.getDeclaredMethod(
                    "setExecutableChooserVisible", boolean.class);
            chooserVisibility.setAccessible(true);
            chooserVisibility.invoke(panel, false);
            Field automaticExecutable = McpAiSettingsPanel.class.getDeclaredField("automaticExecutable");
            automaticExecutable.setAccessible(true);
            automaticExecutable.set(panel, executable);
            Class<?> operationType = Class.forName(McpAiSettingsPanel.class.getName() + "$Operation");
            Object check = Enum.valueOf((Class) operationType, "CHECK");
            Method apply = McpAiSettingsPanel.class.getDeclaredMethod("applyOperationStatus", CodexSkillStatus.class,
                    operationType, Path.class, CodexSkillScope.class);
            apply.setAccessible(true);
            apply.invoke(panel, unsupported, check, executable, CodexSkillScope.PROJECT);

            JPanel executableBlock = component(panel.getRootPanel(), "skillExecutableBlock", JPanel.class);
            JBTextArea detection = component(panel.getRootPanel(), "skillCodexDetectionStatus", JBTextArea.class);
            JBTextArea status = component(panel.getRootPanel(), "skillStatusLabel", JBTextArea.class);
            JBTextArea versions = component(panel.getRootPanel(), "skillVersionSummary", JBTextArea.class);
            assertFalse("An unsupported host is not a CLI version problem", executableBlock.isVisible());
            assertEquals(DocViewBundle.message("skill.codex.detected.platform.unsupported"), detection.getText());
            assertEquals(DocViewBundle.message("skill.status.platform.unsupported"), status.getText());
            assertEquals("platform is not verified", status.getToolTipText());
            assertFalse("Hide the version summary when no Skill versions are known", versions.isVisible());
        } finally {
            panel.dispose();
        }
    }

    public void testPrimarySettingsContentStaysInsideWideAndNarrowViewports() {
        String[] names = {
                "skillSectionHeader", "skillSummary", "skillCodexDetectionStatus", "skillExecutableBlock",
                "codexExecutableField", "skillScopeBlock", "skillScopeSelector", "skillTargetBlock",
                "skillTargetPathField", "skillVersionSummary", "skillStatusLabel", "skillLastCheckLabel",
                "skillActions", "skillCheckUpdatesAction", "skillPrimaryActionButton",
                "skillOperationFeedbackLabel", "skillMcpSectionHeader", "skillMcpServerInfo"
        };
        McpAiConfigurable configurable = new McpAiConfigurable(getProject());
        try {
            assertTrue("The platform must not add its default outer Settings scroll pane",
                    configurable instanceof Configurable.NoScroll);
            JComponent returnedComponent = configurable.createComponent();
            assertTrue("The page keeps one owned scroll pane for vertical scrolling",
                    returnedComponent instanceof JBScrollPane);
            JBScrollPane scrollPane = (JBScrollPane) returnedComponent;
            assertTrue("The owned viewport's root tracks its viewport width",
                    scrollPane.getViewport().getView() instanceof Scrollable);
            JComboBox<?> scopeSelector = component(scrollPane, "skillScopeSelector", JComboBox.class);
            assertEquals(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER,
                    scrollPane.getHorizontalScrollBarPolicy());
            for (Dimension size : new Dimension[]{new Dimension(5120, 2706), new Dimension(1800, 1400),
                    new Dimension(360, 640), new Dimension(5120, 2706)}) {
                scrollPane.setSize(size);
                layoutRecursively(scrollPane);
                JViewport viewport = scrollPane.getViewport();
                assertTrue("Viewport must have a usable width at " + size, viewport.getWidth() > 0);
                assertEquals("The scrollable settings view must track the viewport width at " + size,
                        viewport.getWidth(), viewport.getView().getWidth());
                assertEquals("The two-choice scope selector should keep its natural width at " + size,
                        scopeSelector.getPreferredSize().width, scopeSelector.getWidth());
                viewport.setViewPosition(new Point(120, 0));
                layoutRecursively(scrollPane);
                assertEquals("Horizontal viewport position must stay at the left edge at " + size,
                        0, viewport.getViewPosition().x);
                for (String name : names) {
                    Component item = component(scrollPane, name, Component.class);
                    if (!visibleThrough(item, viewport)) continue;
                    Rectangle bounds = SwingUtilities.convertRectangle(item.getParent(), item.getBounds(), viewport);
                    assertTrue(name + " starts outside the visible viewport at " + size + ": " + bounds,
                            bounds.x >= 0);
                    assertTrue(name + " is clipped horizontally at " + size + ": " + bounds
                                    + " viewport=" + viewport.getWidth(),
                            bounds.x + bounds.width <= viewport.getWidth());
                }
                int alignedX = -1;
                for (String name : new String[]{"skillSectionHeader", "skillSummary", "skillCodexDetectionStatus",
                        "skillScopeBlock", "skillTargetBlock", "skillVersionSummary",
                        "skillStatusLabel", "skillLastCheckLabel", "skillActions", "skillOperationFeedbackLabel",
                        "skillMcpSectionHeader", "skillMcpServerInfo"}) {
                    Component item = component(scrollPane, name, Component.class);
                    if (!visibleThrough(item, viewport)) continue;
                    int x = SwingUtilities.convertPoint(item.getParent(), item.getLocation(), viewport).x;
                    if (alignedX < 0) alignedX = x;
                    assertTrue(name + " should align with the other content blocks at " + size,
                            Math.abs(x - alignedX) <= 2);
                }
            }
        } finally {
            configurable.disposeUIResources();
        }
    }

    public void testCheckUpdatesLinkStaysVisibleAndKeepsActionLayoutWhileRunning() throws Exception {
        McpAiSettingsPanel panel = new McpAiSettingsPanel(getProject());
        try {
            awaitDetection(component(panel.getRootPanel(), "skillCodexDetectionStatus", JBTextArea.class));
            ActionLink check = component(panel.getRootPanel(), "skillCheckUpdatesAction", ActionLink.class);
            JButton primary = component(panel.getRootPanel(), "skillPrimaryActionButton", JButton.class);
            JPanel actions = component(panel.getRootPanel(), "skillActions", JPanel.class);
            assertFalse("ActionLink must not auto-hide when disabled", check.getAutoHideOnDisable());
            assertTrue(check.isVisible());

            actions.setSize(actions.getPreferredSize());
            actions.doLayout();
            int primaryX = primary.getX();
            Dimension preferredSize = actions.getPreferredSize();

            Method begin = McpAiSettingsPanel.class.getDeclaredMethod("beginOperation");
            begin.setAccessible(true);
            assertTrue("The panel should enter its running state", (long) begin.invoke(panel) > 0);
            Method updateActions = McpAiSettingsPanel.class.getDeclaredMethod("updateActionAvailability");
            updateActions.setAccessible(true);
            updateActions.invoke(panel);
            actions.setSize(actions.getPreferredSize());
            actions.doLayout();

            assertFalse(check.isEnabled());
            assertTrue("The disabled Check Updates link remains visible as a placeholder", check.isVisible());
            assertEquals("Disabling Check Updates must not move the primary action", primaryX, primary.getX());
            assertEquals("The action row keeps its preferred size while an operation runs",
                    preferredSize, actions.getPreferredSize());
        } finally {
            panel.dispose();
        }
    }

    public void testInstalledSkillShowsUpdateAsTheSinglePrimaryAction() throws Exception {
        McpAiSettingsPanel panel = new McpAiSettingsPanel(getProject());
        Path target = CodexSkillInstallService.targetPath(getProject(), CodexSkillScope.PROJECT);
        CodexSkillStatus updateAvailable = new CodexSkillStatus(CodexSkillStatus.State.UPDATE_AVAILABLE,
                "raw backend message", CodexSkillInstallService.VERIFIED_CLIENT_VERSION, target,
                "1.0.0", "1.1.0", false, true);
        try {
            Method render = McpAiSettingsPanel.class.getDeclaredMethod("renderStatus", CodexSkillStatus.class);
            render.setAccessible(true);
            render.invoke(panel, updateAvailable);
            Method updateActions = McpAiSettingsPanel.class.getDeclaredMethod("updateActionAvailability");
            updateActions.setAccessible(true);
            updateActions.invoke(panel);

            JButton primary = component(panel.getRootPanel(), "skillPrimaryActionButton", JButton.class);
            JBTextArea statusLabel = component(panel.getRootPanel(), "skillStatusLabel", JBTextArea.class);
            assertEquals(DocViewBundle.message("skill.action.update"), primary.getText());
            assertEquals(DocViewBundle.message("skill.action.update"),
                    primary.getAccessibleContext().getAccessibleName());
            assertEquals(DocViewBundle.message("skill.status.update.available", "1.1.0"),
                    statusLabel.getText());
            assertFalse("The status must not repeat the path displayed in its own field",
                    statusLabel.getText().contains(target.toString()));
            assertEquals("The full backend detail remains available in the status tooltip",
                    "raw backend message", statusLabel.getToolTipText());
            assertEquals("The full target remains available from the read-only path field",
                    target.toString(), component(panel.getRootPanel(), "skillTargetPathField", JBTextField.class)
                            .getToolTipText());
            JBTextArea versions = component(panel.getRootPanel(), "skillVersionSummary", JBTextArea.class);
            assertTrue(versions.isVisible());
            assertTrue(versions.getText().contains(DocViewBundle.message("skill.version.installed", "1.0.0")));
            assertTrue(versions.getText().contains(DocViewBundle.message("skill.version.available", "1.1.0")));
            assertFalse("The Codex build is already shown in its detection status", versions.getText()
                    .contains(CodexSkillInstallService.VERIFIED_CLIENT_VERSION));
            assertFalse(versions.getText().contains(DocViewBundle.message("skill.version.unknown")));
            assertFalse(statusLabel.getText().contains("raw backend message"));
        } finally {
            panel.dispose();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testLateCancellationKeepsCommittedInstallResult() throws Exception {
        McpAiSettingsPanel panel = new McpAiSettingsPanel(getProject());
        Path target = CodexSkillInstallService.targetPath(getProject(), CodexSkillScope.PROJECT);
        CodexSkillStatus committed = new CodexSkillStatus(CodexSkillStatus.State.CURRENT,
                "installed and discovered", CodexSkillInstallService.VERIFIED_CLIENT_VERSION,
                target, "1.1.0", "1.1.0", false, false, CodexSkillScope.PROJECT);
        try {
            awaitDetection(component(panel.getRootPanel(), "skillCodexDetectionStatus", JBTextArea.class));
            Method begin = McpAiSettingsPanel.class.getDeclaredMethod("beginOperation");
            begin.setAccessible(true);
            long request = (long) begin.invoke(panel);
            Class<?> operationType = Class.forName(McpAiSettingsPanel.class.getName() + "$Operation");
            Object install = Enum.valueOf((Class) operationType, "INSTALL");
            Method finishCancellation = McpAiSettingsPanel.class.getDeclaredMethod("finishCancelledOperation",
                    long.class, CodexSkillStatus.class, boolean.class, operationType, Path.class,
                    CodexSkillScope.class);
            finishCancellation.setAccessible(true);
            finishCancellation.invoke(panel, request, committed, true, install,
                    Path.of("/isolated-test/codex"), CodexSkillScope.PROJECT);

            JBTextArea statusLabel = component(panel.getRootPanel(), "skillStatusLabel", JBTextArea.class);
            JBTextArea feedback = component(panel.getRootPanel(), "skillOperationFeedbackLabel", JBTextArea.class);
            JBTextArea versions = component(panel.getRootPanel(), "skillVersionSummary", JBTextArea.class);
            assertTrue("The completed install status remains visible after a late cancellation", statusLabel.isVisible());
            assertEquals(DocViewBundle.message("skill.status.current", "1.1.0"), statusLabel.getText());
            assertEquals("A late cancellation must not replace a committed result with cancellation feedback",
                    "", feedback.getText());
            assertTrue(versions.isVisible());
            assertTrue(versions.getText().contains("1.1.0"));
        } finally {
            panel.dispose();
        }
    }

    public void testLastSuccessfulCheckTimeIsShownAndUnknownStateHidesIt() throws Exception {
        McpAiSettingsPanel panel = new McpAiSettingsPanel(getProject());
        Path target = CodexSkillInstallService.targetPath(getProject(), CodexSkillScope.PROJECT);
        java.time.Instant checkedAt = java.time.Instant.parse("2026-11-13T10:11:12Z");
        CodexSkillStatus checked = new CodexSkillStatus(CodexSkillStatus.State.OFFLINE,
                "offline", CodexSkillInstallService.VERIFIED_CLIENT_VERSION, target,
                "1.0.0", "", false, false, CodexSkillScope.PROJECT, checkedAt);
        try {
            Method render = McpAiSettingsPanel.class.getDeclaredMethod("renderStatus", CodexSkillStatus.class);
            render.setAccessible(true);
            render.invoke(panel, checked);
            JBTextArea lastCheck = component(panel.getRootPanel(), "skillLastCheckLabel", JBTextArea.class);
            assertTrue("Offline status should retain the prior successful check time", lastCheck.isVisible());
            assertTrue(lastCheck.getText(), lastCheck.getText().startsWith(
                    DocViewBundle.message("skill.check.last.success", "").trim()));

            Method unknownStatus = McpAiSettingsPanel.class.getDeclaredMethod("unknownStatus", CodexSkillScope.class);
            unknownStatus.setAccessible(true);
            render.invoke(panel, unknownStatus.invoke(panel, CodexSkillScope.PROJECT));
            assertFalse("A new unverified scope or client must not reuse the previous check time",
                    lastCheck.isVisible());
        } finally {
            panel.dispose();
        }
    }

    public void testMcpSettingsLocationAndStatusMessagesHaveEnglishAndChineseCopy() {
        ResourceBundle.Control propertiesOnly = ResourceBundle.Control.getNoFallbackControl(
                ResourceBundle.Control.FORMAT_PROPERTIES);
        ResourceBundle english = ResourceBundle.getBundle("messages.DocViewBundle", Locale.ENGLISH, propertiesOnly);
        ResourceBundle chinese = ResourceBundle.getBundle("messages.DocViewBundle", Locale.SIMPLIFIED_CHINESE,
                propertiesOnly);
        String englishLocation = english.getString("skill.mcp.server.info");
        String chineseLocation = chinese.getString("skill.mcp.server.info");
        String englishInstallTip = english.getString("skill.action.install.tip");
        String chineseInstallTip = chinese.getString("skill.action.install.tip");
        String englishUpdateTip = english.getString("skill.action.update.tip");
        String chineseUpdateTip = chinese.getString("skill.action.update.tip");
        assertTrue(englishLocation, englishLocation.contains("Settings → AI → MCP Server"));
        assertFalse("The old Tools location must not be shown", englishLocation.contains("Settings → Tools"));
        assertTrue(chineseLocation, chineseLocation.contains("设置 → AI → MCP Server"));
        assertFalse("不得继续显示旧 Tools 路径", chineseLocation.contains("Tools →"));
        assertTrue(englishInstallTip, englishInstallTip.contains("displayed directory"));
        assertFalse("The install tooltip must not assume Project scope", englishInstallTip.contains("project path"));
        assertTrue(chineseInstallTip, chineseInstallTip.contains("当前显示的目录"));
        assertFalse("安装提示不得假定为项目范围", chineseInstallTip.contains("项目路径"));
        assertTrue(englishUpdateTip, englishUpdateTip.contains("displayed directory"));
        assertFalse("The update tooltip must not assume Project scope", englishUpdateTip.contains("project path"));
        assertTrue(chineseUpdateTip, chineseUpdateTip.contains("当前显示目录"));
        for (String key : new String[]{"skill.codex.detected.searching", "skill.codex.detected.pending",
                "skill.codex.detected.verified", "skill.codex.detected.platform.unsupported",
                "skill.codex.detected.unsupported", "skill.codex.detected.failed", "skill.codex.detected.missing",
                "skill.status.unknown", "skill.status.platform.unsupported",
                "skill.status.client.unsupported",
                "skill.status.scope.unsupported", "skill.status.not.installed", "skill.status.current",
                "skill.status.update.available", "skill.status.unmanaged.conflict", "skill.status.modified.conflict",
                "skill.status.incompatible", "skill.status.release.unavailable", "skill.status.offline",
                "skill.status.recovery.pending", "skill.status.error", "skill.check.last.success"}) {
            assertNotNull("Missing English status message " + key, english.getString(key));
            assertNotNull("Missing Chinese status message " + key, chinese.getString(key));
        }
    }

    private static <T extends Component> T component(Container root, String name, Class<T> type) {
        T result = find(root, name, type);
        assertNotNull("Missing settings component " + name, result);
        return result;
    }

    private static <T extends Component> T find(Container parent, String name, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (child.getName() != null && child.getName().equals(name) && type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container nested) {
                T result = find(nested, name, type);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static void layoutRecursively(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container nested) layoutRecursively(nested);
        }
    }

    private static boolean visibleThrough(Component component, Container stopAt) {
        Component current = component;
        while (current != null && current != stopAt) {
            if (!current.isVisible()) return false;
            current = current.getParent();
        }
        return current == stopAt;
    }
}
