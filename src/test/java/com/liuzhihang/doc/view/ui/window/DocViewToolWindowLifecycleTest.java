package com.liuzhihang.doc.view.ui.window;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseListener;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.project.TestProjectManager;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class DocViewToolWindowLifecycleTest extends LightJavaCodeInsightFixtureTestCase {

    private Path otherProjectRoot;
    private Project otherProject;

    @Override
    protected void tearDown() throws Exception {
        try {
            if (otherProject != null && !otherProject.isDisposed()) {
                ApplicationManager.getApplication().invokeAndWait(() ->
                        PlatformTestUtil.forceCloseProjectWithoutSaving(otherProject));
            }
            if (otherProjectRoot != null && Files.exists(otherProjectRoot)) {
                try (var paths = Files.walk(otherProjectRoot)) {
                    paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (java.io.IOException exception) {
                            throw new java.io.UncheckedIOException(exception);
                        }
                    });
                }
            }
        } finally {
            super.tearDown();
        }
    }

    public void testProjectClosingRemovesOnlyItsWindowOnEdtWithoutCreatingContent() throws Exception {
        ToolWindowFixture fixture = new ToolWindowFixture(getProject());
        TrackingFactory factory = new TrackingFactory();
        Project otherProject = createOtherRealProject();

        try {
            EdtTestUtil.runInEdtAndWait(() -> {
                factory.init(fixture.toolWindow);

                assertEquals("Registering the close listener must not eagerly create tool window content",
                        0, factory.contentCreationCount.get());

                ApplicationManager.getApplication().getMessageBus().syncPublisher(ProjectManager.TOPIC)
                        .projectClosingBeforeSave(getProject());
                ApplicationManager.getApplication().getMessageBus().syncPublisher(ProjectCloseListener.TOPIC)
                        .projectClosing(otherProject);
                assertEquals("Before-save notifications and another project's close must not remove this window",
                        0, fixture.removeCount.get());

                ApplicationManager.getApplication().getMessageBus().syncPublisher(ProjectCloseListener.TOPIC)
                        .projectClosing(getProject());

                assertEquals("Closing the owning project must remove its tool window once", 1, fixture.removeCount.get());
                assertTrue("ToolWindow.remove() must run on EDT for the native project-close path",
                        fixture.removeCalledOnEdt.get());
                assertEquals("Closing the project must not eagerly create tool window content", 0,
                        factory.contentCreationCount.get());

                ApplicationManager.getApplication().getMessageBus().syncPublisher(ProjectCloseListener.TOPIC)
                        .projectClosing(getProject());
                assertEquals("ToolWindow.remove() must dispose the tool-window parent so repeated close delivery is harmless",
                        1, fixture.removeCount.get());
            });
        } finally {
            EdtTestUtil.runInEdtAndWait(() -> {
                if (!Disposer.isDisposed(fixture.parent)) Disposer.dispose(fixture.parent);
            });
        }
    }

    public void testDisposingToolWindowParentDisconnectsApplicationListener() {
        ToolWindowFixture fixture = new ToolWindowFixture(getProject());
        TrackingFactory factory = new TrackingFactory();

        try {
            EdtTestUtil.runInEdtAndWait(() -> {
                factory.init(fixture.toolWindow);
                Disposer.dispose(fixture.parent);

                ApplicationManager.getApplication().getMessageBus().syncPublisher(ProjectCloseListener.TOPIC)
                        .projectClosing(getProject());

                assertEquals("A tool-window-owned application listener must be disconnected with its parent",
                        0, fixture.removeCount.get());
            });
        } finally {
            EdtTestUtil.runInEdtAndWait(() -> {
                if (!Disposer.isDisposed(fixture.parent)) Disposer.dispose(fixture.parent);
            });
        }
    }

    private Project createOtherRealProject() throws Exception {
        otherProjectRoot = Files.createTempDirectory("doc-view-tool-window-lifecycle-project-");
        Project[] projectRef = new Project[1];
        ApplicationManager.getApplication().invokeAndWait(() -> {
            TestProjectManager manager = (TestProjectManager) ProjectManager.getInstance();
            Project project = manager.newProject(otherProjectRoot.resolve("other-project.ipr"),
                    getProjectDescriptor().getOpenProjectOptions());
            assertNotNull("The other real project must be created", project);
            projectRef[0] = project;
            assertTrue("The other real project must open", manager.openProject(project));
        });
        otherProject = projectRef[0];
        assertNotNull("The other real project must have a real project root", otherProject.getBasePath());
        assertTrue("The other real project's root must exist", Files.isDirectory(Path.of(otherProject.getBasePath())));
        return otherProject;
    }

    private static final class TrackingFactory extends DocViewToolWindowFactory {
        private final AtomicInteger contentCreationCount = new AtomicInteger();

        @Override
        public void createToolWindowContent(Project project, ToolWindow toolWindow) {
            contentCreationCount.incrementAndGet();
        }
    }

    private static final class ToolWindowFixture {
        private final Disposable parent = Disposer.newDisposable("doc-view-test-tool-window");
        private final AtomicInteger removeCount = new AtomicInteger();
        private final AtomicBoolean removeCalledOnEdt = new AtomicBoolean();
        private final ToolWindow toolWindow;

        private ToolWindowFixture(Project project) {
            toolWindow = (ToolWindow) Proxy.newProxyInstance(ToolWindow.class.getClassLoader(),
                    new Class<?>[]{ToolWindow.class}, (proxy, method, arguments) -> {
                        if (method.getName().equals("getProject")) return project;
                        if (method.getName().equals("getDisposable")) return parent;
                        if (method.getName().equals("remove")) {
                            removeCount.incrementAndGet();
                            removeCalledOnEdt.set(ApplicationManager.getApplication().isDispatchThread());
                            Disposer.dispose(parent);
                            return null;
                        }
                        if (method.getName().equals("toString")) return "Test Doc View tool window";
                        if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                        if (method.getName().equals("equals")) return proxy == arguments[0];
                        Class<?> returnType = method.getReturnType();
                        if (returnType == boolean.class) return false;
                        if (returnType == int.class) return 0;
                        if (returnType == long.class) return 0L;
                        if (returnType == short.class) return (short) 0;
                        if (returnType == byte.class) return (byte) 0;
                        if (returnType == char.class) return (char) 0;
                        if (returnType == float.class) return 0F;
                        if (returnType == double.class) return 0D;
                        return null;
                    });
        }
    }
}
