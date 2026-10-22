package com.liuzhihang.doc.view.contract;

import com.intellij.ide.extensionResources.ExtensionsRootType;
import com.intellij.ide.scratch.ScratchFileService.Option;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
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
import com.liuzhihang.doc.view.ui.window.DocViewNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class ClearCacheActionTest extends LightJavaCodeInsightFixtureTestCase {

    private final List<VirtualFile> scratchFiles = new ArrayList<>();

    public void testClearActionDescriptorsDescribeTheSameOperation() throws Exception {
        try (InputStream descriptor = getClass().getClassLoader().getResourceAsStream("META-INF/plugin.xml")) {
            assertNotNull("The plugin descriptor must be available to the test", descriptor);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(descriptor);

            Element toolbarAction = findAction(document, "liuzhihang.doc.tool.window.toolbar.action.clear");
            Element catalogAction = findAction(document, "liuzhihang.doc.tool.window.catalog.action.clear");
            assertEquals(DocViewBundle.message("action.clear.cache"), toolbarAction.getAttribute("text"));
            assertEquals(DocViewBundle.message("action.clear.cache"), catalogAction.getAttribute("text"));
            assertEquals(DocViewBundle.message("action.clear.cache.description"), toolbarAction.getAttribute("description"));
            assertEquals(DocViewBundle.message("action.clear.cache.description"), catalogAction.getAttribute("description"));
            assertFalse(DocViewBundle.message("action.clear.cache").isBlank());
            assertFalse(DocViewBundle.message("action.clear.cache.description").isBlank());
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

    @Override
    protected void tearDown() throws Exception {
        try {
            ApplicationManager.getApplication().invokeAndWait(() -> {
                FileEditorManager manager = FileEditorManager.getInstance(getProject());
                for (VirtualFile file : scratchFiles) {
                    if (file.isValid() && manager.isFileOpen(file)) {
                        manager.closeFile(file);
                    }
                }
            });
            WriteCommandAction.writeCommandAction(getProject()).withName("Clean up Clear Cache fixture")
                    .compute(() -> {
                        for (VirtualFile file : scratchFiles) {
                            VirtualFile parent = file.getParent();
                            if (file.isValid()) {
                                file.delete(getProject());
                            }
                            if (parent != null && parent.getName().startsWith("ClearCacheActionTest-")) {
                                deleteEmptyFixtureParents(parent);
                            }
                        }
                        return null;
                    });
        } finally {
            super.tearDown();
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

    private VirtualFile createOpenScratchFileAtPath(String path, String text) throws Exception {
        VirtualFile file = WriteCommandAction.writeCommandAction(getProject())
                .withName("Create Clear Cache scratch fixture")
                .compute(() -> {
                    VirtualFile created = ExtensionsRootType.getInstance()
                            .findFile(getProject(), path, Option.create_if_missing);
                    VfsUtil.saveText(created, text);
                    return created;
                });
        scratchFiles.add(file);
        ApplicationManager.getApplication().invokeAndWait(() ->
                FileEditorManager.getInstance(getProject()).openFile(file, true));
        assertTrue("The synthetic Extensions document must open in an editor",
                FileEditorManager.getInstance(getProject()).isFileOpen(file));
        return file;
    }

    private DtoSchemaCacheKey seedCache(DtoSchemaCacheService cache, String typeIdentity) {
        DtoSchemaCacheKey key = cache.key("response", "clear-cache-safety." + typeIdentity);
        Body body = new Body();
        body.setName("cached-schema");
        cache.putBody(key, body);
        assertNotNull("The fixture must contain a readable cache entry before invoking the action", cache.getBody(key));
        return key;
    }

    private void perform(AnAction action, SimpleTree tree) {
        DataContext context = dataId -> {
            if (CommonDataKeys.PROJECT.is(dataId)) {
                return getProject();
            }
            if (tree != null && DocViewDataKeys.WINDOW_CATALOG_TREE.is(dataId)) {
                return tree;
            }
            return null;
        };
        AnActionEvent event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, context);
        ApplicationManager.getApplication().invokeAndWait(() -> action.actionPerformed(event));
    }

    private void assertPreserved(VirtualFile file, byte[] expectedBytes) throws IOException {
        assertTrue("The file must remain valid: " + file.getPath(), file.isValid());
        assertTrue("The file bytes must remain unchanged: " + file.getPath(),
                Arrays.equals(expectedBytes, file.contentsToByteArray()));
        if (ExtensionsRootType.getInstance().containsFile(file)) {
            assertTrue("The Scratch/Extensions editor must remain open: " + file.getPath(),
                    FileEditorManager.getInstance(getProject()).isFileOpen(file));
        }
    }

    private void deleteEmptyFixtureParents(VirtualFile parent) throws IOException {
        while (parent != null && parent.isValid() && parent.isDirectory()
                && parent.getName().startsWith("ClearCacheActionTest-") && parent.getChildren().length == 0) {
            VirtualFile next = parent.getParent();
            parent.delete(getProject());
            parent = next;
        }
    }

    private Element findAction(Document document, String actionId) {
        NodeList actions = document.getElementsByTagName("action");
        for (int index = 0; index < actions.getLength(); index++) {
            Element action = (Element) actions.item(index);
            if (actionId.equals(action.getAttribute("id"))) {
                return action;
            }
        }
        fail("Missing action descriptor: " + actionId);
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
}
