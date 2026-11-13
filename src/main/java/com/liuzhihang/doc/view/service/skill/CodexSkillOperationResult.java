package com.liuzhihang.doc.view.service.skill;

import org.jetbrains.annotations.NotNull;

/** Result of an explicit Skill check, install, or update request. */
public record CodexSkillOperationResult(
        @NotNull CodexSkillStatus status,
        boolean changedFiles,
        boolean committed) {

    public CodexSkillOperationResult(@NotNull CodexSkillStatus status, boolean changedFiles) {
        this(status, changedFiles, false);
    }
}
