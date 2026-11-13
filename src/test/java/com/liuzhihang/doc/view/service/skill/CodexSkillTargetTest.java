package com.liuzhihang.doc.view.service.skill;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

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
        org.junit.Assert.assertFalse(java.nio.file.Files.exists(projectTarget.targetPath()));
        org.junit.Assert.assertFalse(java.nio.file.Files.exists(userTarget.targetPath()));
    }
}
