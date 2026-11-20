package com.liuzhihang.doc.view.service.skill;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

public class CodexSkillTargetTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void resolvesDistinctProjectAndUserRootsWithoutCreatingTargets() throws Exception {
        Path project = temporary.newFolder("project").toPath().toRealPath();
        Path home = temporary.newFolder("home").toPath().toRealPath();

        CodexSkillTarget projectTarget = CodexSkillTarget.forOwnerRoot(project, CodexSkillScope.PROJECT);
        CodexSkillTarget userTarget = CodexSkillTarget.forOwnerRoot(home, CodexSkillScope.USER);

        assertEquals(project.resolve(".agents/skills/doc-view"), projectTarget.targetPath());
        assertEquals(home.resolve(".agents/skills/doc-view"), userTarget.targetPath());
        assertEquals(project.resolve(".agents/skills"), projectTarget.discoveryRoot());
        assertEquals(home.resolve(".agents/skills"), userTarget.discoveryRoot());
        assertFalse(Files.exists(projectTarget.targetPath()));
        assertFalse(Files.exists(userTarget.targetPath()));
    }

    @Test
    public void rejectsMissingProjectAndNonDirectoryOwnersWithoutCreatingATarget() throws Exception {
        Path root = temporary.newFolder("owner").toPath().toRealPath();
        Path missing = root.resolve("missing");
        Path regularFile = Files.createFile(root.resolve("not-a-directory"));

        assertThrows(IOException.class, () -> CodexSkillTarget.resolve(null, CodexSkillScope.PROJECT));
        assertThrows(IOException.class, () -> CodexSkillTarget.forOwnerRoot(missing, CodexSkillScope.PROJECT));
        assertThrows(IOException.class, () -> CodexSkillTarget.forOwnerRoot(regularFile, CodexSkillScope.USER));
        assertFalse(Files.exists(missing));
        assertFalse(Files.exists(root.resolve(".agents")));
    }
}
