package com.liuzhihang.doc.view.service.skill;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

public class SkillInstallTransactionTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void installsUserScopeOnlyUnderTheFixtureHomeAndRecordsScope() throws Exception {
        Path home = temporary.newFolder("isolated-home").toPath().toRealPath();
        Path state = temporary.newFolder("isolated-state").toPath().toRealPath();
        Path target = home.resolve(".agents/skills/doc-view");
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(home, CodexSkillScope.USER, state);
        SkillFixtureFactory.Fixture release = fixture("1.7.0", "user-scope");
        Map<String, byte[]> files = SkillPackageArchive.validate(release.archive(), release.manifest());
        SkillInstallTransaction transaction = new SkillInstallTransaction(CodexSkillScope.USER, home, target, store,
                release.manifest(), files, CodexSkillInstallService.VERIFIED_CLIENT_VERSION, "1.7.0-SNAPSHOT",
                emptyIndicator(), (path, indicator) -> true);

        SkillInstallTransaction.Outcome outcome = transaction.execute();

        assertTrue(outcome.committed());
        assertEquals(CodexSkillScope.USER, store.read().orElseThrow().scope());
        assertTrue(store.read().orElseThrow().targetPath().equals(target.toString()));
        assertTrue(Files.exists(store.lockFile()));
        assertTrue(Files.exists(target.resolve("SKILL.md")));
    }

    @Test
    public void projectAndUserUpdateCancellationDuringStagingKeepsEachInstalledVersion() throws Exception {
        for (CodexSkillScope scope : CodexSkillScope.values()) {
            Path owner = temporary.newFolder("isolated-" + scope.name().toLowerCase()).toPath().toRealPath();
            Path state = temporary.newFolder("isolated-state-" + scope.name().toLowerCase()).toPath().toRealPath();
            Path target = owner.resolve(".agents/skills/doc-view");
            SkillInstallMetadataStore store = new SkillInstallMetadataStore(owner, scope, state);
            SkillFixtureFactory.Fixture first = fixture("1.7.0", scope.name() + "-first");
            SkillInstallTransaction.Outcome installed = scopedTransaction(scope, owner, target, store, first,
                    phase -> {}, (path, indicator) -> true, emptyIndicator()).execute();
            assertTrue(installed.committed());
            byte[] oldBody = Files.readAllBytes(target.resolve("SKILL.md"));
            SkillInstallMetadata oldMetadata = store.read().orElseThrow();

            SkillFixtureFactory.Fixture second = fixture("1.7.1", scope.name() + "-second");
            IsolatedProgressIndicator indicator = emptyIndicator();
            assertThrows(com.intellij.openapi.progress.ProcessCanceledException.class,
                    () -> scopedTransaction(scope, owner, target, store, second, phase -> {
                        if (phase == SkillInstallTransaction.Phase.PREPARING) indicator.cancel();
                    }, (path, progress) -> true, indicator).execute());

            assertArrayEquals(scope + " target bytes after canceled staging", oldBody,
                    Files.readAllBytes(target.resolve("SKILL.md")));
            assertEquals(scope + " metadata after canceled staging", oldMetadata, store.read().orElseThrow());
            assertFalse(scope + " journal after canceled staging", Files.exists(store.journalFile()));
            assertNoTransactionDirectories(target.getParent());
            assertNoMetadataTemporaries(store);
        }
    }

    @Test
    public void installsAndUpdatesOnlyTheVerifiedProjectTree() throws Exception {
        Path root = temporary.newFolder("project").toPath().toRealPath();
        Path state = temporary.newFolder("plugin-state").toPath().toRealPath();
        Path target = root.resolve(".agents/skills/doc-view");
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(root, state);
        SkillFixtureFactory.Fixture first = fixture("1.7.0", "first");

        SkillInstallTransaction.Outcome install = transaction(root, target, store, first, phase -> {}, (path, indicator) -> true).execute();

        assertTrue(install.committed());
        assertTrue(Files.isRegularFile(target.resolve("SKILL.md")));
        assertArrayEquals(first.files().get("SKILL.md"), Files.readAllBytes(target.resolve("SKILL.md")));
        SkillInstallMetadata installed = store.read().orElseThrow();
        assertTrue(SkillTreeVerifier.matches(target, installed));
        assertFalse(Files.exists(store.journalFile()));

        SkillFixtureFactory.Fixture second = fixture("1.7.1", "second");
        SkillInstallTransaction.Outcome update = transaction(root, target, store, second, phase -> {}, (path, indicator) -> true).execute();

        assertTrue(update.committed());
        assertArrayEquals(second.files().get("SKILL.md"), Files.readAllBytes(target.resolve("SKILL.md")));
        assertTrue(store.read().orElseThrow().skillVersion().equals("1.7.1"));
        assertTrue(SkillTreeVerifier.matches(target, store.read().orElseThrow()));
    }

    @Test
    public void preservesManagedTargetEditedAfterStagingBeforeBackupRename() throws Exception {
        Path root = temporary.newFolder("edited-after-stage-project").toPath().toRealPath();
        Path state = temporary.newFolder("edited-after-stage-state").toPath().toRealPath();
        Path target = root.resolve(".agents/skills/doc-view");
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(root, state);
        SkillFixtureFactory.Fixture first = fixture("1.7.0", "original-managed-tree");
        assertTrue(transaction(root, target, store, first, phase -> {}, (path, indicator) -> true)
                .execute().committed());
        Map<String, String> before = snapshotTree(target);
        byte[] oldMetadata = Files.readAllBytes(store.metadataFile());
        byte[] externalEdit = "external edit after staging".getBytes(StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> transaction(root, target, store,
                fixture("1.7.1", "candidate-tree"), phase -> {
                    if (phase == SkillInstallTransaction.Phase.READY) {
                        Files.write(target.resolve("SKILL.md"), externalEdit);
                    }
                }, (path, indicator) -> true).execute());

        Map<String, String> expected = new java.util.LinkedHashMap<>(before);
        expected.put("SKILL.md", SkillReleaseManifest.sha256(externalEdit));
        assertEquals("The concurrent edit remains at the visible target", expected, snapshotTree(target));
        assertArrayEquals(oldMetadata, Files.readAllBytes(store.metadataFile()));
        assertFalse("No recovery record is needed before either target rename", Files.exists(store.journalFile()));
        assertNoTransactionDirectories(target.getParent());
        assertNoMetadataTemporaries(store);
    }

    @Test
    public void preservesChangedOldTreeMovedIntoBackupBeforeCommitValidation() throws Exception {
        Path root = temporary.newFolder("edited-backup-project").toPath().toRealPath();
        Path state = temporary.newFolder("edited-backup-state").toPath().toRealPath();
        Path target = root.resolve(".agents/skills/doc-view");
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(root, state);
        SkillFixtureFactory.Fixture first = fixture("1.7.0", "original-managed-tree");
        assertTrue(transaction(root, target, store, first, phase -> {}, (path, indicator) -> true)
                .execute().committed());
        Map<String, String> before = snapshotTree(target);
        byte[] oldMetadata = Files.readAllBytes(store.metadataFile());
        byte[] externalEdit = "external edit captured in the moved directory".getBytes(StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> transaction(root, target, store,
                fixture("1.7.1", "candidate-tree"), phase -> {
                    if (phase == SkillInstallTransaction.Phase.OLD_BACKED_UP) {
                        Path backup = childWithPrefix(target.getParent(), ".doc-view-skill-backup-");
                        Files.write(backup.resolve("SKILL.md"), externalEdit);
                    }
                }, (path, indicator) -> true).execute());

        Map<String, String> expected = new java.util.LinkedHashMap<>(before);
        expected.put("SKILL.md", SkillReleaseManifest.sha256(externalEdit));
        assertEquals("The changed backup bytes are restored to the visible target", expected, snapshotTree(target));
        assertArrayEquals("The older install record remains intact", oldMetadata, Files.readAllBytes(store.metadataFile()));
        assertFalse("A safely restored external edit needs no pending transaction", Files.exists(store.journalFile()));
        assertNoTransactionDirectories(target.getParent());
        assertNoMetadataTemporaries(store);
    }

    @Test
    public void preservesTargetCreatedAfterStagingDuringFirstInstall() throws Exception {
        Path root = temporary.newFolder("concurrent-first-install-project").toPath().toRealPath();
        Path state = temporary.newFolder("concurrent-first-install-state").toPath().toRealPath();
        Path target = root.resolve(".agents/skills/doc-view");
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(root, state);
        SkillFixtureFactory.Fixture release = fixture("1.7.0", "candidate-tree");
        Map<String, byte[]> externalFiles = Map.of(
                "SKILL.md", "unmanaged concurrent target".getBytes(StandardCharsets.UTF_8),
                "notes/manual.txt", "keep this file".getBytes(StandardCharsets.UTF_8));

        assertThrows(IOException.class, () -> transaction(root, target, store, release, phase -> {
            if (phase == SkillInstallTransaction.Phase.READY) writeTree(target, externalFiles);
        }, (path, indicator) -> true).execute());

        assertEquals("The concurrent first-install tree is not replaced", snapshotTree(externalFiles), snapshotTree(target));
        assertFalse("No install record is written for a target that appeared during staging",
                Files.exists(store.metadataFile()));
        assertFalse(Files.exists(store.journalFile()));
        assertNoTransactionDirectories(target.getParent());
        assertNoMetadataTemporaries(store);
    }

    @Test
    public void recoversFirstInstallAfterStageRenameBeforePhaseJournalUpdate() throws Exception {
        Path root = temporary.newFolder("first-install-rename-gap-project").toPath().toRealPath();
        Path state = temporary.newFolder("first-install-rename-gap-state").toPath().toRealPath();
        Path target = root.resolve(".agents/skills/doc-view");
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(root, state);
        SkillFixtureFactory.Fixture release = fixture("1.7.0", "new tree before journal update");

        assertThrows(AssertionError.class, () -> transaction(root, target, store, release, phase -> {
            if (phase == SkillInstallTransaction.Phase.OLD_BACKED_UP) {
                Path stage = childWithPrefix(target.getParent(), ".doc-view-skill-stage-");
                Files.move(stage, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                throw new AssertionError("simulated process termination before NEW_IN_PLACE journal write");
            }
        }, (path, indicator) -> true).execute());

        assertTrue(Files.exists(store.journalFile()));
        assertArrayEquals(release.files().get("SKILL.md"), Files.readAllBytes(target.resolve("SKILL.md")));
        assertTrue(SkillInstallTransaction.recoverPending(root, target, store));
        assertFalse("An uncommitted first install is removed after recovering the rename gap", Files.exists(target));
        assertFalse(Files.exists(store.metadataFile()));
        assertFalse(Files.exists(store.journalFile()));
        assertNoTransactionDirectories(target.getParent());
        assertNoMetadataTemporaries(store);
    }

    @Test
    public void keepsChangedBackupAndNewTargetPendingIfBackupChangesAfterNewTreeIsInPlace() throws Exception {
        Path root = temporary.newFolder("changed-backup-new-tree-project").toPath().toRealPath();
        Path state = temporary.newFolder("changed-backup-new-tree-state").toPath().toRealPath();
        Path target = root.resolve(".agents/skills/doc-view");
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(root, state);
        SkillFixtureFactory.Fixture first = fixture("1.7.0", "original-managed-tree");
        assertTrue(transaction(root, target, store, first, phase -> {}, (path, indicator) -> true)
                .execute().committed());
        SkillFixtureFactory.Fixture update = fixture("1.7.1", "new managed tree");
        byte[] externalEdit = "external bytes in old backup".getBytes(StandardCharsets.UTF_8);
        Path[] backupPath = new Path[1];

        assertThrows(SkillInstallTransaction.RecoveryPendingException.class,
                () -> transaction(root, target, store, update, phase -> {
                    if (phase == SkillInstallTransaction.Phase.NEW_IN_PLACE) {
                        backupPath[0] = childWithPrefix(target.getParent(), ".doc-view-skill-backup-");
                        Files.write(backupPath[0].resolve("SKILL.md"), externalEdit);
                    }
                }, (path, indicator) -> true).execute());

        assertArrayEquals(update.files().get("SKILL.md"), Files.readAllBytes(target.resolve("SKILL.md")));
        assertTrue("New target and install record still describe the same release",
                SkillTreeVerifier.matches(target, store.read().orElseThrow()));
        assertArrayEquals(externalEdit, Files.readAllBytes(backupPath[0].resolve("SKILL.md")));
        assertTrue("Unsafe cleanup leaves a journal for explicit recovery", Files.exists(store.journalFile()));
        assertFalse("Recovery must not delete either side when the backup no longer matches",
                SkillInstallTransaction.recoverPending(root, target, store));
        assertArrayEquals(externalEdit, Files.readAllBytes(backupPath[0].resolve("SKILL.md")));
        assertTrue(Files.exists(store.journalFile()));
    }

    @Test
    public void preservesUnmanagedAndModifiedTargets() throws Exception {
        for (CodexSkillScope scope : CodexSkillScope.values()) {
            Path owner = temporary.newFolder("protected-owner-" + scope).toPath().toRealPath();
            Path state = temporary.newFolder("protected-state-" + scope).toPath().toRealPath();
            Path target = owner.resolve(".agents/skills/doc-view");
            Files.createDirectories(target);
            byte[] unmanaged = "manual file".getBytes(StandardCharsets.UTF_8);
            Files.write(target.resolve("SKILL.md"), unmanaged);
            SkillInstallMetadataStore store = new SkillInstallMetadataStore(owner, scope, state);
            SkillFixtureFactory.Fixture release = fixture("1.7.0", "candidate-" + scope);

            assertThrows(scope + " unmanaged target must be preserved", IOException.class,
                    () -> scopedTransaction(scope, owner, target, store, release, phase -> {},
                            (path, indicator) -> true, emptyIndicator()).execute());
            assertArrayEquals(scope + " unmanaged file bytes", unmanaged,
                    Files.readAllBytes(target.resolve("SKILL.md")));

            Files.delete(target.resolve("SKILL.md"));
            Files.delete(target);
            SkillFixtureFactory.Fixture first = fixture("1.7.0", "managed-" + scope);
            assertTrue(scopedTransaction(scope, owner, target, store, first, phase -> {},
                    (path, indicator) -> true, emptyIndicator()).execute().committed());
            SkillInstallMetadata oldMetadata = store.read().orElseThrow();
            byte[] modified = "user edit".getBytes(StandardCharsets.UTF_8);
            Files.write(target.resolve("SKILL.md"), modified);
            SkillFixtureFactory.Fixture update = fixture("1.7.1", "updated-" + scope);

            assertThrows(scope + " modified target must be preserved", IOException.class,
                    () -> scopedTransaction(scope, owner, target, store, update, phase -> {},
                            (path, indicator) -> true, emptyIndicator()).execute());
            assertArrayEquals(scope + " modified file bytes", modified,
                    Files.readAllBytes(target.resolve("SKILL.md")));
            assertEquals(oldMetadata, store.read().orElseThrow());
            assertFalse(Files.exists(store.journalFile()));
        }
    }

    @Test
    public void rollsBackEachPreCommitFailureAndCodexDiscoveryFailure() throws Exception {
        for (SkillInstallTransaction.Phase failurePhase : new SkillInstallTransaction.Phase[]{
                SkillInstallTransaction.Phase.PREPARING,
                SkillInstallTransaction.Phase.READY,
                SkillInstallTransaction.Phase.OLD_BACKED_UP,
                SkillInstallTransaction.Phase.NEW_IN_PLACE}) {
            Path root = temporary.newFolder("failure-" + failurePhase).toPath().toRealPath();
            Path state = temporary.newFolder("state-" + failurePhase).toPath().toRealPath();
            Path target = root.resolve(".agents/skills/doc-view");
            SkillInstallMetadataStore store = new SkillInstallMetadataStore(root, state);
            SkillFixtureFactory.Fixture release = fixture("1.7.0", "candidate-" + failurePhase);
            assertThrows(IOException.class, () -> transaction(root, target, store, release,
                    phase -> { if (phase == failurePhase) throw new IOException("injected failure"); },
                    (path, indicator) -> true).execute());
            assertFalse(Files.exists(target));
            assertFalse(Files.exists(store.metadataFile()));
            assertFalse(Files.exists(store.journalFile()));
            assertTrue("The shared target-parent lock remains for cross-profile serialization",
                    Files.exists(root.resolve(".agents/skills/.doc-view-skill-install.lock")));
            assertNoMetadataTemporaries(store);
        }
    }

    @Test
    public void updateFailureDuringTwoRenameGapRestoresOldTreeAndMetadata() throws Exception {
        Path root = temporary.newFolder("project").toPath().toRealPath();
        Path state = temporary.newFolder("plugin-state").toPath().toRealPath();
        Path target = root.resolve(".agents/skills/doc-view");
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(root, state);
        SkillFixtureFactory.Fixture first = fixture("1.7.0", "stable");
        transaction(root, target, store, first, phase -> {}, (path, indicator) -> true).execute();
        byte[] oldBytes = Files.readAllBytes(target.resolve("SKILL.md"));
        SkillInstallMetadata oldMetadata = store.read().orElseThrow();
        SkillFixtureFactory.Fixture second = fixture("1.7.1", "candidate");

        assertThrows(IOException.class, () -> transaction(root, target, store, second, phase -> {
            if (phase == SkillInstallTransaction.Phase.OLD_BACKED_UP) {
                assertFalse(Files.exists(target)); // two renames leave a short documented gap
                throw new IOException("failure in rename gap");
            }
        }, (path, indicator) -> true).execute());

        assertArrayEquals(oldBytes, Files.readAllBytes(target.resolve("SKILL.md")));
        assertTrue(oldMetadata.equals(store.read().orElseThrow()));
        assertFalse(Files.exists(store.journalFile()));
    }

    @Test
    public void everyManagedUpdateRenamePhaseFailureRestoresOldTreeAndMetadata() throws Exception {
        for (CodexSkillScope scope : CodexSkillScope.values()) {
            for (SkillInstallTransaction.Phase failurePhase : new SkillInstallTransaction.Phase[]{
                    SkillInstallTransaction.Phase.PREPARING,
                    SkillInstallTransaction.Phase.READY,
                    SkillInstallTransaction.Phase.OLD_BACKED_UP,
                    SkillInstallTransaction.Phase.NEW_IN_PLACE}) {
                Path owner = temporary.newFolder("update-failure-" + scope + "-" + failurePhase).toPath().toRealPath();
                Path state = temporary.newFolder("update-state-" + scope + "-" + failurePhase).toPath().toRealPath();
                Path target = owner.resolve(".agents/skills/doc-view");
                SkillInstallMetadataStore store = new SkillInstallMetadataStore(owner, scope, state);
                SkillFixtureFactory.Fixture oldRelease = fixture("1.7.0", "old-" + scope + "-" + failurePhase);
                assertTrue(scopedTransaction(scope, owner, target, store, oldRelease, phase -> {},
                        (path, indicator) -> true, emptyIndicator()).execute().committed());
                byte[] oldBody = Files.readAllBytes(target.resolve("SKILL.md"));
                SkillInstallMetadata oldMetadata = store.read().orElseThrow();
                SkillFixtureFactory.Fixture newRelease = fixture("1.7.1", "new-" + scope + "-" + failurePhase);

                assertThrows(IOException.class, () -> scopedTransaction(scope, owner, target, store, newRelease,
                        phase -> { if (phase == failurePhase) throw new IOException("injected update failure"); },
                        (path, indicator) -> true, emptyIndicator()).execute());

                assertArrayEquals("old target at " + scope + "/" + failurePhase, oldBody,
                        Files.readAllBytes(target.resolve("SKILL.md")));
                assertEquals("old metadata at " + scope + "/" + failurePhase, oldMetadata, store.read().orElseThrow());
                assertFalse("journal at " + scope + "/" + failurePhase, Files.exists(store.journalFile()));
                assertNoTransactionDirectories(target.getParent());
                assertNoMetadataTemporaries(store);
            }
        }
    }

    @Test
    public void discoveryFailureRollsBackManagedUpdate() throws Exception {
        Path root = temporary.newFolder("project").toPath().toRealPath();
        Path state = temporary.newFolder("plugin-state").toPath().toRealPath();
        Path target = root.resolve(".agents/skills/doc-view");
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(root, state);
        SkillFixtureFactory.Fixture first = fixture("1.7.0", "old");
        transaction(root, target, store, first, phase -> {}, (path, indicator) -> true).execute();
        byte[] oldBytes = Files.readAllBytes(target.resolve("SKILL.md"));
        SkillInstallMetadata oldMetadata = store.read().orElseThrow();
        SkillFixtureFactory.Fixture second = fixture("1.7.1", "new");

        assertThrows(IOException.class, () -> transaction(root, target, store, second, phase -> {}, (path, indicator) -> false).execute());

        assertArrayEquals(oldBytes, Files.readAllBytes(target.resolve("SKILL.md")));
        assertTrue(oldMetadata.equals(store.read().orElseThrow()));
        assertFalse(Files.exists(store.journalFile()));
    }

    @Test
    public void recoversAfterSimulatedProcessTerminationDuringUpdate() throws Exception {
        for (CodexSkillScope scope : CodexSkillScope.values()) {
            Path owner = temporary.newFolder("crashed-update-" + scope).toPath().toRealPath();
            Path state = temporary.newFolder("crashed-state-" + scope).toPath().toRealPath();
            Path target = owner.resolve(".agents/skills/doc-view");
            SkillInstallMetadataStore store = new SkillInstallMetadataStore(owner, scope, state);
            SkillFixtureFactory.Fixture first = fixture("1.7.0", "old-" + scope);
            assertTrue(scopedTransaction(scope, owner, target, store, first, phase -> {},
                    (path, indicator) -> true, emptyIndicator()).execute().committed());
            byte[] oldBytes = Files.readAllBytes(target.resolve("SKILL.md"));
            SkillInstallMetadata oldMetadata = store.read().orElseThrow();
            SkillFixtureFactory.Fixture second = fixture("1.7.1", "new-" + scope);

            assertThrows(AssertionError.class, () -> scopedTransaction(scope, owner, target, store, second, phase -> {
                if (phase == SkillInstallTransaction.Phase.NEW_IN_PLACE) throw new AssertionError("simulated termination");
            }, (path, indicator) -> true, emptyIndicator()).execute());
            assertTrue(Files.exists(store.journalFile()));
            assertEquals(oldMetadata, store.read().orElseThrow());

            SkillInstallMetadataStore freshStore = new SkillInstallMetadataStore(owner, scope, state);
            assertTrue(SkillInstallTransaction.recoverPending(owner, target, freshStore));

            assertArrayEquals("recovered " + scope + " files", oldBytes,
                    Files.readAllBytes(target.resolve("SKILL.md")));
            assertEquals("recovered " + scope + " metadata", oldMetadata, freshStore.read().orElseThrow());
            assertFalse(Files.exists(freshStore.journalFile()));
            assertNoTransactionDirectories(target.getParent());
            assertNoMetadataTemporaries(freshStore);
        }
    }

    @Test
    public void metadataCommitFailureLeavesJournalAndFreshStoreRecoversOldProjectVersion() throws Exception {
        for (CodexSkillScope scope : CodexSkillScope.values()) {
            Path owner = temporary.newFolder("metadata-commit-" + scope).toPath().toRealPath();
            Path state = temporary.newFolder("metadata-commit-state-" + scope).toPath().toRealPath();
            Path target = owner.resolve(".agents/skills/doc-view");
            SkillInstallMetadataStore originalStore = new SkillInstallMetadataStore(owner, scope, state);
            SkillFixtureFactory.Fixture oldRelease = fixture("1.7.0", "old-complete-tree-" + scope);
            assertTrue(scopedTransaction(scope, owner, target, originalStore, oldRelease, phase -> {},
                    (path, indicator) -> true, emptyIndicator()).execute().committed());
            byte[] oldBody = Files.readAllBytes(target.resolve("SKILL.md"));
            SkillInstallMetadata oldMetadata = originalStore.read().orElseThrow();
            SkillFixtureFactory.Fixture newRelease = fixture("1.7.1", "new-complete-tree-" + scope);

            assertThrows(scope + " metadata failure should preserve recovery state",
                    SkillInstallTransaction.RecoveryPendingException.class,
                    () -> scopedTransaction(scope, owner, target, originalStore, newRelease, phase -> {
                        if (phase == SkillInstallTransaction.Phase.NEW_IN_PLACE) {
                            // Force the metadata atomic move to fail after the new tree is in place.
                            Files.delete(originalStore.metadataFile());
                            Files.createDirectory(originalStore.metadataFile());
                            Files.writeString(originalStore.metadataFile().resolve("blocker"), "injected failure");
                        }
                    }, (path, indicator) -> true, emptyIndicator()).execute());

            assertArrayEquals(scope + " old files are restored while metadata repair is pending", oldBody,
                    Files.readAllBytes(target.resolve("SKILL.md")));
            assertTrue(Files.exists(originalStore.journalFile()));

            Files.delete(originalStore.metadataFile().resolve("blocker"));
            Files.delete(originalStore.metadataFile());
            SkillInstallMetadataStore freshStore = new SkillInstallMetadataStore(owner, scope, state);
            assertTrue(SkillInstallTransaction.recoverPending(owner, target, freshStore));
            assertEquals(oldMetadata, freshStore.read().orElseThrow());
            assertFalse(Files.exists(freshStore.journalFile()));
            assertNoTransactionDirectories(target.getParent());
            assertNoMetadataTemporaries(freshStore);
        }
    }

    @Test
    public void anotherWriterLockRejectsTransactionBeforeWritingSkillFiles() throws Exception {
        for (CodexSkillScope scope : CodexSkillScope.values()) {
            Path owner = temporary.newFolder("locked-owner-" + scope).toPath().toRealPath();
            Path state = temporary.newFolder("locked-state-" + scope).toPath().toRealPath();
            Path target = owner.resolve(".agents/skills/doc-view");
            SkillInstallMetadataStore store = new SkillInstallMetadataStore(owner, scope, state);
            SkillFixtureFactory.Fixture release = fixture("1.7.0", "locked-" + scope);
            Files.createDirectories(store.lockFile().getParent());
            try (var channel = java.nio.channels.FileChannel.open(store.lockFile(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE,
                    java.nio.file.LinkOption.NOFOLLOW_LINKS);
                 var held = channel.lock()) {
                assertThrows(scope + " competing transaction must fail before staging",
                        IOException.class, () -> scopedTransaction(scope, owner, target,
                                store, release, phase -> {}, (path, indicator) -> true,
                                emptyIndicator()).execute());
            }
            assertFalse(Files.exists(target));
            assertFalse(Files.exists(store.metadataFile()));
            assertFalse(Files.exists(store.journalFile()));
            assertNoMetadataTemporaries(store);
        }
    }

    private SkillInstallTransaction transaction(Path root, Path target, SkillInstallMetadataStore store,
                                                SkillFixtureFactory.Fixture fixture,
                                                SkillInstallTransaction.PhaseHook hook,
                                                SkillInstallTransaction.DiscoveryVerifier discovery) throws Exception {
        Map<String, byte[]> files = SkillPackageArchive.validate(fixture.archive(), fixture.manifest());
        return new SkillInstallTransaction(root, target, store, fixture.manifest(), files,
                CodexSkillInstallService.VERIFIED_CLIENT_VERSION, "1.7.0-SNAPSHOT",
                emptyIndicator(), discovery, hook);
    }

    private SkillInstallTransaction scopedTransaction(CodexSkillScope scope, Path owner, Path target,
                                                      SkillInstallMetadataStore store,
                                                      SkillFixtureFactory.Fixture fixture,
                                                      SkillInstallTransaction.PhaseHook hook,
                                                      SkillInstallTransaction.DiscoveryVerifier discovery,
                                                      ProgressIndicator indicator) throws Exception {
        Map<String, byte[]> files = SkillPackageArchive.validate(fixture.archive(), fixture.manifest());
        return new SkillInstallTransaction(scope, owner, target, store, fixture.manifest(), files,
                CodexSkillInstallService.VERIFIED_CLIENT_VERSION, "1.7.0-SNAPSHOT",
                indicator, discovery, hook);
    }

    private static void assertNoTransactionDirectories(Path targetParent) throws IOException {
        try (var children = Files.list(targetParent)) {
            assertFalse(children.anyMatch(path -> path.getFileName().toString().startsWith(".doc-view-skill-stage-")));
        }
        try (var children = Files.list(targetParent)) {
            assertFalse(children.anyMatch(path -> path.getFileName().toString().startsWith(".doc-view-skill-backup-")));
        }
    }

    private static void assertNoMetadataTemporaries(SkillInstallMetadataStore store) throws IOException {
        try (var children = Files.list(store.directory())) {
            assertFalse(children.anyMatch(path -> path.getFileName().toString().startsWith(".metadata-backup-")));
        }
        try (var children = Files.list(store.directory())) {
            assertFalse(children.anyMatch(path -> path.getFileName().toString().contains(".tmp-")));
        }
    }

    private SkillFixtureFactory.Fixture fixture(String version, String body) throws Exception {
        return SkillFixtureFactory.fixture(version, Map.of("SKILL.md",
                ("name: doc-view\n" + body + "\n").getBytes(StandardCharsets.UTF_8)));
    }

    private static IsolatedProgressIndicator emptyIndicator() {
        return new IsolatedProgressIndicator();
    }

    private static final class IsolatedProgressIndicator implements ProgressIndicator {
        private boolean running;
        private boolean canceled;
        private boolean indeterminate = true;
        private double fraction;
        private String text = "";
        private String text2 = "";

        @Override public void start() { running = true; }
        @Override public void stop() { running = false; }
        @Override public boolean isRunning() { return running; }
        @Override public void cancel() { canceled = true; }
        @Override public boolean isCanceled() { return canceled; }
        @Override public void setText(String value) { text = value; }
        @Override public String getText() { return text; }
        @Override public void setText2(String value) { text2 = value; }
        @Override public String getText2() { return text2; }
        @Override public double getFraction() { return fraction; }
        @Override public void setFraction(double value) { fraction = value; }
        @Override public void pushState() {}
        @Override public void popState() {}
        @Override public boolean isModal() { return false; }
        @Override public ModalityState getModalityState() { return ModalityState.NON_MODAL; }
        @Override public void setModalityProgress(ProgressIndicator progress) {}
        @Override public boolean isIndeterminate() { return indeterminate; }
        @Override public void setIndeterminate(boolean value) { indeterminate = value; }
        @Override public boolean isPopupWasShown() { return false; }
        @Override public boolean isShowing() { return false; }

        @Override
        public void checkCanceled() {
            if (canceled) throw new ProcessCanceledException();
        }
    }

    private static Map<String, String> snapshotTree(Path root) throws IOException {
        Map<String, String> snapshot = new java.util.LinkedHashMap<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(candidate -> Files.isRegularFile(candidate,
                    java.nio.file.LinkOption.NOFOLLOW_LINKS)).sorted().toList()) {
                snapshot.put(root.relativize(path).toString(),
                        SkillReleaseManifest.sha256(Files.readAllBytes(path)));
            }
        }
        return snapshot;
    }

    private static Map<String, String> snapshotTree(Map<String, byte[]> files) throws IOException {
        Map<String, String> snapshot = new java.util.LinkedHashMap<>();
        for (var entry : files.entrySet()) {
            snapshot.put(entry.getKey(), SkillReleaseManifest.sha256(entry.getValue()));
        }
        return snapshot;
    }

    private static void writeTree(Path root, Map<String, byte[]> files) throws IOException {
        for (var entry : files.entrySet()) {
            Path destination = root.resolve(entry.getKey()).normalize();
            if (!destination.startsWith(root)) throw new IOException("Fixture path escaped its test directory");
            Files.createDirectories(destination.getParent());
            Files.write(destination, entry.getValue());
        }
    }

    private static Path childWithPrefix(Path parent, String prefix) throws IOException {
        try (var children = Files.list(parent)) {
            return children.filter(path -> path.getFileName().toString().startsWith(prefix))
                    .findFirst().orElseThrow(() -> new IOException("Expected transaction path was not created: " + prefix));
        }
    }
}
