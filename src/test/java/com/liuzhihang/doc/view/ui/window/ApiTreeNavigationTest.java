package com.liuzhihang.doc.view.ui.window;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.psi.PsiClass;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Arrays;
import java.util.List;

public final class ApiTreeNavigationTest extends LightJavaCodeInsightFixtureTestCase {

    private PsiClass controller;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addClass("package java.lang; public final class String {}");
        AnnotationStubs.installSpringWeb(myFixture);
        controller = myFixture.addClass("""
                package contract.navigation;
                import org.springframework.web.bind.annotation.*;

                /** 用户接口 */
                @RestController
                public class UsersController {
                    /** 查询用户 */
                    @GetMapping("/users/name")
                    public String find(String name) { return name; }

                    /** 查询用户 */
                    @GetMapping("/users/id")
                    public String find(long id) { return ""; }

                    @GetMapping("/ping")
                    public void ping() {}
                }
                """);
    }

    public void testClassPresentationKeepsCodeAndBusinessNamesDistinct() {
        ClassNode node = classNode();
        node.update();
        PresentationData presentation = node.getPresentation();
        assertEquals("A code name and business description should both be visible", 2,
                presentation.getColoredText().size());
        assertEquals("UsersController", presentation.getColoredText().getFirst().getText());
        assertEquals("用户接口", presentation.getColoredText().get(1).getText().trim());
        assertEquals(SimpleTextAttributes.GRAYED_ATTRIBUTES,
                presentation.getColoredText().get(1).getAttributes());
        assertEquals("The export title remains the business title", "用户接口", node.getName());
    }

    public void testOverloadsShowMethodNamesWithSignaturesInTooltipAndSearch() {
        SimpleNode[] methods = classNode().getChildren();
        methods[0].update();
        methods[1].update();
        assertEquals("The first overload needs method name and business description", 2,
                methods[0].getPresentation().getColoredText().size());
        assertEquals("The second overload needs method name and business description", 2,
                methods[1].getPresentation().getColoredText().size());
        assertEquals("find", methods[0].getPresentation().getColoredText().getFirst().getText());
        assertEquals("find", methods[1].getPresentation().getColoredText().getFirst().getText());
        assertEquals("查询用户", methods[0].getPresentation().getColoredText().get(1).getText().trim());
        assertEquals("查询用户", methods[1].getPresentation().getColoredText().get(1).getText().trim());
        assertTrue(methods[0].getPresentation().getTooltip().contains("find(java.lang.String)"));
        assertTrue(methods[1].getPresentation().getTooltip().contains("find(long)"));
        assertSame(com.intellij.icons.AllIcons.Nodes.Method, methods[0].getPresentation().getIcon(false));
        assertSame(com.intellij.icons.AllIcons.Nodes.Method, methods[1].getPresentation().getIcon(false));
    }

    public void testMethodNameFallbackIsNotRepeatedAsDescription() {
        SimpleNode method = classNode().getChildren()[2];
        method.update();
        assertEquals(1, method.getPresentation().getColoredText().size());
        assertEquals("ping", method.getPresentation().getColoredText().getFirst().getText());
        assertTrue(method.getPresentation().getTooltip().contains("ping()"));
    }

    public void testRefreshDoesNotAccumulateMethods() {
        ClassNode node = classNode();
        assertEquals(3, node.getChildren().length);
        node.updateNode(getProject());
        node.cleanUpCache();
        assertEquals("Refreshing the same class must replace its methods", 3, node.getChildren().length);
        node.updateNode(getProject());
        node.cleanUpCache();
        assertEquals(3, node.getChildren().length);
    }

    public void testPresentationDoesNotChangeExistingDocumentPaths() {
        ClassNode node = classNode();
        MethodNode method = (MethodNode) node.getChildren()[0];
        node.update();
        method.update();
        assertEquals("查询用户", method.getName());
        assertEquals("Doc View/" + getModule().getName() + "/用户接口", node.docPath(getProject()));
        assertEquals(node.docPath(getProject()) + "/查询用户.md", method.docPath(getProject()));
        assertEquals("Http/" + getModule().getName() + "/用户接口/查询用户.http", method.httpPath(getProject()));
    }

    public void testSpeedSearchIncludesCodeSignatureAndBusinessDescription() throws Exception {
        ClassNode node = classNode();
        MethodNode method = (MethodNode) node.getChildren()[0];
        var searchText = DocViewNode.class.getMethod("getSearchText");
        assertEquals("UsersController 用户接口", searchText.invoke(node));
        assertEquals("find(java.lang.String) 查询用户", searchText.invoke(method));
        assertEquals("ping()", searchText.invoke(node.getChildren()[2]));
        assertEquals("The platform formatter must keep the class search text", node.getSearchText(),
                TreeSpeedSearch.NODE_PRESENTATION_FUNCTION.apply(pathFor(node)));
        assertEquals("The platform formatter must keep the full method signature and title",
                method.getSearchText(), TreeSpeedSearch.NODE_PRESENTATION_FUNCTION.apply(pathFor(method)));
        assertEquals("The method's renderer fallback must retain the prior search text",
                method.getSearchText(), method.toString());
    }

    public void testSpeedSearchIncludesFullDescriptionThroughStructureTreeModel() throws Exception {
        PsiClass describedController = myFixture.addClass("""
                package contract.navigation;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class SearchDescriptionController {
                    /** Nativepageitem23 tailneedle */
                    @GetMapping("/search-description")
                    public void searchDescription() {}
                }
                """);
        ClassNode node = new ClassNode(new ModuleNode(new RootNode(), getModule()), describedController);
        MethodNode method = Arrays.stream(node.getChildren()).map(MethodNode.class::cast)
                .filter(candidate -> "searchDescription".equals(candidate.getPsiMethod().getName()))
                .findFirst().orElseThrow();

        assertEquals("The visible business title keeps the existing 15-character rule",
                "Nativepageitem2", method.getName());
        assertTrue("The tooltip keeps the complete method description",
                method.getPresentation().getTooltip().contains("Nativepageitem23tailneedle"));
        method.update();
        assertEquals("The method row keeps the existing truncated business title", "Nativepageitem2",
                method.getPresentation().getColoredText().get(1).getText().trim());
        assertTrue("The speed-search text must include description characters beyond the title limit",
                method.getSearchText().contains("tailneedle"));
        assertTrue("The platform formatter on a real StructureTreeModel path must include the description tail",
                speedSearchTextFromStructureTreeModel(node, method).contains("tailneedle"));
        assertTrue("Markdown file naming must keep the existing truncated business title",
                method.docPath(getProject()).endsWith("/Nativepageitem2.md"));
        assertTrue("HTTP file naming must keep the existing truncated business title",
                method.httpPath(getProject()).endsWith("/Nativepageitem2.http"));
    }

    public void testSourceNavigationUsesValidPsiAndIgnoresDeletedMethods() throws Exception {
        ClassNode node = classNode();
        MethodNode method = (MethodNode) node.getChildren()[0];
        var source = DocViewWindowPanel.class.getDeclaredMethod("sourceElement", SimpleNode.class);
        source.setAccessible(true);
        assertSame(controller, source.invoke(null, node));
        assertSame(method.getPsiMethod(), source.invoke(null, method));
        assertNull(source.invoke(null, new RootNode()));
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(getProject(), () -> method.getPsiMethod().delete());
        assertNull(source.invoke(null, method));
    }

    public void testModuleClassOrderIsStableAndDoesNotReorderMethods() {
        myFixture.addClass("""
                package contract.navigation;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class AlphaController {
                    @GetMapping("/z-first") public void zFirst() {}
                    @GetMapping("/a-second") public void aSecond() {}
                }
                """);
        myFixture.addClass("""
                package contract.navigation;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class ZuluController {
                    @GetMapping("/only") public void only() {}
                }
                """);

        ModuleNode moduleNode = new ModuleNode(new RootNode(), getModule());
        List<String> firstOrder = classQualifiedNames(moduleNode);
        assertTrue("The module scan must include all three Spring fixture controllers",
                firstOrder.containsAll(List.of("contract.navigation.AlphaController",
                        "contract.navigation.UsersController", "contract.navigation.ZuluController")));
        assertEquals("Class nodes must be ordered by source qualified name", firstOrder.stream().sorted().toList(), firstOrder);

        ClassNode alpha = Arrays.stream(moduleNode.getChildren())
                .map(ClassNode.class::cast)
                .filter(node -> "contract.navigation.AlphaController".equals(node.getPsiClass().getQualifiedName()))
                .findFirst().orElseThrow();
        List<String> methodOrder = Arrays.stream(alpha.getChildren())
                .map(MethodNode.class::cast)
                .map(method -> method.getPsiMethod().getName())
                .toList();
        assertEquals("Class ordering must not reorder methods from the source class", List.of("zFirst", "aSecond"), methodOrder);

        moduleNode.updateNode(getProject());
        moduleNode.cleanUpCache();
        assertEquals("Repeated scans must publish classes in the same order", firstOrder, classQualifiedNames(moduleNode));
    }

    private static List<String> classQualifiedNames(ModuleNode moduleNode) {
        return Arrays.stream(moduleNode.getChildren())
                .map(ClassNode.class::cast)
                .map(node -> node.getPsiClass().getQualifiedName())
                .toList();
    }

    private ClassNode classNode() {
        return new ClassNode(new ModuleNode(new RootNode(), getModule()), controller);
    }

    private static TreePath pathFor(DocViewNode node) {
        return new TreePath(new DefaultMutableTreeNode(node));
    }

    private String speedSearchTextFromStructureTreeModel(ClassNode root, MethodNode target) throws Exception {
        StructureTreeModel<SimpleTreeStructure> model = new StructureTreeModel<>(new SimpleTreeStructure() {
            @Override
            public Object getRootElement() {
                return root;
            }
        }, getTestRootDisposable());
        return model.getInvoker().compute(() -> {
            TreeNode modelRoot = model.getRootImmediately();
            for (int index = 0; index < model.getChildCount(modelRoot); index++) {
                TreeNode child = model.getChild(modelRoot, index);
                if (child instanceof DefaultMutableTreeNode wrapper && wrapper.getUserObject() == target) {
                    return TreeSpeedSearch.NODE_PRESENTATION_FUNCTION.apply(new TreePath(new Object[]{modelRoot, child}));
                }
            }
            throw new AssertionError("The StructureTreeModel wrapper must contain the target MethodNode");
        }).blockingGet(10, java.util.concurrent.TimeUnit.SECONDS);
    }
}
