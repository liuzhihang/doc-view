package com.liuzhihang.doc.view.ui;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.registry.RegistryValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.jcef.JBCefApp;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.enums.FrameworkEnum;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.service.impl.DubboDocViewServiceImpl;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class PreviewLifecycleTest extends LightJavaCodeInsightFixtureTestCase {

    private Project project;
    private EditorFactory editorFactory;
    private Set<Editor> editorsBefore;
    private Editor unrelatedEditor;
    private PsiClass fixtureClass;
    private PsiClass listFixtureClass;
    private final List<PreviewForm> forms = new ArrayList<>();
    private RegistryValue jcefEnabled;
    private RegistryValue jcefTestMode;
    private boolean originalJcefEnabled;
    private boolean originalJcefTestMode;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        project = getProject();
        editorFactory = EditorFactory.getInstance();
        editorsBefore = editorsForProject();
        jcefEnabled = Registry.get("ide.browser.jcef.enabled");
        jcefTestMode = Registry.get("ide.browser.jcef.testMode.enabled");
        originalJcefEnabled = jcefEnabled.asBoolean();
        originalJcefTestMode = jcefTestMode.asBoolean();
        jcefTestMode.setValue(true);
        jcefEnabled.setValue(false);
        AnnotationStubs.installSpringWeb(myFixture);
        AnnotationStubs.installDubbo(myFixture);
        fixtureClass = myFixture.addClass("package preview; public class PreviewFixture {}");

        EdtTestUtil.runInEdtAndWait(() -> {
            unrelatedEditor = editorFactory.createViewer(
                    editorFactory.createDocument("unrelated editor"), project, EditorKind.UNTYPED);
        });
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (project != null && editorFactory != null) {
                EdtTestUtil.runInEdtAndWait(() -> {
                    for (PreviewForm form : forms) {
                        JBPopup popup = fieldValue(form, "popup", JBPopup.class);
                        if (popup != null && !popup.isDisposed()) {
                            popup.cancel();
                        }
                        if (Disposable.class.isInstance(form)) {
                            ((Disposable) form).dispose();
                        }
                    }
                    releaseCreatedEditors();
                });
            }
        } finally {
            try {
                if (jcefEnabled != null && jcefTestMode != null) {
                    jcefEnabled.setValue(originalJcefEnabled);
                    jcefTestMode.setValue(true);
                    JBCefApp.isSupported();
                    jcefTestMode.setValue(originalJcefTestMode);
                }
            } finally {
                super.tearDown();
            }
        }
    }

    public void testUnshownFormDoesNotAllocateEditorOrAffectUnrelatedEditor() throws Exception {
        Set<Editor> editorsBeforeForm = editorsForProject();
        createForm();

        assertEquals("Constructing a preview that was never shown must not allocate a source viewer",
                editorsBeforeForm, editorsForProject());
        assertTrue("An unrelated editor must remain available", isEditorAlive(unrelatedEditor));
    }

    public void testPopupCloseReleasesOnlyThePreviewEditor() throws Exception {
        Set<Editor> editorsBeforePreview = editorsForProject();
        PreviewForm form = createForm();

        try {
            EdtTestUtil.runInEdtAndWait(form::popup);
        } catch (RuntimeException ignoredInFixture) {
            // Light fixtures may not provide a window for showing the popup. The implementation
            // must still release resources created before the show attempt fails.
        }

        JBPopup popup = fieldValue(form, "popup", JBPopup.class);
        if (popup != null && !popup.isDisposed()) {
            EdtTestUtil.runInEdtAndWait(popup::cancel);
        }

        assertEquals("Closing or failing to show a preview must release its viewer",
                editorsBeforePreview, editorsForProject());
        assertTrue("Closing a preview must preserve an unrelated editor", isEditorAlive(unrelatedEditor));
    }

    public void testNewRenderClearsPreviousMarkdownAndShowsVisibleProgress() throws Exception {
        PreviewForm form = createForm();
        EdtTestUtil.runInEdtAndWait(() -> initializeUi(form));

        DocView previous = new DocView("PreviousDoc");
        previous.setPsiClass(fixtureClass);
        DocView failing = new DocView("FailingDoc");
        failing.setPsiClass(fixtureClass);

        AtomicReference<String> loadingState = new AtomicReference<>();
        AtomicReference<Object> loadingExport = new AtomicReference<>();
        EdtTestUtil.runInEdtAndWait(() -> {
            setField(form, "currentDocView", previous);
            setField(form, "currentMarkdownText", "# Previous document\n");
            setDocumentText(form, "# Previous document\n");
            setField(form, "currentDocView", failing);
            invoke(form, "renderMarkdown", new Class<?>[]{DocView.class}, failing);
            loadingState.set(fieldValue(form, "markdownDocument", Document.class).getText());
            loadingExport.set(invoke(form, "captureSingleExport", new Class<?>[0]));
        });

        assertTrue("A new selection must replace stale content with a current loading or terminal state",
                DocViewBundle.message("ui.preview.rendering").equals(loadingState.get())
                        || DocViewBundle.message("ui.preview.render.failed").equals(loadingState.get()));
        assertNull("Progress text must not remain available as exportable Markdown",
                fieldValue(form, "currentMarkdownText", String.class));
        assertNull("An in-progress selection must not produce a single-document export snapshot", loadingExport.get());
    }

    public void testFailedRenderReplacesProgressWithVisibleTerminalState() throws Exception {
        PreviewForm form = createForm();
        EdtTestUtil.runInEdtAndWait(() -> initializeUi(form));
        DocView failing = new DocView("FailingDoc");
        failing.setPsiClass(fixtureClass);
        // DocViewData dereferences the framework type; null creates a deterministic generation failure.
        failing.setType(null);

        AtomicReference<String> initialLoadingState = new AtomicReference<>();
        EdtTestUtil.runInEdtAndWait(() -> {
            setField(form, "currentDocView", failing);
            invoke(form, "renderMarkdown", new Class<?>[]{DocView.class}, failing);
            initialLoadingState.set(fieldValue(form, "markdownDocument", Document.class).getText());
        });

        assertTrue("The real preview editor should show a rendering or current terminal state",
                DocViewBundle.message("ui.preview.rendering").equals(initialLoadingState.get())
                        || DocViewBundle.message("ui.preview.render.failed").equals(initialLoadingState.get()));
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < deadline
                && DocViewBundle.message("ui.preview.rendering").equals(documentText(form))) {
            Thread.sleep(20);
        }
        String visibleText = documentText(form);
        assertEquals("A failed current request must replace the visible rendering state",
                DocViewBundle.message("ui.preview.render.failed"), visibleText);
        assertNull("A failed generation must not expose stale Markdown", fieldValue(form, "currentMarkdownText", String.class));
    }

    public void testEmptyDocumentListReplacesLoadingState() throws Exception {
        PreviewForm form = createForm();
        EdtTestUtil.runInEdtAndWait(() -> {
            initializeUi(form);
            fieldValue(form, "loadRequest", AtomicLong.class).set(1L);
            invoke(form, "applyDocViewList", new Class<?>[]{List.class, long.class}, List.of(), 1L);
        });

        assertEquals(DocViewBundle.message("ui.preview.empty"), documentText(form));
        assertNull("An empty result must not be treated as generated Markdown",
                fieldValue(form, "currentMarkdownText", String.class));
    }

    public void testQueuedListTaskFailureCancellationAndEmptyResultReachVisibleTerminalState() throws Exception {
        assertQueuedListTerminal(() -> { throw new IllegalStateException("fixture list failure"); },
                DocViewBundle.message("ui.preview.load.failed"));
        assertQueuedListTerminal(() -> { throw new ProcessCanceledException(); },
                DocViewBundle.message("ui.preview.load.cancelled"));
        assertQueuedListTerminal(List::of, DocViewBundle.message("ui.preview.empty"));
    }

    private void assertQueuedListTerminal(Supplier<List<DocView>> result, String terminal) throws Exception {
        Disposable serviceOwner = Disposer.newDisposable();
        CountDownLatch called = new CountDownLatch(1);
        try {
            replaceListService(serviceOwner, () -> {
                called.countDown();
                return result.get();
            });
            PreviewForm form = createListForm();
            EdtTestUtil.runInEdtAndWait(() -> {
                initializeUi(form);
                invoke(form, "buildDoc", new Class<?>[0]); // Queue the real production Backgroundable task.
            });
            assertTrue("The list task must execute the real service boundary", called.await(5, TimeUnit.SECONDS));
            awaitMarkdown(form, terminal);
            assertNull(fieldValue(form, "currentMarkdownText", String.class));
            assertNull(invoke(form, "captureSingleExport", new Class<?>[0]));
        } finally {
            Disposer.dispose(serviceOwner);
        }
    }

    public void testQueuedListTaskCompletingAfterCloseCannotMutateReleasedPreview() throws Exception {
        for (boolean fail : new boolean[]{false, true}) {
            Disposable serviceOwner = Disposer.newDisposable();
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            CountDownLatch returned = new CountDownLatch(1);
            try {
                replaceListService(serviceOwner, () -> {
                    started.countDown();
                    try {
                        if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("Test did not release list task");
                        if (fail) throw new IllegalStateException("late list failure");
                        return List.of();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(exception);
                    } finally {
                        returned.countDown();
                    }
                });
                Set<Editor> before = editorsForProject();
                PreviewForm form = createListForm();
                EdtTestUtil.runInEdtAndWait(() -> {
                    initializeUi(form);
                    invoke(form, "buildDoc", new Class<?>[0]);
                });
                assertTrue("The background task must be running before close", started.await(5, TimeUnit.SECONDS));
                Document document = fieldValue(form, "markdownDocument", Document.class);
                String loading = documentText(form);
                EdtTestUtil.runInEdtAndWait(form::dispose);
                assertEquals("Dispose must release the preview before the late result arrives", before, editorsForProject());
                release.countDown();
                assertTrue(returned.await(5, TimeUnit.SECONDS));
                PlatformTestUtil.waitForAllBackgroundActivityToCalmDown();
                EdtTestUtil.runInEdtAndWait(PlatformTestUtil::dispatchAllInvocationEventsInIdeEventQueue);
                assertEquals("Late success/error/cancel must not update the released document", loading, document.getText());
                assertNull(fieldValue(form, "currentMarkdownText", String.class));
                assertNull(invoke(form, "captureSingleExport", new Class<?>[0]));
                assertEquals(before, editorsForProject());
            } finally {
                release.countDown();
                Disposer.dispose(serviceOwner);
            }
        }
    }

    private PreviewForm createListForm() throws Exception {
        if (listFixtureClass == null) {
            listFixtureClass = myFixture.addClass("""
                    package preview;
                    @org.apache.dubbo.config.annotation.DubboService
                    public interface ListTaskFixture { void request(); }
                    """);
        }
        return createForm(listFixtureClass);
    }

    private void replaceListService(Disposable owner, Supplier<List<DocView>> result) {
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), DubboDocViewServiceImpl.class,
                new DubboDocViewServiceImpl() {
                    @Override
                    public List<DocView> buildDoc(PsiClass ownerClass, PsiMethod method) {
                        assertFalse("List generation must run off EDT", ApplicationManager.getApplication().isDispatchThread());
                        assertTrue("The service must be called under a read action", ApplicationManager.getApplication().isReadAccessAllowed());
                        return result.get();
                    }
                }, owner);
    }

    public void testCurrentCancellationReplacesProgressAndStaleCallbacksCannotReplaceNewState() throws Exception {
        PreviewForm form = createForm();
        EdtTestUtil.runInEdtAndWait(() -> initializeUi(form));
        DocView selected = new DocView("CurrentDoc");
        selected.setPsiClass(fixtureClass);

        EdtTestUtil.runInEdtAndWait(() -> {
            setField(form, "currentDocView", selected);
            fieldValue(form, "renderRequest", AtomicLong.class).set(2L);
            setDocumentText(form, DocViewBundle.message("ui.preview.rendering"));
            invoke(form, "handleRenderCancellation", new Class<?>[]{long.class}, 2L);
        });

        String cancellation = documentText(form);
        assertFalse("Cancellation must replace the visible rendering state",
                DocViewBundle.message("ui.preview.rendering").equals(cancellation));
        assertNull("Cancellation must not expose Markdown", fieldValue(form, "currentMarkdownText", String.class));

        EdtTestUtil.runInEdtAndWait(() -> {
            setField(form, "currentMarkdownText", "# Latest document\n");
            setDocumentText(form, "# Latest document\n");
            fieldValue(form, "renderRequest", AtomicLong.class).set(4L);
            invoke(form, "handleRenderFailure", new Class<?>[]{long.class, Throwable.class},
                    3L, new IllegalStateException("stale failure"));
        });

        assertEquals("A late error from an older request must not overwrite the selected document",
                "# Latest document\n", documentText(form));
        assertEquals("# Latest document\n", fieldValue(form, "currentMarkdownText", String.class));

        EdtTestUtil.runInEdtAndWait(() ->
                invoke(form, "handleRenderCancellation", new Class<?>[]{long.class}, 3L));
        assertEquals("A late cancellation from an older request must not clear the selected document",
                "# Latest document\n", documentText(form));
        assertEquals("# Latest document\n", fieldValue(form, "currentMarkdownText", String.class));
        assertSame("A stale cancellation must retain the current document identity", selected,
                fieldValue(form, "currentDocView", DocView.class));
    }

    public void testStaleSuccessfulRenderAndClosedFailureCannotChangePreview() throws Exception {
        PreviewForm form = createForm();
        EdtTestUtil.runInEdtAndWait(() -> initializeUi(form));
        DocView older = new DocView("OlderDoc");
        older.setPsiClass(fixtureClass);
        DocView latest = new DocView("LatestDoc");
        latest.setPsiClass(fixtureClass);

        EdtTestUtil.runInEdtAndWait(() -> {
            setField(form, "currentDocView", latest);
            fieldValue(form, "renderRequest", AtomicLong.class).set(4L);
            setField(form, "currentMarkdownText", "# Latest document\n");
            setDocumentText(form, "# Latest document\n");
            invoke(form, "applyMarkdown", new Class<?>[]{DocView.class, String.class, String.class, long.class},
                    older, "# Stale document\n", "<p>Stale document</p>", 3L);
        });
        assertEquals("# Latest document\n", documentText(form));

        EdtTestUtil.runInEdtAndWait(() -> {
            setField(form, "currentDocView", latest);
            fieldValue(form, "renderRequest", AtomicLong.class).set(5L);
            invoke(form, "dispose", new Class<?>[0]);
            invoke(form, "handleRenderFailure", new Class<?>[]{long.class, Throwable.class},
                    5L, new IllegalStateException("late failure"));
        });
        assertEquals("# Latest document\n", documentText(form));
    }

    public void testExportSnapshotsPreserveExactDataWhenPreviewFieldsChange() throws Exception {
        PreviewForm form = createForm();
        EdtTestUtil.runInEdtAndWait(() -> initializeUi(form));

        DocView current = new DocView("业务文档-A");
        current.setPsiClass(fixtureClass);
        DocView another = new DocView("业务文档-B");
        another.setPsiClass(fixtureClass);
        List<DocView> sourceList = new ArrayList<>(List.of(current, another));
        EdtTestUtil.runInEdtAndWait(() -> {
            setField(form, "currentDocView", current);
            setField(form, "currentMarkdownText", "# 业务文档-A\n正文\n");
            setField(form, "docViewList", sourceList);
        });

        Object singleExport = invoke(form, "captureSingleExport", new Class<?>[0]);
        Object allExport = invoke(form, "captureAllExport", new Class<?>[0]);

        sourceList.clear();
        EdtTestUtil.runInEdtAndWait(() -> {
            setField(form, "currentDocView", null);
            setField(form, "currentMarkdownText", null);
            setField(form, "docViewList", null);
        });

        assertEquals("业务文档-A", recordComponent(singleExport, "fileName"));
        assertEquals("# 业务文档-A\n正文\n", recordComponent(singleExport, "markdownText"));
        assertEquals(fixtureClass.getName(), recordComponent(allExport, "className"));
        assertEquals(List.of(current, another), recordComponent(allExport, "docViews"));
    }

    public void testSpringRenderOnBackgroundAndApplyOnDispatchThread() throws Exception {
        PsiClass springClass = myFixture.addClass("""
                package preview;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class PreviewSpringController {
                    @GetMapping("/spring-preview")
                    public void springEndpoint() {
                    }
                }
                """);
        DocView springDoc = buildMethodDoc(springClass, "springEndpoint");
        assertEquals(FrameworkEnum.SPRING, springDoc.getType());
        String springExpected = markdownText(springDoc);
        assertTrue("The generated Spring contract should retain the real method name", springExpected.contains("springEndpoint"));
        assertTrue("The generated Spring contract should retain the mapped path", springExpected.contains("/spring-preview"));

        PreviewForm form = createForm(springClass);
        EdtTestUtil.runInEdtAndWait(() -> initializeUi(form));
        AtomicReference<Thread> dispatchThread = new AtomicReference<>();
        EdtTestUtil.runInEdtAndWait(() -> dispatchThread.set(Thread.currentThread()));
        renderAndAssert(form, springDoc, springExpected, dispatchThread.get());
    }

    public void testDubboRenderOnBackgroundAndApplyOnDispatchThread() throws Exception {
        PsiClass dubboClass = myFixture.addClass("""
                package preview;

                @org.apache.dubbo.config.annotation.DubboService
                public interface PreviewDubboService {
                    void dubboEndpoint();
                }
                """);
        DocView dubboDoc = buildMethodDoc(dubboClass, "dubboEndpoint");
        assertEquals(FrameworkEnum.DUBBO, dubboDoc.getType());
        String dubboExpected = markdownText(dubboDoc);
        assertTrue("The generated Dubbo contract should retain the real method name", dubboExpected.contains("dubboEndpoint"));
        assertTrue("The generated Dubbo contract should retain the service path", dubboExpected.contains("PreviewDubboService#dubboEndpoint"));

        PreviewForm form = createForm(dubboClass);
        EdtTestUtil.runInEdtAndWait(() -> initializeUi(form));
        AtomicReference<Thread> dispatchThread = new AtomicReference<>();
        EdtTestUtil.runInEdtAndWait(() -> dispatchThread.set(Thread.currentThread()));
        renderAndAssert(form, dubboDoc, dubboExpected, dispatchThread.get());
    }

    private Set<Editor> editorsForProject() {
        Set<Editor> editors = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Editor editor : editorFactory.getAllEditors()) {
            if (project.equals(editor.getProject())) {
                editors.add(editor);
            }
        }
        return editors;
    }

    private PreviewForm createForm() throws Exception {
        return createForm(fixtureClass);
    }

    private PreviewForm createForm(PsiClass ownerClass) throws Exception {
        AtomicReference<PreviewForm> formReference = new AtomicReference<>();
        EdtTestUtil.runInEdtAndWait(() -> formReference.set(new PreviewForm(ownerClass, null)));
        PreviewForm form = formReference.get();
        forms.add(form);
        return form;
    }

    private DocView buildMethodDoc(PsiClass targetClass, String methodName) {
        return ApplicationManager.getApplication().runReadAction((Computable<DocView>) () -> {
            PsiMethod[] methods = targetClass.findMethodsByName(methodName, false);
            if (methods.length != 1) {
                throw new AssertionError("Expected one fixture method named " + methodName);
            }
            return DocViewService.getInstance(project, targetClass).buildClassMethodDoc(targetClass, methods[0]);
        });
    }

    private String markdownText(DocView docView) {
        return ApplicationManager.getApplication().runReadAction(
                (Computable<String>) () -> DocViewData.markdownText(project, docView));
    }

    private void renderAndAssert(PreviewForm form, DocView sourceDoc, String expectedMarkdown, Thread dispatchThread)
            throws Exception {
        AtomicReference<Thread> generationThread = new AtomicReference<>();
        AtomicReference<Thread> applyThread = new AtomicReference<>();
        DocView observedDoc = observingTypeThread(sourceDoc, generationThread);
        Document document = fieldValue(form, "markdownDocument", Document.class);
        DocumentListener listener = new DocumentListener() {
            @Override
            public void beforeDocumentChange(DocumentEvent event) {
            }

            @Override
            public void documentChanged(DocumentEvent event) {
                if (expectedMarkdown.equals(event.getDocument().getText())) {
                    applyThread.set(Thread.currentThread());
                }
            }
        };
        EdtTestUtil.runInEdtAndWait(() -> document.addDocumentListener(listener));
        try {
            EdtTestUtil.runInEdtAndWait(() -> {
                setField(form, "currentDocView", observedDoc);
                invoke(form, "renderMarkdown", new Class<?>[]{DocView.class}, observedDoc);
            });
            awaitMarkdown(form, expectedMarkdown);
            assertEquals(expectedMarkdown, documentText(form));
            AtomicReference<String> generatedMarkdown = new AtomicReference<>();
            EdtTestUtil.runInEdtAndWait(() -> generatedMarkdown.set(fieldValue(form, "currentMarkdownText", String.class)));
            assertEquals("Preview Markdown must match the existing DocView contract output", expectedMarkdown, generatedMarkdown.get());
            assertNotNull("The real DocView template must be evaluated by the background render request", generationThread.get());
            assertNotSame("The DocView template must not be evaluated on the dispatch thread",
                    dispatchThread, generationThread.get());
            assertSame("The generated Markdown must be applied on the dispatch thread", dispatchThread, applyThread.get());
        } finally {
            EdtTestUtil.runInEdtAndWait(() -> document.removeDocumentListener(listener));
        }
    }

    private static DocView observingTypeThread(DocView source, AtomicReference<Thread> observedThread) {
        ThreadObservingDocView observed = new ThreadObservingDocView(observedThread);
        observed.setPsiClass(source.getPsiClass());
        observed.setPsiMethod(source.getPsiMethod());
        observed.setDocTitle(source.getDocTitle());
        observed.setClassTags(source.getClassTags());
        observed.setTags(source.getTags());
        observed.setName(source.getName());
        observed.setDesc(source.getDesc());
        observed.setDomain(source.getDomain());
        observed.setPath(source.getPath());
        observed.setMethod(source.getMethod());
        observed.setChangeLog(source.getChangeLog());
        observed.setHeaderList(source.getHeaderList());
        observed.setReqBody(source.getReqBody());
        observed.setRespBody(source.getRespBody());
        observed.setReqParamList(source.getReqParamList());
        observed.setReqBodyExample(source.getReqBodyExample());
        observed.setReqFormExample(source.getReqFormExample());
        observed.setContentType(source.getContentType());
        observed.setRespExample(source.getRespExample());
        observed.setRemark(source.getRemark());
        observed.setType(source.getType());
        return observed;
    }

    private void awaitMarkdown(PreviewForm form, String expectedMarkdown) throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        String actual = documentText(form);
        while (!expectedMarkdown.equals(actual) && System.nanoTime() < deadline) {
            Thread.sleep(20);
            actual = documentText(form);
        }
        assertEquals("The asynchronous preview should finish with the generated Markdown", expectedMarkdown, actual);
    }

    private static final class ThreadObservingDocView extends DocView {
        private final AtomicReference<Thread> observedThread;

        private ThreadObservingDocView(AtomicReference<Thread> observedThread) {
            this.observedThread = observedThread;
        }

        @Override
        public FrameworkEnum getType() {
            observedThread.set(Thread.currentThread());
            return super.getType();
        }
    }

    private boolean isEditorAlive(Editor expectedEditor) {
        for (Editor editor : editorFactory.getAllEditors()) {
            if (editor == expectedEditor) {
                return !editor.isDisposed();
            }
        }
        return false;
    }

    private void releaseCreatedEditors() {
        for (Editor editor : editorFactory.getAllEditors()) {
            if (project.equals(editor.getProject()) && !editorsBefore.contains(editor) && !editor.isDisposed()) {
                editorFactory.releaseEditor(editor);
            }
        }
    }

    private static String documentText(PreviewForm form) throws Exception {
        AtomicReference<String> text = new AtomicReference<>();
        EdtTestUtil.runInEdtAndWait(() -> text.set(fieldValue(form, "markdownDocument", Document.class).getText()));
        return text.get();
    }

    private static Object invoke(Object target, String name, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Method method = PreviewForm.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method.invoke(target, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not invoke preview behavior " + name, exception);
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = PreviewForm.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not set preview test state " + name, exception);
        }
    }

    private void setDocumentText(PreviewForm form, String text) {
        Document document = fieldValue(form, "markdownDocument", Document.class);
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(
                project, () -> document.setText(text));
    }

    private static void initializeUi(PreviewForm form) {
        invoke(form, "initializeUi", new Class<?>[0]);
    }

    private static <T> T fieldValue(Object target, String name, Class<T> type) {
        try {
            Field field = PreviewForm.class.getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(target));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not read preview state " + name, exception);
        }
    }

    private static Object recordComponent(Object record, String name) {
        try {
            Method accessor = record.getClass().getDeclaredMethod(name);
            accessor.setAccessible(true);
            return accessor.invoke(record);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not read export snapshot component " + name, exception);
        }
    }
}
