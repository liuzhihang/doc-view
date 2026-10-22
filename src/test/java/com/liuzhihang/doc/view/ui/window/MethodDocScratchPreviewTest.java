package com.liuzhihang.doc.view.ui.window;

import com.intellij.ide.extensionResources.ExtensionsRootType;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.enums.FrameworkEnum;
import com.liuzhihang.doc.view.service.impl.SpringDocViewServiceImpl;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;

import java.awt.EventQueue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Regression coverage for opening a method document from the API tree. */
public final class MethodDocScratchPreviewTest extends LightJavaCodeInsightFixtureTestCase {

    private static final String TEST_TEMPLATE = "ASYNC|${DocView.name}|${DocView.path}|${DocView.method}\n";
    private static final String TEST_CONTROLLER = "MethodPreviewController";

    private PsiClass controller;
    private TemplateSettings templateSettings;
    private String originalSpringTemplate;
    private String originalDubboTemplate;
    private final List<VirtualFile> createdScratchFiles = new ArrayList<>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AnnotationStubs.installSpringWeb(myFixture);
        controller = myFixture.addClass("""
                package contract.asyncpreview;

                @org.springframework.web.bind.annotation.RestController
                @org.springframework.web.bind.annotation.RequestMapping("/api")
                public class MethodPreviewController {
                    @org.springframework.web.bind.annotation.GetMapping("/status")
                    public String status() { return "ready"; }
                }
                """);
        templateSettings = TemplateSettings.getInstance(getProject());
        originalSpringTemplate = templateSettings.getSpringTemplate();
        originalDubboTemplate = templateSettings.getDubboTemplate();
        templateSettings.setSpringTemplate(TEST_TEMPLATE);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            Set<VirtualFile> testScratchFiles = new HashSet<>(createdScratchFiles);
            EdtTestUtil.runInEdtAndWait(() -> {
                FileEditorManager manager = FileEditorManager.getInstance(getProject());
                for (VirtualFile file : manager.getOpenFiles()) {
                    if (isTestScratch(file)) {
                        testScratchFiles.add(file);
                        manager.closeFile(file);
                    }
                }
            });
            WriteCommandAction.runWriteCommandAction(getProject(), () -> {
                for (VirtualFile file : testScratchFiles) {
                    if (isTestScratch(file) && file.isValid()) {
                        try {
                            if (!file.isWritable()) {
                                file.setWritable(true);
                            }
                            file.delete(getProject());
                        } catch (IOException ignored) {
                            // The light test project is discarded after this fixture.
                        }
                    }
                }
            });
        } finally {
            if (templateSettings != null) {
                templateSettings.setSpringTemplate(originalSpringTemplate);
                templateSettings.setDubboTemplate(originalDubboTemplate);
            }
            super.tearDown();
        }
    }

    public void testMethodNodeGeneratesOffEdtAndOpensCurrentTemplateScratch() throws Exception {
        Application application = ApplicationManager.getApplication();
        MethodNode node = methodNode();
        String expectedDocPath = node.docPath(getProject());
        Set<VirtualFile> openedBefore = openedFiles();
        CountDownLatch generationStarted = new CountDownLatch(1);
        CountDownLatch releaseGeneration = new CountDownLatch(1);
        CountDownLatch uiProbeCompleted = new CountDownLatch(1);
        CountDownLatch uiProbeProcessed = new CountDownLatch(1);
        AtomicBoolean generationRanOnEdt = new AtomicBoolean(true);
        AtomicBoolean generationHeldReadAction = new AtomicBoolean();
        AtomicBoolean uiEventProcessedBeforeGenerationReleased = new AtomicBoolean();

        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), SpringDocViewServiceImpl.class,
                new SpringDocViewServiceImpl() {
                    @Override
                    public DocView buildClassMethodDoc(PsiClass psiClass, PsiMethod psiMethod) {
                        generationRanOnEdt.set(EventQueue.isDispatchThread());
                        generationHeldReadAction.set(application.isReadAccessAllowed());
                        generationStarted.countDown();
                        awaitRelease(releaseGeneration);
                        return super.buildClassMethodDoc(psiClass, psiMethod);
                    }
                }, getTestRootDisposable());

        application.executeOnPooledThread(() -> {
            try {
                if (!generationStarted.await(10, TimeUnit.SECONDS)) {
                    return;
                }
                application.invokeLater(uiProbeProcessed::countDown);
                uiEventProcessedBeforeGenerationReleased.set(uiProbeProcessed.await(2, TimeUnit.SECONDS));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                releaseGeneration.countDown();
                uiProbeCompleted.countDown();
            }
        });

        EdtTestUtil.runInEdtAndWait(() -> node.handleDoubleClickOrEnter(null, null));

        assertTrue("The production method generator should be reached", generationStarted.await(10, TimeUnit.SECONDS));
        assertTrue("The UI responsiveness probe should finish", uiProbeCompleted.await(10, TimeUnit.SECONDS));
        assertTrue("The EDT must process another UI event while PSI generation is pending",
                uiEventProcessedBeforeGenerationReleased.get());
        assertFalse("Method PSI generation must run away from EDT", generationRanOnEdt.get());
        assertTrue("The method generator must run under read access", generationHeldReadAction.get());

        VirtualFile scratch = awaitNewOpenedScratch(openedBefore);
        String normalizedPath = scratch.getPath().replace('\\', '/');
        String normalizedDocPath = expectedDocPath.replace('\\', '/');
        assertTrue("The existing method document path must be preserved: " + normalizedPath,
                normalizedPath.endsWith(normalizedDocPath) || normalizedPath.endsWith(normalizedDocPath + ".md"));
        assertTrue("The generated Scratch must contain the complete current-template Markdown",
                new String(scratch.contentsToByteArray(), java.nio.charset.StandardCharsets.UTF_8)
                        .contains("ASYNC|status|/api/status|GET"));
    }

    public void testCancelDuringMethodGenerationDoesNotOpenScratchOrNotifyError() throws Exception {
        MethodNode node = methodNode();
        Set<VirtualFile> openedBefore = openedFiles();
        CountDownLatch generationStarted = new CountDownLatch(1);
        CountDownLatch releaseGeneration = new CountDownLatch(1);
        CountDownLatch generationReturned = new CountDownLatch(1);
        AtomicReference<ProgressIndicator> activeIndicator = new AtomicReference<>();
        AtomicReference<Boolean> hadErrorNotification = new AtomicReference<>(false);
        subscribeToDocViewNotifications(hadErrorNotification);

        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), SpringDocViewServiceImpl.class,
                new SpringDocViewServiceImpl() {
                    @Override
                    public DocView buildClassMethodDoc(PsiClass psiClass, PsiMethod psiMethod) {
                        activeIndicator.set(ProgressManager.getInstance().getProgressIndicator());
                        generationStarted.countDown();
                        try {
                            awaitRelease(releaseGeneration);
                            return super.buildClassMethodDoc(psiClass, psiMethod);
                        } finally {
                            generationReturned.countDown();
                        }
                    }
                }, getTestRootDisposable());

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                if (generationStarted.await(10, TimeUnit.SECONDS)) {
                    ProgressIndicator indicator = activeIndicator.get();
                    if (indicator != null) {
                        indicator.cancel();
                    }
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                releaseGeneration.countDown();
            }
        });

        EdtTestUtil.runInEdtAndWait(() -> node.handleDoubleClickOrEnter(null, null));

        assertTrue("The production generator should start", generationStarted.await(10, TimeUnit.SECONDS));
        assertNotNull("Background generation should expose its active cancellation indicator", activeIndicator.get());
        activeIndicator.get().cancel();
        releaseGeneration.countDown();
        assertTrue("The canceled generator should return", generationReturned.await(10, TimeUnit.SECONDS));
        PlatformTestUtil.waitForAllBackgroundActivityToCalmDown();
        EdtTestUtil.runInEdtAndWait(PlatformTestUtil::dispatchAllEventsInIdeEventQueue);

        assertNoNewOpenedScratch(openedBefore);
        assertFalse("A canceled request must not show an error notification", hadErrorNotification.get());
    }

    public void testInvalidMethodPsiIsDiscardedBeforeTheGeneratorRuns() throws Exception {
        MethodNode node = methodNode();
        Set<VirtualFile> openedBefore = openedFiles();
        AtomicReference<Integer> generatorCalls = new AtomicReference<>(0);
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), SpringDocViewServiceImpl.class,
                new SpringDocViewServiceImpl() {
                    @Override
                    public DocView buildClassMethodDoc(PsiClass psiClass, PsiMethod psiMethod) {
                        generatorCalls.updateAndGet(value -> value + 1);
                        DocView docView = new DocView();
                        docView.setPsiClass(psiClass);
                        docView.setPsiMethod(psiMethod);
                        docView.setName("invalid");
                        docView.setPath("/invalid");
                        docView.setMethod("GET");
                        docView.setType(FrameworkEnum.SPRING);
                        docView.setReqBody(new Body());
                        docView.setRespBody(new Body());
                        return docView;
                    }
                }, getTestRootDisposable());

        WriteCommandAction.runWriteCommandAction(getProject(), () -> node.getPsiMethod().delete());
        EdtTestUtil.runInEdtAndWait(() -> node.handleDoubleClickOrEnter(null, null));
        PlatformTestUtil.waitForAllBackgroundActivityToCalmDown();
        EdtTestUtil.runInEdtAndWait(PlatformTestUtil::dispatchAllEventsInIdeEventQueue);

        assertEquals("An invalid source method must not reach document generation", 0, generatorCalls.get().intValue());
        assertNoNewOpenedScratch(openedBefore);
    }

    public void testGenerationFailureNotifiesWithoutOpeningAnIncompleteScratch() throws Exception {
        MethodNode node = methodNode();
        Set<VirtualFile> openedBefore = openedFiles();
        AtomicReference<Boolean> notified = new AtomicReference<>(false);
        subscribeToDocViewNotifications(notified);
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), SpringDocViewServiceImpl.class,
                new SpringDocViewServiceImpl() {
                    @Override
                    public DocView buildClassMethodDoc(PsiClass psiClass, PsiMethod psiMethod) {
                        throw new IllegalStateException("Synthetic method generation failure");
                    }
                }, getTestRootDisposable());

        EdtTestUtil.runInEdtAndWait(() -> node.handleDoubleClickOrEnter(null, null));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!notified.get() && System.nanoTime() < deadline) {
            EdtTestUtil.runInEdtAndWait(PlatformTestUtil::dispatchAllEventsInIdeEventQueue);
            Thread.sleep(20);
        }

        assertTrue("A non-cancellation generation error must notify the user", notified.get());
        assertNoNewOpenedScratch(openedBefore);
    }

    public void testScratchCreationFailureNotifiesWithoutOpeningDocument() throws Exception {
        String methodName = "method" + "x".repeat(300);
        PsiClass longPathController = myFixture.addClass("""
                package contract.asyncpreview;

                @org.springframework.web.bind.annotation.RestController
                @org.springframework.web.bind.annotation.RequestMapping("/api")
                public class MethodPreviewControllerLongPath {
                    @org.springframework.web.bind.annotation.GetMapping("/status")
                    public String %s() { return "ready"; }
                }
                """.formatted(methodName));
        MethodNode node = methodNode(longPathController);
        Set<VirtualFile> openedBefore = openedFiles();
        AtomicReference<Boolean> notified = new AtomicReference<>(false);
        AtomicBoolean generationCompleted = new AtomicBoolean();
        subscribeToDocViewNotifications(notified);
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), SpringDocViewServiceImpl.class,
                new SpringDocViewServiceImpl() {
                    @Override
                    public DocView buildClassMethodDoc(PsiClass psiClass, PsiMethod psiMethod) {
                        DocView docView = super.buildClassMethodDoc(psiClass, psiMethod);
                        generationCompleted.set(true);
                        return docView;
                    }
                }, getTestRootDisposable());
        assertTrue("The method name must exceed the filesystem component limit",
                node.docPath(getProject()).endsWith(methodName + ".md") && methodName.length() > 255);

        EdtTestUtil.runInEdtAndWait(() -> node.handleDoubleClickOrEnter(null, null));
        awaitNotification(notified);

        assertTrue("The test must reach Scratch creation after Markdown generation", generationCompleted.get());
        assertNoNewOpenedScratch(openedBefore);
    }

    public void testScratchWriteFailureNotifiesWithoutOpeningDocument() throws Exception {
        MethodNode node = methodNode();
        Set<VirtualFile> openedBefore = openedFiles();
        AtomicReference<Boolean> notified = new AtomicReference<>(false);
        AtomicBoolean generationCompleted = new AtomicBoolean();
        AtomicBoolean scratchWasMadeReadOnly = new AtomicBoolean();
        subscribeToDocViewNotifications(notified);
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), SpringDocViewServiceImpl.class,
                new SpringDocViewServiceImpl() {
                    @Override
                    public DocView buildClassMethodDoc(PsiClass psiClass, PsiMethod psiMethod) {
                        DocView docView = super.buildClassMethodDoc(psiClass, psiMethod);
                        generationCompleted.set(true);
                        return docView;
                    }
                }, getTestRootDisposable());
        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
            @Override
            public void fileCreated(VirtualFileEvent event) {
                VirtualFile scratchFile = event.getFile();
                if (!scratchFile.getPath().replace('\\', '/').contains(TEST_CONTROLLER)
                        || !"status.md".equals(scratchFile.getName())) {
                    return;
                }
                createdScratchFiles.add(scratchFile);
                try {
                    scratchFile.setWritable(false);
                    scratchWasMadeReadOnly.set(!scratchFile.isWritable());
                } catch (IOException exception) {
                    // The assertion below distinguishes a failed injection from a write failure.
                }
            }
        }, getTestRootDisposable());

        EdtTestUtil.runInEdtAndWait(() -> node.handleDoubleClickOrEnter(null, null));
        awaitNotification(notified);

        assertTrue("The test must reach Scratch creation after Markdown generation", generationCompleted.get());
        assertTrue("The test must make the returned Scratch unwritable before Markdown is saved",
                scratchWasMadeReadOnly.get());
        assertNoNewOpenedScratch(openedBefore);
    }

    private MethodNode methodNode() {
        return methodNode(controller);
    }

    private MethodNode methodNode(PsiClass sourceClass) {
        AtomicReference<MethodNode> result = new AtomicReference<>();
        EdtTestUtil.runInEdtAndWait(() -> {
            ClassNode classNode = new ClassNode(new ModuleNode(new RootNode(), getModule()), sourceClass);
            result.set((MethodNode) classNode.getChildren()[0]);
        });
        return result.get();
    }

    private Set<VirtualFile> openedFiles() {
        AtomicReference<Set<VirtualFile>> result = new AtomicReference<>();
        EdtTestUtil.runInEdtAndWait(() -> result.set(new HashSet<>(Arrays.asList(
                FileEditorManager.getInstance(getProject()).getOpenFiles()))));
        return result.get();
    }

    private VirtualFile awaitNewOpenedScratch(Set<VirtualFile> openedBefore) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            AtomicReference<VirtualFile> result = new AtomicReference<>();
            EdtTestUtil.runInEdtAndWait(() -> {
                for (VirtualFile file : FileEditorManager.getInstance(getProject()).getOpenFiles()) {
                    if (!openedBefore.contains(file) && isTestScratch(file)) {
                        result.set(file);
                        break;
                    }
                }
            });
            if (result.get() != null) {
                return result.get();
            }
            Thread.sleep(20);
        }
        fail("A successful method generation should open its Scratch file");
        throw new AssertionError("Unreachable");
    }

    private void assertNoNewOpenedScratch(Set<VirtualFile> openedBefore) {
        for (VirtualFile file : openedFiles()) {
            assertTrue("A request that does not finish Scratch publication must not open a new test Scratch file",
                    openedBefore.contains(file) || !isTestScratch(file));
        }
    }

    private void awaitNotification(AtomicReference<Boolean> notified) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!notified.get() && System.nanoTime() < deadline) {
            EdtTestUtil.runInEdtAndWait(PlatformTestUtil::dispatchAllEventsInIdeEventQueue);
            Thread.sleep(20);
        }
        assertTrue("A Scratch create/write error must notify the user", notified.get());
    }

    private boolean isTestScratch(VirtualFile file) {
        return file.isValid() && ExtensionsRootType.getInstance().containsFile(file)
                && file.getPath().replace('\\', '/').contains(TEST_CONTROLLER);
    }

    private void subscribeToDocViewNotifications(AtomicReference<Boolean> notified) {
        com.intellij.notification.Notifications listener = new com.intellij.notification.Notifications() {
            @Override
            public void notify(com.intellij.notification.Notification notification) {
                if ("doc-view.NotificationGroup".equals(notification.getGroupId())) {
                    notified.set(true);
                }
            }
        };
        ApplicationManager.getApplication().getMessageBus().connect(getTestRootDisposable())
                .subscribe(com.intellij.notification.Notifications.TOPIC, listener);
        getProject().getMessageBus().connect(getTestRootDisposable())
                .subscribe(com.intellij.notification.Notifications.TOPIC, listener);
    }

    private static void awaitRelease(CountDownLatch release) {
        try {
            if (!release.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("The test must release the background generation gate");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Background generation gate was interrupted", exception);
        }
    }
}
