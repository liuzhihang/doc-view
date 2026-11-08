package com.liuzhihang.doc.view.service.export;

/** A generation stopped before the final index commit. */
public final class ProjectDocumentGenerationException extends RuntimeException {
    public enum Code { SOURCE_CHANGED, TIMEOUT }

    private final Code code;

    public ProjectDocumentGenerationException(Code code) {
        super(code.name());
        this.code = code;
    }

    public Code code() {
        return code;
    }
}
