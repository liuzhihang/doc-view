package com.liuzhihang.doc.view.service.skill;

import com.google.gson.GsonBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/** Opt-in: real Codex discovery + production install service, local immutable release source. */
public final class CodexSkillReleaseAcceptanceTest extends LightJavaCodeInsightFixtureTestCase {
    public void testFreshProcessJournalRecoveryForBothScopes() throws Exception {
        Path output = requiredPath("DOC_VIEW_ACCEPTANCE_OUTPUT");
        Files.createDirectories(output);
        Path manifestPath = requiredPath("DOC_VIEW_ACCEPTANCE_MANIFEST");
        Path archivePath = requiredPath("DOC_VIEW_ACCEPTANCE_ARCHIVE");
        SkillReleaseManifest manifest = SkillReleaseManifest.parse(Files.readAllBytes(manifestPath));
        var expected = SkillPackageArchive.validate(Files.readAllBytes(archivePath), manifest);
        List<Map<String, Object>> checks = new ArrayList<>();
        for (CodexSkillScope scope : CodexSkillScope.values()) {
            for (SkillInstallTransaction.Phase phase : List.of(SkillInstallTransaction.Phase.READY,
                    SkillInstallTransaction.Phase.OLD_BACKED_UP, SkillInstallTransaction.Phase.NEW_IN_PLACE)) {
                Path root = Files.createTempDirectory(output, "crash-" + scope + "-" + phase + "-").toRealPath();
                Path owner = Files.createDirectory(root.resolve("owner"));
                Path metadata = Files.createDirectory(root.resolve("metadata"));
                Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/java").toString(),
                        "-cp", System.getProperty("docview.acceptance.classpath"), SkillCrashProcess.class.getName(),
                        scope.name(), owner.toString(), metadata.toString(), manifestPath.toString(),
                        archivePath.toString(), phase.name()).redirectErrorStream(true)
                        .redirectOutput(root.resolve("child.log").toFile()).start();
                try {
                    assertTrue("Child JVM must finish", child.waitFor(30, TimeUnit.SECONDS));
                    assertEquals(Files.readString(root.resolve("child.log")), 73, child.exitValue());
                } finally { if (child.isAlive()) child.destroyForcibly(); }
                Path target = owner.resolve(".agents/skills/doc-view");
                SkillInstallMetadataStore fresh = new SkillInstallMetadataStore(owner, scope, metadata);
                assertTrue(Files.exists(fresh.journalFile()));
                if (phase == SkillInstallTransaction.Phase.OLD_BACKED_UP) assertFalse(Files.exists(target));
                assertTrue(SkillInstallTransaction.recoverPending(owner, target, fresh));
                assertFiles(expected, target);
                assertEquals(manifest.skillVersion(), fresh.read().orElseThrow().skillVersion());
                assertFalse(Files.exists(fresh.journalFile()));
                assertNoTransactionDirectories(target.getParent(), fresh.directory());
                record(checks, scope, "child-jvm-halt-" + phase + "-recovery");
            }
        }
        Files.writeString(output.resolve("jvm-crash-recovery.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("passed", true, "checks", checks,
                        "actualJvmCrashTested", true, "actualIdeRestartTested", false)));
    }

    public void testBothScopesWithRealCodexAndCandidateArchive() throws Exception {
        Path output = requiredPath("DOC_VIEW_ACCEPTANCE_OUTPUT");
        Path cli = requiredPath("DOC_VIEW_ACCEPTANCE_CODEX");
        Path manifestPath = requiredPath("DOC_VIEW_ACCEPTANCE_MANIFEST");
        SkillReleaseManifest manifest = SkillReleaseManifest.parse(Files.readAllBytes(manifestPath));
        byte[] archive = Files.readAllBytes(requiredPath("DOC_VIEW_ACCEPTANCE_ARCHIVE"));
        Map<String, byte[]> candidateFiles = SkillPackageArchive.validate(archive, manifest);
        Files.createDirectories(output);
        Path temporary = Files.createTempDirectory(output, "lifecycle-").toRealPath();
        Path projectRoot = Files.createDirectory(temporary.resolve("project"));
        Path isolatedUser = Files.createDirectory(temporary.resolve("user"));
        Path projectMetadata = Files.createDirectory(temporary.resolve("project-metadata"));
        Process git = new ProcessBuilder("/usr/bin/git", "init", "--quiet", projectRoot.toString()).start();
        assertTrue(git.waitFor(10, TimeUnit.SECONDS));
        assertEquals(0, git.exitValue());
        Project project = projectAt(projectRoot);
        String oldUserHome = System.getProperty("user.home");
        List<Map<String, Object>> checks = new ArrayList<>();
        boolean passed = false;
        try {
            System.setProperty("user.home", isolatedUser.toString());
            for (CodexSkillScope scope : CodexSkillScope.values()) {
                Path owner = scope == CodexSkillScope.PROJECT ? projectRoot : isolatedUser;
                Path target = owner.resolve(".agents/skills/doc-view");
                Path other = (scope == CodexSkillScope.PROJECT ? isolatedUser : projectRoot)
                        .resolve(".agents/skills/doc-view");
                Map<String, String> otherBefore = hashes(other);
                LocalSource source = new LocalSource(manifest, archive);
                CodexSkillInstallService service = service(project, scope, source, projectMetadata);
                SkillInstallMetadataStore store = scope == CodexSkillScope.PROJECT
                        ? new SkillInstallMetadataStore(owner, scope, projectMetadata)
                        : new SkillInstallMetadataStore(owner, scope);
                CodexSkillStatus initial = background(() -> service.checkForUpdates(scope, cli, new EmptyProgressIndicator()));
                assertEquals(CodexSkillStatus.State.NOT_INSTALLED, initial.state());
                assertNotNull("A successful release query records its local completion time",
                        initial.lastSuccessfulCheckAt());
                assertFalse(Files.exists(target));
                assertFalse(Files.exists(store.metadataFile()));
                assertFalse(Files.exists(store.lockFile()));
                assertTrue(Files.exists(store.lastCheckFile()));
                record(checks, scope, "check-writes-only-separate-last-check-state");
                CodexSkillOperationResult installed = background(() -> service.install(scope, cli, new EmptyProgressIndicator()));
                assertTrue(installed.committed());
                assertEquals(CodexSkillStatus.State.CURRENT, installed.status().state());
                assertEquals(manifest.skillVersion(), installed.status().installedVersion());
                assertFiles(candidateFiles, target);
                assertEquals(scope, store.read().orElseThrow().scope());
                assertEquals(target.toString(), store.read().orElseThrow().targetPath());
                record(checks, scope, "candidate-install-real-discovery");

                Map<String, byte[]> updateFiles = new LinkedHashMap<>(candidateFiles);
                updateFiles.put("SKILL.md", (new String(candidateFiles.get("SKILL.md"), StandardCharsets.UTF_8)
                        + "\n<!-- Isolated lifecycle update fixture. -->\n").getBytes(StandardCharsets.UTF_8));
                source.use(SkillFixtureFactory.fixture("1.7.1", updateFiles));
                assertEquals(CodexSkillStatus.State.UPDATE_AVAILABLE,
                        background(() -> service.checkForUpdates(scope, cli, new EmptyProgressIndicator())).state());
                CodexSkillOperationResult updated = background(() -> service.update(scope, cli, new EmptyProgressIndicator()));
                assertTrue(updated.committed());
                assertEquals("1.7.1", updated.status().installedVersion());
                assertFiles(updateFiles, target);
                record(checks, scope, "update-real-discovery");

                Map<String, String> stable = hashes(target);
                byte[] metadataBefore = Files.readAllBytes(store.metadataFile());
                source.use(SkillFixtureFactory.fixture("1.7.2", candidateFiles));
                source.cancelDownload = true;
                try {
                    CodexSkillOperationResult unexpected = background(() -> service.update(scope, cli, new EmptyProgressIndicator()));
                    fail("Cancelled download must not complete: " + unexpected.status());
                } catch (ProcessCanceledException expected) {
                    assertEquals(stable, hashes(target));
                    assertTrue(java.util.Arrays.equals(metadataBefore, Files.readAllBytes(store.metadataFile())));
                    assertFalse(Files.exists(store.journalFile()));
                }
                source.cancelDownload = false;
                record(checks, scope, "download-cancel-preserves-old-version");

                source.offlineDownload = true;
                CodexSkillOperationResult offline = background(() -> service.update(scope, cli, new EmptyProgressIndicator()));
                assertEquals(CodexSkillStatus.State.OFFLINE, offline.status().state());
                assertFalse(offline.committed());
                assertEquals(stable, hashes(target));
                assertTrue(java.util.Arrays.equals(metadataBefore, Files.readAllBytes(store.metadataFile())));
                source.offlineDownload = false;
                record(checks, scope, "download-failure-preserves-old-version");

                Map<String, byte[]> undiscoverable = Map.of("SKILL.md",
                        "---\nname: different-skill\ndescription: Isolated negative fixture.\n---\nFixture.\n".getBytes(StandardCharsets.UTF_8));
                source.use(SkillFixtureFactory.fixture("1.7.2", undiscoverable));
                CodexSkillOperationResult rejected = background(() -> service.update(scope, cli, new EmptyProgressIndicator()));
                assertEquals(CodexSkillStatus.State.ERROR, rejected.status().state());
                assertFalse(rejected.committed());
                assertEquals(stable, hashes(target));
                assertTrue(java.util.Arrays.equals(metadataBefore, Files.readAllBytes(store.metadataFile())));
                assertFalse(Files.exists(store.journalFile()));
                assertNoTransactionDirectories(target.getParent(), store.directory());
                record(checks, scope, "real-discovery-failure-rolls-back-files-and-metadata");

                source.use(SkillFixtureFactory.fixture("1.7.2", candidateFiles));
                CodexSkillInstallService fresh = service(project, scope, source, projectMetadata);
                CodexSkillOperationResult retried = background(() -> fresh.update(scope, cli, new EmptyProgressIndicator()));
                assertTrue(retried.committed());
                assertEquals("1.7.2", retried.status().installedVersion());
                assertFiles(candidateFiles, target);
                assertNoTransactionDirectories(target.getParent(), store.directory());
                record(checks, scope, "fresh-service-retry-after-rollback");

                Files.writeString(target.resolve("SKILL.md"), "\nUser-owned edit.\n", java.nio.file.StandardOpenOption.APPEND);
                Map<String, String> modified = hashes(target);
                assertEquals(CodexSkillStatus.State.MODIFIED_CONFLICT,
                        background(() -> fresh.checkForUpdates(scope, cli, new EmptyProgressIndicator())).state());
                assertFalse(background(() -> fresh.update(scope, cli, new EmptyProgressIndicator())).committed());
                assertEquals(modified, hashes(target));
                assertEquals(otherBefore, hashes(other));
                record(checks, scope, "modified-conflict-and-other-scope-preserved");
            }
            passed = true;
        } finally {
            System.setProperty("user.home", oldUserHome);
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("passed", passed);
            report.put("clientVersion", CodexClientProbe.SUPPORTED_VERSION);
            report.put("releaseSource", "local immutable candidate; network source injected, not GitHub acceptance");
            report.put("candidateCommit", manifest.commitSha());
            report.put("archiveSha256", SkillReleaseManifest.sha256(archive));
            report.put("checks", checks);
            report.put("isolatedRoot", temporary.toString());
            report.put("actualIdeUiTested", false);
            report.put("actualJvmCrashTested", false);
            report.put("ordinaryHomeChanged", false);
            Files.writeString(output.resolve("real-codex-lifecycle.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }
    }

    private static CodexSkillInstallService service(Project project, CodexSkillScope scope, LocalSource source, Path metadata) {
        return scope == CodexSkillScope.PROJECT ? new CodexSkillInstallService(project, source, metadata)
                : new CodexSkillInstallService(project, source);
    }
    private static Path requiredPath(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing opt-in acceptance input: " + name);
        return Path.of(value).toAbsolutePath().normalize();
    }
    private static void record(List<Map<String, Object>> checks, CodexSkillScope scope, String name) {
        checks.add(Map.of("scope", scope.name(), "case", name, "passed", true));
    }
    private static Map<String, String> hashes(Path root) throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        if (Files.exists(root)) try (var walk = Files.walk(root)) {
            for (Path file : walk.filter(Files::isRegularFile).sorted().toList())
                result.put(root.relativize(file).toString(), SkillReleaseManifest.sha256(Files.readAllBytes(file)));
        }
        return result;
    }
    private static void assertFiles(Map<String, byte[]> files, Path root) throws Exception {
        Map<String, String> expected = new LinkedHashMap<>();
        for (var entry : files.entrySet()) expected.put(entry.getKey(), SkillReleaseManifest.sha256(entry.getValue()));
        assertEquals(expected, hashes(root));
    }
    private static void assertNoTransactionDirectories(Path... roots) throws IOException {
        for (Path root : roots) if (Files.exists(root)) try (var stream = Files.list(root)) {
            assertFalse(stream.anyMatch(p -> p.getFileName().toString().startsWith(".doc-view-skill-stage-")
                    || p.getFileName().toString().startsWith(".doc-view-skill-backup-")
                    || p.getFileName().toString().startsWith(".metadata-backup-")));
        }
    }
    private static <T> T background(Callable<T> call) throws Exception {
        var result = new java.util.concurrent.CompletableFuture<T>();
        var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try { result.complete(call.call()); }
            catch (Throwable error) { failure.set(error); result.complete(null); }
        });
        T value = result.get(90, TimeUnit.SECONDS);
        if (failure.get() instanceof Exception cause) throw cause;
        if (failure.get() instanceof Error cause) throw cause;
        return value;
    }
    private static Project projectAt(Path root) {
        return (Project) Proxy.newProxyInstance(Project.class.getClassLoader(), new Class<?>[]{Project.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getBasePath" -> root.toString();
                    case "getName", "toString" -> "isolated-skill-acceptance";
                    default -> method.getReturnType() == boolean.class ? false : null;
                });
    }
    private static final class LocalSource implements SkillReleaseSource {
        Candidate candidate;
        byte[] archive;
        boolean cancelDownload;
        boolean offlineDownload;
        LocalSource(SkillReleaseManifest manifest, byte[] archive) { this.candidate = new Candidate(manifest, 1); this.archive = archive; }
        void use(SkillFixtureFactory.Fixture fixture) { candidate = new Candidate(fixture.manifest(), 1); archive = fixture.archive(); }
        @Override public Optional<Candidate> latest(ProgressIndicator indicator) { indicator.checkCanceled(); return Optional.of(candidate); }
        @Override public byte[] downloadArchive(Candidate selected, ProgressIndicator indicator) throws IOException {
            if (cancelDownload) { indicator.cancel(); throw new ProcessCanceledException(); }
            if (offlineDownload) throw new GitHubSkillReleaseSource.OfflineException("isolated download failure");
            return archive.clone();
        }
    }
}
