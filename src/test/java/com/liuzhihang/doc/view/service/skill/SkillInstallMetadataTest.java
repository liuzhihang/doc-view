package com.liuzhihang.doc.view.service.skill;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

public class SkillInstallMetadataTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void roundTripsMetadataOutsideTheSkillBody() throws Exception {
        SkillFixtureFactory.Fixture fixture = SkillFixtureFactory.fixture("1.7.0",
                Map.of("SKILL.md", "name: doc-view\n".getBytes(StandardCharsets.UTF_8)));
        SkillInstallMetadata metadata = SkillInstallMetadata.from(fixture.manifest(),
                CodexSkillInstallService.VERIFIED_CLIENT_VERSION, "/project/.agents/skills/doc-view", Instant.EPOCH);

        assertEquals(metadata, SkillInstallMetadata.parse(metadata.encode()));
        assertEquals(CodexSkillScope.PROJECT, SkillInstallMetadata.parse(metadata.encode()).scope());
    }

    @Test
    public void rejectsMalformedMetadataAndUnsupportedClientVersion() throws Exception {
        SkillFixtureFactory.Fixture fixture = SkillFixtureFactory.fixture("1.7.0",
                Map.of("SKILL.md", "name: doc-view\n".getBytes(StandardCharsets.UTF_8)));
        SkillInstallMetadata valid = SkillInstallMetadata.from(fixture.manifest(),
                CodexSkillInstallService.VERIFIED_CLIENT_VERSION, "/project/.agents/skills/doc-view", Instant.EPOCH);
        String json = new String(valid.encode(), StandardCharsets.UTF_8)
                .replace(CodexSkillInstallService.VERIFIED_CLIENT_VERSION, "codex-cli unknown");

        assertThrows(IOException.class, () -> SkillInstallMetadata.parse(json.getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> SkillInstallMetadata.parse("{}".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void scopeAndOwnerRootSeparateMetadataButUserStateIsSharedAcrossProjects() throws Exception {
        Path home = temporary.newFolder("home").toPath().toRealPath();
        Path state = temporary.newFolder("state").toPath().toRealPath();
        Path projectOne = temporary.newFolder("project-one").toPath().toRealPath();
        Path projectTwo = temporary.newFolder("project-two").toPath().toRealPath();

        SkillInstallMetadataStore userFromProjectOne = new SkillInstallMetadataStore(home, CodexSkillScope.USER, state);
        SkillInstallMetadataStore userFromProjectTwo = new SkillInstallMetadataStore(home, CodexSkillScope.USER, state);
        SkillInstallMetadataStore projectStore = new SkillInstallMetadataStore(projectOne, CodexSkillScope.PROJECT, state);

        assertEquals(userFromProjectOne.directory(), userFromProjectTwo.directory());
        assertNotEquals(userFromProjectOne.directory(), projectStore.directory());
        assertEquals(userFromProjectOne.lockFile(), userFromProjectTwo.lockFile());
        assertEquals(userFromProjectOne.lastCheckFile(), userFromProjectTwo.lastCheckFile());
        assertNotEquals(userFromProjectOne.lastCheckFile(), projectStore.lastCheckFile());
        assertTrue(userFromProjectOne.lockFile().endsWith(Path.of(".agents", "skills", ".doc-view-skill-install.lock")));
        assertTrue(userFromProjectOne.directory().startsWith(state));
        assertTrue(projectStore.directory().startsWith(state));

        Files.createDirectories(userFromProjectOne.lockFile().getParent());
        try (FileChannel firstChannel = FileChannel.open(userFromProjectOne.lockFile(),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, java.nio.file.LinkOption.NOFOLLOW_LINKS);
             FileChannel secondChannel = FileChannel.open(userFromProjectTwo.lockFile(),
                     StandardOpenOption.CREATE, StandardOpenOption.WRITE, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            FileLock firstLock = userFromProjectOne.tryLock(firstChannel);
            assertTrue(firstLock != null);
            assertNull("The other project service must share the global writer lock",
                    userFromProjectTwo.tryLock(secondChannel));
            firstLock.close();
        }
    }

    @Test
    public void projectAndUserMetadataSurviveFreshStoreInstancesAtTheirOwnTargets() throws Exception {
        Path home = temporary.newFolder("fresh-store-home").toPath().toRealPath();
        Path project = temporary.newFolder("fresh-store-project").toPath().toRealPath();
        Path state = temporary.newFolder("fresh-store-state").toPath().toRealPath();
        Path userTarget = home.resolve(".agents/skills/doc-view");
        Path projectTarget = project.resolve(".agents/skills/doc-view");
        SkillFixtureFactory.Fixture release = SkillFixtureFactory.fixture("1.7.0",
                Map.of("SKILL.md", "name: doc-view\n".getBytes(StandardCharsets.UTF_8)));
        SkillInstallMetadata userMetadata = SkillInstallMetadata.from(CodexSkillScope.USER, release.manifest(),
                CodexSkillInstallService.VERIFIED_CLIENT_VERSION, userTarget.toString(), Instant.EPOCH);
        SkillInstallMetadata projectMetadata = SkillInstallMetadata.from(CodexSkillScope.PROJECT, release.manifest(),
                CodexSkillInstallService.VERIFIED_CLIENT_VERSION, projectTarget.toString(), Instant.EPOCH);

        SkillInstallMetadataStore firstUserStore = new SkillInstallMetadataStore(home, CodexSkillScope.USER, state);
        SkillInstallMetadataStore firstProjectStore = new SkillInstallMetadataStore(project,
                CodexSkillScope.PROJECT, state);
        firstUserStore.write(userMetadata);
        firstProjectStore.write(projectMetadata);

        SkillInstallMetadataStore reopenedUserStore = new SkillInstallMetadataStore(home, CodexSkillScope.USER, state);
        SkillInstallMetadataStore reopenedProjectStore = new SkillInstallMetadataStore(project,
                CodexSkillScope.PROJECT, state);
        assertEquals(userMetadata, reopenedUserStore.read().orElseThrow());
        assertEquals(projectMetadata, reopenedProjectStore.read().orElseThrow());
        assertNotEquals(reopenedUserStore.metadataFile(), reopenedProjectStore.metadataFile());
        assertTrue(reopenedUserStore.directory().startsWith(state));
        assertTrue(reopenedProjectStore.directory().startsWith(state));
    }

    @Test
    public void successfulCheckTimePersistsSeparatelyFromInstallationMetadata() throws Exception {
        Path home = temporary.newFolder("check-time-home").toPath().toRealPath();
        Path state = temporary.newFolder("check-time-state").toPath().toRealPath();
        SkillInstallMetadataStore project = new SkillInstallMetadataStore(home, CodexSkillScope.PROJECT, state);
        SkillInstallMetadataStore user = new SkillInstallMetadataStore(home, CodexSkillScope.USER, state);
        Instant checkedAt = Instant.parse("2026-11-13T10:11:12Z");
        SkillLastCheckStore projectCheck = new SkillLastCheckStore(project.lastCheckFile());

        assertTrue(projectCheck.read().isEmpty());
        projectCheck.write(checkedAt);

        SkillLastCheckStore reopenedProjectCheck = new SkillLastCheckStore(
                new SkillInstallMetadataStore(home, CodexSkillScope.PROJECT, state).lastCheckFile());
        assertEquals(checkedAt, reopenedProjectCheck.read().orElseThrow());
        assertTrue("The time cache is separate from the install metadata record",
                Files.exists(project.lastCheckFile()) && !Files.exists(project.metadataFile()));
        assertTrue("Project and User scopes keep separate last-check values", new SkillLastCheckStore(
                user.lastCheckFile()).read().isEmpty());
    }

    @Test
    public void lastCheckStateRejectsOversizeAndSymbolicLinkWithoutFollowingThem() throws Exception {
        Path home = temporary.newFolder("check-state-safety-home").toPath().toRealPath();
        Path state = temporary.newFolder("check-state-safety-state").toPath().toRealPath();
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(home, CodexSkillScope.USER, state);
        store.ensureDirectory();
        Files.write(store.lastCheckFile(), new byte[4097]);
        SkillLastCheckStore checkStore = new SkillLastCheckStore(store.lastCheckFile());
        assertThrows(IOException.class, checkStore::read);

        Path external = temporary.newFile("external-check-state.json").toPath();
        byte[] original = "external check state".getBytes(StandardCharsets.UTF_8);
        Files.write(external, original);
        Files.delete(store.lastCheckFile());
        try {
            Files.createSymbolicLink(store.lastCheckFile(), external);
        } catch (UnsupportedOperationException | IOException | SecurityException unavailable) {
            return;
        }
        assertThrows(IOException.class, checkStore::read);
        assertThrows(IOException.class, () -> checkStore.write(Instant.EPOCH));
        assertArrayEquals(original, Files.readAllBytes(external));
    }

    @Test
    public void refusesCorruptOversizedAndSymbolicLinkMetadataWithoutOverwritingIt() throws Exception {
        Path home = temporary.newFolder("corrupt-home").toPath().toRealPath();
        Path state = temporary.newFolder("corrupt-state").toPath().toRealPath();
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(home, CodexSkillScope.USER, state);
        store.ensureDirectory();
        Files.writeString(store.metadataFile(), "{broken");
        assertThrows(IOException.class, store::read);

        Files.write(store.metadataFile(), new byte[64 * 1024 + 1]);
        assertThrows(IOException.class, store::read);

        Path external = temporary.newFile("external-metadata.json").toPath();
        byte[] externalBytes = "preserve external state".getBytes(StandardCharsets.UTF_8);
        Files.write(external, externalBytes);
        Files.delete(store.metadataFile());
        try {
            Files.createSymbolicLink(store.metadataFile(), external);
        } catch (UnsupportedOperationException | IOException | SecurityException unavailable) {
            // The filesystem may forbid symlink fixtures; corruption and size limits above still run.
            return;
        }
        assertThrows(IOException.class, store::read);
        assertThrows(IOException.class, () -> store.write(SkillInstallMetadata.from(
                SkillFixtureFactory.fixture("1.7.0", Map.of("SKILL.md", "name: doc-view\n"
                        .getBytes(StandardCharsets.UTF_8))).manifest(),
                CodexSkillInstallService.VERIFIED_CLIENT_VERSION,
                home.resolve(".agents/skills/doc-view").toString(), Instant.EPOCH)));
        assertArrayEquals(externalBytes, Files.readAllBytes(external));
    }

    @Test
    public void defaultUserMetadataLivesBesideAgentsButOutsideTheSkillTree() throws Exception {
        Path home = temporary.newFolder("isolated-home").toPath().toRealPath();
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(home, CodexSkillScope.USER);

        assertTrue(store.directory().startsWith(home.resolve(".agents/.doc-view-skill-state")));
        assertFalse(store.directory().startsWith(home.resolve(".agents/skills")));
        assertEquals(home.resolve(".agents/skills/.doc-view-skill-install.lock"), store.lockFile());
    }
}
