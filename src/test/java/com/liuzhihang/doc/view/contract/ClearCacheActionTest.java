package com.liuzhihang.doc.view.contract;

import com.intellij.ide.extensionResources.ExtensionsRootType;
import com.intellij.ide.scratch.ScratchFileService.Option;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.project.TestProjectManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.action.toolbar.window.WindowClearAction;
import com.liuzhihang.doc.view.action.toolbar.window.catalog.CatalogClearAction;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.DtoSchemaCacheService;
import com.liuzhihang.doc.view.service.DtoSchemaCacheService.DtoSchemaCacheKey;
import com.liuzhihang.doc.view.service.export.ProjectDocumentExporter;
import com.liuzhihang.doc.view.service.export.ProjectDocumentIdentity;
import com.liuzhihang.doc.view.service.export.ProjectDocumentPage;
import com.liuzhihang.doc.view.service.export.ProjectDocumentFixtures;
import com.liuzhihang.doc.view.ui.window.DocViewNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public final class ClearCacheActionTest extends LightJavaCodeInsightFixtureTestCase {

    private final List<ProjectScratchFixture> projectScratchFiles = new ArrayList<>();
    private final List<Path> exportedArtifacts = new ArrayList<>();
    private final List<Project> temporaryProjects = new ArrayList<>();
    private final List<Path> temporaryProjectRoots = new ArrayList<>();

    public void testClearActionDescriptorsDescribeTheSameOperation() throws Exception {
        try (InputStream descriptor = getClass().getClassLoader().getResourceAsStream("META-INF/plugin.xml")) {
            assertNotNull("The plugin descriptor must be available to the test", descriptor);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(descriptor);

            Element toolbarAction = findAction(document, "liuzhihang.doc.tool.window.toolbar.action.clear");
            Element catalogAction = findAction(document, "liuzhihang.doc.tool.window.catalog.action.clear");
            assertEquals("Clear Cache", toolbarAction.getAttribute("text"));
            assertEquals(toolbarAction.getAttribute("text"), catalogAction.getAttribute("text"));
            assertEquals(toolbarAction.getAttribute("description"), catalogAction.getAttribute("description"));
            assertFalse(DocViewBundle.message("action.clear.cache").isBlank());
            assertFalse(DocViewBundle.message("action.clear.cache.description").isBlank());

            Element catalogGroup = findDescriptor(document, "group", "liuzhihang.doc.tool.window.catalog.action");
            assertFalse("The preserved catalog ID must not add Clear Cache back to the context menu",
                    catalogAction.getParentNode().isSameNode(catalogGroup));
            assertEquals("com.liuzhihang.doc.view.action.toolbar.window.WindowClearAction",
                    toolbarAction.getAttribute("class"));
            assertEquals(toolbarAction.getAttribute("class"), catalogAction.getAttribute("class"));

            ActionManager actionManager = ActionManager.getInstance();
            AnAction toolbarRegistered = actionManager.getAction("liuzhihang.doc.tool.window.toolbar.action.clear");
            AnAction legacyRegistered = actionManager.getAction("liuzhihang.doc.tool.window.catalog.action.clear");
            assertNotNull("The toolbar Clear Cache action must be discoverable by its retained action ID",
                    toolbarRegistered);
            assertNotNull("The legacy catalog Clear Cache action ID must remain available in Find Action",
                    legacyRegistered);
            assertEquals(toolbarRegistered.getTemplatePresentation().getText(),
                    legacyRegistered.getTemplatePresentation().getText());
            assertEquals(toolbarRegistered.getTemplatePresentation().getDescription(),
                    legacyRegistered.getTemplatePresentation().getDescription());
            assertEquals(DocViewBundle.message("action.liuzhihang.doc.tool.window.toolbar.action.clear.text"),
                    legacyRegistered.getTemplatePresentation().getText());
            assertEquals(DocViewBundle.message("action.liuzhihang.doc.tool.window.toolbar.action.clear.description"),
                    legacyRegistered.getTemplatePresentation().getDescription());
        }
    }

    public void testWindowActionClearsCacheWithoutDeletingScratchOrProjectFiles() throws Exception {
        VirtualFile projectFile = addUnrelatedProjectFile();
        byte[] projectBytes = projectFile.contentsToByteArray();
        VirtualFile scratchFile = createOpenScratchFile("window-action.md", "手写内容：保留窗口动作文件");
        byte[] scratchBytes = scratchFile.contentsToByteArray();
        DtoSchemaCacheService cache = DtoSchemaCacheService.getInstance(getProject());
        DtoSchemaCacheKey cacheKey = seedCache(cache, "window-action");

        perform(new WindowClearAction(), null);

        assertNull("The action must clear the active project's DTO schema cache", cache.getBody(cacheKey));
        assertPreserved(scratchFile, scratchBytes);
        assertPreserved(projectFile, projectBytes);
    }

    public void testCatalogActionPreservesSelectedScratchDocumentAndStillClearsCache() throws Exception {
        VirtualFile projectFile = addUnrelatedProjectFile();
        byte[] projectBytes = projectFile.contentsToByteArray();
        String scratchPath = "Doc View/ClearCacheActionTest-" + UUID.randomUUID() + "/catalog-selected.md";
        VirtualFile scratchFile = createOpenScratchFileAtPath(scratchPath, "手写内容：保留目录动作文件");
        byte[] scratchBytes = scratchFile.contentsToByteArray();
        DtoSchemaCacheService cache = DtoSchemaCacheService.getInstance(getProject());
        DtoSchemaCacheKey cacheKey = seedCache(cache, "catalog-selected-action");
        DocViewNode selectedNode = new TestDocViewNode(scratchPath);
        SimpleTree tree = new SelectedNodeTree(selectedNode);

        perform(new CatalogClearAction(), tree);

        assertNull("The action must clear the active project's DTO schema cache", cache.getBody(cacheKey));
        assertSame("Clearing cache must not replace the directory selection", selectedNode, tree.getSelectedNode());
        assertPreserved(scratchFile, scratchBytes);
        assertPreserved(projectFile, projectBytes);
    }

    public void testCatalogActionWithNoSelectionStillClearsCacheAndPreservesFiles() throws Exception {
        VirtualFile projectFile = addUnrelatedProjectFile();
        byte[] projectBytes = projectFile.contentsToByteArray();
        VirtualFile scratchFile = createOpenScratchFile("catalog-no-selection.md", "手写内容：无选择时保留");
        byte[] scratchBytes = scratchFile.contentsToByteArray();
        DtoSchemaCacheService cache = DtoSchemaCacheService.getInstance(getProject());
        DtoSchemaCacheKey cacheKey = seedCache(cache, "catalog-empty-selection");

        perform(new CatalogClearAction(), new SelectedNodeTree(null));

        assertNull("An empty selection must not prevent cache clearing", cache.getBody(cacheKey));
        assertPreserved(scratchFile, scratchBytes);
        assertPreserved(projectFile, projectBytes);
    }

    public void testWindowActionIsProjectScopedAndPreservesDraftsAndExportedDocuments() throws Exception {
        Project projectA = createRealProject("ClearCacheActionProjectA");
        Project projectB = createRealProject("ClearCacheActionProjectB");
        assertNotSame("The A/B isolation fixture must use distinct real projects", projectA, projectB);
        assertNotNull("Project A must provide its own local root", projectA.getBasePath());
        assertTrue("The active A project's local root must exist before exporting",
                Files.isDirectory(Path.of(projectA.getBasePath())));
        assertEquals("Project A must use the dedicated temporary root created for this test",
                temporaryProjectRoots.get(0).toRealPath(), Path.of(projectA.getBasePath()).toRealPath());

        VirtualFile scratchMarkdown = createOpenScratchFile(projectA, "handwritten.md", "# 手写 Scratch\n不要删除");
        byte[] scratchMarkdownBytes = scratchMarkdown.contentsToByteArray();
        String originalHttpDraft = "GET https://example.invalid/api/items\nAccept: application/json\n";
        VirtualFile httpDraft = createOpenScratchFile(projectA, "request.http", originalHttpDraft);
        byte[] httpDraftBytes = httpDraft.contentsToByteArray();
        com.intellij.openapi.editor.Document httpDocument = FileDocumentManager.getInstance().getDocument(httpDraft);
        assertNotNull("The open HTTP Scratch file must have an editor Document", httpDocument);
        String unsavedHttpDraft = "GET https://example.invalid/api/items?draft=true\nX-User-Draft: keep\n";
        WriteCommandAction.writeCommandAction(projectA).withName("Edit unsaved HTTP draft")
                .compute(() -> {
                    httpDocument.setText(unsavedHttpDraft);
                    return null;
                });
        assertTrue("The HTTP draft fixture must be unsaved before Clear Cache",
                FileDocumentManager.getInstance().isFileModified(httpDraft));

        Path[] exported = createExportedMarkdownArtifacts(projectA);
        byte[] exportedIndexBytes = Files.readAllBytes(exported[0]);
        byte[] exportedPageBytes = Files.readAllBytes(exported[1]);

        DtoSchemaCacheService cacheA = DtoSchemaCacheService.getInstance(projectA);
        DtoSchemaCacheService cacheB = DtoSchemaCacheService.getInstance(projectB);
        assertNotSame("Each actual Project must own a different schema cache service", cacheA, cacheB);
        DtoSchemaCacheKey keyA = seedCache(cacheA, "shared-type-identity", "project-a-schema");
        DtoSchemaCacheKey keyB = seedCache(cacheB, "shared-type-identity", "project-b-schema");
        assertEquals("The A/B fixture must use the same logical cache key", keyA.typeIdentity(), keyB.typeIdentity());
        assertEquals("project-b-schema", cacheB.getBody(keyB).getName());

        perform(new WindowClearAction(), null, projectA);

        assertNull("Clear Cache from the A window must clear A's schema cache", cacheA.getBody(keyA));
        assertNotNull("Clear Cache from the A window must leave B's schema cache populated", cacheB.getBody(keyB));
        assertEquals("B's cached schema must remain unchanged", "project-b-schema", cacheB.getBody(keyB).getName());
        assertPreserved(projectA, scratchMarkdown, scratchMarkdownBytes);
        assertPreserved(projectA, httpDraft, httpDraftBytes);
        assertEquals("Clear Cache must preserve the current unsaved HTTP Document text",
                unsavedHttpDraft, httpDocument.getText());
        assertTrue("Clear Cache must not mark the HTTP draft saved",
                FileDocumentManager.getInstance().isFileModified(httpDraft));
        assertTrue("The HTTP draft editor must remain open", FileEditorManager.getInstance(projectA).isFileOpen(httpDraft));
        assertTrue("Clear Cache must preserve the exported project index bytes",
                Arrays.equals(exportedIndexBytes, Files.readAllBytes(exported[0])));
        assertTrue("Clear Cache must preserve the exported API page bytes",
                Arrays.equals(exportedPageBytes, Files.readAllBytes(exported[1])));
    }

    @Override
    protected void tearDown() throws Exception {
        Throwable cleanupFailure = null;
        cleanupFailure = attemptCleanup(cleanupFailure, "project Scratch files", this::cleanupProjectScratchFiles);
        cleanupFailure = attemptCleanup(cleanupFailure, "exported artifacts", this::cleanupExportedArtifacts);
        cleanupFailure = attemptCleanup(cleanupFailure, "temporary projects", this::closeTemporaryProjects);
        cleanupFailure = attemptCleanup(cleanupFailure, "temporary project roots", this::deleteTemporaryProjectRoots);
        cleanupFailure = attemptCleanup(cleanupFailure, "Light fixture", super::tearDown);
        if (cleanupFailure != null) {
            if (cleanupFailure instanceof Exception exception) {
                throw exception;
            }
            if (cleanupFailure instanceof Error error) {
                throw error;
            }
        }
    }

    private VirtualFile addUnrelatedProjectFile() {
        PsiFile file = myFixture.addFileToProject("clear-cache-safety/keep.md", "# 项目文件保留");
        return file.getVirtualFile();
    }

    private VirtualFile createOpenScratchFile(String fileName, String text) throws Exception {
        String path = "Doc View/ClearCacheActionTest-" + UUID.randomUUID() + "/" + fileName;
        return createOpenScratchFileAtPath(path, text);
    }

    private VirtualFile createOpenScratchFile(Project project, String fileName, String text) throws Exception {
        String path = "Doc View/ClearCacheActionTest-" + UUID.randomUUID() + "/" + fileName;
        VirtualFile file = createScratchFile(project, path, text);
        projectScratchFiles.add(new ProjectScratchFixture(project, file));
        return openScratchFile(project, file);
    }

    private VirtualFile createOpenScratchFileAtPath(String path, String text) throws Exception {
        VirtualFile file = createScratchFile(getProject(), path, text);
        projectScratchFiles.add(new ProjectScratchFixture(getProject(), file));
        return openScratchFile(getProject(), file);
    }

    private VirtualFile createScratchFile(Project project, String path, String text) throws Exception {
        return WriteCommandAction.writeCommandAction(project)
                .withName("Create Clear Cache scratch fixture")
                .compute(() -> {
                    VirtualFile created = ExtensionsRootType.getInstance()
                            .findFile(project, path, Option.create_if_missing);
                    VfsUtil.saveText(created, text);
                    return created;
                });
    }

    private VirtualFile openScratchFile(Project project, VirtualFile file) {
        ApplicationManager.getApplication().invokeAndWait(() ->
                FileEditorManager.getInstance(project).openFile(file, true));
        assertTrue("The synthetic Extensions document must open in an editor",
                FileEditorManager.getInstance(project).isFileOpen(file));
        return file;
    }

    private DtoSchemaCacheKey seedCache(DtoSchemaCacheService cache, String typeIdentity) {
        return seedCache(cache, typeIdentity, "cached-schema");
    }

    private DtoSchemaCacheKey seedCache(DtoSchemaCacheService cache, String typeIdentity, String bodyName) {
        DtoSchemaCacheKey key = cache.key("response", "clear-cache-safety." + typeIdentity);
        Body body = new Body();
        body.setName(bodyName);
        cache.putBody(key, body);
        assertNotNull("The fixture must contain a readable cache entry before invoking the action", cache.getBody(key));
        return key;
    }

    private void perform(AnAction action, SimpleTree tree) {
        perform(action, tree, getProject());
    }

    private void perform(AnAction action, SimpleTree tree, Project project) {
        DataContext context = dataId -> {
            if (CommonDataKeys.PROJECT.is(dataId)) {
                return project;
            }
            if (tree != null && DocViewDataKeys.WINDOW_CATALOG_TREE.is(dataId)) {
                return tree;
            }
            return null;
        };
        AnActionEvent event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, context);
        ApplicationManager.getApplication().invokeAndWait(() -> action.actionPerformed(event));
    }

    private Project createRealProject(String projectName) throws IOException {
        Path projectRoot = Files.createTempDirectory("doc-view-" + projectName + "-");
        temporaryProjectRoots.add(projectRoot);
        Project[] projectRef = new Project[1];
        ApplicationManager.getApplication().invokeAndWait(() -> {
            TestProjectManager manager = (TestProjectManager) ProjectManager.getInstance();
            Project project = manager.newProject(
                    projectRoot.resolve(projectName + ".ipr"), getProjectDescriptor().getOpenProjectOptions());
            assertNotNull("The temporary real project must be created", project);
            temporaryProjects.add(project);
            projectRef[0] = project;
            assertTrue("The temporary real project must open", manager.openProject(project));
        });
        assertNotNull("The temporary real project must be created", projectRef[0]);
        assertNotNull("The temporary real project must provide its base path", projectRef[0].getBasePath());
        assertTrue("The temporary real project must use its own existing root",
                Files.isDirectory(Path.of(projectRef[0].getBasePath())));
        assertEquals("The project base path must point into its dedicated temporary root",
                projectRoot.toRealPath(), Path.of(projectRef[0].getBasePath()).toRealPath());
        return projectRef[0];
    }

    private void cleanupProjectScratchFiles() throws Exception {
        if (projectScratchFiles.isEmpty()) {
            return;
        }
        Throwable cleanupFailure = null;
        for (ProjectScratchFixture fixture : projectScratchFiles) {
            try {
                cleanupProjectScratchFile(fixture);
            } catch (Exception | Error failure) {
                cleanupFailure = appendCleanupFailure(cleanupFailure, failure);
            }
        }
        throwCleanupFailure(cleanupFailure);
    }

    private void cleanupProjectScratchFile(ProjectScratchFixture fixture) throws Exception {
        Project project = fixture.project();
        VirtualFile file = fixture.file();
        if (project.isDisposed()) {
            return;
        }
        WriteCommandAction.writeCommandAction(project).withName("Restore Clear Cache draft fixture")
                .compute(() -> {
                    FileDocumentManager documents = FileDocumentManager.getInstance();
                    if (file.isValid() && documents.isFileModified(file)) {
                        com.intellij.openapi.editor.Document document = documents.getCachedDocument(file);
                        if (document != null) {
                            documents.reloadFromDisk(document);
                        }
                    }
                    return null;
                });
        ApplicationManager.getApplication().invokeAndWait(() -> {
            if (file.isValid() && FileEditorManager.getInstance(project).isFileOpen(file)) {
                FileEditorManager.getInstance(project).closeFile(file);
            }
        });
        WriteCommandAction.writeCommandAction(project).withName("Delete Clear Cache project Scratch fixture")
                .compute(() -> {
                    VirtualFile parent = file.getParent();
                    if (file.isValid()) {
                        file.delete(project);
                    }
                    if (parent != null && parent.getName().startsWith("ClearCacheActionTest-")) {
                        deleteEmptyFixtureParents(project, parent);
                    }
                    return null;
                });
    }

    private void closeTemporaryProjects() throws Exception {
        Throwable cleanupFailure = null;
        for (int index = temporaryProjects.size() - 1; index >= 0; index--) {
            Project project = temporaryProjects.get(index);
            if (project.isDisposed()) {
                continue;
            }
            try {
                ApplicationManager.getApplication().invokeAndWait(() ->
                        PlatformTestUtil.forceCloseProjectWithoutSaving(project));
            } catch (Exception | Error failure) {
                cleanupFailure = appendCleanupFailure(cleanupFailure, failure);
            }
        }
        throwCleanupFailure(cleanupFailure);
    }

    private void deleteTemporaryProjectRoots() throws Exception {
        Throwable cleanupFailure = null;
        for (int index = temporaryProjectRoots.size() - 1; index >= 0; index--) {
            Path projectRoot = temporaryProjectRoots.get(index);
            if (!Files.exists(projectRoot)) {
                continue;
            }
            try (Stream<Path> paths = Files.walk(projectRoot)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception | Error failure) {
                        cleanupFailure = appendCleanupFailure(cleanupFailure, failure);
                    }
                }
            } catch (Exception | Error failure) {
                cleanupFailure = appendCleanupFailure(cleanupFailure, failure);
            }
        }
        throwCleanupFailure(cleanupFailure);
    }

    private Throwable attemptCleanup(Throwable previousFailure, String cleanupName, CleanupAction cleanup) {
        try {
            cleanup.run();
        } catch (Exception | Error failure) {
            previousFailure = appendCleanupFailure(previousFailure,
                    new Exception("Failed to clean " + cleanupName, failure));
        }
        return previousFailure;
    }

    private Throwable appendCleanupFailure(Throwable previousFailure, Throwable failure) {
        if (previousFailure == null) {
            return failure;
        }
        previousFailure.addSuppressed(failure);
        return previousFailure;
    }

    private void throwCleanupFailure(Throwable failure) throws Exception {
        if (failure instanceof Exception exception) {
            throw exception;
        }
        if (failure instanceof Error error) {
            throw error;
        }
    }

    private Path[] createExportedMarkdownArtifacts(Project project) throws IOException {
        String basePath = project.getBasePath();
        assertNotNull("The real project fixture must have a local project root", basePath);
        Path projectRoot = Path.of(basePath);
        assertTrue("The active project's local root must exist before export", Files.isDirectory(projectRoot));
        Path outputRoot = projectRoot.resolve("docs/doc-view");
        assertFalse("The temporary fixture must not contain unrelated export output", Files.exists(outputRoot));
        ProjectDocumentIdentity identity = ProjectDocumentIdentity.of("clear-cache-test",
                "src/main/java/clearcache/ClearCacheController.java", "clearcache.ClearCacheController",
                "find", List.of("java.lang.String"));
        ProjectDocumentPage page = ProjectDocumentFixtures.page(identity, "SPRING", "# Exported page\n\nUser content.\n");
        Path index = outputRoot.resolve("index.md");
        Path exportedPage = outputRoot.resolve("apis").resolve(identity.pageFileName());
        exportedArtifacts.add(index);
        exportedArtifacts.add(exportedPage);

        new ProjectDocumentExporter().export(projectRoot, ProjectDocumentFixtures.snapshot(List.of(page)));
        exportedArtifacts.add(outputRoot.resolve("index.json"));
        exportedArtifacts.add(outputRoot.resolve(".doc-view.lock"));
        exportedArtifacts.add(outputRoot.resolve("apis").resolve(identity.pageFileName().replace(".md", ".json")));
        assertTrue("The export fixture must create the project index", Files.isRegularFile(index));
        assertTrue("The export fixture must create an API Markdown page", Files.isRegularFile(exportedPage));
        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(index);
        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(exportedPage);
        return new Path[]{index, exportedPage};
    }

    private void cleanupExportedArtifacts() throws IOException {
        for (Path artifact : exportedArtifacts) {
            Files.deleteIfExists(artifact);
        }
        if (exportedArtifacts.isEmpty()) {
            return;
        }
        Path outputRoot = exportedArtifacts.get(0).getParent();
        Path apiDirectory = outputRoot == null ? null : outputRoot.resolve("apis");
        if (apiDirectory != null && Files.isDirectory(apiDirectory)) {
            try (var children = Files.list(apiDirectory)) {
                if (children.findAny().isEmpty()) {
                    Files.deleteIfExists(apiDirectory);
                }
            }
        }
        if (outputRoot != null && Files.isDirectory(outputRoot)) {
            try (var children = Files.list(outputRoot)) {
                if (children.findAny().isEmpty()) {
                    Files.deleteIfExists(outputRoot);
                }
            }
        }
        Path docs = outputRoot == null ? null : outputRoot.getParent();
        if (docs != null && Files.isDirectory(docs)) {
            try (var children = Files.list(docs)) {
                if (children.findAny().isEmpty()) {
                    Files.deleteIfExists(docs);
                }
            }
        }
        Path projectRoot = docs == null ? null : docs.getParent();
        if (projectRoot != null) {
            VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(projectRoot);
            if (root != null) {
                root.refresh(false, true, null);
            }
        }
    }

    private void assertPreserved(VirtualFile file, byte[] expectedBytes) throws IOException {
        assertPreserved(getProject(), file, expectedBytes);
    }

    private void assertPreserved(Project project, VirtualFile file, byte[] expectedBytes) throws IOException {
        assertTrue("The file must remain valid: " + file.getPath(), file.isValid());
        assertTrue("The file bytes must remain unchanged: " + file.getPath(),
                Arrays.equals(expectedBytes, file.contentsToByteArray()));
        if (ExtensionsRootType.getInstance().containsFile(file)) {
            assertTrue("The Scratch/Extensions editor must remain open: " + file.getPath(),
                    FileEditorManager.getInstance(project).isFileOpen(file));
        }
    }

    private void deleteEmptyFixtureParents(VirtualFile parent) throws IOException {
        deleteEmptyFixtureParents(getProject(), parent);
    }

    private void deleteEmptyFixtureParents(Project project, VirtualFile parent) throws IOException {
        while (parent != null && parent.isValid() && parent.isDirectory()
                && parent.getName().startsWith("ClearCacheActionTest-") && parent.getChildren().length == 0) {
            VirtualFile next = parent.getParent();
            parent.delete(project);
            parent = next;
        }
    }

    private Element findAction(Document document, String actionId) {
        return findDescriptor(document, "action", actionId);
    }

    private Element findDescriptor(Document document, String tagName, String descriptorId) {
        NodeList descriptors = document.getElementsByTagName(tagName);
        for (int index = 0; index < descriptors.getLength(); index++) {
            Element descriptor = (Element) descriptors.item(index);
            if (descriptorId.equals(descriptor.getAttribute("id"))) {
                return descriptor;
            }
        }
        fail("Missing " + tagName + " descriptor: " + descriptorId);
        throw new AssertionError("Unreachable");
    }

    private static final class SelectedNodeTree extends SimpleTree {
        private final SimpleNode selectedNode;

        private SelectedNodeTree(SimpleNode selectedNode) {
            this.selectedNode = selectedNode;
        }

        @Override
        public SimpleNode getSelectedNode() {
            return selectedNode;
        }
    }

    private static final class TestDocViewNode extends DocViewNode {
        private final String documentPath;

        private TestDocViewNode(String documentPath) {
            super(null);
            this.documentPath = documentPath;
        }

        @Override
        protected SimpleNode[] buildChildren() {
            return new SimpleNode[0];
        }

        @Override
        public List<DocView> docViewList() {
            return List.of();
        }

        @Override
        public void updateNode(Project project) {
        }

        @Override
        public String docPath(Project project) {
            return documentPath;
        }

        @Override
        public String httpPath(Project project) {
            return documentPath.replace(".md", ".http");
        }
    }

    private record ProjectScratchFixture(Project project, VirtualFile file) {
    }

    @FunctionalInterface
    private interface CleanupAction {
        void run() throws Exception;
    }
}
