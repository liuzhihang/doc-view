package com.liuzhihang.doc.view.service.export;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.psi.PsiClass;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.testsupport.LocalProjectJavaSources;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ProjectDocumentMarkdownTest extends LightJavaCodeInsightFixtureTestCase {

    private String originalSpringTemplate;
    private String originalDubboTemplate;
    private PsiClass springController;
    private PsiClass dubboService;
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
        springController = sources.addClass("projectdocs/golden/GoldenController.java", """
                package projectdocs.golden;

                @org.springframework.web.bind.annotation.RestController
                public class GoldenController {
                    @org.springframework.web.bind.annotation.GetMapping("/golden")
                    public String getGolden() {
                        return "golden";
                    }
                }
                """);
        dubboService = sources.addClass("projectdocs/golden/GoldenService.java", """
                package projectdocs.golden;

                @org.apache.dubbo.config.annotation.DubboService
                public interface GoldenService {
                    String findGolden(String key);
                }
                """);

        TemplateSettings templates = TemplateSettings.getInstance(getProject());
        originalSpringTemplate = templates.getSpringTemplate();
        originalDubboTemplate = templates.getDubboTemplate();
        templates.setSpringTemplate("SPRING|${DocView.name}|${DocView.path}|${DocView.method}\n");
        templates.setDubboTemplate("DUBBO|${DocView.name}|${DocView.type}\n");
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            TemplateSettings templates = TemplateSettings.getInstance(getProject());
            templates.setSpringTemplate(originalSpringTemplate);
            templates.setDubboTemplate(originalDubboTemplate);
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

    public void testSnapshotRendersSpringAndDubboPagesWithCurrentTemplateVariables() throws Exception {
        ProjectDocumentSnapshot snapshot = ReadAction.computeBlocking(() ->
                ProjectDocumentSnapshotService.getInstance(getProject())
                        .createSnapshot(new EmptyProgressIndicator()));

        ProjectDocumentPage spring = page(snapshot, "projectdocs.golden.GoldenController", "getGolden");
        ProjectDocumentPage dubbo = page(snapshot, "projectdocs.golden.GoldenService", "findGolden");
        assertNotNull(spring);
        assertNotNull(dubbo);
        assertEquals(golden("project-export-spring.md"), spring.markdown());
        assertEquals(golden("project-export-dubbo.md"), dubbo.markdown());
        assertEquals("src/projectdocs/golden/GoldenController.java", spring.identity().sourcePath());
        assertEquals("src/projectdocs/golden/GoldenService.java", dubbo.identity().sourcePath());
        assertTrue(springController.getContainingFile().getVirtualFile().isInLocalFileSystem());
        assertTrue(dubboService.getContainingFile().getVirtualFile().isInLocalFileSystem());
    }


    public void testClassDescriptionChangeChangesMarkdownHashWithoutChangingMethodDescription() {
        TemplateSettings.getInstance(getProject()).setSpringTemplate("${DocView.classDesc}|${DocView.desc}");
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            springController = com.intellij.psi.JavaPsiFacade.getInstance(getProject()).findClass("projectdocs.golden.GoldenController", com.intellij.psi.search.GlobalSearchScope.projectScope(getProject()));
            assertNotNull(springController);
            var factory = com.intellij.psi.JavaPsiFacade.getElementFactory(getProject());
            springController.addBefore(factory.createDocCommentFromText("/** Class title\n * Original class description */"), springController.getFirstChild());
        });
        ProjectDocumentSnapshot before = ReadAction.computeBlocking(() -> ProjectDocumentSnapshotService.getInstance(getProject()).createSnapshot(new EmptyProgressIndicator()));
        ProjectDocumentPage original = page(before, "projectdocs.golden.GoldenController", "getGolden");
        assertEquals("Original class description|", original.markdown());
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            springController = com.intellij.psi.JavaPsiFacade.getInstance(getProject()).findClass("projectdocs.golden.GoldenController", com.intellij.psi.search.GlobalSearchScope.projectScope(getProject()));
            assertNotNull(springController);
            var factory = com.intellij.psi.JavaPsiFacade.getElementFactory(getProject());
            springController.getDocComment().replace(factory.createDocCommentFromText("/** Class title\n * Updated class description */"));
        });
        ProjectDocumentSnapshot after = ReadAction.computeBlocking(() -> ProjectDocumentSnapshotService.getInstance(getProject()).createSnapshot(new EmptyProgressIndicator()));
        ProjectDocumentPage changed = page(after, "projectdocs.golden.GoldenController", "getGolden");
        assertEquals("Updated class description|", changed.markdown());
        assertFalse(com.liuzhihang.doc.view.service.document.ProjectDocumentFormat.sha256(original.markdown().getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .equals(com.liuzhihang.doc.view.service.document.ProjectDocumentFormat.sha256(changed.markdown().getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        assertEquals(original.data().description(), changed.data().description());
        assertEquals(original.data().identity(), changed.data().identity());
        assertFalse(original.data().source().capturedContentSha256().equals(changed.data().source().capturedContentSha256()));
    }

    private static ProjectDocumentPage page(ProjectDocumentSnapshot snapshot, String className, String methodName) {
        return snapshot.pages().stream()
                .filter(candidate -> candidate.identity().qualifiedClassName().equals(className))
                .filter(candidate -> candidate.identity().methodName().equals(methodName))
                .findFirst()
                .orElse(null);
    }

    private static String golden(String name) throws Exception {
        return Files.readString(Path.of("src/test/testData/contracts/markdown", name).toAbsolutePath());
    }
}
