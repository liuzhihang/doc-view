package com.liuzhihang.doc.view.service.skill;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;

/** Immutable status shown by the Doc View MCP / AI settings page. */
public record CodexSkillStatus(
        @NotNull State state,
        @NotNull String message,
        @NotNull String clientVersion,
        @NotNull Path targetPath,
        @NotNull String installedVersion,
        @NotNull String availableVersion,
        boolean canInstall,
        boolean canUpdate,
        @NotNull CodexSkillScope scope,
        @Nullable Instant lastSuccessfulCheckAt) {

    public CodexSkillStatus(@NotNull State state, @NotNull String message, @NotNull String clientVersion,
                            @NotNull Path targetPath, @NotNull String installedVersion,
                            @NotNull String availableVersion, boolean canInstall, boolean canUpdate,
                            @NotNull CodexSkillScope scope) {
        this(state, message, clientVersion, targetPath, installedVersion, availableVersion,
                canInstall, canUpdate, scope, null);
    }

    public CodexSkillStatus(@NotNull State state, @NotNull String message, @NotNull String clientVersion,
                            @NotNull Path targetPath, @NotNull String installedVersion,
                            @NotNull String availableVersion, boolean canInstall, boolean canUpdate) {
        this(state, message, clientVersion, targetPath, installedVersion, availableVersion,
                canInstall, canUpdate, CodexSkillScope.PROJECT, null);
    }

    CodexSkillStatus withLastSuccessfulCheckAt(@Nullable Instant checkedAt) {
        return new CodexSkillStatus(state, message, clientVersion, targetPath, installedVersion,
                availableVersion, canInstall, canUpdate, scope, checkedAt);
    }

    public enum State {
        UNKNOWN,
        PLATFORM_UNSUPPORTED,
        CLIENT_UNSUPPORTED,
        UNSUPPORTED_SCOPE,
        NOT_INSTALLED,
        CURRENT,
        UPDATE_AVAILABLE,
        UNMANAGED_CONFLICT,
        MODIFIED_CONFLICT,
        INCOMPATIBLE,
        NO_COMPATIBLE_RELEASE,
        OFFLINE,
        RECOVERY_PENDING,
        ERROR
    }

    static CodexSkillStatus unknown(Path targetPath) {
        return unknown(CodexSkillScope.PROJECT, targetPath);
    }

    static CodexSkillStatus unknown(CodexSkillScope scope, Path targetPath) {
        return new CodexSkillStatus(State.UNKNOWN, "Check the Codex executable to read Skill status.",
                "", targetPath, "", "", false, false, scope, null);
    }
}
