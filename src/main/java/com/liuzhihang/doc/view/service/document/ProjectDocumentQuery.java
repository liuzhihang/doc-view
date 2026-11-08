package com.liuzhihang.doc.view.service.document;

import org.jetbrains.annotations.Nullable;

/** Immutable search input. Filter values are compared exactly as supplied. */
public record ProjectDocumentQuery(@Nullable String query,
                                   @Nullable String module,
                                   @Nullable String framework,
                                   @Nullable String httpMethod,
                                   @Nullable Integer limit,
                                   @Nullable String cursor) {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;

    public ProjectDocumentQuery {
        query = query == null ? "" : query;
    }

    public static ProjectDocumentQuery all() {
        return new ProjectDocumentQuery("", null, null, null, null, null);
    }

    public int effectiveLimit() {
        return limit == null ? DEFAULT_LIMIT : limit;
    }
}
