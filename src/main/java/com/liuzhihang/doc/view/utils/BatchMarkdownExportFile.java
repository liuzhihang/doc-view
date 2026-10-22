package com.liuzhihang.doc.view.utils;

import java.util.Objects;

/** Rendered, PSI-free output for one target in a legacy batch export. */
public record BatchMarkdownExportFile(String fileName, String markdown) {

    public BatchMarkdownExportFile {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(markdown, "markdown");
    }
}
