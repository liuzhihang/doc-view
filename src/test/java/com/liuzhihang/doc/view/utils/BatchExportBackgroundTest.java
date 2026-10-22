package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiClass;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.impl.SpringDocViewServiceImpl;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;

import java.awt.EventQueue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BatchExportBackgroundTest extends LightJavaCodeInsightFixtureTestCase {

    private PsiClass controller;
    private String originalSpringTemplate;
    private String originalDubboTemplate;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AnnotationStubs.installSpringWeb(myFixture);
        controller = myFixture.addClass("""
                package batchexport;

                @org.springframework.web.bind.annotation.RestController
                public class ExportController {
                    @org.springframework.web.bind.annotation.GetMapping("/background")
                    public String background() {
                        return "ready";
                    }
                }
                """);
        TemplateSettings templates = TemplateSettings.getInstance(getProject());
        originalSpringTemplate = templates.getSpringTemplate();
        originalDubboTemplate = templates.getDubboTemplate();
        templates.setSpringTemplate("BATCH|${DocView.name}|${DocView.path}|${DocView.method}\n");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            TemplateSettings templates = TemplateSettings.getInstance(getProject());
            templates.setSpringTemplate(originalSpringTemplate);
            templates.setDubboTemplate(originalDubboTemplate);
        } finally {
            super.tearDown();
        }
    }

    public void testBatchSnapshotRendersUnderBackgroundReadActionAndWritesAfterIt() throws Exception {
        var app = ApplicationManager.getApplication();
        List<BatchMarkdownExportFile> snapshot = app.executeOnPooledThread(() -> ReadAction.computeBlocking(() -> {
            assertFalse("PSI preparation must not run on EDT", EventQueue.isDispatchThread());
            assertTrue("PSI preparation must hold a read action", app.isReadAccessAllowed());
            DocView document = new SpringDocViewServiceImpl().buildClassMethodDoc(
                    controller, controller.findMethodsByName("background", false)[0]);
            return ExportUtils.renderBatch(getProject(), "ExportController", List.of(document), true,
                    new EmptyProgressIndicator());
        })).get(10, TimeUnit.SECONDS);

        assertEquals(1, snapshot.size());
        assertEquals("ExportController.md", snapshot.getFirst().fileName());
        assertEquals("BATCH|background|/background|GET\n", snapshot.getFirst().markdown());

        Path output = Files.createTempDirectory("doc-view-batch-background-");
        try {
            AtomicBoolean wroteOffEdtWithoutReadAction = new AtomicBoolean();
            app.executeOnPooledThread((Callable<Void>) () -> {
                wroteOffEdtWithoutReadAction.set(!EventQueue.isDispatchThread() && !app.isReadAccessAllowed());
                BatchMarkdownExportWriter.write(output, snapshot, new EmptyProgressIndicator());
                return null;
            }).get(10, TimeUnit.SECONDS);
            assertTrue("The write phase must consume the immutable string snapshot outside PSI read access",
                    wroteOffEdtWithoutReadAction.get());
            assertEquals("BATCH|background|/background|GET\n", Files.readString(output.resolve("ExportController.md")));
        } finally {
            try (var paths = Files.walk(output)) {
                for (Path path : paths.sorted((left, right) -> right.getNameCount() - left.getNameCount()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    public void testCanceledBatchWriteKeepsExistingTargetBytes() throws Exception {
        Path output = Files.createTempDirectory("doc-view-batch-cancel-");
        Path target = output.resolve("ExportController.md");
        Files.writeString(target, "existing user document\n");
        EmptyProgressIndicator indicator = new EmptyProgressIndicator();
        indicator.cancel();
        try {
            BatchMarkdownExportWriter.write(output,
                    List.of(new BatchMarkdownExportFile("ExportController.md", "new snapshot\n")), indicator);
            fail("Expected the cancelled batch to stop before replacing the target");
        } catch (ProcessCanceledException expected) {
            assertEquals("existing user document\n", Files.readString(target));
        } finally {
            try (var paths = Files.walk(output)) {
                for (Path path : paths.sorted((left, right) -> right.getNameCount() - left.getNameCount()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
