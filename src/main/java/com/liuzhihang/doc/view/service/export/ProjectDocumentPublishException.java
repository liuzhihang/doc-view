package com.liuzhihang.doc.view.service.export;

import java.io.IOException;
import java.util.List;

public final class ProjectDocumentPublishException extends IOException {

    private final List<String> publishedPaths;
    private final String failedPath;
    private final List<String> temporaryPaths;

    public ProjectDocumentPublishException(IOException cause,
                                           List<String> publishedPaths,
                                           String failedPath,
                                           List<String> temporaryPaths) {
        super("Project documentation export failed at " + failedPath
                + " after publishing: " + String.join(", ", publishedPaths), cause);
        this.publishedPaths = List.copyOf(publishedPaths);
        this.failedPath = failedPath;
        this.temporaryPaths = List.copyOf(temporaryPaths);
    }

    public List<String> publishedPaths() {
        return publishedPaths;
    }

    public String failedPath() {
        return failedPath;
    }

    public List<String> temporaryPaths() {
        return temporaryPaths;
    }
}
