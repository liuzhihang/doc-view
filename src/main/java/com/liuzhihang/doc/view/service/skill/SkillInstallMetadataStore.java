package com.liuzhihang.doc.view.service.skill;

import com.intellij.openapi.application.PathManager;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/** Stores Project state in IDE config and shared User state under Doc View's home metadata directory. */
final class SkillInstallMetadataStore {
    private final CodexSkillScope scope;
    private final Path ownerRoot;
    private final Path directory;
    private final Path metadataFile;
    private final Path lastCheckFile;
    private final Path journalFile;
    private final Path lockFile;

    SkillInstallMetadataStore(Path projectRoot) throws IOException {
        this(projectRoot, CodexSkillScope.PROJECT);
    }

    SkillInstallMetadataStore(Path projectRoot, Path storageRoot) throws IOException {
        this(projectRoot, CodexSkillScope.PROJECT, storageRoot);
    }

    SkillInstallMetadataStore(Path ownerRoot, CodexSkillScope scope) throws IOException {
        this(ownerRoot, scope, defaultStorageRoot(ownerRoot, scope));
    }

    SkillInstallMetadataStore(Path ownerRoot, CodexSkillScope scope, Path storageRoot) throws IOException {
        this.scope = java.util.Objects.requireNonNull(scope, "scope");
        this.ownerRoot = ownerRoot.toRealPath();
        String key = sha256(scope.name() + "\n" + this.ownerRoot);
        this.directory = storageRoot.resolve(key).toAbsolutePath().normalize();
        this.metadataFile = directory.resolve("metadata.json");
        this.lastCheckFile = directory.resolve("last-check.json");
        this.journalFile = directory.resolve("transaction.json");
        this.lockFile = this.ownerRoot.resolve(".agents/skills/.doc-view-skill-install.lock").normalize();
    }

    private static Path defaultStorageRoot(Path ownerRoot, CodexSkillScope scope) {
        if (scope == CodexSkillScope.USER) {
            return ownerRoot.resolve(".agents/.doc-view-skill-state");
        }
        return Path.of(PathManager.getConfigPath(), "doc-view", "skill-install");
    }

    Path directory() { return directory; }
    CodexSkillScope scope() { return scope; }
    Path ownerRoot() { return ownerRoot; }
    Path metadataFile() { return metadataFile; }
    Path lastCheckFile() { return lastCheckFile; }
    Path journalFile() { return journalFile; }
    Path lockFile() { return lockFile; }

    Optional<SkillInstallMetadata> read() throws IOException {
        rejectPathLinks(directory);
        if (!Files.exists(metadataFile, LinkOption.NOFOLLOW_LINKS)) return Optional.empty();
        rejectSymlink(metadataFile);
        long size = Files.size(metadataFile);
        if (size > 64 * 1024) throw new IOException("Skill installation metadata exceeds 64 KiB");
        return Optional.of(SkillInstallMetadata.parse(Files.readAllBytes(metadataFile)));
    }

    void write(SkillInstallMetadata metadata) throws IOException {
        ensureDirectory();
        rejectSymlink(metadataFile);
        atomicWrite(metadataFile, metadata.encode());
    }

    FileLock tryLock(FileChannel channel) throws IOException {
        try {
            return channel.tryLock();
        } catch (OverlappingFileLockException heldInThisProcess) {
            return null;
        }
    }

    void ensureDirectory() throws IOException {
        Path parent = directory.getParent();
        if (parent == null) throw new IOException("Skill metadata path is invalid");
        rejectPathLinks(parent);
        Files.createDirectories(parent);
        rejectPathLinks(parent);
        if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(directory)) {
            throw new IOException("Skill metadata directory is a symbolic link");
        }
        Files.createDirectories(directory);
        rejectPathLinks(directory);
    }

    static void atomicWrite(Path destination, byte[] bytes) throws IOException {
        Path parent = destination.getParent();
        if (parent == null) throw new IOException("Cannot write a file without a parent directory");
        rejectPathLinks(parent);
        Files.createDirectories(parent);
        rejectPathLinks(parent);
        rejectSymlink(destination);
        Path temporary = parent.resolve("." + destination.getFileName() + ".tmp-" + java.util.UUID.randomUUID());
        try {
            Files.write(temporary, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                throw new IOException("Atomic metadata replacement is not supported on this filesystem", unsupported);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static void rejectSymlink(Path path) throws IOException {
        if (Files.isSymbolicLink(path)) throw new IOException("Unexpected symbolic link in Skill install state");
    }

    static void rejectPathLinks(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        Path current = absolute.getRoot();
        for (Path part : absolute) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current)) throw new IOException("Unexpected symbolic link in Skill install state");
        }
    }

    private static String sha256(String value) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-256 is unavailable", impossible);
        }
    }
}
