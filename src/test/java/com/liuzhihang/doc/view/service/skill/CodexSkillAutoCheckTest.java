package com.liuzhihang.doc.view.service.skill;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.components.JBTextArea;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.ui.McpAiSettingsPanel;
import org.junit.Assume;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Uses real background Tasks and a local CLI fixture; never contacts GitHub or personal Skill directories. */
public final class CodexSkillAutoCheckTest extends LightJavaCodeInsightFixtureTestCase {
    private Path root;
    private String oldHome;
    private String oldHeadless;
    private McpAiSettingsPanel panel;

    @Override protected void setUp() throws Exception {
        super.setUp();
        Assume.assumeTrue(CodexClientProbe.isVerifiedHost());
        root = Files.createTempDirectory("doc-view-auto-check-").toRealPath();
        oldHome = System.getProperty("user.home");
        oldHeadless = System.getProperty("intellij.progress.task.ignoreHeadless");
        Files.createDirectories(root.resolve("home"));
        Files.createDirectories(root.resolve("project"));
        System.setProperty("user.home", root.resolve("home").toString());
        System.setProperty("intellij.progress.task.ignoreHeadless", "true");
    }

    @Override protected void tearDown() throws Exception {
        try {
            if (panel != null) panel.dispose();
            PlatformTestUtil.waitForAllBackgroundActivityToCalmDown();
            if (oldHome != null) System.setProperty("user.home", oldHome);
            if (oldHeadless == null) System.clearProperty("intellij.progress.task.ignoreHeadless");
            else System.setProperty("intellij.progress.task.ignoreHeadless", oldHeadless);
            if (root != null) try (var files = Files.walk(root)) {
                for (Path file : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(file);
            }
        } finally { super.tearDown(); }
    }

    public void testOpeningAndScopeChangeCheckOnceWithoutInstalling() throws Exception {
        AtomicInteger checks = new AtomicInteger();
        createPanel(source(indicator -> { checks.incrementAndGet(); return Optional.empty(); }));
        await(() -> checks.get() == 1 && label().getText().equals(DocViewBundle.message("skill.status.release.unavailable")));
        JBTextArea detection = find(panel.getRootPanel(), "skillCodexDetectionStatus", JBTextArea.class);
        assertEquals(root.resolve("codex").toString(), detection.getToolTipText());
        assertFalse("The executable path belongs in the tooltip", detection.getText().contains(root.toString()));
        assertFalse(Files.exists(root.resolve("project/.agents")));
        assertFalse(Files.exists(root.resolve("home/.agents/skills")));
        for (int i = 0; i < 5; i++) { panel.getRootPanel().doLayout(); panel.getRootPanel().repaint(); }
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertEquals("Layout does not schedule checks", 1, checks.get());
        JComboBox<?> scope = find(panel.getRootPanel(), "skillScopeSelector", JComboBox.class);
        scope.setSelectedIndex(1);
        await(() -> checks.get() == 2 && scope.isEnabled());
        assertFalse(Files.exists(root.resolve("home/.agents/skills")));
        assertFalse(Files.exists(root.resolve("project/.agents")));
        try (var files = Files.walk(root)) {
            assertTrue("Checks never write install metadata, journals or writer locks", files.noneMatch(p ->
                    p.getFileName().toString().equals("install.json") || p.getFileName().toString().equals("journal.json")
                            || p.getFileName().toString().endsWith(".lock")));
        }
    }

    public void testSelectingAnotherExecutableAfterAnUnsupportedBuildChecksOnceWithoutInstalling() throws Exception {
        AtomicInteger checks = new AtomicInteger();
        String unsupported = "codex-cli 0.0.0-unverified";
        createPanel(source(indicator -> { checks.incrementAndGet(); return Optional.empty(); }), unsupported);
        await(() -> label().getText().equals(DocViewBundle.message("skill.status.client.unsupported",
                unsupported, CodexSkillInstallService.VERIFIED_CLIENT_VERSION)));
        assertEquals("An unsupported executable must not reach the release source", 0, checks.get());

        Path replacement = root.resolve("selected-codex");
        writeVersionFixture(replacement, CodexSkillInstallService.VERIFIED_CLIENT_VERSION);
        TextFieldWithBrowseButton chooser = find(panel.getRootPanel(), "codexExecutableField",
                TextFieldWithBrowseButton.class);
        assertNotNull(chooser);
        assertTrue(chooser.isEnabled());
        chooser.setText(replacement.toString());
        await(() -> label().getText().equals(DocViewBundle.message("skill.status.release.unavailable")));

        JBTextArea detection = find(panel.getRootPanel(), "skillCodexDetectionStatus", JBTextArea.class);
        assertEquals(replacement.toString(), detection.getToolTipText());
        PlatformTestUtil.waitForAllBackgroundActivityToCalmDown();
        assertEquals("Replacing the path must start exactly one check", 1, checks.get());
        assertFalse(Files.exists(root.resolve("project/.agents")));
        assertFalse(Files.exists(root.resolve("home/.agents")));
    }

    public void testDisposingDuringAutomaticCheckCancelsAndDropsLateUiResult() throws Exception {
        AtomicBoolean entered = new AtomicBoolean();
        AtomicBoolean exited = new AtomicBoolean();
        createPanel(source(indicator -> {
            entered.set(true);
            try { while (true) { indicator.checkCanceled(); java.util.concurrent.locks.LockSupport.parkNanos(1_000_000); } }
            finally { exited.set(true); }
        }));
        await(entered::get);
        String before = label().getText();
        panel.dispose();
        await(exited::get);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertTrue(panel.isDisposed());
        assertEquals("Closed views receive no late status", before, label().getText());
        assertFalse(Files.exists(root.resolve("project/.agents")));
    }

    public void testAsyncCheckPreservesPathSelectionUntilTargetChanges() throws Exception {
        AtomicBoolean entered = new AtomicBoolean();
        AtomicBoolean release = new AtomicBoolean();
        createPanel(source(indicator -> {
            entered.set(true);
            while (!release.get()) {
                indicator.checkCanceled();
                java.util.concurrent.locks.LockSupport.parkNanos(1_000_000);
            }
            return Optional.empty();
        }));
        try {
            await(entered::get);
            JTextField path = find(panel.getRootPanel(), "skillTargetPathField", JTextField.class);
            String suffix = ".agents/skills/doc-view";
            path.select(path.getText().length() - suffix.length(), path.getText().length());
            release.set(true);
            await(() -> label().getText().equals(DocViewBundle.message("skill.status.release.unavailable")));
            assertEquals("Background completion must not interrupt copying the path", suffix, path.getSelectedText());
            find(panel.getRootPanel(), "skillScopeSelector", JComboBox.class).setSelectedIndex(1);
            assertEquals(root.resolve("home").resolve(suffix).toString(), path.getText());
            assertNull(path.getSelectedText());
            assertEquals(0, path.getCaretPosition());
        } finally {
            release.set(true);
        }
    }

    public void testProgressNamesTheAutomaticCheckAndCollapsesAfterCompletion() throws Exception {
        AtomicBoolean release = new AtomicBoolean();
        AtomicBoolean entered = new AtomicBoolean();
        createPanel(source(indicator -> {
            entered.set(true);
            while (!release.get()) {
                indicator.checkCanceled();
                java.util.concurrent.locks.LockSupport.parkNanos(1_000_000);
            }
            return Optional.empty();
        }));
        try {
            await(entered::get);
            JBTextArea feedback = find(panel.getRootPanel(), "skillOperationFeedbackLabel", JBTextArea.class);
            assertTrue(feedback.isVisible());
            assertEquals(DocViewBundle.message("skill.operation.title.check"), feedback.getText());
            release.set(true);
            await(() -> label().getText().equals(DocViewBundle.message("skill.status.release.unavailable")));
            assertFalse(feedback.isVisible());
        } finally {
            release.set(true);
        }
    }

    public void testDisposedPanelIsCollectibleWhileRootRemainsExternallyHeld() throws Exception {
        AtomicInteger checks = new AtomicInteger();
        createPanel(source(indicator -> { checks.incrementAndGet(); return Optional.empty(); }));
        await(() -> checks.get() == 1
                && label().getText().equals(DocViewBundle.message("skill.status.release.unavailable")));

        var externallyHeldRoot = panel.getRootPanel();
        WeakReference<McpAiSettingsPanel> panelReference = new WeakReference<>(panel);
        panel.dispose();
        panel = null;
        PlatformTestUtil.waitForAllBackgroundActivityToCalmDown();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        awaitCollected(panelReference);
        assertTrue("The test must retain the disposed panel's Swing tree", externallyHeldRoot.getComponentCount() > 0);
        Reference.reachabilityFence(externallyHeldRoot);
    }

    private void createPanel(SkillReleaseSource source) throws Exception {
        createPanel(source, CodexSkillInstallService.VERIFIED_CLIENT_VERSION);
    }

    private void createPanel(SkillReleaseSource source, String version) throws Exception {
        Project actual = getProject();
        Project project = (Project) Proxy.newProxyInstance(Project.class.getClassLoader(), new Class[]{Project.class},
                (proxy, method, args) -> method.getName().equals("getBasePath") ? root.resolve("project").toString()
                        : method.invoke(actual, args));
        Path cli = root.resolve("codex");
        writeVersionFixture(cli, version);
        CodexSkillInstallService service = new CodexSkillInstallService(project, source, root.resolve("metadata"));
        var constructor = McpAiSettingsPanel.class.getDeclaredConstructor(Project.class, CodexSkillInstallService.class, Supplier.class);
        constructor.setAccessible(true);
        panel = constructor.newInstance(project, service, (Supplier<List<Path>>) () -> List.of(cli));
    }

    private static void writeVersionFixture(Path file, String version) throws Exception {
        Files.writeString(file, "#!/bin/sh\nprintf '%s\\n' '" + version + "'\n");
        assertTrue(file.toFile().setExecutable(true));
    }

    private interface Lookup { Optional<SkillReleaseSource.Candidate> run(ProgressIndicator indicator); }
    private SkillReleaseSource source(Lookup lookup) {
        return new SkillReleaseSource() {
            @Override public Optional<Candidate> latest(ProgressIndicator indicator) { return lookup.run(indicator); }
            @Override public byte[] downloadArchive(Candidate candidate, ProgressIndicator indicator) {
                throw new AssertionError("Automatic checks must never download Skill archives");
            }
        };
    }
    private JBTextArea label() { return find(panel.getRootPanel(), "skillStatusLabel", JBTextArea.class); }
    private void await(BooleanSupplier condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < end) {
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            if (condition.getAsBoolean()) return;
            Thread.sleep(15);
        }
        fail("Background state not reached; " + label().getText());
    }

    private void awaitCollected(WeakReference<?> reference) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < end) {
            System.gc();
            System.runFinalization();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            if (reference.get() == null) return;
            Thread.sleep(25);
        }
        assertNull("A disposed McpAiSettingsPanel remains reachable from its externally held Swing root",
                reference.get());
    }

    private static <T extends Component> T find(Container parent, String name, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName()) && type.isInstance(child)) return type.cast(child);
            if (child instanceof Container nested) { T found = find(nested, name, type); if (found != null) return found; }
        }
        return null;
    }
}
