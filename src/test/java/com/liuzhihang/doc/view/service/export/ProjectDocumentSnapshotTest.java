package com.liuzhihang.doc.view.service.export;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.DocViewBuildInfo;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.service.document.ProjectDocumentFormat;
import com.liuzhihang.doc.view.testsupport.LocalProjectJavaSources;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
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
        assertEquals(DocViewBuildInfo.version(), defaultSnapshot.producerVersion());
        assertTrue(defaultSnapshot.generatorFingerprint().matches("[a-f0-9]{64}"));
        assertFalse(spring.identity().pageFileName().equals(dubboString.identity().pageFileName()));
        assertFalse(dubboString.identity().pageFileName().equals(dubboLong.identity().pageFileName()));
        assertSourcePath(spring, controller);
        assertSourcePath(feign, feignClient);
        assertSourcePath(dubboString, dubboService);
        assertProjectedSource(spring, controller);
        assertProjectedSource(feign, feignClient);
        assertProjectedSource(dubboString, dubboService);

        settings.setIncludeNormalInterface(true);
        ProjectDocumentSnapshot withNormalInterfaces = ReadAction.computeBlocking(() ->
                ProjectDocumentSnapshotService.getInstance(getProject())
                        .createSnapshot(new EmptyProgressIndicator()));

        assertEquals(5, withNormalInterfaces.pages().size());
        ProjectDocumentPage normalInterface = page(withNormalInterfaces, "projectdocs.plain.MappedContract", "invoke", List.of());
        assertNotNull(normalInterface);
        assertEquals("INTERFACE", normalInterface.framework());
        assertEquals("interface", normalInterface.data().framework());
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

    public void testCommittedUnsavedDocumentTextIsHashedWithoutSavingTheFile() throws Exception {
        var psiFile = controller.getContainingFile();
        Document document = PsiDocumentManager.getInstance(getProject()).getDocument(psiFile);
        assertNotNull(document);
        assertTrue(PsiDocumentManager.getInstance(getProject()).isCommitted(document));
        Path sourcePath = Path.of(psiFile.getVirtualFile().getPath());
        byte[] diskBefore = java.nio.file.Files.readAllBytes(sourcePath);
        String originalText = document.getText();
        String editedText = originalText + "\n// unsaved snapshot marker\n";
        try {
            ApplicationManager.getApplication().runWriteAction(() -> document.setText(editedText));
            PsiDocumentManager.getInstance(getProject()).commitDocument(document);

            ProjectDocumentSnapshot snapshot = ReadAction.computeBlocking(() ->
                    ProjectDocumentSnapshotService.getInstance(getProject())
                            .createSnapshot(new EmptyProgressIndicator()));
            ProjectDocumentPage page = page(snapshot, "projectdocs.spring.UserController", "findUser",
                    List.of("java.lang.String"));
            assertNotNull(page);
            assertTrue(snapshot.hasUnsavedDocuments());
            assertEquals(ProjectDocumentFormat.sha256(editedText.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    page.data().source().capturedContentSha256());
            assertTrue(java.util.Arrays.equals(diskBefore, java.nio.file.Files.readAllBytes(sourcePath)));
        } finally {
            ApplicationManager.getApplication().runWriteAction(() -> document.setText(originalText));
            PsiDocumentManager.getInstance(getProject()).commitDocument(document);
        }
    }

    public void testSnapshotRejectsAnUncommittedSourceDocument() {
        var psiFile = controller.getContainingFile();
        Document document = PsiDocumentManager.getInstance(getProject()).getDocument(psiFile);
        assertNotNull(document);
        String originalText = document.getText();
        try {
            ApplicationManager.getApplication().runWriteAction(() -> document.setText(originalText + "\n// pending\n"));
            assertFalse(PsiDocumentManager.getInstance(getProject()).isCommitted(document));
            try {
                ReadAction.computeBlocking(() -> ProjectDocumentSnapshotService.getInstance(getProject())
                        .createSnapshot(new EmptyProgressIndicator()));
                fail("An uncommitted source Document must not be read as a PSI snapshot");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("uncommitted"));
            }
        } finally {
            ApplicationManager.getApplication().runWriteAction(() -> document.setText(originalText));
            PsiDocumentManager.getInstance(getProject()).commitDocument(document);
        }
    }

    public void testClassesInOneSourceFileShareTheCapturedDocumentHash() throws Exception {
        PsiClass firstClass = sources.addClass("projectdocs/multi/MultiController.java", """
                package projectdocs.multi;

                @org.springframework.web.bind.annotation.RestController
                class AlphaController {
                    @org.springframework.web.bind.annotation.GetMapping("/alpha")
                    public String alpha() { return "alpha"; }
                }

                @org.springframework.web.bind.annotation.RestController
                class BetaController {
                    @org.springframework.web.bind.annotation.GetMapping("/beta")
                    public String beta() { return "beta"; }
                }
                """);
        PsiJavaFile sourceFile = (PsiJavaFile) firstClass.getContainingFile();
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());

        ProjectDocumentSnapshot snapshot = ReadAction.computeBlocking(() -> ProjectDocumentSnapshotService
                .getInstance(getProject()).createSnapshot(new EmptyProgressIndicator()));

        ProjectDocumentPage alpha = page(snapshot, "projectdocs.multi.AlphaController", "alpha", List.of());
        ProjectDocumentPage beta = page(snapshot, "projectdocs.multi.BetaController", "beta", List.of());
        assertNotNull(alpha);
        assertNotNull(beta);
        assertEquals(alpha.data().source().path(), beta.data().source().path());
        assertEquals(alpha.data().source().capturedContentSha256(), beta.data().source().capturedContentSha256());
        var document = PsiDocumentManager.getInstance(getProject()).getDocument(sourceFile);
        assertNotNull(document);
        assertEquals(ProjectDocumentFormat.sha256(document.getText().getBytes(StandardCharsets.UTF_8)),
                alpha.data().source().capturedContentSha256());
    }

    public void testUnsavedDtoDocumentParticipatingInParsingIsReportedWithoutSaving() throws Exception {
        sources.addClass("org/springframework/web/bind/annotation/PostMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface PostMapping { String[] value() default {}; }
                """);
        sources.addClass("org/springframework/web/bind/annotation/RequestBody.java", """
                package org.springframework.web.bind.annotation;
                public @interface RequestBody { boolean required() default true; }
                """);
        PsiClass dto = sources.addClass("projectdocs/dto/RequestData.java", """
                package projectdocs.dto;
                public class RequestData {
                    private String existingField;
                }
                """);
        PsiClass dtoController = sources.addClass("projectdocs/dto/RequestController.java", """
                package projectdocs.dto;
                @org.springframework.web.bind.annotation.RestController
                public class RequestController {
                    @org.springframework.web.bind.annotation.PostMapping("/requests")
                    public RequestData create(
                            @org.springframework.web.bind.annotation.RequestBody RequestData request) {
                        return request;
                    }
                }
                """);
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());
        Document dtoDocument = PsiDocumentManager.getInstance(getProject()).getDocument(dto.getContainingFile());
        assertNotNull(dtoDocument);
        String originalText = dtoDocument.getText();
        String editedText = originalText.replace("private String existingField;",
                "private String existingField;\n    private String editorOnlyField;");
        assertFalse(originalText.equals(editedText));
        Path dtoPath = Path.of(dto.getContainingFile().getVirtualFile().getPath());
        byte[] diskBefore = java.nio.file.Files.readAllBytes(dtoPath);
        try {
            ApplicationManager.getApplication().runWriteAction(() -> dtoDocument.setText(editedText));
            PsiDocumentManager.getInstance(getProject()).commitDocument(dtoDocument);

            ProjectDocumentSnapshot snapshot = ReadAction.computeBlocking(() -> ProjectDocumentSnapshotService
                    .getInstance(getProject()).createSnapshot(new EmptyProgressIndicator()));

            ProjectDocumentPage page = page(snapshot, "projectdocs.dto.RequestController", "create",
                    List.of("projectdocs.dto.RequestData"));
            assertNotNull(page);
            assertTrue("Unsaved DTO input must be included in the snapshot state", snapshot.hasUnsavedDocuments());
            assertTrue(page.data().requestBody().fields().stream()
                    .anyMatch(field -> "editorOnlyField".equals(field.name())));
            assertTrue(page.data().responseBody().fields().stream()
                    .anyMatch(field -> "editorOnlyField".equals(field.name())));
            assertTrue(java.util.Arrays.equals(diskBefore, java.nio.file.Files.readAllBytes(dtoPath)));
            assertTrue(PsiDocumentManager.getInstance(getProject()).isCommitted(dtoDocument));
        } finally {
            ApplicationManager.getApplication().runWriteAction(() -> dtoDocument.setText(originalText));
            PsiDocumentManager.getInstance(getProject()).commitDocument(dtoDocument);
        }
    }

    public void testUnsavedDubboXmlDocumentParticipatingInSnapshotIsReportedWithoutSaving() throws Exception {
        PsiClass xmlOnlyService = sources.addClass("projectdocs/dubbo/XmlOnlyUserService.java", """
                package projectdocs.dubbo;

                public interface XmlOnlyUserService {
                    String lookup(String id);
                }
                """);
        Path xmlPath = sources.projectRoot().resolve("src/dubbo-provider.xml");
        String originalXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:dubbo="http://dubbo.apache.org/schema/dubbo">
                </beans>
                """;
        String editedXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:dubbo="http://dubbo.apache.org/schema/dubbo">
                    <dubbo:service interface="projectdocs.dubbo.XmlOnlyUserService" ref="provider"/>
                </beans>
                """;
        java.nio.file.Files.writeString(xmlPath, originalXml, StandardCharsets.UTF_8);
        VirtualFile xmlVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(xmlPath);
        Document xmlDocument = xmlVirtualFile == null ? null : FileDocumentManager.getInstance().getDocument(xmlVirtualFile);
        byte[] diskBefore = java.nio.file.Files.readAllBytes(xmlPath);
        try {
            assertNotNull("The Dubbo XML configuration must be visible to the virtual file system", xmlVirtualFile);
            assertTrue("The project-local Dubbo XML must be in a project content root",
                    com.intellij.openapi.roots.ProjectFileIndex.getInstance(getProject()).isInContent(xmlVirtualFile));
            PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(xmlVirtualFile);
            assertTrue("The configuration must be parsed as XML PSI", psiFile instanceof XmlFile);
            assertNotNull("The Dubbo XML configuration must have an IDE Document", xmlDocument);
            assertFalse(FileDocumentManager.getInstance().isDocumentUnsaved(xmlDocument));

            ApplicationManager.getApplication().runWriteAction(() -> xmlDocument.setText(editedXml));
            PsiDocumentManager.getInstance(getProject()).commitDocument(xmlDocument);
            assertTrue("Committing PSI must not save the edited XML Document",
                    FileDocumentManager.getInstance().isDocumentUnsaved(xmlDocument));
            assertTrue("The XML Document used by Dubbo discovery must be committed to PSI",
                    PsiDocumentManager.getInstance(getProject()).isCommitted(xmlDocument));
            IndexingTestUtil.waitUntilIndexesAreReady(getProject());

            ProjectDocumentSnapshot snapshot = ReadAction.computeBlocking(() -> ProjectDocumentSnapshotService
                    .getInstance(getProject()).createSnapshot(new EmptyProgressIndicator()));

            ProjectDocumentPage page = page(snapshot, "projectdocs.dubbo.XmlOnlyUserService", "lookup",
                    List.of("java.lang.String"));
            assertNotNull("The snapshot must use the unsaved committed Dubbo XML declaration", page);
            assertTrue("Unsaved XML used for Dubbo discovery must be reported in the index state",
                    snapshot.hasUnsavedDocuments());
            assertTrue("Snapshot generation must not save the user's XML configuration",
                    java.util.Arrays.equals(diskBefore, java.nio.file.Files.readAllBytes(xmlPath)));
            Document serviceDocument = PsiDocumentManager.getInstance(getProject())
                    .getDocument(xmlOnlyService.getContainingFile());
            assertNotNull("The XML-only service Java file must have a Document", serviceDocument);
            assertFalse("The XML-only service Java file itself is not unsaved",
                    FileDocumentManager.getInstance().isDocumentUnsaved(serviceDocument));
        } finally {
            if (xmlDocument != null) {
                ApplicationManager.getApplication().runWriteAction(() -> xmlDocument.setText(originalXml));
                PsiDocumentManager.getInstance(getProject()).commitDocument(xmlDocument);
                FileDocumentManager.getInstance().saveDocument(xmlDocument);
            }
            java.nio.file.Files.deleteIfExists(xmlPath);
            LocalFileSystem.getInstance().refreshAndFindFileByNioFile(xmlPath);
        }
    }

    public void testSnapshotRejectsUncommittedXmlAfterEarlierCommittedUnsavedXml() throws Exception {
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(getProject());
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        psiDocumentManager.commitAllDocuments();
        fileDocumentManager.saveAllDocuments();

        Path firstPath = sources.projectRoot().resolve("src/dubbo-ordered-first.xml");
        Path secondPath = sources.projectRoot().resolve("src/dubbo-ordered-second.xml");
        String originalText = "<beans xmlns=\"http://www.springframework.org/schema/beans\"/>\n";
        String editedText = "<beans xmlns=\"http://www.springframework.org/schema/beans\">\n<!-- editor only -->\n</beans>\n";
        java.nio.file.Files.writeString(firstPath, originalText, StandardCharsets.UTF_8);
        java.nio.file.Files.writeString(secondPath, originalText, StandardCharsets.UTF_8);
        VirtualFile firstVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(firstPath);
        VirtualFile secondVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(secondPath);
        PsiFile firstPsiFile = firstVirtualFile == null ? null : PsiManager.getInstance(getProject()).findFile(firstVirtualFile);
        PsiFile secondPsiFile = secondVirtualFile == null ? null : PsiManager.getInstance(getProject()).findFile(secondVirtualFile);
        Document firstDocument = firstPsiFile == null ? null : psiDocumentManager.getDocument(firstPsiFile);
        Document secondDocument = secondPsiFile == null ? null : psiDocumentManager.getDocument(secondPsiFile);
        byte[] firstDiskBefore = java.nio.file.Files.readAllBytes(firstPath);
        byte[] secondDiskBefore = java.nio.file.Files.readAllBytes(secondPath);
        try {
            assertNotNull("The first XML input must be visible to the virtual file system", firstVirtualFile);
            assertNotNull("The second XML input must be visible to the virtual file system", secondVirtualFile);
            assertTrue("The first input must be XML PSI", firstPsiFile instanceof XmlFile);
            assertTrue("The second input must be XML PSI", secondPsiFile instanceof XmlFile);
            assertNotNull("The first XML input must have an IDE Document", firstDocument);
            assertNotNull("The second XML input must have an IDE Document", secondDocument);
            assertTrue(com.intellij.openapi.roots.ProjectFileIndex.getInstance(getProject())
                    .isInContent(firstVirtualFile));
            assertTrue(com.intellij.openapi.roots.ProjectFileIndex.getInstance(getProject())
                    .isInContent(secondVirtualFile));
            IndexingTestUtil.waitUntilIndexesAreReady(getProject());

            // Observe the actual FDM order, then recreate the pair one at a time so committing
            // the first Document cannot implicitly commit the still-untouched second Document.
            ApplicationManager.getApplication().runWriteAction(() -> {
                firstDocument.setText(editedText);
                secondDocument.setText(editedText);
            });
            Document[] dirtyBeforeCommit = fileDocumentManager.getUnsavedDocuments();
            int firstIndex = identityIndex(dirtyBeforeCommit, firstDocument);
            int secondIndex = identityIndex(dirtyBeforeCommit, secondDocument);
            assertTrue("Both XML Documents must remain in the unsaved-document list", firstIndex >= 0 && secondIndex >= 0);
            Document earlierDocument = firstIndex < secondIndex ? firstDocument : secondDocument;
            Document laterDocument = firstIndex < secondIndex ? secondDocument : firstDocument;
            restoreAndSaveXmlDocuments(psiDocumentManager, fileDocumentManager,
                    originalText, firstDocument, secondDocument);

            boolean orderedPair = prepareCommittedThenUncommittedXml(
                    earlierDocument, laterDocument, editedText, psiDocumentManager, fileDocumentManager);
            if (!orderedPair) {
                restoreAndSaveXmlDocuments(psiDocumentManager, fileDocumentManager,
                        originalText, firstDocument, secondDocument);
                orderedPair = prepareCommittedThenUncommittedXml(
                        laterDocument, earlierDocument, editedText, psiDocumentManager, fileDocumentManager);
                if (orderedPair) {
                    Document swap = earlierDocument;
                    earlierDocument = laterDocument;
                    laterDocument = swap;
                }
            }
            assertTrue("A committed unsaved XML input must precede a later uncommitted XML input in FDM order",
                    orderedPair);

            try {
                ReadAction.computeBlocking(() -> ProjectDocumentSnapshotService.getInstance(getProject())
                        .createSnapshot(new EmptyProgressIndicator()));
                fail("A later uncommitted XML input must not be hidden by an earlier committed unsaved input");
            } catch (IllegalStateException expected) {
                assertTrue("Unexpected snapshot rejection reason: " + expected.getMessage(),
                        expected.getMessage() != null && expected.getMessage().contains("uncommitted"));
            }

            assertTrue("The first XML Document must not be saved during validation",
                    java.util.Arrays.equals(firstDiskBefore, java.nio.file.Files.readAllBytes(firstPath)));
            assertTrue("The second XML Document must not be saved during validation",
                    java.util.Arrays.equals(secondDiskBefore, java.nio.file.Files.readAllBytes(secondPath)));
        } finally {
            if (firstDocument != null && secondDocument != null) {
                restoreAndSaveXmlDocuments(psiDocumentManager, fileDocumentManager,
                        originalText, firstDocument, secondDocument);
            }
            java.nio.file.Files.deleteIfExists(firstPath);
            java.nio.file.Files.deleteIfExists(secondPath);
            LocalFileSystem.getInstance().refreshAndFindFileByNioFile(firstPath);
            LocalFileSystem.getInstance().refreshAndFindFileByNioFile(secondPath);
        }
    }

    private static boolean prepareCommittedThenUncommittedXml(Document committedDocument,
                                                               Document uncommittedDocument,
                                                               String editedText,
                                                               PsiDocumentManager psiDocumentManager,
                                                               FileDocumentManager fileDocumentManager) {
        ApplicationManager.getApplication().runWriteAction(() -> committedDocument.setText(editedText));
        psiDocumentManager.commitDocument(committedDocument);
        if (!fileDocumentManager.isDocumentUnsaved(committedDocument)
                || !psiDocumentManager.isCommitted(committedDocument)) {
            return false;
        }
        ApplicationManager.getApplication().runWriteAction(() -> uncommittedDocument.setText(editedText));
        if (!fileDocumentManager.isDocumentUnsaved(uncommittedDocument)
                || psiDocumentManager.isCommitted(uncommittedDocument)) {
            return false;
        }
        Document[] unsaved = fileDocumentManager.getUnsavedDocuments();
        int committedIndex = identityIndex(unsaved, committedDocument);
        int uncommittedIndex = identityIndex(unsaved, uncommittedDocument);
        return committedIndex >= 0 && uncommittedIndex > committedIndex;
    }

    private static void restoreAndSaveXmlDocuments(PsiDocumentManager psiDocumentManager,
                                                   FileDocumentManager fileDocumentManager,
                                                   String originalText,
                                                   Document firstDocument,
                                                   Document secondDocument) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            firstDocument.setText(originalText);
            secondDocument.setText(originalText);
        });
        psiDocumentManager.commitAllDocuments();
        fileDocumentManager.saveDocument(firstDocument);
        fileDocumentManager.saveDocument(secondDocument);
    }

    private static int identityIndex(Document[] documents, Document target) {
        for (int i = 0; i < documents.length; i++) {
            if (documents[i] == target) {
                return i;
            }
        }
        return -1;
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

    private void assertProjectedSource(ProjectDocumentPage page, PsiClass psiClass) {
        assertEquals(page.identity().canonicalForm(), page.data().identity().canonicalForm());
        assertEquals(page.identity().sha256(), page.data().apiId());
        assertEquals(page.identity().sourcePath(), page.data().source().path());
        assertEquals("ide-document-text", page.data().source().basis());
        assertNotNull(page.data().source().line());
        assertTrue(page.data().source().line() > 0);
        var document = PsiDocumentManager.getInstance(getProject()).getDocument(psiClass.getContainingFile());
        assertNotNull(document);
        assertTrue(PsiDocumentManager.getInstance(getProject()).isCommitted(document));
        assertEquals(ProjectDocumentFormat.sha256(document.getText().getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                page.data().source().capturedContentSha256());
    }
}
