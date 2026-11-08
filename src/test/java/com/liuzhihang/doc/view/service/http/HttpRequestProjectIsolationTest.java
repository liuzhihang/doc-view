package com.liuzhihang.doc.view.service.http;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.RunAll;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.action.toolbar.window.catalog.CatalogHttpClientAction;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;
import com.liuzhihang.doc.view.ui.window.MethodNode;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public final class HttpRequestProjectIsolationTest extends UsefulTestCase {
    private HttpProjectFixtureSupport projectA;
    private HttpProjectFixtureSupport projectB;
    private Disposable listenerDisposable;
    private String previousAsyncProperty;
    private final List<VirtualFile> generatedFiles = new CopyOnWriteArrayList<>();
    private final List<HttpRequestService> services = new CopyOnWriteArrayList<>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        previousAsyncProperty = System.getProperty("intellij.progress.task.ignoreHeadless");
        System.setProperty("intellij.progress.task.ignoreHeadless", "true");

        try {
            projectA = HttpProjectFixtureSupport.create("HttpRequestProjectA");
            AnnotationStubs.installSpringWeb(projectA.fixture());
            projectA.renameModule("shared-module");

            listenerDisposable = Disposer.newDisposable("http-project-isolation-listener");
            VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
                @Override
                public void fileCreated(VirtualFileEvent event) {
                    VirtualFile file = event.getFile();
                    if ("http".equals(file.getExtension())
                            && file.getPath().replace('\\', '/').contains("doc-view-http/")) {
                        generatedFiles.add(file);
                    }
                }
            }, listenerDisposable);
        } catch (Exception | Error failure) {
            try {
                RunAll.runAll(this::closeFixtures, this::disposeListener, this::restoreAsyncProperty,
                        super::tearDown);
            } catch (Throwable cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    @Override
    protected void tearDown() throws Exception {
        RunAll.runAll(this::disposeServices, PlatformTestUtil::waitForAllBackgroundActivityToCalmDown,
                this::removeGeneratedFiles, this::closeFixtures, this::disposeListener, this::restoreAsyncProperty,
                super::tearDown);
    }

    public void testIdenticalModuleAndMethodIdentityUsesDifferentProjectScratchPaths() throws Exception {
        PsiMethod methodA = sharedMethod(projectA.fixture());
        Project firstProject = projectA.project();
        var firstModule = projectA.fixture().getModule();
        String moduleNameA = firstModule.getName();
        String projectAName = firstProject.getName();
        String relativePathA = relativeSourcePath(projectA, methodA);
        String classNameA = methodA.getContainingClass().getQualifiedName();
        String signatureA = signature(methodA);

        HttpRequestService serviceA = installService(projectA, new HttpRequestBuilder());
        start(serviceA, methodA);
        await(() -> serviceA.activeRequestCount() == 0);
        assertEquals("The first project should publish exactly one scratch draft", 1, generatedFiles.size());
        VirtualFile draftA = generatedFiles.getFirst();
        String textA = read(draftA);
        assertTrue(textA, textA.contains("# Project: " + projectAName));
        assertTrue(textA, textA.contains("SharedController.lookup()"));
        // Generation completion precedes the platform asynchronous editor-open result.
        await(() -> isOpenIn(firstProject, draftA));
        assertTrue("Project A should open its generated request in its editor manager",
                isOpenIn(firstProject, draftA));
        assertOnlyOpenHttpDraft(firstProject, draftA);

        // HeavyIdeaTestFixture tracks the process-wide editor multicaster. Close A before
        // creating B so the first project's BookmarkManager listener cannot look like a leak
        // introduced by the second fixture.
        closeProjectA();

        projectB = HttpProjectFixtureSupport.create("HttpRequestProjectB");
        AnnotationStubs.installSpringWeb(projectB.fixture());
        projectB.renameModule("shared-module");
        PsiMethod methodB = sharedMethod(projectB.fixture());
        Project secondProject = projectB.project();
        var secondModule = projectB.fixture().getModule();

        assertNotSame("The fixtures must be backed by separate real Project instances", firstProject, secondProject);
        assertNotSame(firstModule, secondModule);
        assertEquals("Both fixtures must intentionally use the same module identity",
                moduleNameA, secondModule.getName());
        assertEquals("Both source files must have the same project-relative location",
                relativePathA, relativeSourcePath(projectB, methodB));
        assertEquals(classNameA, methodB.getContainingClass().getQualifiedName());
        assertEquals(signatureA, signature(methodB));

        HttpRequestService serviceB = installService(projectB, new HttpRequestBuilder());
        start(serviceB, methodB);
        await(() -> serviceB.activeRequestCount() == 0);
        assertEquals("The second project should publish its own scratch draft", 2, generatedFiles.size());
        VirtualFile draftB = generatedFiles.getLast();

        assertEquals("Identical method identity should keep the same request filename",
                draftA.getName(), draftB.getName());
        assertFalse("Project-specific scratch paths must not collide", draftA.getPath().equals(draftB.getPath()));
        assertFalse("The project digest must isolate the request roots",
                draftA.getParent().getPath().equals(draftB.getParent().getPath()));
        String textB = read(draftB);
        assertTrue(textB, textB.contains("# Project: " + secondProject.getName()));
        assertTrue(textB, textB.contains("SharedController.lookup()"));
        await(() -> isOpenIn(secondProject, draftB));
        assertTrue("Project B should open only its generated request in its editor manager",
                isOpenIn(secondProject, draftB));
        assertFalse("Project B must not open Project A's request draft",
                isOpenIn(secondProject, draftA));
        assertOnlyOpenHttpDraft(secondProject, draftB);
    }

    public void testCatalogActionUsesSelectedMethodDespiteAnotherEditorAndLaterSelectionChange() throws Exception {
        JavaCodeInsightTestFixture fixture = projectA.fixture();
        PsiClass selectedClass = fixture.addClass("""
                package contract.httpselection;
                @org.springframework.web.bind.annotation.RestController
                public class SelectedController {
                    @org.springframework.web.bind.annotation.GetMapping("/selected")
                    public String selected() { return "selected"; }
                }
                """);
        PsiClass editorClass = fixture.addClass("""
                package contract.httpselection;
                @org.springframework.web.bind.annotation.RestController
                public class EditorController {
                    @org.springframework.web.bind.annotation.GetMapping("/editor")
                    public String editor() { return "editor"; }
                }
                """);
        PsiMethod selectedMethod = onlyMethod(selectedClass, "selected");
        PsiMethod editorMethod = onlyMethod(editorClass, "editor");
        TestMethodNode selectedNode = new TestMethodNode(selectedClass, selectedMethod);
        TestMethodNode editorNode = new TestMethodNode(editorClass, editorMethod);
        MutableSelectedNodeTree tree = new MutableSelectedNodeTree(selectedNode);

        VirtualFile editorFile = editorMethod.getContainingFile().getVirtualFile();
        AtomicReference<Editor> currentEditorRef = new AtomicReference<>();
        EdtTestUtil.runInEdtAndWait(() -> {
            fixture.openFileInEditor(editorFile);
            VirtualFile[] selectedEditorFiles = FileEditorManager.getInstance(projectA.project()).getSelectedFiles();
            assertTrue("The current editor must show a file other than the selected catalog method",
                    Arrays.stream(selectedEditorFiles).anyMatch(file -> file.equals(editorFile)));
            currentEditorRef.set(FileEditorManager.getInstance(projectA.project()).getSelectedTextEditor());
        });
        Editor currentEditor = currentEditorRef.get();
        assertNotNull("The fixture should expose the other method as the current text editor", currentEditor);

        BlockingBuilder builder = new BlockingBuilder();
        HttpRequestService service = installService(projectA, builder);
        CatalogHttpClientAction action = new CatalogHttpClientAction();
        DataContext context = dataId -> {
            if (CommonDataKeys.PROJECT.is(dataId)) return projectA.project();
            if (CommonDataKeys.VIRTUAL_FILE.is(dataId)) return editorFile;
            if (CommonDataKeys.EDITOR.is(dataId)) return currentEditor;
            if (DocViewDataKeys.WINDOW_CATALOG_TREE.is(dataId)) return tree;
            return null;
        };
        AnActionEvent event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.POPUP, context);

        EdtTestUtil.runInEdtAndWait(() -> action.actionPerformed(event));
        try {
            assertTrue("The selected method should reach the background builder", builder.started.await(10, TimeUnit.SECONDS));
            assertSame("The action must capture the catalog method, not the editor method",
                    selectedMethod, builder.target.get());
            EdtTestUtil.runInEdtAndWait(() -> tree.setSelectedNode(editorNode));
            assertSame(editorNode, tree.getSelectedNode());
        } finally {
            builder.release.countDown();
        }

        await(() -> service.activeRequestCount() == 0);
        assertEquals("Changing selection after triggering must not publish a second target", 1, generatedFiles.size());
        String request = read(generatedFiles.getFirst());
        assertTrue(request, request.contains("SelectedController.selected()"));
        assertTrue(request, request.contains("GET {{host}}/selected"));
        assertFalse("The editor method must not leak into the generated request", request.contains("/editor"));
        assertFalse("The editor method must not replace the captured source identity",
                request.contains("EditorController.editor()"));
    }

    private PsiMethod sharedMethod(JavaCodeInsightTestFixture fixture) {
        PsiClass owner = fixture.addClass("""
                package contract.projectisolation;
                @org.springframework.web.bind.annotation.RestController
                public class SharedController {
                    @org.springframework.web.bind.annotation.GetMapping("/shared")
                    public String lookup() { return "shared"; }
                }
                """);
        return onlyMethod(owner, "lookup");
    }

    private static PsiMethod onlyMethod(PsiClass owner, String name) {
        PsiMethod[] methods = owner.findMethodsByName(name, false);
        assertEquals("The fixture should contain one method named " + name, 1, methods.length);
        return methods[0];
    }

    private String relativeSourcePath(HttpProjectFixtureSupport fixture, PsiMethod method) {
        String basePath = fixture.project().getBasePath();
        assertNotNull("A directory-based project must have a base path", basePath);
        String normalizedBase = basePath.replace('\\', '/') + "/";
        String normalizedFile = method.getContainingFile().getVirtualFile().getPath().replace('\\', '/');
        assertTrue("Fixture source should be inside its project root", normalizedFile.startsWith(normalizedBase));
        return normalizedFile.substring(normalizedBase.length());
    }

    private static String signature(PsiMethod method) {
        return method.getName() + "(" + Arrays.stream(method.getParameterList().getParameters())
                .map(parameter -> parameter.getType().getCanonicalText()).collect(Collectors.joining(", ")) + ")";
    }

    private HttpRequestService installService(HttpProjectFixtureSupport fixture, HttpRequestBuilder builder) {
        HttpRequestService service = fixture.installHttpService(builder);
        services.add(service);
        return service;
    }

    private void start(HttpRequestService service, PsiMethod method) {
        EdtTestUtil.runInEdtAndWait(() -> service.generate(method));
    }

    private void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            EdtTestUtil.runInEdtAndWait(PlatformTestUtil::dispatchAllEventsInIdeEventQueue);
            Thread.sleep(20);
        }
        assertTrue("Background request should finish", condition.getAsBoolean());
    }

    private static String read(VirtualFile file) throws IOException {
        return new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
    }

    private boolean isOpenIn(Project project, VirtualFile file) {
        AtomicReference<Boolean> open = new AtomicReference<>(false);
        EdtTestUtil.runInEdtAndWait(() -> open.set(Arrays.stream(FileEditorManager.getInstance(project).getOpenFiles())
                .anyMatch(candidate -> candidate.equals(file))));
        return open.get();
    }

    private void assertOnlyOpenHttpDraft(Project project, VirtualFile expected) {
        AtomicReference<List<VirtualFile>> openHttpFiles = new AtomicReference<>(List.of());
        EdtTestUtil.runInEdtAndWait(() -> openHttpFiles.set(Arrays.stream(
                        FileEditorManager.getInstance(project).getOpenFiles())
                .filter(file -> "http".equals(file.getExtension())).toList()));
        assertEquals("Each project should have exactly its own generated HTTP draft open",
                List.of(expected), openHttpFiles.get());
    }

    private void closeProjectA() throws Exception {
        if (projectA == null) {
            return;
        }
        try {
            projectA.close();
        } finally {
            projectA = null;
        }
    }

    private void removeGeneratedFiles() throws Exception {
        EdtTestUtil.runInEdtAndWait(() -> ApplicationManager.getApplication().runWriteAction(() -> {
            for (HttpProjectFixtureSupport fixture : new HttpProjectFixtureSupport[]{projectA, projectB}) {
                if (fixture == null || fixture.project().isDisposed()) continue;
                FileEditorManager manager = FileEditorManager.getInstance(fixture.project());
                for (VirtualFile file : generatedFiles) manager.closeFile(file);
            }
            for (VirtualFile file : generatedFiles) {
                if (!file.isValid()) continue;
                try {
                    file.setWritable(true);
                    file.delete(this);
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            }
        }));
    }

    private void closeFixtures() throws Exception {
        RunAll.runAll(this::closeProjectB, this::closeProjectA);
    }

    private void disposeServices() throws Exception {
        try {
            RunAll.runAll(services, HttpRequestService::dispose);
        } finally {
            services.clear();
        }
    }

    private void closeProjectB() throws Exception {
        if (projectB != null) {
            try {
                projectB.close();
            } finally {
                projectB = null;
            }
        }
    }

    private void disposeListener() {
        if (listenerDisposable != null) {
            Disposer.dispose(listenerDisposable);
            listenerDisposable = null;
        }
    }

    private void restoreAsyncProperty() {
        if (previousAsyncProperty == null) System.clearProperty("intellij.progress.task.ignoreHeadless");
        else System.setProperty("intellij.progress.task.ignoreHeadless", previousAsyncProperty);
    }

    private static final class MutableSelectedNodeTree extends SimpleTree {
        private volatile SimpleNode selectedNode;

        private MutableSelectedNodeTree(SimpleNode selectedNode) {
            this.selectedNode = selectedNode;
        }

        @Override
        public SimpleNode getSelectedNode() {
            return selectedNode;
        }

        private void setSelectedNode(SimpleNode selectedNode) {
            this.selectedNode = selectedNode;
        }
    }

    private static final class TestMethodNode extends MethodNode {
        private TestMethodNode(PsiClass owner, PsiMethod method) {
            super(null, owner, method);
        }
    }

    private static final class BlockingBuilder extends HttpRequestBuilder {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicReference<PsiMethod> target = new AtomicReference<>();

        @Override
        public HttpRequestSnapshot build(PsiMethod method) throws HttpRequestException {
            target.set(method);
            started.countDown();
            try {
                while (!release.await(20, TimeUnit.MILLISECONDS)) {
                    ProgressManager.checkCanceled();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ProcessCanceledException();
            }
            ProgressManager.checkCanceled();
            return super.build(method);
        }
    }

}
