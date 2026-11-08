package com.liuzhihang.doc.view.service.document;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A bounded, user-safe failure from reading or querying the project document store. */
public final class ProjectDocumentQueryException extends RuntimeException {

    public enum Code {
        INVALID_ARGUMENT,
        PROJECT_NOT_OPEN,
        INDEX_NOT_FOUND,
        UNSUPPORTED_SCHEMA,
        INDEX_INVALID,
        INDEX_INCONSISTENT,
        API_NOT_FOUND,
        CURSOR_STALE,
        SNAPSHOT_CHANGED,
        BUSY,
        CANCELLED,
        TIMEOUT,
        LIMIT_EXCEEDED,
        IO_ERROR
    }

    private final Code code;
    private final String relativePath;

    public ProjectDocumentQueryException(@NotNull Code code, @NotNull String message) {
        this(code, message, null, null);
    }

    public ProjectDocumentQueryException(@NotNull Code code,
                                         @NotNull String message,
                                         @Nullable String relativePath) {
        this(code, message, relativePath, null);
    }

    public ProjectDocumentQueryException(@NotNull Code code,
                                         @NotNull String message,
                                         @Nullable String relativePath,
                                         @Nullable Throwable cause) {
        super(message, cause);
        this.code = code;
        this.relativePath = relativePath;
    }

    public @NotNull Code code() {
        return code;
    }

    public @Nullable String relativePath() {
        return relativePath;
    }
}
