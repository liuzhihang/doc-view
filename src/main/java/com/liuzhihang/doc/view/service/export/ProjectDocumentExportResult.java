package com.liuzhihang.doc.view.service.export;

import java.util.List;

public record ProjectDocumentExportResult(int exportedPageCount,
                                         List<String> changedPaths,
                                         List<String> unchangedPaths,
                                         String snapshotId,
                                         boolean previousIndexExisted,
                                         int createdPageCount,
                                         int updatedPageCount,
                                         int unchangedPageCount) {
    public ProjectDocumentExportResult {
        changedPaths = List.copyOf(changedPaths);
        unchangedPaths = List.copyOf(unchangedPaths);
    }
}
