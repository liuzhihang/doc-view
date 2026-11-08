package com.liuzhihang.doc.view.service.document;

/** Immutable, raw-byte hash-verified human-readable index page. */
public record ProjectDocumentIndexMarkdownResult(
        String markdown,
        String relativePath,
        String snapshotId,
        ProjectDocumentIndexStatus.Freshness freshness) {
}
