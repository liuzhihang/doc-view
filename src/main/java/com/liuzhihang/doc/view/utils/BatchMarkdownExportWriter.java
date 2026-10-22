package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.progress.ProgressIndicator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Writes already-rendered batch export data outside a PSI read action. */
public final class BatchMarkdownExportWriter {

    private BatchMarkdownExportWriter() {
    }

    public static void write(Path outputDirectory,
                             List<BatchMarkdownExportFile> files,
                             ProgressIndicator indicator) throws IOException {
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        Objects.requireNonNull(files, "files");
        Objects.requireNonNull(indicator, "indicator");
        Path directory = outputDirectory.toRealPath();
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Export destination is not a directory");
        }

        List<Path> targets = new ArrayList<>();
        for (BatchMarkdownExportFile file : files) {
            String name = file.fileName();
            Path relative = Path.of(name);
            if (name.isBlank() || relative.isAbsolute() || relative.getNameCount() != 1
                    || name.equals(".") || name.equals("..") || name.contains("/") || name.contains("\\")) {
                throw new IOException("Export file name is invalid");
            }
            Path target = directory.resolve(relative);
            if (Files.isSymbolicLink(target)
                    || (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS))) {
                throw new IOException("Export target is not a regular file: " + name);
            }
            targets.add(target);
        }

        for (int i = 0; i < files.size(); i++) {
            indicator.checkCanceled();
            Files.writeString(targets.get(i), files.get(i).markdown(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        }
    }
}
