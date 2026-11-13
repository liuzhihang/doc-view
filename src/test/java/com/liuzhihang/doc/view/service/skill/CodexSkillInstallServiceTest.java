package com.liuzhihang.doc.view.service.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.Assume;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public final class CodexSkillInstallServiceTest extends LightJavaCodeInsightFixtureTestCase {
    private final List<Path> temporaryRoots = new ArrayList<>();

    @Override
    protected void tearDown() throws Exception {
        try {
            for (Path root : temporaryRoots) deleteTree(root);
        } finally {
            super.tearDown();
        }
    }

    public void testNoCompatibleReleaseCheckDoesNotCreateSkillTargetMetadataOrWriterLock() throws Exception {
        Assume.assumeTrue("Codex Skill lifecycle is currently verified on macOS Apple Silicon",
                CodexClientProbe.isVerifiedHost());
        Path root = tempDir("project-no-release").toRealPath();
        Path target = root.resolve(".agents/skills/doc-view");
        Path cli = fakeCodexExecutable();
        Project project = projectAt(root);
        Path metadataRoot = tempDir("metadata-no-release").toRealPath();
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(root, CodexSkillScope.PROJECT, metadataRoot);
        SkillReleaseSource source = source(Optional.empty(), null);
        CodexSkillInstallService service = new CodexSkillInstallService(project, source, metadataRoot);

        CodexSkillStatus status = inBackground(() -> service.checkForUpdates(
                CodexSkillScope.PROJECT, cli, new EmptyProgressIndicator()));

        assertEquals(CodexSkillStatus.State.NO_COMPATIBLE_RELEASE, status.state());
        assertEquals(CodexSkillScope.PROJECT, status.scope());
        assertFalse(Files.exists(target));
        assertFalse(Files.exists(root.resolve(".agents")));
        assertFalse(Files.exists(store.metadataFile()));
        assertFalse(Files.exists(store.journalFile()));
        assertFalse(Files.exists(store.lockFile()));
        assertTrue("A successful release lookup persists only the separate check timestamp",
                Files.exists(store.lastCheckFile()));
        assertTrue(new SkillLastCheckStore(store.lastCheckFile()).read().isPresent());
        assertFalse("The check cache must not create an install metadata directory entry", Files.exists(store.metadataFile()));

        Path userTarget = CodexSkillInstallService.targetPath(project, CodexSkillScope.USER);
        assertEquals(Path.of(System.getProperty("user.home")).resolve(".agents/skills/doc-view"), userTarget);
        assertNotEquals("Project and User scopes must not collapse to the same target", target, userTarget);
    }

    public void testPendingRecoveryRetainsVerifiedClientIdentityWithoutNetworkOrWrites() throws Exception {
        assertPendingRecoveryClientGate(false);
    }

    public void testPendingRecoveryDoesNotEnableAnUnsupportedClient() throws Exception {
        assertPendingRecoveryClientGate(true);
    }

    public void testUpdateDoesNotRecoverWithAnUnsupportedClient() throws Exception {
        assertRecoveryUpdateClientGate(true);
    }

    public void testUpdateReportsLocalRecoveryEvenWhenReleaseCheckIsOffline() throws Exception {
        assertRecoveryUpdateClientGate(false);
    }

    private void assertRecoveryUpdateClientGate(boolean unsupported) throws Exception {
        Assume.assumeTrue(CodexClientProbe.isVerifiedHost());
        Path root = tempDir("project-recovery-update").toRealPath();
        Path target = root.resolve(".agents/skills/doc-view");
        Path metadataRoot = tempDir("metadata-recovery-update").toRealPath();
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(root, CodexSkillScope.PROJECT, metadataRoot);
        byte[] oldBytes = "---\nname: doc-view\n---\nprevious complete version\n".getBytes(StandardCharsets.UTF_8);
        SkillFixtureFactory.Fixture old = SkillFixtureFactory.fixture("1.7.0", Map.of("SKILL.md", oldBytes));
        Files.createDirectories(target);
        Files.write(target.resolve("SKILL.md"), oldBytes);
        store.write(SkillInstallMetadata.from(CodexSkillScope.PROJECT, old.manifest(),
                CodexSkillInstallService.VERIFIED_CLIENT_VERSION, target.toString(), Instant.EPOCH));
        byte[] oldMetadata = Files.readAllBytes(store.metadataFile());
        SkillFixtureFactory.Fixture newer = SkillFixtureFactory.fixture("1.7.1", Map.of("SKILL.md", "new fixture".getBytes(StandardCharsets.UTF_8)));
        try {
            new SkillInstallTransaction(CodexSkillScope.PROJECT, root, target, store, newer.manifest(), newer.files(),
                    CodexSkillInstallService.VERIFIED_CLIENT_VERSION, "1.7.1", new EmptyProgressIndicator(),
                    (path, progress) -> true, phase -> {
                        if (phase == SkillInstallTransaction.Phase.OLD_BACKED_UP) throw new AssertionError("simulated termination");
                    }).execute();
            fail("The fixture must stop after backing up the old directory");
        } catch (AssertionError expected) {
            assertEquals("simulated termination", expected.getMessage());
        }
        assertFalse(Files.exists(target));
        byte[] journal = Files.readAllBytes(store.journalFile());
        Path cli = fakeCodexExecutable();
        if (unsupported) Files.writeString(cli, "#!/bin/sh\nprintf '%s\\n' 'codex-cli 0.0.0'\n");
        CodexSkillInstallService service = new CodexSkillInstallService(projectAt(root),
                source(Optional.empty(), new GitHubSkillReleaseSource.OfflineException("fixture offline")), metadataRoot);
        CodexSkillOperationResult result = inBackground(() -> service.update(
                CodexSkillScope.PROJECT, cli, new EmptyProgressIndicator()));
        assertFalse(result.committed());
        if (unsupported) {
            assertEquals(CodexSkillStatus.State.CLIENT_UNSUPPORTED, result.status().state());
            assertFalse(result.changedFiles());
            assertFalse("Unsupported clients must leave the interrupted tree untouched", Files.exists(target));
            assertArrayEquals(journal, Files.readAllBytes(store.journalFile()));
        } else {
            assertEquals(CodexSkillStatus.State.OFFLINE, result.status().state());
            assertTrue("Recovery is an actual local file change even when the later network check fails", result.changedFiles());
            assertTrue(result.status().message().contains("restored"));
            assertArrayEquals(oldBytes, Files.readAllBytes(target.resolve("SKILL.md")));
            assertFalse(Files.exists(store.journalFile()));
        }
        assertArrayEquals(oldMetadata, Files.readAllBytes(store.metadataFile()));
    }

    private void assertPendingRecoveryClientGate(boolean unsupported) throws Exception {
        Assume.assumeTrue(CodexClientProbe.isVerifiedHost());
        Path root = tempDir("project-pending-recovery").toRealPath();
        Path metadataRoot = tempDir("metadata-pending-recovery").toRealPath();
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(root, CodexSkillScope.PROJECT, metadataRoot);
        Files.createDirectories(store.directory());
        byte[] journal = "pending fixture; inspection must not recover or parse it".getBytes(StandardCharsets.UTF_8);
        Files.write(store.journalFile(), journal);
        Path cli = fakeCodexExecutable();
        if (unsupported) {
            Files.writeString(cli, "#!/bin/sh\nprintf '%s\\n' 'codex-cli 0.0.0'\n");
        }
        SkillReleaseSource noNetwork = new SkillReleaseSource() {
            @Override public Optional<Candidate> latest(com.intellij.openapi.progress.ProgressIndicator indicator) {
                throw new AssertionError("Pending recovery must not query the network");
            }
            @Override public byte[] downloadArchive(Candidate candidate, com.intellij.openapi.progress.ProgressIndicator indicator) {
                throw new AssertionError("Pending recovery must not download an archive");
            }
        };
        CodexSkillInstallService service = new CodexSkillInstallService(projectAt(root), noNetwork, metadataRoot);
        CodexSkillStatus status = inBackground(() -> service.checkForUpdates(
                CodexSkillScope.PROJECT, cli, new EmptyProgressIndicator()));
        assertEquals("Unexpected recovery check status: " + status,
                unsupported ? CodexSkillStatus.State.CLIENT_UNSUPPORTED : CodexSkillStatus.State.RECOVERY_PENDING,
                status.state());
        assertEquals(unsupported ? "codex-cli 0.0.0" : CodexSkillInstallService.VERIFIED_CLIENT_VERSION,
                status.clientVersion());
        assertEquals(!unsupported, status.canUpdate());
        assertArrayEquals(journal, Files.readAllBytes(store.journalFile()));
        assertFalse(Files.exists(root.resolve(".agents")));
        assertFalse(Files.exists(store.metadataFile()));
        assertFalse(Files.exists(store.lockFile()));
        assertFalse(Files.exists(store.lastCheckFile()));
    }

    public void testOfflineCheckPreservesManagedSkillAndMetadataBytes() throws Exception {
        Assume.assumeTrue("Codex Skill lifecycle is currently verified on macOS Apple Silicon",
                CodexClientProbe.isVerifiedHost());
        Path root = tempDir("project-offline").toRealPath();
        Path target = root.resolve(".agents/skills/doc-view");
        Path cli = fakeCodexExecutable();
        Project project = projectAt(root);
        Path metadataRoot = tempDir("metadata-offline").toRealPath();
        byte[] skillBytes = "---\nname: doc-view\n---\nfixture\n".getBytes(StandardCharsets.UTF_8);
        SkillFixtureFactory.Fixture fixture = SkillFixtureFactory.fixture("1.7.0", Map.of("SKILL.md", skillBytes));
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(root, CodexSkillScope.PROJECT, metadataRoot);
        Files.createDirectories(target);
        Files.write(target.resolve("SKILL.md"), skillBytes);
        store.write(SkillInstallMetadata.from(CodexSkillScope.PROJECT, fixture.manifest(),
                CodexSkillInstallService.VERIFIED_CLIENT_VERSION, target.toString(), Instant.EPOCH));
        byte[] originalMetadata = Files.readAllBytes(store.metadataFile());
        SkillLastCheckStore checkStore = new SkillLastCheckStore(store.lastCheckFile());
        checkStore.write(Instant.EPOCH);
        byte[] originalCheckState = Files.readAllBytes(store.lastCheckFile());
        SkillReleaseSource source = source(Optional.empty(), new GitHubSkillReleaseSource.OfflineException("fixture offline"));
        CodexSkillInstallService service = new CodexSkillInstallService(project, source, metadataRoot);

        try {
            CodexSkillStatus status = inBackground(() -> service.checkForUpdates(
                    CodexSkillScope.PROJECT, cli, new EmptyProgressIndicator()));

            assertEquals(CodexSkillStatus.State.OFFLINE, status.state());
            assertEquals("An offline check displays the previous successful check time",
                    Instant.EPOCH, status.lastSuccessfulCheckAt());
            assertArrayEquals(skillBytes, Files.readAllBytes(target.resolve("SKILL.md")));
            assertArrayEquals(originalMetadata, Files.readAllBytes(store.metadataFile()));
            assertArrayEquals(originalCheckState, Files.readAllBytes(store.lastCheckFile()));
            assertFalse("Read-only update checks must not create the writer lock", Files.exists(store.lockFile()));
        } finally {
            deleteTree(target);
            deleteTree(store.directory());
        }
    }

    public void testSuccessfulReleaseLookupPersistsCheckTimeOutsideInstallMetadata() throws Exception {
        Assume.assumeTrue("Codex Skill lifecycle is currently verified on macOS Apple Silicon",
                CodexClientProbe.isVerifiedHost());
        Path root = tempDir("project-successful-check").toRealPath();
        Path target = root.resolve(".agents/skills/doc-view");
        Path cli = fakeCodexExecutable();
        Project project = projectAt(root);
        Path metadataRoot = tempDir("metadata-successful-check").toRealPath();
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(root, CodexSkillScope.PROJECT, metadataRoot);
        SkillReleaseSource source = source(Optional.empty(), null);
        CodexSkillInstallService service = new CodexSkillInstallService(project, source, metadataRoot);

        CodexSkillStatus status = inBackground(() -> service.checkForUpdates(
                CodexSkillScope.PROJECT, cli, new EmptyProgressIndicator()));

        assertEquals(CodexSkillStatus.State.NO_COMPATIBLE_RELEASE, status.state());
        assertTrue(status.lastSuccessfulCheckAt().isAfter(Instant.EPOCH));
        assertEquals(status.lastSuccessfulCheckAt(),
                new SkillLastCheckStore(store.lastCheckFile()).read().orElseThrow());
        assertFalse(Files.exists(target));
        assertFalse("Check for Updates must not write the install record", Files.exists(store.metadataFile()));
        assertFalse("Check for Updates must not create the install writer lock", Files.exists(store.lockFile()));
    }

    public void testLastCheckPersistenceFailureIsNotReportedAsOffline() throws Exception {
        Assume.assumeTrue("Codex Skill lifecycle is currently verified on macOS Apple Silicon",
                CodexClientProbe.isVerifiedHost());
        Path root = tempDir("project-check-state-failure").toRealPath();
        Path cli = fakeCodexExecutable();
        Project project = projectAt(root);
        Path metadataRoot = tempDir("metadata-check-state-failure").toRealPath();
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(root, CodexSkillScope.PROJECT, metadataRoot);
        SkillReleaseSource source = new SkillReleaseSource() {
            @Override
            public Optional<Candidate> latest(com.intellij.openapi.progress.ProgressIndicator indicator) throws IOException {
                indicator.checkCanceled();
                Files.createDirectories(store.lastCheckFile());
                return Optional.empty();
            }

            @Override
            public byte[] downloadArchive(Candidate candidate, com.intellij.openapi.progress.ProgressIndicator indicator) {
                throw new AssertionError("Check for Updates must not download a Skill archive");
            }
        };
        CodexSkillInstallService service = new CodexSkillInstallService(project, source, metadataRoot);

        CodexSkillStatus status = inBackground(() -> service.checkForUpdates(
                CodexSkillScope.PROJECT, cli, new EmptyProgressIndicator()));

        assertEquals("A local state write failure is an error, not an offline network result",
                CodexSkillStatus.State.ERROR, status.state());
        assertNull(status.lastSuccessfulCheckAt());
        assertFalse(Files.exists(store.metadataFile()));
        assertFalse(Files.exists(store.lockFile()));
    }

    public void testRejectsExpandedZipBombWithoutChangingManagedTargetOrInstallMetadata() throws Exception {
        Assume.assumeTrue("Codex Skill lifecycle is currently verified on macOS Apple Silicon",
                CodexClientProbe.isVerifiedHost());
        Path root = tempDir("project-zip-bomb").toRealPath();
        Path target = root.resolve(".agents/skills/doc-view");
        Path cli = fakeCodexExecutable();
        Project project = projectAt(root);
        Path metadataRoot = tempDir("metadata-zip-bomb").toRealPath();
        Map<String, byte[]> oldFiles = new LinkedHashMap<>();
        oldFiles.put("SKILL.md", "---\nname: doc-view\n---\nold managed skill\n".getBytes(StandardCharsets.UTF_8));
        oldFiles.put("references/guide.md", "old guide\n".getBytes(StandardCharsets.UTF_8));
        SkillFixtureFactory.Fixture oldRelease = SkillFixtureFactory.fixture("1.7.0", oldFiles);
        writeTree(target, oldFiles);
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(root, CodexSkillScope.PROJECT, metadataRoot);
        store.write(SkillInstallMetadata.from(CodexSkillScope.PROJECT, oldRelease.manifest(),
                CodexSkillInstallService.VERIFIED_CLIENT_VERSION, target.toString(), Instant.EPOCH));
        Map<String, String> originalTree = treeSnapshot(target);
        byte[] originalMetadata = Files.readAllBytes(store.metadataFile());

        SkillFixtureFactory.Fixture declaredRelease = SkillFixtureFactory.fixture("1.7.1",
                Map.of("SKILL.md", "---\nname: doc-view\n---\nnew skill\n".getBytes(StandardCharsets.UTF_8)));
        byte[] expandedContent = new byte[(int) SkillReleaseManifest.MAX_FILE_BYTES + 1];
        byte[] oversizedArchive = SkillFixtureFactory.zip(Map.of("SKILL.md", expandedContent));
        assertTrue("The compressed fixture must fit the archive limit", oversizedArchive.length < SkillReleaseManifest.MAX_ARCHIVE_BYTES);
        SkillReleaseManifest maliciousManifest = withArchiveBytes(declaredRelease.manifest(), oversizedArchive);
        SkillReleaseSource.Candidate candidate = new SkillReleaseSource.Candidate(maliciousManifest, 7123L);
        List<String> sourceCalls = new ArrayList<>();
        SkillReleaseSource releaseSource = new SkillReleaseSource() {
            @Override
            public Optional<Candidate> latest(com.intellij.openapi.progress.ProgressIndicator indicator) {
                indicator.checkCanceled();
                sourceCalls.add("latest");
                return Optional.of(candidate);
            }

            @Override
            public byte[] downloadArchive(Candidate requested, com.intellij.openapi.progress.ProgressIndicator indicator) {
                indicator.checkCanceled();
                assertEquals(candidate, requested);
                sourceCalls.add("download");
                return oversizedArchive;
            }
        };
        // This case verifies archive rejection, not cold shell startup. Warm and validate the same tiny CLI
        // fixture with a test-side allowance before exercising the production probe's unchanged 3s bound.
        warmAndValidateFakeCodexExecutable(cli);
        CodexSkillInstallService service = new CodexSkillInstallService(project, releaseSource, metadataRoot);

        CodexSkillOperationResult result = inBackground(() -> service.update(
                CodexSkillScope.PROJECT, cli, new EmptyProgressIndicator()));

        assertEquals(CodexSkillStatus.State.ERROR, result.status().state());
        assertFalse("Archive validation must stop before the install transaction", result.changedFiles());
        assertFalse(result.committed());
        assertEquals("Update stopped before the expected archive validation: " + result.status(),
                List.of("latest", "download"), sourceCalls);
        assertEquals("Every target file remains byte-for-byte unchanged", originalTree, treeSnapshot(target));
        assertArrayEquals("The install record remains byte-for-byte unchanged", originalMetadata,
                Files.readAllBytes(store.metadataFile()));
        assertFalse(Files.exists(store.journalFile(), LinkOption.NOFOLLOW_LINKS));
    }

    private Path fakeCodexExecutable() throws IOException {
        Path executable = Files.createTempFile("doc-view-skill-fake-codex-", ".sh");
        temporaryRoots.add(executable);
        Files.writeString(executable, "#!/bin/sh\nprintf '%s\\n' '" + CodexClientProbe.SUPPORTED_VERSION + "'\n",
                StandardCharsets.UTF_8);
        if (!executable.toFile().setExecutable(true)) throw new IOException("Could not mark fixture CLI executable");
        return executable;
    }

    private static void warmAndValidateFakeCodexExecutable(Path executable) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(executable.toString(), "--version")
                .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                throw new IOException("Test fake Codex executable did not exit during fixture warm-up");
            }
            byte[] output = process.getInputStream().readNBytes(4097);
            String version = new String(output, StandardCharsets.UTF_8).strip();
            if (output.length > 4096 || process.exitValue() != 0
                    || !CodexClientProbe.SUPPORTED_VERSION.equals(version)) {
                throw new IOException("Test fake Codex executable returned an unexpected version response");
            }
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            try {
                process.getInputStream().close();
            } catch (IOException ignored) {
                // The process has already exited or been terminated by this fixture.
            }
        }
    }

    private Path tempDir(String name) throws IOException {
        Path directory = Files.createTempDirectory("doc-view-skill-" + name + "-");
        temporaryRoots.add(directory);
        return directory;
    }

    private static Project projectAt(Path root) {
        return (Project) Proxy.newProxyInstance(Project.class.getClassLoader(), new Class<?>[]{Project.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getBasePath" -> root.toString();
                    case "isDisposed" -> false;
                    case "getName" -> "doc-view-skill-fixture";
                    case "toString" -> "Project[doc-view-skill-fixture]";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }

    private static SkillReleaseSource source(Optional<SkillReleaseSource.Candidate> result, IOException failure) {
        return new SkillReleaseSource() {
            @Override
            public Optional<Candidate> latest(com.intellij.openapi.progress.ProgressIndicator indicator) throws IOException {
                indicator.checkCanceled();
                if (failure != null) throw failure;
                return result;
            }

            @Override
            public byte[] downloadArchive(Candidate candidate, com.intellij.openapi.progress.ProgressIndicator indicator) {
                throw new AssertionError("Check for Updates must not download a Skill archive");
            }
        };
    }

    private static SkillReleaseManifest withArchiveBytes(SkillReleaseManifest base, byte[] archive) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("formatVersion", 1);
        root.addProperty("skillVersion", base.skillVersion());
        root.addProperty("releaseTag", base.releaseTag());
        root.addProperty("repository", SkillReleaseManifest.REPOSITORY);
        root.addProperty("commitSha", base.commitSha());
        root.addProperty("sourcePath", SkillReleaseManifest.SOURCE_PATH);
        root.addProperty("archiveRoot", SkillReleaseManifest.ARCHIVE_ROOT);
        root.addProperty("toolContractVersion", base.toolContractVersion());
        root.addProperty("toolContractFingerprint", base.toolContractFingerprint());
        JsonObject range = new JsonObject();
        range.addProperty("minimumInclusive", base.minimumPluginVersion());
        range.addProperty("maximumExclusive", base.maximumPluginVersionExclusive());
        root.add("pluginVersionRange", range);
        JsonArray files = new JsonArray();
        for (SkillReleaseManifest.FileRecord file : base.files()) {
            JsonObject record = new JsonObject();
            record.addProperty("path", file.path());
            record.addProperty("size", file.size());
            record.addProperty("sha256", file.sha256());
            files.add(record);
        }
        root.add("files", files);
        JsonObject archiveRecord = new JsonObject();
        archiveRecord.addProperty("file", base.archive().file());
        archiveRecord.addProperty("size", archive.length);
        archiveRecord.addProperty("sha256", SkillReleaseManifest.sha256(archive));
        root.add("archive", archiveRecord);
        return SkillReleaseManifest.parse(root.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void writeTree(Path root, Map<String, byte[]> files) throws IOException {
        for (var entry : files.entrySet()) {
            Path destination = root.resolve(entry.getKey()).normalize();
            if (!destination.startsWith(root)) throw new IOException("Fixture path escaped its test directory");
            Files.createDirectories(destination.getParent());
            Files.write(destination, entry.getValue());
        }
    }

    private static Map<String, String> treeSnapshot(Path root) throws IOException {
        Map<String, String> snapshot = new LinkedHashMap<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(candidate -> Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS))
                    .sorted().toList()) {
                snapshot.put(root.relativize(path).toString(),
                        SkillReleaseManifest.sha256(Files.readAllBytes(path)));
            }
        }
        return snapshot;
    }

    private static <T> T inBackground(java.util.concurrent.Callable<T> action) throws Exception {
        return ApplicationManager.getApplication().executeOnPooledThread(action).get(15, TimeUnit.SECONDS);
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
