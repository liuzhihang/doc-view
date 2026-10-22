package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import junit.framework.TestCase;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class BatchExportOverwriteTest extends TestCase {

    private Path outputDirectory;

    @Override
    protected void setUp() throws Exception {
        outputDirectory = Files.createTempDirectory("doc-view-batch-export-test-");
    }

    @Override
    protected void tearDown() throws Exception {
        if (outputDirectory != null && Files.exists(outputDirectory)) {
            try (var paths = Files.walk(outputDirectory)) {
                for (Path path : paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    public void testConfirmedBatchWriteReplacesInsteadOfAppending() throws Exception {
        Path target = outputDirectory.resolve("DocView.md");
        Files.writeString(target, "old document\n", StandardCharsets.UTF_8);

        BatchMarkdownExportWriter.write(outputDirectory,
                List.of(new BatchMarkdownExportFile("DocView.md", "current document\n")),
                new EmptyProgressIndicator());

        assertEquals("current document\n", Files.readString(target));
    }

    public void testAllTargetsAreValidatedBeforeAnyBatchWrite() throws Exception {
        Path firstTarget = outputDirectory.resolve("first.md");
        Path invalidTarget = outputDirectory.resolve("invalid.md");
        Files.writeString(firstTarget, "old first\n", StandardCharsets.UTF_8);
        Files.createDirectory(invalidTarget);

        try {
            BatchMarkdownExportWriter.write(outputDirectory, List.of(
                    new BatchMarkdownExportFile("first.md", "new first\n"),
                    new BatchMarkdownExportFile("invalid.md", "new invalid\n")
            ), new EmptyProgressIndicator());
            fail("expected invalid target failure");
        } catch (java.io.IOException expected) {
            assertEquals("old first\n", Files.readString(firstTarget));
        }
    }
}
