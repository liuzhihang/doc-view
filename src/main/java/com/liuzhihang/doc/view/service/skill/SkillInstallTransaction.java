package com.liuzhihang.doc.view.service.skill;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Two-rename, journaled scoped Skill install/update with conservative recovery. */
final class SkillInstallTransaction {
    enum Phase { PREPARING, READY, OLD_BACKED_UP, EXTERNAL_CHANGE_RESTORE_PENDING, NEW_IN_PLACE, COMMITTED }
    record Outcome(boolean committed, boolean recoveryPending, boolean filesChanged) {}
    static final class RecoveryPendingException extends IOException {
        RecoveryPendingException(String message, Throwable cause) { super(message, cause); }
    }

    @FunctionalInterface
    interface DiscoveryVerifier {
        boolean verify(Path expectedSkillFile, ProgressIndicator indicator) throws IOException;
    }

    @FunctionalInterface
    interface PhaseHook {
        void after(Phase phase) throws IOException;
    }

    private final CodexSkillScope scope;
    private final Path ownerRoot;
    private final Path target;
    private final SkillInstallMetadataStore store;
    private final SkillReleaseManifest manifest;
    private final Map<String, byte[]> files;
    private final String clientVersion;
    private final String pluginVersion;
    private final ProgressIndicator indicator;
    private final DiscoveryVerifier discoveryVerifier;
    private final PhaseHook hook;

    SkillInstallTransaction(Path projectRoot, Path target, SkillInstallMetadataStore store,
                            SkillReleaseManifest manifest, Map<String, byte[]> files,
                            String clientVersion, String pluginVersion, ProgressIndicator indicator,
                            DiscoveryVerifier discoveryVerifier) {
        this(CodexSkillScope.PROJECT, projectRoot, target, store, manifest, files, clientVersion, pluginVersion, indicator,
                discoveryVerifier, phase -> {});
    }

    SkillInstallTransaction(Path projectRoot, Path target, SkillInstallMetadataStore store,
                            SkillReleaseManifest manifest, Map<String, byte[]> files,
                            String clientVersion, String pluginVersion, ProgressIndicator indicator,
                            DiscoveryVerifier discoveryVerifier, PhaseHook hook) {
        this(CodexSkillScope.PROJECT, projectRoot, target, store, manifest, files, clientVersion,
                pluginVersion, indicator, discoveryVerifier, hook);
    }

    SkillInstallTransaction(CodexSkillScope scope, Path ownerRoot, Path target, SkillInstallMetadataStore store,
                            SkillReleaseManifest manifest, Map<String, byte[]> files,
                            String clientVersion, String pluginVersion, ProgressIndicator indicator,
                            DiscoveryVerifier discoveryVerifier) {
        this(scope, ownerRoot, target, store, manifest, files, clientVersion, pluginVersion, indicator,
                discoveryVerifier, phase -> {});
    }

    SkillInstallTransaction(CodexSkillScope scope, Path ownerRoot, Path target, SkillInstallMetadataStore store,
                            SkillReleaseManifest manifest, Map<String, byte[]> files,
                            String clientVersion, String pluginVersion, ProgressIndicator indicator,
                            DiscoveryVerifier discoveryVerifier, PhaseHook hook) {
        this.scope = scope;
        this.ownerRoot = ownerRoot;
        this.target = target;
        this.store = store;
        this.manifest = manifest;
        this.files = files;
        this.clientVersion = clientVersion;
        this.pluginVersion = pluginVersion;
        this.indicator = indicator;
        this.discoveryVerifier = discoveryVerifier;
        this.hook = hook;
    }

    Outcome execute() throws IOException {
        indicator.checkCanceled();
        ensureScopeTarget();
        store.ensureDirectory();
        ensureSafeOwnerParents(ownerRoot, target.getParent());
        SkillInstallMetadataStore.rejectSymlink(store.lockFile());
        try (FileChannel channel = FileChannel.open(store.lockFile(), StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            FileLock lock = store.tryLock(channel);
            if (lock == null) throw new IOException("Another Doc View Skill install or update is already running");
            try (lock) {
                if (Files.exists(store.journalFile(), LinkOption.NOFOLLOW_LINKS)) {
                    if (!recover()) return new Outcome(false, true, true);
                }
                return executeLocked();
            }
        }
    }

    static boolean recover(Path target, SkillInstallMetadataStore store) throws IOException {
        if (!Files.exists(store.journalFile(), LinkOption.NOFOLLOW_LINKS)) return true;
        SkillInstallMetadataStore.rejectSymlink(store.journalFile());
        long size = Files.size(store.journalFile());
        if (size < 1 || size > 128 * 1024) return false;
        Journal journal;
        try {
            journal = Journal.parse(Files.readAllBytes(store.journalFile()));
        } catch (IOException | RuntimeException invalid) {
            return false;
        }
        Path expectedProjectRoot = target.getParent() == null || target.getParent().getParent() == null
                || target.getParent().getParent().getParent() == null ? null
                : target.getParent().getParent().getParent();
        if (expectedProjectRoot == null || journal.scope() != store.scope()
                || !journal.ownerRoot().equals(expectedProjectRoot.toString())
                || !journal.target().equals(target.toAbsolutePath().normalize().toString())
                || !journal.stage().getParent().equals(target.getParent())
                || !journal.backup().getParent().equals(target.getParent())
                || !journal.metadataBackup().getParent().equals(store.directory())
                || !safeTransactionName(journal.stage(), ".doc-view-skill-stage-")
                || !safeTransactionName(journal.backup(), ".doc-view-skill-backup-")
                || !safeTransactionName(journal.metadataBackup(), ".metadata-backup-")) {
            return false;
        }
        Path ownerRootPath = Path.of(journal.ownerRoot()).toAbsolutePath().normalize();
        Set<Path> allowedMissingParents = Set.of(ownerRootPath.resolve(".agents"),
                ownerRootPath.resolve(".agents").resolve("skills"));
        for (String value : journal.createdParents()) {
            Path created = Path.of(value).toAbsolutePath().normalize();
            if (!allowedMissingParents.contains(created)) return false;
        }
        SkillInstallMetadata newMetadata = SkillInstallMetadata.parse(journal.newMetadata());
        if (newMetadata.scope() != journal.scope()) return false;
        if (journal.phase() == Phase.COMMITTED) {
            if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS) || !SkillTreeVerifier.matches(target, newMetadata)) return false;
            OptionalMetadata current = readMetadataBytes(store.metadataFile());
            if (!current.exists() || !SkillInstallMetadata.parse(current.bytes()).equals(newMetadata)) return false;
            if (journal.targetHadOld()) {
                SkillInstallMetadata oldMetadata = readOldMetadata(journal, store);
                if (oldMetadata == null || oldMetadata.scope() != journal.scope()
                        || Files.exists(journal.backup(), LinkOption.NOFOLLOW_LINKS)
                        && !SkillTreeVerifier.matches(journal.backup(), oldMetadata)) return false;
            }
            try {
                cleanup(journal.stage(), journal.backup(), journal.metadataBackup(), store.journalFile());
                return !Files.exists(store.journalFile(), LinkOption.NOFOLLOW_LINKS);
            } catch (IOException cleanupFailure) {
                return false;
            }
        }

        boolean backupExists = Files.exists(journal.backup(), LinkOption.NOFOLLOW_LINKS);
        boolean stageExists = Files.exists(journal.stage(), LinkOption.NOFOLLOW_LINKS);
        if (journal.phase() == Phase.EXTERNAL_CHANGE_RESTORE_PENDING) {
            return recoverExternalChangeRestore(journal, target, store, backupExists);
        }
        if (!backupExists && (journal.phase() == Phase.READY
                || journal.phase() == Phase.OLD_BACKED_UP && !journal.targetHadOld() && stageExists)) {
            // No previous tree was moved to backup. Preserve the target exactly as found and discard only
            // this transaction's staging and journal. This also covers a concurrent first-install target.
            cleanup(journal.stage(), null, journal.metadataBackup(), store.journalFile());
            removeCreatedParents(journal.createdParents());
            return !Files.exists(store.journalFile(), LinkOption.NOFOLLOW_LINKS);
        }
        if (backupExists && journal.targetHadOld() && !Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            SkillInstallMetadata oldMetadata = readOldMetadata(journal, store);
            if (oldMetadata == null || oldMetadata.scope() != journal.scope()) return false;
            if (!SkillTreeVerifier.matches(journal.backup(), oldMetadata)) {
                // The old target changed in the final check/rename window. Persist the restoration phase
                // before moving those unknown bytes back so a later invocation can finish safely.
                journal = journal.withPhase(Phase.EXTERNAL_CHANGE_RESTORE_PENDING);
                SkillInstallMetadataStore.atomicWrite(store.journalFile(), journal.encode());
                if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) atomicMove(journal.backup(), target);
                if (Files.exists(journal.backup(), LinkOption.NOFOLLOW_LINKS)
                        || !Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return false;
                if (!restoreOldMetadata(journal, store)) return false;
                cleanup(journal.stage(), null, journal.metadataBackup(), store.journalFile());
                return !Files.exists(store.journalFile(), LinkOption.NOFOLLOW_LINKS);
            }
        }
        if (backupExists && journal.targetHadOld() && Files.exists(target, LinkOption.NOFOLLOW_LINKS)
                && SkillTreeVerifier.matches(target, newMetadata)) {
            SkillInstallMetadata oldMetadata = readOldMetadata(journal, store);
            if (oldMetadata == null || oldMetadata.scope() != journal.scope()
                    || !SkillTreeVerifier.matches(journal.backup(), oldMetadata)) return false;
        }
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            if (backupExists || !journal.targetHadOld()) {
                // If staging still exists, the target was not installed by our stage rename. Preserve it.
                if (stageExists || !SkillTreeVerifier.matches(target, newMetadata)) return false;
                deleteTree(target);
            } else {
                // Before the first rename, the original target must still match the old install record.
                SkillInstallMetadata oldMetadata = readOldMetadata(journal, store);
            if (oldMetadata == null || oldMetadata.scope() != journal.scope()
                    || !SkillTreeVerifier.matches(target, oldMetadata)) return false;
            }
        }
        if (backupExists) {
            SkillInstallMetadata oldMetadata = readOldMetadata(journal, store);
            if (oldMetadata == null || oldMetadata.scope() != journal.scope()
                    || !SkillTreeVerifier.matches(journal.backup(), oldMetadata)) return false;
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return false;
            atomicMove(journal.backup(), target);
        } else if (journal.targetHadOld() && !Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }

        if (journal.metadataHadOld()) {
            if (Files.exists(journal.metadataBackup(), LinkOption.NOFOLLOW_LINKS)) {
                OptionalMetadata saved = readMetadataBytes(journal.metadataBackup());
                if (!saved.exists()) return false;
                byte[] previous = saved.bytes();
                if (!SkillReleaseManifest.sha256(previous).equals(journal.oldMetadataSha256())
                        || !SkillInstallMetadata.parse(previous).equals(journal.oldMetadata())) return false;
                SkillInstallMetadataStore.atomicWrite(store.metadataFile(), previous);
            } else {
                OptionalMetadata current = readMetadataBytes(store.metadataFile());
                if (!current.exists() || !SkillReleaseManifest.sha256(current.bytes()).equals(journal.oldMetadataSha256())
                        || !SkillInstallMetadata.parse(current.bytes()).equals(journal.oldMetadata())) return false;
            }
        } else if (Files.exists(store.metadataFile(), LinkOption.NOFOLLOW_LINKS)) {
            SkillInstallMetadataStore.rejectSymlink(store.metadataFile());
            SkillInstallMetadata current = SkillInstallMetadata.parse(Files.readAllBytes(store.metadataFile()));
            if (!current.equals(newMetadata)) return false;
            Files.delete(store.metadataFile());
        }
        cleanup(journal.stage(), null, journal.metadataBackup(), store.journalFile());
        removeCreatedParents(journal.createdParents());
        return !Files.exists(store.journalFile(), LinkOption.NOFOLLOW_LINKS);
    }

    static boolean recoverPending(Path ownerRoot, Path target, SkillInstallMetadataStore store) throws IOException {
        Path expectedProjectRoot = target.getParent() == null || target.getParent().getParent() == null
                || target.getParent().getParent().getParent() == null ? null
                : target.getParent().getParent().getParent();
        if (expectedProjectRoot == null || !ownerRoot.toRealPath().equals(expectedProjectRoot)
                || !store.ownerRoot().equals(expectedProjectRoot)
                || !target.equals(expectedProjectRoot.resolve(".agents/skills/doc-view"))
                || !store.lockFile().equals(target.getParent().resolve(".doc-view-skill-install.lock"))) return false;
        if (!Files.exists(store.journalFile(), LinkOption.NOFOLLOW_LINKS)) return true;
        store.ensureDirectory();
        ensureSafeOwnerParents(ownerRoot, target.getParent());
        SkillInstallMetadataStore.rejectSymlink(store.lockFile());
        try (FileChannel channel = FileChannel.open(store.lockFile(), StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            FileLock lock = store.tryLock(channel);
            if (lock == null) return false;
            try (lock) {
                try { return recover(target, store); }
                catch (IOException unsafeState) { return false; }
            }
        }
    }

    private Outcome executeLocked() throws IOException {
        Path targetParent = target.getParent();
        if (targetParent == null) throw new IOException("Skill target has no parent");
        if (store.scope() != scope || !store.ownerRoot().equals(ownerRoot.toRealPath())) {
            throw new IOException("Skill install state belongs to a different scope or owner root");
        }
        SkillInstallMetadata oldMetadata = store.read().orElse(null);
        boolean oldTargetExists = Files.exists(target, LinkOption.NOFOLLOW_LINKS);
        if (oldTargetExists) {
            if (oldMetadata == null || oldMetadata.scope() != scope || !oldMetadata.targetPath().equals(target.toString())
                    || !SkillTreeVerifier.matches(target, oldMetadata)) {
                throw new IOException("Existing Skill is unmanaged or modified; it was left untouched");
            }
        } else if (oldMetadata != null && !oldMetadata.targetPath().equals(target.toString())) {
            throw new IOException("Stored Skill metadata belongs to a different target path");
        }
        if (oldTargetExists && SkillReleaseManifest.compareVersion(manifest.skillVersion(), oldMetadata.skillVersion()) <= 0) {
            throw new IOException("The requested Skill version is not newer than the managed installation");
        }

        SkillInstallMetadata newMetadata = SkillInstallMetadata.from(scope, manifest, clientVersion, target.toString(), Instant.now());
        String token = UUID.randomUUID().toString();
        Path stage = targetParent.resolve(".doc-view-skill-stage-" + token);
        Path backup = targetParent.resolve(".doc-view-skill-backup-" + token);
        Path metadataBackup = store.directory().resolve(".metadata-backup-" + token);
        byte[] oldMetadataBytes = oldMetadata == null ? null : Files.readAllBytes(store.metadataFile());
        List<String> createdParents = createdProjectParents(targetParent);
        Journal journal = new Journal(Phase.PREPARING, scope, ownerRoot.toAbsolutePath().normalize().toString(),
                target.toAbsolutePath().normalize().toString(), stage, backup, metadataBackup, oldTargetExists,
                oldMetadata != null, oldMetadata, oldMetadataBytes == null ? "" : SkillReleaseManifest.sha256(oldMetadataBytes),
                createdParents, newMetadata.encode());
        writeJournal(journal);
        boolean committed = false;
        try {
            hook.after(Phase.PREPARING);
            if (oldMetadataBytes != null) {
                SkillInstallMetadataStore.atomicWrite(metadataBackup, oldMetadataBytes);
            }
            createSafeScopeParents(targetParent);
            Files.createDirectory(stage);
            writeStage(stage, newMetadata);
            if (!SkillTreeVerifier.matches(stage, newMetadata)) throw new IOException("Staged Skill failed its file inventory check");
            indicator.checkCanceled();
            if (oldTargetExists && !SkillTreeVerifier.matches(target, oldMetadata)) {
                throw new IOException("Managed Skill changed while the update was staged; it was left untouched");
            }
            if (!oldTargetExists && Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Skill target appeared while installation was staged; it was left untouched");
            }
            journal = journal.withPhase(Phase.READY);
            writeJournal(journal);
            hook.after(Phase.READY);

            if (oldTargetExists && !SkillTreeVerifier.matches(target, oldMetadata)) {
                throw new IOException("Managed Skill changed while the update was staged; it was left untouched");
            }
            if (!oldTargetExists && Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Skill target appeared while installation was staged; it was left untouched");
            }

            // From the first rename to the durable terminal record, cancellation is deferred and failures roll back.
            if (oldTargetExists) atomicMove(target, backup);
            journal = journal.withPhase(Phase.OLD_BACKED_UP);
            writeJournal(journal);
            hook.after(Phase.OLD_BACKED_UP);

            if (oldTargetExists && !SkillTreeVerifier.matches(backup, oldMetadata)) {
                throw new IOException("Managed Skill changed during update commit; external bytes were preserved and the update was not applied");
            }

            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Skill target was concurrently recreated during commit");
            }
            atomicMove(stage, target);
            journal = journal.withPhase(Phase.NEW_IN_PLACE);
            writeJournal(journal);
            hook.after(Phase.NEW_IN_PLACE);

            store.write(newMetadata);
            if (!discoveryVerifier.verify(target.resolve("SKILL.md"), indicator)) {
                throw new IOException("Codex did not discover the installed Skill at the verified path");
            }
            if (!SkillTreeVerifier.matches(target, newMetadata)) {
                throw new IOException("The new Skill changed before commit; recovery is required");
            }
            if (oldTargetExists && !SkillTreeVerifier.matches(backup, oldMetadata)) {
                throw new IOException("The previous Skill backup changed before commit; recovery is required");
            }
            journal = journal.withPhase(Phase.COMMITTED);
            writeJournal(journal);
            committed = true;
        } catch (ProcessCanceledException cancelled) {
            if (!recover()) throw new RecoveryPendingException("Skill operation was cancelled; recovery is pending", cancelled);
            throw cancelled;
        } catch (IOException | RuntimeException failure) {
            if (!recover()) throw new RecoveryPendingException("Skill operation failed; recovery is pending", failure);
            if (failure instanceof IOException io) throw io;
            throw failure;
        }

        if (!committed) return new Outcome(false, true, true);
        try {
            cleanup(stage, backup, metadataBackup, store.journalFile());
            if (Files.exists(store.journalFile(), LinkOption.NOFOLLOW_LINKS)) {
                return new Outcome(false, true, true);
            }
        } catch (IOException cleanupFailure) {
            return new Outcome(false, true, true);
        }
        return new Outcome(true, false, true);
    }

    private void writeStage(Path stage, SkillInstallMetadata metadata) throws IOException {
        manifest.validateCompatibility(pluginVersion, SkillReleaseManifest.localContractBytes());
        if (files.size() != manifest.files().size()) throw new IOException("Validated Skill file count changed before install");
        for (var file : manifest.files()) {
            indicator.checkCanceled();
            byte[] content = files.get(file.path());
            if (content == null || content.length != file.size()
                    || !SkillReleaseManifest.sha256(content).equals(file.sha256())) {
                throw new IOException("Validated Skill content changed before install");
            }
            SkillReleaseManifest.validateMetadataPath(file.path());
            Path destination = stage.resolve(file.path()).normalize();
            if (!destination.startsWith(stage)) throw new IOException("Skill path escaped its staging directory");
            Path parent = destination.getParent();
            if (parent != null) Files.createDirectories(parent);
            SkillInstallMetadataStore.rejectPathLinks(destination.getParent() == null ? stage : destination.getParent());
            Files.write(destination, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
    }

    private void createSafeScopeParents(Path parent) throws IOException {
        if (!parent.startsWith(ownerRoot)) throw new IOException("Skill parent is outside the selected scope root");
        Path current = ownerRoot;
        for (Path component : ownerRoot.relativize(parent)) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) throw new IOException("Skill parent contains a symbolic link");
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(current);
            if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Skill parent is not a directory");
        }
    }

    private static void ensureSafeOwnerParents(Path ownerRoot, Path parent) throws IOException {
        if (parent == null || !parent.startsWith(ownerRoot)) {
            throw new IOException("Skill parent is outside the selected scope root");
        }
        Path current = ownerRoot;
        for (Path component : ownerRoot.relativize(parent)) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) throw new IOException("Skill parent contains a symbolic link");
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(current);
            if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Skill parent is not a directory");
        }
    }

    private List<String> createdProjectParents(Path parent) throws IOException {
        List<String> missing = new ArrayList<>();
        Path current = ownerRoot;
        for (Path component : ownerRoot.relativize(parent)) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) throw new IOException("Skill parent contains a symbolic link");
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) missing.add(current.toString());
            else if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Skill parent is not a directory");
        }
        return List.copyOf(missing);
    }

    private boolean recover() throws IOException {
        try { return recover(target, store); }
        catch (IOException unsafeState) { return false; }
    }

    private void writeJournal(Journal journal) throws IOException {
        SkillInstallMetadataStore.atomicWrite(store.journalFile(), journal.encode());
    }

    private void ensureScopeTarget() throws IOException {
        Path realRoot = ownerRoot.toRealPath();
        Path expected = realRoot.resolve(".agents/skills/doc-view").normalize();
        if (!realRoot.equals(ownerRoot.toAbsolutePath().normalize())
                || !target.toAbsolutePath().normalize().equals(expected)
                || store.scope() != scope || !store.ownerRoot().equals(realRoot)) {
            throw new IOException("Skill target does not resolve inside the selected scope root");
        }
        Path current = realRoot;
        for (Path component : realRoot.relativize(target.toAbsolutePath().normalize())) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) throw new IOException("Skill target path contains a symbolic link");
        }
    }

    private static SkillInstallMetadata readOldMetadata(Journal journal, SkillInstallMetadataStore store) throws IOException {
        if (journal.oldMetadata() != null) return journal.oldMetadata();
        if (!journal.metadataHadOld()) return null;
        if (Files.exists(journal.metadataBackup(), LinkOption.NOFOLLOW_LINKS)) {
            return SkillInstallMetadata.parse(Files.readAllBytes(journal.metadataBackup()));
        }
        OptionalMetadata bytes = readMetadataBytes(store.metadataFile());
        return bytes.exists() ? SkillInstallMetadata.parse(bytes.bytes()) : null;
    }

    private static boolean recoverExternalChangeRestore(Journal journal, Path target,
                                                        SkillInstallMetadataStore store,
                                                        boolean backupExists) throws IOException {
        if (!journal.targetHadOld() || !journal.metadataHadOld()) return false;
        if (backupExists) {
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return false;
            atomicMove(journal.backup(), target);
        }
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return false;
        if (!restoreOldMetadata(journal, store)) return false;
        cleanup(journal.stage(), null, journal.metadataBackup(), store.journalFile());
        return !Files.exists(store.journalFile(), LinkOption.NOFOLLOW_LINKS);
    }

    private static boolean restoreOldMetadata(Journal journal, SkillInstallMetadataStore store) throws IOException {
        if (journal.metadataHadOld()) {
            OptionalMetadata current = readMetadataBytes(store.metadataFile());
            if (current.exists() && SkillReleaseManifest.sha256(current.bytes()).equals(journal.oldMetadataSha256())
                    && SkillInstallMetadata.parse(current.bytes()).equals(journal.oldMetadata())) return true;
            if (Files.exists(journal.metadataBackup(), LinkOption.NOFOLLOW_LINKS)) {
                OptionalMetadata saved = readMetadataBytes(journal.metadataBackup());
                if (!saved.exists() || !SkillReleaseManifest.sha256(saved.bytes()).equals(journal.oldMetadataSha256())
                        || !SkillInstallMetadata.parse(saved.bytes()).equals(journal.oldMetadata())) return false;
                SkillInstallMetadataStore.atomicWrite(store.metadataFile(), saved.bytes());
                return true;
            }
            return current.exists() && SkillReleaseManifest.sha256(current.bytes()).equals(journal.oldMetadataSha256())
                    && SkillInstallMetadata.parse(current.bytes()).equals(journal.oldMetadata());
        }
        if (!Files.exists(store.metadataFile(), LinkOption.NOFOLLOW_LINKS)) return true;
        SkillInstallMetadataStore.rejectSymlink(store.metadataFile());
        SkillInstallMetadata current = SkillInstallMetadata.parse(Files.readAllBytes(store.metadataFile()));
        if (!current.equals(SkillInstallMetadata.parse(journal.newMetadata()))) return false;
        Files.delete(store.metadataFile());
        return true;
    }

    private static OptionalMetadata readMetadataBytes(Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return new OptionalMetadata(false, new byte[0]);
        SkillInstallMetadataStore.rejectSymlink(path);
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length > 64 * 1024) throw new IOException("Skill metadata exceeds its size limit");
        return new OptionalMetadata(true, bytes);
    }

    private static void atomicMove(Path source, Path destination) throws IOException {
        SkillInstallMetadataStore.rejectSymlink(source);
        SkillInstallMetadataStore.rejectSymlink(destination);
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            throw new IOException("Atomic Skill directory replacement is not supported on this filesystem", unsupported);
        }
    }

    private static boolean safeTransactionName(Path path, String prefix) {
        String name = path.getFileName() == null ? "" : path.getFileName().toString();
        return name.startsWith(prefix) && name.length() > prefix.length()
                && name.substring(prefix.length()).matches("[0-9a-f-]{36}");
    }

    private static void cleanup(Path stage, Path backup, Path metadataBackup, Path journal) throws IOException {
        if (backup != null) deleteTree(backup);
        deleteTree(stage);
        Files.deleteIfExists(metadataBackup);
        Files.deleteIfExists(journal);
    }

    private static void deleteTree(Path path) throws IOException {
        if (path == null || !Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path directory, IOException failure) throws IOException {
                if (failure != null) throw failure;
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void removeCreatedParents(List<String> parents) throws IOException {
        for (int index = parents.size() - 1; index >= 0; index--) {
            Path path = Path.of(parents.get(index));
            if (Files.isSymbolicLink(path) || !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) continue;
            try (var children = Files.list(path)) {
                if (children.findAny().isPresent()) continue;
            }
            Files.deleteIfExists(path);
        }
    }

    private record OptionalMetadata(boolean exists, byte[] bytes) {}

    private record Journal(Phase phase, CodexSkillScope scope, String ownerRoot, String target, Path stage, Path backup,
                           Path metadataBackup, boolean targetHadOld, boolean metadataHadOld,
                           SkillInstallMetadata oldMetadata, String oldMetadataSha256,
                           List<String> createdParents, byte[] newMetadata) {
        Journal withPhase(Phase value) {
            return new Journal(value, scope, ownerRoot, target, stage, backup, metadataBackup, targetHadOld,
                    metadataHadOld, oldMetadata, oldMetadataSha256, createdParents, newMetadata);
        }

        byte[] encode() {
            JsonObject root = new JsonObject();
            root.addProperty("formatVersion", 2);
            root.addProperty("scope", scope.name());
            root.addProperty("phase", phase.name());
            root.addProperty("ownerRoot", ownerRoot);
            root.addProperty("target", target);
            root.addProperty("stage", stage.toString());
            root.addProperty("backup", backup.toString());
            root.addProperty("metadataBackup", metadataBackup.toString());
            root.addProperty("targetHadOld", targetHadOld);
            root.addProperty("metadataHadOld", metadataHadOld);
            root.addProperty("oldMetadataSha256", oldMetadataSha256);
            root.add("oldMetadata", oldMetadata == null ? null : JsonParser.parseString(new String(oldMetadata.encode(), StandardCharsets.UTF_8)));
            var parents = new com.google.gson.JsonArray();
            createdParents.forEach(parents::add);
            root.add("createdParents", parents);
            root.addProperty("newMetadataBase64", Base64.getEncoder().encodeToString(newMetadata));
            return (root + "\n").getBytes(StandardCharsets.UTF_8);
        }

        public byte[] newMetadata() { return newMetadata; }

        static Journal parse(byte[] bytes) throws IOException {
            try {
                JsonObject root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                Set<String> keys = Set.of("formatVersion", "scope", "phase", "ownerRoot", "target", "stage", "backup",
                        "metadataBackup", "targetHadOld", "metadataHadOld", "oldMetadataSha256", "oldMetadata",
                        "createdParents", "newMetadataBase64");
                if (!root.keySet().equals(keys) || root.get("formatVersion").getAsInt() != 2) return invalid();
                CodexSkillScope scope = CodexSkillScope.valueOf(root.get("scope").getAsString());
                Phase phase = Phase.valueOf(root.get("phase").getAsString());
                SkillInstallMetadata old = root.get("oldMetadata").isJsonNull() ? null
                        : SkillInstallMetadata.parse(root.get("oldMetadata").toString().getBytes(StandardCharsets.UTF_8));
                byte[] newer = Base64.getDecoder().decode(root.get("newMetadataBase64").getAsString());
                if (newer.length > 64 * 1024) return invalid();
                if (root.getAsJsonArray("createdParents").size() > 2) return invalid();
                List<String> createdParents = new ArrayList<>();
                root.getAsJsonArray("createdParents").forEach(parent -> {
                    String path = parent.getAsString();
                    if (!Path.of(path).isAbsolute()) throw new IllegalArgumentException("non-absolute parent");
                    createdParents.add(Path.of(path).normalize().toString());
                });
                String sha = root.get("oldMetadataSha256").getAsString();
                if (!sha.isEmpty() && !sha.matches("[a-f0-9]{64}")) return invalid();
                if (root.get("metadataHadOld").getAsBoolean() != (old != null)) return invalid();
                Path ownerRoot = absolutePath(root.get("ownerRoot").getAsString());
                Path target = absolutePath(root.get("target").getAsString());
                Path stage = absolutePath(root.get("stage").getAsString());
                Path backup = absolutePath(root.get("backup").getAsString());
                Path metadataBackup = absolutePath(root.get("metadataBackup").getAsString());
                return new Journal(phase, scope, ownerRoot.toString(), target.toString(), stage, backup, metadataBackup,
                        root.get("targetHadOld").getAsBoolean(), root.get("metadataHadOld").getAsBoolean(),
                        old, sha, List.copyOf(createdParents), newer);
            } catch (IOException failure) {
                throw failure;
            } catch (RuntimeException malformed) {
                throw new IOException("Skill transaction journal is malformed", malformed);
            }
        }

        private static Journal invalid() throws IOException { throw new IOException("Skill transaction journal is invalid"); }

        private static Path absolutePath(String value) throws IOException {
            Path path = Path.of(value);
            if (!path.isAbsolute() || !path.normalize().equals(path)) throw new IOException("Skill journal path is not absolute and normalized");
            return path;
        }
    }
}
