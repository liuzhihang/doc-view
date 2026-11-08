package com.liuzhihang.doc.view.service.document;

import com.liuzhihang.doc.view.service.document.ProjectDocumentData.ApiData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.EntryData;

/** Immutable, hash-verified detail and its index summary from one snapshot. */
public record ProjectDocumentDetailResult(EntryData summary,
                                          ApiData detail,
                                          String snapshotId,
                                          ProjectDocumentIndexStatus.Freshness freshness) {
}
