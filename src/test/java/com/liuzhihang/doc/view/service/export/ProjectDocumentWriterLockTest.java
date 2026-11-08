package com.liuzhihang.doc.view.service.export;

import com.liuzhihang.doc.view.service.document.ProjectDocumentQueryException;
import junit.framework.TestCase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.liuzhihang.doc.view.service.export.ProjectDocumentFixtures.snapshot;

public final class ProjectDocumentWriterLockTest extends TestCase {
    public void testIndependentProcessWriterReturnsBusyWhileDifferentRootCanPublish() throws Exception {
        Path root = Files.createTempDirectory("doc-view-writer-lock-");
        Process process = null;
        try {
            Path output = Files.createDirectories(root.resolve("docs/doc-view"));
            Path ready = root.resolve("child-output.txt");
            Path classesPath = Files.createDirectory(root.resolve("probe-classes"));
            String resource = DocumentWriterLockProbe.class.getName().replace('.', '/') + ".class";
            Path classFile = classesPath.resolve(resource);
            Files.createDirectories(classFile.getParent());
            try (var bytes = DocumentWriterLockProbe.class.getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(bytes);
                Files.copy(bytes, classFile);
            }
            String classes = classesPath.toString();
            process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/java").toString(),
                    "-cp", classes, DocumentWriterLockProbe.class.getName(), output.resolve(".doc-view.lock").toString())
                    .redirectErrorStream(true).redirectOutput(ready.toFile()).start();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < deadline && process.isAlive() && !Files.readString(ready).contains("READY")) {
                Thread.sleep(10);
            }
            assertTrue("child writer must hold the lock: " + Files.readString(ready), Files.readString(ready).contains("READY"));
            try {
                new ProjectDocumentExporter().export(root, snapshot(List.of()),
                        new ProjectDocumentExporterTest.TestProgressIndicator());
                fail("second writer must be rejected");
            } catch (ProjectDocumentQueryException busy) {
                assertEquals(ProjectDocumentQueryException.Code.BUSY, busy.code());
            }
            assertFalse(Files.exists(output.resolve("index.json")));
            Path other = Files.createDirectory(root.resolve("different-project"));
            new ProjectDocumentExporter().export(other, snapshot(List.of()),
                    new ProjectDocumentExporterTest.TestProgressIndicator());
            assertTrue(Files.exists(other.resolve("docs/doc-view/index.json")));
            process.getOutputStream().write(1);
            process.getOutputStream().flush();
            assertTrue(process.waitFor(5, TimeUnit.SECONDS));
            assertEquals(0, process.exitValue());
            new ProjectDocumentExporter().export(root, snapshot(List.of()),
                    new ProjectDocumentExporterTest.TestProgressIndicator());
            assertTrue(Files.exists(output.resolve("index.json")));
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
}
