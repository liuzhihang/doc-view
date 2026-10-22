package com.liuzhihang.doc.view.action.toolbar.window;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.project.DumbService;
import com.intellij.testFramework.DumbModeTestUtils;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.testsupport.LocalProjectJavaSources;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public final class ProjectDocumentExportActionTest extends LightJavaCodeInsightFixtureTestCase {

    private LocalProjectJavaSources sources;
    private Boolean originalIncludeNormalInterface;

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_21;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sources = new LocalProjectJavaSources(getProject(), getModule());
        sources.installApiAnnotationStubs();
        originalIncludeNormalInterface = Settings.getInstance(getProject()).getIncludeNormalInterface();
        sources.addClass("projectdocs/action/ProjectController.java", """
                package projectdocs.action;

                @org.springframework.web.bind.annotation.RestController
                public class ProjectController {
                    @org.springframework.web.bind.annotation.GetMapping("/action-export")
                    public String find(@org.springframework.web.bind.annotation.RequestParam("id") String id) {
                        return id;
                    }
                }
                """);
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            Settings.getInstance(getProject()).setIncludeNormalInterface(originalIncludeNormalInterface);
        } finally {
            try {
                if (sources != null) {
                    sources.cleanup();
                }
            } finally {
                super.tearDown();
            }
        }
    }

    public void testActionPublishesProjectSnapshotAfterBackgroundScan() throws Exception {
        ProjectDocumentExportAction action = new ProjectDocumentExportAction();
        AnActionEvent event = event(action);
        VirtualFile projectRoot = LocalFileSystem.getInstance()
                .findFileByNioFile(Path.of(getProject().getBasePath()));
        assertNotNull("The action fixture must have a local project root", projectRoot);
        assertNull("The output directory must be absent before the first export",
                projectRoot.findChild("docs"));
        action.update(event);
        assertTrue("The action is available when the project index is ready",
                event.getPresentation().isEnabledAndVisible());

        ApplicationManager.getApplication().invokeAndWait(() -> action.actionPerformed(event));

        Path index = Path.of(getProject().getBasePath()).resolve("docs/doc-view/index.md");
        assertTrue("The action must finish its background scan and publish the project index", awaitFile(index));
        String indexText = Files.readString(index);
        assertTrue(indexText.contains("### `projectdocs.action.ProjectController`"));
        assertTrue(indexText.contains("`find("));
        assertTrue(indexText.contains("[Document](apis/ProjectController-find-"));
        Path pages = index.getParent().resolve("apis");
        try (var files = Files.list(pages)) {
            assertEquals(1L, files.filter(path -> path.getFileName().toString().endsWith(".md")).count());
        }
        assertTrue("The action must refresh its new output into the project VFS without a project-wide refresh",
                awaitProjectDocumentVfs(projectRoot));
    }

    public void testActionIsHiddenAndDoesNotExportDuringDumbMode() throws Exception {
        ProjectDocumentExportAction action = new ProjectDocumentExportAction();
        AnActionEvent event = event(action);
        DumbModeTestUtils.runInDumbModeSynchronously(getProject(), () -> {
            assertTrue(DumbService.getInstance(getProject()).isDumb());
            action.update(event);
            assertFalse(event.getPresentation().isEnabled());
            assertFalse(event.getPresentation().isVisible());
            action.actionPerformed(event);
        });

        Path index = Path.of(getProject().getBasePath()).resolve("docs/doc-view/index.md");
        assertFalse("Dumb Mode must not start a partial export", Files.exists(index));
    }

    private AnActionEvent event(ProjectDocumentExportAction action) {
        DataContext context = dataId -> CommonDataKeys.PROJECT.is(dataId) ? getProject() : null;
        return AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, context);
    }

    private static boolean awaitFile(Path file) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        Application application = ApplicationManager.getApplication();
        while (!Files.exists(file) && System.nanoTime() < deadline) {
            if (application.isDispatchThread()) {
                PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            }
            Thread.sleep(10);
        }
        return Files.exists(file);
    }

    private static boolean awaitProjectDocumentVfs(VirtualFile projectRoot) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        Application application = ApplicationManager.getApplication();
        while (System.nanoTime() < deadline) {
            VirtualFile docs = projectRoot.findChild("docs");
            VirtualFile docView = docs == null ? null : docs.findChild("doc-view");
            VirtualFile index = docView == null ? null : docView.findChild("index.md");
            VirtualFile apis = docView == null ? null : docView.findChild("apis");
            if (index != null && index.isValid() && apis != null && apis.isValid()
                    && apis.getChildren().length == 1) {
                return true;
            }
            if (application.isDispatchThread()) {
                PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            }
            Thread.sleep(10);
        }
        return false;
    }
}
