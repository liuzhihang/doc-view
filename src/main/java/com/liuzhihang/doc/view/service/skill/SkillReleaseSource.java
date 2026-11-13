package com.liuzhihang.doc.view.service.skill;

import com.intellij.openapi.progress.ProgressIndicator;

import java.io.IOException;
import java.util.Optional;

interface SkillReleaseSource {
    record Candidate(SkillReleaseManifest manifest, long archiveAssetId) {}

    Optional<Candidate> latest(ProgressIndicator indicator) throws IOException;

    byte[] downloadArchive(Candidate candidate, ProgressIndicator indicator) throws IOException;
}
