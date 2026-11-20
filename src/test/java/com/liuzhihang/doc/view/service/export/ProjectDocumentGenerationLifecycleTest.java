package com.liuzhihang.doc.view.service.export;

import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.action.toolbar.window.ProjectDocumentExportAction;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.service.document.ProjectDocumentIndexListener;
import com.liuzhihang.doc.view.testsupport.LocalProjectJavaSources;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import com.liuzhihang.doc.view.utils.DubboPsiUtils;
import com.liuzhihang.doc.view.utils.FeignPsiUtil;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
                        + "expected=" + success + ", notifications=" + notifications
                        + (notifications.contains(success) ? "" : "; candidates=" + docViewCandidateDiagnostic()),
                notifications.contains(success));
        assertFalse("A committed generation must not be reported as cancelled",
                notifications.contains(DocViewBundle.message("project.export.cancelled")));
    }

    private String docViewCandidateDiagnostic() {
        try {
            return ReadAction.compute(() -> {
                Map<String, String> candidates = new TreeMap<>();
                for (Module module : ModuleManager.getInstance(getProject()).getModules()) {
                    addCandidateDiagnostics(candidates, module, "DUBBO", DubboPsiUtils.findDocViewFromModule(module));
                    addCandidateDiagnostics(candidates, module, "FEIGN", FeignPsiUtil.findDocViewFromModule(module));
                    addCandidateDiagnostics(candidates, module, "SPRING", SpringPsiUtils.findDocViewFromModule(module));
                    if (Boolean.TRUE.equals(Settings.getInstance(getProject()).getIncludeNormalInterface())) {
                        AllClassesSearch.search(GlobalSearchScope.moduleScope(module), getProject()).forEach(psiClass -> {
                            if (psiClass.isInterface() && !psiClass.isAnnotationType()) {
                                addCandidateDiagnostics(candidates, module, "INTERFACE", List.of(psiClass));
                            }
                            return true;
                        });
                    }
                }
                return candidates.isEmpty() ? "<none>" : String.join("\n", candidates.values());
            });
        } catch (RuntimeException failure) {
            return "<diagnostic failed: " + failure.getClass().getSimpleName() + ": " + failure.getMessage() + ">";
        }
    }

    private void addCandidateDiagnostics(Map<String, String> diagnostics, Module module, String framework,
                                         List<PsiClass> psiClasses) {
        for (PsiClass psiClass : psiClasses) {
            VirtualFile virtualFile = psiClass.getContainingFile() == null
                    ? null : psiClass.getContainingFile().getVirtualFile();
            String path = virtualFile == null ? "<none>" : virtualFile.getPath();
            String sourcePath = projectRelativeSourcePath(path, virtualFile);
            List<String> methods = new ArrayList<>();
            for (PsiMethod method : psiClass.getMethods()) {
                if (!DocViewUtils.isDocViewMethod(method)) continue;
                List<String> parameterTypes = new ArrayList<>();
                for (PsiParameter parameter : method.getParameterList().getParameters()) {
                    PsiType type = parameter.getType();
                    parameterTypes.add(type.isValid() ? type.getCanonicalText() : "<invalid>");
                }
                methods.add(method.getName() + "(" + String.join(", ", parameterTypes) + ")");
            }
            String key = framework + "|" + module.getName() + "|" + path + "|" + psiClass.getQualifiedName();
            boolean inContent = virtualFile != null && virtualFile.isValid()
                    && ProjectFileIndex.getInstance(getProject()).isInContent(virtualFile);
            diagnostics.put(key, "framework=" + framework + ", module=" + module.getName()
                    + ", class=" + psiClass.getQualifiedName() + ", vfsPath=" + path
                    + ", vfsValid=" + (virtualFile != null && virtualFile.isValid())
                    + ", inContent=" + inContent
                    + ", sourcePath=" + sourcePath + ", docViewMethods=" + methods);
        }
    }

    private String projectRelativeSourcePath(String vfsPath, VirtualFile virtualFile) {
        if (virtualFile == null || !virtualFile.isInLocalFileSystem()) return "<not-local>";
        try {
            Path root = Path.of(getProject().getBasePath()).toRealPath();
            Path source = Path.of(vfsPath).toRealPath();
            return source.startsWith(root) ? root.relativize(source).toString() : "<outside-project>";
        } catch (IOException | RuntimeException unavailable) {
            return "<unavailable:" + unavailable.getClass().getSimpleName() + ">";
        }
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
