package com.liuzhihang.doc.view.service.export;

public final class ProjectDocumentIndexNotReadyException extends IllegalStateException {

    public ProjectDocumentIndexNotReadyException() {
        super("Project indexes are not ready");
    }
}
