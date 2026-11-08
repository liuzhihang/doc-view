package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.DumbModeTestUtils;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.action.toolbar.window.catalog.CatalogHttpClientAction;
import com.liuzhihang.doc.view.action.toolbar.window.catalog.CatalogOpenAction;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.http.HttpRequestBuilder;
import com.liuzhihang.doc.view.service.http.HttpRequestService;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;
import com.liuzhihang.doc.view.ui.window.DocViewToolWindowFactory;
import com.liuzhihang.doc.view.ui.window.DocViewNode;
import com.liuzhihang.doc.view.ui.window.MethodNode;
import com.liuzhihang.doc.view.ui.window.DocViewWindowPanel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class DocViewActionLayoutTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AnnotationStubs.installSpringWeb(myFixture);
        AnnotationStubs.installDubbo(myFixture);
    }

    public void testToolWindowToolbarContainsOnlyTheFiveCommonEntriesAndTwoSeparators() throws Exception {
        Document descriptor = loadPluginDescriptor();
        Element toolbar = findById(descriptor, "group", "liuzhihang.doc.tool.window.toolbar.action");

        assertEquals("The toolbar should keep the five common entries in their specified order",
                List.of(
                        "action:liuzhihang.doc.tool.window.toolbar.action.refresh",
                        "separator",
                        "group:liuzhihang.doc.tool.window.toolbar.action.export.menu",
                        "action:liuzhihang.doc.tool.window.toolbar.action.upload",
                        "separator",
                        "action:liuzhihang.doc.tool.window.toolbar.action.expand",
                        "action:liuzhihang.doc.tool.window.toolbar.action.collapse"),
                directEntries(toolbar));

        assertNotNull("Moving maintenance actions must preserve the Settings action ID",
                findById(descriptor, "action", "liuzhihang.doc.tool.window.toolbar.action.setting"));
        assertNotNull("Moving maintenance actions must preserve the Clear Cache action ID",
                findById(descriptor, "action", "liuzhihang.doc.tool.window.toolbar.action.clear"));

        Element catalog = findById(descriptor, "group", "liuzhihang.doc.tool.window.catalog.action");
        assertNull("Project-level Clear Cache must not remain in the catalog context menu",
                findByIdOrNull(catalog, "action", "liuzhihang.doc.tool.window.catalog.action.clear"));

        Element maintenance = findById(descriptor, "group", "liuzhihang.doc.tool.window.maintenance.action");
        assertEquals("The native gear group should contain only the existing maintenance actions",
                List.of("action:liuzhihang.doc.tool.window.toolbar.action.setting",
                        "action:liuzhihang.doc.tool.window.toolbar.action.clear"), directEntries(maintenance));
        Element retainedCatalogClear = findById(descriptor, "action",
                "liuzhihang.doc.tool.window.catalog.action.clear");
        assertEquals("The legacy catalog action ID must remain registered for Find Action and shortcuts",
                "com.liuzhihang.doc.view.action.toolbar.window.WindowClearAction",
                retainedCatalogClear.getAttribute("class"));
        assertFalse("The legacy catalog action must no longer be a catalog menu child",
                retainedCatalogClear.getParentNode().isSameNode(catalog));
    }

    public void testFactoryAttachesMaintenanceActionsThroughTheNativeGearApiAndProvidesItsProject() {
        AtomicReference<ActionGroup> registeredGearActions = new AtomicReference<>();
        AtomicReference<Content> createdContent = new AtomicReference<>();
        Project project = getProject();
        ContentManager contentManager = proxy(ContentManager.class, (proxy, method, args) -> {
            if ("addContent".equals(method.getName())) {
                createdContent.set((Content) args[0]);
                return null;
            }
            return defaultValue(method.getReturnType());
        });
        ToolWindow toolWindow = proxy(ToolWindow.class, (proxy, method, args) -> {
            if ("setAdditionalGearActions".equals(method.getName())) {
                registeredGearActions.set((ActionGroup) args[0]);
                return null;
            }
            if ("getContentManager".equals(method.getName())) {
                return contentManager;
            }
            if ("getProject".equals(method.getName())) {
                return project;
            }
            if ("isDisposed".equals(method.getName())) {
                return true;
            }
            return defaultValue(method.getReturnType());
        });

        try {
            ApplicationManager.getApplication().invokeAndWait(() ->
                    new DocViewToolWindowFactory().createToolWindowContent(project, toolWindow));

            ActionManager actionManager = ActionManager.getInstance();
            ActionGroup expectedGroup = (ActionGroup) actionManager.getAction(
                    "liuzhihang.doc.tool.window.maintenance.action");
            assertNotNull("The maintenance action group must be registered for Find Action", expectedGroup);
            assertNotNull("The factory must attach Doc View maintenance actions to the native gear menu",
                    registeredGearActions.get());
            assertSame(expectedGroup, registeredGearActions.get());
            List<String> gearActionIds = new ArrayList<>();
            for (var action : registeredGearActions.get().getChildren(null)) {
                gearActionIds.add(actionManager.getId(action));
            }
            assertEquals(List.of("liuzhihang.doc.tool.window.toolbar.action.setting",
                    "liuzhihang.doc.tool.window.toolbar.action.clear"), gearActionIds);

            Content content = createdContent.get();
            assertNotNull("The fixture must capture the tool window content", content);
            DocViewWindowPanel panel = (DocViewWindowPanel) content.getComponent();
            AtomicReference<Project> projectInSnapshot = new AtomicReference<>();
            DataSink sink = proxy(DataSink.class, (proxy, method, args) -> {
                if ("set".equals(method.getName()) && args[0] == CommonDataKeys.PROJECT) {
                    projectInSnapshot.set((Project) args[1]);
                }
                return null;
            });
            ApplicationManager.getApplication().invokeAndWait(() -> panel.uiDataSnapshot(sink));
            assertSame("Gear actions must resolve the project belonging to their Doc View window",
                    project, projectInSnapshot.get());
        } finally {
            Content content = createdContent.get();
            if (content != null && content.getDisposer() instanceof DocViewWindowPanel panel) {
                ApplicationManager.getApplication().invokeAndWait(panel::dispose);
            }
        }
    }

    public void testOpenActionHasNoTreeExpansionSideEffectForANonMethodNode() {
        TestDocViewNode selectedNode = new TestDocViewNode();
        ExpansionTrackingTree tree = new ExpansionTrackingTree(selectedNode);
        tree.expansionRequests = 0;
        assertTrue("The fixture must select a node that has a child", tree.hasExpandableSelectedNode());
        assertFalse("The selected expandable node must start collapsed", tree.isSelectedNodeExpanded());
        CatalogOpenAction action = new CatalogOpenAction();
        DataContext context = dataId -> {
            if (CommonDataKeys.PROJECT.is(dataId)) {
                return getProject();
            }
            if (DocViewDataKeys.WINDOW_CATALOG_TREE.is(dataId)) {
                return tree;
            }
            return null;
        };
        AnActionEvent event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, context);

        ApplicationManager.getApplication().invokeAndWait(() -> {
            action.update(event);
            assertFalse("Open Markdown must be hidden for a non-method node",
                    event.getPresentation().isVisible());
            assertFalse("Open Markdown must be disabled for a non-method node",
                    event.getPresentation().isEnabled());
            action.actionPerformed(event);
        });

        assertEquals("Opening a non-method node must not expand the catalog tree", 0, tree.expansionRequests);
        assertFalse("Opening a non-method node must leave its collapsed descendants collapsed",
                tree.isSelectedNodeExpanded());
    }

    public void testHttpActionHidesForEmptyNonMethodDubboAndInvalidSelections() {
        CatalogHttpClientAction action = new CatalogHttpClientAction();
        assertUnavailable(action, event(action, new SelectedNodeTree(null)), "empty selection");
        assertUnavailable(action, event(action, new SelectedNodeTree(new TestDocViewNode())), "non-method selection");

        PsiClass dubboClass = myFixture.addClass("""
                package contract.actionstate;
                @org.apache.dubbo.config.annotation.DubboService
                public class DubboOnlyService {
                    @org.springframework.web.bind.annotation.GetMapping("/not-http-for-doc-view")
                    public void call() { }
                }
                """);
        MethodNode dubboMethod = methodNode(dubboClass, "call");
        assertFalse("The fixture must represent a Dubbo class, not an HTTP endpoint",
                dubboMethod.isHttpEndpoint());
        assertUnavailable(action, event(action, new SelectedNodeTree(dubboMethod)), "Dubbo method");

        PsiClass springClass = springEndpointClass("InvalidEndpoint",
                "@org.springframework.web.bind.annotation.GetMapping(\"/items\")");
        MethodNode invalidMethod = methodNode(springClass, "call");
        WriteCommandAction.runWriteCommandAction(getProject(), invalidMethod.getPsiMethod()::delete);
        assertFalse("The fixture method must be invalidated before checking the action",
                invalidMethod.getPsiMethod().isValid());
        assertUnavailable(action, event(action, new SelectedNodeTree(invalidMethod)), "invalid PSI method");
    }

    public void testHttpActionHidesWhenMappingIsRemovedFromAnExistingMethodNode() {
        CatalogHttpClientAction action = new CatalogHttpClientAction();
        MethodNode method = methodNode(springEndpointClass("MappingRemovedEndpoint",
                "@org.springframework.web.bind.annotation.GetMapping(\"/items\")"), "call");
        assertTrue("The fixture must begin with an HTTP endpoint", method.isHttpEndpoint());

        var mapping = method.getPsiMethod().getAnnotation("org.springframework.web.bind.annotation.GetMapping");
        assertNotNull("The fixture method must have its mapping annotation", mapping);
        WriteCommandAction.runWriteCommandAction(getProject(), mapping::delete);
        assertTrue("Removing an annotation must leave the selected method itself valid",
                method.getPsiMethod().isValid());
        assertFalse("Current PSI must no longer classify the method as an HTTP candidate",
                HttpRequestBuilder.isCandidate(method.getPsiMethod()));

        assertUnavailable(action, event(action, new SelectedNodeTree(method)), "method after mapping removal");
    }

    public void testHttpActionShowsValidEndpointAndReportsActualDependencyState() {
        CatalogHttpClientAction action = new CatalogHttpClientAction();
        MethodNode method = methodNode(springEndpointClass("AvailableEndpoint",
                "@org.springframework.web.bind.annotation.GetMapping(\"/items\")"), "call");
        AnActionEvent event = event(action, new SelectedNodeTree(method));

        EdtTestUtil.runInEdtAndWait(() -> action.update(event));

        assertTrue("A valid Spring endpoint should remain visible even when the client dependency is unavailable",
                event.getPresentation().isVisible());
        assertSame("HTTP action state is read from the tree on EDT",
                com.intellij.openapi.actionSystem.ActionUpdateThread.EDT, action.getActionUpdateThread());
        if (HttpRequestService.isHttpClientAvailable()) {
            assertTrue("A valid endpoint with the client available should be enabled",
                    event.getPresentation().isEnabled());
            assertEquals(DocViewBundle.message(
                            "action.liuzhihang.doc.view.action.toolbar.window.catalog.httpclient.description"),
                    event.getPresentation().getDescription());
        } else {
            assertFalse("A valid endpoint without the client dependency should be disabled",
                    event.getPresentation().isEnabled());
            assertEquals(DocViewBundle.message("http.request.unavailable.client"),
                    event.getPresentation().getDescription());
        }
    }

    public void testHttpActionReportsIndexingForAValidEndpoint() {
        CatalogHttpClientAction action = new CatalogHttpClientAction();
        MethodNode method = methodNode(springEndpointClass("IndexingEndpoint",
                "@org.springframework.web.bind.annotation.GetMapping(\"/items\")"), "call");
        AnActionEvent event = event(action, new SelectedNodeTree(method));

        DumbModeTestUtils.runInDumbModeSynchronously(getProject(), () -> {
            assertTrue("The fixture must actually enter Dumb Mode", DumbService.isDumb(getProject()));
            EdtTestUtil.runInEdtAndWait(() -> action.update(event));
        });

        assertTrue("The endpoint remains discoverable while indexing", event.getPresentation().isVisible());
        assertFalse("Generation must be disabled while indexing", event.getPresentation().isEnabled());
        assertEquals(DocViewBundle.message("http.request.unavailable.indexing"),
                event.getPresentation().getDescription());
    }

    private void assertUnavailable(CatalogHttpClientAction action, AnActionEvent event, String reason) {
        EdtTestUtil.runInEdtAndWait(() -> action.update(event));
        assertFalse("HTTP request generation must be hidden for " + reason, event.getPresentation().isVisible());
        assertFalse("HTTP request generation must be disabled for " + reason, event.getPresentation().isEnabled());
    }

    private AnActionEvent event(CatalogHttpClientAction action, SimpleTree tree) {
        DataContext context = dataId -> {
            if (CommonDataKeys.PROJECT.is(dataId)) {
                return getProject();
            }
            if (DocViewDataKeys.WINDOW_CATALOG_TREE.is(dataId)) {
                return tree;
            }
            return null;
        };
        return AnActionEvent.createFromAnAction(action, null, ActionPlaces.POPUP, context);
    }

    private PsiClass springEndpointClass(String className, String mapping) {
        return myFixture.addClass("""
                package contract.actionstate;
                @org.springframework.web.bind.annotation.RestController
                public class %s {
                    %s
                    public void call() { }
                }
                """.formatted(className, mapping));
    }

    private MethodNode methodNode(PsiClass psiClass, String methodName) {
        PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
        assertEquals("The fixture class must contain exactly one selected method", 1, methods.length);
        return new TestMethodNode(psiClass, methods[0]);
    }

    private Document loadPluginDescriptor() throws Exception {
        try (InputStream descriptor = getClass().getClassLoader().getResourceAsStream("META-INF/plugin.xml")) {
            assertNotNull("The plugin descriptor must be available to the test", descriptor);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            return factory.newDocumentBuilder().parse(descriptor);
        }
    }

    private Element findById(Document document, String tag, String id) {
        Element element = findByIdOrNull(document.getDocumentElement(), tag, id);
        assertNotNull("Missing " + tag + " descriptor: " + id, element);
        return element;
    }

    private Element findByIdOrNull(Element parent, String tag, String id) {
        if (tag.equals(parent.getTagName()) && id.equals(parent.getAttribute("id"))) {
            return parent;
        }
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element childElement) {
                Element found = findByIdOrNull(childElement, tag, id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private List<String> directEntries(Element group) {
        List<String> entries = new ArrayList<>();
        for (Node child = group.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof Element element)) {
                continue;
            }
            if ("separator".equals(element.getTagName())) {
                entries.add("separator");
            } else if ("action".equals(element.getTagName()) || "group".equals(element.getTagName())) {
                entries.add(element.getTagName() + ":" + element.getAttribute("id"));
            }
        }
        return entries;
    }

    private <T> T proxy(Class<T> type, InvocationHandler handler) {
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (instance, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> type.getSimpleName() + " test proxy";
                    case "hashCode" -> System.identityHashCode(instance);
                    case "equals" -> instance == args[0];
                    default -> null;
                };
            }
            return handler.invoke(instance, method, args);
        });
        return type.cast(proxy);
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }

    private static final class ExpansionTrackingTree extends SimpleTree {
        private final SimpleNode selectedNode;
        private final TreePath selectedExpandablePath;
        private int expansionRequests;

        private ExpansionTrackingTree(SimpleNode selectedNode) {
            super(expandableModel());
            this.selectedNode = selectedNode;
            Object root = getModel().getRoot();
            Object expandableChild = getModel().getChild(root, 0);
            selectedExpandablePath = new TreePath(new Object[]{root, expandableChild});
            setSelectionPath(selectedExpandablePath);
        }

        @Override
        public SimpleNode getSelectedNode() {
            return selectedNode;
        }

        @Override
        public void expandPath(TreePath path) {
            expansionRequests++;
            super.expandPath(path);
        }

        private boolean hasExpandableSelectedNode() {
            return getModel().getChildCount(selectedExpandablePath.getLastPathComponent()) > 0;
        }

        private boolean isSelectedNodeExpanded() {
            return isExpanded(selectedExpandablePath);
        }

        private static DefaultTreeModel expandableModel() {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
            DefaultMutableTreeNode expandableChild = new DefaultMutableTreeNode("expandable child");
            expandableChild.add(new DefaultMutableTreeNode("grandchild"));
            root.add(expandableChild);
            return new DefaultTreeModel(root);
        }
    }

    private static final class TestDocViewNode extends DocViewNode {
        private TestDocViewNode() {
            super(null);
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
            return "";
        }

        @Override
        public String httpPath(Project project) {
            return "";
        }
    }

    private static final class TestMethodNode extends MethodNode {
        private TestMethodNode(PsiClass psiClass, PsiMethod psiMethod) {
            super(null, psiClass, psiMethod);
        }
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
}
