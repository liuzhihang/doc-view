package com.liuzhihang.doc.view.service.skill;

import com.intellij.openapi.project.Project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Canonical owner root and fixed destination for one supported discovery scope. */
record CodexSkillTarget(CodexSkillScope scope, Path ownerRoot, Path targetPath) {
    static CodexSkillTarget resolve(Project project, CodexSkillScope scope) throws IOException {
        if (scope == null) throw new IOException("Choose Project or User Skill scope");
        String rawRoot = switch (scope) {
            case PROJECT -> project == null ? null : project.getBasePath();
            case USER -> System.getProperty("user.home");
        };
        if (rawRoot == null || rawRoot.isBlank()) {
            throw new IOException(scope == CodexSkillScope.PROJECT
                    ? "Open a local project before using Project Skill scope."
                    : "The current user home directory could not be determined.");
        }
        return forOwnerRoot(Path.of(rawRoot), scope);
    }

    static CodexSkillTarget forOwnerRoot(Path path, CodexSkillScope scope) throws IOException {
        if (scope == null) throw new IOException("Choose Project or User Skill scope");
        if (!path.isAbsolute() || !Files.isDirectory(path)) {
            throw new IOException("The selected Skill scope does not have a local directory.");
        }
        Path root = path.toRealPath();
        return new CodexSkillTarget(scope, root, root.resolve(".agents/skills/doc-view").normalize());
    }

    static Path displayPath(Project project, CodexSkillScope scope) {
        String rawRoot = switch (scope) {
            case PROJECT -> project == null ? null : project.getBasePath();
            case USER -> System.getProperty("user.home");
        };
        Path root = rawRoot == null || rawRoot.isBlank()
                ? Path.of(".").toAbsolutePath().normalize() : Path.of(rawRoot).toAbsolutePath().normalize();
        return root.resolve(".agents/skills/doc-view").normalize();
    }

    Path discoveryRoot() {
        return ownerRoot.resolve(".agents/skills");
    }
}
