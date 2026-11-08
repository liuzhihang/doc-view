package com.liuzhihang.doc.view.service.document;

import com.liuzhihang.doc.view.service.document.ProjectDocumentData.EntryData;

/** Immutable, raw-byte hash-verified Markdown page and its index summary. */
public record ProjectDocumentMarkdownResult(EntryData summary,
                                            String markdown,
                                            String relativePath,
                                            String snapshotId,
                                            ProjectDocumentIndexStatus.Freshness freshness) {
}
