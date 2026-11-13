package com.liuzhihang.doc.view.service.skill;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class CodexClientProbeTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void requiresAnAbsoluteExecutableRegularFile() throws Exception {
        Path file = temporary.newFile("codex").toPath();
        assertTrue(file.toFile().setExecutable(true));

        assertEquals(file.toRealPath(), CodexClientProbe.checkedExecutable(file));
        assertThrows(IOException.class, () -> CodexClientProbe.checkedExecutable(Path.of("codex")));
        assertThrows(IOException.class, () -> CodexClientProbe.checkedExecutable(temporary.getRoot().toPath()));
    }

    @Test
    public void discoveryMatchRequiresExactPathNameAndScope() throws Exception {
        Path home = temporary.newFolder("home").toPath().toRealPath();
        String skillPath = home.resolve(".agents/skills/doc-view/SKILL.md").toString();
        JsonObject userSkill = skill("doc-view", "user", skillPath);
        JsonObject projectSkill = skill("doc-view", "repo", skillPath);
        JsonObject otherSkill = skill("doc-view", "user", home.resolve(".agents/skills/other/SKILL.md").toString());

        assertTrue(CodexClientProbe.matchesExpectedSkill(userSkill, skillPath, "user"));
        assertFalse(CodexClientProbe.matchesExpectedSkill(projectSkill, skillPath, "user"));
        assertFalse(CodexClientProbe.matchesExpectedSkill(otherSkill, skillPath, "user"));
    }

    @Test
    public void hostGateMatchesOnlyTheVerifiedMacAppleSiliconPlatform() {
        assertTrue(CodexClientProbe.isVerifiedHost("Mac OS X", "aarch64"));
        assertTrue(CodexClientProbe.isVerifiedHost("Darwin", "arm64"));
        assertFalse(CodexClientProbe.isVerifiedHost("Windows 11", "aarch64"));
        assertFalse(CodexClientProbe.isVerifiedHost("Mac OS X", "x86_64"));
    }

    private static JsonObject skill(String name, String scope, String path) {
        JsonObject skill = new JsonObject();
        skill.addProperty("name", name);
        skill.addProperty("scope", scope);
        skill.addProperty("path", path);
        return skill;
    }
}
