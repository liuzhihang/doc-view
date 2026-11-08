package com.liuzhihang.doc.view.service.document;

import com.liuzhihang.doc.view.service.document.ProjectDocumentData.EntryData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Immutable page of matching entries from one committed index generation. */
public record ProjectDocumentQueryResult(List<EntryData> items,
                                         int totalMatched,
                                         @Nullable String nextCursor,
                                         String snapshotId,
                                         ProjectDocumentIndexStatus.Freshness freshness,
                                         boolean hasUnsavedDocuments,
                                         String generatedAt) {

    public ProjectDocumentQueryResult {
        items = List.copyOf(items);
    }
}
