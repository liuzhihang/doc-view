package com.liuzhihang.doc.view.service.export;

import java.util.Objects;

/** Immutable, PSI-free source document ready for project export. */
public record ProjectDocumentPage(ProjectDocumentIdentity identity, String framework, String markdown) {

    public ProjectDocumentPage {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(framework, "framework");
        Objects.requireNonNull(markdown, "markdown");
    }
}
