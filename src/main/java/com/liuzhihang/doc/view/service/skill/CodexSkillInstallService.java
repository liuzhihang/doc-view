package com.liuzhihang.doc.view.service.skill;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.DocViewBuildInfo;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Scoped Codex Skill lifecycle. Process, network, and tree inspection run only in background operations. */
@Service(Service.Level.PROJECT)
public final class CodexSkillInstallService {
    public static final String VERIFIED_CLIENT_VERSION = CodexClientProbe.SUPPORTED_VERSION;
    private static final String TARGET_RELATIVE_PATH = ".agents/skills/doc-view";
    private static final Path VERIFIED_CHATGPT_CLI = Path.of(
            "/Applications/ChatGPT.app/Contents/Resources/codex-cli/bin/codex");
    private static final String NO_RELEASE_MESSAGE =
            "No compatible stable Doc View Skill release is available. No Skill files were changed.";

    private final Project project;
    private final CodexClientProbe clientProbe = new CodexClientProbe();
    private final SkillReleaseSource releaseSource;
    private final Path metadataStorageRoot;
    private volatile CodexSkillStatus projectStatus;
    private volatile CodexSkillStatus userStatus;
    private volatile SkillReleaseSource.Candidate projectCandidate;
    private volatile SkillReleaseSource.Candidate userCandidate;
    private volatile Instant projectLastSuccessfulCheckAt;
    private volatile Instant userLastSuccessfulCheckAt;

    public CodexSkillInstallService(@NotNull Project project) {
        this(project, new GitHubSkillReleaseSource());
    }

    CodexSkillInstallService(@NotNull Project project, @NotNull SkillReleaseSource releaseSource) {
        this(project, releaseSource, null);
    }

    CodexSkillInstallService(@NotNull Project project, @NotNull SkillReleaseSource releaseSource,
                             Path metadataStorageRoot) {
        this.project = project;
        this.releaseSource = releaseSource;
        this.metadataStorageRoot = metadataStorageRoot;
        this.projectStatus = CodexSkillStatus.unknown(CodexSkillScope.PROJECT,
                CodexSkillTarget.displayPath(project, CodexSkillScope.PROJECT));
        this.userStatus = CodexSkillStatus.unknown(CodexSkillScope.USER,
                CodexSkillTarget.displayPath(project, CodexSkillScope.USER));
    }

    public static CodexSkillInstallService getInstance(@NotNull Project project) {
        return project.getService(CodexSkillInstallService.class);
    }

    /** Returns cached in-memory state only; this method does no process launch, network access, or tree walk. */
    public static @NotNull CodexSkillStatus getStatus(@NotNull Project project) {
        return getStatus(project, CodexSkillScope.PROJECT);
    }

    /** Returns cached in-memory state for one scope; it does not inspect files or launch the client. */
    public static @NotNull CodexSkillStatus getStatus(@NotNull Project project, @NotNull CodexSkillScope scope) {
        return getInstance(project).status(scope);
    }

    /** The fixed repo-scope location proved discoverable by the supported Codex client build. */
    public static @NotNull Path targetPath(@NotNull Project project) {
        return targetPath(project, CodexSkillScope.PROJECT);
    }

    /** Returns the canonical destination for the selected discovery scope without creating it. */
    public static @NotNull Path targetPath(@NotNull Project project, @NotNull CodexSkillScope scope) {
        return CodexSkillTarget.displayPath(project, scope);
    }

    /** Returns the first existing Codex candidate; callers must validate its exact build in a background task. */
    public static @NotNull Optional<Path> defaultCodexExecutable() {
        return codexExecutableCandidates().stream().findFirst();
    }

    /** Existing bundle candidate has priority; PATH is only a discovery source, never a version trust signal. */
    public static @NotNull List<Path> codexExecutableCandidates() {
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();
        addExecutableCandidate(candidates, VERIFIED_CHATGPT_CLI);
        String path = System.getenv().getOrDefault("PATH", "");
        String executableName = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")
                ? "codex.exe" : "codex";
        for (String entry : path.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))) {
            if (entry == null || entry.isBlank()) continue;
            try {
                addExecutableCandidate(candidates, Path.of(entry).resolve(executableName));
            } catch (RuntimeException ignored) {
                // Ignore malformed PATH entries; the UI can expose a file chooser if no candidate is usable.
            }
        }
        return List.copyOf(candidates);
    }

    private static void addExecutableCandidate(LinkedHashSet<Path> candidates, Path path) {
        try {
            Path real = path.toRealPath();
            if (Files.isRegularFile(real, LinkOption.NOFOLLOW_LINKS) && Files.isExecutable(real)) candidates.add(real);
        } catch (IOException | RuntimeException ignored) {
            // Missing candidates are normal; do not surface them as install errors.
        }
    }

    /**
     * Checks the selected Codex binary against the exact discovery-tested version. Call from a cancellable
     * background task; discovery does not change Codex settings or roots.
     */
    public synchronized @NotNull CodexSkillStatus checkForUpdates(@NotNull Path codexExecutable,
                                                                   @NotNull ProgressIndicator indicator) {
        return checkForUpdates(CodexSkillScope.PROJECT, codexExecutable, indicator);
    }

    public synchronized @NotNull CodexSkillStatus checkForUpdates(@NotNull CodexSkillScope scope,
                                                                   @NotNull Path codexExecutable,
                                                                   @NotNull ProgressIndicator indicator) {
        requireBackgroundThread();
        setCandidate(scope, null);
        String clientVersion = "";
        try {
            Path projectRoot = validatedProjectRoot();
            CodexSkillTarget resolved = resolveTarget(scope);
            Path ownerRoot = resolved.ownerRoot();
            Path target = resolved.targetPath();
            if (!CodexClientProbe.isVerifiedHost()) {
                return publish(new CodexSkillStatus(CodexSkillStatus.State.PLATFORM_UNSUPPORTED,
                        "Skill discovery is currently verified only on macOS Apple Silicon.",
                        "", target, "", "", false, false, scope));
            }
            if (Files.isSymbolicLink(target)) {
                return publish(new CodexSkillStatus(CodexSkillStatus.State.UNMANAGED_CONFLICT,
                        "The Skill target is a symbolic link and was left untouched.",
                        "", target, "", "", false, false, scope));
            }
            validateTarget(ownerRoot, target, scope);
            SkillInstallMetadataStore store = metadataStore(ownerRoot, scope);
            Instant previousSuccessfulCheck = new SkillLastCheckStore(store.lastCheckFile()).read().orElse(null);
            setLastSuccessfulCheckAt(scope, previousSuccessfulCheck);
            SkillInstallMetadataStore.rejectPathLinks(store.journalFile());
            if (Files.exists(store.journalFile(), LinkOption.NOFOLLOW_LINKS)) {
                SkillInstallMetadataStore.rejectSymlink(store.journalFile());
                return publish(new CodexSkillStatus(CodexSkillStatus.State.RECOVERY_PENDING,
                        "A previous Skill file transaction needs recovery. Run Install or Update to restore its last complete state.",
                        "", target, "", "", true, true, scope));
            }
            clientVersion = clientProbe.version(codexExecutable, projectRoot, indicator);
            if (!VERIFIED_CLIENT_VERSION.equals(clientVersion)) {
                return publish(new CodexSkillStatus(CodexSkillStatus.State.CLIENT_UNSUPPORTED,
                        "Unsupported Codex build. Verified build: " + VERIFIED_CLIENT_VERSION + ".",
                        clientVersion, target, "", "", false, false, scope));
            }
            indicator.checkCanceled();
            SkillInstallMetadata metadata = null;
            try {
                metadata = store.read().orElse(null);
            } catch (IOException corrupt) {
                return publish(new CodexSkillStatus(CodexSkillStatus.State.MODIFIED_CONFLICT,
                        "Doc View installation metadata is missing or invalid; the target will not be changed.",
                        clientVersion, target, "", "", false, false, scope));
            }
            if (metadata != null && (metadata.scope() != scope || !metadata.targetPath().equals(target.toString()))) {
                return publish(new CodexSkillStatus(CodexSkillStatus.State.MODIFIED_CONFLICT,
                        "Doc View installation metadata belongs to another Skill scope or target.",
                        clientVersion, target, metadata.skillVersion(), "", false, false, scope));
            }
            boolean targetExists = Files.exists(target, LinkOption.NOFOLLOW_LINKS);
            if (targetExists && metadata == null) {
                return publish(new CodexSkillStatus(CodexSkillStatus.State.UNMANAGED_CONFLICT,
                        Files.isSymbolicLink(target)
                                ? "The Skill target is a symbolic link and was left untouched."
                                : "The Skill target already exists without Doc View install metadata and was left untouched.",
                        clientVersion, target, "", "", false, false, scope));
            }
            if (targetExists && (metadata == null || !metadata.targetPath().equals(target.toString())
                    || !SkillTreeVerifier.matches(target, metadata))) {
                return publish(new CodexSkillStatus(CodexSkillStatus.State.MODIFIED_CONFLICT,
                        "The installed Skill differs from Doc View's recorded files and was left untouched.",
                        clientVersion, target, metadata == null ? "" : metadata.skillVersion(), "", false, false, scope));
            }

            SkillReleaseSource.Candidate candidate;
            try {
                Optional<SkillReleaseSource.Candidate> found = releaseSource.latest(indicator);
                indicator.checkCanceled();
                Instant checkedAt = Instant.now();
                new SkillLastCheckStore(store.lastCheckFile()).write(checkedAt);
                setLastSuccessfulCheckAt(scope, checkedAt);
                if (found.isEmpty()) {
                    setCandidate(scope, null);
                    return publish(new CodexSkillStatus(CodexSkillStatus.State.NO_COMPATIBLE_RELEASE,
                            NO_RELEASE_MESSAGE, clientVersion, target,
                            metadata == null ? "" : metadata.skillVersion(), "", false, false, scope));
                }
                candidate = found.get();
            } catch (ProcessCanceledException cancelled) {
                throw cancelled;
            } catch (GitHubSkillReleaseSource.OfflineException unavailable) {
                setCandidate(scope, null);
                return publish(new CodexSkillStatus(CodexSkillStatus.State.OFFLINE,
                        "Could not check the official Doc View release. Existing Skill files were not changed.",
                        clientVersion, target, metadata == null ? "" : metadata.skillVersion(),
                        status(scope).availableVersion(), false, false, scope));
            } catch (IOException rejectedRelease) {
                setCandidate(scope, null);
                return publish(new CodexSkillStatus(CodexSkillStatus.State.ERROR,
                        safeMessage(rejectedRelease), clientVersion, target,
                        metadata == null ? "" : metadata.skillVersion(), "", false, false, scope));
            }
            setCandidate(scope, candidate);
            String available = candidate.manifest().skillVersion();
            try {
                candidate.manifest().validateCompatibility(DocViewBuildInfo.version(), SkillReleaseManifest.localContractBytes());
            } catch (IOException incompatible) {
                return publish(new CodexSkillStatus(CodexSkillStatus.State.INCOMPATIBLE,
                        incompatible.getMessage(), clientVersion, target,
                        metadata == null ? "" : metadata.skillVersion(), available, false, false, scope));
            }
            if (targetExists && metadata != null) {
                int compare = SkillReleaseManifest.compareVersion(available, metadata.skillVersion());
                if (compare > 0) {
                    return publish(new CodexSkillStatus(CodexSkillStatus.State.UPDATE_AVAILABLE,
                            "A compatible Doc View Skill update is available.", clientVersion, target,
                            metadata.skillVersion(), available, false, true, scope));
                }
                return publish(new CodexSkillStatus(CodexSkillStatus.State.CURRENT,
                        compare == 0 ? "The managed Doc View Skill is current."
                                : "The installed Skill is newer than the latest compatible release; downgrade is disabled.",
                        clientVersion, target, metadata.skillVersion(), available, false, false, scope));
            }
            return publish(new CodexSkillStatus(CodexSkillStatus.State.NOT_INSTALLED,
                    "A compatible Doc View Skill is available to install.", clientVersion, target,
                    metadata == null ? "" : metadata.skillVersion(), available, true, false, scope));
        } catch (ProcessCanceledException cancelled) {
            throw cancelled;
        } catch (UnsupportedProjectException unsupported) {
            return publish(new CodexSkillStatus(CodexSkillStatus.State.UNSUPPORTED_SCOPE,
                    unsupported.getMessage(), "", CodexSkillTarget.displayPath(project, scope), "", "", false, false, scope));
        } catch (IOException | RuntimeException failure) {
            if (failure instanceof ProcessCanceledException cancelled) throw cancelled;
            return publish(new CodexSkillStatus(CodexSkillStatus.State.ERROR,
                    safeMessage(failure), clientVersion, CodexSkillTarget.displayPath(project, scope), "", "", false, false, scope));
        }
    }

    /** Installs only the current official stable Skill after an explicit user action. */
    public synchronized @NotNull CodexSkillOperationResult install(@NotNull Path codexExecutable,
                                                                    @NotNull ProgressIndicator indicator) {
        return install(CodexSkillScope.PROJECT, codexExecutable, indicator);
    }

    /** Installs the current official stable Skill into the selected discovery scope. */
    public synchronized @NotNull CodexSkillOperationResult install(@NotNull CodexSkillScope scope,
                                                                    @NotNull Path codexExecutable,
                                                                    @NotNull ProgressIndicator indicator) {
        return installOrUpdate(scope, codexExecutable, indicator, false);
    }

    /** Updates only a clean Doc View-managed Skill after an explicit user action. */
    public synchronized @NotNull CodexSkillOperationResult update(@NotNull Path codexExecutable,
                                                                   @NotNull ProgressIndicator indicator) {
        return update(CodexSkillScope.PROJECT, codexExecutable, indicator);
    }

    /** Updates only a clean Doc View-managed Skill in the selected discovery scope. */
    public synchronized @NotNull CodexSkillOperationResult update(@NotNull CodexSkillScope scope,
                                                                   @NotNull Path codexExecutable,
                                                                   @NotNull ProgressIndicator indicator) {
        return installOrUpdate(scope, codexExecutable, indicator, true);
    }

    private CodexSkillOperationResult installOrUpdate(CodexSkillScope scope, Path codexExecutable,
                                                       ProgressIndicator indicator, boolean update) {
        requireBackgroundThread();
        Path displayTarget = CodexSkillTarget.displayPath(project, scope);
        if (!CodexClientProbe.isVerifiedHost()) {
            CodexSkillStatus unsupported = publish(new CodexSkillStatus(CodexSkillStatus.State.PLATFORM_UNSUPPORTED,
                    "Skill discovery is currently verified only on macOS Apple Silicon.",
                    "", displayTarget, "", "", false, false, scope));
            return new CodexSkillOperationResult(unsupported, false);
        }
        try {
            Path projectRoot = validatedProjectRoot();
            CodexSkillTarget resolved = resolveTarget(scope);
            Path ownerRoot = resolved.ownerRoot();
            Path target = resolved.targetPath();
            SkillInstallMetadataStore store = metadataStore(ownerRoot, scope);
            if (!SkillInstallTransaction.recoverPending(ownerRoot, target, store)) {
                return new CodexSkillOperationResult(publish(new CodexSkillStatus(
                        CodexSkillStatus.State.RECOVERY_PENDING,
                        "A previous Skill transaction could not be safely recovered; no further changes were made.",
                        "", target, "", "", false, false, scope)), true);
            }
            CodexSkillStatus status = checkForUpdates(scope, codexExecutable, indicator);
            if (update ? !status.canUpdate() : !status.canInstall()) {
                return new CodexSkillOperationResult(status, false);
            }
            SkillReleaseSource.Candidate candidate = candidate(scope);
            if (candidate == null || !candidate.manifest().skillVersion().equals(status.availableVersion())) {
                return new CodexSkillOperationResult(publish(new CodexSkillStatus(CodexSkillStatus.State.ERROR,
                        "The checked Skill release is no longer available. Check for updates again.",
                        status.clientVersion(), target, status.installedVersion(), status.availableVersion(), false, false, scope)), false);
            }
            byte[] archive = releaseSource.downloadArchive(candidate, indicator);
            Map<String, byte[]> files = SkillPackageArchive.validate(archive, candidate.manifest());
            candidate.manifest().validateCompatibility(DocViewBuildInfo.version(), SkillReleaseManifest.localContractBytes());
            indicator.checkCanceled();
            SkillInstallTransaction transaction = new SkillInstallTransaction(scope, ownerRoot, target, store,
                    candidate.manifest(), files, status.clientVersion(), DocViewBuildInfo.version(), indicator,
                    (skillFile, progress) -> clientProbe.discover(codexExecutable, projectRoot, resolved, progress).discovered());
            SkillInstallTransaction.Outcome outcome = transaction.execute();
            if (outcome.recoveryPending()) {
                CodexSkillStatus pending = publish(new CodexSkillStatus(CodexSkillStatus.State.RECOVERY_PENDING,
                        "The install reached a recoverable file state that needs another recovery attempt.",
                        status.clientVersion(), target, status.installedVersion(), status.availableVersion(), false, false, scope));
                return new CodexSkillOperationResult(pending, outcome.filesChanged());
            }
            CodexSkillStatus complete = publish(new CodexSkillStatus(CodexSkillStatus.State.CURRENT,
                    "Doc View Skill " + candidate.manifest().skillVersion() + " is installed and discovered by Codex.",
                    status.clientVersion(), target, candidate.manifest().skillVersion(), candidate.manifest().skillVersion(),
                    false, false, scope));
            return new CodexSkillOperationResult(complete, outcome.filesChanged(), outcome.committed());
        } catch (ProcessCanceledException cancelled) {
            throw cancelled;
        } catch (GitHubSkillReleaseSource.OfflineException unavailable) {
            CodexSkillStatus previous = status(scope);
            CodexSkillStatus status = publish(new CodexSkillStatus(CodexSkillStatus.State.OFFLINE,
                    "Could not download the official Skill archive. The installed files were not changed.",
                    previous.clientVersion(), previous.targetPath(), previous.installedVersion(),
                    previous.availableVersion(), false, false, scope));
            return new CodexSkillOperationResult(status, false);
        } catch (SkillInstallTransaction.RecoveryPendingException pending) {
            CodexSkillStatus status = publish(new CodexSkillStatus(CodexSkillStatus.State.RECOVERY_PENDING,
                    safeMessage(pending), "", displayTarget, "", "", true, true, scope));
            return new CodexSkillOperationResult(status, true);
        } catch (IOException | RuntimeException failure) {
            if (failure instanceof ProcessCanceledException cancelled) throw cancelled;
            CodexSkillStatus status = publish(new CodexSkillStatus(CodexSkillStatus.State.ERROR,
                    safeMessage(failure), "", displayTarget, "", "", false, false, scope));
            return new CodexSkillOperationResult(status, false);
        }
    }

    private CodexSkillStatus publish(CodexSkillStatus status) {
        Instant lastSuccessfulCheck = status.lastSuccessfulCheckAt() == null
                ? lastSuccessfulCheckAt(status.scope()) : status.lastSuccessfulCheckAt();
        if (!java.util.Objects.equals(status.lastSuccessfulCheckAt(), lastSuccessfulCheck)) {
            status = status.withLastSuccessfulCheckAt(lastSuccessfulCheck);
        }
        if (status.scope() == CodexSkillScope.USER) userStatus = status;
        else projectStatus = status;
        return status;
    }

    private Instant lastSuccessfulCheckAt(CodexSkillScope scope) {
        return scope == CodexSkillScope.USER ? userLastSuccessfulCheckAt : projectLastSuccessfulCheckAt;
    }

    private void setLastSuccessfulCheckAt(CodexSkillScope scope, Instant checkedAt) {
        if (scope == CodexSkillScope.USER) userLastSuccessfulCheckAt = checkedAt;
        else projectLastSuccessfulCheckAt = checkedAt;
    }

    private CodexSkillStatus status(CodexSkillScope scope) {
        return scope == CodexSkillScope.USER ? userStatus : projectStatus;
    }

    private SkillReleaseSource.Candidate candidate(CodexSkillScope scope) {
        return scope == CodexSkillScope.USER ? userCandidate : projectCandidate;
    }

    private void setCandidate(CodexSkillScope scope, SkillReleaseSource.Candidate candidate) {
        if (scope == CodexSkillScope.USER) userCandidate = candidate;
        else projectCandidate = candidate;
    }

    private CodexSkillTarget resolveTarget(CodexSkillScope scope) throws IOException {
        try {
            return CodexSkillTarget.resolve(project, scope);
        } catch (IOException failure) {
            if (scope == CodexSkillScope.PROJECT) throw new UnsupportedProjectException(failure.getMessage());
            throw failure;
        }
    }

    private SkillInstallMetadataStore metadataStore(Path ownerRoot, CodexSkillScope scope) throws IOException {
        return metadataStorageRoot == null
                ? new SkillInstallMetadataStore(ownerRoot, scope)
                : new SkillInstallMetadataStore(ownerRoot, scope, metadataStorageRoot);
    }

    private Path validatedProjectRoot() throws IOException {
        if (project.isDisposed() || project.getBasePath() == null || project.getBasePath().isBlank()) {
            throw new UnsupportedProjectException("Open a local project before checking the Project Skill.");
        }
        Path root = Path.of(project.getBasePath());
        if (!root.isAbsolute() || !Files.isDirectory(root)) {
            throw new UnsupportedProjectException("The selected project does not have a local directory.");
        }
        return root.toRealPath();
    }

    private static void validateTarget(Path root, Path target, CodexSkillScope scope) throws IOException {
        if (!target.startsWith(root) || !target.equals(root.resolve(TARGET_RELATIVE_PATH).normalize())) {
            throw new IOException("The Skill target is outside the selected scope root.");
        }
        Path current = root;
        for (Path component : Path.of(TARGET_RELATIVE_PATH)) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) {
                throw new IOException("The " + scope + " Skill path contains a symbolic link and was left untouched.");
            }
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)
                    && !current.equals(target)) {
                throw new IOException("The Skill parent path is not a directory.");
            }
        }
    }

    private static void requireBackgroundThread() {
        if (ApplicationManager.getApplication().isDispatchThread()
                || ApplicationManager.getApplication().isReadAccessAllowed()) {
            throw new IllegalStateException("Codex Skill checks must run in a background task without read access");
        }
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? "Codex Skill check failed. No Skill files were changed." : message;
    }

    private static final class UnsupportedProjectException extends IOException {
        private UnsupportedProjectException(String message) { super(message); }
    }
}
