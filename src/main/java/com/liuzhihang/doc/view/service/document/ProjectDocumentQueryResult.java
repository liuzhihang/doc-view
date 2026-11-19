package com.liuzhihang.doc.view.service.document;

import com.liuzhihang.doc.view.service.document.ProjectDocumentData.EntryData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Immutable page of matching entries from one verified index, including the status observed during that read. */
public record ProjectDocumentQueryResult(List<EntryData> items,
                                         int totalMatched,
                                         @Nullable String nextCursor,
                                         ProjectDocumentIndexStatus indexStatus) {

    public ProjectDocumentQueryResult {
        items = List.copyOf(items);
    }
}
