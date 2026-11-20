package com.liuzhihang.doc.view.service.skill;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Assume;
import org.junit.rules.TemporaryFolder;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CodexClientProbeTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void requiresAnAbsoluteExecutableRegularFile() throws Exception {
        Path file = temporary.newFile("codex").toPath();
        assertTrue(file.toFile().setExecutable(true));

        assertEquals(file.toRealPath(), CodexClientProbe.checkedExecutable(file));
        assertThrows(IOException.class, () -> CodexClientProbe.checkedExecutable(Path.of("codex")));
        assertThrows(IOException.class, () -> CodexClientProbe.checkedExecutable(temporary.getRoot().toPath()));
    }

    @Test
    public void discoveryMatchRequiresExactPathNameAndScope() throws Exception {
        Path home = temporary.newFolder("home").toPath().toRealPath();
        String skillPath = home.resolve(".agents/skills/doc-view/SKILL.md").toString();
        JsonObject userSkill = skill("doc-view", "user", skillPath);
        JsonObject projectSkill = skill("doc-view", "repo", skillPath);
        JsonObject otherSkill = skill("doc-view", "user", home.resolve(".agents/skills/other/SKILL.md").toString());

        assertTrue(CodexClientProbe.matchesExpectedSkill(userSkill, skillPath, "user"));
        assertFalse(CodexClientProbe.matchesExpectedSkill(projectSkill, skillPath, "user"));
        assertFalse(CodexClientProbe.matchesExpectedSkill(otherSkill, skillPath, "user"));
        userSkill.remove("path");
        assertFalse(CodexClientProbe.matchesExpectedSkill(userSkill, skillPath, "user"));
    }

    @Test
    public void hostGateMatchesOnlyTheVerifiedMacAppleSiliconPlatform() {
        assertTrue(CodexClientProbe.isVerifiedHost("Mac OS X", "aarch64"));
        assertTrue(CodexClientProbe.isVerifiedHost("Darwin", "arm64"));
        assertFalse(CodexClientProbe.isVerifiedHost("Windows 11", "aarch64"));
        assertFalse(CodexClientProbe.isVerifiedHost("Mac OS X", "x86_64"));
    }

    @Test
    public void versionCancellationStopsTrackedProcessTreeAndDeletesTemporaryHomes() throws Exception {
        assertCancellationStopsTrackedProcessTreeAndDeletesTemporaryHomes(false);
    }

    @Test
    public void discoveryCancellationStopsTrackedProcessTreeAndDeletesTemporaryHomes() throws Exception {
        assertCancellationStopsTrackedProcessTreeAndDeletesTemporaryHomes(true);
    }

    private void assertCancellationStopsTrackedProcessTreeAndDeletesTemporaryHomes(boolean discovery)
            throws Exception {
        Assume.assumeFalse("The controlled fake CLI uses a POSIX shell",
                System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win"));
        Path control = temporary.newFolder("probe-cancel-control").toPath();
        Path projectRoot = temporary.newFolder("probe-cancel-project").toPath().toRealPath();
        Path shellPidMarker = control.resolve("shell.pid");
        Path childPidMarker = control.resolve("child.pid");
        Path homeMarker = control.resolve("home.path");
        Path codexHomeMarker = control.resolve("codex-home.path");
        Path executable = temporary.newFile("fake-codex.sh").toPath();
        Files.writeString(executable, blockingFakeCli(shellPidMarker, childPidMarker, homeMarker, codexHomeMarker),
                StandardCharsets.UTF_8);
        assertTrue("Could not mark fake Codex executable", executable.toFile().setExecutable(true));

        TestProgressIndicator indicator = new TestProgressIndicator();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> operation = null;
        long shellPid = -1;
        long childPid = -1;
        try {
            CodexClientProbe probe = new CodexClientProbe();
            CodexSkillTarget target = CodexSkillTarget.forOwnerRoot(projectRoot, CodexSkillScope.PROJECT);
            operation = executor.submit(() -> discovery
                    ? probe.discover(executable, projectRoot, target, indicator)
                    : probe.version(executable, projectRoot, indicator));

            awaitNonEmptyFile(childPidMarker, 5, TimeUnit.SECONDS);
            awaitNonEmptyFile(shellPidMarker, 5, TimeUnit.SECONDS);
            awaitNonEmptyFile(homeMarker, 5, TimeUnit.SECONDS);
            awaitNonEmptyFile(codexHomeMarker, 5, TimeUnit.SECONDS);
            shellPid = Long.parseLong(Files.readString(shellPidMarker).strip());
            childPid = Long.parseLong(Files.readString(childPidMarker).strip());
            Path processHome = Path.of(Files.readString(homeMarker).strip());
            Path isolatedCodexHome = Path.of(Files.readString(codexHomeMarker).strip());
            assertTrue("The probe must isolate HOME", processHome.startsWith(Path.of(System.getProperty("java.io.tmpdir"))));
            assertTrue("The probe must isolate CODEX_HOME", isolatedCodexHome.startsWith(processHome));

            indicator.cancel();
            try {
                operation.get(5, TimeUnit.SECONDS);
                fail("The cancelled probe must propagate ProcessCanceledException");
            } catch (ExecutionException cancelled) {
                assertTrue("Expected ProcessCanceledException, got " + cancelled.getCause(),
                        cancelled.getCause() instanceof ProcessCanceledException);
            }

            assertTrue("Probe shell PID did not exit: " + shellPid, awaitProcessExit(shellPid, 2, TimeUnit.SECONDS));
            assertTrue("Probe child PID did not exit: " + childPid, awaitProcessExit(childPid, 2, TimeUnit.SECONDS));
            assertFalse("Temporary HOME was not deleted: " + processHome, Files.exists(processHome));
            assertFalse("Isolated CODEX_HOME was not deleted: " + isolatedCodexHome, Files.exists(isolatedCodexHome));
        } finally {
            indicator.cancel();
            if (operation != null && !operation.isDone()) operation.cancel(true);
            if (shellPid <= 0) shellPid = readTrackedPid(shellPidMarker);
            if (childPid <= 0) childPid = readTrackedPid(childPidMarker);
            if (shellPid > 0) destroyTrackedProcess(shellPid);
            if (childPid > 0) destroyTrackedProcess(childPid);
            executor.shutdownNow();
            executor.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    private static String blockingFakeCli(Path shellPidMarker, Path childPidMarker,
                                          Path homeMarker, Path codexHomeMarker) {
        return "#!/bin/sh\n"
                + writeMarker(shellPidMarker, "\"$$\"")
                + writeMarker(homeMarker, "\"$HOME\"")
                + writeMarker(codexHomeMarker, "\"$CODEX_HOME\"")
                + "if [ \"$1\" = \"--version\" ]; then printf '%s\\n' '"
                + CodexClientProbe.SUPPORTED_VERSION + "'; fi\n"
                + "/bin/sleep 60 &\n"
                + "child=$!\n"
                + writeMarker(childPidMarker, "\"$child\"")
                + "wait \"$child\"\n";
    }

    private static String writeMarker(Path marker, String value) {
        String temporaryMarker = shellQuote(marker + ".tmp");
        String publishedMarker = shellQuote(marker.toString());
        return "printf '%s' " + value + " > " + temporaryMarker + "\n"
                + "mv " + temporaryMarker + " " + publishedMarker + "\n";
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private static void awaitNonEmptyFile(Path path, long timeout, TimeUnit unit) throws Exception {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline && (!Files.exists(path) || Files.size(path) == 0)) {
            TimeUnit.MILLISECONDS.sleep(10);
        }
        assertTrue("Timed out waiting for fake Codex marker " + path.getFileName(),
                Files.exists(path) && Files.size(path) > 0);
    }

    private static boolean awaitProcessExit(long pid, long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline && ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)) {
            TimeUnit.MILLISECONDS.sleep(10);
        }
        return !ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    private static void destroyTrackedProcess(long pid) {
        ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
        if (handle == null || !handle.isAlive()) return;
        handle.destroyForcibly();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        boolean interrupted = false;
        try {
            while (handle.isAlive() && System.nanoTime() < deadline) TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException interruption) {
            interrupted = true;
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private static long readTrackedPid(Path marker) {
        try {
            if (Files.exists(marker) && Files.size(marker) > 0) return Long.parseLong(Files.readString(marker).strip());
        } catch (IOException | NumberFormatException ignored) {
            // Cleanup only ever targets a complete PID written by this test's fake CLI.
        }
        return -1;
    }

    private static final class TestProgressIndicator implements ProgressIndicator {
        private volatile boolean canceled;

        @Override public void start() {}
        @Override public void stop() {}
        @Override public boolean isRunning() { return true; }
        @Override public void cancel() { canceled = true; }
        @Override public boolean isCanceled() { return canceled; }
        @Override public void setText(String text) {}
        @Override public String getText() { return ""; }
        @Override public void setText2(String text) {}
        @Override public String getText2() { return ""; }
        @Override public double getFraction() { return 0; }
        @Override public void setFraction(double fraction) {}
        @Override public void pushState() {}
        @Override public void popState() {}
        @Override public boolean isModal() { return false; }
        @Override public ModalityState getModalityState() { return ModalityState.nonModal(); }
        @Override public void setModalityProgress(ProgressIndicator progress) {}
        @Override public boolean isIndeterminate() { return false; }
        @Override public void setIndeterminate(boolean indeterminate) {}
        @Override public void checkCanceled() throws ProcessCanceledException {
            if (canceled) throw new ProcessCanceledException();
        }
        @Override public boolean isPopupWasShown() { return false; }
        @Override public boolean isShowing() { return false; }
    }

    private static JsonObject skill(String name, String scope, String path) {
        JsonObject skill = new JsonObject();
        skill.addProperty("name", name);
        skill.addProperty("scope", scope);
        skill.addProperty("path", path);
        return skill;
    }
}
