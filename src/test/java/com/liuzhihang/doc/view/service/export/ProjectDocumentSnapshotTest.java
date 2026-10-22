package com.liuzhihang.doc.view.service.export;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiClass;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.testsupport.LocalProjectJavaSources;

import java.nio.file.Path;
import java.util.List;

public final class ProjectDocumentSnapshotTest extends LightJavaCodeInsightFixtureTestCase {

    private PsiClass controller;
    private PsiClass feignClient;
    private PsiClass dubboService;
    private Boolean originalIncludeNormalInterface;
    private LocalProjectJavaSources sources;

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
        controller = sources.addClass("projectdocs/spring/UserController.java", """
                package projectdocs.spring;

                @org.springframework.web.bind.annotation.RestController
                @org.springframework.web.bind.annotation.RequestMapping("/api")
                public class UserController {
                    @org.springframework.web.bind.annotation.GetMapping("/users")
                    public String findUser(@org.springframework.web.bind.annotation.RequestParam("id") String id) {
                        return id;
                    }
                }
                """);
        feignClient = sources.addClass("projectdocs/feign/UserClient.java", """
                package projectdocs.feign;

                @org.springframework.cloud.openfeign.FeignClient
                public interface UserClient {
                    @org.springframework.web.bind.annotation.GetMapping("/client-users")
                    String fetchUsers();
                }
                """);
        dubboService = sources.addClass("projectdocs/dubbo/UserService.java", """
                package projectdocs.dubbo;

                @org.apache.dubbo.config.annotation.DubboService
                public interface UserService {
                    String lookup(String id);
                    String lookup(long id);
                }
                """);
        sources.addClass("projectdocs/plain/MappedContract.java", """
                package projectdocs.plain;

                public interface MappedContract {
                    @org.springframework.web.bind.annotation.GetMapping("/plain")
                    String invoke();
                }
                """);
        sources.addClass("projectdocs/plain/Marker.java", """
                package projectdocs.plain;

                public @interface Marker {
                    String value();
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

    public void testProjectScanKeepsFrameworkCandidatesAndOptionalInterfaces() {
        Settings settings = Settings.getInstance(getProject());
        settings.setIncludeNormalInterface(false);

        ProjectDocumentSnapshot defaultSnapshot = ReadAction.computeBlocking(() ->
                ProjectDocumentSnapshotService.getInstance(getProject())
                        .createSnapshot(new EmptyProgressIndicator()));

        assertEquals(4, defaultSnapshot.pages().size());
        ProjectDocumentPage spring = page(defaultSnapshot, "projectdocs.spring.UserController", "findUser", List.of("java.lang.String"));
        ProjectDocumentPage feign = page(defaultSnapshot, "projectdocs.feign.UserClient", "fetchUsers", List.of());
        ProjectDocumentPage dubboString = page(defaultSnapshot, "projectdocs.dubbo.UserService", "lookup", List.of("java.lang.String"));
        ProjectDocumentPage dubboLong = page(defaultSnapshot, "projectdocs.dubbo.UserService", "lookup", List.of("long"));
        assertNotNull(spring);
        assertNotNull(feign);
        assertNotNull(dubboString);
        assertNotNull(dubboLong);
        assertEquals("SPRING", spring.framework());
        assertEquals("FEIGN", feign.framework());
        assertEquals("DUBBO", dubboString.framework());
        assertEquals("DUBBO", dubboLong.framework());
        assertFalse(spring.identity().pageFileName().equals(dubboString.identity().pageFileName()));
        assertFalse(dubboString.identity().pageFileName().equals(dubboLong.identity().pageFileName()));
        assertSourcePath(spring, controller);
        assertSourcePath(feign, feignClient);
        assertSourcePath(dubboString, dubboService);

        settings.setIncludeNormalInterface(true);
        ProjectDocumentSnapshot withNormalInterfaces = ReadAction.computeBlocking(() ->
                ProjectDocumentSnapshotService.getInstance(getProject())
                        .createSnapshot(new EmptyProgressIndicator()));

        assertEquals(5, withNormalInterfaces.pages().size());
        ProjectDocumentPage normalInterface = page(withNormalInterfaces, "projectdocs.plain.MappedContract", "invoke", List.of());
        assertNotNull(normalInterface);
        assertEquals("INTERFACE", normalInterface.framework());
        assertEquals("DUBBO", dubboPage(withNormalInterfaces).framework());
        assertNull("Annotation types are not API interfaces",
                page(withNormalInterfaces, "projectdocs.plain.Marker", "value", List.of()));
    }

    public void testCanceledSnapshotStopsBeforeReturningPartialPages() {
        EmptyProgressIndicator indicator = new EmptyProgressIndicator();
        indicator.cancel();
        try {
            ReadAction.computeBlocking(() -> ProjectDocumentSnapshotService.getInstance(getProject()).createSnapshot(indicator));
            fail("Expected cancellation before a partial snapshot is returned");
        } catch (ProcessCanceledException expected) {
            // The snapshot contract returns a complete immutable snapshot or cancellation.
        }
    }

    private ProjectDocumentPage dubboPage(ProjectDocumentSnapshot snapshot) {
        return page(snapshot, "projectdocs.dubbo.UserService", "lookup", List.of("java.lang.String"));
    }

    private static ProjectDocumentPage page(ProjectDocumentSnapshot snapshot,
                                            String className,
                                            String methodName,
                                            List<String> parameterTypes) {
        return snapshot.pages().stream()
                .filter(candidate -> candidate.identity().qualifiedClassName().equals(className))
                .filter(candidate -> candidate.identity().methodName().equals(methodName))
                .filter(candidate -> candidate.identity().parameterTypes().equals(parameterTypes))
                .findFirst()
                .orElse(null);
    }

    private void assertSourcePath(ProjectDocumentPage page, PsiClass psiClass) {
        assertNotNull("The fixture project needs a local root for an export snapshot", getProject().getBasePath());
        Path root = Path.of(getProject().getBasePath()).toAbsolutePath().normalize();
        assertTrue("Snapshot PSI must come from LocalFileSystem",
                psiClass.getContainingFile().getVirtualFile().isInLocalFileSystem());
        Path source = Path.of(psiClass.getContainingFile().getVirtualFile().getPath()).toAbsolutePath().normalize();
        assertEquals(root.relativize(source).toString().replace('\\', '/'), page.identity().sourcePath());
    }
}
