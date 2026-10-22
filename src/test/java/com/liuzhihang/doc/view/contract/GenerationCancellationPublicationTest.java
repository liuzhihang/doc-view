package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.registry.RegistryValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.jcef.JBCefApp;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.DtoSchemaCacheService;
import com.liuzhihang.doc.view.service.impl.SpringDocViewServiceImpl;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;
import com.liuzhihang.doc.view.ui.PreviewForm;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Full preview-generation cancellation coverage with real Java PSI and the production service. */
public final class GenerationCancellationPublicationTest extends LightJavaCodeInsightFixtureTestCase {

    private PreviewForm previewForm;
    private Disposable serviceOwner;
    private RegistryValue jcefEnabled;
    private RegistryValue jcefTestMode;
    private boolean originalJcefEnabled;
    private boolean originalJcefTestMode;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AnnotationStubs.installSpringWeb(myFixture);
        jcefEnabled = Registry.get("ide.browser.jcef.enabled");
        jcefTestMode = Registry.get("ide.browser.jcef.testMode.enabled");
        originalJcefEnabled = jcefEnabled.asBoolean();
        originalJcefTestMode = jcefTestMode.asBoolean();
        jcefTestMode.setValue(true);
        jcefEnabled.setValue(false);
    }

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_21;
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (previewForm != null) {
                EdtTestUtil.runInEdtAndWait(previewForm::dispose);
            }
            if (serviceOwner != null) {
                Disposer.dispose(serviceOwner);
            }
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

    public void testCancellationDuringWholeClassGenerationDoesNotPublishPartialPreview() throws Exception {
        myFixture.addClass("""
                package contract.cancellation;

                public class PreviewDetail {
                    private String first;
                    private String second;
                }
                """);
        myFixture.addClass("""
                package contract.cancellation;

                public class PreviewResponse {
                    private String id;
                    private PreviewDetail detail;
                }
                """);
        var controller = myFixture.addClass("""
                package contract.cancellation;

                @org.springframework.web.bind.annotation.RestController
                public class PreviewController {
                    @org.springframework.web.bind.annotation.GetMapping("/first")
                    public PreviewResponse first() { return null; }

                    @org.springframework.web.bind.annotation.GetMapping("/second")
                    public PreviewResponse second() { return null; }
                }
                """);

        AtomicInteger generatedMethods = new AtomicInteger();
        AtomicBoolean activeTaskCancelled = new AtomicBoolean();
        AtomicBoolean nestedResponseGenerated = new AtomicBoolean();
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), SpringDocViewServiceImpl.class,
                new SpringDocViewServiceImpl() {
                    @Override
                    public DocView buildClassMethodDoc(PsiClass psiClass, @NotNull PsiMethod psiMethod) {
                        DocView docView = super.buildClassMethodDoc(psiClass, psiMethod);
                        if (generatedMethods.incrementAndGet() == 1) {
                            nestedResponseGenerated.set(containsField(docView.getRespBody(), "detail")
                                    && containsField(docView.getRespBody(), "first"));
                            ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                            if (indicator != null) {
                                indicator.cancel();
                                activeTaskCancelled.set(indicator.isCanceled());
                            }
                        }
                        return docView;
                    }
                }, serviceOwner = Disposer.newDisposable());

        EdtTestUtil.runInEdtAndWait(() -> {
            previewForm = new PreviewForm(controller, null);
            invoke(previewForm, "initializeUi");
            invoke(previewForm, "buildDoc");
        });

        awaitPreviewStatus(DocViewBundle.message("ui.preview.load.cancelled"));

        assertEquals("The real class generator must finish one endpoint before cancellation", 1,
                generatedMethods.get());
        assertTrue("The completed endpoint must include its real nested DTO body", nestedResponseGenerated.get());
        assertTrue("Cancellation must target the active preview generation task", activeTaskCancelled.get());
        assertNull("The canceled list must never be published to the preview", fieldValue(previewForm, "docViewList"));
        assertNull("No partial document may become the current preview", fieldValue(previewForm, "currentDocView"));
        assertNull("Canceled generation must not make Markdown available for export",
                invoke(previewForm, "captureSingleExport"));
        assertNull("Canceled generation must not make an all-doc export snapshot",
                invoke(previewForm, "captureAllExport"));
        assertTrue("Canceled generation must not populate the preview Markdown cache",
                ((Map<?, ?>) fieldValue(previewForm, "markdownCache")).isEmpty());
        assertEquals("The terminal cancellation state must be visible",
                DocViewBundle.message("ui.preview.load.cancelled"),
                documentText(previewForm));
    }

    public void testCancellationInsideNestedResponseTraversalDoesNotPublishPartialPreview() throws Exception {
        myFixture.addClass("""
                package contract.cancellation;

                public class RecursivePreviewDetail {
                    private String beforeCancellation;
                    private String cancelOnlyNestedField;
                    private String afterCancellation;
                }
                """);
        myFixture.addClass("""
                package contract.cancellation;

                public class RecursivePreviewResponse {
                    private String id;
                    private RecursivePreviewDetail detail;
                }
                """);
        var controller = myFixture.addClass("""
                package contract.cancellation;

                @org.springframework.web.bind.annotation.RestController
                public class RecursivePreviewController {
                    @org.springframework.web.bind.annotation.GetMapping("/recursive")
                    public RecursivePreviewResponse recursive() { return null; }
                }
                """);

        AtomicReference<ProgressIndicator> activeIndicator = new AtomicReference<>();
        AtomicBoolean nestedFieldReached = new AtomicBoolean();
        AtomicInteger methodBuildEntries = new AtomicInteger();
        AtomicBoolean methodBuildReturned = new AtomicBoolean();
        Set<String> observedExcludeFieldNames = new HashSet<>() {
            @Override
            public boolean contains(Object value) {
                if ("cancelOnlyNestedField".equals(value)) {
                    nestedFieldReached.set(true);
                    ProgressIndicator indicator = activeIndicator.get();
                    if (indicator != null) {
                        indicator.cancel();
                    }
                }
                return super.contains(value);
            }
        };
        observedExcludeFieldNames.add("serialVersionUID");
        Settings testSettings = new Settings() {
            @Override
            public Set<String> getExcludeFieldNames() {
                return observedExcludeFieldNames;
            }
        };
        ServiceContainerUtil.replaceService(getProject(), Settings.class, testSettings, getTestRootDisposable());

        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), SpringDocViewServiceImpl.class,
                new SpringDocViewServiceImpl() {
                    @Override
                    public DocView buildClassMethodDoc(PsiClass psiClass, @NotNull PsiMethod psiMethod) {
                        methodBuildEntries.incrementAndGet();
                        activeIndicator.set(ProgressManager.getInstance().getProgressIndicator());
                        DocView docView = super.buildClassMethodDoc(psiClass, psiMethod);
                        methodBuildReturned.set(true);
                        return docView;
                    }
                }, serviceOwner = Disposer.newDisposable());

        EdtTestUtil.runInEdtAndWait(() -> {
            previewForm = new PreviewForm(controller, null);
            invoke(previewForm, "initializeUi");
            invoke(previewForm, "buildDoc");
        });

        awaitPreviewStatus(DocViewBundle.message("ui.preview.load.cancelled"));

        assertTrue("The production recursive parser must reach the uniquely named nested DTO field",
                nestedFieldReached.get());
        assertNotNull("The Spring method must run under the active preview task indicator", activeIndicator.get());
        assertTrue("Cancellation must target the active preview generation task", activeIndicator.get().isCanceled());
        assertEquals("The nested cancellation must occur inside one real endpoint build", 1,
                methodBuildEntries.get());
        assertFalse("The Spring method must not return a partially generated DocView", methodBuildReturned.get());
        assertNull("The canceled list must never be published to the preview", fieldValue(previewForm, "docViewList"));
        assertNull("No partial document may become the current preview", fieldValue(previewForm, "currentDocView"));
        assertNull("Canceled generation must not make Markdown available for export",
                invoke(previewForm, "captureSingleExport"));
        assertNull("Canceled generation must not make an all-doc export snapshot",
                invoke(previewForm, "captureAllExport"));
        assertTrue("Canceled generation must not populate the preview Markdown cache",
                ((Map<?, ?>) fieldValue(previewForm, "markdownCache")).isEmpty());
        assertEquals("The terminal cancellation state must be visible",
                DocViewBundle.message("ui.preview.load.cancelled"),
                documentText(previewForm));

        PsiMethod endpoint = controller.findMethodsByName("recursive", false)[0];
        PsiType responseType = endpoint.getReturnType();
        DtoSchemaCacheService cache = DtoSchemaCacheService.getInstance(getProject());
        DtoSchemaCacheService.DtoSchemaCacheKey responseKey = cache.key("spring-response-body",
                responseType.getCanonicalText());
        assertNull("A recursive parse canceled before returning must not cache a partial response schema",
                cache.getBody(responseKey));
    }

    private void awaitPreviewStatus(String expected) throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (!expected.equals(documentText(previewForm)) && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertEquals("The canceled production task must reach a visible terminal state", expected,
                documentText(previewForm));
    }

    private static String documentText(PreviewForm form) {
        AtomicReference<String> text = new AtomicReference<>();
        EdtTestUtil.runInEdtAndWait(() -> text.set(fieldValue(form, "markdownDocument", Document.class).getText()));
        return text.get();
    }

    private static Object invoke(PreviewForm form, String methodName) {
        try {
            Method method = PreviewForm.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(form);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not invoke preview behavior " + methodName, exception);
        }
    }

    private static Object fieldValue(PreviewForm form, String fieldName) {
        try {
            Field field = PreviewForm.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(form);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not read preview state " + fieldName, exception);
        }
    }

    private static boolean containsField(Body body, String name) {
        if (body == null) {
            return false;
        }
        if (name.equals(body.getName())) {
            return true;
        }
        return body.getChildList().stream().anyMatch(child -> containsField(child, name));
    }

    private static <T> T fieldValue(PreviewForm form, String fieldName, Class<T> type) {
        return type.cast(fieldValue(form, fieldName));
    }
}
