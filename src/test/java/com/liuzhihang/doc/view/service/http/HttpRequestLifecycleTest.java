package com.liuzhihang.doc.view.service.http;

import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.testFramework.DumbModeTestUtils;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;

import java.awt.EventQueue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

public final class HttpRequestLifecycleTest extends LightJavaCodeInsightFixtureTestCase {
    private PsiMethod method;
    private HttpRequestService service;
    private String oldAsyncProperty;
    private final List<VirtualFile> created = new CopyOnWriteArrayList<>();
    private final List<String> notifications = new CopyOnWriteArrayList<>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // IntelliJ makes Task.queue synchronous in tests by default; exercise production scheduling.
        oldAsyncProperty = System.getProperty("intellij.progress.task.ignoreHeadless");
        System.setProperty("intellij.progress.task.ignoreHeadless", "true");
        AnnotationStubs.installSpringWeb(myFixture);
        method = myFixture.addClass("""
                package contract.httplifecycle;
                @org.springframework.web.bind.annotation.RestController
                public class HttpLifecycleController {
                    @org.springframework.web.bind.annotation.GetMapping("/ready")
                    public String ready() { return "ready"; }
                    @org.springframework.web.bind.annotation.GetMapping("/other")
                    public String ready(@org.springframework.web.bind.annotation.RequestParam("ignored") int ignored) {
                        return "other";
                    }
                }
                """).findMethodsByName("ready", false)[0];
        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
            @Override
            public void fileCreated(VirtualFileEvent event) {
                VirtualFile file = event.getFile();
                if ("http".equals(file.getExtension()) && file.getPath().contains("doc-view-http/")) {
                    created.add(file);
                }
            }
        }, getTestRootDisposable());
        getProject().getMessageBus().connect(getTestRootDisposable()).subscribe(Notifications.TOPIC,
                new Notifications() {
                    @Override
                    public void notify(Notification notification) {
                        if ("doc-view.NotificationGroup".equals(notification.getGroupId())) {
                            notifications.add(notification.getContent());
                        }
                    }
                });
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (service != null) service.dispose();
            PlatformTestUtil.waitForAllBackgroundActivityToCalmDown();
            EdtTestUtil.runInEdtAndWait(() -> WriteCommandAction.runWriteCommandAction(getProject(), () -> {
                for (VirtualFile file : created) {
                    if (!file.isValid()) continue;
                    FileEditorManager.getInstance(getProject()).closeFile(file);
                    try {
                        file.setWritable(true);
                        file.delete(this);
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                }
            }));
        } finally {
            if (oldAsyncProperty == null) System.clearProperty("intellij.progress.task.ignoreHeadless");
            else System.setProperty("intellij.progress.task.ignoreHeadless", oldAsyncProperty);
            super.tearDown();
        }
    }

    public void testGenerationIsOffEdtResponsiveAndCoalescesTheSameInFlightMethod() throws Exception {
        BlockingBuilder builder = new BlockingBuilder();
        service = new HttpRequestService(getProject(), builder, () -> true);
        EdtTestUtil.runInEdtAndWait(() -> { service.generate(method); service.generate(method); });
        try {
            assertTrue(builder.started.await(10, TimeUnit.SECONDS));
            AtomicBoolean uiResponded = new AtomicBoolean();
            EdtTestUtil.runInEdtAndWait(() -> uiResponded.set(true));
            assertTrue(uiResponded.get());
            assertFalse(builder.onEdt.get());
            assertTrue(builder.readAccess.get());
            assertEquals(1, service.activeRequestCount());
        } finally {
            builder.release.countDown();
        }
        await(() -> service.activeRequestCount() == 0);
        assertEquals(1, builder.calls.get());
        assertEquals(1, created.size());
        assertTrue(read(created.getFirst()).contains("GET {{host}}/ready"));
    }

    public void testCancelBeforePublicationCreatesNothingAndAllowsRetry() throws Exception {
        BlockingBuilder builder = new BlockingBuilder();
        service = new HttpRequestService(getProject(), builder, () -> true);
        start();
        assertTrue(builder.started.await(10, TimeUnit.SECONDS));
        // Cancel the task's visible indicator. The NBRA's inner indicator only cancels a read
        // attempt and is deliberately retried after a pending write action.
        java.lang.reflect.Field activeField = HttpRequestService.class.getDeclaredField("active");
        activeField.setAccessible(true);
        Object request = ((java.util.Map<?, ?>) activeField.get(service)).values().iterator().next();
        java.lang.reflect.Field indicatorField = request.getClass().getDeclaredField("indicator");
        indicatorField.setAccessible(true);
        ((ProgressIndicator) indicatorField.get(request)).cancel();
        builder.release.countDown();
        await(() -> service.activeRequestCount() == 0);
        assertEmpty(created);
        assertEmpty(notifications);
        start();
        await(() -> service.activeRequestCount() == 0);
        assertEquals(1, created.size());
    }

    public void testClientBecomingUnavailableBeforePublicationReportsClientReason() throws Exception {
        AtomicBoolean clientAvailable = new AtomicBoolean(true);
        AtomicInteger calls = new AtomicInteger();
        HttpRequestBuilder builder = new HttpRequestBuilder() {
            @Override
            public HttpRequestSnapshot build(PsiMethod current) throws HttpRequestException {
                calls.incrementAndGet();
                HttpRequestSnapshot snapshot = super.build(current);
                clientAvailable.set(false);
                return snapshot;
            }
        };
        service = new HttpRequestService(getProject(), builder, clientAvailable::get);
        start();
        await(() -> service.activeRequestCount() == 0);
        assertEquals("The client should become unavailable only after the request snapshot is built", 1, calls.get());
        assertEmpty(created);
        assertReason("http.request.unavailable.client");
    }

    public void testSourceWriteInterruptsReadAndNeverPublishesAnOldSnapshot() throws Exception {
        BlockingBuilder builder = new BlockingBuilder();
        service = new HttpRequestService(getProject(), builder, () -> true);
        start();
        assertTrue(builder.started.await(10, TimeUnit.SECONDS));
        try {
            EdtTestUtil.runInEdtAndWait(() -> WriteCommandAction.runWriteCommandAction(getProject(),
                    () -> { method.setName("changed"); }));
        } finally {
            builder.release.countDown();
        }
        await(() -> service.activeRequestCount() == 0);
        assertEmpty(created);
        assertReason("http.request.error.stale_source");
    }

    public void testUnrelatedSourceWriteDuringReadIsRetriedAndProducesDraft() throws Exception {
        CountDownLatch firstAttemptStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstAttempt = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        HttpRequestBuilder builder = new HttpRequestBuilder() {
            @Override
            public HttpRequestSnapshot build(PsiMethod current) throws HttpRequestException {
                if (calls.incrementAndGet() == 1) {
                    firstAttemptStarted.countDown();
                    try {
                        while (!releaseFirstAttempt.await(20, TimeUnit.MILLISECONDS)) {
                            ProgressManager.checkCanceled();
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new com.intellij.openapi.progress.ProcessCanceledException();
                    }
                    ProgressManager.checkCanceled();
                }
                return super.build(current);
            }
        };
        service = new HttpRequestService(getProject(), builder, () -> true);
        VirtualFile unrelatedFile = myFixture.addClass("""
                package contract.httplifecycle;
                public class UnrelatedSource { public String value() { return "before"; } }
                """).getContainingFile().getVirtualFile();
        var unrelatedDocument = FileDocumentManager.getInstance().getDocument(unrelatedFile);
        assertNotNull(unrelatedDocument);

        start();
        assertTrue("The request should reach its first background read",
                firstAttemptStarted.await(10, TimeUnit.SECONDS));
        try {
            EdtTestUtil.runInEdtAndWait(() -> WriteCommandAction.runWriteCommandAction(getProject(),
                    () -> unrelatedDocument.setText(unrelatedDocument.getText().replace("before", "after"))));
        } finally {
            releaseFirstAttempt.countDown();
        }

        await(() -> service.activeRequestCount() == 0);
        assertTrue("A write to another source file should cancel and retry the non-blocking read", calls.get() >= 2);
        assertEquals(1, created.size());
        assertTrue(read(created.getFirst()).contains("GET {{host}}/ready"));
        assertTrue(unrelatedDocument.getText().contains("after"));
        assertEmpty(notifications);
    }

    public void testDisposedServiceCancelsWorkAndRejectsLatePublication() throws Exception {
        BlockingBuilder builder = new BlockingBuilder();
        service = new HttpRequestService(getProject(), builder, () -> true);
        start();
        assertTrue(builder.started.await(10, TimeUnit.SECONDS));
        service.dispose();
        builder.release.countDown();
        assertTrue(builder.indicator.get().isCanceled());
        await(() -> service.activeRequestCount() == 0);
        PlatformTestUtil.waitForAllBackgroundActivityToCalmDown();
        assertEmpty(created);
        assertEmpty(notifications);
    }

    public void testRegenerationKeepsUnsavedUserTextAndCreatesNewDraft() throws Exception {
        service = new HttpRequestService(getProject(), new HttpRequestBuilder(), () -> true);
        start();
        await(() -> service.activeRequestCount() == 0);
        assertEquals(1, created.size());
        VirtualFile first = created.getFirst();
        String original = read(first);
        String edited = original + "\n# user edit stays unsaved\n";
        EdtTestUtil.runInEdtAndWait(() -> WriteCommandAction.runWriteCommandAction(getProject(),
                () -> FileDocumentManager.getInstance().getDocument(first).setText(edited)));
        start();
        await(() -> service.activeRequestCount() == 0);
        assertEquals(2, created.size());
        assertNotSame(first, created.getLast());
        assertEquals(edited, FileDocumentManager.getInstance().getDocument(first).getText());
        assertEquals(original, read(first));
        assertTrue(FileDocumentManager.getInstance().isFileModified(first));
        assertEquals(original, read(created.getLast()));
    }

    public void testUnsavedJavaEditorTextIsCommittedWithoutSavingDisk() throws Exception {
        service = new HttpRequestService(getProject(), new HttpRequestBuilder(), () -> true);
        var sourceFile = method.getContainingFile().getVirtualFile();
        String original = read(sourceFile);
        var document = PsiDocumentManager.getInstance(getProject()).getDocument(method.getContainingFile());
        EdtTestUtil.runInEdtAndWait(() -> WriteCommandAction.runWriteCommandAction(getProject(),
                () -> document.setText(document.getText().replace("/ready", "/edited"))));
        start();
        await(() -> service.activeRequestCount() == 0);
        assertEquals(1, created.size());
        assertTrue(read(created.getFirst()).contains("GET {{host}}/edited"));
        assertEquals(original, read(sourceFile));
        assertTrue(FileDocumentManager.getInstance().isFileModified(sourceFile));
    }

    public void testMissingClientAndInvalidSourceDoNotReachBuilder() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        HttpRequestBuilder builder = new HttpRequestBuilder() {
            @Override
            public HttpRequestSnapshot build(PsiMethod current) throws HttpRequestException {
                calls.incrementAndGet();
                return super.build(current);
            }
        };
        service = new HttpRequestService(getProject(), builder, () -> false);
        start();
        assertEquals(0, calls.get());
        assertEquals(1, notifications.size());
        EdtTestUtil.runInEdtAndWait(() -> WriteCommandAction.runWriteCommandAction(getProject(), method::delete));
        start();
        assertEquals(0, calls.get());
        assertEmpty(created);
    }

    public void testMissingClientCanBeRestoredAndRetried() throws Exception {
        AtomicBoolean clientAvailable = new AtomicBoolean();
        AtomicInteger calls = new AtomicInteger();
        HttpRequestBuilder builder = new HttpRequestBuilder() {
            @Override
            public HttpRequestSnapshot build(PsiMethod current) throws HttpRequestException {
                calls.incrementAndGet();
                return super.build(current);
            }
        };
        service = new HttpRequestService(getProject(), builder, clientAvailable::get);

        start();
        assertEquals(0, calls.get());
        assertEmpty(created);
        assertReason("http.request.unavailable.client");

        clientAvailable.set(true);
        start();
        await(() -> service.activeRequestCount() == 0);
        assertEquals(1, calls.get());
        assertEquals(1, created.size());
        assertTrue(read(created.getFirst()).contains("GET {{host}}/ready"));
        assertReason("http.request.unavailable.client");
    }

    public void testDumbModeRejectsRequestBeforeBuilderWithIndexingReason() {
        AtomicInteger calls = new AtomicInteger();
        HttpRequestBuilder builder = new HttpRequestBuilder() {
            @Override
            public HttpRequestSnapshot build(PsiMethod current) throws HttpRequestException {
                calls.incrementAndGet();
                return super.build(current);
            }
        };
        service = new HttpRequestService(getProject(), builder, () -> true);

        DumbModeTestUtils.runInDumbModeSynchronously(getProject(), () -> {
            assertTrue("The fixture must actually enter Dumb Mode", DumbService.isDumb(getProject()));
            start();
            assertEquals(0, calls.get());
            assertEmpty(created);
            assertReason("http.request.unavailable.indexing");
        });
    }

    public void testUnexpectedBuilderFailurePublishesNoDraftAndCanBeRetried() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        HttpRequestBuilder builder = new HttpRequestBuilder() {
            @Override
            public HttpRequestSnapshot build(PsiMethod current) throws HttpRequestException {
                if (calls.incrementAndGet() == 1) {
                    throw new IllegalStateException("private source detail");
                }
                return super.build(current);
            }
        };
        service = new HttpRequestService(getProject(), builder, () -> true);

        start();
        await(() -> service.activeRequestCount() == 0);
        assertEquals(1, calls.get());
        assertEmpty(created);
        assertReason("http.request.error.build_failed");
        assertFalse("Notifications must not expose source-specific exception text",
                notifications.getFirst().contains("private source detail"));

        start();
        await(() -> service.activeRequestCount() == 0);
        assertEquals(2, calls.get());
        assertEquals(1, created.size());
        assertTrue(read(created.getFirst()).contains("GET {{host}}/ready"));
        assertReason("http.request.error.build_failed");
    }

    public void testSameNamedOverloadsCreateIndependentDraftNamesAndContexts() throws Exception {
        service = new HttpRequestService(getProject(), new HttpRequestBuilder(), () -> true);
        PsiMethod noArguments = method;
        PsiMethod oneArgument = java.util.Arrays.stream(method.getContainingClass().findMethodsByName("ready", false))
                .filter(candidate -> candidate.getParameterList().getParametersCount() == 1)
                .findFirst().orElseThrow();

        start(noArguments);
        await(() -> service.activeRequestCount() == 0);
        start(oneArgument);
        await(() -> service.activeRequestCount() == 0);

        assertEquals(2, created.size());
        VirtualFile first = created.getFirst();
        VirtualFile second = created.getLast();
        assertNotSame(first, second);
        assertFalse("Overloads need distinct stable Scratch identities", first.getPath().equals(second.getPath()));
        assertTrue(read(first).contains("HttpLifecycleController.ready()"));
        assertTrue(read(second).contains("HttpLifecycleController.ready(int)"));
        assertEmpty(notifications);
    }

    public void testOversizedUtf8RequestPreservesUnsavedDraftAndAllowsRetry() throws Exception {
        String oversized = "界".repeat(8_500_000);
        VirtualFile sizeProbe = new LightVirtualFile("request.http");
        assertFalse("Character count alone would incorrectly accept this request",
                SingleRootFileViewProvider.isTooLargeForContentLoading(sizeProbe, (long) oversized.length()));
        assertTrue("The fixture must exceed the actual platform content loading limit",
                SingleRootFileViewProvider.isTooLargeForContentLoading(sizeProbe,
                        (long) oversized.getBytes(StandardCharsets.UTF_8).length));
        AtomicInteger calls = new AtomicInteger();
        service = new HttpRequestService(getProject(), new HttpRequestBuilder() {
            @Override
            public HttpRequestSnapshot build(PsiMethod current) {
                return calls.incrementAndGet() == 2 ? new HttpRequestSnapshot(oversized) : super.build(current);
            }
        }, () -> true);
        start();
        await(() -> service.activeRequestCount() == 0);
        VirtualFile first = created.getFirst();
        String original = read(first);
        String edited = original + "\n# preserve unsaved user edit\n";
        EdtTestUtil.runInEdtAndWait(() -> WriteCommandAction.runWriteCommandAction(getProject(),
                () -> FileDocumentManager.getInstance().getDocument(first).setText(edited)));

        start();
        await(() -> service.activeRequestCount() == 0);
        assertEquals("Oversized content must never create another Scratch file", 1, created.size());
        assertReason("http.request.error.too_large");
        assertEquals(edited, FileDocumentManager.getInstance().getDocument(first).getText());
        assertEquals(original, read(first));
        assertTrue(FileDocumentManager.getInstance().isFileModified(first));
        assertTrue(FileEditorManager.getInstance(getProject()).isFileOpen(first));

        start();
        await(() -> service.activeRequestCount() == 0);
        assertEquals(2, created.size());
        assertTrue(read(created.getLast()).contains("GET {{host}}/ready"));
        assertEquals(edited, FileDocumentManager.getInstance().getDocument(first).getText());
        assertReason("http.request.error.too_large");
    }

    public void testSizeLimitIncludesSourceCommentsAndAllowsEditableLargeRequests() throws Exception {
        VirtualFile sizeProbe = new LightVirtualFile("request.http");
        long low = 0;
        long high = 128L * 1024 * 1024;
        assertTrue(SingleRootFileViewProvider.isTooLargeForContentLoading(sizeProbe, high));
        while (low + 1 < high) {
            long middle = (low + high) / 2;
            if (SingleRootFileViewProvider.isTooLargeForContentLoading(sizeProbe, middle)) high = middle;
            else low = middle;
        }
        String boundarySnapshot = "#".repeat(Math.toIntExact(low));
        String editableSnapshot = "POST {{host}}/large\nContent-Type: text/plain\n\n" + "a".repeat(5_658_147);
        assertTrue("This accepted request exceeds the separate code insight limit",
                SingleRootFileViewProvider.isTooLargeForIntelligence(sizeProbe, (long) editableSnapshot.length()));
        AtomicInteger calls = new AtomicInteger();
        service = new HttpRequestService(getProject(), new HttpRequestBuilder() {
            @Override
            public HttpRequestSnapshot build(PsiMethod current) {
                return new HttpRequestSnapshot(calls.incrementAndGet() == 1 ? boundarySnapshot : editableSnapshot);
            }
        }, () -> true);

        start();
        await(() -> service.activeRequestCount() == 0);
        assertEmpty("Adding source comments must push the boundary snapshot over the content loading limit", created);
        assertReason("http.request.error.too_large");

        start();
        await(() -> service.activeRequestCount() == 0);
        assertEquals(1, created.size());
        assertTrue(read(created.getFirst()).endsWith(editableSnapshot));
        assertNotNull(FileDocumentManager.getInstance().getDocument(created.getFirst()));
        assertTrue(FileEditorManager.getInstance(getProject()).isFileOpen(created.getFirst()));
        assertReason("http.request.error.too_large");
    }

    public void testWriteFailureRemovesOnlyTheNewIncompleteFile() throws Exception {
        service = new HttpRequestService(getProject(), new HttpRequestBuilder(), () -> true);
        start();
        await(() -> service.activeRequestCount() == 0);
        VirtualFile first = created.getFirst();
        String original = read(first);
        AtomicBoolean injected = new AtomicBoolean();
        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
            @Override
            public void fileCreated(VirtualFileEvent event) {
                VirtualFile file = event.getFile();
                if ("http".equals(file.getExtension()) && file.getPath().contains("doc-view-http/")) {
                    try { file.setWritable(false); injected.set(true); }
                    catch (IOException exception) { throw new RuntimeException(exception); }
                }
            }
        }, getTestRootDisposable());
        start();
        await(() -> service.activeRequestCount() == 0);
        assertTrue(injected.get());
        assertEquals(2, created.size());
        assertFalse("An incomplete new file must be removed", created.getLast().isValid());
        assertEquals(original, read(first));
        assertFalse(notifications.isEmpty());
    }

    private void start() {
        start(method);
    }

    private void start(PsiMethod selectedMethod) {
        EdtTestUtil.runInEdtAndWait(() -> service.generate(selectedMethod));
    }

    private void assertReason(String key) {
        assertEquals(List.of(DocViewBundle.message(key)), List.copyOf(notifications));
    }

    private void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            EdtTestUtil.runInEdtAndWait(PlatformTestUtil::dispatchAllEventsInIdeEventQueue);
            Thread.sleep(20);
        }
        assertTrue("Background request should finish", condition.getAsBoolean());
    }

    private String read(VirtualFile file) throws IOException {
        return new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
    }

    private static final class BlockingBuilder extends HttpRequestBuilder {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicReference<ProgressIndicator> indicator = new AtomicReference<>();
        private final AtomicBoolean onEdt = new AtomicBoolean();
        private final AtomicBoolean readAccess = new AtomicBoolean();
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public HttpRequestSnapshot build(PsiMethod current) throws HttpRequestException {
            calls.incrementAndGet();
            onEdt.set(EventQueue.isDispatchThread());
            readAccess.set(ApplicationManager.getApplication().isReadAccessAllowed());
            indicator.set(ProgressManager.getInstance().getProgressIndicator());
            started.countDown();
            try {
                while (!release.await(20, TimeUnit.MILLISECONDS)) {
                    ProgressManager.checkCanceled();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new com.intellij.openapi.progress.ProcessCanceledException();
            }
            ProgressManager.checkCanceled();
            return super.build(current);
        }
    }
}
