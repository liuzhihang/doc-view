package com.liuzhihang.doc.view.service.export;

import java.io.IOException;
import java.util.List;

public final class ProjectDocumentTemporaryCleanupException extends IOException {

    private final List<String> temporaryPaths;

    public ProjectDocumentTemporaryCleanupException(List<String> temporaryPaths) {
        this(temporaryPaths, null);
    }

    public ProjectDocumentTemporaryCleanupException(List<String> temporaryPaths, Throwable cause) {
        super("Project documentation temporary files could not be removed: " + String.join(", ", temporaryPaths),
                cause);
        this.temporaryPaths = List.copyOf(temporaryPaths);
    }

    public List<String> temporaryPaths() {
        return temporaryPaths;
    }
}
