package com.liuzhihang.doc.view.service.skill;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.application.ModalityState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/** Test-only child JVM. Exits without finally/rollback to exercise the durable journal. */
public final class SkillCrashProcess {
    public static void main(String[] args) throws Exception {
        CodexSkillScope scope = CodexSkillScope.valueOf(args[0]);
        Path owner = Path.of(args[1]).toRealPath();
        Path target = owner.resolve(".agents/skills/doc-view");
        SkillInstallMetadataStore store = new SkillInstallMetadataStore(owner, scope, Path.of(args[2]));
        SkillReleaseManifest manifest = SkillReleaseManifest.parse(Files.readAllBytes(Path.of(args[3])));
        var files = SkillPackageArchive.validate(Files.readAllBytes(Path.of(args[4])), manifest);
        var installed = new SkillInstallTransaction(scope, owner, target, store, manifest, files,
                CodexClientProbe.SUPPORTED_VERSION, "1.7.0-SNAPSHOT", new EmptyProgressIndicator(ModalityState.nonModal()),
                (file, progress) -> true).execute();
        if (!installed.committed()) throw new IllegalStateException("Baseline install failed");
        var changed = new LinkedHashMap<>(files);
        changed.put("SKILL.md", (new String(files.get("SKILL.md"), StandardCharsets.UTF_8)
                + "\n<!-- Child JVM crash fixture update. -->\n").getBytes(StandardCharsets.UTF_8));
        var newer = SkillFixtureFactory.fixture("1.7.1", changed);
        SkillInstallTransaction.Phase stop = SkillInstallTransaction.Phase.valueOf(args[5]);
        new SkillInstallTransaction(scope, owner, target, store, newer.manifest(), newer.files(),
                CodexClientProbe.SUPPORTED_VERSION, "1.7.0-SNAPSHOT", new EmptyProgressIndicator(ModalityState.nonModal()),
                (file, progress) -> true, phase -> {
                    if (phase == stop) Runtime.getRuntime().halt(73);
                }).execute();
        throw new IllegalStateException("Crash phase was not reached");
    }
}
