package com.liuzhihang.doc.view.service.skill;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class SkillReleaseManifestTest {
    @Test
    public void bindsSourceCommitContractAndSnapshotPluginRange() throws Exception {
        SkillFixtureFactory.Fixture fixture = fixture("1.7.0");

        fixture.manifest().validateCompatibility("1.7.0-SNAPSHOT", fixture.contract());

        assertEquals("v1.7.0", fixture.manifest().releaseTag());
        assertEquals(1, fixture.manifest().toolContractVersion());
    }

    @Test
    public void rejectsWrongContractFingerprintAndOlderPlugin() throws Exception {
        SkillFixtureFactory.Fixture fixture = fixture("1.7.0");

        assertThrows(IOException.class,
                () -> fixture.manifest().validateCompatibility("1.7.0", "other".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class,
                () -> fixture.manifest().validateCompatibility("1.6.2", fixture.contract()));
    }

    @Test
    public void rejectsTraversalAndCaseCollidingManifestPaths() throws Exception {
        assertThrows(IOException.class, () -> fixtureWithPaths(Map.of("../outside", new byte[]{1})));
        Map<String, byte[]> collision = new LinkedHashMap<>();
        collision.put("SKILL.md", new byte[]{1});
        collision.put("skill.md", new byte[]{2});
        assertThrows(IOException.class, () -> fixtureWithPaths(collision));
    }

    @Test
    public void rejectsMissingRequiredSkillAndMalformedArchiveMetadata() throws Exception {
        assertThrows(IOException.class, () -> fixtureWithPaths(Map.of("guide.md", new byte[]{1})));
    }

    private static SkillFixtureFactory.Fixture fixture(String version) throws Exception {
        return fixtureWithPaths(Map.of("SKILL.md", "name: doc-view\n".getBytes(StandardCharsets.UTF_8)));
    }

    private static SkillFixtureFactory.Fixture fixtureWithPaths(Map<String, byte[]> files) throws Exception {
        return SkillFixtureFactory.fixture("1.7.0", files);
    }
}
