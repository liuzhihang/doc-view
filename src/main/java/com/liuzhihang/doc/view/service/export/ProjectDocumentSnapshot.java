package com.liuzhihang.doc.view.service.export;

import java.util.List;
import java.util.Objects;

/** A complete project scan result containing no PSI references. */
public record ProjectDocumentSnapshot(List<ProjectDocumentPage> pages,
                                      int skippedCount,
                                      String producerVersion,
                                      String generatorFingerprint,
                                      boolean hasUnsavedDocuments) {

    public ProjectDocumentSnapshot {
        pages = List.copyOf(pages);
        if (skippedCount < 0) {
            throw new IllegalArgumentException("skippedCount must not be negative");
        }
        Objects.requireNonNull(producerVersion, "producerVersion");
        Objects.requireNonNull(generatorFingerprint, "generatorFingerprint");
    }
}
