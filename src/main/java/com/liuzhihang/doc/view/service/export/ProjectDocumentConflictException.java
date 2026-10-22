package com.liuzhihang.doc.view.service.export;

import java.io.IOException;
import java.util.List;

public final class ProjectDocumentConflictException extends IOException {

    private final List<String> conflicts;

    public ProjectDocumentConflictException(List<String> conflicts) {
        super("Project documentation export conflicts: " + String.join(", ", conflicts));
        this.conflicts = List.copyOf(conflicts);
    }

    public List<String> conflicts() {
        return conflicts;
    }
}
