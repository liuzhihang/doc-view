package com.liuzhihang.doc.view.service.export;

import java.util.List;

/** A complete project scan result containing no PSI references. */
public record ProjectDocumentSnapshot(List<ProjectDocumentPage> pages, int skippedCount) {

    public ProjectDocumentSnapshot {
        pages = List.copyOf(pages);
        if (skippedCount < 0) {
            throw new IllegalArgumentException("skippedCount must not be negative");
        }
    }
}
