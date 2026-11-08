package com.liuzhihang.doc.view.service.export;

import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.action.toolbar.window.ProjectDocumentExportAction;
import com.liuzhihang.doc.view.service.document.ProjectDocumentIndexListener;
import com.liuzhihang.doc.view.testsupport.LocalProjectJavaSources;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

public final class ProjectDocumentGenerationLifecycleTest extends LightJavaCodeInsightFixtureTestCase {

    private LocalProjectJavaSources sources;
    private String previousAsyncProperty;
    private Path documentRoot;
    private final List<String> notifications = new CopyOnWriteArrayList<>();
    private final AtomicBoolean commitCallbackObserved = new AtomicBoolean();
    private final AtomicBoolean indexExistedAtCallback = new AtomicBoolean();
    private final AtomicReference<ProgressIndicator> callbackIndicator = new AtomicReference<>();

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_21;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        previousAsyncProperty = System.getProperty("intellij.progress.task.ignoreHeadless");
        System.setProperty("intellij.progress.task.ignoreHeadless", "true");
        Path projectRoot = Path.of(getProject().getBasePath());
        documentRoot = projectRoot.resolve("docs/doc-view");
        clearOutput();

        sources = new LocalProjectJavaSources(getProject(), getModule());
        sources.installApiAnnotationStubs();
        sources.addClass("projectdocs/lifecycle/LifecycleController.java", """
                package projectdocs.lifecycle;

                @org.springframework.web.bind.annotation.RestController
                public class LifecycleController {
                    @org.springframework.web.bind.annotation.GetMapping("/lifecycle")
                    public String lifecycle() { return "ready"; }
                }
                """);
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());

        getProject().getMessageBus().connect(getTestRootDisposable()).subscribe(Notifications.TOPIC,
                new Notifications() {
                    @Override
                    public void notify(Notification notification) {
                        if ("doc-view.NotificationGroup".equals(notification.getGroupId())) {
                            notifications.add(notification.getContent());
                        }
                    }
                });
        getProject().getMessageBus().connect(getTestRootDisposable()).subscribe(ProjectDocumentIndexListener.TOPIC,
                (inputEpoch, snapshotId) -> {
                    if (snapshotId == null || !Files.isRegularFile(documentRoot.resolve("index.json"))
                            || !commitCallbackObserved.compareAndSet(false, true)) {
                        return;
                    }
                    indexExistedAtCallback.set(Files.isRegularFile(documentRoot.resolve("index.json")));
                    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                    callbackIndicator.set(indicator);
                    if (indicator != null) {
                        indicator.cancel();
                    }
                    throw new ProcessCanceledException();
                });
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            PlatformTestUtil.waitForAllBackgroundActivityToCalmDown();
            clearOutput();
            if (sources != null) {
                sources.cleanup();
            }
        } finally {
            if (previousAsyncProperty == null) {
                System.clearProperty("intellij.progress.task.ignoreHeadless");
            } else {
                System.setProperty("intellij.progress.task.ignoreHeadless", previousAsyncProperty);
            }
            super.tearDown();
        }
    }

    public void testCommitCallbackCancellationStillReportsActionSuccess() throws Exception {
        EdtTestUtil.runInEdtAndWait(() -> ProjectDocumentExportAction.generateProject(getProject()));

        await(commitCallbackObserved::get, "The committed index callback should run");
        assertTrue("The listener must run only after index.json has been committed", indexExistedAtCallback.get());
        assertNotNull("Generation must run inside the public ProgressManager wrapper", callbackIndicator.get());
        assertTrue("The callback cancels the actual generation indicator", callbackIndicator.get().isCanceled());
        assertTrue("A committed index remains on disk", Files.isRegularFile(documentRoot.resolve("index.json")));

        await(() -> !notifications.isEmpty(), "The action should report its post-commit outcome");
        String success = DocViewBundle.message("project.export.success", 1, 0,
                DocViewBundle.message("project.export.changed", 4));
        assertTrue("A committed generation must report success even when cancellation is observed afterwards; "
                        + "expected=" + success + ", notifications=" + notifications,
                notifications.contains(success));
        assertFalse("A committed generation must not be reported as cancelled",
                notifications.contains(DocViewBundle.message("project.export.cancelled")));
    }

    private void await(BooleanSupplier condition, String expectation) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            EdtTestUtil.runInEdtAndWait(PlatformTestUtil::dispatchAllEventsInIdeEventQueue);
            Thread.sleep(10);
        }
        assertTrue(expectation + "; notifications=" + notifications
                + "; callback=" + commitCallbackObserved.get()
                + "; index=" + Files.isRegularFile(documentRoot.resolve("index.json")), condition.getAsBoolean());
    }

    private void clearOutput() throws Exception {
        if (!Files.exists(documentRoot)) {
            return;
        }
        try (var paths = Files.walk(documentRoot)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
        Path docs = documentRoot.getParent();
        if (Files.isDirectory(docs)) {
            try (var children = Files.list(docs)) {
                if (children.findAny().isEmpty()) {
                    Files.deleteIfExists(docs);
                }
            }
        }
    }
}
